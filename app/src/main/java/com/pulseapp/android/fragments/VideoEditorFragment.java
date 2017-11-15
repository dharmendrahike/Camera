package com.pulseapp.android.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.broadcast.FilterManager;
import com.pulseapp.android.broadcast.MoviePlayer;
import com.pulseapp.android.broadcast.RecordingActionControllerKitkat;
import com.pulseapp.android.broadcast.SurfaceRenderer;
import com.pulseapp.android.customViews.OvalColorPicker;
import com.pulseapp.android.downloader.ChatCameraStickerDownloader;
import com.pulseapp.android.downloader.TemplateDownloader;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.models.LocationTemplateModel;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.stickers.EmoticonFragment;
import com.pulseapp.android.stickers.ParentStickerFragment;
import com.pulseapp.android.stickers.ScribbleView;
import com.pulseapp.android.stickers.ThemeLoader;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BlurBuilder;
import com.pulseapp.android.util.CapturedMediaController;
import com.pulseapp.android.util.PrivacyPopup;
import com.pulseapp.android.util.WriteImageCallback;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by bajaj on 14/1/16.
 */
public class VideoEditorFragment extends BaseFragment implements
        RecordingActionControllerKitkat.VideoLifecycleEventListener,
        RecordingActionControllerKitkat.ImageEventListener, RecordingActionControllerKitkat.VideoEventListener, View.OnClickListener, LocationListener {

    private static final String TAG = "VideoEditorFragment";
    private int MEDIA_TYPE;
    private final int SCRIBBLE_LAYER = 3;
    private final int EMOTICON_LAYER = 2;
    private final int VIEW_PAGER_LAYER = 1;
    private final int NONE = -1;
    public boolean isScribblingMode;
    private RecordingActionControllerKitkat controller;
    private ScribbleView scribbleView;
    private FrameLayout majorDetector, themeFrame;
    private int whichLayer;
    private ParentStickerFragment parentStickerFragment;
    private EmoticonFragment emoticonFragment;
    private ImageView scribbleIv, emojiIv, addTextIv;
    private GLSurfaceView mGLSurfaceView;
    private String mMP4File;
    private String mOutFilePath;
    private View rootView;
    private boolean justSave = false;
    private TextView mTextView;
    private ImageView editImageView, saveImageView, sendImageView, timerImageView, locationImageView, closeBtn;
    public ImageView backButton;
    private TextView filterNameTv;
    private FrameLayout emojiPagerFrame, editFrameLayout;
    private ViewControlsCallback viewControlsCallback;
    private LinearLayout saveAndSoundLayout;
    LocationRequest locationRequest;
    private static boolean requestLocationUpdate = false;
    private ImageView addToMyStreamIv, shareToWhatsAppIv;
    private static final int REQUEST_CHECK_SETTINGS = 5;
    private boolean gotoShareScreen = false;
    private View.OnTouchListener majorListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, final MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                checkWhichLayer();
            }

            emitTouchEvent(event);

            return true;
        }
    };
    private Toast customToastTut;
    private boolean flag;

    private void toggleEmoticonVisibility() {
        if (parentEmojiPagerFragment != null)
            parentEmojiPagerFragment.hideKeyboard();

        if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
            emojiPagerFrame.setVisibility(View.GONE);
            saveAndSoundLayout.setVisibility(View.VISIBLE);
            sendImageView.setVisibility(View.VISIBLE);
            addToMyStreamIv.setVisibility(View.VISIBLE);
            shareToWhatsAppIv.setVisibility(View.VISIBLE);
            if (isPublicStreamContributionMode())
                bottomView.setVisibility(View.VISIBLE);
        } else {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SEARCH_STICKER);

            emojiPagerFrame.setVisibility(View.VISIBLE);
            saveAndSoundLayout.setVisibility(View.GONE);
            sendImageView.setVisibility(View.GONE);
            addToMyStreamIv.setVisibility(View.GONE);
            shareToWhatsAppIv.setVisibility(View.GONE);

            if (isPublicStreamContributionMode())
                bottomView.setVisibility(View.GONE);
        }

    }

    private String selectedMediaPath = null;
    private static Bitmap capturedBitmap = null;
    private Bitmap mOverlayBitmap;
    private String mCurrentFilter;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;
//        traverse(TAG, new File(context.getFilesDir().getAbsolutePath() + File.separator));
        requestApproximateLocation = true;
        googleApiClient = new GoogleApiClient.Builder(context, connectionCallbacks, connectionFailedListener).addApi(LocationServices.API).build();
//        firebase = FireBaseHelper.getInstance(getActivity()).getNewFireBase(null,null);
//        themeFireBase = firebase.child("themes");
//        themeFireBase.keepSynced(true);
//        themeModels = new ArrayList<>();
        if (CameraActivity.IGNORE_BACK_STACK_LISTENER) {
            ((CameraActivity) getActivity()).stopCameraPreview();
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            selectedMediaPath = bundle.getString(AppLibrary.SELECTED_MEDIA_PATH);
        } else {
            capturedBitmap = CapturedMediaController.getInstance().getCapturedBitmap();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_video_editor, container, false);

        if (mFireBaseHelper == null || savedInstanceState != null) return rootView;

        progressBarLayout = rootView.findViewById(R.id.progressLayout);
        progressBarLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;//else the filters would consume the touch
            }
        });
        progessTextView = (TextView) rootView.findViewById(R.id.progressTV);
        progessTextView.setTypeface(fontPicker.getMontserratRegular());
        progessTextView.setGravity(Gravity.CENTER);

        ((ProgressBar) rootView.findViewById(R.id.progressBar))
                .getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);

        initializeObjectViews(rootView);
        validateFragmentViews();
        initVideoEditorControls(rootView);
        initColorEditor(rootView);
        loadEmoticonFragment();
