package com.pulseapp.android.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.activities.VideoEditorActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.broadcast.CameraShotCallback;
import com.pulseapp.android.broadcast.FilterManager;
import com.pulseapp.android.broadcast.FiltersFragment;
import com.pulseapp.android.broadcast.MyImageView;
import com.pulseapp.android.broadcast.PreviewModeChangeListener;
import com.pulseapp.android.broadcast.StreamingActionControllerKitKat;
import com.pulseapp.android.broadcast.StreamingActionControllerKitKat.FaceUpdateListener;
import com.pulseapp.android.customViews.CircularTextView;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AnimatedCircleView;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BlurBuilder;
import com.pulseapp.android.util.CapturedMediaController;
import com.pulseapp.android.util.ColoredTimelineView;
import com.pulseapp.android.util.CustomTypefaceSpan;
import com.pulseapp.android.util.CustomViewPager;
import com.pulseapp.android.util.FocusAnimation;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.MiuiPermissionHandler;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by abc on 2/16/2016.
 */
public class CameraFragment extends BaseFragment implements View.OnClickListener,
        /*ParentStickerFragment.SceneCustomizationListener,*/ PreviewModeChangeListener, CameraShotCallback,
        StreamingActionControllerKitKat.CameraControlCallback,
        StreamingActionControllerKitKat.FilterControlCallback, FaceUpdateListener, CanvasFragment.ViewControlsCallback {

    private static final String TAG = "CameraFragment";
    private static final int MSG_SET_FILTER_THUMBNAIL = 1;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1;
    private static final int RECORD_THRESHOLD_TIME = 15;
    private static final int PERMISSION_ACCESS_EXTERNAL_STORAGE = 0;

    private boolean mIsStickerFragmentPopped = false;
    private ColoredTimelineView timeLineView;

    public StreamingActionControllerKitKat mController;
    public boolean mRecordVideoOngoing;
    private int mResId;
    private boolean mAssetsVisible;

    /* Filters Controller */
    private FilterManager mFilter;
    private FiltersFragment mFilterFragment;

    float mDesiredAspectRatio = (float) 9 / 8;
    Bitmap doubleShotCaptureBitmap = null;
    Bitmap doubleShotCaptureTopBitmap = null;
    Bitmap doubleShotCaptureBottomBitmap = null;
    Canvas doubleShotCanvas;
    private ArrayList<String> recentPictureList;

    private LinearLayout doubleShotScene;
    private View rootView;
    private ImageView previewImage, recordButton;
    Picasso pic;

    private SeekBar seekBar1, seekBar2, seekBar3;

    private Handler timeLineHandler = new Handler();
    private Handler focusHandler = new Handler();
    private Long recordStartTime = 0L;
    private Runnable updateTimeLineThread = new Runnable() {
        @Override
        public void run() {
            long timeDiffInMilliseconds = System.currentTimeMillis() - recordStartTime;
            updateTimeLine(timeDiffInMilliseconds);
            if (timeDiffInMilliseconds / 1000 == RECORD_THRESHOLD_TIME) {
                timeLineHandler.removeCallbacksAndMessages(null);
                onRecordVideoStopped();
            }
            timeLineHandler.postDelayed(updateTimeLineThread, 16);
        }
    };

    /* handler to post all actions in this page */
    private android.os.Handler mHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message message) {
            int what = message.what;
            switch (what) {
                case MSG_SET_FILTER_THUMBNAIL:
                    if (mFilterFragment != null) {
                        mFilterFragment.setFilterThumbnail((Bitmap) message.obj, message.arg1);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private MyImageView imageView;
    private GLSurfaceView preview;
    public static int leftEyeX;
    public static int leftEyeY;
    public static int rightEyeX, rightEyeY, rightEarX, rightEarY, leftEarX, leftEarY, leftCheekX, rightCheekX, leftCheekY, rightCheekY;
    public static int leftMouthX, leftMouthY, rightMouthX, rightMouthY, bottomMouthX, bottomMouthY;
    public static int noseX, noseY;
    public static float yaw, roll, smileProbability;
    public static int faceX, faceY, faceWidth, faceHeight;
    private boolean animateRunning = false;
    private boolean filtersOpen = false;

    private ViewControlsCallback viewControlCallback;
    private boolean mDoubleShotEnabled;
    private boolean mDoubleShotBackOngoing;
    private ImageView topImageView, bottomImageView;
    public static boolean mVisible;
    private boolean fromIntent;
    private HandlerThread cursorThread;
    private Handler cursorHandler;
    private ImageView cameraSwapButton;
    private ImageView flashLayout;
    private TextView filterNameTV;
    private Cursor cursor;
    private AnimatedCircleView focusPointer;
    private CircularTextView unSeenCountTextView;
    private int unseenItemCount;

/*
    @Override
    public void setOverlayResource(int resId) {
        mResId = resId;
    }

    @Override
    public void setOverlay(float x, float y, int resId) {
        if (resId < 0) {
            ((StreamingActionControllerKitKat) mController).addOverlay(x, y, mResId);
        } else {
            ((StreamingActionControllerKitKat) mController).addOverlay(x, y, resId);
        }
    }

    @Override
    public void toggleButtonsAnimation(boolean makeAssetsVisible) {
        if (mAssetsVisible != makeAssetsVisible) {
            mAssetsVisible = makeAssetsVisible;
        }
    }

    @Override
    public void setOverlays(ThemeModel themeModels) {
        if (themeModels == null) {
            ((StreamingActionControllerKitKat) mController).addOverlays(null);
            return;
        }
        ArrayList<ThemeModel> arrayList = new ArrayList<>();
        arrayList.add(themeModels);
        ((StreamingActionControllerKitKat) mController).addOverlays(arrayList);
    }

    @Override
    public void setOverlay(StickerModel stickerModel) {
        ((StreamingActionControllerKitKat) mController).addOverlay(stickerModel);
    }
*/

    @Override
    public void setFilter(int filter, boolean enable) {
        ((StreamingActionControllerKitKat) mController).setFilter(filter, enable);
    }

    @Override
    public void triggerFilterThumbnail(int begin, int end) {
        Log.d(TAG, "request received for filter thumbnail capture");
        ((StreamingActionControllerKitKat) mController).triggerFilterThumbnail(begin, end);
    }

    @Override
    public void stopFilterThumbnail() {
        Log.d(TAG, "request received for stopping thumbnail capture");
        ((StreamingActionControllerKitKat) mController).stopFilterThumbnail();
    }

    @Override
    public void onBackCapture(Bitmap bitmap, String mediaPath) {
        doubleShotCaptureTopBitmap = bitmap; //Bitmap.createBitmap(bitmap);

        int width = doubleShotCaptureTopBitmap.getWidth();
        int height = (int) (width / mDesiredAspectRatio * 2);

        doubleShotCaptureBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        doubleShotCanvas = new Canvas(doubleShotCaptureBitmap);

        doubleShotCanvas.drawARGB(0, 0, 0, 0);

        Rect destRect = new Rect(0, 0, width, height / 2);
        Rect rect = scaleCenter(doubleShotCaptureTopBitmap, destRect, true);
        doubleShotCanvas.clipRect(destRect, Region.Op.REPLACE);
        doubleShotCanvas.drawBitmap(doubleShotCaptureTopBitmap, null, rect, new Paint());
//        Paint rectPaint = new Paint();
//        rectPaint.setColor(Color.BLACK);
//        rectPaint.setStyle(Paint.Style.STROKE);
//        doubleShotCanvas.drawRect(destRect, rectPaint);

        topImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        topImageView.setImageBitmap(doubleShotCaptureTopBitmap);

        bottomImageView.setBackgroundResource(R.drawable.transparent_background);
        switchCameraClicked();
        mDoubleShotBackOngoing = false;
        animatePreview();
    }

    @Override
    public void onFrontCapture(Bitmap bitmap, String mediaPath) {
        doubleShotCaptureBottomBitmap = Bitmap.createBitmap(bitmap);

        int totalWidth = doubleShotCaptureBitmap.getWidth();
        int totalHeight = doubleShotCaptureBitmap.getHeight();

        Rect destRect = new Rect(0, totalHeight / 2, totalWidth, totalHeight);
        Rect rect = scaleCenter(doubleShotCaptureBottomBitmap, destRect, true);

        Paint paint = new Paint();
        doubleShotCanvas.clipRect(destRect, Region.Op.REPLACE);
        doubleShotCanvas.drawBitmap(doubleShotCaptureBottomBitmap, null, rect, paint);
//        Paint rectPaint = new Paint();
//        rectPaint.setColor(Color.BLACK);
//        rectPaint.setStyle(Paint.Style.STROKE);
//        doubleShotCanvas.drawRect(destRect, rectPaint);
        bottomImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bottomImageView.setImageBitmap(doubleShotCaptureBottomBitmap);

//        pauseCameraPreview();

        viewControlCallback.launchVideoEditorFragment(doubleShotCaptureBitmap);

        mDoubleShotBackOngoing = true;
        mDoubleShotEnabled = false;
    }

    private void signalCameraSwap() {
        ((StreamingActionControllerKitKat) mController).signalSwapCamera();
    }

    @Override
    public void onCameraCapture(Bitmap bitmap, String mediaPath) {
        previewImage.setImageBitmap(bitmap);
        previewImage.setVisibility(View.VISIBLE);
        viewControlCallback.launchVideoEditorFragment(bitmap);
    }

    public void handleFilterThumbnail(Bitmap bitmap, int filter) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER_THUMBNAIL, filter, 0, bitmap));
    }

    public CameraFragment() {
        super.registerForInAppSignals(true);
//        mVisible = true;
        mAssetsVisible = true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal event) {
        AppLibrary.log_d(TAG, "caught Base Signal: " + event.getClass().getName());
        if (event instanceof BroadCastSignals.CameraPreviewStartedSignal) {
            //  camera preview started.
//            Fragment m;
////            if (getChildFragmentManager().findFragmentByTag("filter_frag") != null) {
//                mFilter = mController.getFilter();
//                //Do Nothing
//            } else {
//                CameraActivity.PreRecordFilters = true; //If some error, PreRecordFilters will be reset in FiltersFragment()
//                m = new FiltersFragment();
//                mFilterFragment = (FiltersFragment) m;
//                if (m != null) {
            mFilter = mController.getFilter();
//                    if (mFilter != null) {
////                        Bundle args = new Bundle();
////                        args.putSerializable("filter_instance", mFilter);
////                        m.setArguments(args);
////                        getChildFragmentManager().beginTransaction().replace(R.id.filter_fragments_container, m, "filter_frag").commitAllowingStateLoss();
//                    } else
//                        CameraActivity.PreRecordFilters = false;
//                } else
//                    CameraActivity.PreRecordFilters = false;
//            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mFilter != null) {
                        if (CameraActivity.flashMode) {
                            rootView.findViewById(R.id.flashLayout).setBackgroundResource(R.drawable.clear_flash);
                            rootView.findViewById(R.id.flashLayout).setVisibility(View.VISIBLE);
                        } else
                            rootView.findViewById(R.id.flashLayout).setVisibility(View.GONE);

//                        if (!CameraActivity.PreRecordFilters)
//                            filterLayout.setVisibility(View.GONE);
                    } else {
                        rootView.findViewById(R.id.flashLayout).setVisibility(View.GONE);
//                        filterLayout.setVisibility(View.GONE);
                    }
                }
            });
        } else if (event instanceof BroadCastSignals.FilterChangeSignal) {
            BroadCastSignals.FilterChangeSignal mEvent = (BroadCastSignals.FilterChangeSignal) event;
            if (this.mController != null && mEvent.getFilter() != null) {
                this.mController.setNewFilter(mEvent.getFilter());
            }

        } else if (event instanceof BroadCastSignals.RestartBroadCastSignal) {

            BroadCastSignals.RestartBroadCastSignal mEvent = (BroadCastSignals.RestartBroadCastSignal) event;
            //  TODO LATER get reason param and make corresponding API calls before starting streaming.
            if (mController != null) {
                mController.restartController(mEvent);
            }

        } else if (event instanceof BroadCastSignals.ErrorSignal) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Goes back safely to dashboard
                    Toast.makeText(getActivity(), "Sorry, something went wrong with your camera. Please try again!", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (event instanceof BroadCastSignals.DoubleShotSignal) {
            BroadCastSignals.DoubleShotSignal mEvent = (BroadCastSignals.DoubleShotSignal) event;
            boolean enable = mEvent.getEnable();
            if (enable && !mDoubleShotEnabled)
                toggleDoubleShotCapture();
            else if (!enable && mDoubleShotEnabled)
                toggleDoubleShotCapture();
        } else if (event instanceof BroadCastSignals.FocusSignal) {
            BroadCastSignals.FocusSignal mEvent = (BroadCastSignals.FocusSignal) event;

            focusHandler.removeCallbacksAndMessages(null);
            focusPointer.setVisibility(View.GONE);

            focusPointer.setX(mEvent.x - (AppLibrary.convertDpToPixels(context, 80) / 2)); //Since the Rect is fixed
            focusPointer.setY(mEvent.y - (AppLibrary.convertDpToPixels(context, 80) / 2)); //Since the Rect is fixed
            focusPointer.setAngle(0);
            focusPointer.setVisibility(View.VISIBLE);
            FocusAnimation animation = new FocusAnimation(focusPointer, 360);
            animation.setDuration(250);
            focusPointer.startAnimation(animation);

            focusHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    focusPointer.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            viewControlCallback = (ViewControlsCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString() + " must implement ViewControlCallback");
        }
//        recentPictureList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.camera_fragment, container, false);
        if (savedInstanceState != null)
            return rootView;

        initializeViewObjects(rootView);

        try {
            fromIntent = getArguments().getBoolean("fromIntent");
            if (fromIntent)
                setVisibility(false); //If its from the intent, set camera visibility as false
        } catch (Exception e) {
            fromIntent = false;
        }

//        rootView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                rootView.getParent().requestDisallowInterceptTouchEvent(true);
//                return false;
//            }
//        });
        mDoubleShotEnabled = false;
        mDoubleShotBackOngoing = true;
        return rootView;
    }

    private Toast customToastTut;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null)
            return;

        if (getPreferences().getInt(AppLibrary.FIRST_TIME_TUTORIAL, 0) == 0) {
            final ImageView tutorial = (ImageView) rootView.findViewById(R.id.tutorialImage);
            tutorial.setImageResource(R.drawable.tutorial);
            tutorial.setVisibility(View.VISIBLE);
            tutorial.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tutorial.setVisibility(View.GONE);
                    boolean b = showXiaomiPopUp(rootView);
                    if (!b)
                        if (!AppLibrary.IS_CAMERA_STICKER_SHOWN_IN_THIS_SESSION)
                            initTuts();
                }
            });

            getPreferences().edit().putInt(AppLibrary.FIRST_TIME_TUTORIAL, 1).commit();
        } else {
            boolean b = showXiaomiPopUp(rootView);
            if (!b)
                if (!AppLibrary.IS_CAMERA_STICKER_SHOWN_IN_THIS_SESSION)
                    initTuts();
        }
    }

    private void initTuts() {
        AppLibrary.IS_CAMERA_STICKER_SHOWN_IN_THIS_SESSION = true;
        int gravity = Gravity.BOTTOM | Gravity.CENTER;
        int previousCount = getPreferences().getInt(AppLibrary.CAMERA_TUTORIAL, 0);
        int maxCount = AppLibrary.CAMERA_TUTORIAL_COUNT;

        Log.d(TAG, "initTuts: prev " + previousCount + " max " + maxCount);

        if (previousCount <= maxCount && mVisible) {
            customToastTut = showCustomToast(getActivity(), R.layout.tut1, gravity, 0, 100, 5000);
            getPreferences().edit().putInt(AppLibrary.CAMERA_TUTORIAL, ++previousCount).apply();
        }
    }

    public View getRootView() {
        return rootView.findViewById(R.id.revealView);
    }

    @Override
    public void setFaceBounds(int x, int y, int width, int height) {
        faceX = x;
        faceY = y;
        faceWidth = width;
        faceHeight = height;
    }

    @Override
    public void setLeftEyePosition(int x, int y) {
        leftEyeX = x;
        leftEyeY = y;
    }

    @Override
    public void setRightEyePosition(int x, int y) {
        rightEyeX = x;
        rightEyeY = y;
    }

    @Override
    public void setRightEarPosition(int x, int y) {
        rightEarX = x;
        rightEarY = y;
    }

    @Override
    public void setLeftEarPosition(int x, int y) {
        leftEarX = x;
        leftEarY = y;
    }

    @Override
    public void setLeftCheekPosition(int x, int y) {
        leftCheekX = x;
        leftCheekY = y;
    }

    @Override
    public void setRightCheekPosition(int x, int y) {
        rightCheekX = x;
        rightCheekY = y;
    }

    @Override
    public void setNosePosition(int x, int y) {
        noseX = x;
        noseY = y;
    }

    @Override
    public void setLeftMouthPosition(int x, int y) {
        leftMouthX = x;
        leftMouthY = y;
    }

    @Override
    public void setRightMouthPosition(int x, int y) {
        rightMouthX = x;
        rightMouthY = y;
    }

    @Override
    public void setBottomMouthPosition(int x, int y) {
        bottomMouthX = x;
        bottomMouthY = y;
    }

    @Override
    public void updateFacePos(float yaw, float roll, float smileProbability) {
        this.yaw = yaw;
        this.roll = roll;
        this.smileProbability = smileProbability;
        imageView.postInvalidate();
    }

    @Override
    public void updateFacePos() {
        imageView.postInvalidate();
    }

    ImageView brushIv;
    ImageView textIv;
    public LinearLayout backButton;

    private void initializeViewObjects(View rootView) {
        unSeenCountTextView = (CircularTextView) rootView.findViewById(R.id.unSeenCountTextView);
        unSeenCountTextView.setSolidColor("#9900ff");
        cameraSwapButton = (ImageView) rootView.findViewById(R.id.cameraButton);
        cameraSwapButton.setTag(BACK);
        flashLayout = (ImageView) rootView.findViewById(R.id.flashLayout);
        cameraSwapButton.setOnClickListener(this);
        flashLayout.setOnClickListener(this);
        backButton = (LinearLayout) rootView.findViewById(R.id.backButton);
        backButton.setOnClickListener(this);

        brushIv = (ImageView) rootView.findViewById(R.id.brushButton);
        textIv = (ImageView) rootView.findViewById(R.id.writeTextButton);
        brushIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCustomToast(customToastTut);
                CanvasFragment canvasFragment = getCanvasFragment();
                if (canvasFragment != null) {
                    if (canvasFragment.isColorPickerOpen()) {
                        notifyFocus(false);
                    } else {
                        notifyFocus(true);
                    }
                    canvasFragment.toggleColorPicker();
                    canvasFragment.editText.clearFocus();
                    canvasFragment.editText.setCursorVisible(false);
                    canvasFragment.hideKeyboard();
                }
            }
        });
        textIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCustomToast(customToastTut);
                CanvasFragment canvasFragment = getCanvasFragment();
                if (canvasFragment != null) {
                    if (canvasFragment.isColorPickerOpen())
                        canvasFragment.toggleColorPicker();
                    if (canvasFragment.editText.getText().toString().length() <= 0) {
                        canvasFragment.editText.requestFocus();
                        canvasFragment.showKeyBoard();
                        notifyFocus(true);
                    } else {
                        canvasFragment.editText.clearFocus();
                        canvasFragment.editText.setCursorVisible(false);
                        canvasFragment.hideKeyboard();
                        notifyFocus(false);
                    }

                    canvasFragment.toggleEditTextProperties();
                }
            }
        });
        imageView = (MyImageView) rootView.findViewById(R.id.imageview);
        //  onRecordVideoFragmentLoad();
        previewImage = (ImageView) rootView.findViewById(R.id.previewImage);
        recordButton = (ImageView) rootView.findViewById(R.id.recordButton);
