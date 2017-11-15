package com.pulseapp.android.models;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by deepankur on 9/7/16.
 */
public class LocalDataHelper implements FireBaseKEYIDS {

    private static HashMap<LocalDataChild, LocalDataHelper> dataHelperHashMap = new HashMap<>();
    private String TAG = getClass().getSimpleName();
    private static DataSnapshot contributedDetailsSnapshot;

    public void writeNewMediaDownloadDetailsToMemory(String mediaId, MediaModel mediaModel, int status) {
        if (this.getMediaDownloadStatus(mediaModel.mediaId) == null) {
            MediaDownloadDetails details = new MediaDownloadDetails();

            details.moments = mediaModel.addedTo.moments;
            details.createdAt = mediaModel.createdAt;
            details.status = status;
            details.url = mediaModel.url;
            synchronized (downloadStatusHashMap) {
                downloadStatusHashMap.put(mediaId, details);
            }
        }
    }

    public void setContributedDetailsSnapshot(DataSnapshot contributedDetailsSnapshot) {
        this.contributedDetailsSnapshot = contributedDetailsSnapshot;
    }

    public DataSnapshot getContributedDetailsSnapshot() {
        return contributedDetailsSnapshot;
    }

    public enum LocalDataChild {MEDIA_DOWNLOAD, LOCATION, STICKERS}

    public static LinkedHashMap<String, MediaDownloadDetails> downloadStatusHashMap;
    private FireBaseHelper fireBaseHelper;
    private boolean ready = false;

    public boolean isReady() {
        return ready;
    }

    public static LocalDataHelper getInstance(FireBaseHelper fireBaseHelper, LocalDataChild localDataChild) {
        if (dataHelperHashMap.containsKey(localDataChild))
            return dataHelperHashMap.get(localDataChild);
        LocalDataHelper helper = new LocalDataHelper(fireBaseHelper, localDataChild);
        dataHelperHashMap.put(localDataChild, helper);
        return helper;
    }

    private LocalDataHelper(FireBaseHelper fireBaseHelper, LocalDataChild localDataChild) {
        this.fireBaseHelper = fireBaseHelper;
        if (localDataChild == LocalDataChild.MEDIA_DOWNLOAD)
            initMediaDownload();
    }

    private void initMediaDownload() {
        if (downloadStatusHashMap == null)
            downloadStatusHashMap = new LinkedHashMap<>();
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), MEDIA_DOWNLOAD}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

//                fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), MEDIA_DOWNLOAD}).keepSynced(true);

                synchronized (downloadStatusHashMap) {
                    if (dataSnapshot != null) {
                        for (DataSnapshot childSnap : dataSnapshot.getChildren()) {
                            downloadStatusHashMap.put(childSnap.getKey(), childSnap.getValue(MediaDownloadDetails.class));
                        }
                        //clear data here
                    }
                }

                ready = true;
                Log.d(TAG, "onLocal data ready");
                if (localDataReadyListener != null)
                    localDataReadyListener.onLocalDataReady();

                fireBaseHelper.startCleaningDataService();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    /**
     * @param momentModel
     * @param media
     * @param mediaDownloadStatus
     * @param storagePath
     * @param momentType          whether public stream  {@link #MY_MOMENT } or friends/followers Stream {@link #CUSTOM_MOMENT }
     */
    @SuppressWarnings("JavaDoc")
    public void updateStatusInLocalData(@Nullable MomentModel momentModel, final MomentModel.Media media, final int mediaDownloadStatus, @Nullable String storagePath, int momentType) {
        final DatabaseReference newFireBase = fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), MEDIA_DOWNLOAD});
        final boolean writingFirstTime = updateInMemory(momentModel, media, mediaDownloadStatus, storagePath, momentType, newFireBase);

        if (momentModel != null)
            newFireBase.child(media.mediaId).child(CONTRIBUTED_MOMENTS).child(momentModel.momentId).setValue(momentType);
        if (storagePath != null && !storagePath.contains("https:"))
            newFireBase.child(media.mediaId).child(URL).setValue(storagePath);
        newFireBase.child(media.mediaId).child(CREATED_AT).setValue(media.createdAt);
        if (writingFirstTime) {
            newFireBase.child(media.mediaId).child(SOURCE).setValue(ANDROID);
            newFireBase.child(media.mediaId).child(DOWNLOAD_STATUS).setValue(mediaDownloadStatus);
        }
    }

    /**
     * We can put this check to write created at time if the item does,t exist in memory that mea
     *
     * @param mediaId to check
     * @return
     */
    private boolean existsInMemory(String mediaId) {
        return downloadStatusHashMap != null && downloadStatusHashMap.containsKey(mediaId);
    }

    /**
     * @return not present earlier in memory
     * true, if writing for the first time
     * (implying that data will be written first time and hence created at should be added)
     */
    private boolean updateInMemory(@Nullable MomentModel momentModel, MomentModel.Media media, int mediaDownloadStatus, @Nullable String storagePath, int momentType, DatabaseReference newFireBase) {

//        boolean isWrittenFirstTime;
        Integer previousStatus;

        if (downloadStatusHashMap.get(media.mediaId) == null) {
            MediaDownloadDetails downloadStatus = new MediaDownloadDetails();
            downloadStatus.createdAt = media.createdAt;
            downloadStatus.status = mediaDownloadStatus;
            downloadStatus.url = storagePath;

            if (momentModel != null) {
                HashMap<String, Integer> map = new HashMap<>();
                map.put(momentModel.momentId, momentType);
                downloadStatus.moments = map;
            }
            synchronized (downloadStatusHashMap) {
                downloadStatusHashMap.put(media.mediaId, downloadStatus);
            }
            return true;
        } else {
            previousStatus = downloadStatusHashMap.get(media.mediaId).status;

            if (mediaDownloadStatus > previousStatus) {//current in-app status is > db status
                downloadStatusHashMap.get(media.mediaId).status = mediaDownloadStatus;
                if (mediaDownloadStatus != ERROR_DOWNLOADING_MEDIA && mediaDownloadStatus != MEDIA_DOWNLOADING)
                    newFireBase.child(media.mediaId).child(DOWNLOAD_STATUS).setValue(mediaDownloadStatus);
            }
            if (momentModel != null) {
                HashMap<String, Integer> moments = downloadStatusHashMap.get(media.mediaId).moments;
                if (moments == null) moments = new HashMap<>();
                moments.put(momentModel.momentId, momentType);
            }
            if (storagePath != null) {
                downloadStatusHashMap.get(media.mediaId).url = storagePath;
            }
            return false;
        }
    }

    public MediaDownloadDetails getMediaDownloadStatus(String mediaId) {
        return downloadStatusHashMap.get(mediaId);
    }

    public void removeMediaIfExists(String mediaId) {
        if (downloadStatusHashMap != null && downloadStatusHashMap.containsKey(mediaId))
            downloadStatusHashMap.remove(mediaId);
    }

    private LocalDataReadyListener localDataReadyListener;

    public void setLocalDataReadyListener(LocalDataReadyListener localDataReadyListener) {
        this.localDataReadyListener = localDataReadyListener;
    }

    public LocalDataReadyListener getLocalDataReadyListener() {
        return localDataReadyListener;
    }

    public interface LocalDataReadyListener {
        void onLocalDataReady();
    }


}
