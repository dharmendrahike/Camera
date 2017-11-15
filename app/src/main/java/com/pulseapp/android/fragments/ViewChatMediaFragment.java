package com.pulseapp.android.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.pulseapp.android.ExoPlayer.EventLogger;
import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.ChatMediaModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.OnScreenshotTakenListener;
import com.pulseapp.android.util.SmartViewPage;
import com.pulseapp.android.util.SnapshotDetector;
import com.pulseapp.android.util.ViewPageCloseCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by user on 5/28/2016.
 */
public class ViewChatMediaFragment extends Fragment implements View.OnClickListener, OnScreenshotTakenListener,EventLogger.OnPlayerStateChanged, ViewPageCloseCallback{

    private static final String TAG = "ViewChatMediaFragment";
    private View rootView;
    private ImageView momentImageView;
    private RoomsModel.Messages currentMessageModel;
    private RoomsModel.Messages previousMessageModel;
    private ListIterator<Map.Entry<String, RoomsModel.Messages>> messageIterator;
    private int currentMediaType;
    private boolean isCurrentMediaPaused = false;
    private ViewControlsCallback viewControlsCallback;
    private FireBaseHelper mFireBaseHelper;
    private String roomId;
    private boolean isFirstTime = true;
    private CountDownTimer timer;
    private long timerTime;
    private android.os.Handler autoPlayHandler = new Handler();
    private static final int AUTO_PLAY_THRESHOLD_TIME = 7;
    private Runnable autoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            playNextMedia();
        }
    };
    private int roomType;
    private SmartViewPage viewPage;

    public void startAutoPlay() {
        if(timer!=null) {
            timer.cancel();
            timer = null;
        }
        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(AUTO_PLAY_THRESHOLD_TIME * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    public void stopAutoPlay() {
        autoPlayHandler.removeCallbacksAndMessages(null);

        if (timer!=null) {
            timer.cancel();
            timer = null;
        }
    }

    public void resumeAutoPlay() {
        if(timer!=null) {
            timer.cancel();
            timer = null;
        }

        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(timerTime, 1000) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        Bundle bundle = getArguments();
        roomId = bundle.getString(AppLibrary.ROOM_ID);
        roomType = bundle.getInt(AppLibrary.ROOM_TYPE);
        LinkedHashMap<String,RoomsModel.Messages> chatMediaMap = viewControlsCallback.getDownloadedChatMediaMap(bundle.getString(AppLibrary.MEDIA_ID),bundle.getString(AppLibrary.MESSAGE_ID));
        ArrayList<Map.Entry<String,RoomsModel.Messages>> list;
        if (chatMediaMap != null)
            list = new ArrayList<>(chatMediaMap.entrySet());
        else {
            list = new ArrayList<>();
        }
        messageIterator = list.listIterator();
        mFireBaseHelper = FireBaseHelper.getInstance(getActivity());

        MasterClass.snapshotDetector.clearMediaList();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        ((CameraActivity) getActivity()).toggleFullScreen(true);
        rootView = inflater.inflate(R.layout.view_chat_media_fragment,container,false);
        initializeViewObjects(rootView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getActivity().getWindow();
            AppLibrary.goTrulyFullscreen(w);
        }

        return rootView;
    }

    private void initializeViewObjects(View rootView) {
        MasterClass.snapshotDetector.setListener(this);
        rootView.findViewById(R.id.closeButton).setOnClickListener(this);
        momentImageView = (ImageView) rootView.findViewById(R.id.momentImage);

        viewPage = ((SmartViewPage) rootView.findViewById(R.id.touch_frame));
        viewPage.setListener(this);

        final GestureDetector gestureDetector = new GestureDetector(getActivity(),new CustomGestureListener());
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean touchResult = true;
                boolean result = gestureDetector.onTouchEvent(event);

                if (isCurrentMediaPaused && event.getActionMasked()==MotionEvent.ACTION_UP) {
                    resumeCurrentMedia();
                }

                return touchResult || result;
            }
        });
        playNextMedia();
    }

    private void resumeCurrentMedia(){
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().resumeCurrentMedia();
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE){
            resumeAutoPlay();
        }
        isCurrentMediaPaused = false;
    }

    private void pauseCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().pauseCurrentMedia();
        }else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE){
            stopAutoPlay();
        }
        isCurrentMediaPaused = true;
    }

    private String getNextMediaUrl(){
        String url = null;
        previousMessageModel = currentMessageModel;
        if (messageIterator.hasNext()){
            if (previousMessageModel != null && !previousMessageModel.memberId.equals(mFireBaseHelper.getMyUserId())) {
                mFireBaseHelper.openMedia(null, previousMessageModel.mediaId, roomId, previousMessageModel.messageId, previousMessageModel.expiryType,roomType);
                viewControlsCallback.onOpenMedia(previousMessageModel.messageId,previousMessageModel.mediaId,previousMessageModel.position);
            }
            currentMessageModel = messageIterator.next().getValue();
            url = currentMessageModel.uri;
        } else {
            // exit the fragment
            if (previousMessageModel != null && !previousMessageModel.memberId.equals(mFireBaseHelper.getMyUserId())) {
                mFireBaseHelper.openMedia(null, previousMessageModel.mediaId, roomId, previousMessageModel.messageId, previousMessageModel.expiryType,roomType);
                viewControlsCallback.onOpenMedia(previousMessageModel.messageId,previousMessageModel.mediaId,previousMessageModel.position);
            }
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onDestroy();
            }
            stopAutoPlay();
            viewControlsCallback.onCloseViewChatMediaFragment();
        }
        return url;
    }

    public void playNextMedia() {
        String url = getNextMediaUrl();
        if (url != null) {
            if (previousMessageModel != null && AppLibrary.getMediaType(previousMessageModel.uri) == AppLibrary.MEDIA_TYPE_VIDEO){
                MediaPlayer.getInstance().onPlayerRelease();
            }
            int mediaType = AppLibrary.getMediaType(url);
            if (mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                stopAutoPlay();
                currentMediaType = AppLibrary.MEDIA_TYPE_VIDEO;
                File file = new File(url);
                MediaPlayer.getInstance().initializePlayer(this,getActivity(), rootView, file.getAbsolutePath());
            } else if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
                currentMediaType = AppLibrary.MEDIA_TYPE_IMAGE;
                momentImageView.setImageURI(Uri.fromFile(new File(url)));
                startAutoPlay();
                if (momentImageView.getVisibility() == View.GONE) {
                    momentImageView.setVisibility(View.VISIBLE);
                }
            } else {
                // error
                AppLibrary.log_e(TAG,"Error playing next Media");
            }

            if (currentMessageModel != null) {
//                MasterClass.snapshotDetector.addToMediaList((int) System.currentTimeMillis(), currentMessageModel.mediaId, cu);
            }
        }
    }

    public void hideImageView() {
        if (momentImageView != null && momentImageView.getVisibility() == View.VISIBLE)
            momentImageView.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback)context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        MasterClass.snapshotDetector.start(getActivity());
        if (!isFirstTime){
            isFirstTime = false;
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onResume();
            }
            resumeAutoPlay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MasterClass.snapshotDetector.stop(getActivity());
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO)
            MediaPlayer.getInstance().onPause();
        stopAutoPlay();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        ((CameraActivity) getActivity()).toggleFullScreen(false);
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        MediaPlayer.getInstance().onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.closeButton:
                stopAutoPlay();
                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onDestroy();
                }
                viewControlsCallback.onCloseViewChatMediaFragment();
                break;
        }
    }