//        rootView.findViewById(R.id.surfaceView).setVisibility(View.VISIBLE);

        doubleShotScene = (LinearLayout) rootView.findViewById(R.id.doubleshot_container);

        topImageView = (ImageView) rootView.findViewById(R.id.doubleshot_top);
        bottomImageView = (ImageView) rootView.findViewById(R.id.doubleshot_bottom);

        rootView.findViewById(R.id.doubleShot_button).setOnClickListener(this);
        recordButton.setOnTouchListener(mRecordButtonTouchListener);
        rootView.findViewById(R.id.filterLayout).setOnClickListener(this);
        rootView.findViewById(R.id.canvasButton).setOnClickListener(this);
        rootView.findViewById(R.id.galleryOverlay).setOnClickListener(this);

        mRecordButtonGestureListener = new GestureDetector(getContext(), new CustomGestureListener());

        filterNameTV = (TextView) rootView.findViewById(R.id.filterNameTV);
        filterNameTV.setTypeface(fontPicker.getMontserratRegular());
        focusPointer = (AnimatedCircleView) rootView.findViewById(R.id.focus_pointer);

        timeLineView = (ColoredTimelineView) rootView.findViewById(R.id.timeLineView);

        if (mController == null) {
            preview = (GLSurfaceView) viewControlCallback.getCameraView();
            if (preview == null)
                return;
            mController = new StreamingActionControllerKitKat(getActivity(), preview, (TextView) rootView.findViewById(R.id.textMessage));
            ((StreamingActionControllerKitKat) mController).setFaceUpdateListener(this);
        }
        mController.setCameraControlCallback(this);
        mController.setFilterControlCallback(this);
        mController.setDoubleShotCallback(this);