//        loadTextFragment();
        loadEmoticonViewPager();

        return rootView;
    }

    private void initializeObjectViews(View rootView) {
        addToMyStreamIv = (ImageView) rootView.findViewById(R.id.addToMyStreamIV);
        shareToWhatsAppIv = (ImageView) rootView.findViewById(R.id.shareToWhatsAppIV);
        //since we will be performing click on save button whenever this is clicked,
        // we must disable the sound , else there will be dual sound feedback
        shareToWhatsAppIv.setSoundEffectsEnabled(false);

        boolean isWhatsAppInstalled = AppLibrary.isPackageInstalled(WHATSAPP_PACKAGE_NAME, context);


        /**
         *using this approach instead of toggling visibility we won't have add additional variables
         *to toggle the visibility of whatsapp Btn and add to my stream .
         * Hence we can safely toggle the visibility with other elements
         *
         */
        if (isWhatsAppInstalled)
            addToMyStreamIv.getLayoutParams().width = 0;
        else
            shareToWhatsAppIv.getLayoutParams().width = 0;

        if (this.isPublicStreamContributionMode()){
            addToMyStreamIv.getLayoutParams().width = 0;
            shareToWhatsAppIv.getLayoutParams().width = 0;
        }

        videoEditorLl = (LinearLayout) rootView.findViewById(R.id.videoEditorLayout);
        editImageView = (ImageView) rootView.findViewById(R.id.picture);
        editFrameLayout = (FrameLayout) rootView.findViewById(R.id.editLayout);
//        editFrameLayout.setDrawingCacheEnabled(true);
//        editFrameLayout.setDrawingCacheQuality(FrameLayout.DRAWING_CACHE_QUALITY_HIGH);
        mGLSurfaceView = (GLSurfaceView) rootView.findViewById(R.id.glsurfaceview);
        mGLSurfaceView.setZOrderMediaOverlay(true);
        mTextView = (TextView) rootView.findViewById(R.id.dummy_textview);
        saveImageView = (ImageView) rootView.findViewById(R.id.saveButton);
        timerImageView = (ImageView) rootView.findViewById(R.id.timer_Iv);
        locationImageView = (ImageView) rootView.findViewById(R.id.location_IV);
        backButton = (ImageView) rootView.findViewById(R.id.backButton);
        sendImageView = (ImageView) rootView.findViewById(R.id.send);
        majorDetector = (FrameLayout) rootView.findViewById(R.id.majorTouchDetector);
        emojiPagerFrame = (FrameLayout) rootView.findViewById(R.id.emojiPagerFrame);
        scribbleView = (ScribbleView) rootView.findViewById(R.id.scribbleView);
        closeBtn = (ImageView) rootView.findViewById(R.id.closeButton);
        saveAndSoundLayout = (LinearLayout) rootView.findViewById(R.id.save_soundLL);
        filterNameTv = (TextView) rootView.findViewById(R.id.filterNameTV);
        filterNameTv.setTypeface(fontPicker.getMontserratRegular());
        themeFrame = (FrameLayout) rootView.findViewById(R.id.themeFrameHolder);

        addToMyStreamIv.setOnClickListener(this);
        shareToWhatsAppIv.setOnClickListener(this);
        sendImageView.setOnClickListener(this);
        saveImageView.setOnClickListener(this);
        timerImageView.setOnClickListener(this);
        locationImageView.setOnClickListener(this);
        backButton.setOnClickListener(this);
        rootView.findViewById(R.id.closeButton).setOnClickListener(this);
        majorDetector.setOnTouchListener(majorListener);

        scribbleView.setScribbleListener(new ScribbleView.ScribbleListener() {
            @Override
            public void onScribbleStart() {
                onEmoticonMovementStateChanged(false);
            }

            @Override
            public void onScribbleEnd() {
                onEmoticonMovementStateChanged(true);
            }

            @Override
            public void onTap(MotionEvent event) {

            }

            @Override
            public void onUndoScribble() {
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, " onResumeCalled");
//        setCanPlay(true);
/*        try {
            if (controller != null) {
                controller.resumeView();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onPause() {
        Log.d(TAG, " onPauseCalled");
        super.onPause();
        cancelCustomToast(customToastTut);
//        if (controller != null) {
////            controller.pauseView();
//            setCanPlay(false);
//        }
    }

    public void filterSwipeRight() {

        int currentFilter = controller.decrementFilter();
        animateFilterName(currentFilter);
        mCurrentFilter = FilterManager.getColorEffectName(currentFilter);

        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_VIDEO) {
            if (mCurrentFilter.equalsIgnoreCase("Slow-Mo") || mCurrentFilter.equalsIgnoreCase("Fast-Forward")) {
                View view = rootView.findViewById(R.id.soundButton);
                ((ImageView) view).setImageResource(R.drawable.sound_deactivated);
                view.setClickable(false);
            } else {
                resetMediaState();
            }
        }
        animateDots(currentFilter);

    }

    public void filterSwipeLeft() {

        int currentFilter = controller.incrementFilter();
        animateFilterName(currentFilter);
        mCurrentFilter = FilterManager.getColorEffectName(currentFilter);

        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_VIDEO) {
            if (mCurrentFilter.equalsIgnoreCase("Slow-Mo") || mCurrentFilter.equalsIgnoreCase("Fast-Forward")) {
                View view = rootView.findViewById(R.id.soundButton);
                ((ImageView) view).setImageResource(R.drawable.sound_deactivated);
                view.setClickable(false);
            } else {
                resetMediaState();
            }
        }
        animateDots(currentFilter);
    }


    private LinearLayout dotsLayout;

    //    private int[] imagesFilters = new int[]{0, 7, 8, 9, 10, 12, 13, 14};
    private int[] imagesFilters = SurfaceRenderer.filterOrder;
    private int[] videoFilters = SurfaceRenderer.filterOrderVideo;


    void initAnimationViews() {
        if (dotsLayout == null) {
            int filtersArray[] = this.MEDIA_TYPE == MEDIA_TYPE_IMAGE ? imagesFilters : videoFilters;
            dotsLayout = (LinearLayout) rootView.findViewById(R.id.dotsLayout);
            dotsLayout.setAlpha(0f);

            for (int i = 0; i < filtersArray.length; i++) {
                ImageView imageView = new ImageView(getActivity());
                imageView.setTag(filtersArray[i]);
                dotsLayout.addView(imageView);
                int margin = AppLibrary.convertDpToPixels(context, 4);
                ((LinearLayout.LayoutParams) imageView.getLayoutParams()).leftMargin = margin;
                ((LinearLayout.LayoutParams) imageView.getLayoutParams()).rightMargin = margin;
            }
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animateDots(0);

            }
        }, 400);
    }

    private void animateDots(int currentCount) {

        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dotIv = (ImageView) dotsLayout.getChildAt(i);
            if (((Integer) dotIv.getTag()) == currentCount) {
                dotIv.setImageResource(R.drawable.dotselected_svg);
            } else {
                dotIv.setImageResource(R.drawable.dotdefault_svg);
            }
        }

        Log.d(TAG, "animateDots:  count " + currentCount);
        alphaAnimationHandler.removeCallbacksAndMessages(null);
        alphaAnimationHandler.postDelayed(fadeOutRunnable, 5000);

        dotsLayout.animate().alpha(1f).setDuration(300).start();
    }

    private Handler alphaAnimationHandler = new Handler();
    private Runnable fadeOutRunnable = new Runnable() {
        @Override
        public void run() {
            dotsLayout.animate().alpha(0f).setDuration(300).start();
        }
    };

    private void animateFilterName(int currentFilter) {
        String filterName = FilterManager.getColorEffectName(currentFilter);
        Log.d(TAG, "animateFilterName filterName");

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APPLY_FILTER, AnalyticsEvents.FILTER_NAME, filterName);

        if (filterName.contains("Fast-Forward") || filterName.contains("Slow-Mo"))
            animateThemeName(filterName);
    }

    private void animateThemeName(String filterName) {

        filterNameTv.setText(filterName);
        filterNameTv.setVisibility(View.VISIBLE);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(100);
        fadeOut.setDuration(500);

        AnimationSet animation = new AnimationSet(false); //change to false
        // animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        filterNameTv.setAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                filterNameTv.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void addEmoticons(View v) {
        emoticonFragment.addEmoticonFromGrid(v);
        hideEmoticonViewPager();
    }

    public void hideEmoticonViewPager() {
        emojiPagerFrame.setVisibility(View.GONE);
        saveAndSoundLayout.setVisibility(View.VISIBLE);
        sendImageView.setVisibility(View.VISIBLE);
        addToMyStreamIv.setVisibility(View.VISIBLE);
        shareToWhatsAppIv.setVisibility(View.VISIBLE);
        if (this.isPublicStreamContributionMode())
            bottomView.setVisibility(View.VISIBLE);


        checkAndToggleBackButton(false);
    }

    private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
//        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }

    private void validateFragmentViews() {
        Log.d(TAG, "validateFragmentViews...");
        if (selectedMediaPath != null && (selectedMediaPath.toLowerCase().endsWith(".jpg") || selectedMediaPath.toLowerCase().endsWith(".jpeg") || selectedMediaPath.toLowerCase().endsWith(".png") || selectedMediaPath.toLowerCase().endsWith(".webp"))) {
            // update ui based on image editing options
            MEDIA_TYPE = AppLibrary.MEDIA_TYPE_IMAGE;
            editImageView.setVisibility(View.VISIBLE);

            Bitmap bitmap = BitmapFactory.decodeFile(selectedMediaPath/*, options*/);
            selectedMediaPath = null;

            if (bitmap == null) {
                BaseFragment.showShortToastMessage("Sorry, unable to open image!");
                getActivity().onBackPressed();
                return;
            }

            float deviceAspectRatio = (float) AppLibrary.getDeviceParams(getActivity(), "width") / AppLibrary.getDeviceParams(getActivity(), "height");
            float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();
            if (Math.abs(aspectRatio - deviceAspectRatio) > 0.03) {
                AppLibrary.log_d(TAG, "Landscape image found");
                Bitmap modifiedBitmap = createNewBitmap(bitmap);
                setCapturedBitmap(modifiedBitmap);
            } else {
                AppLibrary.log_d(TAG, "Portrait image found");
                if (bitmap.getConfig() == Bitmap.Config.RGB_565) {
                    setCapturedBitmap(convert(bitmap, Bitmap.Config.ARGB_8888));
                } else {
                    setCapturedBitmap(bitmap);
                }
//                editImageView.setImageURI(Uri.fromFile(new File(selectedMediaPath)));
                editImageView.setImageBitmap(capturedBitmap);
            }

            prepareImageCapture();
            controller.setOverlayView(editImageView);
//            Picasso.with(getActivity()).load(new File(selectedMediaPath)).centerCrop().fit().into(editImageView);
        } else if (selectedMediaPath != null && selectedMediaPath.toLowerCase().endsWith(".mp4")) {
            MEDIA_TYPE = AppLibrary.MEDIA_TYPE_VIDEO;
//            rootView.findViewById(R.id.timer_Iv).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.timer_Iv).setOnClickListener(this);
            rootView.findViewById(R.id.soundButton).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.soundButton).setOnClickListener(this);
            rootView.findViewById(R.id.soundButton).setTag(R.drawable.sound_on_svg);
            // video editing options
            startVideoDecodeEncode();
        } else if (capturedBitmap != null) {
            MEDIA_TYPE = AppLibrary.MEDIA_TYPE_IMAGE;
            editImageView.setVisibility(View.VISIBLE);
            editImageView.setImageBitmap(capturedBitmap);
            prepareImageCapture();
            controller.setOverlayView(editImageView);
        }
    }

    private void setCapturedBitmap(Bitmap bitmap) {
        capturedBitmap = bitmap;
        CapturedMediaController.getInstance().setCapturedBitmap(capturedBitmap);
    }

    private Bitmap createNewBitmap(Bitmap bitmap) {
        int deviceHeight = AppLibrary.getDeviceParams(getActivity(), "height");
        int deviceWidth = AppLibrary.getDeviceParams(getActivity(), "width");
        int imageWidth = deviceWidth;
        int imageHeight = (int) (((float) (bitmap.getHeight() * imageWidth / bitmap.getWidth())));
        Bitmap fillBitmap = Bitmap.createBitmap(deviceWidth, deviceHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fillBitmap);
        Bitmap blurBitmap = BlurBuilder.blur(getActivity(), bitmap);
        Rect rect = scaleCenter(blurBitmap, new Rect(0, 0, deviceWidth, deviceHeight), true);
//        Rect originalRect = scaleCenter(bitmap, new Rect(0,(deviceHeight-deviceWidth)/2, deviceWidth, deviceWidth),false);
        Rect originalRect = scaleCenter(bitmap, new Rect(0, 0, deviceWidth, deviceHeight), false);
        canvas.drawBitmap(blurBitmap, null, rect, new Paint());
        canvas.drawBitmap(bitmap, null, originalRect, new Paint());
//        editImageView.setImageBitmap(fillBitmap);
        return fillBitmap;
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

    private void prepareImageCapture() {
        Log.d(TAG, "START IMAGE CAPTURE");

        if (controller == null) {
            controller = new RecordingActionControllerKitkat(getActivity(), mTextView, mGLSurfaceView);
        }
        controller.setImageEventListener(this);
/*        if (selectedMediaPath != null) {
            controller.setSourceMediaFile(selectedMediaPath, MEDIA_TYPE_IMAGE);
        } else {*/
        controller.setSourceMedia(capturedBitmap, MEDIA_TYPE_IMAGE);
/*        }*/
        ((CameraActivity) getActivity()).setRecordingActionControllerReference(controller);

        mGLSurfaceView.setVisibility(View.VISIBLE);
    }

    final int NUMBER_OF_MOVES_FOR_SLIDE_ANIMATION = 5;

    private void emitTouchEvent(MotionEvent event) {
        switch (whichLayer) {
            case NONE:
                emoticonFragment.setTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) break;
//                textFragment.setTouchEvent(event);
                break;
//            case TEXT_LAYER:
//                textFragment.setTouchEvent(event);
//                break;
            case EMOTICON_LAYER:
//                Log.d(TAG, "EMOTICON_LAYER touch " + getActionName(event.getAction()) + " emoticonsTranslated " + emoticonsTranslated + " moves" + numberOfMovesAfterTranslation);
                emoticonFragment.setTouchEvent(event);

                if (event.getAction() == MotionEvent.ACTION_MOVE && emoticonsTranslated) {
                    ++numberOfMovesAfterTranslation;
                    if (numberOfMovesAfterTranslation == NUMBER_OF_MOVES_FOR_SLIDE_ANIMATION) {
                        onEmoticonMovementStateChanged(false);
                    }
                    break;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (emoticonsTranslated && numberOfMovesAfterTranslation >= NUMBER_OF_MOVES_FOR_SLIDE_ANIMATION) {
                        onEmoticonMovementStateChanged(true);
                    }
                    emoticonsTranslated = false;
                    numberOfMovesAfterTranslation = 0;
                }

                break;
            case VIEW_PAGER_LAYER:
//                if (event.getPointerCount() != 1 || parentStickerFragment == null ||
//                        parentStickerFragment.stickerPager == null) break;
                Log.d(TAG, "parentStickerFragment " + parentStickerFragment);
                if (parentStickerFragment != null && parentStickerFragment.stickerPager != null)
//                    parentStickerFragment.stickerPager.onTouchEvent(event);
                    parentStickerFragment.stickerPager.dispatchTouchEvent(event);
                break;
            case SCRIBBLE_LAYER:
                scribbleView.onTouchEvent(event);
                break;
            default:
                throw new RuntimeException("What the Fuck");
        }
        // Log.d(TAG, " emitting touch event to" + getLayerNAme(whichLayer) + " " + getActionName(event.getAction()));
    }

    private boolean emoticonsTranslated;
    private int numberOfMovesAfterTranslation;

    public void setEmoticonTranslated(boolean translated) {
        Log.d(TAG, "emoticonsTranslated " + translated);
        this.emoticonsTranslated = translated;
    }

    boolean isBackButton = false;
    boolean settled = true;
    boolean isTutorialShown = false;

    public void onEmoticonMovementStateChanged(boolean settled) {
        Log.d(TAG, "onEmoticonMovementStateChanged " + settled);
        float endAlpha;
        int animationDuration = 100;
        Runnable r = null;

        if (this.settled == settled)
            return; //Avoid repeat calls - there has to be a false before a true

        if (!settled) {
            r = new Runnable() {
                @Override
                public void run() {
                    videoEditorLl.setVisibility(View.GONE);
                    if (backButton.getVisibility() == View.VISIBLE) {
                        backButton.setVisibility(View.GONE);
                        isBackButton = true;
                    } else {
                        closeBtn.setVisibility(View.GONE);
                        isBackButton = false;
                    }
                    sendImageView.setVisibility(View.GONE);
                    addToMyStreamIv.setVisibility(View.GONE);
                    shareToWhatsAppIv.setVisibility(View.GONE);
                    saveAndSoundLayout.setVisibility(View.GONE);
                    if (isScribblingMode) {
                        ovalColorPicker.setVisibility(View.GONE);
                    }
                    if (isPublicStreamContributionMode()) {
                        bottomView.setVisibility(View.GONE);
                    }

                    if (rootView.findViewById(R.id.discoverStickerIncludedView).getVisibility() == View.VISIBLE) {
                        rootView.findViewById(R.id.discoverStickerIncludedView).setVisibility(View.GONE);
                        isTutorialShown = true;
                    }

                }
            };
            endAlpha = 0f;
        } else {
            videoEditorLl.setVisibility(View.VISIBLE);

            if (isBackButton)
                backButton.setVisibility(View.VISIBLE);
            else
                closeBtn.setVisibility(View.VISIBLE);

            sendImageView.setVisibility(View.VISIBLE);
            addToMyStreamIv.setVisibility(View.VISIBLE);
            shareToWhatsAppIv.setVisibility(View.VISIBLE);
            saveAndSoundLayout.setVisibility(View.VISIBLE);
            if (isScribblingMode) {
                ovalColorPicker.setVisibility(View.VISIBLE);
            }
            if (isPublicStreamContributionMode()) {
                bottomView.setVisibility(View.VISIBLE);
            }

            if (isTutorialShown) {
                rootView.findViewById(R.id.discoverStickerIncludedView).setVisibility(View.VISIBLE);
                isTutorialShown = false;
            }

            endAlpha = 1f;
        }


        if (r != null) {
            videoEditorLl.animate().alpha(endAlpha).setDuration(animationDuration).withEndAction(r).start();
            closeBtn.animate().alpha(endAlpha).setDuration(animationDuration).start();
            backButton.animate().alpha(endAlpha).setDuration(animationDuration).start();
            sendImageView.animate().alpha(endAlpha).setDuration(animationDuration).start();
            addToMyStreamIv.animate().alpha(endAlpha).setDuration(animationDuration).start();
            shareToWhatsAppIv.animate().alpha(endAlpha).setDuration(animationDuration).start();
            saveAndSoundLayout.animate().alpha(endAlpha).setDuration(animationDuration).start();
            if (isScribblingMode) {
                ovalColorPicker.animate().alpha(endAlpha).setDuration(animationDuration).start();
            }
            if (isPublicStreamContributionMode()) {
                bottomView.animate().alpha(endAlpha).setDuration(animationDuration).start();
            }
        } else {
            videoEditorLl.animate().alpha(endAlpha).setDuration(animationDuration).start();
            closeBtn.animate().alpha(endAlpha).setDuration(animationDuration).start();
            backButton.animate().alpha(endAlpha).setDuration(animationDuration).start();
            sendImageView.animate().alpha(endAlpha).setDuration(animationDuration).start();
            addToMyStreamIv.animate().alpha(endAlpha).setDuration(animationDuration).start();
            shareToWhatsAppIv.animate().alpha(endAlpha).setDuration(animationDuration).start();
            saveAndSoundLayout.animate().alpha(endAlpha).setDuration(animationDuration).start();
            if (isScribblingMode) {
                ovalColorPicker.animate().alpha(endAlpha).setDuration(animationDuration).start();
            }
            if (isPublicStreamContributionMode()) {
                bottomView.animate().alpha(endAlpha).setDuration(animationDuration).start();
            }
        }

        this.settled = settled;
    }

    private void checkWhichLayer() {
        whichLayer = EMOTICON_LAYER;

        if (isScribblingMode) {
            whichLayer = SCRIBBLE_LAYER;
            Log.d(TAG, " which Layer " + whichLayer);
//            undoIv.setVisibility(View.VISIBLE); // FIXME: 7/23/16
            ovalColorPicker.showUndo();
        }
        if (isLocationMode) {
            whichLayer = VIEW_PAGER_LAYER;
        }

    }

    private void initVideoEditorControls(View rootView) {
        scribbleIv = (ImageView) rootView.findViewById(R.id.SCRIBBLE_TV);
        emojiIv = (ImageView) rootView.findViewById(R.id.emoji_IV);
        addTextIv = (ImageView) rootView.findViewById(R.id.text_IV);

        scribbleIv.setOnClickListener(this);
        emojiIv.setOnClickListener(this);
        addTextIv.setOnClickListener(this);
    }

    private void toggleScribbleButton() {
        isScribblingMode = !isScribblingMode;
        locationImageView.setVisibility(isScribblingMode ? View.GONE : View.VISIBLE);//todo test
        ovalColorPicker.setVisibility(isScribblingMode ? View.VISIBLE : View.GONE);
        emojiIv.setVisibility(isScribblingMode ? View.GONE : View.VISIBLE);
        addTextIv.setVisibility(isScribblingMode ? View.GONE : View.VISIBLE);

        if (isScribblingMode)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SKETCH);
    }

    private void toggleBackButton() {
        if (backButton.getVisibility() == View.VISIBLE) {
            backButton.setVisibility(View.GONE);
            closeBtn.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.GONE);
        }
    }

    public void checkAndToggleBackButton(boolean backVisible) {
        if (backVisible) {
            backButton.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.GONE);
        } else {
            backButton.setVisibility(View.GONE);
            closeBtn.setVisibility(View.VISIBLE);
        }
    }

    public void startVideoDecodeEncode() {
        Log.d(TAG, "START VIDEO DECODE ENCODE");
        mGLSurfaceView.setVisibility(View.VISIBLE);

        setShareVideoFile(); //Default set to saved location

        if (controller == null) {
            controller = new RecordingActionControllerKitkat(getActivity(), mTextView, mGLSurfaceView);
        }

        controller.setVideoLifecycleListener(this);
        controller.prepare();
        ((CameraActivity) getActivity()).setRecordingActionControllerReference(controller);

        controller.setSourceMediaFile(mMP4File, MEDIA_TYPE_VIDEO);
        controller.setLastRecodedFilepath(mOutFilePath);
    }

    private void setShareVideoFile() {
        mMP4File = selectedMediaPath;
        String suffixString = "_share";
        mOutFilePath = mMP4File.replace(".mp4", suffixString + ".mp4");
    }

    private void setVideoFilePath(boolean justSave) {
        String suffixString;
        if (!justSave) {
            setShareVideoFile();
            if (controller != null)
                controller.setLastRecodedFilepath(mOutFilePath);
        } else {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            suffixString = "_saved";
            String name = date + "_" + today.hour + today.minute + today.second + suffixString + ".mp4";

            String mediaStoragePath = AppLibrary.setupOutputDirectoryForRecordedFile();

            File mediaDir = new File(mediaStoragePath);
            if (!mediaDir.exists())
                mediaDir.mkdirs();
            editedFile = new File(mediaDir, name);
            mOutFilePath = editedFile.getAbsolutePath();
            if (controller != null)
                controller.setLastRecodedFilepath(editedFile.getAbsolutePath());
        }
    }

    public void destroyRenderer() {
        if (this.controller != null) {
            this.controller.pauseView(true);
            controller.setRenderTargetType(0);
            controller.stopAndCleanupPlayer(true);
            if (this.controller.isPublishing())
                this.controller.actionPublish();

            this.controller.destroy();
            this.controller = null;
            Log.d(TAG, "Controller destroyed");
            cleanup();
            MoviePlayer.ENABLE_AUDIO = true; //Reset any audio changes
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ThemeLoader.destroy();
        selectedMediaPath = null;
        isDestroyed = true;
        if (mGLSurfaceView != null) {
            mGLSurfaceView.removeCallbacks(null);
            mGLSurfaceView = null;
        }
        if (controller != null) {
            controller.setPreviewSurfaceView(null);
        }
//        cleanup();
//        CapturedMediaController.getInstance().clearBitmap();
    }

    private void cleanup() {
        if (capturedBitmap != null) {
            capturedBitmap.recycle();
            capturedBitmap = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (CameraActivity.IGNORE_BACK_STACK_LISTENER) {
            ((CameraActivity) getActivity()).startCameraPreview(0);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) return;

        // tutorial toast for filters
        if (!getPreferences().getBoolean(AppLibrary.FILTER_TUT_SHOWN, false)) {
            cancelCustomToast(customToastTut);
            customToastTut = showCustomToast(getActivity(), R.layout.tut2, Gravity.CENTER, 0, 0, 7000);
            getPreferences().edit().putBoolean(AppLibrary.FILTER_TUT_SHOWN, true).commit();
        }
        if (this.getPublicMomentModel() != null)
            initPublicContributionView();

        initDiscoverTutorial();
        initAnimationViews();
    }

    public void actionToggle() {
        //  this will start/stop streaming.
        if (controller != null)
            controller.actionPublish();
    }

    @Override
    public void videoPlaybackStarted() {
    }

    @Override
    public void videoPlaybackPaused() {

    }

    @Override
    public void videoPlaybackStopped() {

    }

    @Override
    public void videoSaved() {
        if (justSave && !shareToWhatsApp) {
            final CameraActivity activity = ((CameraActivity) getActivity());
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (activity != null) {
                                toggleUiOnOff(false, "Saving...");
                                Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show();
                                addRecordingToMediaLibrary(false);
                            }
                        }
                    });
        } else if (shareToWhatsApp) {

//            final CameraActivity activity = ((CameraActivity) getActivity());
            getActivity().runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null) {
                                addRecordingToMediaLibrary(false);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        toggleUiOnOff(false, "Saving...");
                                        shareMediaToWhatsApp(AppLibrary.MEDIA_TYPE_VIDEO);
                                    }
                                }, 1000);
                            }
                        }
                    });
        } else {
            synchronized (CameraActivity.mVideoLock) {
                CameraActivity.mVideoLock.notifyAll(); //Signal to uploadThread that we're ready for upload
            }
        }
    }

    @Override
    public void closeVideoPlayback() {
        Toast toast = Toast.makeText(context, "Video too short. Please try again!", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        final CameraActivity activity = ((CameraActivity) getActivity());
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        if (activity != null) {
                            activity.onBackPressed();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        cancelCustomToast(customToastTut);
        if (alphaAnimationHandler != null && dotsLayout != null && dotsLayout.getAlpha() > 0f) {
            alphaAnimationHandler.removeCallbacksAndMessages(null);
            alphaAnimationHandler.post(fadeOutRunnable);
        }

        switch (v.getId()) {
            case R.id.SCRIBBLE_TV:
                if (emoticonFragment.textInFocus)
                    emoticonFragment.minimizeEditor();

                if (emojiPagerFrame.getVisibility() == View.VISIBLE)
                    toggleEmoticonVisibility();

                hidePopUp();
                toggleScribbleButton();

                if (isScribblingMode && backButton.getVisibility() == View.GONE)
                    toggleBackButton();
                else if (!isScribblingMode && backButton.getVisibility() == View.VISIBLE)
                    toggleBackButton();

                break;
            case R.id.emoji_IV:
                if (emoticonFragment.textInFocus)
                    emoticonFragment.minimizeEditor();

                int numberOfFragments = ChatCameraStickerDownloader.getChatCameraStickerDownloader(context).registerEmoticonFragment(parentEmojiPagerFragment);
                hidePopUp();
                if (numberOfFragments <= 0) {//emoji view pager won't be shown now
                    showShortToastMessage("No Stickers");
                    break;
                }

                toggleEmoticonVisibility();

                if (emojiPagerFrame.getVisibility() == View.VISIBLE && backButton.getVisibility() == View.GONE)
                    toggleBackButton();
                else if (emojiPagerFrame.getVisibility() == View.GONE && backButton.getVisibility() == View.VISIBLE)
                    toggleBackButton();

                isScribblingMode = false;
                break;
            case R.id.text_IV:
                if (emojiPagerFrame.getVisibility() == View.VISIBLE)
                    toggleEmoticonVisibility();

                isScribblingMode = false;
                emoticonFragment.toggleTextBox();

                if (emoticonFragment.textInFocus && backButton.getVisibility() == View.GONE)
                    toggleBackButton();
                else if (!emoticonFragment.textInFocus && backButton.getVisibility() == View.VISIBLE)
                    toggleBackButton();
                break;
            case R.id.saveButton:

                if (emoticonFragment.textInFocus) {
                    emoticonFragment.minimizeEditor();
                }

                if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
                    toggleEmoticonVisibility();
                }

                if (isScribblingMode) {
                    toggleScribbleButton();
                }

                checkAndToggleBackButton(false);

                requestSaveEditedMedia();
                break;
            /**
             * {@link #initPublicContributionView()} is also performing click on R.id.send
             */
            case R.id.send:
                if (emoticonFragment.textInFocus) {
                    emoticonFragment.minimizeEditor();
                }

                if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
                    toggleEmoticonVisibility();
                }

                if (isScribblingMode) {
                    toggleScribbleButton();
                }

                checkAndToggleBackButton(false);

                gotoShareScreen = true;
                sendEditedMedia(true);
                break;
            case R.id.closeButton:
                if (emoticonFragment.textInFocus) {
                    emoticonFragment.minimizeEditor();
                    break;
                }

                if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
                    toggleEmoticonVisibility();
                    break;
                }

                if (isScribblingMode) {
                    toggleScribbleButton();
                    break;
                }

                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.CANCEL_MEDIA);
                viewControlsCallback.clearEditorFragment();
                break;

            case R.id.backButton:
                if (emoticonFragment.textInFocus) {
                    emoticonFragment.minimizeEditor();
                }

                if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
                    toggleEmoticonVisibility();
                }

                if (isScribblingMode) {
                    toggleScribbleButton();
                }

                if (parentStickerFragment != null) {
                    int progressBarVisible = parentStickerFragment.getProgressBarVisibility();
                    exitLocationMode(progressBarVisible == View.VISIBLE);
                } else
                    exitLocationMode(false);

                checkAndToggleBackButton(false);
                break;
            case R.id.soundButton:
                toggleMediaVolume(v);
                break;
            case R.id.timer_Iv:
