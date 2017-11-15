package com.pulseapp.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.HomeMomentAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by deepankur on 8/30/16.
 */

public class FriendsMomentsFragment extends BaseFragment {

    private String TAG = getClass().getSimpleName();
    private ViewControlsCallback viewControlsCallback;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, getCurrentMethodName());
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback)getParentFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getCurrentMethodName());
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState!=null) return;

        Log.d(TAG, getCurrentMethodName());
        loadMoments();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, getCurrentMethodName());
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, getCurrentMethodName());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, getCurrentMethodName());
    }

    /**
     * For updating the time shown beneath each moment every minute
     */
    private boolean isDestroyed = false;

    void updateTimeForViews() {
        MomentHolder[] holders = new MomentHolder[]{unseenFriendsHolder, unseenFollowersHolder, seenFriendsHolder, seenFollowersHolder};
        for (MomentHolder holder : holders) {
            if (holder != null && holder.momentList != null) {
                for (int j = 0; j < holder.momentList.size(); j++) {
                    final HomeMomentViewModel momentViewModel = holder.momentList.get(j);
                    momentViewModel.updatedAtText = AppLibrary.timeAccCurrentTime(momentViewModel.updatedAt);
                }
                holder.forceSortAllDataSet();
            }
        }


        int unseenCount = 0;
        MomentHolder[] holderset = new MomentHolder[]{unseenFriendsHolder, unseenFollowersHolder};
        for (MomentHolder holder : holderset) {
            if (holder != null && holder.momentList != null) {
                for (HomeMomentViewModel model : holder.momentList) {
                    if (model.momentStatus == UNSEEN_MOMENT) {
                        unseenCount++;
                    }
                }
            }
        }

        if (getActivity() != null)
            ((CameraActivity)getActivity()).onUnSeenItemsChanges(unseenCount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, getCurrentMethodName());
        isDestroyed = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, getCurrentMethodName());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, getCurrentMethodName());
    }


    /**
     * Both Seen and Unseen will be represented in this list;only when the data is fetched will have their pointers added and removed
     * Keeping static as list is already dynamic hence need not be refreshed on press of home/back button
     */
    private static ArrayList<HomeMomentViewModel> momentViewModelsList;

    public static String getMomentNameById(String momentId) {
        if (momentViewModelsList == null) return null;
        for (HomeMomentViewModel model : momentViewModelsList) {
            if (model.momentId.equals(momentId))
                return model.name;
        }
        return null;
    }

    private void loadMoments() {
        if (momentViewModelsList == null)
            momentViewModelsList = new ArrayList<>();

        // friends moment
        mFireBaseHelper.getNewFireBase(ANCHOR_USERS, new String[]{myUserId, MOMENT_ROOM}).orderByChild(UPDATED_AT).startAt(mFireBaseHelper.getMaximumMediaDuration()).addChildEventListener(friendsMomentRoomListener);
        getDownloader().setOnFriendMomentDownloadListener(friendsdownloadListener);

        loadFollowers();
    }

    private void loadFollowers(){
        // followers moment
        mFireBaseHelper.getNewFireBase(FOLLOWED_ROOM, new String[]{myUserId}).orderByChild(UPDATED_AT).startAt(mFireBaseHelper.getMaximumMediaDuration()).addChildEventListener(followersMomentRoomListener);
        getDownloader().setOnFollowersMomentDownloadListener(followersdownloadListener);
    }

    void updateMomentStatusInArrayList(String momentId, int newStatus) {
        MomentHolder[] holders = new MomentHolder[]{unseenFriendsHolder, unseenFollowersHolder, seenFriendsHolder, seenFollowersHolder};

        outer:
        for (MomentHolder holder : holders) {
            if (holder != null && holder.momentList != null) {
                for (HomeMomentViewModel model : holder.momentList) {
                    if (model.momentId.equals(momentId)) {
                        model.momentStatus = newStatus;
                        break outer;
                    }
                }
            }
        }

        int unseenCount = 0;
        MomentHolder[] holderset = new MomentHolder[]{unseenFriendsHolder, unseenFollowersHolder};
        for (MomentHolder holder : holderset) {
            if (holder != null && holder.momentList != null) {
                for (HomeMomentViewModel model : holder.momentList) {
                    if (model.momentStatus == UNSEEN_MOMENT) {
                        unseenCount++;
                    }
                }
            }
        }

        if (getActivity() != null)
            ((CameraActivity)getActivity()).onUnSeenItemsChanges(unseenCount);
    }

    private DynamicDownloader.OnMomentDownloadListener friendsdownloadListener = new DynamicDownloader.OnMomentDownloadListener() {
        @Override
        public void onMomentStateChangedForViews(final String momentId, final int newState) {
            if (getActivity() == null)
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int oldState = -111;
                    for (HomeMomentViewModel loopingModel : momentViewModelsList) {
                        if (loopingModel.momentId.equals(momentId)) {
                            oldState = loopingModel.momentStatus;
                            loopingModel.momentStatus = newState;
                            updateMomentStatusInArrayList(momentId, newState);
                        }
                    }
                    switch (newState) {
                        case UNSEEN_MOMENT:
                            if (seenFriendsHolder.contains(momentId))
                                seenFriendsHolder.remove(momentId);
                            if (!unseenFriendsHolder.contains(momentId)) {
                                unseenFriendsHolder.add(momentId);
                            } else {
                                unseenFriendsHolder.refreshMomentState(momentId, oldState);
                            }
                            break;
                        case DOWNLOADING_MOMENT:
                            if (unseenFriendsHolder.contains(momentId))
                                unseenFriendsHolder.refreshMomentState(momentId, oldState);
                            else if (seenFriendsHolder.contains(momentId))
                                seenFriendsHolder.refreshMomentState(momentId, oldState);
                            else {//neither seen nor unseen list has the moment
                                Log.e(TAG, " moment " + momentId + " having downloaded status DOWNLOADING_MOMENT not found in either list");
                            }
                            break;
                        case SEEN_MOMENT:
                            if (!unseenFriendsHolder.contains(momentId) && !seenFriendsHolder.contains(momentId)) {
                                seenFriendsHolder.add(momentId);
                            } else if (unseenFriendsHolder.contains(momentId)) {
                                unseenFriendsHolder.refreshMomentState(momentId, oldState);
                            } else if (seenFriendsHolder.contains(momentId)) {
                                seenFriendsHolder.refreshMomentState(momentId, oldState);
                            }
                            break;
                        case READY_TO_VIEW_MOMENT:
                            break;
                        case READY_AND_SEEN_MOMENT:
                            break;
                    }
                    refreshLayout();
                }
            });
        }

        @Override
        public void openWaitingMoments(final String momentId) {
            if (getActivity() == null)
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((CameraActivity) getActivity()).loadViewPublicMomentFragment(momentId, READY_TO_VIEW_MOMENT, "friend");
                }
            });
        }

        @Override
        public void onLastMediaLoaded(final MomentModel.Media mediaObj, final MomentModel momentModel) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FriendsMomentsFragment.MomentHolder momentHolder = null;
                    final String momentId = momentModel.momentId;
                    if (AppLibrary.checkStringObject(mediaObj.thumbnail) == null) {
                        for (final HomeMomentViewModel model : momentViewModelsList) {
                            if (model.momentId != null && model.momentId.equals(momentId)) {
                                model.updatedAt = mediaObj.createdAt;
                                model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                model.lastMediaId = mediaObj.mediaId;
                                model.showDisplayPicture = true;
                                if (unseenFriendsHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    momentHolder = unseenFriendsHolder;
                                    unseenFriendsHolder.forceRefreshMomentState(momentId);
                                } else if (seenFriendsHolder.contains(momentId)) {
                                    momentHolder = seenFriendsHolder;
                                    seenFriendsHolder.forceRefreshMomentState(momentId);
                                }
                                break;
                            }
                        }
                    } else {
                        for (final HomeMomentViewModel model : momentViewModelsList) {
                            if (model.momentId != null && model.momentId.equals(momentId)) {
                                model.updatedAt = mediaObj.createdAt;
                                model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                model.lastMediaId = mediaObj.mediaId;
                                model.thumbnailUrl = mediaObj.thumbnail;
                                model.showDisplayPicture = true;
                                AppLibrary.log_d(TAG, "Got the last media thumbnail for media Id -" + mediaObj.mediaId + ", momentId -" + momentId);
                                if (unseenFriendsHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    momentHolder = unseenFriendsHolder;
                                    unseenFriendsHolder.forceRefreshMomentState(momentId);
                                } else if (seenFriendsHolder.contains(momentId)) {
                                    momentHolder = seenFriendsHolder;
                                    seenFriendsHolder.forceRefreshMomentState(momentId);
                                }
                                break;
                            }
                        }
                    }

                    validateMomentsForSort(momentHolder);
                }
            });
        }

        @Override
        public void onThumbnailChanged(final MomentModel.Media mediaObj,final MomentModel momentModel) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final String momentId = momentModel.momentId;
                    for (final HomeMomentViewModel model : momentViewModelsList) {
                        if (model.momentId != null && model.momentId.equals(momentId)) {
                            if ((model.lastMediaId == null || model.lastMediaId.equals(mediaObj.mediaId)) && (model.thumbnailUrl == null || !model.thumbnailUrl.equals(mediaObj.thumbnail))) {
                                model.thumbnailUrl = mediaObj.thumbnail;
                                model.showDisplayPicture = true;
                                AppLibrary.log_d(TAG, "onThumbnailChanged, thumbnail -" + mediaObj.thumbnail);
                                if (unseenFriendsHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    unseenFriendsHolder.forceRefreshMomentState(momentId);
                                } else if (seenFriendsHolder.contains(momentId)) {
                                    seenFriendsHolder.forceRefreshMomentState(momentId);
                                }
                            }
                            break;
                        }
                    }
                }
            });
        }

        @Override
        public void nullifyMomentPreviousThumbnail(MomentModel momentModel) {
            String momentId = momentModel.momentId;
            for (HomeMomentViewModel model : momentViewModelsList) {
                if (model.momentId != null && model.momentId.equals(momentId)) {
                    model.thumbnailUrl = null;
                    model.lastMediaId = null;
                    break;
                }
            }
        }
    };

    private void validateMomentsForSort(FriendsMomentsFragment.MomentHolder momentHolder) {
        if (momentHolder == null || momentHolder.momentList == null || momentHolder.momentList.size() == 0)
            return;
        List<HomeMomentViewModel> momentList = momentHolder.momentList;
        boolean shuffle = true;
        for (HomeMomentViewModel model : momentList) {
            if (!model.showDisplayPicture) {
                shuffle = false;
                break;
            }
        }

        if (shuffle)
            sortMoments(momentHolder);
    }

    private void sortMoments(MomentHolder momentHolder){
        momentHolder.forceSortAllDataSet();
    }

    private DynamicDownloader.OnMomentDownloadListener followersdownloadListener = new DynamicDownloader.OnMomentDownloadListener() {
        @Override
        public void onMomentStateChangedForViews(final String momentId, final int newState) {
            if (getActivity() == null)
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int oldState = -111;
                    for (HomeMomentViewModel loopingModel : momentViewModelsList) {
                        if (loopingModel.momentId.equals(momentId)) {
                            oldState = loopingModel.momentStatus;
                            loopingModel.momentStatus = newState;
                            updateMomentStatusInArrayList(momentId, newState);
                        }
                    }
                    switch (newState) {
                        case UNSEEN_MOMENT:
                            if (seenFollowersHolder.contains(momentId))
                                seenFollowersHolder.remove(momentId);
                            if (!unseenFollowersHolder.contains(momentId)) {
                                unseenFollowersHolder.add(momentId);
                            } else {
                                unseenFollowersHolder.refreshMomentState(momentId, oldState);
                            }
                            break;
                        case DOWNLOADING_MOMENT:
                            if (unseenFollowersHolder.contains(momentId))
                                unseenFollowersHolder.refreshMomentState(momentId, oldState);
                            else if (seenFollowersHolder.contains(momentId))
                                seenFollowersHolder.refreshMomentState(momentId, oldState);
                            else {//neither seen nor unseen list has the moment
                                Log.e(TAG, " moment " + momentId + " having downloaded status DOWNLOADING_MOMENT not found in either list");
                            }
                            break;
                        case SEEN_MOMENT:
                            if (!unseenFollowersHolder.contains(momentId) && !seenFollowersHolder.contains(momentId)) {
                                seenFollowersHolder.add(momentId);
                            } else if (unseenFollowersHolder.contains(momentId)) {
                                unseenFollowersHolder.refreshMomentState(momentId, oldState);
                            } else if (seenFollowersHolder.contains(momentId)) {
                                seenFollowersHolder.refreshMomentState(momentId, oldState);
                            }
                            break;
                        case READY_TO_VIEW_MOMENT:
                            break;
                        case READY_AND_SEEN_MOMENT:
                            break;
                    }
                    refreshLayout();
                }
            });
        }

        @Override
        public void openWaitingMoments(final String momentId) {
            if (getActivity() == null)
                return;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((CameraActivity) getActivity()).loadViewPublicMomentFragment(momentId, READY_TO_VIEW_MOMENT, "follower");
                }
            });
        }

        @Override
        public void onLastMediaLoaded(final MomentModel.Media mediaObj, final MomentModel momentModel) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FriendsMomentsFragment.MomentHolder momentHolder = null;
                    final String momentId = momentModel.momentId;
                    if (AppLibrary.checkStringObject(mediaObj.thumbnail) == null) {
                        for (final HomeMomentViewModel model : momentViewModelsList) {
                            if (model.momentId != null && model.momentId.equals(momentId)) {
                                model.updatedAt = mediaObj.createdAt;
                                model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                model.lastMediaId = mediaObj.mediaId;
                                model.showDisplayPicture = true;
                                if (unseenFollowersHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    momentHolder = unseenFollowersHolder;
                                    unseenFollowersHolder.forceRefreshMomentState(momentId);
                                } else if (seenFollowersHolder.contains(momentId)) {
                                    momentHolder = seenFollowersHolder;
                                    seenFollowersHolder.forceRefreshMomentState(momentId);
                                }
                                break;
                            }
                        }
                    } else {
                        for (final HomeMomentViewModel model : momentViewModelsList) {
                            if (model.momentId != null && model.momentId.equals(momentId)) {
                                model.updatedAt = mediaObj.createdAt;
                                model.updatedAtText = AppLibrary.timeAccCurrentTime(model.updatedAt);
                                model.lastMediaId = mediaObj.mediaId;
                                model.thumbnailUrl = mediaObj.thumbnail;
                                model.showDisplayPicture = true;
                                AppLibrary.log_d(TAG, "Got the last media thumbnail for media Id -" + mediaObj.mediaId + ", momentId -" + momentId);
                                if (unseenFollowersHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    momentHolder = unseenFollowersHolder;
                                    unseenFollowersHolder.forceRefreshMomentState(momentId);
                                } else if (seenFollowersHolder.contains(momentId)) {
                                    momentHolder = seenFollowersHolder;
                                    seenFollowersHolder.forceRefreshMomentState(momentId);
                                }
                                break;
                            }
                        }
                    }

                    validateMomentsForSort(momentHolder);
                }
            });
        }

        @Override
        public void onThumbnailChanged(final MomentModel.Media mediaObj,final MomentModel momentModel) {
            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final String momentId = momentModel.momentId;
                    for (final HomeMomentViewModel model : momentViewModelsList) {
                        if (model.momentId != null && model.momentId.equals(momentId)) {
                            if ((model.lastMediaId == null || model.lastMediaId.equals(mediaObj.mediaId)) && (model.thumbnailUrl == null || !model.thumbnailUrl.equals(mediaObj.thumbnail))) {
                                model.thumbnailUrl = mediaObj.thumbnail;
                                model.showDisplayPicture = true;
                                AppLibrary.log_d(TAG, "onThumbnailChanged, thumbnail -" + mediaObj.thumbnail);
                                if (unseenFollowersHolder.contains(momentId)) {
                                    AppLibrary.log_d(TAG, "Refreshing moment");
                                    unseenFollowersHolder.forceRefreshMomentState(momentId);
                                } else if (seenFollowersHolder.contains(momentId)) {
                                    seenFollowersHolder.forceRefreshMomentState(momentId);
                                }
                            }
                            break;
                        }
                    }
                }
            });
        }

        @Override
        public void nullifyMomentPreviousThumbnail(MomentModel momentModel) {
            String momentId = momentModel.momentId;
            for (HomeMomentViewModel model : momentViewModelsList) {
                if (model.momentId != null && model.momentId.equals(momentId)) {
                    model.thumbnailUrl = null;
                    model.lastMediaId = null;
                    break;
                }
            }
        }
    };

    boolean isExpiredMoment(DataSnapshot momentRoom) {
        long now = System.currentTimeMillis() - mFireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000;
        Long dtStart = (Long) momentRoom.child(UPDATED_AT).getValue();
        if (now > dtStart) {
            return true;
        }
        return false;
    }

    private ChildEventListener friendsMomentRoomListener = new ChildEventListener() {
        @Override
        public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
            AppLibrary.log_d(TAG, "Moment Room onChildAdded -" + dataSnapshot.getKey());
            if (mFireBaseHelper != null && mFireBaseHelper.getFirebaseHelperHandler() != null) {
                mFireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        boolean isExpired = isExpiredMoment(dataSnapshot);
                        if (!isExpired) {
                            final Long updatedAt = dataSnapshot.child(UPDATED_AT).getValue(Long.class);
                            if (dataSnapshot.hasChild(DETAILS) && dataSnapshot.child(DETAILS).child(MOMENT_ID).getValue() != null) {
                                UserModel.Rooms momentRoomValue = dataSnapshot.getValue(UserModel.Rooms.class);
                                final long offset = updatedAt + mFireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000 - System.currentTimeMillis();

                                final HomeMomentViewModel momentRoomView = new HomeMomentViewModel(dataSnapshot.getKey(),
                                        momentRoomValue.details.get(NAME), momentRoomValue.details.get(IMAGE_URL),
                                        AppLibrary.timeAccCurrentTime(updatedAt), offset,
                                        momentRoomValue.details.get(MOMENT_ID), false, FRIEND_ROOM, MEDIA_DOWNLOAD_NOT_STARTED, null);
                                momentRoomView.handle = momentRoomValue.details.get(HANDLE);
                                momentRoomView.userId = momentRoomValue.details.get(USER_ID);
                                momentRoomView.handle = momentRoomValue.details.get(HANDLE);
                                momentViewModelsList.add(momentRoomView);
                                downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(momentRoomView), DynamicDownloader.MomentType.FRIEND_MOMENT);
                            } else {
                                mFireBaseHelper.getNewFireBase(ANCHOR_ROOMS, new String[]{dataSnapshot.getKey()}).keepSynced(true);
                                mFireBaseHelper.getNewFireBase(ANCHOR_ROOMS, new String[]{dataSnapshot.getKey()}).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot members : dataSnapshot.child(MEMBERS).getChildren()) {
                                            if (members.getKey().equals(mFireBaseHelper.getMyUserId())) {
                                                continue;
                                            }

                                            for (HomeMomentViewModel model : momentViewModelsList) {
                                                if (model.roomId != null && model.roomId.equals(dataSnapshot.getKey())) {
                                                    Log.e(TAG, model.roomId + " queried more than once from fireBase; returning ");
                                                    return;
                                                }
                                            }

                                            RoomsModel.Members friend = members.getValue(RoomsModel.Members.class);
                                            friend.memberId = members.getKey();
                                            //getting the display stuff here

                                            final long offset = updatedAt + mFireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000 - System.currentTimeMillis();

                                            final HomeMomentViewModel momentRoomView = new HomeMomentViewModel(dataSnapshot.getKey(),
                                                    friend.name, friend.imageUrl,
                                                    AppLibrary.timeAccCurrentTime(updatedAt), offset,
                                                    friend.momentId, false, FRIEND_ROOM, MEDIA_DOWNLOAD_NOT_STARTED, null);
                                            momentRoomView.handle = friend.handle;
                                            momentRoomView.userId = friend.memberId;
                                            momentViewModelsList.add(momentRoomView);
                                            downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(momentRoomView), DynamicDownloader.MomentType.FRIEND_MOMENT);
                                            break;
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }

                                });
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onChildChanged(final DataSnapshot dataSnapshot, String s) {
            AppLibrary.log_d(TAG,"Moment Room onChildChanged -"+dataSnapshot.getKey());
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

    };

    public String getMomentIdFromUserId(String userId) {
        if (momentViewModelsList == null || userId == null)
            return null;
        for (int i = 0; i < momentViewModelsList.size(); i++) {
            HomeMomentViewModel momentViewModel = momentViewModelsList.get(i);
            if (momentViewModel.userId.equals(userId))
                return momentViewModel.momentId;
        }
        return null;
    }

    private ChildEventListener followersMomentRoomListener = new ChildEventListener() {
        @Override
        public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
            if (mFireBaseHelper != null && mFireBaseHelper.getFirebaseHelperHandler() != null) {
                mFireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        boolean isExpired = isExpiredMoment(dataSnapshot);
                        if (!isExpired) {
                            if (dataSnapshot.hasChild(UPDATED_AT) && dataSnapshot.hasChild(NAME) && dataSnapshot.hasChild(IMAGE_URL)
                                    && dataSnapshot.hasChild(MOMENT_ID) && dataSnapshot.hasChild(HANDLE)) {
                                final Long updatedAt = dataSnapshot.child(UPDATED_AT).getValue(Long.class);
                                final long offset = updatedAt + mFireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000 - System.currentTimeMillis();

                                final HomeMomentViewModel momentRoomView = new HomeMomentViewModel(null,
                                        (String) dataSnapshot.child(NAME).getValue(), (String) dataSnapshot.child(IMAGE_URL).getValue(),
                                        AppLibrary.timeAccCurrentTime(updatedAt), offset,
                                        (String) dataSnapshot.child(MOMENT_ID).getValue(), false, FRIEND_ROOM, MEDIA_DOWNLOAD_NOT_STARTED, null);
                                momentRoomView.handle = (String) dataSnapshot.child(HANDLE).getValue();
                                momentRoomView.userId = dataSnapshot.getKey();
                                momentViewModelsList.add(momentRoomView);
                                downloader.registerNewStreamForDownload(generateMomentModelFromHomeMomentModelView(momentRoomView), DynamicDownloader.MomentType.FOLLOWER_MOMENT);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onChildChanged(final DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            AppLibrary.log_d(TAG,"followed room removed");
            if (dataSnapshot.getValue() == null)
                return;

            Iterator<HomeMomentViewModel> momentIterator = momentViewModelsList.iterator();
            while (momentIterator.hasNext()) {
                HomeMomentViewModel homeMomentViewModel = momentIterator.next();
                if (homeMomentViewModel.userId != null && homeMomentViewModel.userId.equals(dataSnapshot.getKey())){
                    final DynamicDownloader downloader = DynamicDownloader.getInstance(context);
                    if (downloader != null && downloader.getStreamsLinkedMap() != null && downloader.getStreamsLinkedMap().containsKey(homeMomentViewModel.momentId)) {
                        downloader.getStreamsLinkedMap().remove(homeMomentViewModel.momentId);
                    }
                    momentIterator.remove();
                    if (unseenFollowersHolder.contains(homeMomentViewModel.momentId))
                        unseenFollowersHolder.remove(homeMomentViewModel.momentId);
                    else if (seenFollowersHolder.contains(homeMomentViewModel.momentId))
                        seenFollowersHolder.remove(homeMomentViewModel.momentId);
                    refreshLayout();
                    break;
                }
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };


    private MomentHolder unseenFriendsHolder, unseenFollowersHolder, seenFriendsHolder, seenFollowersHolder;
    private static ArrayList<HomeMomentViewModel> unseenFriendsList, unseenFollowersList, seenFriendsList, seenFollowersList;

    public static ArrayList<HomeMomentViewModel> getFriendsWithMomentsList() {
        if (unseenFriendsList == null && unseenFollowersList == null && seenFriendsList == null && seenFollowersList == null)
            return null;
        ArrayList<HomeMomentViewModel> list = new ArrayList<>();
        if (unseenFriendsList != null)
            list.addAll(unseenFriendsList);
        if (unseenFollowersList != null)
            list.addAll(unseenFollowersList);
        if (seenFriendsList != null)
            list.addAll(seenFriendsList);
        if (seenFollowersList != null)
            list.addAll(seenFollowersList);
        return list;
    }

    private ArrayList<HomeMomentViewModel> getInitialList(int View_TYPE) {

        if (View_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER && unseenFriendsList != null)
            return unseenFriendsList;
        else if (View_TYPE == SEEN_FRIEND_MOMENT_RECYCLER && seenFriendsList != null)
            return seenFriendsList;
        else if (View_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER && unseenFollowersList != null)
            return unseenFollowersList;
        else if (View_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER && seenFollowersList != null)
            return seenFollowersList;

        ArrayList<HomeMomentViewModel> list = new ArrayList<>();
        if (View_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER)
            unseenFriendsList = list;
        else if (View_TYPE == SEEN_FRIEND_MOMENT_RECYCLER)
            seenFriendsList = list;
        else if (View_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER)
            unseenFollowersList = list;
        else if (View_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER)
            seenFollowersList = list;
        return list;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends_moments, container, false);

        if (savedInstanceState!=null) return rootView;

        if (unseenFriendsHolder == null)
            unseenFriendsHolder = new MomentHolder(UNSEEN_FRIEND_MOMENT_RECYCLER, getInitialList(UNSEEN_FRIEND_MOMENT_RECYCLER), (RecyclerView) rootView.findViewById(R.id.unSeenFriendsRecyclerView), (TextView) rootView.findViewById(R.id.unSeenFriendsMomentHeader), null);

        if (seenFriendsHolder == null)
            seenFriendsHolder = new MomentHolder(SEEN_FRIEND_MOMENT_RECYCLER, getInitialList(SEEN_FRIEND_MOMENT_RECYCLER), (RecyclerView) rootView.findViewById(R.id.seenFriendsRecyclerView), (TextView) rootView.findViewById(R.id.seenFriendsMomentHeader), null);

        if (unseenFollowersHolder == null)
            unseenFollowersHolder = new MomentHolder(UNSEEN_FOLLOWER_MOMENT_RECYCLER, getInitialList(UNSEEN_FOLLOWER_MOMENT_RECYCLER), (RecyclerView) rootView.findViewById(R.id.unSeenFollowersRecyclerView), (TextView) rootView.findViewById(R.id.unSeenFollowersMomentHeader), null);

        if (seenFollowersHolder == null)
            seenFollowersHolder = new MomentHolder(SEEN_FOLLOWER_MOMENT_RECYCLER, getInitialList(SEEN_FOLLOWER_MOMENT_RECYCLER), (RecyclerView) rootView.findViewById(R.id.seenFollowersRecyclerView), (TextView) rootView.findViewById(R.id.seenFollowersMomentHeader), null);

        noMomentsLayout = rootView.findViewById(R.id.no_moments_layout);
        momentsPresentLayout = rootView.findViewById(R.id.momentsPresentLayout);
        seenMomentHeaderView = rootView.findViewById(R.id.seenMomentsHeaderLayout);

        View includedNoMomentView = rootView.findViewById(R.id.include_no_moment_layout);
        includedNoMomentView.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraActivity cameraActivity = (CameraActivity) getActivity();
                if (cameraActivity != null)
                    cameraActivity.requestAddFriendFragmentOpen();
            }
        });


        refreshLayout();
        return rootView;
    }


    private DynamicDownloader downloader;

    private DynamicDownloader getDownloader() {
        if (downloader == null)
            downloader = DynamicDownloader.getInstance(getActivity());
        return downloader;
    }

    public boolean notifyTapOnMoment(String momentId,int momentType) {
        if (momentType == SEEN_FRIEND_MOMENT_RECYCLER || momentType == UNSEEN_FRIEND_MOMENT_RECYCLER)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FRIEND_STREAM_CLICK,AnalyticsEvents.STREAM_ID,momentId);
        else if (momentType == SEEN_FOLLOWER_MOMENT_RECYCLER || momentType == UNSEEN_FOLLOWER_MOMENT_RECYCLER)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FOLLOWER_STREAM_CLICK,AnalyticsEvents.STREAM_ID,momentId);

        final boolean ready = downloader.notifyTapOnMoment(momentId);
        if (ready) {
            if (momentType == SEEN_FRIEND_MOMENT_RECYCLER || momentType == UNSEEN_FRIEND_MOMENT_RECYCLER)
                ((CameraActivity) getActivity()).loadViewPublicMomentFragment(momentId, READY_TO_VIEW_MOMENT, "friend");
            else if (momentType == SEEN_FOLLOWER_MOMENT_RECYCLER || momentType == UNSEEN_FOLLOWER_MOMENT_RECYCLER)
                ((CameraActivity) getActivity()).loadViewPublicMomentFragment(momentId, READY_TO_VIEW_MOMENT, "follower");
        }
        return ready;
    }

    private void onSingleTapOnMoment(HomeMomentViewModel model) {
        notifyTapOnMoment(model.momentId,model.momentType);
    }


    private View noMomentsLayout, momentsPresentLayout, seenMomentHeaderView;

    /**
     * to control the layout saying no friends add new friends
     */
    public void refreshLayout() {
        if (getCurrentMomentCount(UNSEEN_FRIEND_MOMENT_RECYCLER) == 0 && getCurrentMomentCount(SEEN_FRIEND_MOMENT_RECYCLER) == 0
                && getCurrentMomentCount(UNSEEN_FOLLOWER_MOMENT_RECYCLER) == 0 && getCurrentMomentCount(SEEN_FOLLOWER_MOMENT_RECYCLER) == 0) {
            noMomentsLayout.setVisibility(View.VISIBLE);
            momentsPresentLayout.setVisibility(View.GONE);
        } else {
            noMomentsLayout.setVisibility(View.GONE);
            momentsPresentLayout.setVisibility(View.VISIBLE);

            if (getCurrentMomentCount(UNSEEN_FRIEND_MOMENT_RECYCLER) == 0 && getCurrentMomentCount(UNSEEN_FOLLOWER_MOMENT_RECYCLER) == 0) {
                // hide recent updates and shift settings icon to SEEN SECTION
                viewControlsCallback.onHideRecentUpdateHeader();

                unseenFriendsHolder.header.setVisibility(View.GONE);
                unseenFollowersHolder.header.setVisibility(View.GONE);
            } else {
                viewControlsCallback.onShowRecentUpdatesHeader();

                if (getCurrentMomentCount(UNSEEN_FRIEND_MOMENT_RECYCLER) == 0)
                    unseenFriendsHolder.header.setVisibility(View.GONE);
                else
                    unseenFriendsHolder.header.setVisibility(View.VISIBLE);

                if (getCurrentMomentCount(UNSEEN_FOLLOWER_MOMENT_RECYCLER) == 0)
                    unseenFollowersHolder.header.setVisibility(View.GONE);
                else
                    unseenFollowersHolder.header.setVisibility(View.VISIBLE);
            }

            if (getCurrentMomentCount(SEEN_FRIEND_MOMENT_RECYCLER) == 0 && getCurrentMomentCount(SEEN_FOLLOWER_MOMENT_RECYCLER) == 0) {
                // hide seen header
                seenMomentHeaderView.setVisibility(View.GONE);
                seenFriendsHolder.header.setVisibility(View.GONE);
                seenFollowersHolder.header.setVisibility(View.GONE);
            } else {
                seenMomentHeaderView.setVisibility(View.VISIBLE);
                if (getCurrentMomentCount(SEEN_FRIEND_MOMENT_RECYCLER) == 0)
                    seenFriendsHolder.header.setVisibility(View.GONE);
                else
                    seenFriendsHolder.header.setVisibility(View.VISIBLE);

                if (getCurrentMomentCount(SEEN_FOLLOWER_MOMENT_RECYCLER) == 0)
                    seenFollowersHolder.header.setVisibility(View.GONE);
                else
                    seenFollowersHolder.header.setVisibility(View.VISIBLE);
            }
        }
    }

    public void refreshSeenUnseenOnCameraSwipe() {
        if (unseenFriendsHolder != null && unseenFriendsHolder.momentList != null) {
            ArrayList<String> removedModel = new ArrayList<>();
            for (HomeMomentViewModel model : unseenFriendsHolder.momentList) {
                if (model.momentStatus == SEEN_MOMENT) {
                    model.refreshExpiredAngle();
                    removedModel.add(model.momentId);
                }
            }
            for (String s : removedModel) {
                unseenFriendsHolder.remove(s);
                seenFriendsHolder.add(s);
            }
        }
        if (unseenFollowersHolder != null && unseenFollowersHolder.momentList != null) {
            ArrayList<String> removedModel = new ArrayList<>();
            for (HomeMomentViewModel model : unseenFollowersHolder.momentList) {
                if (model.momentStatus == SEEN_MOMENT) {
                    model.refreshExpiredAngle();
                    removedModel.add(model.momentId);
                }
            }
            for (String s : removedModel) {
                unseenFollowersHolder.remove(s);
                seenFollowersHolder.add(s);
            }
        }
        refreshLayout();
        updateTimeForViews();
    }

    private int getCurrentMomentCount(int viewType) {
        MomentHolder holder = null;
        if (viewType == SEEN_FRIEND_MOMENT_RECYCLER)
            holder = seenFriendsHolder;
        else if (viewType == SEEN_FOLLOWER_MOMENT_RECYCLER)
            holder = seenFollowersHolder;
        else if (viewType == UNSEEN_FRIEND_MOMENT_RECYCLER)
            holder = unseenFriendsHolder;
        else if (viewType == UNSEEN_FOLLOWER_MOMENT_RECYCLER)
            holder = unseenFollowersHolder;

        return (holder == null || holder.momentList == null) ? 0 : holder.momentList.size();
    }

    public void updateFollowersList(boolean follow) {
        if (follow) {
            // followed user
            if (unseenFollowersHolder.momentList.size() == 0 || seenFollowersHolder.momentList.size() == 0) {
                loadFollowers();
            }
        } else {
            // unfollowed user
        }
    }

    public interface ViewControlsCallback {
//        void onLoadViewMomentFragment(String momentId, int momentStatus);
        void onHideRecentUpdateHeader();

        void onSettingsClicked();

        void onShowRecentUpdatesHeader();
    }

    private class MomentHolder {
        int VIEW_TYPE;
        ArrayList<HomeMomentViewModel> momentList;
        RecyclerView recyclerView;
        TextView header;
        HomeMomentAdapter adapter;

        MomentHolder(int VIEW_TYPE, ArrayList<HomeMomentViewModel> momentList, RecyclerView recyclerView, TextView header, HomeMomentAdapter adapter) {
            this.VIEW_TYPE = VIEW_TYPE;
            this.recyclerView = recyclerView;
            this.header = header;
            this.adapter = adapter;
            this.momentList = momentList;
            initRecyclerView();
        }

        private boolean contains(String momentId) {
            if (momentList == null) return false;
            for (HomeMomentViewModel momentViewModel : momentList)
                if (momentViewModel.momentId.equals(momentId)) return true;
            return false;
        }

        private void add(String momentId) {
            if (this.contains(momentId))
                throw new RuntimeException("cannot add multiple momentsIds to same list");
            for (int i = 0; i < momentViewModelsList.size(); i++) {
                if (momentViewModelsList.get(i).momentId.equals(momentId)) {
                    momentViewModelsList.get(i).updatedAtText = AppLibrary.timeAccCurrentTime(momentViewModelsList.get(i).updatedAt);
                    this.momentList.add(momentViewModelsList.get(i));
                    momentViewModelsList.get(i).refreshExpiredAngle();
                    this.adapter.notifyItemInserted(momentList.size() - 1);
                    return;
                }
            }
            throw new RuntimeException("momentId not found in main list");
        }

        private void forceRefreshMomentState(String momentId) {
            for (int i = 0; i < this.momentList.size(); i++) {
                if (momentList.get(i).momentId.equals(momentId)) {
                    momentList.get(i).refreshExpiredAngle();
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
//            checkAndDoSorting(this.momentList, this.adapter);
//            Log.e(TAG, " cannot refresh positions for " + momentId);
        }

        private void forceSortAllDataSet() {
            checkAndDoSorting(this.momentList,this.adapter);
        }

        private void refreshMomentState(String momentId, int oldState) {
            for (int i = 0; i < this.momentList.size(); i++) {
                if (momentList.get(i).momentId.equals(momentId)) {
                    if (momentList.get(i).momentStatus != oldState) {
                        adapter.notifyItemChanged(i);
                        AppLibrary.log_d(TAG, "Notifying");
                    } else Log.d(TAG, " not notifiying");
                    break;
                }
            }
            Log.e(TAG, " cannot refresh positions for " + momentId);
        }

        private void remove(String momentId) {
            int index = -1;
            for (int i = 0; i < momentList.size(); i++) {
                if (momentList.get(i).momentId.equals(momentId)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                this.momentList.remove(index);
                adapter.notifyItemRemoved(index);
            } else
                throw new RuntimeException("unable to remove the moment id; as it doesn't exist");
        }

        private void initRecyclerView() {
            recyclerView.setNestedScrollingEnabled(false);
            int dps_16 = AppLibrary.convertDpToPixels(getActivity(), 16);
            if (VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER || VIEW_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER) {
                recyclerView.setPadding(dps_16, dps_16 / 2, 0, dps_16 / 2);
            }
            if (VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER || VIEW_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER) {
                recyclerView.setPadding(dps_16, 0, 0, dps_16 / 2);
            }

            recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), (VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER || VIEW_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER) ? 4 : 3));
            adapter = new HomeMomentAdapter(myUserId, getActivity(), momentList, VIEW_TYPE, new RecyclerViewClickInterface() {
                @Override
                public void onItemClick(int position, Object data) {

                    HomeMomentViewModel model = (HomeMomentViewModel) data;
                    if (model.clickType == HomeMomentViewModel.ClickType.SINGLE_TAP) {
                        onSingleTapOnMoment(model);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.DOUBLE_TAP) {
                        Log.d(TAG, " got double click on " + model.momentId);
                    } else if (model.clickType == HomeMomentViewModel.ClickType.LONG_PRESS) {
                    }
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void checkAndDoSorting(ArrayList<HomeMomentViewModel> list, HomeMomentAdapter adapter) {
        if (needSorting(list)) {
            getSortedList(list);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean needSorting(ArrayList<HomeMomentViewModel> list) {
        long currentTime = Long.MAX_VALUE;
        for (HomeMomentViewModel model : list) {
            if (model.updatedAt > currentTime) {
                Log.d(TAG, " sorting the list ");
                return true;
            }
            currentTime = model.updatedAt;
        }
        return false;
    }

    private ArrayList<HomeMomentViewModel> getSortedList(ArrayList<HomeMomentViewModel> rooms) {
        if (rooms != null) {
            Collections.sort(rooms, new Comparator<HomeMomentViewModel>() {
                @Override
                public int compare(HomeMomentViewModel ele1,
                                   HomeMomentViewModel ele2) {
                    return (int) (ele2.updatedAt - ele1.updatedAt);
                }
            });
        }
        return rooms;
    }

}