//        flashImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeFlashMode(v);
//            }
//        });
        seekBar1 = (SeekBar) rootView.findViewById(R.id.seekbar1);
        seekBar2 = (SeekBar) rootView.findViewById(R.id.seekbar2);
        seekBar3 = (SeekBar) rootView.findViewById(R.id.seekbar3);

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((StreamingActionControllerKitKat) mController).set3DPosX((float) progress / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((StreamingActionControllerKitKat) mController).set3DPosY((float) progress / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((StreamingActionControllerKitKat) mController).set3DPosZ((float) progress / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void updateUnSeenItemsCount(int unSeenCount) {
        if (rootView == null) return; //If rootview hasn't inflated by then
        unseenItemCount = unSeenCount;
        unSeenCountTextView = (CircularTextView) rootView.findViewById(R.id.unSeenCountTextView);
        if (unseenItemCount > 0) {
            unSeenCountTextView.setText(String.valueOf(unSeenCount));
            unSeenCountTextView.setVisibility(View.VISIBLE);
        } else {
            unSeenCountTextView.setVisibility(View.GONE);
        }
    }

    private void animateThemeName(String filterName) {

        filterNameTV.setText(filterName);
        filterNameTV.setVisibility(View.VISIBLE);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(100);
        fadeOut.setDuration(700);

        AnimationSet animation = new AnimationSet(false); //change to false
        // animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        filterNameTV.setAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                filterNameTV.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


//    private void showFilters() {
//        if (getChildFragmentManager().findFragmentByTag("filter_frag") != null) {
//            openFilters();
//        }
//    }

//    private void openFilters() {
//        if (animateRunning) return;
//        animateRunning = true;
//        rootView.findViewById(R.id.filters_overlay).setVisibility(View.VISIBLE);
//        rootView.findViewById(R.id.filter_fragments_container).setVisibility(View.VISIBLE);
//        TranslateAnimation mAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);
//        mAnim.setDuration(AppLibrary.FILTERS_SLIDER_ANIMATION_DURATION);
//        mAnim.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            public void onAnimationEnd(Animation animation) {
//                animateRunning = false;
//            }
//
//            public void onAnimationRepeat(Animation animation) {
//            }
//        });
//        rootView.findViewById(R.id.filter_fragments_container).startAnimation(mAnim);
//        filtersOpen = true;
//        mFilterFragment.renderFilterThumbnails();
//        mFilterFragment.makeVisible(true);
//    }

//    private void closeFilters() {
//        stopFilterThumbnail();
//        if (animateRunning) return;
//        animateRunning = true;
//        TranslateAnimation mAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f);
//        mAnim.setDuration(AppLibrary.FILTERS_SLIDER_ANIMATION_DURATION);
//        mAnim.setAnimationListener(new Animation.AnimationListener() {
//            public void onAnimationStart(Animation animation) {
//            }
//
//            public void onAnimationEnd(Animation animation) {
//                rootView.findViewById(R.id.filter_fragments_container).setVisibility(View.GONE);
//                rootView.findViewById(R.id.filters_overlay).setVisibility(View.GONE);
//                animateRunning = false;
//            }
//
//            public void onAnimationRepeat(Animation animation) {
//            }
//        });
//        rootView.findViewById(R.id.filter_fragments_container).startAnimation(mAnim);
//        filtersOpen = false;
//        mFilterFragment.makeVisible(false);
//    }

    @Override
    public void notifyFocus(boolean hasFocus) {
        CanvasFragment canvasFragment = getCanvasFragment();
        if (canvasFragment != null) {
            if (canvasFragment.isCanvasLocked() || hasFocus) {
                viewControlCallback.getParentViewPager().setCanSwipe(false);
                if (hasFocus) {
                    if (canvasFragment.getCloseButtonReference().getVisibility() == View.VISIBLE)
                        canvasFragment.getCloseButtonReference().setVisibility(View.GONE);
                    if (rootView.findViewById(R.id.backButton).getVisibility() == View.GONE)
                        rootView.findViewById(R.id.backButton).setVisibility(View.VISIBLE);
                } else {
                    if (canvasFragment.getCloseButtonReference().getVisibility() == View.GONE)
                        canvasFragment.getCloseButtonReference().setVisibility(View.VISIBLE);
                    if (rootView.findViewById(R.id.backButton).getVisibility() == View.VISIBLE)
                        rootView.findViewById(R.id.backButton).setVisibility(View.GONE);
                }

                if (canvasFragment.isCanvasLocked())
                    toggleUnseenTextViewVisibility(true);

                if (canvasFragment.isCanvasLocked() && CanvasFragment.getContributableStreams) {
                    if (AroundYouFragment.knownLocation == null) //This known location in AroundYouFragment is the last known location within the app
                       // ((CameraActivity) getActivity()).fetchContributableMoments(null, null);
                   // else
                      //  ((CameraActivity) getActivity()).fetchContributableMoments(AroundYouFragment.knownLocation.getLatitude(), AroundYouFragment.knownLocation.getLongitude());

                    CanvasFragment.getContributableStreams = false;
                }
            } else {
                if (canvasFragment.isCanvasLocked()) {
                    if (rootView.findViewById(R.id.backButton).getVisibility() == View.VISIBLE)
                        rootView.findViewById(R.id.backButton).setVisibility(View.GONE);
                    if (canvasFragment.getCloseButtonReference().getVisibility() == View.GONE)
                        canvasFragment.getCloseButtonReference().setVisibility(View.VISIBLE);
                } else {
                    if (canvasFragment.getCloseButtonReference().getVisibility() == View.VISIBLE)
                        canvasFragment.getCloseButtonReference().setVisibility(View.GONE);
                    if (rootView.findViewById(R.id.backButton).getVisibility() == View.GONE)
                        rootView.findViewById(R.id.backButton).setVisibility(View.VISIBLE);
                    toggleUnseenTextViewVisibility(false);
                }
                viewControlCallback.getParentViewPager().setCanSwipe(true);
            }
            viewControlCallback.getParentViewPager().setEditingDone(!hasFocus || !canvasFragment.isCanvasLocked());
        }
    }

    @Override
    public void onHideCanvasFragment() {
        viewControlCallback.onCanvasFragmentHide(true);
        updateCameraOptions();
        resumeCameraPreview();
    }

    @Override
    public void toggleUnseenTextViewVisibility(boolean hide) {
        if (hide || unseenItemCount <= 0) {
            unSeenCountTextView.setVisibility(View.GONE);
        } else {
            unSeenCountTextView.setVisibility(View.VISIBLE);
        }
    }


    public void onHideCanvasFragmentWithoutCameraPreview() {
        viewControlCallback.onCanvasFragmentHide(false);
        updateCameraOptions();
    }

    private void updateCameraOptions() {
        if (unseenItemCount > 0)
            unSeenCountTextView.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.topLayerLayout).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.cameraControlsLayout).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.textOptionLayout).setVisibility(View.GONE);
    }

    @Override
    public void launchShareFragment(String absolutePath, String s) {
        viewControlCallback.launchShareFragment(absolutePath, s);
    }

    @Override
    public void uploadMediaToFireBase(boolean facebookPost, boolean postAnonymous, int action_type, HashMap<String, String> selectedRoomsForMoment, String mediaPath, HashMap<String, Integer> momentList, HashMap<String, Integer> roomList, int expiryType, String mediaText) {
        viewControlCallback.uploadMediaToFireBase(facebookPost, postAnonymous, action_type, selectedRoomsForMoment, mediaPath, momentList, roomList, expiryType, mediaText, null);

    }

//    private void launchVideoEditorActivity(String path) {
//        Intent intent = new Intent(getActivity(), VideoEditorActivity.class);
//        intent.putExtra(AppLibrary.SELECTED_MEDIA_PATH, path);
//        startActivity(intent);
//    }

    private void launchVideoEditorActivityWithBitmap(Bitmap capturedBitmap) {
        CapturedMediaController.getInstance().setCapturedBitmap(capturedBitmap);
        Intent intent = new Intent(getActivity(), VideoEditorActivity.class);
        intent.putExtra(AppLibrary.CAPTURED_BITMAP, true);
        startActivity(intent);
    }

    public void toggleDoubleShotCapture() {
        mDoubleShotEnabled = !mDoubleShotEnabled;
        mDoubleShotBackOngoing = true;

        ((StreamingActionControllerKitKat) mController).enableDoubleShot(mDoubleShotEnabled);

        animatePreview();
    }

    public void translatePreview() {
        translatePreview(0);
    }

    private void animatePreview() {
        translatePreview(300);

        if (mDoubleShotEnabled && mDoubleShotBackOngoing) {
            animateThemeName("Double Shot");
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.DOUBLE_SHOT);
        }
    }

    private void translatePreview(int durationms) {
        float mTranslateY = 0.0f;
//        mTranslateY = (mDoubleShotEnabled == mDoubleShotBackOngoing) ? 0.25f : -0.25f;
        if (mDoubleShotEnabled) {
            if (mDoubleShotBackOngoing) {
                mTranslateY = -0.25f;
            } else {
                mTranslateY = 0.25f;
            }
        } else {
            mTranslateY = 0.0f;
        }

        mTranslateY *= AppLibrary.getDeviceParams(getActivity(), "height");
        ObjectAnimator translator = ObjectAnimator.ofFloat(preview, "translationY", mTranslateY);
        translator.setDuration(durationms);
        AnimatorSet translatorAnimatorSet = new AnimatorSet();
        translatorAnimatorSet.play(translator);
        translatorAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
/*                if(mDoubleShotEnabled) {*/
                doubleShotPreviewMakeVisible(true);
/*                }*/
            }

            @Override
            public void onAnimationEnd(Animator animation) {
/*                if(!mDoubleShotEnabled) {*/
                doubleShotPreviewMakeVisible(false);
/*                }*/
/*                doubleShotScene.setVisibility(mDoubleShotEnabled ? View.VISIBLE : View.GONE);
                topImageView.setVisibility(mDoubleShotEnabled ? View.VISIBLE : View.GONE);
                bottomImageView.setVisibility(mDoubleShotEnabled ? View.VISIBLE : View.GONE);*/
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        translatorAnimatorSet.start();
    }

    private void doubleShotPreviewMakeVisible(boolean before) {

        if (mDoubleShotEnabled) {
            rootView.findViewById(R.id.canvasButton).setVisibility(View.GONE);
            rootView.findViewById(R.id.filterLayout).setVisibility(View.GONE);

            rootView.findViewById(R.id.galleryTV).setVisibility(View.GONE);
            rootView.findViewById(R.id.textTV).setVisibility(View.GONE);

            rootView.findViewById(R.id.doubleShot_button).setVisibility(View.GONE);
            viewControlCallback.setViewpagerSwipable(false);

            doubleShotScene.setVisibility(View.VISIBLE);
            topImageView.setVisibility(View.VISIBLE);
            bottomImageView.setVisibility(View.VISIBLE);
            topImageView.setImageResource(R.drawable.transparent_background);
            if (mDoubleShotBackOngoing) {
                topImageView.setBackgroundResource(R.drawable.transparent_background);
//                topImageView.setVisibility(View.INVISIBLE);
                bottomImageView.setBackgroundResource(R.drawable.gradient_fill);
            } else {
                topImageView.setImageBitmap(doubleShotCaptureTopBitmap);
                if (before) {
                    bottomImageView.setBackgroundResource(R.drawable.gradient_fill);
                } else {
                    bottomImageView.setBackgroundResource(R.drawable.transparent_background);
                }
            }
        } else {
            topImageView.setVisibility(before ? View.VISIBLE : View.GONE);
            bottomImageView.setVisibility(before ? View.VISIBLE : View.GONE);
            if (before) {
                topImageView.setBackgroundResource(R.drawable.transparent_background);
            } else {
                topImageView.setBackgroundResource(R.drawable.gradient_fill);
            }
            bottomImageView.setBackgroundResource(R.drawable.gradient_fill);
            doubleShotScene.setVisibility(before ? View.VISIBLE : View.GONE);

            rootView.findViewById(R.id.canvasButton).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.filterLayout).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.doubleShot_button).setVisibility(View.VISIBLE);

            rootView.findViewById(R.id.galleryTV).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.textTV).setVisibility(View.VISIBLE);

            viewControlCallback.setViewpagerSwipable(true);
        }
    }

    GestureDetector mRecordButtonGestureListener;

    View.OnTouchListener mRecordButtonTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            cancelCustomToast(customToastTut);