//                toggleVideoPlaybackSpeed();
                break;
            case R.id.location_IV:
                cancelCustomToast(customToastTut);
                if (emoticonFragment.textInFocus)
                    emoticonFragment.minimizeEditor();

                if (emojiPagerFrame.getVisibility() == View.VISIBLE)
                    toggleEmoticonVisibility();

                hidePopUp();
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SEARCH_LOCATION_STICKER);
                requestedEnterLocationMode();
                break;
            case R.id.addToMyStreamIV:
                if (!getPreferences().getBoolean(AppLibrary.DONT_SHOW_DIALOG_AGAIN, false))
                    showMyStreamConfirmationDialog();
                else shareToStream();
                break;

            case R.id.shareToWhatsAppIV:
                shareToWhatsApp = true;
                rootView.findViewById(R.id.saveButton).performClick();
                break;

            case R.id.btn_positive:
                if (dialog != null) {
                    dialog.dismiss();
                }
                shareToStream();
                if (flag)
                    getPreferences().edit().putBoolean(AppLibrary.DONT_SHOW_DIALOG_AGAIN, flag).commit();
                break;
            default:
                //nothing
                break;
        }
    }


    private void initDiscoverTutorial() {
        if (!AppLibrary.IS_EMOJI_DISCOVER_TUTORIAL_SHOWN_IN_THIS_SESSION && shouldShouldStickerDiscoveryTut()) {
            rootView.findViewById(R.id.discoverStickerIncludedView).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.discoverStickerIncludedView).findViewById(R.id.popupBackgroundIV).setBackgroundResource(R.drawable.sticker_discover_svg);
            rootView.findViewById(R.id.discoverStickerIncludedView).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rootView.findViewById(R.id.emoji_IV).performClick();
                }
            });

        }
    }

    private boolean shouldShouldStickerDiscoveryTut() {
        int previousCount = getPreferences().getInt(AppLibrary.EMOJI_DISCOVER_TUTORIAL, 0);
        int maxCount = AppLibrary.EMOJI_DISCOVER_TUTORIAL_COUNT;
        return previousCount <= maxCount;
    }

    private void hidePopUp() {
        int visibility = rootView.findViewById(R.id.discoverStickerIncludedView).getVisibility();
        if (visibility == View.GONE) return;//already desired visibility

        AppLibrary.IS_EMOJI_DISCOVER_TUTORIAL_SHOWN_IN_THIS_SESSION = true;
        rootView.findViewById(R.id.discoverStickerIncludedView).setVisibility(View.GONE);
        int previousCount = getPreferences().getInt(AppLibrary.EMOJI_DISCOVER_TUTORIAL, 0);
        getPreferences().edit().putInt(AppLibrary.EMOJI_DISCOVER_TUTORIAL, ++previousCount).commit();
    }

    /**
     * will be called for sharing to my stream as well as individual stream
     */
    private void shareToStream() {
        if (controller == null) {
            showShortToastMessage("Oops! Something went wrong. Please try again later!");
            return;
        }

        if (emoticonFragment.textInFocus) {
            emoticonFragment.minimizeEditor();
        }

        if (emojiPagerFrame.getVisibility() == View.VISIBLE) {
            toggleEmoticonVisibility();
        }

        if (isScribblingMode) {
            toggleScribbleButton();
        }

        checkAndToggleBackButton(false);

        toggleUiOnOff(true, "Posting to " + (getPublicMomentModel() == null ? "your Stream" : getPublicMomentModel().name) + "... ");
        gotoShareScreen = false;

        controller.setCanPlay(false);
        sendEditedMedia(false);//preparing bitmaps and all
    }

    Dialog dialog;

    private void showMyStreamConfirmationDialog() {
        final Dialog customDialog = new Dialog(context);
        dialog = customDialog;
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setContentView(R.layout.dialog_my_stream_share);
//        customDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        final AppCompatCheckBox dontShowAgain;
        TextView tvPositive, checkboxText;

        tvPositive = (TextView) customDialog.findViewById(R.id.btn_positive);
        dontShowAgain = (AppCompatCheckBox) customDialog.findViewById(R.id.dont_show_again_checkbox);
        checkboxText = (TextView) customDialog.findViewById(R.id.checkbox_text);

        tvPositive.setOnClickListener(this);
        checkboxText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dontShowAgain.toggle();
            }
        });
        dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flag = isChecked;
            }
        });

