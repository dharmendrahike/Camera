package com.pulseapp.android.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.login.LoginResult;
import com.pulseapp.android.ExoPlayer.EventLogger;
import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.MyMediaViewerListAdapter;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.customViews.PieView;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.MediaModelView;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.ViewerDetails;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.OnSwipeTouchListener;
import com.pulseapp.android.util.SmartViewPage;
import com.pulseapp.android.util.ViewPageCloseCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by deepankur on 6/9/16.
 */

public class ViewMyMediaFragment extends BaseFragment implements View.OnClickListener, EventLogger.OnPlayerStateChanged, ViewPageCloseCallback, FireBaseKEYIDS {

    //    private final static boolean APPLY_BLUR = false;
    private View rootView;
    private View slidingView;
    private RelativeLayout sliderHeaderView;
    private MotionEvent swipeEvent;
    private View headerSwipeDetectorView, bodySwipeDetectorView;
    private String TAG = getClass().getSimpleName();
    private GestureDetector HeaderGestureListener;
    private int displayHeight;
    private int sliderHeaderHeight;
    private int MAXIMUM_DISPLACEMENT_DISTANCE;
    boolean swipeDetected;
    private String mediaId;
    private String momentId;
    private MediaModelView currentMediaModelView;
    private int currentMediaType;
    private Iterator<Map.Entry<String, MediaModelView>> mediaIterator;
    private String nextMediaUrl;
    private MediaModelView previousMediaModelView;
    private ViewControlsCallback viewControlsCallback;
    private ImageView mediaImageView;
    private boolean isFirstTime = true;
    private boolean isCurrentMediaPaused = false;
    private android.os.Handler autoPlayHandler = new Handler();
    private static final int AUTO_PLAY_THRESHOLD_TIME = 5;
    private CountDownTimer timer;
    private long timerTime, totaltimePerMedia;
    private ImageView blurredIv;
    private FrameLayout backgroundFrame;
    private Runnable autoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            playNextMedia();
        }
    };
    private RecyclerView viewersRecyclerView;
    private ImageView arrowIv;
    private SmartViewPage viewPage;
    private static final int PERMISSION_ACCESS_EXTERNAL_STORAGE = 0;
    private boolean isDestroyed;
    private boolean ignoreTimer = false;
    private PieView pieView;

    public void startAutoPlay(long time) {
        if (ignoreTimer) return;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        totaltimePerMedia = time;

        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(totaltimePerMedia, 10) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;

                float angle = ((float) timerTime / totaltimePerMedia) * 360;
                pieView.setPercentage((angle / 360) * 100);
                pieView.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    public void stopAutoPlay() {
        autoPlayHandler.removeCallbacksAndMessages(null);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;

        Bundle bundle = getArguments();
        mediaId = bundle.getString(AppLibrary.MEDIA_ID);
        momentId = bundle.getString(AppLibrary.MOMENT_ID);
        if (momentId.equals(mFireBaseHelper.getMyUserModel().momentId)) {
            if (mediaId != null) {
                // fetch media list containing only this mediaId
                HashMap<String, MediaModelView> hashMap = new HashMap<>();
                hashMap.put(mediaId, mFireBaseHelper.getMyStreams().get(mediaId));
                mediaIterator = hashMap.entrySet().iterator();
                ignoreTimer = true;

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.WATCH_MY_MEDIA);
            } else {
                // fetch all the media list
                mediaIterator = mFireBaseHelper.getMyDownloadedStreams().entrySet().iterator();
                ignoreTimer = false;

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.WATCH_MY_STREAM);
            }
        } else {
            if (mediaId != null) {
                // fetch media list containing only this mediaId
                HashMap<String, MediaModelView> hashMap = new HashMap<>();
                hashMap.put(mediaId, mFireBaseHelper.getContributableDownloadedStreams(momentId).get(mediaId));
                mediaIterator = hashMap.entrySet().iterator();
                ignoreTimer = true;

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.WATCH_MY_CONTRIBUTED_MEDIA);
            } else {
                // fetch all the media list
                mediaIterator = mFireBaseHelper.getContributableDownloadedStreams(momentId).entrySet().iterator();
                ignoreTimer = false;

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.WATCH_MY_CONTRIBUTED_STREAM);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isFirstTime) {
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onResume();
            }
            resumeAutoPlay();
        }

        isFirstTime = false;

        ((CameraActivity) getActivity()).toggleFullScreen(true);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO)
            MediaPlayer.getInstance().onPause();
        stopAutoPlay();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_view_my_media, container, false);
        initSharePopUp(rootView);
        if (savedInstanceState != null) return rootView;

        arrowIv = (ImageView) rootView.findViewById(R.id.arrowIV);
        mediaImageView = (ImageView) rootView.findViewById(R.id.mediaImage);
        viewersRecyclerView = (RecyclerView) rootView.findViewById(R.id.viewersRecyclerView);
        viewersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rootView.findViewById(R.id.deleteIV).setOnClickListener(this);
        rootView.findViewById(R.id.downloadIV).setOnClickListener(this);
        sliderHeaderView = (RelativeLayout) rootView.findViewById(R.id.sliderHeaderRL);
        slidingView = rootView.findViewById(R.id.sliderFrame);
        blurredIv = (ImageView) rootView.findViewById(R.id.blurredIV);
        pieView = (PieView) rootView.findViewById(R.id.pieView);
        if (!ignoreTimer) pieView.setVisibility(View.VISIBLE);

        viewPage = ((SmartViewPage) rootView.findViewById(R.id.touch_frame));
        viewPage.setListener(this);


        Log.d(TAG, " ---- " + AppLibrary.getDeviceParams((CameraActivity) context)[1]);
        Log.d(TAG, " ---- real" + AppLibrary.getFullScreenDeviceParams((CameraActivity) context)[1]);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Window w = getActivity().getWindow();
            AppLibrary.goTrulyFullscreen(w);
            w.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    Log.d(TAG, "onSystemUiVisibilityChange");
                    //first wait for system to do whatever it must;and then instead of
                    //getting variables from the system we measure our views as we are sure that
                    //they are being drawn correctly because of their height being specified as MATCH_PARENT

                    if (!isDestroyed && slidingView.getViewTreeObserver().isAlive()) {
                        slidingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (slidingView.getViewTreeObserver().isAlive()) {
                                    // only need to calculate once, so remove listener
                                    slidingView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                }

                                displayHeight = slidingView.getMeasuredHeight();
                                sliderHeaderHeight = AppLibrary.convertDpToPixels(context, 56);//same as defined in xml
                                MAXIMUM_DISPLACEMENT_DISTANCE = displayHeight - sliderHeaderHeight;
                                slidingView.animate().translationY(MAXIMUM_DISPLACEMENT_DISTANCE).setDuration(100).start();
                                arrowIv.setScaleY(-1);
                                Log.d(TAG, "VTO sliderHeaderHeight " + sliderHeaderHeight + " MAXIMUM_DISPLACEMENT_DISTANCE " + MAXIMUM_DISPLACEMENT_DISTANCE);

                            }
                        });
                    }

                }
            });
        }


        backgroundFrame = (FrameLayout) rootView.findViewById(R.id.backgroundFrame);

        handleSwipeGestureOnHeader();
        handleSwipeGestureOnBody();
        HeaderGestureListener = new GestureDetector(context, new CustomGestureListener());

        final GestureDetector detector = new GestureDetector(context, new MyGestureListener());
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean touchResult = true;
                boolean result = detector.onTouchEvent(event); //Result is true when swiping to top

                if (!result && isCurrentMediaPaused && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    resumeCurrentMedia();
                }

                return touchResult || result;
            }
        });

        sliderHeaderView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                {//swipe thing
                    swipeEvent = MotionEvent.obtain(event);
                    swipeEvent.offsetLocation(swipeEvent.getRawX(), swipeEvent.getRawY());
                    headerSwipeDetectorView.dispatchTouchEvent(swipeEvent);
                }
                //translation thing
                handleHeaderTranslation(event);

                //tap thing
                HeaderGestureListener.onTouchEvent(event);
                return true;
            }
        });

        slidingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                {//swipe thing
                    swipeEvent = MotionEvent.obtain(event);
                    swipeEvent.offsetLocation(swipeEvent.getRawX(), swipeEvent.getRawY());
                    bodySwipeDetectorView.dispatchTouchEvent(swipeEvent);
                }

                handleBodyTranslation(event);
                return true;
            }
        });
        playNextMedia();
        return rootView;
    }

    public void resumeAutoPlay() {
        if (ignoreTimer) return;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(timerTime, 10) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;

                float angle = ((float) timerTime / totaltimePerMedia) * 360;
                pieView.setPercentage((angle / 360) * 100);
                pieView.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    private void resumeCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().resumeCurrentMedia();
        }
        resumeAutoPlay();
        isCurrentMediaPaused = false;
    }

    private void pauseCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().pauseCurrentMedia();
        }
        stopAutoPlay();
        isCurrentMediaPaused = true;
    }

    public void playNextMedia() {
        Log.d(TAG, "playing next media");
        String url = getNextMediaUrl();
        if (url != null) {
            if (currentMediaModelView.status != UPLOADING_NOT_READY_VIDEO) {
                if (previousMediaModelView != null && AppLibrary.getMediaType(previousMediaModelView.url) == AppLibrary.MEDIA_TYPE_VIDEO) {
                    MediaPlayer.getInstance().onPlayerRelease();
                }

                stopAutoPlay();

                int mediaType = AppLibrary.getMediaType(url);
                if (mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    currentMediaType = AppLibrary.MEDIA_TYPE_VIDEO;
                    File file = new File(url);
                    if (ignoreTimer)
                        MediaPlayer.getInstance().isLooping = true;
                    else
                        startAutoPlay(currentMediaModelView.duration == 0 ? 15000 : currentMediaModelView.duration);

                    MediaPlayer.getInstance().initializePlayer(this, getActivity(), rootView, file.getAbsolutePath());
                } else if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
                    currentMediaType = AppLibrary.MEDIA_TYPE_IMAGE;
                    mediaImageView.setImageURI(Uri.fromFile(new File(url)));

                    if (!ignoreTimer)
                        startAutoPlay(currentMediaModelView.duration == 0 ? AUTO_PLAY_THRESHOLD_TIME * 1000 : currentMediaModelView.duration);

                    if (mediaImageView.getVisibility() == View.GONE) {
                        mediaImageView.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                getNextMediaUrl();
            }
        }
        if (currentMediaModelView != null && currentMediaModelView.status != UPLOADING_NOT_READY_VIDEO) {
            if (currentMediaModelView.viewerDetails != null) {
                int count = currentMediaModelView.viewerDetails.size() + currentMediaModelView.webViews;
                ((TextView) rootView.findViewById(R.id.viewsText)).setText(String.valueOf(count));
                int screenShots = 0;
                for (Map.Entry<String, ViewerDetails> entry : currentMediaModelView.viewerDetails.entrySet()) {
                    if (entry.getValue().screenShotted) {
                        screenShots++;
                    }
                }
                ((TextView) rootView.findViewById(R.id.screenShotText)).setText(String.valueOf(screenShots));
            } else {
                if (currentMediaModelView.webViews > 0)
                    ((TextView) rootView.findViewById(R.id.viewsText)).setText(String.valueOf(currentMediaModelView.webViews));
                else
                    ((TextView) rootView.findViewById(R.id.viewsText)).setText("0");
                ((TextView) rootView.findViewById(R.id.screenShotText)).setText("0");
            }

            if (currentMediaModelView != null && currentMediaModelView.mediaState == 0) {
                rootView.findViewById(R.id.deleteIV).setVisibility(View.VISIBLE);
            } else {
                rootView.findViewById(R.id.deleteIV).setVisibility(View.GONE);
            }

            if (currentMediaModelView.viewerDetails == null)
                currentMediaModelView.viewerDetails = new HashMap<>();

//            if (adapter == null)
            new ViewsSortingTask(currentMediaModelView).execute();
//            viewersRecyclerView.setAdapter(adapter);
        }
    }

    MyMediaViewerListAdapter adapter;
    private float firstDownRawY;
    private float moves = 0;

    private void handleHeaderTranslation(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            moves = 0;
            viewPage.setCancelAnimation(true);
            firstDownRawY = event.getRawY() - slidingView.getTranslationY();
            Log.d(TAG, "handleHeaderTranslation ACTION_DOWN");
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            slidingView.setTranslationY(event.getRawY() - firstDownRawY);
            ++moves;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            viewPage.setCancelAnimation(true);
            Log.d(TAG, "handleHeaderTranslation ACTION_UP");
            if (!swipeDetected && moves > 3)
                settleDownTheView();
            swipeDetected = false;
        }
    }

    private float previousRawY;

    private void handleBodyTranslation(MotionEvent event) {

        float eventRawY = event.getRawY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            viewPage.setCancelAnimation(true);
            previousRawY = eventRawY;
            Log.d(TAG, "handleHeaderTranslation ACTION_DOWN");
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

            float sliderTranslationY = slidingView.getTranslationY();
            if (sliderTranslationY + eventRawY - previousRawY >= 0 &&
                    sliderTranslationY + eventRawY - previousRawY <= MAXIMUM_DISPLACEMENT_DISTANCE) {
                slidingView.setTranslationY(sliderTranslationY + eventRawY - previousRawY);
            }
            previousRawY = eventRawY;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            viewPage.setCancelAnimation(false);
            Log.d(TAG, "handleHeaderTranslation ACTION_UP");
            if (!swipeDetected)
                settleDownTheView();
            swipeDetected = false;
        }
    }

    private void settleDownTheView() {
        float dis = slidingView.getTranslationY();
        if (dis > Math.abs(dis - MAXIMUM_DISPLACEMENT_DISTANCE)) {
            closeDrawer();
        } else openDrawer();
    }


    private void handleSwipeGestureOnHeader() {
        headerSwipeDetectorView = new View(context);
        headerSwipeDetectorView.setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeTop() {
                Log.d(TAG, "onSwipeTop");
                swipeDetected = true;
                openDrawer();
            }

            @Override
            public void onSwipeRight() {
            }

            @Override
            public void onSwipeLeft() {
            }

            @Override
            public void onSwipeBottom() {
                Log.d(TAG, "onSwipeBottom");
                swipeDetected = true;
                closeDrawer();
            }
        });
    }


    private void handleSwipeGestureOnBody() {
        bodySwipeDetectorView = new View(context);
        bodySwipeDetectorView.setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeTop() {
                Log.d(TAG, "onSwipeTop");
                swipeDetected = true;
                openDrawer();
            }

            @Override
            public void onSwipeRight() {
            }

            @Override
            public void onSwipeLeft() {
            }

            @Override
            public void onSwipeBottom() {
                Log.d(TAG, "onSwipeBottom");
                swipeDetected = true;
                closeDrawer();
            }
        });
    }

    private int ANIMATION_DURATION = 250;
    private boolean inAnimation = false;

    private void openDrawer() {
        if (inAnimation) return;
        inAnimation = true;
        Log.d(TAG, "openDrawer");
        pauseCurrentMedia();

        ObjectAnimator o = ObjectAnimator.ofFloat(slidingView, "translationY", slidingView.getTranslationY(), 0);
        o.setDuration(ANIMATION_DURATION);
        o.addListener(animatorListener);
        o.start();
        ObjectAnimator.ofFloat(arrowIv, "scaleY", arrowIv.getScaleY(), 1).setDuration(ANIMATION_DURATION).start();

        new Handler().postDelayed(r2, ANIMATION_DURATION);
    }

    Runnable r1 = new Runnable() {
        @Override
        public void run() {
            viewPage.setCancelAnimation(false);
        }
    };

    Runnable r2 = new Runnable() {
        @Override
        public void run() {
            viewPage.setCancelAnimation(true);
        }
    };

    private void closeDrawer() {
        if (inAnimation) return;

        inAnimation = true;
        Log.d(TAG, "closeDrawer");
        resumeCurrentMedia();

        ObjectAnimator o = ObjectAnimator.ofFloat(slidingView, "translationY", slidingView.getTranslationY(), MAXIMUM_DISPLACEMENT_DISTANCE).setDuration(ANIMATION_DURATION);
        o.addListener(animatorListener);
        o.start();
        ObjectAnimator.ofFloat(arrowIv, "scaleY", arrowIv.getScaleY(), -1).setDuration(ANIMATION_DURATION).start();

        new Handler().postDelayed(r1, ANIMATION_DURATION);
    }

    private Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            Log.d(TAG, "onAnimationStart");