//    @Override
//    public void onScreenshotTaken(String mediaId) {
//        FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getActivity());
//        if (!currentMessageModel.memberId.equals(mFireBaseHelper.getMyUserId())) {
//            AppLibrary.log_d("SnapshotDetector", "Screen Shot taken media is -" + mediaId);
//            fireBaseHelper.updateOnScreenShotTaken(currentMessageModel.memberId, roomId, mediaId);
//        }
//    }

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
        stopAutoPlay();
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().onDestroy();
        }
        viewControlsCallback.onCloseViewChatMediaFragment();
    }

    @Override
    public void onScreenshotTaken(HashMap<String, String> mediaDetails) {

    }

    private class FetchImageTask extends AsyncTask<String,Void,Bitmap> {

        String imageUrl;

        public FetchImageTask(String url){
            this.imageUrl = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                bitmap = Picasso.with(getActivity())
                        .load(new File(imageUrl))
                        .centerCrop()
                        .resize(AppLibrary.getDeviceParams(getActivity(),"width"),
                                AppLibrary.getDeviceParams(getActivity(),"height")).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null){
                momentImageView.setImageBitmap(bitmap);
            }
        }
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
                            result = false;
                        } else {
                            onSwipeLeft();
                        }
                    }
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
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

        public void onSingleTap(){
            if (isCurrentMediaPaused) {
                resumeCurrentMedia();
            } else
                playNextMedia();
        }

        public void onLongHold(){
            if (!isCurrentMediaPaused)
                pauseCurrentMedia();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ViewControlsCallback{
        LinkedHashMap<String,RoomsModel.Messages> getDownloadedChatMediaMap(String mediaId, String messageId);
        void onCloseViewChatMediaFragment();
        void onOpenMedia(String messageId,String mediaId,int position);
    }
}