//        customDialog.findViewById(R.id.rootView).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismissPopup(customDialog);
//            }
//        });

//        customDialog.findViewById(R.id.popupView).setTranslationY(1000);
//        customDialog.findViewById(R.id.popupView).animate().translationY(0).setDuration(300).start();
//        ObjectAnimator.ofFloat(customDialog.findViewById(R.id.tintFrame), "alpha", 0f, 1f).setDuration(300).start();
        customDialog.show();
    }

    SettingsModel settingsModel;

    /**
     * will add to either a publicly contributable stream or myStram
     */
    private void addMediaToStream() {
        settingsModel = mFireBaseHelper.getSettingsModel();
        String mediaText = emoticonFragment.getMediaText();//Get text to write to firebase
        String mediaPath;
        HashMap<String, String> selectedRoom = null;
        int actionType = PrivacyPopup.ALL_FRIEND_ROW;
        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_IMAGE) {
            mediaPath = editedFile.getAbsolutePath();
        } else {
            mediaPath = mOutFilePath;
        }
        HashMap<String, Integer> selectedMomentList = new HashMap<>();

        if (this.getPublicMomentModel() == null)
            selectedMomentList.put(mFireBaseHelper.getMyUserModel().momentId, MY_MOMENT);
        else selectedMomentList.put(publicMomentModel.momentId, CUSTOM_MOMENT);


        if (settingsModel != null && settingsModel.lastUsedPrivacy != null) {
            if (settingsModel.lastUsedPrivacy.equalsIgnoreCase(PRIVACY_ALL_FRIENDS_AND_FOLLOWERS)) {
                actionType = PrivacyPopup.ALL_FRIENDS_AND_FOLLOWERS;
                selectedRoom = null;
            } else if (settingsModel.lastUsedPrivacy.equalsIgnoreCase(PRIVACY_ALL_FRIENDS)) {
                actionType = PrivacyPopup.ALL_FRIEND_ROW;
                selectedRoom = null;
            } else if (settingsModel.lastUsedPrivacy.equalsIgnoreCase(PRIVACY_FRIENDS_EXCEPT)) {
                actionType = PrivacyPopup.FRIENDS_EXCEPT_ROW;
                selectedRoom = fetchIgnoredList();
            } else {
                actionType = PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW;
                selectedRoom = fetchCustomFriendsList();
            }
        }
        // add false for isAnonymous
        viewControlsCallback.uploadMediaToFireBase(false, false, actionType, selectedRoom, mediaPath, selectedMomentList, null, 0, mediaText, publicMomentModel);
    }

    private HashMap<String, String> fetchCustomFriendsList() {
        if (settingsModel != null && settingsModel.customFriendList != null && settingsModel.lastUsedPrivacy != null) {
            HashMap<String, String> map = new HashMap<>();
            for (Map.Entry<String, SettingsModel.MemberDetails> entry :
                    settingsModel.customFriendList.get(settingsModel.lastUsedPrivacy).members.entrySet()) {
                map.put(entry.getKey(), entry.getValue().roomId);
            }
            return map;
        }
        return null;
    }

    private HashMap<String, String> fetchIgnoredList() {
        if (settingsModel != null && settingsModel.ignoredList != null) {
            HashMap<String, String> map = new HashMap<>();
            for (Map.Entry<String, SettingsModel.MemberDetails> entry : settingsModel.ignoredList.entrySet()) {
                map.put(entry.getKey(), entry.getValue().roomId);
            }
            return map;
        }
        return null;
    }

    public void exitLocationModeOnSingleTap(boolean hideViewPager) {
        exitLocationMode(hideViewPager);
        cancelCustomToast(customToastTut);
    }

