package com.pulseapp.android.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.ExoPlayer.EventLogger;
import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
//import com.pulseapp.android.downloader.NearbyMomentDownloader;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.OnScreenshotTakenListener;
import com.pulseapp.android.util.RoundedTransformation;
import com.pulseapp.android.util.SmartViewPage;
import com.pulseapp.android.util.ViewPageCloseCallback;
import com.pulseapp.android.util.ViewpageTimer;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by user on 5/12/2016.
 */
public class ViewMomentDetailsFragment extends Fragment implements FireBaseKEYIDS, View.OnClickListener, OnScreenshotTakenListener, EventLogger.OnPlayerStateChanged,ViewPageCloseCallback {

    private static final String TAG = "ViewMomentDetailsFragment";
    private ViewControlsCallback viewControlsCallback;
    private ListIterator<Map.Entry<String, MomentModel.Media>> mediaIterator;
    private View rootView;
    private MomentModel currentMomentModel;
    private MomentModel previousMomentModel;
    private ArrayList<Map.Entry<String, MomentModel.Media>> currentMediaList;
    private ImageView momentImageView;
    private ListIterator<Map.Entry<String, MomentModel>> momentIterator;
    private int currentMediaType;
    private int momentCounter = 0;
    private MomentModel.Media currentMediaModel;
    private MomentModel.Media previousMediaModel;
    private boolean isCurrentMediaPaused = false;
    private boolean isNearByMomentOpened = false;
    private boolean isFirstTime = true;
    private SmartViewPage viewPage;
    public ViewpageTimer viewpageTimer;
    private long currentTotalTime = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isNearByMomentOpened)
            MasterClass.snapshotDetector.clearMediaList();
    }

    public void setIsNearBy(boolean isNearByMomentOpened) {
        this.isNearByMomentOpened = isNearByMomentOpened;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.moment_details_fragment, container, false);
        initializeViewObjects(rootView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getActivity().getWindow();
            AppLibrary.goTrulyFullscreen(w);
        }

        return rootView;
    }

    private void initializeViewObjects(View rootView) {
        rootView.findViewById(R.id.closeButton).setOnClickListener(this);
        if (!isNearByMomentOpened)
            MasterClass.snapshotDetector.setListener(this);
        momentImageView = (ImageView) rootView.findViewById(R.id.momentImage);
        final GestureDetector gestureDetector = new GestureDetector(getActivity(), new CustomGestureListener());
        momentIterator = viewControlsCallback.getMomentListIterator();

        viewPage = ((SmartViewPage) rootView.findViewById(R.id.touch_frame));
        viewPage.setRawEndPoints(getActivity());
        viewPage.setListener(this);

        viewpageTimer = ((ViewpageTimer) rootView.findViewById(R.id.timer));
        viewpageTimer.setAngle(360);
        viewpageTimer.setEraserSize(0);

        viewControlsCallback.setGestureDetector(gestureDetector);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean touchResult = true;
                boolean result = gestureDetector.onTouchEvent(event);

                if (isCurrentMediaPaused && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    resumeCurrentMedia();
                }

                return touchResult || result;
            }
        });
        playNextMedia();
    }

    private void switchToNextMoment() {
        momentIterator = viewControlsCallback.getMomentListIterator();
        currentMomentModel = null;
        previousMomentModel = null;
        if (momentIterator.hasNext()) {
            currentMomentModel = momentIterator.next().getValue();
            currentMediaList = new ArrayList<>(currentMomentModel.medias.entrySet());

            currentTotalTime = 0;
            if(viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT) {
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    if (currentMediaList.get(i).getValue().status==MEDIA_VIEWED) {
                        //Skip it
                    }
                    else
                        currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            } else {
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            }
            viewControlsCallback.setTotalTime(currentTotalTime);
            AppLibrary.log_d(TAG, "Currently total time is -" + currentTotalTime);

            mediaIterator = currentMediaList.listIterator();
            initializeMomentViewObjects();
            momentCounter++;
        } else {
            while (momentIterator.hasPrevious()) {
                currentMomentModel = momentIterator.previous().getValue();
            }
            currentMediaList = new ArrayList<>(currentMomentModel.medias.entrySet());

            currentTotalTime = 0;
            if (viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT){
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    if (currentMediaList.get(i).getValue().status==MEDIA_VIEWED) {
                        //Skip it
                    }
                    else
                        currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            } else {
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            }
            viewControlsCallback.setTotalTime(currentTotalTime);

            mediaIterator = currentMediaList.listIterator();
        }
    }

    private void initializeMomentViewObjects() {
//        for (Map.Entry<String, MomentModel.Media> entry : currentMomentModel.medias.entrySet()) {
//            Picasso.with(getActivity()).load(new File(entry.getValue().url)).fit().transform(new RoundedTransformation())
//                    .into((ImageView) rootView.findViewById(R.id.createrImage));
//            break;
//        }
        ((TextView) rootView.findViewById(R.id.createrName)).setText(currentMomentModel.name);
        AppLibrary.log_d(TAG, "Currently Watching Moment Id -" + currentMomentModel.momentId);
    }

    private void switchToPreviousMoment() {
        momentIterator = viewControlsCallback.getMomentListIterator();
        if (momentIterator.hasPrevious()) {
            currentMomentModel = momentIterator.previous().getValue();
            currentMediaList = new ArrayList<>(currentMomentModel.medias.entrySet());

            currentTotalTime = 0;
            if (viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT){
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    if (currentMediaList.get(i).getValue().status==MEDIA_VIEWED) {
                        //Skip it
                    }
                    else
                        currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            } else {
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            }
            viewControlsCallback.setTotalTime(currentTotalTime);

            mediaIterator = currentMediaList.listIterator();
        } else {
            // switch to last unseen moment in the list
            while (momentIterator.hasNext()) {
                currentMomentModel = momentIterator.next().getValue();
            }
            currentMediaList = new ArrayList<>(currentMomentModel.medias.entrySet());

            currentTotalTime = 0;
            if (viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT){
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    if (currentMediaList.get(i).getValue().status==MEDIA_VIEWED) {
                        //Skip it
                    }
                    else
                        currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            } else {
                for (int i = 0, d = currentMediaList.size(); i < d; i++) {
                    currentTotalTime += currentMediaList.get(i).getValue().duration;
                }
            }
            viewControlsCallback.setTotalTime(currentTotalTime);

            mediaIterator = currentMediaList.listIterator();
        }
        initializeMomentViewObjects();
    }

    private void pauseCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().pauseCurrentMedia();
            viewControlsCallback.stopAutoPlay();
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            viewControlsCallback.stopAutoPlay();
        }
        isCurrentMediaPaused = true;
    }

    private void resumeCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().resumeCurrentMedia();
            viewControlsCallback.resumeAutoPlay();
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            viewControlsCallback.resumeAutoPlay();
        }
        isCurrentMediaPaused = false;
    }

    @SuppressLint("LongLogTag")
    private String getNextMediaUrl() {
        String url = null;
        if (mediaIterator == null) {
            switchToNextMoment();
        }
        previousMediaModel = currentMediaModel;
        if (mediaIterator.hasNext()) {
            currentMediaModel = mediaIterator.next().getValue();

            String text = AppLibrary.timeAccCurrentTime(currentMediaModel.createdAt);
            if (!text.isEmpty() && !text.contains("Now"))
                text = text + " ago";
            ((TextView) rootView.findViewById(R.id.createdTime)).setText(text);

            if (viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT && currentMediaModel.status == MEDIA_VIEWED) {
                url = getNextMediaUrl();
            } else {
                if (previousMediaModel != null && currentMomentModel != null) {
                    if (isNearByMomentOpened) {
                        FireBaseHelper.getInstance(getActivity()).openNearbyMedia(currentMomentModel.momentId, previousMediaModel.mediaId,null,null,0,0);
                    } else
                        FireBaseHelper.getInstance(getActivity()).openMedia(currentMomentModel.momentId, previousMediaModel.mediaId, null, null, -1, FRIEND_ROOM);
                }
                url = currentMediaModel.url;
            }
        } else {
            if (viewControlsCallback.getMomentStatus() == READY_TO_VIEW_MOMENT && currentMediaModel.status == MEDIA_VIEWED) {

            } else {
                // update seen media to fire base for the last media item in a moment
                if (previousMediaModel != null && currentMomentModel != null) {
                    if (isNearByMomentOpened)
                        FireBaseHelper.getInstance(getActivity()).openNearbyMedia(currentMomentModel.momentId, previousMediaModel.mediaId,null,null,0,0);
                    else
                        FireBaseHelper.getInstance(getActivity()).openMedia(currentMomentModel.momentId, previousMediaModel.mediaId, null, null, -1, FRIEND_ROOM);
                }
            }

            if (momentIterator.hasNext()) {
                // last media in the current moment so transit to upcoming fragment
                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onPause();
                }
                FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getActivity());
                if (isNearByMomentOpened) {
//                    if (NearbyMomentDownloader.getNearByMoment(currentMomentModel.momentId) != null)
//                        NearbyMomentDownloader.getNearByMoment(currentMomentModel.momentId).momentStatus = SEEN_MOMENT;
                    if (DynamicDownloader.getInstance(getActivity()).getNearByMoment(currentMomentModel.momentId)!= null)
                        DynamicDownloader.getInstance(getActivity()).getNearByMoment(currentMomentModel.momentId).momentStatus = SEEN_MOMENT;
                    //todo proper testing

                } else {
                    fireBaseHelper.viewCompleteMoment(currentMomentModel.momentId,viewControlsCallback.getMomentStatus());
                }
                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onPlayerRelease();
                }
                currentMediaModel = null;
                viewControlsCallback.transitToUpcomingMoments(momentCounter);
            } else {
                FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getActivity());
                if (isNearByMomentOpened) {
//                    if (NearbyMomentDownloader.getNearByMoment(currentMomentModel.momentId) != null)
//                        NearbyMomentDownloader.getNearByMoment(currentMomentModel.momentId).momentStatus = SEEN_MOMENT;
                    if (DynamicDownloader.getInstance(getActivity()).getNearByMoment(currentMomentModel.momentId)!= null)
                        DynamicDownloader.getInstance(getActivity()).getNearByMoment(currentMomentModel.momentId).momentStatus = SEEN_MOMENT;

                    //todo proper testing
                } else {
                    fireBaseHelper.viewCompleteMoment(currentMomentModel.momentId,viewControlsCallback.getMomentStatus());
                }
                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onDestroy();
                }
                Log.d(TAG, " finishing activity on all media seen ");
                viewControlsCallback.onCloseMomentsFragment();
            }
        }
        return url;
    }


    public void playNextMedia() {
        String url = getNextMediaUrl();
        if (url != null) {
            if (previousMediaModel != null && AppLibrary.getMediaType(previousMediaModel.url) == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onPlayerRelease();
            }
            int mediaType = AppLibrary.getMediaType(url);
            if (mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                currentMediaType = AppLibrary.MEDIA_TYPE_VIDEO;
                File file = new File(url);
                MediaPlayer.getInstance().initializePlayer(this, getActivity(), rootView, file.getAbsolutePath());

                if (currentMediaModel.duration!=0)
                    viewControlsCallback.startAutoPlay(currentMediaModel.duration);
                else
                    viewControlsCallback.startAutoPlay(15000);

            } else if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
                currentMediaType = AppLibrary.MEDIA_TYPE_IMAGE;
                AppLibrary.log_d(TAG, "Currently displaying media with mediaId -" + currentMediaModel.mediaId);
                momentImageView.setImageURI(Uri.fromFile(new File(url)));

                if (currentMediaModel.duration!=0)
                    viewControlsCallback.startAutoPlay(currentMediaModel.duration);
                else
                    viewControlsCallback.startAutoPlay(15000);

                if (momentImageView.getVisibility() == View.GONE) {
                    momentImageView.setVisibility(View.VISIBLE);
                }
            } else {
                // error
                AppLibrary.log_e(TAG, "Error playing next Media");
            }
            if (nearbyDownloader == null)
                nearbyDownloader = DynamicDownloader.getInstance(getActivity());
//            if (this.isNearByMomentOpened)
//                nearbyDownloader.notifySwitchToNextMedia(currentMomentModel.momentId,currentMediaModel);

//            if (currentMediaModel != null)
//                MasterClass.snapshotDetector.addToMediaList((int)System.currentTimeMillis(),currentMediaModel.mediaId);
        }
    }

    DynamicDownloader nearbyDownloader;
    public boolean fileExists(String value) {
        return new File(value).exists();
    }

    public void hideImageView() {
        if (momentImageView != null && momentImageView.getVisibility() == View.VISIBLE)
            momentImageView.setVisibility(View.GONE);
    }

    private void playPreviousMoment() {
        currentMediaModel = null;
        currentMomentModel = null;
        switchToPreviousMoment();
        String url = getNextMediaUrl();
        if (url != null) {
            int mediaType = AppLibrary.getMediaType(url);
            if (mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                currentMediaType = AppLibrary.MEDIA_TYPE_VIDEO;
                File file = new File(url);
                MediaPlayer.getInstance().initializePlayer(this, getActivity(), rootView, file.getAbsolutePath());

                if (currentMediaModel.duration!=0)
                    viewControlsCallback.startAutoPlay(currentMediaModel.duration);
                else
                    viewControlsCallback.startAutoPlay(15000);

            } else if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
                currentMediaType = AppLibrary.MEDIA_TYPE_IMAGE;
                momentImageView.setImageURI(Uri.fromFile(new File(url)));

                if (currentMediaModel.duration!=0)
                    viewControlsCallback.startAutoPlay(currentMediaModel.duration);
                else
                    viewControlsCallback.startAutoPlay(15000);

                if (momentImageView.getVisibility() == View.GONE) {
                    momentImageView.setVisibility(View.VISIBLE);
                    if (previousMediaModel != null && AppLibrary.getMediaType(previousMediaModel.url) == AppLibrary.MEDIA_TYPE_VIDEO) {
                        MediaPlayer.getInstance().onPause();
                    }
                }
            } else {
                // error
            }
        } else {
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onDestroy();
            }
            viewControlsCallback.onCloseMomentsFragment();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) getParentFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isNearByMomentOpened)
            MasterClass.snapshotDetector.start(getActivity());
        if (!isFirstTime) {
            isFirstTime = false;
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onResume();
            }
            viewControlsCallback.resumeAutoPlay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isNearByMomentOpened)
            MasterClass.snapshotDetector.stop(getActivity());
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO)
            MediaPlayer.getInstance().onPause();
        viewControlsCallback.stopAutoPlay();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onSwipeRightAction() {
        playPreviousMoment();
    }

    public void updatePlaylist() {
        momentImageView.setImageBitmap(null);
        switchToNextMoment();
        playNextMedia();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeButton:
                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onDestroy();
                }
                viewControlsCallback.onCloseMomentsFragment();
                break;
        }
    }


    public Bitmap getActiveBitmap(){
        momentImageView.setDrawingCacheEnabled(true);
        momentImageView.buildDrawingCache(true);
        if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE)
            return Bitmap.createBitmap(momentImageView.getDrawingCache());
        else
            return MediaPlayer.getInstance().getBitmap();
    }

    @Override
    public void onStateEnded() {
        MediaPlayer.getInstance().onPlayerRelease();
        playNextMedia();
    }

    @Override
    public void onStateReady() {
        hideImageView();
    }

    @Override
    public void onMinimize() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().onDestroy();
        }
        viewControlsCallback.onCloseMomentsFragment();
    }

    @Override
    public void onScreenshotTaken(HashMap<String, String> mediaDetails) {

    }

    private class CustomGestureListener implements GestureDetector.OnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;


        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            onSingleTap();
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onLongHold();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                            result = true;
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

        public void onSwipeRight() {
        }

        public void onSwipeLeft() {
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }

        public void onSingleTap() {
            if (isCurrentMediaPaused) {
                resumeCurrentMedia();
            } else
                playNextMedia();
        }

        public void onLongHold() {
            if (!isCurrentMediaPaused)
                pauseCurrentMedia();
        }
    }

    public interface ViewControlsCallback {
        void setGestureDetector(GestureDetector gestureDetector);

        ListIterator<Map.Entry<String, MomentModel>> getMomentListIterator();

        void startAutoPlay(long time);

        void transitToUpcomingMoments(int position);

        int getMomentStatus();

        void stopAutoPlay();

        void resumeAutoPlay();

        void onCloseMomentsFragment();

        void setTotalTime(long totalTime);
    }
//
//    @Override
//    public void onScreenshotTaken(String mediaId) {
//        FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getActivity());
//        if (!isNearByMomentOpened) {
//            AppLibrary.log_d("SnapshotDetector", "Screen Shot taken media is -" + mediaId);
//            fireBaseHelper.updateOnScreenShotTaken(currentMomentModel.userId, currentMomentModel.roomId, mediaId);
//        }
//    }
}