//          boolean touchResult = true; // To be set while handling various touch events.
            mRecordButtonGestureListener.onTouchEvent(event);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    onRecordVideoStopped();
                    break;
                default:
//                    event.setLocation(event.getX(), event.getY());
                    handleTouchEvent(event); // Pass touch events while recording
                    break;
            }
            return true;
        }
    };

    @Override
    public void onSwitchCamera() {
        switchCameraClicked();
    }

    final String FRONT = "front", BACK = "back";

    @Override
    public void afterSwitchCamera() {
        cancelCustomToast(customToastTut);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = (ImageView) rootView.findViewById(R.id.cameraButton);
                if (imageView != null) {
                    String tag = (String) imageView.getTag();
                    if (tag.equals(BACK)) {
                        imageView.setTag(FRONT);
                    } else {
                        imageView.setTag(BACK);
                    }
                }
            }
        });
    }

    @Override
    public void onHandleFilterThumbnail(Bitmap bitmap, int filter) {
        handleFilterThumbnail(bitmap, filter);
    }

    public void onFlashClickTriggered(View v) {
        changeFlashMode(v);
    }

    public void onCameraClicked(View v) {
        switchCamera();
    }

    public void setVisibility(boolean visible) {
        mVisible = visible;
    }

    public void handleTouchEvent(MotionEvent event) {
        try {
            preview.dispatchTouchEvent(event);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    public void filterSwipeLeft() {
        mController.decrementFilter();
    }

    public void filterSwipeRight() {
        mController.incrementFilter();
    }

    public void removeTuts() {
        cancelCustomToast(customToastTut);
    }

    private class CustomGestureListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            onImageCaptureRequest();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            checkAndStartVideo();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    public void checkAndStartVideo() {
        if (!mDoubleShotEnabled) {
            onRecordVideoStarted();
        } else {
            showCustomToast(getActivity(), R.layout.only_text_toast, Gravity.CENTER, 0, 0, 3000, "Tap to click a picture in double-shot mode");
        }
    }

    public void updateTimeLine(long elapsedTime) {
        float elapsedPercentage = (float) elapsedTime / (15 * 1000);
        timeLineView.update(elapsedPercentage);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callMethodsToStartCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
//        translatePreview(); //Being called from onBackStackChanged
        resetFragmentViews();
        if (mVisible) { //Will not open camera if its from intent
            Log.d(TAG, "onResume, open camera");
            openCamera();

            if (!mDoubleShotEnabled) //Kept here so that we don't have to mess around with the Canvas fragment flow
                viewControlCallback.setViewpagerSwipable(true);
        }
    }

    public void resetFragmentViews() {
        rootView.findViewById(R.id.galleryOverlay).setVisibility(View.GONE);
        rootView.findViewById(R.id.imageGallery).setVisibility(View.GONE);
        getActivity().findViewById(R.id.topLayerLayout).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.cameraButton).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.backButton).setVisibility(View.VISIBLE);
        // Flash is being set in the camerapreviewstarted signal if hidden (for video)

        if (!mDoubleShotEnabled) {
            if (doubleShotScene != null)
                doubleShotScene.setVisibility(View.GONE);

            ((ImageView) rootView.findViewById(R.id.doubleshot_bottom)).setImageBitmap(null);
            ((ImageView) rootView.findViewById(R.id.doubleshot_top)).setImageBitmap(null);

            getActivity().findViewById(R.id.doubleShot_button).setVisibility(View.VISIBLE);
            updateBottomLayout();

            cleanUp();
        }

        if (timeLineView != null) {
            timeLineView.update(0);
            timeLineView.setVisibility(View.GONE);
        }

        if (previewImage != null) {
            previewImage.setVisibility(View.GONE);
            previewImage.setImageBitmap(null);
        }
    }

    private void cleanUp() {
        if (doubleShotCaptureTopBitmap != null) {
            doubleShotCaptureTopBitmap.recycle();
            doubleShotCaptureTopBitmap = null;
        }

        if (doubleShotCaptureBottomBitmap != null) {
            doubleShotCaptureBottomBitmap.recycle();
            doubleShotCaptureBottomBitmap = null;
        }

        // Don't recycle final generated bitmap yet.
        // Let VideoEditorFragment handle cleaning up after it's done using.
/*        if (doubleShotCaptureBitmap!=null) {
            doubleShotCaptureBitmap.recycle();
            doubleShotCaptureBitmap = null;
        }*/
    }

    public void updateBottomLayout() {
        rootView.findViewById(R.id.canvasButton).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.filterLayout).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.bottomLayout).setVisibility(View.VISIBLE);

        rootView.findViewById(R.id.galleryTV).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.textTV).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelCustomToast(customToastTut);
        releaseCamera();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void releaseCamera() {
        if (mController != null)
            mController.pauseView();
    }

    private void openCamera() {
        try {
            if (mController != null)
                mController.resumeView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void pauseCameraPreview() {
        if (mController == null) return;
        ((StreamingActionControllerKitKat) mController).pauseCameraPreview();

        if (mVisible) {
            setVisibility(false);
        }
    }


    public void resumeCameraPreview() {
        if (mController == null) return;

        ((StreamingActionControllerKitKat) mController).resumeCameraPreview(); //Also starts camera if not already started

        if (!mVisible) {
            setVisibility(true);
        }

        resetFragmentViews();
        translatePreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mController != null) {

            if (this.mController.isPublishing())
                this.mController.actionPublish();

            this.mController.destroy();
            this.mController = null;
        }

        if (cursorHandler != null)
            cursorHandler.removeCallbacksAndMessages(null);

        if (cursorThread != null)
            cursorThread.interrupt();

        if (cursor != null) {
            cursor = null;
        }

        cleanUp();
    }

    @Override
    public void onClick(View v) {
        cancelCustomToast(customToastTut);
        switch (v.getId()) {
            case R.id.recordButton:
//                onRecordVideoStarted();
                break;
            case R.id.filterLayout:
                requestStartCursorThread();
                break;
            case R.id.doubleShot_button:
                toggleDoubleShotCapture();
                break;
            case R.id.canvasButton:
                CanvasFragment fragment = getCanvasFragment();
                if (fragment == null)
                    launchCanvasFragment();
                else
                    fragment.startRevealAnimation();
                viewControlCallback.onLaunchCanvasFragment();
                toggleUnseenTextViewVisibility(false);
                pauseCameraPreview();
                break;
            case R.id.galleryOverlay:
                hideRecentImagesViewLayout();
                break;
            case R.id.backButton:
                CanvasFragment canvasFragment = getCanvasFragment();
                if (((CameraActivity) getActivity()).isCanvasFragmentActive() && canvasFragment != null && canvasFragment.isCanvasLocked()) {
                    canvasFragment.undoAction();
                } else {
                    if (mDoubleShotEnabled) {
                        toggleDoubleShotCapture();
                        return;
                    }

                    viewControlCallback.getParentViewPager().setCanSwipe(true);

                    if (canvasFragment != null) {
                        canvasFragment.undoEditChanges();
                    }
                    if (((CameraActivity) getActivity()).isPublicStreamContributionMode()) {
                        ((CameraActivity) getActivity()).goBackToStream();
                    } else
                        viewControlCallback.getParentViewPager().setCurrentItem(0);
                }
                break;
            case R.id.flashLayout:
                onFlashClickTriggered(v);
                break;
            case R.id.cameraButton:
                onCameraClicked(v);
                break;
            default:
                break;
        }
    }

    private void startCursorThread() {
        cursorThread = new HandlerThread("imageCursor");
        cursorThread.start();
        cursorHandler = new Handler(cursorThread.getLooper());
        cursorHandler.post(new Runnable() {
            @Override
            public void run() {
                showRecentImagesFromGallery();
            }
        });
    }

    private void requestStartCursorThread() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_ACCESS_EXTERNAL_STORAGE);
        } else {
            startCursorThread();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCursorThread();
                } else {
                    showShortToastMessage(" Please provide access to show recent images");
                }
                break;
        }
    }

    private CanvasFragment getCanvasFragment() {
        CanvasFragment fragment = (CanvasFragment) getChildFragmentManager().findFragmentByTag("canvasFragment");
        return fragment;
    }

    private void launchCanvasFragment() {
        getChildFragmentManager().beginTransaction()
                .add(R.id.cameraFragmentContainer, new CanvasFragment(), "canvasFragment")
                .commitAllowingStateLoss();
    }

    private void showRecentImagesFromGallery() {
        if (recentPictureList == null)
            recentPictureList = new ArrayList<>();
        else recentPictureList.clear();

        long DAY = mFireBaseHelper.getMediaExpiryTime() * 60 * 60 * 1000;
        final int imageDimension = AppLibrary.convertDpToPixels(getActivity(), 80);
        String[] projection = new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.MIME_TYPE};
        cursor = getActivity().managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor != null && cursor.getCount() > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cursor.moveToFirst();
            int maxCount = cursor.getCount() > 100 ? 100 : cursor.getCount(); //Go through the last 100 pictures - Some of them might be screenshots to skip
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((LinearLayout) rootView.findViewById(R.id.filter_fragments_container)).removeAllViews();
                }
            });
            int j = 0;
            for (int i = 0; i < maxCount; i++) {
                long takenTime;
                if (cursor != null)
                    takenTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN));
                else
                    break;

                long currentTime = System.currentTimeMillis();
                if (currentTime - takenTime <= DAY) {
                    final String path;
                    if (cursor != null)
                        path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    else
                        break;

                    File file = new File(path);
                    if (!file.exists() || path.endsWith(".PNG") || path.endsWith(".png")) { //Skip PNG images for now - Contains screenshots + Has bug
                        if (cursor != null) {
                            cursor.moveToNext();
                            continue;
                        } else
                            break;
                    }
                    recentPictureList.add(path);

                    final ImageView imageView = new ImageView(getActivity());
                    int imageMargin = AppLibrary.convertDpToPixels(getActivity(), 3);
                    params.setMargins(imageMargin, imageMargin, 0, imageMargin);
                    imageView.setId(j);
                    imageView.setLayoutParams(params);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String path = recentPictureList.get(v.getId());
                            hideRecentImagesViewLayout();
                            if (cursorHandler != null)
                                cursorHandler.removeCallbacksAndMessages(null);

                            if (cursorThread != null)
                                cursorThread.interrupt();

                            if (cursor != null) {
                                cursor = null;
                            }
                            viewControlCallback.launchVideoEditorFragment(path);
                        }
                    });

                    Picasso.Builder builder = new Picasso.Builder(getActivity());
                    builder.listener(new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                            Log.d(TAG, "Image loading failed");
                            exception.printStackTrace();
                        }
                    });

                    if (pic == null)
                        pic = builder.build();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pic.load(new File(path))
                                    .resize(imageDimension, imageDimension).centerCrop().into(imageView);
                            ((LinearLayout) rootView.findViewById(R.id.filter_fragments_container)).addView(imageView);
                            rootView.findViewById(R.id.galleryOverlay).setVisibility(View.VISIBLE);
                            rootView.findViewById(R.id.imageGallery).setVisibility(View.VISIBLE);
                            rootView.findViewById(R.id.bottomLayout).setVisibility(View.GONE);
                            rootView.findViewById(R.id.galleryTV).setVisibility(View.GONE);
                            rootView.findViewById(R.id.textTV).setVisibility(View.GONE);
                        }
                    });

                    j++;
                }

                if (cursor != null)
                    cursor.moveToNext();
                else
                    break;
            }