//    private void toggleVideoPlaybackSpeed() {
//        controller.toggleVideoPlaybackSpeed();
//    }

    private void toggleMediaVolume(View view) {
        int tag = (int) view.getTag();
        if (tag == R.drawable.sound_off_svg) {
            ((ImageView) view).setImageResource(R.drawable.sound_on_svg);
            view.setTag(R.drawable.sound_on_svg);
            controller.toggleAudio(false);
        } else {
            ((ImageView) view).setImageResource(R.drawable.sound_off_svg);
            view.setTag(R.drawable.sound_off_svg);
            controller.toggleAudio(true);
        }
    }

    private void saveEditedMedia() {
        editFrameLayout.setDrawingCacheEnabled(true);
        editFrameLayout.setDrawingCacheQuality(FrameLayout.DRAWING_CACHE_QUALITY_HIGH);
        editFrameLayout.buildDrawingCache(true);

        //TODO this should give only the overlay (emoticons, text and the doodle) \
        //TODO rather than the full bitmap including the overlay
        Bitmap src = editFrameLayout.getDrawingCache();
        if (src != null) {
            mOverlayBitmap = Bitmap.createBitmap(src);
            src.recycle();
        } else
            mOverlayBitmap = null;

        editFrameLayout.setDrawingCacheEnabled(false);

        if (controller==null) return;

        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_IMAGE) {
            controller.goToShareAction(false);
            justSave = true;
            setImageFilePath(justSave, getActivity(), MEDIA_TYPE, "_saved");
            captureEditedBitmap();
        } else if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_VIDEO) {
            controller.goToShareAction(false);
//            toggleUiOnOff(true, "Saving...");
            justSave = true;
            setVideoFilePath(justSave);
            encodeEditedVideo(mOverlayBitmap);
        }

        toggleUiOnOff(true, shareToWhatsApp ? "Sharing..." : "Saving...");

        if (shareToWhatsApp)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.QUICK_WHATSAPP);
        else
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SAVE_MEDIA);
    }

    private void resetMediaState() {
        View view = rootView.findViewById(R.id.soundButton);
        int tag = (int) view.getTag();
        if (tag == R.drawable.sound_off_svg) {
            ((ImageView) view).setImageResource(R.drawable.sound_off_svg);
        } else {
            ((ImageView) view).setImageResource(R.drawable.sound_on_svg);
        }
        view.setClickable(true);
    }

    private void sendEditedMedia(boolean gotoShareScreen) {
        editFrameLayout.setDrawingCacheEnabled(true);
        editFrameLayout.setDrawingCacheQuality(FrameLayout.DRAWING_CACHE_QUALITY_HIGH);
        editFrameLayout.buildDrawingCache(true);

        //TODO this should give only the overlay (emoticons, text and the doodle) \
        //TODO rather than the full bitmap including the overlay

        Bitmap src = editFrameLayout.getDrawingCache();
        if (src != null) {
            mOverlayBitmap = Bitmap.createBitmap(src);
            src.recycle();
        } else
            mOverlayBitmap = null;

        editFrameLayout.setDrawingCacheEnabled(false);

        if (controller==null) return;

        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_IMAGE) {
            controller.goToShareAction(true);
            justSave = false;
            setImageFilePath(justSave, getActivity(), MEDIA_TYPE, "_share");
            if (gotoShareScreen)
                onShareAction();
            captureEditedBitmap();
        } else if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_VIDEO) {
            controller.goToShareAction(true);
            justSave = false;
            setVideoFilePath(justSave);
            if (gotoShareScreen) {
                onShareAction();
            } else {
                controller.setmVideoEventListener(this);
            }
            encodeEditedVideo(mOverlayBitmap);
        }

        if (!gotoShareScreen)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.QUICK_POST);
        else
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SEND_MEDIA);
    }

    private void requestSaveEditedMedia() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_ACCESS_EXTERNAL_STORAGE);
        } else {
            saveEditedMedia();
        }
    }

    public boolean mSavingVideo;

    private void toggleUiOnOff(boolean blockUi, String text) {
        mSavingVideo = blockUi;
        onEmoticonMovementStateChanged(!blockUi);
        if (blockUi) {
            progessTextView.setText(text);
            progressBarLayout.setVisibility(View.VISIBLE);
            progessTextView.setVisibility(View.VISIBLE);
        } else {
            progressBarLayout.setVisibility(View.GONE);
            progessTextView.setVisibility(View.GONE);
        }
    }

    private void captureEditedBitmap() {
        controller.captureBitmap();
    }

    public void resumeMediaView() {
        if (MEDIA_TYPE == MEDIA_TYPE_IMAGE) {
            prepareImageCapture();
        } else if (MEDIA_TYPE == MEDIA_TYPE_VIDEO) {
            videoPlayback();
        } else {
            Log.w(TAG, "resumeMediaView:: Unknown media type");
        }
    }

    public void videoPlayback() {
        controller.setRenderTargetType(MoviePlayer.RENDER_TARGET_DISPLAY);

        controller.requestStartVideoRendering();
    }

    private void encodeEditedVideo(Bitmap bitmapOverlay) {
        if (controller == null) {
            startVideoDecodeEncode();
        }
        controller.setVideoOverlay(bitmapOverlay);
        controller.setRenderTargetType(MoviePlayer.RENDER_TARGET_VIDEO);

        controller.requestStartVideoRendering();
    }

    private void loadTemplateFragment(ParentStickerFragment parentStickerFragment) {
        if (getActivity() == null) return;

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.themeFrameHolder, parentStickerFragment);
        ft.commitAllowingStateLoss();
    }

    private void loadEmoticonFragment() {
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (emoticonFragment == null)
            emoticonFragment = new EmoticonFragment();
        ft.replace(R.id.emoticonFrameHolder, emoticonFragment, "emoticonFragment");
        ft.commitAllowingStateLoss();
    }

