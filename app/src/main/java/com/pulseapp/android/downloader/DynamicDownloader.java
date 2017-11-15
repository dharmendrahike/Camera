package com.pulseapp.android.downloader;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.ViewPublicMomentFragment;
import com.pulseapp.android.models.LocalDataHelper;
import com.pulseapp.android.models.MediaDownloadDetails;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.PrivacyPopup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

//Created by deepankur on 7/14/16.

public class DynamicDownloader implements FireBaseKEYIDS {

    private static String TAG = DynamicDownloader.class.getSimpleName();
    private OnMomentDownloadListener onFollowersMomentDownloadListener;
//    private LinkedHashMap<String, MomentType> momentIdsSet;

    private MomentType getMomentType(MomentModel momentModel) {
        return getMomentType(momentModel.momentId);
    }

    private MomentType getMomentType(String momentId) {
        if (!streamsLinkedMap.containsKey(momentId))
            return null;
        return streamsLinkedMap.get(momentId).getValue();
    }

    private static FireBaseHelper fireBaseHelper;
    //    private static Context context;
    private static String myUserId;
    private static DynamicDownloader dynamicDownloader;

    public static DynamicDownloader getInstance(Context context) {
//        StorageHelper.printStats(context);
//        if (DynamicDownloader.context == null) DynamicDownloader.context = context;
        if (DynamicDownloader.fireBaseHelper == null)
            fireBaseHelper = FireBaseHelper.getInstance(context);
        if (DynamicDownloader.myUserId == null)
            DynamicDownloader.myUserId = fireBaseHelper.getMyUserId();

        if (dynamicDownloader == null)
            dynamicDownloader = new DynamicDownloader(context);
        return dynamicDownloader;
    }

    //around you downloader constructor ONLY
    protected DynamicDownloader(Context context) {
//        this.momentIdsSet = new LinkedHashMap<>();
        localDataHelper = LocalDataHelper.getInstance(fireBaseHelper, LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD);

        this.validateNearbyFileSystem(context);
        this.playlistController = PlaylistController.getInstance(context);
    }

    private static String nearbyMediaStoragePath;

    private void validateNearbyFileSystem(Context context) {
        nearbyMediaStoragePath = this.getFilesDirectory(context);
        nearbyMediaStoragePath += "downloadedMedia" + File.separator;
        File mediaDir = new File(nearbyMediaStoragePath);
        if (!mediaDir.exists()) {
            final boolean mkdirs = mediaDir.mkdirs();
            if (!mkdirs) Log.e(TAG, " directory not made");
        }
    }

    public static String getFilesDirectory(Context mContext) {
        return mContext.getFilesDir().getAbsolutePath() + File.separator;
    }

    public enum MomentType {PUBLIC_MOMENT, FRIEND_MOMENT, FOLLOWER_MOMENT}

    private LocalDataHelper localDataHelper;
    /**
     * All the moments on which downloader is called will be stored here only if the local data is not ready
     * The object will be nullyfied after use.
     */
    private LinkedHashMap<MomentModel, MomentType> waitingForLocalDataMap;

    public void registerNewStreamForDownload(final MomentModel momentModel, final MomentType momentType) {

        if (!localDataHelper.isReady()) {
            if (localDataHelper.getLocalDataReadyListener() == null)
                localDataHelper.setLocalDataReadyListener(new LocalDataHelper.LocalDataReadyListener() {
                    @Override
                    public void onLocalDataReady() {
                        for (Map.Entry<MomentModel, MomentType> entry : waitingForLocalDataMap.entrySet()) {
                            registerNewStreamForDownload(entry.getKey(), entry.getValue());
                        }
                        waitingForLocalDataMap = null;
                    }
                });
            if (waitingForLocalDataMap == null) {
                waitingForLocalDataMap = new LinkedHashMap<>();
                waitingForLocalDataMap.put(momentModel, momentType);
            }
            return;
        }

        if (fireBaseHelper != null && fireBaseHelper.getFirebaseHelperHandler() != null) {
            fireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (streamsLinkedMap.containsKey(momentModel.momentId)) {
                        Log.e(TAG, "downloader already has stream : " + momentModel.momentId);
                        return;
                    }
                    momentModel.medias = new LinkedHashMap<>();
                    streamsLinkedMap.put(momentModel.momentId, (new MapReturn<MomentModel, MomentType>()).returnMapValue(momentModel, momentType));

//                    mediasQueriedPerMomentMap.put(momentModel, 0);
                    arrayListLinkedHashMap.put(momentModel, new ArrayList<MomentModel.Media>());
                    peekIndexOfMediaWatchedMap.put(momentModel, -1);
                    addListenersOnMediasOfNearByMoment(momentModel);
                }
            });
        }
    }


//    private LinkedHashMap<MomentModel, Integer> mediasQueriedPerMomentMap = new LinkedHashMap<>();

    /**
     * function to be removed; currently returns true always
     */
//    private boolean allMediasQueryWRTMomentFromDataBase(MomentModel momentModel) {
//        final int totalMedias = arrayListLinkedHashMap.get(momentModel).size();
//        int mediasQueriedFromDb = mediasQueriedPerMomentMap.get(momentModel);
//        return mediasQueriedFromDb != 0 && mediasQueriedFromDb >= totalMedias;
//    }