//            cursor.close();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recentPictureList.size() > 0) {

                        RelativeLayout relativeLayout = new RelativeLayout(context);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(imageDimension, imageDimension);
                        int imageMargin = AppLibrary.convertDpToPixels(getActivity(), 3);
                        relativeLayout.setPadding(imageMargin, 0, 0, 0);
                        relativeLayout.setLayoutParams(params);

                        ImageView imageView = new ImageView(getActivity());
                        imageView.setBackgroundColor(getResources().getColor(R.color.black_overlay_light));
                        RelativeLayout.LayoutParams lprela = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        imageView.setLayoutParams(lprela);
                        relativeLayout.addView(imageView);

                        TextView textView = new TextView(getActivity());
                        textView.setTypeface(fontPicker.getMontserratRegular());
                        textView.setText(mFireBaseHelper.getMediaExpiryTime() + " hrs\nago");
                        textView.setTextSize(14);
                        textView.setGravity(Gravity.CENTER);
                        textView.setTextColor(Color.WHITE);
                        RelativeLayout.LayoutParams lptext = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        lptext.addRule(RelativeLayout.CENTER_IN_PARENT);
                        textView.setLayoutParams(lptext);
                        relativeLayout.addView(textView);

                        ((LinearLayout) rootView.findViewById(R.id.filter_fragments_container)).addView(relativeLayout);

                    } else {
//                        Toast.makeText(getActivity(), "Sorry, no recent pictures in the last 24 hrs", Toast.LENGTH_SHORT).show();
                        cancelCustomToast(customToastTut);
                        showCustomToast(getActivity(), R.layout.only_text_toast, Gravity.CENTER, 0, 0, 2000, getString(R.string.no_pictures_in_hrs) + mFireBaseHelper.getMediaExpiryTime() + " hrs");
                    }
                }
            });
        }
    }

    private void hideRecentImagesViewLayout() {
        rootView.findViewById(R.id.imageGallery).setVisibility(View.GONE);
        rootView.findViewById(R.id.galleryOverlay).setVisibility(View.GONE);
        rootView.findViewById(R.id.bottomLayout).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.galleryTV).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.textTV).setVisibility(View.VISIBLE);

        if (cursorHandler != null)
            cursorHandler.removeCallbacksAndMessages(null);

        if (cursorThread != null)
            cursorThread.interrupt();

        if (cursor != null) {
            cursor = null;
        }
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) { //Not being used now
//        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
//                Uri selectedUri = data.getData();
//                String selectedMediaPath = getPath(selectedUri);
//                if (selectedMediaPath != null) {
//                    AppLibrary.log_d(TAG, "Selected media file path -" + selectedMediaPath);
//                    if (selectedMediaPath.endsWith(".mp4")) {
//                        viewControlCallback.launchVideoEditorFragment(selectedMediaPath);
//                    } else if (selectedMediaPath.endsWith(".jpg") || selectedMediaPath.endsWith(".jpeg")) {
//                        viewControlCallback.launchVideoEditorFragment(selectedMediaPath);
//                    } else {
//                        Toast.makeText(getActivity(), "Invalid Media File.", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        }
//    }

    public String getPath(Uri contentUri) {
        String res = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor.moveToFirst()) {
            ;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    /**
     * @param source   original bitmap to extract original width and height values
     * @param destRect container of the target bitmap and thus location specifier.
     * @param crop     true for centerCrop and false for centerInside
     * @return
     */
    public Rect scaleCenter(Bitmap source, Rect destRect, boolean crop) {
        int destWidth = destRect.width();
        int destHeight = destRect.height();

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) destWidth / sourceWidth;
        float yScale = (float) destHeight / sourceHeight;

        float scale;
        if (crop) {
            scale = Math.max(xScale, yScale);
        } else {
            scale = Math.min(xScale, yScale);
        }

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (destWidth - scaledWidth) / 2;
        float top = (destHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRectF = new RectF(left, top, left + scaledWidth, top + scaledHeight);
        targetRectF.offset(destRect.left, destRect.top);

        Rect targetRect = new Rect();
        targetRectF.roundOut(targetRect);
        return targetRect;
    }

    public Bitmap blurFillBackgound(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = (int) (width / mDesiredAspectRatio);

        Bitmap fillBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = (int) ((float) width / bitmapWidth * bitmapWidth);

        Canvas canvas = new Canvas(fillBitmap);

//        Rect rect = new Rect(0, (height-bitmapHeight)/2, width, (height+bitmapHeight)/2);
        Rect rect = scaleCenter(bitmap, new Rect(0, 0, width, height), false);

        canvas.drawBitmap(bitmap, null, rect, new Paint());

        Bitmap blurBitmap = BlurBuilder.blur(getActivity(), bitmap);

        bitmapWidth = blurBitmap.getWidth();
        bitmapHeight = blurBitmap.getHeight();

//        Rect fullRect = new Rect(0, (height-bitmapHeight)/2, width, (height+bitmapHeight)/2);
        Rect fullRect = scaleCenter(bitmap, new Rect(0, 0, width, height), true);

        canvas.drawBitmap(blurBitmap, null, fullRect, new Paint());
        return fillBitmap;
    }

    private void saveBitmap(Bitmap bitmap) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String name = date + "_" + today.hour + today.minute + today.second + "_doubleshot.jpg";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        File file = new File(dir, name);
        writeFile(bitmap, file);
    }

    public void writeFile(Bitmap bmp, File f) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ex) {
            }
        }
    }

    public void onRecordVideoStarted() {
        if (mRecordVideoOngoing) return;
        viewControlCallback.setViewpagerSwipable(false);
        mRecordVideoOngoing = true;
        recordStartTime = System.currentTimeMillis();
        timeLineView.setVisibility(View.VISIBLE);
        recordButton.setImageResource(R.drawable.play_svg_purple);
        getActivity().findViewById(R.id.backButton).setVisibility(View.GONE);
        getActivity().findViewById(R.id.doubleShot_button).setVisibility(View.GONE);
        getActivity().findViewById(R.id.flashLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.filterLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.canvasButton).setVisibility(View.GONE);
        timeLineHandler.postDelayed(updateTimeLineThread, 0);
        ((StreamingActionControllerKitKat) mController).startPublishing();

        rootView.findViewById(R.id.textTV).setVisibility(View.GONE);
        rootView.findViewById(R.id.galleryTV).setVisibility(View.GONE);
    }

    public void onRecordVideoStopped() {
        if (!mRecordVideoOngoing) return;
        mRecordVideoOngoing = false;
        recordButton.setImageResource(R.drawable.play_svg);
        timeLineHandler.removeCallbacksAndMessages(null);
        stopRecording();
    }

    private void stopRecording() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() { //Don't use any existing handlers as callbacks might be removed
///*                popTheStickerFragment();*/

        stopPublishingStream();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mController != null) {
                    mController.pauseView();
                }
            }
        }, 200);

        synchronized (mController.mRecorderLock) {
            try {
                mController.mRecorderLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewControlCallback.launchVideoEditorFragment(StreamingActionControllerKitKat.getLastRecordedFilepath());
                }
            }, 100);
        }
    }

    public void startPublishingStream() {
        if (!this.mController.isPublishing())
            actionToggle();
    }

    private void stopPublishingStream() {
        if (this.mController.isPublishing())
            actionToggle();
    }

    public void actionToggle() {
        //  this will start/stop streaming.
        if (mController != null)
            mController.actionPublish();
    }

    private void popTheStickerFragment() {
        if (mIsStickerFragmentPopped) return;
        mIsStickerFragmentPopped = true;
        try {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment fragment = fragmentManager.findFragmentByTag("ParentSticker");
            if (fragment != null)
                fragmentTransaction.remove(fragment).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onImageCaptureRequest() {
        if (mRecordVideoOngoing) return;
        Log.d(TAG, "Request to capture image");

        viewControlCallback.setViewpagerSwipable(false);

        if (mDoubleShotEnabled && mDoubleShotBackOngoing) {
            //User can still cancel the double shot
        } else {
            getActivity().findViewById(R.id.backButton).setVisibility(View.GONE);
            getActivity().findViewById(R.id.doubleShot_button).setVisibility(View.GONE);
            getActivity().findViewById(R.id.flashLayout).setVisibility(View.GONE);
            getActivity().findViewById(R.id.cameraButton).setVisibility(View.GONE);
        }

        rootView.findViewById(R.id.filterLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.canvasButton).setVisibility(View.GONE);

        rootView.findViewById(R.id.galleryTV).setVisibility(View.GONE);
        rootView.findViewById(R.id.textTV).setVisibility(View.GONE);

        ((StreamingActionControllerKitKat) mController).captureImage("dlkfj");
    }

    private void callMethodsToStartCamera() {
        if (mController != null) {
            mController.publishing = false;

//        this.mController.setStreamKey("720p", "720p"); //IMPORTANT - Can control camera opening resolution here!
//        setUpListeners();
            setUpController();

/*        try {
            this.mController.resumeView(); //ResumeView currently handles starting camera only - not recording
        } catch (Exception e) {
            e.printStackTrace();
            //Handle exception and shift them to dashboard
            return;
        }*/

//        launchStickerFragment();
        }
    }

    public void setUpController() {
        this.mController.prepare();
        mFilter = this.mController.getFilter();
    }

    public void switchCamera() {
        switchCameraClicked();
    }

    private void changeFlashMode(View flashIconView) {
        if (mFilter != null) {
            String flashMode = mFilter.rotateFlashMode();
            if (flashMode.equals("off")) {
                rootView.findViewById(R.id.flashLayout).setBackgroundResource(R.drawable.clear_flash);
            } else if (flashMode.equals("on") || flashMode.equals("torch")) {
                rootView.findViewById(R.id.flashLayout).setBackgroundResource(R.drawable.flash);
            }
            MasterClass.getEventBus().post(new BroadCastSignals.FilterChangeSignal(mFilter));
        }
    }

    public void switchCameraClicked() {
        if (mController != null) {
            mController.restartController(new BroadCastSignals.RestartBroadCastSignal(null, true));
        }
    }

    public interface ViewControlsCallback {

        void launchVideoEditorFragment(Bitmap bitmap);

        void launchVideoEditorFragment(String uri);

        void onLaunchCanvasFragment();

        void setViewpagerSwipable(boolean enable);

        CustomViewPager getParentViewPager();

        GLSurfaceView getCameraView();

        void onCanvasFragmentHide(boolean cameraActive);

        void launchShareFragment(String absolutePath, String s);

        void uploadMediaToFireBase(boolean facebookPost, boolean postAnonymous, int action_type, HashMap<String, String> selectedRoomsForMoment, String mediaPath, HashMap<String, Integer> momentList
                , HashMap<String, Integer> roomList, int expiryType, String mediaText, CustomMomentModel publicStreamContribution);

    }

    int widthHeightOfCanvasButton[] = new int[2];
    int widthHeightOfCanvasText[] = new int[2];

    /**
     * @param enterContributionMode we wont toggle the visibility here as it is already messed upon with by
     *                              double shot n shit
     */
    public void togglePublicContributionMode(boolean enterContributionMode) {
        if (rootView == null) return;

        View canvasBtn = rootView.findViewById(R.id.canvasButton);

        if (widthHeightOfCanvasButton[0] == 0)
            widthHeightOfCanvasButton[0] = canvasBtn.getLayoutParams().width;
        if (widthHeightOfCanvasButton[1] == 0)
            widthHeightOfCanvasButton[1] = canvasBtn.getLayoutParams().height;

        View canvasText = rootView.findViewById(R.id.textTV);
        if (widthHeightOfCanvasText[0] == 0)
            widthHeightOfCanvasText[0] = canvasText.getLayoutParams().width;
        if (widthHeightOfCanvasText[1] == 0)
            widthHeightOfCanvasText[1] = canvasText.getLayoutParams().height;

        canvasBtn.getLayoutParams().width = enterContributionMode ? 0 : widthHeightOfCanvasButton[0];
        canvasBtn.getLayoutParams().height = enterContributionMode ? 0 : widthHeightOfCanvasButton[1];

        canvasText.getLayoutParams().width = enterContributionMode ? 0 : widthHeightOfCanvasText[0];
        canvasText.getLayoutParams().height = enterContributionMode ? 0 : widthHeightOfCanvasText[1];


        rootView.findViewById(R.id.unSeenCountTextView).setVisibility((enterContributionMode || (unseenItemCount <= 0)) ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.streamName).setVisibility(enterContributionMode ? View.VISIBLE : View.GONE);


        if (enterContributionMode) {
            String text = "Contribute to \n" + (((CameraActivity) getActivity()).getPublicStreamContribution().name);
            SpannableString content = new SpannableString(text);
            content.setSpan(new RelativeSizeSpan((11 / 12f)), 0, "Contribute to \n".length(), 0); // set size
            ((TextView) rootView.findViewById(R.id.streamName)).setText(content);
        }

        canvasBtn.invalidate();
        canvasBtn.requestLayout();
    }

    /**
     * @param rootView of the fragment
     * @return true if it will block the UI for showing popup false otherwise
     */
    private boolean showXiaomiPopUp(final View rootView) {

        final SharedPreferences prefs = getActivity().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        final int z = prefs.getInt(AppLibrary.XIAOMI_AUTO_START_PERMISSION, 0);
        if (z > 0) {
            Log.d(TAG, "showXiaomiPopUp: already shown returning ");
            return false;
        }
        String MiUiVersion = MiuiPermissionHandler.getMiUiVersionProperty();
        if (MiUiVersion == null || TextUtils.isEmpty(MiUiVersion)) {
            //no need to show  the popup as the device is not xiaomi
            prefs.edit().putInt(AppLibrary.XIAOMI_AUTO_START_PERMISSION, z + 1).apply();
            return false;
        } else {
            rootView.findViewById(R.id.xiaomiAutoStartRL).setVisibility(View.VISIBLE);
            applyTypeFaces((TextView) rootView.findViewById(R.id.intro1), (TextView) rootView.findViewById(R.id.intro2));
            rootView.findViewById(R.id.xiaomiAutoStartRL).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {//intercepting the touch events
                    return true;
                }
            });

            rootView.findViewById(R.id.enableAutoStartTV).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rootView.findViewById(R.id.xiaomiAutoStartRL).setVisibility(View.GONE);
                    prefs.edit().putInt(AppLibrary.XIAOMI_AUTO_START_PERMISSION, z + 1).apply();
                    MiuiPermissionHandler.openMiuiPermissionActivity(context);
                    if (!AppLibrary.IS_CAMERA_STICKER_SHOWN_IN_THIS_SESSION)
                        initTuts();
                }
            });
            return true;
        }
    }

    private void applyTypeFaces(TextView textView1, TextView textView2) {

        /**
         * Note as this  linkedHashMap will FAIL in case same key is inserted more than once.
         * SomeOtherDataStructure must be used in that case
         */
        LinkedHashMap<String, Boolean> stringBooleanLinkedHashMap = new LinkedHashMap<>();//true for bold , false for normal texts
        stringBooleanLinkedHashMap.put("On MI devices you need to add ", false);
        stringBooleanLinkedHashMap.put("Pulse", true);
        stringBooleanLinkedHashMap.put(" to autostart list.", false);

        setSpannable(stringBooleanLinkedHashMap, textView1);
        stringBooleanLinkedHashMap.clear();

        //        Tap “Enable” and turn on Pulse in the list to never miss a notification
        stringBooleanLinkedHashMap.put("Tap ", false);
        stringBooleanLinkedHashMap.put("“Enable”", true);
        stringBooleanLinkedHashMap.put(" and turn on ", false);
        stringBooleanLinkedHashMap.put("Pulse", true);
        stringBooleanLinkedHashMap.put(" in the list to never miss a notification", false);

        setSpannable(stringBooleanLinkedHashMap, textView2);
        stringBooleanLinkedHashMap.clear();

    }

    private void setSpannable(LinkedHashMap<String, Boolean> stringBooleanLinkedHashMap, TextView textView) {
        String buffer = "";
        Typeface regular = FontPicker.getInstance(context).getMontserratRegular();
        Typeface bold = FontPicker.getInstance(context).getMontserratBold();

        String s = "";
        for (Map.Entry<String, Boolean> entry : stringBooleanLinkedHashMap.entrySet()) {
            s += entry.getKey();
        }
        Spannable spannable = new SpannableString(s);
        for (Map.Entry<String, Boolean> entry : stringBooleanLinkedHashMap.entrySet()) {
            spannable.setSpan(new CustomTypefaceSpan("regular", entry.getValue() ? bold : regular), buffer.length(), (buffer.length() + entry.getKey().length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            buffer += entry.getKey();
        }
        textView.setText(spannable);
    }
}