//    private void loadTextFragment() {
//        FragmentManager fm = getFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//        textFragment = new TextFragment();
//        ft.add(R.id.editTextFrame, textFragment, "textFragment");
//        ft.commit();
//    }
//
//    private TextFragment getTextFragment() {
//        return (TextFragment) getFragmentManager().findFragmentByTag("textFragment");
//    }

    ParentEmojiPagerFragment parentEmojiPagerFragment;

    private void loadEmoticonViewPager() {
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (parentEmojiPagerFragment == null)
            parentEmojiPagerFragment = new ParentEmojiPagerFragment();
        ft.replace(R.id.emojiPagerFrame, parentEmojiPagerFragment);
        ft.commitAllowingStateLoss();
    }

    OvalColorPicker ovalColorPicker;

    private void initColorEditor(View rootView) {
//        colorLayout = (LinearLayout) rootView.findViewById(R.id.colorLayout);
//        rootView.findViewById(R.id.redBox).setOnClickListener(colorListener);
//        rootView.findViewById(R.id.greenBox).setOnClickListener(colorListener);
//        rootView.findViewById(R.id.blueBox).setOnClickListener(colorListener);
//        rootView.findViewById(R.id.purpleBox).setOnClickListener(colorListener);
//        addTickMark(rootView.findViewById(R.id.redBox));
//

        ovalColorPicker = (OvalColorPicker) rootView.findViewById(R.id.ovalColorPicker);
        ovalColorPicker.setColorPickerListener(colorPickerListener);
    }


    OvalColorPicker.ColorPickerListener colorPickerListener = new OvalColorPicker.ColorPickerListener() {
        @Override
        public void onColorSelected(int colorInt) {
            scribbleView.setPaintColor(colorInt);
        }

        @Override
        public int onUndoClicked() {
            //            undoIv.setVisibility(pendingListSize > 0 ? View.VISIBLE : View.GONE);
            return scribbleView.undo();
        }
    };


    private Bitmap addOverlayToBitmap(Bitmap bitmap, Bitmap overlay) {
//        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Bitmap result = Bitmap.createBitmap(bitmap);
        Canvas canvas = new Canvas(result);
//        canvas.drawBitmap(bitmap, new Matrix(), null);
        try {
            canvas.drawBitmap(overlay, 0, 0, null);
        } catch (Exception e) {
            e.printStackTrace(); //Couldn't draw overlay
        }
//        bitmap.recycle();
        return result;
    }


    @Override
    public void onCaptureBitmap(Bitmap bitmap,Bitmap watermark) {
        Bitmap result = addOverlayToBitmap(bitmap,mOverlayBitmap);

        if(justSave) //Apply Watermark
            result = addWaterMarkToBitmap(result, watermark);

        if (mOverlayBitmap != null) {
            mOverlayBitmap.recycle();
            mOverlayBitmap = null;
        }

        writeFile(result, editedFile, justSave, gotoShareScreen, new WriteImageCallback() {
            @Override
            public void onWriteImageComplete() {
                if (!justSave)
                    addMediaToStream();
                else if (shareToWhatsApp)
                    shareMediaToWhatsApp(AppLibrary.MEDIA_TYPE_IMAGE);
            }
        }, !this.shareToWhatsApp);
        toggleUiOnOff(false, null);
    }

    private boolean shareToWhatsApp;

    private void shareMediaToWhatsApp(int mediaType) {
        shareToWhatsApp = false;
        launchMediaShareIntent(null, null, AppLibrary.WHATSAPP_PACKAGE_NAME, mediaType, editedFile.getAbsolutePath());

    }

    void addRecordingToMediaLibrary(boolean image) {
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

    private void onShareAction() {
        String mediaText = emoticonFragment.getMediaText();//Get text to write to firebase
        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_IMAGE) {
            viewControlsCallback.launchShareMediaFragment(editedFile.getAbsolutePath(), mediaText);
        } else {
            viewControlsCallback.launchShareMediaFragment(mOutFilePath, mediaText);
        }
    }

    public void setCanPlay(boolean canPlay) {
        if (controller != null) {
            controller.setCanPlay(canPlay);
        }
    }

    @Override
    public void onRecordingStarted() {
        addMediaToStream();
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            Log.d(TAG, " single Tap confirmed");
//            textFragment.notifyTap(event);
            return true;
        }
    }

    public interface ViewControlsCallback {
        void clearEditorFragment();

        void launchShareMediaFragment(String mediaPath, String mediaText);

        void uploadMediaToFireBase(boolean facebookPost, boolean postAnonymous, int action_type, HashMap<String, String> selectedRoomsForMoment, String mediaPath, HashMap<String, Integer> momentList
                , HashMap<String, Integer> roomList, int expiryType, String mediaText, @Nullable CustomMomentModel publicStreamContribution);


        void fetchContributableMoments(Double latitude, Double longitude);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (googleApiClient == null) return;

        googleApiClient.disconnect();
    }

    private Location knownLocation;
    private LinearLayout videoEditorLl;
    private final int PERMISSION_ACCESS_LOCATION = 0;
    private final int PERMISSION_ACCESS_EXTERNAL_STORAGE = 1;
    private GoogleApiClient googleApiClient;
    private boolean isLocationMode;
    private boolean isDestroyed;
    //    private String[] templateIds;
    //    private int templatesReceivedCount = 0;
    private boolean apiGivingAnyTemplates = true;//api doesn't give any template now? it won't later(not with same location)

    private void requestedEnterLocationMode() {
        if (!apiGivingAnyTemplates) {
            Log.d(TAG, " redundant api call returning");
            showShortToastMessage("No geo-filters around you");
            return;
        }

        Log.d(TAG, "requestedEnterLocationMode location " + knownLocation);
        if (knownLocation == null) {
            Log.d(TAG, " startingLocation Update");
            startLocationUpdate();
        } else {
            if (templateModelLinkedHashMap == null)
                getLocationStickersApi(knownLocation.getLatitude(), knownLocation.getLongitude());
            else
                enterLocationMode();

           // viewControlsCallback.fetchContributableMoments(knownLocation.getLatitude(), knownLocation.getLongitude());
        }
    }

    private void enterLocationMode() {

        isLocationMode = true;
        videoEditorLl.setVisibility(View.GONE);
        saveAndSoundLayout.setVisibility(View.GONE);
        themeFrame.setVisibility(View.VISIBLE);
        sendImageView.setVisibility(View.GONE);
        addToMyStreamIv.setVisibility(View.GONE);
        shareToWhatsAppIv.setVisibility(View.GONE);

        if (isPublicStreamContributionMode()) {
            bottomView.setVisibility(View.GONE);
        }

        if (backButton.getVisibility() == View.GONE)
            toggleBackButton();
    }

    private LinkedHashMap<String, LocationTemplateModel> templateModelLinkedHashMap;

    /**
     * @param hideViewPager supply true here if the progress bar in thegetContributable moments viewPager child is visible
     */
    private void exitLocationMode(boolean hideViewPager) {
        videoEditorLl.setVisibility(View.VISIBLE);
        saveAndSoundLayout.setVisibility(View.VISIBLE);
        isLocationMode = false;
        themeFrame.setVisibility(hideViewPager ? View.GONE : View.VISIBLE);

        if (isPublicStreamContributionMode()) {
            bottomView.setVisibility(View.VISIBLE);
        }

        progressBarLayout.setVisibility(View.GONE);
        sendImageView.setVisibility(View.VISIBLE);
        addToMyStreamIv.setVisibility(View.VISIBLE);
        shareToWhatsAppIv.setVisibility(View.VISIBLE);

        if (backButton.getVisibility() == View.VISIBLE)
            toggleBackButton();
    }

    private void startLocationUpdate() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_LOCATION);
        } else {
            showSmartGpsDialog();
        }
    }

    @SuppressWarnings("deprecation")
    private void getLocationStickersApi(double lati, double longi) {
        if (!isInternetAvailable(true)) {
            Log.e(TAG, " not hitting api; no internet");
            exitLocationMode(true);
            return;
        }

        progessTextView.setText("Finding location...");
        progressBarLayout.setVisibility(View.VISIBLE);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("latitude", String.valueOf(lati)));
        pairs.add(new BasicNameValuePair("longitude", String.valueOf(longi)));
        pairs.add(new BasicNameValuePair("userId", myUserId));

        RequestManager.makePostRequest(context, RequestManager.LOCATION_TEMPLATES_REQUEST, RequestManager.LOCATION_TEMPLATE_RESPONSE,
                null, pairs, locationStickerCallback);
    }

    View progressBarLayout;
    TextView progessTextView;
    private RequestManager.OnRequestFinishCallback locationStickerCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            progressBarLayout.setVisibility(View.GONE);
            try {
                final JSONObject object = (JSONObject) response;
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        Log.d(TAG, "getNearByTemplates Success, response -" + object.getString("value"));
                        JSONArray templateIdArray = object.getJSONArray("value");
                        if (templateModelLinkedHashMap != null) {
                            Log.e(TAG, " multiple locationRequestsToApi returning");
                            return;
                        }

                        if (templateIdArray.length() == 0) {
                            apiGivingAnyTemplates = false;
                            showShortToastMessage("No geo-filters around you");
                            exitLocationMode(true);
                            return;
                        }
                        parseTemplatesFromJson(object.getJSONArray("value"));

                        if (templateModelLinkedHashMap != null) {
                            if (!getPreferences().getBoolean(AppLibrary.LOCATION_TUT_SHOWN, false)) {
                                // tutorial toast for filters
                                customToastTut = showCustomToast(getActivity(), R.layout.tut4, Gravity.CENTER, 0, 0, 7000);
                                getPreferences().edit().putBoolean(AppLibrary.LOCATION_TUT_SHOWN, true).commit();
                            }

                            enterLocationMode();
                        }
                    } else {
                        Log.e(TAG, "getNearByTemplates Error, response -" + object.getString("value"));
                    }
                } else {
                    exitLocationMode(true);
                    Log.e(TAG, "getNearByTemplates Error, response -" + object);
                    // request failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "getNearByTemplates JsonException " + e);
            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

    private void parseTemplatesFromJson(JSONArray templateArray) {
        if (templateModelLinkedHashMap == null) {
            templateModelLinkedHashMap = new LinkedHashMap<>();
        }
        try {
            for (int i = 0; i < templateArray.length(); i++) {
                final JSONObject templateObject = (JSONObject) templateArray.get(i);
                LocationTemplateModel model = new LocationTemplateModel();
                model.name = templateObject.getString("name");


                HashMap<String, LocationTemplateModel.LocationSticker> stickersMap = new HashMap<>();

                final JSONArray stickersArray = templateObject.getJSONArray("stickers");
                for (int j = 0; j < stickersArray.length(); j++) {
                    final JSONObject stickerObject = (JSONObject) stickersArray.get(j);
                    LocationTemplateModel.LocationSticker sticker = new LocationTemplateModel.LocationSticker();
                    sticker.mStickerId = stickerObject.getString("id");
                    sticker.marginTop = (float) stickerObject.getDouble("marginTop");
                    sticker.marginLeft = (float) stickerObject.getDouble("marginLeft");
                    sticker.width = (float) stickerObject.getDouble("width");
                    sticker.height = (float) stickerObject.getDouble("height");
                    sticker.degree = stickerObject.getInt("degree");
                    sticker.mStickerId = stickerObject.getString("id");
                    sticker.pictureUrl = stickerObject.getString("pictureUrl");
                    stickersMap.put(sticker.mStickerId, sticker);
                }
                model.stickers = stickersMap;
                templateModelLinkedHashMap.put(templateObject.getString("_id"), model);

            }
        } catch (JSONException e) {
            e.printStackTrace();
            templateModelLinkedHashMap = null;
            return;
        }

        int i = 0;
        for (Map.Entry<String, LocationTemplateModel> entry : templateModelLinkedHashMap.entrySet()) {
            ++i;
            notifyDownloader(entry.getKey(), entry.getValue(), i);
        }
    }


    void notifyDownloader(String templateId, LocationTemplateModel templateModel, int count) {
        TemplateDownloader templateDownloader = TemplateDownloader.getInstance(context);
        templateDownloader.notifyTemplateLoadedFromFireBase(templateId, templateModel);

        if (count == templateModelLinkedHashMap.size()) {
            templateModelLinkedHashMap.put(CLEAR_SCREEN, null);//now that we have some themes lets add clear screen as well
            templateDownloader.setTemplateLinkedMap(templateModelLinkedHashMap);
            ParentStickerFragment.PAGE_COUNT = templateModelLinkedHashMap.size();

            if (parentStickerFragment == null)
                parentStickerFragment = new ParentStickerFragment();
            parentStickerFragment.setThemeNames(templateModelLinkedHashMap);

            Log.d(TAG, " filledTemplateModel for " + templateModelLinkedHashMap.size() + " screens");
            loadTemplateFragment(parentStickerFragment);
        }
    }

    private void showSmartGpsDialog() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(3 * 1000);

        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            progessTextView.setText("Finding location...");
                            progressBarLayout.setVisibility(View.VISIBLE);
                            enterLocationMode();

//                            Location location = getLocation();
//                            if (location != null)
//                                startActualLocationUpdate(location);
//                            else
                            LocationServices.FusedLocationApi.requestLocationUpdates(   //Get high quality location
                                    googleApiClient, locationRequest, VideoEditorFragment.this);

                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        showGpsDialog(); //Fallback - Show custom dialog for location settings
                        break;
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (location.getAccuracy() < 100) {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                startActualLocationUpdate(location);
            } else {
                //Continue listening
            }
        } else {
            exitLocationMode(true);
        }
    }

    private void startActualLocationUpdate(Location location) {
        knownLocation = location;
        double lat = knownLocation.getLatitude(), lon = knownLocation.getLongitude();
        getLocationStickersApi(lat, lon);
       // viewControlsCallback.fetchContributableMoments(lat, lon);

        if (getActivity() != null && ((CameraActivity) getActivity()).getDashboardFragment() != null && ((CameraActivity) getActivity()).getDashboardFragment().aroundYouFragment != null)
            ((CameraActivity) getActivity()).getDashboardFragment().aroundYouFragment.refreshAroundYouData(lat, lon);   //Refresh aroundYou

        Log.d(TAG, " getLocation: lat " + lat + " long " + lon);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        if (googleApiClient != null && googleApiClient.isConnected()) {
                            startLocationUpdate(); //Restart location update to get location
                        } else {
                            requestLocationUpdate = true;
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        showShortToastMessage(" Please give location access to tag");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    private void showGpsDialog() {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_enable_gps);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.findViewById(R.id.rootView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        Button enableBtn = (Button) dialog.findViewById(R.id.enableBTN),
                denyBtn = (Button) dialog.findViewById(R.id.denyBTN);
        enableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gpsOptionsIntent = new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(gpsOptionsIntent, REQUEST_CHECK_SETTINGS);
                dialog.dismiss();
            }
        });
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private boolean requestApproximateLocation = false;
    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, " onConnected " + bundle);

            if (requestApproximateLocation) {
                Location location = getLocation();
                if (location != null) {
            //        viewControlsCallback.fetchContributableMoments(location.getLatitude(), location.getLongitude());
                } else {
                   // viewControlsCallback.fetchContributableMoments(null, null);
                }
                requestApproximateLocation = false;
            }

            if (requestLocationUpdate) {
                startLocationUpdate();
                requestLocationUpdate = false;
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, " onConnectionSuspended " + i);

        }
    };

    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, " onConnectionFailed " + connectionResult);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (googleApiClient != null && googleApiClient.isConnected()) {
                        startLocationUpdate(); //Restart location update to get location
                    } else {
                        requestLocationUpdate = true;
                    }
                } else {
                    showShortToastMessage(" Please give location access to tag");
                }
                break;
            case PERMISSION_ACCESS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestSaveEditedMedia(); //Now save the file
                } else {
                    showShortToastMessage(" Please provide access to save to Gallery");
                }
                break;
        }
    }

    private Location getLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            Log.d(TAG, " getLastLocation " + lastLocation);
            return lastLocation;
        } else {
            Log.e(TAG, " getLastLocation null");
            return null;
        }
    }

    public void removeTuts() {
        cancelCustomToast(customToastTut);
    }

    private CustomMomentModel publicMomentModel;

    private CustomMomentModel getPublicMomentModel() {
        return publicMomentModel;
    }

    public void setPublicMomentModel(CustomMomentModel publicMomentModel) {
        this.publicMomentModel = publicMomentModel;
        if (publicMomentModel != null)
            CameraActivity.contributableMoments.put(publicMomentModel.momentId, publicMomentModel);
    }


    private void initPublicContributionView() {
        this.bottomView = rootView.findViewById(R.id.contributionIncludedView);
        super.initPublicContributionView(rootView, publicMomentModel, new OnPublicContributionListener() {
            @Override
            public void onContributeToMoreStreams() {
                rootView.findViewById(R.id.send).performClick();
                contributionDialog = null;
            }

            @Override
            public void onContributeNow() {
//                ((CameraActivity) getActivity()).onExitContributionToPublicStream();
                shareToStream();
            }
        });
    }

    private boolean actionPerformed() {

        if (emoticonFragment != null && emoticonFragment.doesHaveAnyStickers())
            return true;
        if (scribbleView != null && scribbleView.getPathListSize() > 0)
            return true;
        if (parentStickerFragment != null && !parentStickerFragment.isClearScreenSelected())
            return true;

        return false;
    }

    private Dialog contributionDialog;

    /**
     * @return true if want to consume the harware back
     * false otherwise
     */
    public boolean checkAndShowPopup() {

        if (discardPressed)
            return false;

        if (!actionPerformed())
            return false;

        if (contributionDialog == null) {
            contributionDialog = showCustomDialog(false, getActivity(), null, "Are you sure you want to discard your masterpiece?"
                    , "DISCARD", "CANCEL", customBtnListener);

            contributionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(final DialogInterface arg0) {
                    contributionDialog = null;
                    if (getActivity() != null)
                        ((CameraActivity) getActivity()).toggleFullScreen(true);
                    else
                        Log.e(TAG, "onDismiss: null.");
                }
            });
            return true;
        } else {
            contributionDialog.dismiss();
            return false;
        }
    }

    boolean discardPressed = false;
    private View.OnClickListener customBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_positive:
                    contributionDialog.dismiss();
                    discardPressed = true;
                    getActivity().onBackPressed();
                    break;
                case R.id.btn_negative:
                    contributionDialog.dismiss();
                    break;
            }
        }
    };

    private boolean isPublicStreamContributionMode() {//for posting to contributable stream
        return ((CameraActivity) getActivity()).isPublicStreamContributionMode();
    }

    View bottomView;

//    void toggleBottomBar(boolean makeVisible) {
//        bottomView.setVisibility(makeVisible ? View.VISIBLE : View.GONE);
//
//    }
}