//    private void notifyMediaQueriedFromLocalData(MomentModel momentModel, MomentModel.Media media) {
//        int previousCount = mediasQueriedPerMomentMap.get(momentModel);
//        mediasQueriedPerMomentMap.put(momentModel, ++previousCount);
//    }

    private class MapReturn<K, V> extends HashMap<K, V> {
        public Map.Entry<K, V> returnMapValue(K k, V v) {
            return new java.util.AbstractMap.SimpleEntry<K, V>(k, v);
        }
    }

    private boolean isExpiredMedia(MomentModel.Media media, MomentType momentType) {
        if (momentType == MomentType.PUBLIC_MOMENT) return false;
        long now = System.currentTimeMillis() - (fireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000);
        long dtStart = media.createdAt;
        return now > dtStart;
    }

    /**
     * Every Media and moment will be represented here
     */
    private final LinkedHashMap<String, Map.Entry<MomentModel, MomentType>> streamsLinkedMap = new LinkedHashMap<>();

    public LinkedHashMap<String, Map.Entry<MomentModel, MomentType>> getStreamsLinkedMap() {
        return streamsLinkedMap;
    }

    MomentModel getMomentModel(String momentId) {
        if (!streamsLinkedMap.containsKey(momentId)) return null;
        final Map.Entry<MomentModel, MomentType> momentModelMomentTypeEntry = streamsLinkedMap.get(momentId);
        return momentModelMomentTypeEntry.getKey();
    }

    /**
     * Downloading will be index oriented
     */
    private final LinkedHashMap<MomentModel, ArrayList<MomentModel.Media>> arrayListLinkedHashMap = new LinkedHashMap<>();

    public LinkedHashMap<MomentModel, ArrayList<MomentModel.Media>> getMomentMediaHashMap() {
        return arrayListLinkedHashMap;
    }

    private void addListenersOnMediasOfNearByMoment(final MomentModel momentModel) {
        final Handler mediaHandler = new Handler();
        Query firebase = fireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentModel.momentId, MEDIA});
        if (getMomentType(momentModel) == MomentType.FRIEND_MOMENT || getMomentType(momentModel) == MomentType.FOLLOWER_MOMENT)
            firebase = firebase.orderByChild(CREATED_AT).startAt(fireBaseHelper.getMaximumMediaDuration());
        else
            firebase = firebase.orderByPriority();
        firebase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                if (fireBaseHelper != null && fireBaseHelper.getFirebaseHelperHandler() != null) {
                    fireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                        @Override
                        public void run() {
//                            Log.d(TAG, "run: crashing @ "+momentModel.momentId);
//                            Log.d(TAG, "run: crashing @ " + dataSnapshot.getKey());
                            final MomentModel.Media media = dataSnapshot.getValue(MomentModel.Media.class);
                            media.thumbnail = changeAmazonUrlToCDNUrl(media.thumbnail);
                            media.mediaId = dataSnapshot.getKey();
                            if (!dataSnapshot.hasChild(ANONYMOUS)) {
                                media.anonymous = true;
                            }
                            if (media.url == null) {
                                Log.e(TAG, " ignoring garbage value----> " + media.mediaId + " to be handled by crone");
                                return;
                            }
                            final ArrayList<MomentModel.Media> mediasList = arrayListLinkedHashMap.get(momentModel);
                            if (momentModel.medias.get(media.mediaId) != null) {
                                Log.e(TAG, "media id: " + media.mediaId + " already exists in the data supersetMediaMap");
                            } else {
                                Log.d(TAG, "new media added to moment " + momentModel.name + " id: " + media.mediaId);
                                if (!isExpiredMedia(media, getMomentType(momentModel))) {
                                    if (getMomentType(momentModel) == MomentType.PUBLIC_MOMENT || (getMomentType(momentModel) == MomentType.FOLLOWER_MOMENT && media.privacy != null && media.privacy.type == PrivacyPopup.ALL_FRIENDS_AND_FOLLOWERS) || (getMomentType(momentModel) == MomentType.FRIEND_MOMENT && !fireBaseHelper.checkIfMediaExempted(media))) {
                                        momentModel.medias.put(media.mediaId, media);
                                        mediasList.add(media);
                                        if (getMomentType(momentModel) == MomentType.FOLLOWER_MOMENT)
                                            onFollowersMomentDownloadListener.nullifyMomentPreviousThumbnail(momentModel);
                                        else if (getMomentType(momentModel) == MomentType.FRIEND_MOMENT)
                                            onFriendMomentDownloadListener.nullifyMomentPreviousThumbnail(momentModel);
                                        else if (getMomentType(momentModel) == MomentType.PUBLIC_MOMENT)
                                            onPublicMomentDownloadListener.nullifyMomentPreviousThumbnail(momentModel);
                                        notifyMediaAddedToMoment(momentModel, media);

                                        //Refresh indices so that both dynamic downloader and fragment can recalculate
                                        ViewPublicMomentFragment.refreshIndexOfPlaylistOnNewMediaAdded(momentModel);
                                        peekIndexOfMediaWatchedMap.put(momentModel, -1);

                                        mediaHandler.removeCallbacksAndMessages(null);
                                        mediaHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                AppLibrary.log_d(TAG, "Last Media for momentId -" + momentModel.momentId);

                                                if (watchNowTappedMomentId != null && momentModel.momentId.equals(watchNowTappedMomentId)) { //Handles case of tapping on stream before loading medias
                                                    if (!isMinimumMediasPresentToOpenMoment(watchNowTappedMomentId)) {
                                                        notifyTapOnMoment(watchNowTappedMomentId);
                                                    }
                                                    watchNowTappedMomentId = null;
                                                }
                                                if (getMomentType(momentModel) == MomentType.FOLLOWER_MOMENT)
                                                    onFollowersMomentDownloadListener.onLastMediaLoaded(media, momentModel);
                                                else if (getMomentType(momentModel) == MomentType.FRIEND_MOMENT)
                                                    onFriendMomentDownloadListener.onLastMediaLoaded(media, momentModel);
                                                else if (getMomentType(momentModel) == MomentType.PUBLIC_MOMENT)
                                                    onPublicMomentDownloadListener.onLastMediaLoaded(media, momentModel);
                                            }
                                        }, 500);
                                    }
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onChildChanged(final DataSnapshot dataSnapshot, String s) {
                if (fireBaseHelper != null && fireBaseHelper.getFirebaseHelperHandler() != null) {
                    fireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            final MomentModel.Media media = dataSnapshot.getValue(MomentModel.Media.class);
                            media.mediaId = dataSnapshot.getKey();
                            media.thumbnail = changeAmazonUrlToCDNUrl(media.thumbnail);
                            if (getMomentType(momentModel) == MomentType.FOLLOWER_MOMENT)
                                onFollowersMomentDownloadListener.onThumbnailChanged(media, momentModel);
                            else if (getMomentType(momentModel) == MomentType.FRIEND_MOMENT)
                                onFriendMomentDownloadListener.onThumbnailChanged(media, momentModel);
                            else if (getMomentType(momentModel) == MomentType.PUBLIC_MOMENT)
                                onPublicMomentDownloadListener.onThumbnailChanged(media, momentModel);
                        }
                    });
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }


    private void updateDownscaledUrl(MomentModel.Media media) {
        if (media.downscale != null && media.downscale.p_480 != null)
            media.url = media.downscale.p_480;
    }

    private String changeAmazonUrlToCDNUrl(final String amazonUrl){
        if (amazonUrl==null||amazonUrl.length()==0) return amazonUrl;
        return ThinDownloader.getCdnUrl(amazonUrl);
    }


    private void notifyMediaAddedToMoment(final MomentModel momentModel, final MomentModel.Media media) {
            if (media.type == MEDIA_TYPE_VIDEO)
                updateDownscaledUrl(media);
        final MediaDownloadDetails downloadDetails = localDataHelper.getMediaDownloadStatus(media.mediaId);
//        notifyMediaQueriedFromLocalData(momentModel, media);
        if (downloadDetails != null) {
            final Integer downloadStatusInt = downloadDetails.getStatus();
            if (downloadStatusInt >= MEDIA_DOWNLOAD_COMPLETE) {//downloaded or viewed
                Log.d(TAG, "notify media added " + downloadDetails.toString());
                String url = downloadDetails.getUrl();
                if (url != null /*&& dataSnapshot.child(URL).exists()*/) {
                    if ((new File(url)).exists()) {//everything Ok here
                        loadMediaPathFromStorage(momentModel, downloadDetails, media);
                    } else {
                        Log.e(TAG, "fireBase says media downloaded but media not present locally id: " + media.mediaId);
                        updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
                    }
                } else {
                    Log.e(TAG, "local uri is null in fireBase id: " + media.mediaId);
                    updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
                }
                //we also check whether the media is currently being downloaded as fireBase just
                //has status of either downloaded or not downloaded
            } else if (downloadStatusInt == MEDIA_DOWNLOAD_NOT_STARTED && !mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
            } else {//status is ERROR_DOWNLOADING or MEDIA_DOWNLOAD_IN_PROGRESS; do nothing here;currently unreachable statement as stated above

            }
        } else {//there is no existence of a particular media in localData so --- add to local download status and push
            updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
        }

        if (media.status == MEDIA_DOWNLOAD_NOT_STARTED)
            checkIfShouldDownloadNewMediaAdded(momentModel, media); //ToDo: This is where we need to control autoDownload at start

        //we will only call these function when all medias wrt moment has been queried;
        //ie we  just after getting the last media
        {
            updateStatusForViews(momentModel);
            checkForWaitingMoments(momentModel);
        }

//        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE, media.mediaId}).addListenerForSingleValueEvent(new ValueEventListener() {
//            @SuppressWarnings("StatementWithEmptyBody")
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
////                notifyMediaQueriedFromLocalData(momentModel, media);
//                if (dataSnapshot != null && dataSnapshot.getKey() != null && dataSnapshot.child(DOWNLOAD_STATUS).getValue(Integer.class) != null) {
//                    final Integer downloadStatusInt = dataSnapshot.child(DOWNLOAD_STATUS).getValue(Integer.class);
//                    if (downloadStatusInt >= MEDIA_DOWNLOAD_COMPLETE) {//downloaded or viewed
//                        Log.d(TAG, "notify media added " + dataSnapshot.toString());
//                        if (dataSnapshot.child(URL) != null && dataSnapshot.child(URL).exists()) {
//                            if ((new File(dataSnapshot.child(URL).getValue(String.class))).exists()) {//everything Ok here
//                                loadMediaPathFromStorage(momentModel, dataSnapshot, media);
//                            } else {
//                                Log.e(TAG, "fireBase says media downloaded but media not present locally id: " + media.mediaId);
//                                updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
//                            }
//                        } else {
//                            Log.e(TAG, "local uri is null in fireBase id: " + media.mediaId);
//                            updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
//                        }
//                        //we also check whether the media is currently being downloaded as fireBase just
//                        //has status of either downloaded or not downloaded
//                    } else if (downloadStatusInt == MEDIA_DOWNLOAD_NOT_STARTED && !mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
//                        updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
//                    } else {//status is ERROR_DOWNLOADING or MEDIA_DOWNLOAD_IN_PROGRESS; do nothing here;currently unreachable statement as stated above
//
//                    }
//                } else {//there is no existence of a particular media in localData so --- add to local download status and push
//                    updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_NOT_STARTED, media.url);
//                }
//                if (media.status == MEDIA_DOWNLOAD_NOT_STARTED)
//                    checkIfShouldDownloadNewMediaAdded(momentModel, media); //ToDo: This is where we need to control autoDownload at start
//
//                //we will only call these function when all medias wrt moment has been queried;
//                //ie we  just after getting the last media
//                if (allMediasQueryWRTMomentFromDataBase(momentModel)) {
//                    updateStatusForViews(momentModel);
//                    checkForWaitingMoments(momentModel);
//                }
//            }
//
//            @Override
//            public void onCancelled(FirebaseError firebaseError) {
//
//            }
//        });
    }

    private void updateLocalStatus(MomentModel momentModel, MomentModel.Media media, int downloadStatus, String url) {//url may represent Uri
//        updateStatusInFireBaseLocalData(momentModel, media, downloadStatus, url);
        localDataHelper.updateStatusInLocalData(momentModel, media, downloadStatus, url, getMomentType(momentModel) == MomentType.PUBLIC_MOMENT ? 2 : 1);
    }


    /**
     * represents highest index of media watched for each moment
     */
    private LinkedHashMap<MomentModel, Integer> peekIndexOfMediaWatchedMap = new LinkedHashMap<>();//will represent the current media being


    private void checkIfShouldDownloadNewMediaAdded(MomentModel momentModel, MomentModel.Media media) {
        final Integer previousWatchedIndex = peekIndexOfMediaWatchedMap.get(momentModel);
        if (previousWatchedIndex != null && previousWatchedIndex != -1) {
            final ArrayList<MomentModel.Media> list = arrayListLinkedHashMap.get(momentModel);
            if (list != null && list.size() > 0)
                for (int i = previousWatchedIndex; i < list.size(); i++) {
                    MomentModel.Media currentMedia = list.get(i);
                    if (mediasCurrentlyBeingDownloaded.contains(currentMedia.mediaId))
                        break;
                    if (list.indexOf(media) - previousWatchedIndex < LOT_SIZE) {
                        { //For real-time update of seen-to-unseen + downloading state
                            updateStatusForViews(momentModel);
                        }
                        startDownload(momentModel, media);
                        break;
                    }
                }
        }
    }

    /**
     * will be called whenever we switch to next media, in order to ensure that the downloads does't lack behind
     *
     * @param momentModel              being watched
     * @param indexOfMediaBeingWatched .-1 will represent just being tapped
     */
    public void notifySwitchToNextMedia(MomentModel momentModel, int indexOfMediaBeingWatched) {

        Integer recordedHighestWatchedMediaIdIndex = peekIndexOfMediaWatchedMap.get(momentModel);

        Log.d(TAG, "notifySwitchToNextMedia: index " + indexOfMediaBeingWatched);
        if (recordedHighestWatchedMediaIdIndex == null)
            recordedHighestWatchedMediaIdIndex = -1;

        if (indexOfMediaBeingWatched >= recordedHighestWatchedMediaIdIndex)
            peekIndexOfMediaWatchedMap.put(momentModel, indexOfMediaBeingWatched);

        final ArrayList<MomentModel.Media> list = arrayListLinkedHashMap.get(momentModel);
        int j = indexOfMediaBeingWatched + LOT_SIZE;
        if (j > list.size())
            j = list.size();

        if (indexOfMediaBeingWatched == -1)
            indexOfMediaBeingWatched = 0;

        for (int i = indexOfMediaBeingWatched; i < j; i++) {
            final MomentModel.Media media = list.get(i);

            if (mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                Log.d(TAG, " media " + media.mediaId + " is already downloading;hence breaking out ");
                break;
            }
            if (media.status == MEDIA_DOWNLOAD_NOT_STARTED || media.status == ERROR_DOWNLOADING_MEDIA) {
                startDownload(momentModel, media);
                break;
            }
        }
        updateStatusForViews(momentModel);
    }

    private void notifyDownloadCompleted(MomentModel momentModel, MomentModel.Media media, String currentMediaStoragePath) {
        int indexOfMediaForWhichDownloadIsCompleted = arrayListLinkedHashMap.get(momentModel).indexOf(media);
        int lastWatchedIndexOfMedia = peekIndexOfMediaWatchedMap.get(momentModel);//not nullable
        Log.d(TAG, "notifyDownloadCompleted " + indexOfMediaForWhichDownloadIsCompleted);

        final ArrayList<MomentModel.Media> list = arrayListLinkedHashMap.get(momentModel);
        for (int i = indexOfMediaForWhichDownloadIsCompleted; i < list.size(); i++) {
            final MomentModel.Media currentMedia = list.get(i);
            if (Math.abs(indexOfMediaForWhichDownloadIsCompleted - lastWatchedIndexOfMedia) > LOT_SIZE)
                break;
            Log.d(TAG, "falling short on medias...should download some stuff");
            if (mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                Log.d(TAG, " media " + media.mediaId + " is already downloading;hence breaking out ");
                break;
            }

            if (currentMedia.status == MEDIA_DOWNLOAD_NOT_STARTED || currentMedia.status == ERROR_DOWNLOADING_MEDIA) {
                startDownload(momentModel, currentMedia);
                break;
            }
        }
//        }
//        checkIfMinimumMediasPresentToOpenMoment(momentModel);
        updateStatusForViews(momentModel);
        playlistController.notifyMediaDownloaded(momentModel, media);
        updateLocalStatus(momentModel, media, MEDIA_DOWNLOAD_COMPLETE, currentMediaStoragePath);
        checkForWaitingMoments(momentModel);
    }

    //downloaded for each moment
    final int MINIMUM_MEDIA_TO_VIEW_MOMENT = 1;
    /**
     * All the moments which were tapped and were unable to open are recorded here;
     * the list is clear whenever there is scroll or  any of the moment becomes ready to view
     */
    private static ArrayList<String> waitingMoments = new ArrayList<>();

    public static void notifyScrollDetectedInDashboard() {
        Log.d(TAG, "notifyScrollDetectedInDashboard");
        if (waitingMoments != null)
            waitingMoments.clear();
    }

    /**
     * Here we will check if any of the moment is waiting for the media/data to be loaded. If its read, we play it!
     *
     * @param momentModel for which the media item just got downloaded
     */
    private void checkForWaitingMoments(MomentModel momentModel) {
        if (waitingMoments.size() == 0)
            return;
        if (waitingMoments.contains(momentModel.momentId)) {
            if (isMinimumMediasPresentToOpenMoment(momentModel.momentId)) {
//                onAroundYouDownloadListener.openWaitingMoments(momentModel.momentId);
                onOpenWaitingMoments(momentModel.momentId);
                waitingMoments.clear();
            }
        }
    }

    static String watchNowTappedMomentId;

    public static void notifyWatchNowTapped(String momentId) {
        watchNowTappedMomentId = momentId;
    }

    /**
     * @param momentId which was tapped
     * @return true if it is ready to open; false otherwise
     */
    public boolean notifyTapOnMoment(final String momentId) {
        //No moment id or no medias case
        if (getMomentModel(momentId) == null || getMomentModel(momentId).medias == null || getMomentModel(momentId).medias.size() == 0) {
            if (getMomentType(momentId) == null) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notifyTapOnMoment(momentId);
                    }
                }, 100);
            } else {
                if (getMomentType(momentId) == MomentType.PUBLIC_MOMENT && onPublicMomentDownloadListener != null)
                    onPublicMomentDownloadListener.onMomentStateChangedForViews(momentId, DOWNLOADING_MOMENT);
            }

            if (!waitingMoments.contains(momentId))
                waitingMoments.add(momentId);
            return false;
        }

        MomentModel momentModel = getMomentModel(momentId);
        Integer lastIndex = peekIndexOfMediaWatchedMap.get(momentModel); //Highest viewed media index

        if (lastIndex == null || lastIndex == -1) { //First time the app is opened, tapping should set the index to iterate from the next unseen item
            for (int i = 0; i < arrayListLinkedHashMap.get(momentModel).size(); i++) {
                final MomentModel.Media media = arrayListLinkedHashMap.get(momentModel).get(i);
                if (media.status == MEDIA_DOWNLOAD_NOT_STARTED || media.status == MEDIA_DOWNLOAD_COMPLETE || mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                    lastIndex = i - 1;
                    break;
                }
            }
        }

        notifySwitchToNextMedia(getMomentModel(momentId), (lastIndex != null && lastIndex >= 0) ? lastIndex : -1);
        updateStatusForViews(getMomentModel(momentId));

        boolean ready = isMinimumMediasPresentToOpenMoment(momentId);
        if (!ready) {
            if (!waitingMoments.contains(momentId))
                waitingMoments.add(momentId);
        } else
            waitingMoments.clear();
        //else everything Ok
        return ready;
    }

    private HashSet<String> mediasCurrentlyBeingDownloaded = new HashSet<>();
    private final int LOT_SIZE = 4;

    private void loadMediaPathFromStorage(MomentModel momentModel, DataSnapshot localDataSnap, MomentModel.Media media) {
        media.status = localDataSnap.child(DOWNLOAD_STATUS).getValue(Integer.class);
        media.url = localDataSnap.child(URL).getValue(String.class);
        // loaded only the concerned moment's media

        //checking if the media is present in other moments
        //note:-- if the media is seen for one item it will be marked as seen in other moments as of now
        //because we don't have the any mechanism as of now to persist the state for media w.r.t each moments

        for (Map.Entry<String, Map.Entry<MomentModel, MomentType>> outerEntry : streamsLinkedMap.entrySet()) {
            final Map.Entry<MomentModel, MomentType> value = outerEntry.getValue();
            final MomentModel entryMoment = value.getKey();
            if (entryMoment.momentId.equals(momentModel.momentId)) continue;
            if (entryMoment.medias.containsKey(media.mediaId)) {//adding to medias in other moments
                final MomentModel.Media media1 = entryMoment.medias.get(media.mediaId);
                media1.status = localDataSnap.child(DOWNLOAD_STATUS).getValue(Integer.class);
                media1.url = localDataSnap.child(URL).getValue(String.class);
            }
        }

    }

    private void loadMediaPathFromStorage(MomentModel momentModel, MediaDownloadDetails downloadDetails, MomentModel.Media media) {
        media.status = downloadDetails.getStatus();
        media.url = downloadDetails.getUrl();

        for (Map.Entry<String, Map.Entry<MomentModel, MomentType>> outerEntry : streamsLinkedMap.entrySet()) {
            final Map.Entry<MomentModel, MomentType> value = outerEntry.getValue();
            final MomentModel entryMoment = value.getKey();
            if (entryMoment.momentId.equals(momentModel.momentId)) continue;
            if (entryMoment.medias.containsKey(media.mediaId)) {//adding to medias in other moments
                final MomentModel.Media media1 = entryMoment.medias.get(media.mediaId);
                media1.status = downloadDetails.getStatus();
                media1.url = downloadDetails.getUrl();
            }
        }


    }

    public enum QueueStatus {
        DOWNLOADING, WAITING //to show loader in the views
    }

    private HashMap<MomentModel, QueueStatus> queueStatusHashMap = new HashMap<>();

    private void updateQueueStatusForMoment(MomentModel momentModel, QueueStatus queueStatus) {
        if (queueStatusHashMap.get(momentModel) == null || queueStatusHashMap.get(momentModel) != queueStatus) {
            queueStatusHashMap.put(momentModel, queueStatus);
        }
    }

    private void startDownload(final MomentModel momentModel, final MomentModel.Media media) {
        final String mediaId = media.mediaId;
        String mediaUrl = media.url;
        if (mediaId == null || mediaUrl == null) {
            Log.e(TAG, " wtf cannot start download without these; returning !");
            return;
        }
        int p = mediaUrl.lastIndexOf(".");
        String extension = mediaUrl.substring(p + 1);
        if (p == -1 || !extension.matches("\\w+")) {
            Log.e(TAG, " startDownload no extension found returning");
            return;
        }
        final String currentMediaStoragePath = nearbyMediaStoragePath + mediaId + "." + extension;
        Log.d(TAG, " currentMedia " + mediaId + "." + extension);
        File file = new File(currentMediaStoragePath);
        Log.d(TAG, " mediaUrl " + mediaUrl);
        updateQueueStatusForMoment(momentModel, QueueStatus.DOWNLOADING);

//        TransferObserver transferObserver;

//        String key;
//        try {
//            key = mediaUrl.split(AppLibrary.MediaHostBucket + "/")[1];
//            transferObserver = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting download
//        } catch (Exception e) { //Temporary to support shifting to new bucket
//            key = mediaUrl.split("pulse.resources/")[1];
//            transferObserver = MasterClass.getTransferUtility_US().download("pulse.resources", key, file); //Starting download
//        }

//        final int transferId = transferObserver.getId();
//
//        Log.d(TAG, " key " + key);
        mediasCurrentlyBeingDownloaded.add(mediaId);
        ThinDownloader.ThinTransferObserver thinTransferObserver = new ThinDownloader.ThinTransferObserver() {
            @Override
            public void onDownloadComplete() {
                newUpdateMediaDownloadProgress(momentModel, media, MEDIA_DOWNLOAD_COMPLETE, currentMediaStoragePath);
            }

            @Override
            public void onDownloadFailed() {
                newUpdateMediaDownloadProgress(momentModel, media, ERROR_DOWNLOADING_MEDIA, currentMediaStoragePath);

            }
        };
        getThinDownloader().downloadFile(MasterClass.getGlobalContext(), currentMediaStoragePath, mediaUrl, thinTransferObserver);

        //        Log.d(TAG, " downloadId " + transferId);
        //        transferObserver.setTransferListener(new TransferListener() {
        //            @Override
        //            public void onStateChanged(final int i, final TransferState transferState) {
        //
        //                Log.d(TAG, "onStateChanged -" + transferState.toString() + " for mediaId" + i);
        //                if (transferState.toString().equals("IN_PROGRESS")) {
        //                    Log.d(TAG, "MEDIA DOWNLOAD IN_PROGRESS for " + i + " id:-- " + mediaId);
        ////                    newUpdateMediaDownloadProgress(momentModel, media, MEDIA_DOWNLOADING, currentMediaStoragePath);
        //                } else if (transferState.toString().equals("COMPLETED")) {
        //                    Log.d(TAG, "MEDIA DOWNLOAD COMPLETED for mediaId" + i + " id:-- " + mediaId);
        //                } else {
        //                    MasterClass.getTransferUtility().cancel(transferId);
        //                    Log.d(TAG, " downloading failed for mediaId " + i);
        //                }
        //            }
        //
        //            @Override
        //            public void onProgressChanged(int i, long l, long l1) {
        //                Log.d(TAG, "onProgressChanged l= " + l + " l1= " + l1);
        //                if (l1 > 0)
        //                    downloadProgressMap.put(mediaId, (float) ((l / l1) * 100));
        //
        //                if (l == l1)
        //                    Log.d(TAG, " onProgressChanged i: " + i + " l " + l + " l1 " + l1);
        //            }
        //
        //            @Override
        //            public void onError(final int i, final Exception e) {
        //                e.printStackTrace();
        ////                updateQueueStatusForMoment(momentModel, QueueStatus.WAITING);
        //            }
        //        });
        updateStatusForViews(momentModel);

    }

    private static HashMap<String, Float> downloadProgressMap = new HashMap<>();
    private static HashMap<String, String> momentId_mediaProgressMap = new HashMap<>();

    public static Float getProgressForAMoment(String momentId) {
        String mediaId = momentId_mediaProgressMap.get(momentId);
        if (mediaId == null)
            return null;
        return downloadProgressMap.get(mediaId);

    }


    private void newUpdateMediaDownloadProgress(MomentModel momentModel, MomentModel.Media media, int mediaDownloadStatus, @Nullable String currentMediaStoragePath) {
        QueueStatus queueStatus = null;
        if (mediaDownloadStatus == ERROR_DOWNLOADING_MEDIA) {
            if (mediasCurrentlyBeingDownloaded.contains(media.mediaId)) return;

            if ((new File(media.url)).exists()) { //faulty call to onError
                newUpdateMediaDownloadProgress(momentModel, media, MEDIA_DOWNLOAD_COMPLETE, currentMediaStoragePath);
                return;
            }

            if (retryAttemptAllowed(media.mediaId)) {
                startDownload(momentModel, media);
                queueStatus = QueueStatus.DOWNLOADING;
            } else {
                media.status = ERROR_DOWNLOADING_MEDIA;
                mediasCurrentlyBeingDownloaded.remove(media.mediaId);
                queueStatus = QueueStatus.WAITING;
            }
        } else if (mediaDownloadStatus == MEDIA_DOWNLOAD_COMPLETE) {//media downloaded!!
            //first updating for the moment for which media is downloaded
            media.status = mediaDownloadStatus;
            media.url = currentMediaStoragePath;
            mediasCurrentlyBeingDownloaded.remove(media.mediaId);


            for (Map.Entry<String, Map.Entry<MomentModel, MomentType>> entry : streamsLinkedMap.entrySet()) {
                final Map.Entry<MomentModel, MomentType> value = entry.getValue();
                final MomentModel momentModel1 = value.getKey();
                final HashMap<String, MomentModel.Media> medias = momentModel1.medias;
                if (medias.containsKey(media.mediaId)) {//adding for other moments if it exists
                    for (Map.Entry<String, MomentModel.Media> stringMediaEntry : medias.entrySet()) {
                        if (stringMediaEntry.getKey().equals(media.mediaId)) {
                            stringMediaEntry.getValue().status = mediaDownloadStatus;
                            stringMediaEntry.getValue().url = currentMediaStoragePath;
//                            notifyDownloadCompleted(momentModel1, stringMediaEntry.getValue(), currentMediaStoragePath);
                            break;
                        }
                    }
                }
            }


            queueStatus = QueueStatus.WAITING;
            notifyDownloadCompleted(momentModel, media, currentMediaStoragePath);
        } else if (mediaDownloadStatus == MEDIA_DOWNLOADING) {//do nothing until download is in progress
            queueStatus = QueueStatus.DOWNLOADING;
        }
        updateQueueStatusForMoment(momentModel, queueStatus);
    }

    private HashMap<String, Integer> retryDownloadMap = new HashMap<>();
    final int RETRY_ATTEMPTS = 4;

    private boolean retryAttemptAllowed(String mediaId) {
        if (retryDownloadMap.get(mediaId) == null) {
            retryDownloadMap.put(mediaId, 1);
            return true;
        }
        int earlierRetryAttempts = retryDownloadMap.get(mediaId);
        if (earlierRetryAttempts >= this.RETRY_ATTEMPTS)
            return false;
        retryDownloadMap.put(mediaId, ++earlierRetryAttempts);
        return true;
    }

    private OnMomentDownloadListener onPublicMomentDownloadListener, onFriendMomentDownloadListener;

    public void setOnAroundYouDownloadListener(OnMomentDownloadListener onAroundYouDownloadListener) {
        this.onPublicMomentDownloadListener = onAroundYouDownloadListener;
    }

    public void setOnFriendMomentDownloadListener(OnMomentDownloadListener onAroundYouDownloadListener) {
        this.onFriendMomentDownloadListener = onAroundYouDownloadListener;
    }

    public void setOnFollowersMomentDownloadListener(OnMomentDownloadListener onFollowersMomentDownloadListener) {
        this.onFollowersMomentDownloadListener = onFollowersMomentDownloadListener;
    }

    public void updateStatusForAllMomentsOnRefresh() {
        if (fireBaseHelper != null && fireBaseHelper.getFirebaseHelperHandler() != null) {
            fireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                @Override
                public void run() {
                    for (Map.Entry<MomentModel, ArrayList<MomentModel.Media>> entry : arrayListLinkedHashMap.entrySet())
                        if (getMomentType(entry.getKey().momentId) == MomentType.PUBLIC_MOMENT)
                            updateStatusForViews(entry.getKey());
                }
            });
        }
    }

    private void updateStatusForViews(MomentModel momentModel) {
        if (getMomentType(momentModel.momentId) == MomentType.PUBLIC_MOMENT)
            updateStatusForPublicMoment(momentModel);
        else
            updateStatusForFriendsMoments(momentModel);
    }

    private void updateStatusForPublicMoment(MomentModel momentModel) {
        int downloadStatus = -11;//default
        final ArrayList<MomentModel.Media> list = arrayListLinkedHashMap.get(momentModel);
        for (int i = 0; i < list.size(); i++) {  //ToDo - Can be optimized by checking the view status from the last watched index
            final MomentModel.Media media = list.get(i);
            if (mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                downloadStatus = DOWNLOADING_MOMENT;
                momentId_mediaProgressMap.put(momentModel.momentId, media.mediaId);
                break;
            } else if (media.status == MEDIA_DOWNLOAD_COMPLETE) {
                downloadStatus = UNSEEN_MOMENT;
            } else if (media.status == MEDIA_DOWNLOAD_NOT_STARTED) {
                downloadStatus = UNSEEN_MOMENT;
                break;
            } else if (media.status == MEDIA_VIEWED) {
                if (i == list.size() - 1) {//last item
                    if (media.status == MEDIA_VIEWED)
                        downloadStatus = SEEN_MOMENT;
                } //else continue  ; not the last item
            }
        }

        if (downloadStatus == DOWNLOADING_MOMENT) {
            if (isMinimumMediasPresentToOpenMoment(momentModel.momentId)) {
                downloadStatus = UNSEEN_MOMENT;
            }
        }

        Log.d(TAG, "updateStatusForViews " + momentModel.name + " status " + getMomentStatusString(downloadStatus) + " peek " + peekIndexOfMediaWatchedMap.get(momentModel));
        if (downloadStatus != -11)
            onPublicMomentDownloadListener.onMomentStateChangedForViews(momentModel.momentId, downloadStatus);
        else Log.e(TAG, " unrecognized status");
    }


    private void updateStatusForFriendsMoments(MomentModel momentModel) {
        Log.d(TAG, "updateStatusForFriendsMoments " + momentModel.momentId);
        int downloadingIndex = -1;

        int downloadStatus = -11;//default
        final ArrayList<MomentModel.Media> list = arrayListLinkedHashMap.get(momentModel);
        for (int i = 0; i < list.size(); i++) {
            final MomentModel.Media media = list.get(i);
            if (mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                downloadStatus = DOWNLOADING_MOMENT;
                downloadingIndex = i;

                break;
            } else if (media.status == MEDIA_DOWNLOAD_COMPLETE) {
                downloadStatus = UNSEEN_MOMENT;
            } else if (media.status == MEDIA_DOWNLOAD_NOT_STARTED) {
                downloadStatus = UNSEEN_MOMENT;
                break;
            } else if (media.status == MEDIA_VIEWED) {
                if (i == list.size() - 1) {//last item
                    if (media.status == MEDIA_VIEWED)
                        downloadStatus = SEEN_MOMENT;
                }
            }
        }

//        if (downloadStatus == DOWNLOADING_MOMENT) {
//            final Integer peekWatchedMedia = peekIndexOfMediaWatchedMap.get(momentModel);
//            if (downloadingIndex - peekWatchedMedia > MINIMUM_MEDIA_TO_VIEW_MOMENT)
//                downloadStatus = UNSEEN_MOMENT;
//        }


        if (downloadStatus == DOWNLOADING_MOMENT) {
            if (isMinimumMediasPresentToOpenMoment(momentModel.momentId)) {
                downloadStatus = UNSEEN_MOMENT;
            }
        }
        if (getMomentType(momentModel.momentId) == MomentType.FRIEND_MOMENT)
            onFriendMomentDownloadListener.onMomentStateChangedForViews(momentModel.momentId, downloadStatus);
        else if (getMomentType(momentModel.momentId) == MomentType.FOLLOWER_MOMENT)
            onFollowersMomentDownloadListener.onMomentStateChangedForViews(momentModel.momentId, downloadStatus);
    }

    private void onOpenWaitingMoments(String momentId) {
//        if (momentIdsSet.get(momentId) == MomentType.PUBLIC_MOMENT)
        if (getMomentType(momentId) == MomentType.PUBLIC_MOMENT)
            onPublicMomentDownloadListener.openWaitingMoments(momentId);
        else if (getMomentType(momentId) == MomentType.FRIEND_MOMENT) {
            onFriendMomentDownloadListener.openWaitingMoments(momentId);
        } else if (getMomentType(momentId) == MomentType.FOLLOWER_MOMENT)
            onFollowersMomentDownloadListener.openWaitingMoments(momentId);
    }

    public static String getMomentStatusString(int status) {
        switch (status) {
            // Moment Seen status
            case UNSEEN_MOMENT:
                return "UNSEEN_MOMENT";
            case DOWNLOADING_MOMENT:
                return "DOWNLOADING_MOMENT";
            case READY_TO_VIEW_MOMENT:
                return "READY_TO_VIEW_MOMENT";
            case READY_AND_SEEN_MOMENT:
                return "READY_AND_SEEN_MOMENT";
            case SEEN_MOMENT:
                return "SEEN_MOMENT";
            case SEEN_BUT_DOWNLOADING:
                return "SEEN_BUT_DOWNLOADING";
            default:
                return null;
        }
    }

    public boolean isMinimumMediasPresentToOpenMoment(String momentId) {

        MomentModel momentModel = getMomentModel(momentId);
        int downloadedCount = 0;
        int viewedCount = 0;

        if (momentModel==null || arrayListLinkedHashMap==null || arrayListLinkedHashMap.get(momentModel)==null) return false;

        boolean isMinimumMediasPresent = false;
        for (int i = 0; i < arrayListLinkedHashMap.get(momentModel).size(); i++) {
            if (arrayListLinkedHashMap.get(momentModel).get(i).status == MEDIA_DOWNLOAD_COMPLETE) {
                ++downloadedCount;
            } else if (arrayListLinkedHashMap.get(momentModel).get(i).status == MEDIA_VIEWED) {
                ++viewedCount;
            }
        }


//        int index = peekIndexOfMediaWatchedMap.get(momentModel);

        if (viewedCount == arrayListLinkedHashMap.get(momentModel).size()) { //All items viewed
            isMinimumMediasPresent = true;
        } else {
            if (downloadedCount >= MINIMUM_MEDIA_TO_VIEW_MOMENT || downloadedCount == (arrayListLinkedHashMap.get(momentModel).size() - viewedCount)) {
                isMinimumMediasPresent = true; //All items downloaded or minimum number of items downloaded
            }
//            else if (downloadedCount==0 && index>-1 && index < (arrayListLinkedHashMap.get(momentModel).size()-1) && arrayListLinkedHashMap.get(momentModel).get(index+1).status==MEDIA_VIEWED){
//                isMinimumMediasPresent = true; //Weird case when next set of 5 medias have already been watched before
//            }
        }

        return isMinimumMediasPresent;
    }

    @SuppressWarnings("UnnecessaryContinue")
    private boolean isMinimumMediasPresentToOpenMomentFromCurrentIndex(MomentModel momentModel) {

        int indexFromWhichPlaylistStarts = -1;
        for (int i = 0; i < arrayListLinkedHashMap.get(momentModel).size(); i++) {
            final MomentModel.Media media = arrayListLinkedHashMap.get(momentModel).get(i);
            if (media.status == MEDIA_VIEWED) {
                //Viewed item skipped
                continue;
            } else if (media.status == MEDIA_DOWNLOAD_NOT_STARTED || media.status == MEDIA_DOWNLOAD_COMPLETE || mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                indexFromWhichPlaylistStarts = i;
                break;
            }
        }

        if (indexFromWhichPlaylistStarts == -1)//default; every item viewed
            return true;

        int j = indexFromWhichPlaylistStarts + MINIMUM_MEDIA_TO_VIEW_MOMENT;
        if (j > arrayListLinkedHashMap.get(momentModel).size())
            j = arrayListLinkedHashMap.get(momentModel).size();
        boolean present = true;
        for (int i = 0; i < j; i++) {
            final MomentModel.Media media = arrayListLinkedHashMap.get(momentModel).get(i);
            if (media.status == MEDIA_DOWNLOAD_NOT_STARTED && !mediasCurrentlyBeingDownloaded.contains(media.mediaId)) {
                present = false;
//                if (i == 0) {
                startDownload(momentModel, media);
                break;
//                }
            }
        }
        return present;
    }

    public interface OnMomentDownloadListener {
        void onMomentStateChangedForViews(String momentId, int newState);

        void openWaitingMoments(String momentId);

        void onLastMediaLoaded(MomentModel.Media mediaObj, MomentModel momentModel);

        void onThumbnailChanged(MomentModel.Media mediaObj, MomentModel momentModel);

        void nullifyMomentPreviousThumbnail(MomentModel momentModel);
    }

    HashSet<String> friendsMoments = new HashSet<>();

    public MomentModel getNearByMoment(String momentId) {
        return getMomentModel(momentId);
    }

    private PlaylistController playlistController;

    /**
     * @param momentId being opened
     * @return momentModel object & the array list associated with it
     */
    public Object[] getMomentsData(String momentId) {
        MomentModel momentModel = getNearByMoment(momentId);
        return new Object[]{momentModel, arrayListLinkedHashMap.get(momentModel)};
    }

    void notifyCompleteMomentWatched(String momentId) {
//        if (momentIdsSet.get(momentId) == MomentType.FRIEND_MOMENT)
        if (getMomentType(momentId) == MomentType.FRIEND_MOMENT)
            onFriendMomentDownloadListener.onMomentStateChangedForViews(momentId, SEEN_MOMENT);
        else if (getMomentType(momentId) == MomentType.FOLLOWER_MOMENT)
            onFollowersMomentDownloadListener.onMomentStateChangedForViews(momentId, SEEN_MOMENT);
        else if (getMomentType(momentId) == MomentType.PUBLIC_MOMENT)
            onPublicMomentDownloadListener.onMomentStateChangedForViews(momentId, SEEN_MOMENT);
    }

    private ThinDownloader thinDownloader;

    private ThinDownloader getThinDownloader() {
        if (thinDownloader == null)
            thinDownloader = new ThinDownloader(MasterClass.getGlobalContext());
        return thinDownloader;
    }
}