//            inAnimation = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            inAnimation = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            Log.d(TAG, "onAnimationCancel");
            inAnimation = false;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFireBaseHelper == null || getActivity() == null) return;

        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO)
            MediaPlayer.getInstance().onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;

        if (mFireBaseHelper == null || getActivity() == null) return;

        ((CameraActivity) getActivity()).toggleFullScreen(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.deleteIV:
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.DELETE_MY_MEDIA);
                deleteMedia();
                break;
            case R.id.downloadIV:
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SAVE_MY_MEDIA);
                requestDownloadMedia();
                break;
            case R.id.closeButton:
                stopAutoPlay();
//                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onDestroy();
//                }
                viewControlsCallback.onCloseViewMyMediaFragment();
                break;
        }
    }

    private void requestDownloadMedia() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_ACCESS_EXTERNAL_STORAGE);
        } else {
            downloadMedia();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadMedia();
                } else {
                    showShortToastMessage(" Please provide access to save media");
                }
                break;
        }
    }


    private void downloadMedia() {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String suffixString = "_saved";
        String extension = null;
        if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            extension = ".jpg";
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            extension = ".mp4";
        }
        String name = date + "_" + today.hour + today.minute + today.second + suffixString + extension;
        String mediaStoragePath = AppLibrary.setupOutputDirectoryForRecordedFile(); //Uses external SD card
        File mediaDir = new File(mediaStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();

        File sourceFile = new File(currentMediaModelView.url);
        File destFile = new File(mediaDir, name);
        try {
            if (sourceFile.exists()) {

                InputStream in = new FileInputStream(sourceFile);
                OutputStream out = new FileOutputStream(destFile);

                if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {

                    Bitmap bitmap = (BitmapFactory.decodeFile(sourceFile.getAbsolutePath())).copy(Bitmap.Config.ARGB_8888, true);
                    Bitmap bitmapWaterMark = (BitmapFactory.decodeResource(context.getResources(), R.drawable.stamp_image)).copy(Bitmap.Config.ARGB_8888, true);

                    Bitmap result = addWaterMarkToBitmap(bitmap, bitmapWaterMark); //Add watermark

                    result.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    Log.v(TAG, "Copy file successful.");
                    Toast.makeText(getActivity(), "Saved to Gallery", Toast.LENGTH_SHORT).show();

                    addRecordingToMediaLibrary(true, destFile);

//                    if (bitmap != null) {
//                        bitmap.recycle();
//                    }

                   if (bitmapWaterMark != null){
                        bitmapWaterMark.recycle();
                    }

                    if(result!=null) {
                        result.recycle();
                    }

                } else {
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    Log.v(TAG, "Copy file successful.");
                    Toast.makeText(getActivity(), "Saved to Gallery", Toast.LENGTH_SHORT).show();

                    addRecordingToMediaLibrary(false, destFile);
                }

                in.close();
                out.close();

            } else {
                Log.v(TAG, "Copy file failed. Source file missing.");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void addRecordingToMediaLibrary(boolean image, File editedFile) {
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        if (image) {
            values.put(MediaStore.Images.Media.TITLE, editedFile.getName());
            values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current / 1000));
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATA, editedFile.getAbsolutePath());
        } else {
            values.put(MediaStore.Video.Media.TITLE, editedFile.getName());
            values.put(MediaStore.Video.Media.DATE_ADDED, (int) (current / 1000));
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, editedFile.getAbsolutePath());
        }

        if (getActivity() != null) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            Uri base;
            if (image)
                base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            else
                base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            try {
                Uri newUri = contentResolver.insert(base, values);
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteMedia() {
        mFireBaseHelper.deleteMyMediaFromMyMoments(currentMediaModelView.mediaId);
        viewControlsCallback.onDeleteMyMedia();
        Toast.makeText(getActivity(), "Deleted", Toast.LENGTH_SHORT).show();
        playNextMedia();
    }

    public String getNextMediaUrl() {
        String url = null;
        previousMediaModelView = currentMediaModelView;
        if (mediaIterator.hasNext()) {
            currentMediaModelView = mediaIterator.next().getValue();

            if (currentMediaModelView == null) {
                MediaPlayer.getInstance().onDestroy();
                viewControlsCallback.onCloseViewMyMediaFragment();
                return null;
            }

            url = currentMediaModelView.url;
        } else {
            // exit the fragment
//            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().onDestroy();
//            }
            stopAutoPlay();
            viewControlsCallback.onCloseViewMyMediaFragment();
        }
        return url;
    }

    @Override
    public void onStateEnded() {
        MediaPlayer.getInstance().onPlayerRelease();
        playNextMedia();
    }

    @Override
    public void onStateReady() {
        if (mediaImageView != null && mediaImageView.getVisibility() == View.VISIBLE)
            mediaImageView.setVisibility(View.GONE);
    }

    @Override
    public void onMinimize() {
        stopAutoPlay();
//        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
        MediaPlayer.getInstance().onDestroy();
//        }
        viewControlsCallback.onCloseViewMyMediaFragment();
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            Log.d(TAG, "onSingleTapConfirmed at " + x + " " + y);
//            final int pos = getCurrentSliderPosition();
            Log.d(TAG, "Sliding view translation is " + slidingView.getTranslationY());
            if ((int) slidingView.getTranslationY() < (MAXIMUM_DISPLACEMENT_DISTANCE / 2))
                closeDrawer();
            else
                openDrawer();

            return super.onSingleTapConfirmed(e);
        }
    }

    public interface ViewControlsCallback {
        void onCloseViewMyMediaFragment();

        void onDeleteMyMedia();
    }

    private class MyGestureListener implements OnGestureListener {

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
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        result = true;
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

        void onSwipeLeft() {
        }

        void onSwipeTop() {
            openDrawer();
        }

        void onSwipeBottom() {
        }

        void onSingleTap() {
            if (isCurrentMediaPaused) {
                resumeCurrentMedia();
            } else
                playNextMedia();
        }

        void onLongHold() {
            if (!isCurrentMediaPaused)
                pauseCurrentMedia();
        }
    }

    private class ViewsSortingTask extends AsyncTask<MediaModelView, Void, ArrayList<Object>> {
        MediaModelView mediaModelView;
        ViewsSortingTask(MediaModelView mediaModelView) {
        this.mediaModelView = mediaModelView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Object> doInBackground(MediaModelView... params) {
            ArrayList<Object> friendsList = new ArrayList<>();
            ArrayList<Object> followersList = new ArrayList<>();
            ArrayList<Object> unionList = new ArrayList<>();


            HashMap<String, SocialModel.Friends> friends = mFireBaseHelper.getSocialModel().friends;
            if (friends == null)
                friends = new HashMap<>();
            Set<String> friendIds = friends.keySet();
            //making a new set to avoid repetition;
            // although it will be emty and code will always go to else
            if (mediaModelView.viewerDetails != null) {
                for (Map.Entry<String, ViewerDetails> entry : mediaModelView.viewerDetails.entrySet()) {
                    if (friendIds.contains(entry.getKey())) {
                        friendsList.add(new android.util.Pair<>(entry.getKey(), entry.getValue()));
                    } else {
                        if (!entry.getKey().equals(mFireBaseHelper.getMyUserId()))
                            followersList.add(new android.util.Pair<>(entry.getKey(), entry.getValue()));
                        else {
                            entry.getValue().name = "You";
                            friendsList.add(new android.util.Pair<>(entry.getKey(), entry.getValue()));
                        }
                    }
                }
            }


            if (friendsList.size() > 0) {
                unionList.add("Friends");
                unionList.addAll(friendsList);
            }
            if (followersList.size() > 0) {
                if (momentId.equals(mFireBaseHelper.getMyUserModel().momentId))
                    unionList.add("Followers");
                else
                    unionList.add("Others");

                unionList.addAll(followersList);
            }

            if (mediaModelView.webViews > 0) {
//                unionList.add("Web Viewers");
                unionList.add(mediaModelView.webViews);
            }
            friendsList.clear();
            followersList.clear();
            return unionList;
        }

        @Override
        protected void onPostExecute(ArrayList<Object> list) {
            super.onPostExecute(list);
            adapter = new MyMediaViewerListAdapter(list);
            viewersRecyclerView.setAdapter(adapter);
        }
    }

    private void initSharePopUp(View rootView) {
        rootView.findViewById(R.id.shareIv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseCurrentMedia();
                showFacebookSharePopup(new CameraActivity.SharePopupCallbacks() {
                    @Override
                    public void onShareFacebookClicked() {
                        if (facebookController != null && AccessToken.getCurrentAccessToken() != null
                                && AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions")) {
                            // provided publish permissions
                            if (mFireBaseHelper.getMyUserId() != null && momentId != null
                                    && AccessToken.getCurrentAccessToken().getToken() != null
                                    && currentMediaModelView.mediaId != null
                                    && currentMediaModelView.status == MEDIA_UPLOADING_COMPLETE) {
                                HashMap<Object, Object> postObject = new HashMap<>();
                                postObject.put(MOMENT_ID, momentId);
                                //postObject.put(HANDLE, publicMomentModel.handle);
                                postObject.put(TYPE, 3);
                                postObject.put(MEDIA_ID, currentMediaModelView.mediaId);
                                postObject.put(USER_ID, mFireBaseHelper.getMyUserId());
                                postObject.put(TOKEN, AccessToken.getCurrentAccessToken().getToken());
                                postObject.put("myStream", true);
                                mFireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, postObject);
                                if (isInternetAvailable(false))
                                    showShortToastMessage("Shared on Facebook");
                                else
                                    showShortToastMessage("Will be shared on Facebook when online");
                            } else {
                                if (currentMediaModelView.status == MEDIA_UPLOADING_COMPLETE)
                                    showShortToastMessage("Sorry! Something went wrong");
                                else {
                                    showShortToastMessage("Please upload before sharing to Facebook");
                                }
                            }
                            resumeCurrentMedia();
                        } else {
                            facebookController.doFacebookLogin(Arrays.asList("publish_actions"), new CameraActivity.FacebookLoginCallback() {
                                @Override
                                public void onSuccessfulLoginCallback(LoginResult loginResult) {
                                    if (mFireBaseHelper.getMyUserId() != null
                                            && momentId != null
                                            && loginResult.getAccessToken().getToken() != null
                                            && currentMediaModelView.mediaId != null
                                            && currentMediaModelView.status == MEDIA_UPLOADING_COMPLETE) {
                                        HashMap<Object, Object> postObject = new HashMap<Object, Object>();
                                        postObject.put(TYPE, 3);
                                        postObject.put(MOMENT_ID, momentId);
//                                        postObject.put(HANDLE, publicMomentModel.handle);
                                        postObject.put(MEDIA_ID, currentMediaModelView.mediaId);
                                        postObject.put(USER_ID, mFireBaseHelper.getMyUserId());
                                        postObject.put(TOKEN, loginResult.getAccessToken().getToken());
                                        postObject.put("myStream", true);

                                        mFireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, postObject);
                                        if (isInternetAvailable(false))
                                            showShortToastMessage("Shared on Facebook");
                                        else
                                            showShortToastMessage("Will be shared on Facebook when online");
                                    } else {

                                        if (currentMediaModelView.status == MEDIA_UPLOADING_COMPLETE)
                                            showShortToastMessage("Sorry! Something went wrong");
                                        else {
                                            showShortToastMessage("Please upload before sharing to Facebook");
                                        }
                                    }
                                    resumeCurrentMedia();
                                }

                                @Override
                                public void onErrorCallback() {
                                    resumeCurrentMedia();
                                }
                            });
                        }
                    }

                    @Override
                    public void onShareIntentClicked() {

                        launchMediaShareIntent(null, null, null, currentMediaModelView.type, currentMediaModelView.url);
                    }

                    @Override
                    public void onPopupDismiss() {
                        resumeCurrentMedia();
                    }

                    @Override
                    public void onWatsAppShareClicked() {
//                        if (publicMomentModel != null && publicMomentModel.handle != null && publicMomentModel.name != null)
                        launchMediaShareIntent(null, null, AppLibrary.WHATSAPP_PACKAGE_NAME, currentMediaModelView.type, currentMediaModelView.url);
                    }
                });
            }
        });
    }
}
