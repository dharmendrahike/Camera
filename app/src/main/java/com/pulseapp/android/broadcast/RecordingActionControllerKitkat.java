package com.pulseapp.android.broadcast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.broadcast.RecordingMuxer.MediaFrame;
import com.pulseapp.android.gles.InputSurface;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.stickers.ThemeModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BlurBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

//import com.instalively.android.stickers.StickerModel;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;

/**
 * Created by bajaj on 21/1/16.
 */
@SuppressLint("LongLogTag")
public class RecordingActionControllerKitkat extends StreamingActionController implements
        AdapterView.OnItemSelectedListener,
        TextureMovieEncoder.Callback,
        VideoEncoderCore.Callback,
        RecordingMuxer.Callback,
        CameraCapture.CameraCaptureCallback,
        MoviePlayer.PlayerFeedback,
        MoviePlayer.FrameReceiver {
    protected static final String TAG = "RecordingActionController";
    private static final boolean VERBOSE = false;
    private static final boolean RECORD_STREAM = true;
    public static boolean LIVE_STREAM = false;
    /*    private final GestureDetector mGestureListener;
        private final ScaleGestureDetector mScaleListener;*/
    private final boolean OFFSCREEN_ENABLED = false;
    public ImageView mImageView;
    private int mEncoderWidth;
    private int mEncoderHeight;
    private GLSurfaceView mGLSurfaceView;
    //    private TimeSync timeSync;
    private CameraHandler mCameraHandler;
    private SurfaceRenderer mRenderer;
    private SurfaceTexture mSurfaceTexture = null; //Handle with care
    private long lastSentVideoTS = 0;
    private long lastSentAudioTS = 0;
    private boolean isCameraPaused = false;
    private boolean doAudioCheck = false;
    private int audioCorrectionCounter = 0;
    public long pausedAudioDuration = 0;
    private RecordingMuxer mRecordingMuxer = null;
    private String mOutputDir = "";

    /**
     * Renderer, when ready, will check for the flag before starting the rendering.
     * This is useful especially when requestVideoRendering and the playerReady callback
     * are both in different threads.
     *
     * Used to indicate if rendering should begin again, if it was stopped for some reason.
     * The reason could be either successful playback cycle and the video set to loop, or
     * the user pressing 'save'/'send' button at any time in the editor page.
     * The rendering could be to video/display or both depending on the conditions
     */
    private boolean requestPlayback;
    private Bitmap mBitmapOverlay;
    public int mMediaType;
    private String mCaptureImagePath;
    private Bitmap mCapturedBitmap;
    private int mPrevRenderTargetType;
    private boolean canPlay;
    private boolean goToShareAction;
    private Bitmap mWaterMarkBitmap;

    public void setImageEventListener(ImageEventListener imageEventListener) {
        mImageEventListener = imageEventListener;
    }

    public void setmVideoEventListener(VideoEventListener videoEventListener){
        mVideoEventListener = videoEventListener;
    }

    public void setVideoLifecycleListener(VideoLifecycleEventListener videoLifecycleListener) {
        mVideoLifecycleListener = videoLifecycleListener;
    }

    private VideoLifecycleEventListener mVideoLifecycleListener;
    private ImageEventListener mImageEventListener;
    private VideoEventListener mVideoEventListener;

    public String getSourceMediaFile() {
        return mSourceMediaFile;
    }

    public void setSourceMediaFile(String sourceMediaFile, int mediaType) {
        Log.d(TAG, "setSourceMediaFile: " + sourceMediaFile);
        mSourceMediaFile = sourceMediaFile;
        mCapturedBitmap = null;
        setMediaType(mediaType);
    }

    private void setMediaType(int mediaType) {
        mMediaType = mediaType;
        mRenderer.setIsPlayImageStream(mMediaType == AppLibrary.MEDIA_TYPE_VIDEO);
        if(mMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            mRenderer.setImageBackground(mCapturedBitmap);
        }
    }

    private String mSourceMediaFile;

    public String getLastRecodedFilepath() {
        return mLastRecordedFilepath;
    }

    public void setLastRecodedFilepath(String lastRecodedFilepath) {
        mLastRecordedFilepath = lastRecodedFilepath;
    }

    private String mLastRecordedFilepath = "";
    private boolean recordingStarted = false;
    private boolean sendFramesToRecorder = false;
    private MediaActionSound mSound = null;
    private TransferObserver upload;
    //    CameraActivity.BroadCastStatus status;
    private int videoBitate = 0;
    private GestureDetector mGestureListener = null;
    private ScaleGestureDetector mScaleListener = null;
    private CameraHandlerThread mHandlerThread = null;
    private CameraHandlerThread mSensorThread = null;
    private Handler mSensorHandler;
    public long AudioStartTimestampDelta = 0;
    private long tempPauseVideoTS = 0;
    private int rtmpErrorCounter = 0;
    private boolean isError = false;

    private MoviePlayer mMoviePlayer;
    private InputSurface mDecoderSurface;
    private MoviePlayer.PlayTask mPlayTask;

    private int mRenderTargetType; //bit masks, to accommodate multiple render targets

    /*
    Thread for stopping and destroying RtmpMuxer
     */
    private CameraHandlerThread mRtmpMuxerAsyncStartStopThread = null;


    //Audio-only mode related
    byte[] smallestFoundType1NAL = null;
    private static final int AUDIO_MODE_KEYFRAME_INTERVAL = 10;
    private int mAudioModeKeyframeCounter = 0;
    private byte[] lastSentType5NAL = null;
    private int bitrateReductionsConsecutiveCounter = 0;
    private boolean continueWithoutBitrateChecks = false;
    private int THRESHOLD_HIGHEST_VIDEO_BITRATE = 450;
    private final int THRESHOLD_LOWEST_VIDEO_BITRATE = 150;
    private int BITRATE_DROP_PER_CYCLE = 40;
    private int BITRATE_INCREASE_PER_CYCLE = 25;


    //Music playback related objects
    private BackgroundMusicPlayer mMusicPlayer = null;
    //TODO: Clean below
    private int tempMusicStartPosition = 2000;
    private int tempMusicStopPosition = 2 * 60000;
    private String tempMusicFile = "file:///storage/sdcard1/Music/Test.mp3";
    private String temMusicFile1 = "file:///storage/sdcard1/Music/Test1.mp3";

    private boolean mSmartFocusMode = true;

    // Camera filters; must match up with cameraFilterNames in strings.xml
    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_BLUR = 2;
    static final int FILTER_SHARPEN = 3;
    static final int FILTER_EDGE_DETECT = 4;
    static final int FILTER_EMBOSS = 5;

    private float mZoomLevel = 1f;

    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();

    /**
     * Camera Focus parameters
     */
    private int SENSOR_UPDATE_INTERVAL = 150; //ms
    Sensor mAcceleroSensor;
    float[] mLinearAcceleration = new float[3];
    private SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;

    private float[] gravity = {0.0f, 0.0f, 0.0f};
    private float[] velGravity = {0.0f, 0.0f, 0.0f};

    private long mCurrTimeStamp, mPrevTimeStamp;
    private float[] mVel = {0.0f, 0.0f, 0.0f};
    private float[] mRawVel = {0.0f, 0.0f, 0.0f};

    private static boolean mMoving = false;
    private float DEFAULT_VEL_HIGH_THRESHOLD = 40.0f;
    private float DEFAULT_VEL_SUPERHIGH_THRESHOLD = 55.0f;
    private volatile float VEL_HIGH_THRESHOLD = 40.0f;
    private float VEL_LOW_THRESHOLD = 10.0f;

    private CameraFocusCallback mCameraFocusCallback;
    private long mLastCameraFocusTime = 0;

    Runnable mSmoothFocusRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("FOCUS", "FOCUS MODE VIDEO");
            if (camera != null) {
                try {
                    camera.cancelAutoFocus();
                    Camera.Parameters params = camera.getParameters();
                    if (params != null) {
                        params.setFocusMode(smoothFocusMode);  //The sensor calls come for back-camera only
                        camera.setParameters(params);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    Runnable mSharpOnceFocusRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("FOCUS", "FOCUS MODE FIXED");
            if (camera != null) {
                try {
                    camera.autoFocus(mCameraFocusCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    CameraCapture mCameraCapturer;
    DummySurfaceRenderer mDummyRenderer;

    private FaceDetector mFaceDetector;
    private CustomHandlerThread mCaptureResultHandlerThread;
    private Handler mCaptureResultHandler;
    //    private static boolean mThumbnailCaptureMode;
//    private boolean mGIFCaptureMode;
    private String mThumbnailCaptureImageName;
    private Bitmap mThumbnailFaceBitmap;
    private Bitmap mThumbnailStaticBitmap;
    private Bitmap mThumbnailNonStaticBitmap;
    private Bitmap mPotentialFaceBitmap;

    private LinkedBlockingQueue<Bitmap> mCapturedBitmapConsumerQueue;
    private LinkedBlockingQueue<Bitmap> mCapturedBitmapProducerQueue;

    private static int mCaptureMode;
    private int mCurrentCaptureFilter;

    public static int CAPTURE_MODE_NONE = 0;

    public static int CAPTURE_MODE_THUMBNAIL = 0x01;
    public static int CAPTURE_MODE_GIF = 0x02;
    public static int CAPTURE_MODE_FACE = 0x04;
    public static int CAPTURE_MODE_FILTER = 0x08;
    public static int CAPTURE_MODE_BACKGROUND = 0x10;
    public static int CAPTURE_MODE_CURRENT_FRAME = 0x20;
    private boolean mVideoBackgroundEnabled = false;

    @Override
    public void processCaptureFrame() {
        if (mCaptureResultHandler != null) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_HANDLE_CAPTURE));
        }
    }

    @Override
    public void processCaptureFrame(int tag) {
/*        if((mCaptureMode & CAPTURE_MODE_FILTER) == 0) {
            Log.w(TAG, "Frame received for filter mode by mistake");
            return;
        }*/
        if (mCaptureResultHandler != null) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_HANDLE_CAPTURE_FILTER, tag, 0));
        }
    }

    public void handleCaptureFrame() {
        Log.d(TAG, "capture mode is " + mCaptureMode);
        Bitmap bitmap = mCapturedBitmapProducerQueue.poll();
        Log.d(TAG, "Camera Capture callback processCaptureFrame with bitmap " + bitmap);

        if (bitmap == null) {
            Log.e(TAG, "processCaptureFrame bitmap captured is null");
            return;
        }

        boolean moving = mMoving;

        if ((mCaptureMode & CAPTURE_MODE_CURRENT_FRAME) > 0) {
            mCaptureMode &= ~CAPTURE_MODE_CURRENT_FRAME;
            if (mCaptureResultHandler != null) {
                Bitmap finall = BlurBuilder.blur(mContext, bitmap, 0.5f, 24.0f);
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_CURRENT_FRAME, finall));
                if (!mCapturedBitmapConsumerQueue.offer(bitmap)) {
                    Log.d(TAG, "Failed to add bitmap to queue");
                }
                return;
            }
        }
        if ((mCaptureMode & CAPTURE_MODE_THUMBNAIL) > 0) {
            if (moving && mThumbnailNonStaticBitmap == null) {
                mThumbnailNonStaticBitmap = bitmap.copy(bitmap.getConfig(), false);
            }
            if (!moving && mThumbnailStaticBitmap == null) {
                mThumbnailStaticBitmap = bitmap.copy(bitmap.getConfig(), false);
            }
        }

        if ((mCaptureMode & CAPTURE_MODE_GIF) > 0) {
            if (mCaptureResultHandler != null) {
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_GIF, bitmap));
            }
            mCaptureMode &= ~CAPTURE_MODE_GIF;
            if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
        }

        if ((mCaptureMode & CAPTURE_MODE_BACKGROUND) > 0) {
            Log.d(TAG, "Processing capture mode frame for video background");
            if (mCaptureResultHandler != null) {
//                Log.d(TAG, "Set message for video background");
//                float [] colors = extractColors(bitmap);
                Bitmap blurBitmap = BlurBuilder.blur(mContext, bitmap, 0.5f, 16.0f);
                if (blurBitmap != null && mCaptureResultHandler != null) {
                    mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_VIDEO_BACKGROUND, 0, 0, blurBitmap));
                }
/*                if(colors != null && mCaptureResultHandler != null) {
                    mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_VIDEO_BACKGROUND, 0, 0, colors));
                }*/
            }
            mCaptureMode &= ~CAPTURE_MODE_BACKGROUND;
            if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
        }

        if ((mCaptureMode & (CAPTURE_MODE_FACE | CAPTURE_MODE_THUMBNAIL)) > 0) {
            if ((mCaptureMode & CAPTURE_MODE_THUMBNAIL) > 0) {
                // In thumbnail mode,
                // No need to go further if we have already got a face thumbnail ready.
                if (mThumbnailFaceBitmap != null) {
                    if (!mCapturedBitmapConsumerQueue.offer(bitmap)) {
                        Log.d(TAG, "Failed to add bitmap to queue");
                    }
                    return;
                }
            }

            if (mCaptureResultHandler != null) {
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_FRAME, bitmap));
            }
        } else {
            if (!mCapturedBitmapConsumerQueue.offer(bitmap)) {
                Log.d(TAG, "Failed to add bitmap to queue");
            }
            return;
        }
    }

    public void handleCaptureFrame(int tag) {
        mCurrentCaptureFilter = tag;

        if ((tag > (FilterManager.FILTER_COUNT - 1)))

        {
            Log.w(TAG, "Frame received for filter out of bounds tag " + tag);
            return;
        }

        Bitmap bitmap = mCapturedBitmapProducerQueue.poll();
//        Log.d(TAG, "Filtered Camera Capture callback processCaptureFrame with bitmap " + bitmap + " for tag " + tag);

        if (bitmap == null)

        {
            Log.e(TAG, "processCaptureFrame bitmap captured is null");
            return;
        }


        if ((mCaptureMode & CAPTURE_MODE_CURRENT_FRAME) > 0)

        {
            mCaptureMode &= ~CAPTURE_MODE_CURRENT_FRAME;
            if (mCaptureResultHandler != null) {
                Bitmap finall = BlurBuilder.blur(mContext, bitmap, 0.5f, 24.0f);
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_CURRENT_FRAME, finall));
                if (!mCapturedBitmapConsumerQueue.offer(bitmap)) {
                    Log.d(TAG, "Failed to add bitmap to queue");
                }
                return;
            }
        }

        Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        mCapturedBitmapConsumerQueue.offer(bitmap);

        if ((mCaptureMode & CAPTURE_MODE_GIF) > 0)

        {
            if (mCaptureResultHandler != null) {
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_GIF, bitmap));
            }
            mCaptureMode &= ~CAPTURE_MODE_GIF;
            if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
        }

        if ((mCaptureMode & CAPTURE_MODE_BACKGROUND) > 0)

        {
//            Log.d(TAG, "Processing capture mode frame for video background");
            if (mCaptureResultHandler != null) {
//                Log.d(TAG, "Set message for video background");
//            float [] colors = extractColors(bitmap);

                Bitmap blurBitmap = BlurBuilder.blur(mContext, bitmap, 0.5f, 16.0f);
                if (blurBitmap != null && mCaptureResultHandler != null) {
                    mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_VIDEO_BACKGROUND, 0, 0, blurBitmap));
                }
/*            if(colors != null && mCaptureResultHandler != null) {
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_VIDEO_BACKGROUND, 0, 0, colors));
            }*/
            }
            mCaptureMode &= ~CAPTURE_MODE_BACKGROUND;
            if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
        }

//        Log.d(TAG, "sending message for PROCESS FILTER for filter " + tag);

        if (mCaptureResultHandler != null)

        {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_FILTER, tag, 0, copy));
        }

        if (tag >= (FilterManager.FILTER_COUNT - 1))

        {
            mCaptureMode &= ~CAPTURE_MODE_FILTER;
            if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
            mCameraCapturer.setFilter(0);
            return;
        }

        //Filters start from index 1 for Cameracapturer. So rebasing tag to 1 from 0 by adding an extra 1
        mCameraCapturer.setFilter((tag + 1) + 1);
    }

    float[] convertBGRAtoRGBA(float[] colors) {
        float[] colorsF = new float[]{0f, 0f, 0f, 0f};
        for (int j = 0; j < 3; j++) {
            colorsF[j] = colors[2 - j];
        }
        colorsF[3] = colors[3];
        return colorsF;
    }

    // Takes pixel data and returns the avg normalized to 1 as floats
    float[] getAvgColor(int[] pixels) {
        long cc[] = new long[]{0, 0, 0, 0};

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < 4; j++) {
                cc[j] += ((pixels[i] >> (8 * j)) & 0xFF);
            }
        }
        for (int j = 0; j < 4; j++) {
            cc[j] /= pixels.length;
        }
        float[] colors = new float[]{0f, 0f, 0f, 0f};
        for (int j = 0; j < 4; j++) {
            colors[j] = cc[j];
            colors[j] /= 255.0f;
        }
        return colors;
    }

    private float[] extractColors(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.setScale(0.25f, 0.25f);

        Bitmap uBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight() / 2, matrix, false);
        float[] colors = new float[16];
        float[] bgra, rgba;

        int[] uPixels = new int[uBitmap.getWidth() * uBitmap.getHeight()];
        uBitmap.getPixels(uPixels, 0, uBitmap.getWidth(), 0, 0, uBitmap.getWidth(), uBitmap.getHeight());
        bgra = getAvgColor(uPixels);
        rgba = convertBGRAtoRGBA(bgra);

        for (int i = 2; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                colors[4 * i + j] = rgba[j];
            }
        }

        Bitmap lBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight() / 2, matrix, false);
        int[] lPixels = new int[lBitmap.getWidth() * lBitmap.getHeight()];
        lBitmap.getPixels(lPixels, 0, lBitmap.getWidth(), 0, 0, lBitmap.getWidth(), lBitmap.getHeight());
        bgra = getAvgColor(lPixels);
        rgba = convertBGRAtoRGBA(bgra);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                colors[4 * i + j] = rgba[j];
            }
        }

        return colors;
    }

    @Override
    public void setBitmapQueues(LinkedBlockingQueue<Bitmap>[] bitmapQueues) {
        mCapturedBitmapProducerQueue = bitmapQueues[0];
        mCapturedBitmapConsumerQueue = bitmapQueues[1];
    }

    public void setOutFilePath(String outFilePath) {
        mLastRecordedFilepath = outFilePath;
    }

    public void setZoom(float zoom) {
        mRenderer.setZoom(zoom);
    }

    public void setRenderTargetType(int renderTargetType) {
        mRenderTargetType = renderTargetType;
        if(mRenderer != null) {
            mRenderer.setRenderTargetType(mRenderTargetType);
        }
    }

    public void setVideoOverlay(Bitmap bitmapOverlay) {
        mBitmapOverlay = bitmapOverlay;
    }

    public void setSourceMedia(Bitmap capturedBitmap, int mediaTypeImage) {
        Log.d(TAG, "setSourceMedia: " + capturedBitmap);
        mCapturedBitmap = capturedBitmap;
        mSourceMediaFile = null;
        setMediaType(mediaTypeImage);
    }

    public void setOverlayView(ImageView imageView) {
        mImageView = imageView;
        mRenderer.setOverlayView(mImageView);
    }

    public void setCanPlay(boolean canPlay) {
        this.canPlay = canPlay;
    }

    public interface VideoLifecycleEventListener {
        void videoPlaybackStarted();

        void videoPlaybackPaused();

        void videoPlaybackStopped();
        void closeVideoPlayback();
        void videoSaved();
    }

    public void captureBitmap() {
        mRenderer.requestCaptureBitmap();
    }

    public int decrementFilter() {
        int currentFilter = mRenderer.decrementFilter();
        if(mMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            mRenderer.requestRender(false);
        }
        return currentFilter;
    }

    public int incrementFilter() {
        int currentFilter = mRenderer.incrementFilter();
        if(mMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            mRenderer.requestRender(false);
        }
        return currentFilter;
    }

    public interface ImageEventListener {
        void onCaptureBitmap(Bitmap bitmap,Bitmap watermark);
    }

    public interface VideoEventListener{
        void onRecordingStarted();
    }

/*    @Override
    public void processCaptureFrame(Bitmap bitmap) {
        Log.d(TAG, "Camera Capture callback processCaptureFrame with bitmap " + bitmap);
        boolean moving = mMoving;

        if(mCaptureMode == CAPTURE_MODE_THUMBNAIL) {
            if(moving && mThumbnailNonStaticBitmap == null) {
                mThumbnailNonStaticBitmap = bitmap;
            }
            if(!moving && mThumbnailStaticBitmap == null) {
                mThumbnailStaticBitmap = bitmap;
            }

            if(mThumbnailFaceBitmap != null) {
                mCaptureMode = CAPTURE_MODE_NONE;
                return;
            }
        } else if(mCaptureMode == CAPTURE_MODE_GIF) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_GIF, bitmap));
        }

        if(mCaptureMode == CAPTURE_MODE_FACE || mCaptureMode == CAPTURE_MODE_THUMBNAIL) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_FRAME, bitmap));
        } else {
            Bitmap copy;
            if(mCaptureMode == CAPTURE_MODE_FILTER) {
                copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_FILTER, 0, 0, copy));
            }
            mCapturedBitmapQueue.offer(bitmap);
        }
    }*/

    public static class CustomHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CustomHandlerThread(RecordingActionControllerKitkat controller, String tag) {
            super(tag);
            start();
            mHandler = new CustomHandler(controller, getLooper());
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    public static final int MSG_PROCESS_FRAME = 1;
    public static final int MSG_PROCESS_THUMBNAIL = 2;
    public static final int MSG_PROCESS_GIF = 3;
    public static final int MSG_PROCESS_FILTER = 4;
    public static final int MSG_PROCESS_VIDEO_BACKGROUND = 5;
    public static final int MSG_PROCESS_CURRENT_FRAME = 6;

    public static final int MSG_HANDLE_CAPTURE = 101;
    public static final int MSG_HANDLE_CAPTURE_FILTER = 102;

    /**
     * Handles encoder state change requests.  The handler is created on the Camera Capture thread.
     */
    private static class CustomHandler extends Handler {
        private WeakReference<RecordingActionControllerKitkat> mCameraCaptureWeakRef;

        public CustomHandler(RecordingActionControllerKitkat controller) {
            mCameraCaptureWeakRef = new WeakReference<RecordingActionControllerKitkat>(controller);
        }

        public CustomHandler(RecordingActionControllerKitkat controller, Looper looper) {
            super(looper);
            mCameraCaptureWeakRef = new WeakReference<RecordingActionControllerKitkat>(controller);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            RecordingActionControllerKitkat controller = mCameraCaptureWeakRef.get();
            if (controller == null) {
                Log.w(TAG, "CameraCapture handleMessage: cameraCapturer is null");
                return;
            }

            switch (what) {
                case MSG_PROCESS_FRAME:
                    controller.handleProcessFrame((Bitmap) obj);
                    break;
                case MSG_PROCESS_THUMBNAIL:
//                    controller.handleProcessThumbnail((inputMessage.arg1 == 0) ? false : true);
                    controller.handleProcessThumbnail();
                    break;
                case MSG_PROCESS_GIF:
                    break;
                case MSG_PROCESS_CURRENT_FRAME:
                    controller.stopCameraSwapAnimation(((Bitmap) obj));
                    controller.restartController();
                    break;
                case MSG_PROCESS_FILTER:
                    int filter = inputMessage.arg1;
                    controller.handleProcessFilterImage((Bitmap) obj, filter);
                    break;
                case MSG_PROCESS_VIDEO_BACKGROUND:
                    if (obj instanceof Bitmap) {
                        controller.handleProcessVideoBackground((Bitmap) obj);
                    } else {
                        controller.handleProcessVideoBackground((float[]) obj);
                    }
                    break;
                case MSG_HANDLE_CAPTURE:
                    controller.handleCaptureFrame();
                    break;
                case MSG_HANDLE_CAPTURE_FILTER:
                    controller.handleCaptureFrame(inputMessage.arg1);
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void stopCameraSwapAnimation(Bitmap bitmap) {
        mRenderer.stopCameraSwapAnimation(bitmap);
    }

    private void handleProcessFrame(Bitmap bitmap) {
        if (mFaceDetector != null) {
            mPotentialFaceBitmap = bitmap;
            if (mFaceDetector.isOperational()) {
                Log.d(TAG, "onPreviewFrame :: sending to Face Detector");
                try {
                    mFaceDetector.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
                } catch (Exception e) {
                    mCapturedBitmapConsumerQueue.offer(bitmap);
                    e.printStackTrace();
                }
            } else {
                mCapturedBitmapConsumerQueue.offer(bitmap);
                Log.d(TAG, "onPreviewFrame :: Face Detector seems to have been released");
            }
        } else {
            mCapturedBitmapConsumerQueue.offer(bitmap);
        }
    }

    private void handleProcessFilterImage(Bitmap bitmap, int filter) {
        if (VERBOSE) Log.d(TAG, "handleProcessFilterImage for filter " + filter);
//        ((CameraActivity) mContext).handleFilterThumbnail(bitmap, filter);
    }

    private void handleProcessVideoBackground(Bitmap bitmap) {
//        Log.d(TAG, "handleProcess video background");
        mRenderer.setInitialTime(System.currentTimeMillis());
        mRenderer.setVideoBackground(bitmap);
    }

    private void handleProcessVideoBackground(float[] colors) {
        mRenderer.setInitialTime(System.currentTimeMillis());
        mRenderer.setVideoBackground(colors);
    }

    private void handleProcessThumbnail() {
        mCaptureMode &= ~CAPTURE_MODE_THUMBNAIL;
        if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();

        if ((mCaptureMode & CAPTURE_MODE_FILTER) > 0) {
            mCaptureResultHandler.post(mFilterCaptureRunnable);
        }
        Bitmap bitmap = null;
        if (mThumbnailFaceBitmap != null) {
            bitmap = mThumbnailFaceBitmap;
            Log.d(TAG, "Received face bitmap for Thumbnail");
        } else if (mThumbnailStaticBitmap != null) {
            bitmap = mThumbnailStaticBitmap;
            Log.d(TAG, "Received static bitmap for Thumbnail");
        } else if (mThumbnailNonStaticBitmap != null) {
            bitmap = mThumbnailNonStaticBitmap;
            Log.d(TAG, "Received NonStatic bitmap for Thumbnail");
        } else {
            Log.e(TAG, "No proper image set for Thumbnail");
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String name = date + "_" + today.hour + today.minute + today.second + ".jpg";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        this.file = new File(dir, name);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Log.d(TAG, "Before processing thumbnail bitmap :: width : " + w + " height : " + h);
        Matrix matrix = new Matrix();
        Bitmap outputBitmap = bitmap;

        if (w < h) {
            matrix.setScale(((float) h) / w, ((float) h) / w);
//            Log.d(TAG, "Cropping landscape thumbnail from portrait image");
            outputBitmap = Bitmap.createBitmap(bitmap, 0, (int) ((h - (((float) w) * w) / h) / 2), w, (int) ((((float) w) * w) / h), matrix, true);
        } else {
            Log.d(TAG, "thumbnail image is by default landscape");
        }

        ByteArrayOutputStream output_stream2 = new ByteArrayOutputStream();
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output_stream2);
        byte[] byt = output_stream2.toByteArray();
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            outStream.write(byt);
            outStream.close();
            AppLibrary.log_d("Track", "Successfully wrote file");
            uploadThumbnail(mThumbnailCaptureImageName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mThumbnailNonStaticBitmap = null;
            mThumbnailStaticBitmap = null;
            mThumbnailFaceBitmap = null;
        }
    }

    // The Aux Buffers are not being used for Camera Preview
// They are used for rotation of the preview frame... Because unlike SurfaceTexture preview,
// camera preview to byte buffers is NOT aligned with sensors
    private byte[][] mAuxBuffers;
    private boolean mVisionFeaturesEnabled;
    private int mCameraRotation;

    private void setupFaceDetection() {
        Log.d(TAG, "setupFaceDetection :: Camera preview width : " + mCameraPreviewWidth + " height : " + mCameraPreviewHeight);

        Context context = mContext.getApplicationContext();

        if (context == null)
            return;
/*        mFaceDetector = new FaceDetector.Builder(mContext)
                .setProminentFaceOnly(true)
                .build();*/

        mFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        mFaceDetector.setProcessor(new LargestFaceFocusingProcessor(mFaceDetector, new FaceTracker()));

        if (!mFaceDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.e(TAG, "Face detector dependencies are not yet available.");
        }
        if (mCameraCapturer != null) {
            mCameraCapturer.setCaptureFrequency(6);
        }
    }

    private void stopFaceDetection() {
        if (mFaceDetector != null) {
            mFaceDetector.release();
            mFaceDetector = null;
        }
    }

    private class FaceTracker extends Tracker<Face> {

        @Override
        public void onDone() {
            Log.d(TAG, "CameraSource FaceTracker :: onDone");
            if (mPotentialFaceBitmap != null) {
                mCapturedBitmapConsumerQueue.offer(mPotentialFaceBitmap);
                mPotentialFaceBitmap = null;
            }
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
//            Log.d(TAG, "CameraSource FaceTracker :: onMissing:  operational : " + detections.detectorIsOperational());
            SparseArray<Face> faces = detections.getDetectedItems();

            int numOfFaces = faces.size();
            for (int i = 0; i < numOfFaces; i++) {
                Face face = faces.get(i);
                Log.d(TAG, "CameraSource FaceTracker :: Face :: " + "id :: " + face.getId() + " position: " + face.getPosition().x + " " + face.getPosition().y);
            }
            if (mPotentialFaceBitmap != null) {
                mCapturedBitmapConsumerQueue.offer(mPotentialFaceBitmap);
                mPotentialFaceBitmap = null;
            }
        }

        @Override
        public void onNewItem(int id, Face face) {
            Log.d(TAG, "CameraSource FaceTracker :: onNewItem");
            Log.d(TAG, "CameraSource FaceTracker :: Face :: " + "id :: " + face.getId() + " position: " + face.getPosition().x + " " + face.getPosition().y);
            if (mPotentialFaceBitmap != null) {
                mCapturedBitmapConsumerQueue.offer(mPotentialFaceBitmap);
                mPotentialFaceBitmap = null;
            }
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            Log.d(TAG, "CameraSource FaceTracker :: onUpdate" + "Height : " + face.getHeight() + "Width : " + face.getWidth());
            Log.d(TAG, "CameraSource FaceTracker :: onUpdate" + "y : " + face.getEulerY() + "z : " + face.getEulerZ());
            if (((mCaptureMode & CAPTURE_MODE_THUMBNAIL) > 0) && (mThumbnailFaceBitmap == null) && (mPotentialFaceBitmap != null)) {
                mThumbnailFaceBitmap = mPotentialFaceBitmap.copy(Bitmap.Config.ARGB_8888, false);
                mCapturedBitmapConsumerQueue.offer(mPotentialFaceBitmap);
                mPotentialFaceBitmap = null;
                if (mCaptureResultHandler != null) {
                    mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_THUMBNAIL));
                }
            }
//            Log.d(TAG, "CameraSource FaceTracker :: onUpdate", face.getIsLeftEyeOpenProbability())
        }
    }

    private int mFrameCount;

    private class TimeSync {
        private long MAX_DESYNC;
        private long MAX_SHIFT;
        private long TIME_STEP;
        private long audioTime;
        private boolean hasAudio;
        private long lastVideoTimestamp;
        private long minShift;
        private long nextTime;
        private long videoShift;
        private long videoTime;

        private TimeSync() {
            this.TIME_STEP = 5000;
            this.MAX_DESYNC = 300;
            this.MAX_SHIFT = 2000;
        }

        private void reset() {
            long dt = (this.audioTime - this.videoTime) - this.videoShift;
            if (this.hasAudio && Math.abs(dt) < Math.abs(this.minShift)) {
                this.minShift = dt;
            }
            long t = System.currentTimeMillis();
            if (t >= this.nextTime) {
                this.nextTime = this.TIME_STEP + t;
                if (this.hasAudio) {
                    AppLibrary.log_d(TAG, "Min shift=" + this.minShift);
                    if (Math.abs(this.minShift) > this.MAX_DESYNC) {
                        this.videoShift = this.minShift;
                        AppLibrary.log_d(TAG, "Video shift=" + this.videoShift);
                    }
                    this.minShift = this.MAX_SHIFT;
                }
            }
        }

        public void init() {
            this.nextTime = System.currentTimeMillis() + this.TIME_STEP;
            this.audioTime = 0;
            this.videoTime = 0;
            this.minShift = 0;
            this.videoShift = 0;
            this.lastVideoTimestamp = 0;
            this.hasAudio = false;
        }

        public long processAudio(long ts) {
            this.audioTime = ts;
            reset();
            this.hasAudio = true;
            return ts;
        }

        public long processVideo(long ts) {
            this.videoTime = ts;
            reset();
            ts += this.videoShift;
            if (ts <= this.lastVideoTimestamp) {
                ts = this.lastVideoTimestamp + 1;
            }
            this.lastVideoTimestamp = ts;
            return ts;
        }
    }

    private class CameraFocusCallback implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                Log.d(TAG, "camera auto focus success");
                if (mCaptureMode != CAPTURE_MODE_NONE) {
                    mRenderer.setCaptureEnabled(true);
                }
            } else {
                try {
                    Log.d(TAG, "camera auto focus not done yet, trying to focus again!");
                    if (camera != null) {
                        Camera.Parameters params = camera.getParameters();
                        if (params != null) {
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                            camera.setParameters(params);
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (!success) {
                                        Log.d(TAG, "camera auto focus not done yet, giving up!");
                                    }
                                    if (mCaptureMode != CAPTURE_MODE_NONE) {
                                        mRenderer.setCaptureEnabled(true);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e6) {
                    AppLibrary.log_e(TAG, "Failed to focus again");
                }
            }
        }
    }

    int[] focusIntensity = new int[]{500};

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
            int x = (int) e.getX();
            int y = (int) e.getY();
            Log.d(TAG, "auto focus TAP received at " + x + " " + y);
            if (camera != null) {
                Rect focusRect = mRenderer.getTransformedFocusRect(x, y, focusIntensity);
                Camera.Parameters params = camera.getParameters();
                if (params != null & focusRect != null & params.getMaxNumFocusAreas() > 0) {
                    ArrayList<Camera.Area> focusAreas = new ArrayList<>();
                    focusAreas.add(new Camera.Area(focusRect, focusIntensity[0]));
                    Log.d(TAG, "Trying to auto focus");
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    params.setFocusAreas(focusAreas);
                    try {
                        camera.setParameters(params);
                        VEL_HIGH_THRESHOLD = DEFAULT_VEL_SUPERHIGH_THRESHOLD;
                    } catch (Exception exception) {
                        Log.e(TAG, "Not able to set focus for the camera");
                    }
                    if (mCameraFocusCallback != null) {
                        try {
                            camera.autoFocus(mCameraFocusCallback);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
            mSensorHandler.removeCallbacksAndMessages(null);
//            mCameraHandler.postDelayed(mCameraFocusRunnable, FOCUS_INTERVAL);
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private class ScaleDetectorListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        float scaleFocusX = 0;
        float scaleFocusY = 0;

        @Override
        public boolean onScale(ScaleGestureDetector arg0) {
            float scale = arg0.getScaleFactor() * mZoomLevel;

            mZoomLevel = scale;
            mZoomLevel = Math.max(1.0f, Math.min(mZoomLevel, 3.0f));
            setScale(scale);
            return true;
        }
    }

    public Activity mContext;

    public RecordingActionControllerKitkat(Activity context, TextView tv, GLSurfaceView sv) {
        this.mContext = context;
        this.publishing = false;
        this.textMessage = tv;
        this.mGLSurfaceView = sv;

        this.mSound = new MediaActionSound();

        preferences = context.getSharedPreferences(AppLibrary.BroadCastSettings, Context.MODE_PRIVATE);
        editor = preferences.edit();
        BroadCastSettings.initialize(context);
        mSettings = BroadCastSettings.HD_QUALITY_SHARING;

        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);
        sVideoEncoder.setCallback(this);

        setupOutputDirectoryForRecordedFile();

        String encoderSize = mSettings.getDefaultEncoderSize();
        String[] sizes = encoderSize.split("x");
        mEncoderWidth = Integer.parseInt(sizes[0]);
        mEncoderHeight = Integer.parseInt(sizes[1]);

        mCameraProperties = new SparseArray<>(3);

//        AppLibrary.log_i("Camera", "RecordingActionControllerKitkat pre-starting Camera");

        sVideoEncoder.setCallback(this);

        mGLSurfaceView.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new SurfaceRenderer(mContext, sVideoEncoder,
                mSettings.videoBitRate * 1024,
                mSettings.getVideoFrameRate());
        mRenderer.setTargetView(mGLSurfaceView);
//        mRenderer.setOverlayView(mImageView);
        mRenderer.setController(this);
        mRenderer.setContext(mContext);
        mRenderer.setPlayerCallbackListener(this);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        canPlay = true;
        mRenderTargetType = MoviePlayer.RENDER_TARGET_DISPLAY;
        mRenderer.setRenderTargetType(mRenderTargetType);

        if (OFFSCREEN_ENABLED) {
            mCameraCapturer = new CameraCapture(this,context);
            mCameraCapturer.setSwappable((Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1));
        }
        mDummyRenderer = new DummySurfaceRenderer();
        mCaptureResultHandlerThread = new CustomHandlerThread(this, "CaptureResultHandlerThread");
        mCaptureResultHandler = mCaptureResultHandlerThread.getHandler();

        if (OFFSCREEN_ENABLED)
            mRenderer.setCameraCapturer(mCameraCapturer);

        mRenderer.setDummyRenderer(mDummyRenderer);

        // call prepareForVideoRendering after setting everything related to renderer, dummyrenderer etc.
//        mRenderer.prepareForVideoRendering();

        mCurrentCaptureFilter = 0;

        mSensorThread = new CameraHandlerThread();
        mSensorThread.setPriority(Thread.MIN_PRIORITY);
        mSensorHandler = mSensorThread.getHandler();

        mGestureListener = new GestureDetector(mContext, new CustomGestureListener());
        mScaleListener = new ScaleGestureDetector(mContext, new ScaleDetectorListener());
        mGLSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleOnTouch(event);
            }
        });
        if (mSmartFocusMode) {
            initiateSensors();
        }

//        this.layout.setTag(BroadCastSettings.aspectRatio);
        if (OFFSCREEN_ENABLED) {
            mVisionFeaturesEnabled = true;
        } else {
            mVisionFeaturesEnabled = false;
        }
    }

    // To render/encode the final edited video
    public void requestStartVideoRendering() {
        mRenderer.setContext(mContext);
        requestPlayback = true;
        if (mMoviePlayer == null) {
            // If mMoviePlayer is null at this point, it hasn't been in use or hasn't been created.
            // So create a new instance and prepare it.
            mRenderer.pause();
//            mRenderer.releaseRenderer();
        } else {
            stopAndCleanupPlayer(true);
            if(mRenderTargetType > 0 && requestPlayback) {
                playerReady();
            }
        }
    }

    public void goToShareAction(boolean goToShareAction){
        this.goToShareAction=goToShareAction;
    }

    public void setPreviewSurfaceView(GLSurfaceView sv) {
        mGLSurfaceView = sv;
        mRenderer.setTargetView(mGLSurfaceView);
    }

    public void stopAndCleanupPlayer() {
        stopAndCleanupPlayer(false);
    }

    public void stopAndCleanupPlayer(boolean waitForPlayer) {
        if (mPlayTask==null) { //In case of any exception
            mRenderer.pause();
            stopPublishingStream();
            return;
        }

        synchronized (mPlayTask) {
            if(waitForPlayer) {
                mPlayTask.requestStop();
                mPlayTask.waitForStop();
            }
            mRenderer.pause();
            if(mMoviePlayer != null) {
                mMoviePlayer.releaseDecoder();
            }
        }
    }

    public void startVideoRendering() {

        mRenderer.setRenderTargetType(mRenderTargetType);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.prepareRenderer();
                mRenderer.addDrawable(0.0f, 0.0f, 1.0f, 1.0f, mBitmapOverlay, true);
                if(mRenderTargetType==MoviePlayer.RENDER_TARGET_VIDEO){
                    if(!goToShareAction){
                        mRenderer.addDrawable(mEncoderWidth,mEncoderHeight, R.drawable.stamp);
                    }
                }
                mRenderer.prepareForVideoRendering();
            }
        });
    }

    public void toggleAudio(boolean mute) {
        mMoviePlayer.toggleAudio(mute);
    }

    public boolean handleOnTouch(MotionEvent event) {
//        if (mMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
//            mRenderer.requestRender(false);
//        }
        if (mSmartFocusMode) {
            mGestureListener.onTouchEvent(event);
        }
        mScaleListener.onTouchEvent(event);

        return false;
    }

    public void addOverlay(float x, float y, int resId) {
        mRenderer.addDrawable(x, y, resId);
    }

    public void addOverlays(ArrayList<ThemeModel> themeModels) {
        mRenderer.addOverlays(themeModels);
    }

    /*    public void addOverlays(ArrayList<StickerModel> stickerModels) {
        mRenderer.addOverlays(stickerModels);
    }*/

/*    public void addOverlay(StickerModel stickerModel) {
        mRenderer.addDrawable(stickerModel.mPositionX, stickerModel.mPositionY, stickerModel.mWidth, stickerModel.mHeight, stickerModel.mResId);
//        mRenderer.addDrawable(x, y, resId);
    }*/

    private void findSupportedFocusMode(Camera.Parameters params) {
        if (params != null) {
            boolean continuousVideoFocusSupported = false;
            boolean autoFocusModeSupported = false;
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes != null) {
                for (String focus : focusModes) {
                    if (focus.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        smoothFocusMode = focus;
                        continuousVideoFocusSupported = true;
                        break;
                    }
                    if (focus.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        autoFocusModeSupported = true;
                    }
                }
                if (!continuousVideoFocusSupported && autoFocusModeSupported) {
                    smoothFocusMode = Camera.Parameters.FOCUS_MODE_AUTO;
                }
            }
        }
    }

    private void queryCameraProperties(int cameraId) {
        CameraProperties cameraProperties = mCameraProperties.get(cameraId);

        mCameraPreviewSize = cameraProperties.mPreviewSize;
        mCameraPreviewFrameRate = cameraProperties.mPreviewFrameRate;
        mCameraPreviewAspectRatio = cameraProperties.mPreviewAspectRatio;
        mCameraPreviewWidth = cameraProperties.mPreviewWidth;
        mCameraPreviewHeight = cameraProperties.mPreviewHeight;
        Log.d(TAG, "Setting camera props to " + mCameraPreviewSize + " fps: " + mCameraPreviewFrameRate + " aspect: " + mCameraPreviewAspectRatio);
    }

    private CameraProperties getCameraPropertiesFromSharedPreferences(int cameraId, boolean getDefaultValue) {
        Log.d(TAG, "Getting camera " + cameraId + " properties from shared preferences");

        if (!getDefaultValue & !preferences.contains(SharedPreferencesIdentity + SPI_PreviewSize + cameraId))
            return null;

        CameraProperties cameraProperties = new CameraProperties();

        cameraProperties.mPreviewSize = preferences.getString(SharedPreferencesIdentity + SPI_PreviewSize + cameraId, "640x480");
        cameraProperties.mPreviewFrameRate = preferences.getInt(SharedPreferencesIdentity + SPI_PreviewFrameRate + cameraId, new Integer(20));
        cameraProperties.mPreviewAspectRatio = Double.parseDouble(preferences.getString(SharedPreferencesIdentity + SPI_PreviewAspectRatio + cameraId, "1.3333"));
        cameraProperties.mPreviewWidth = preferences.getInt(SharedPreferencesIdentity + SPI_PreviewWidth + cameraId, new Integer(640));
        cameraProperties.mPreviewHeight = preferences.getInt(SharedPreferencesIdentity + SPI_PreviewHeight + cameraId, new Integer(480));

        smoothFocusMode = preferences.getString(SharedPreferencesIdentity + SPI_Focus, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        cameraProperties.stored = true;

        return cameraProperties;
    }

    private void storeCameraPropertiesToSharedPreferences(int cameraId) {
        Log.d(TAG, "Storing properties for camera " + cameraId);
        CameraProperties cameraProperties = mCameraProperties.get(cameraId);

        editor.putString(SharedPreferencesIdentity + SPI_PreviewSize + cameraId, cameraProperties.mPreviewSize);
        editor.putInt(SharedPreferencesIdentity + SPI_PreviewWidth + cameraId, cameraProperties.mPreviewWidth);
        editor.putInt(SharedPreferencesIdentity + SPI_PreviewHeight + cameraId, cameraProperties.mPreviewHeight);
        editor.putInt(SharedPreferencesIdentity + SPI_PreviewFrameRate + cameraId, cameraProperties.mPreviewFrameRate);
        editor.putString(SharedPreferencesIdentity + SPI_PreviewAspectRatio + cameraId, Double.toString(cameraProperties.mPreviewAspectRatio));

        editor.putString(SharedPreferencesIdentity + SPI_Focus, smoothFocusMode);
        editor.commit();
    }

    private boolean initCameraProperties(Camera camera, int cameraId) {
        Log.d(TAG, "Configuring camera " + cameraId + " for first time");
        boolean status = mSettings.resetPreviewSizes(camera.getParameters());

        if (!status)
            return false; //Something went wrong while initializing properties. Returning silently in the hope for something better than an apocalypse
        CameraProperties cameraProperties = new CameraProperties();

        cameraProperties.mPreviewSize = mSettings.getVideoSize();
        cameraProperties.mPreviewWidth = mSettings.getVideoWidth();
        cameraProperties.mPreviewHeight = mSettings.getVideoHeight();
        cameraProperties.mPreviewFrameRate = mSettings.getVideoFrameRate();
        cameraProperties.mPreviewAspectRatio = mSettings.getAspectRatio();

        cameraProperties.stored = true;

        mCameraProperties.put(cameraId, cameraProperties);

        Camera.Parameters params = camera.getParameters();
        findSupportedFocusMode(params);
        return true;
    }

    public void setFilter(final int filter, final boolean enable) {
        mRenderer.setFilter(/*1 << */filter, enable);
        setBackgroundVideoEnabled(mVideoBackgroundEnabled);
    }

    public void setCaptureFilter(final int filter, final boolean enable) {
        mRenderer.setCaptureFilter(/*1 << */filter);
    }

    public void triggerFilterThumbnail(final int begin, final int end) {
        //Delay filter capture in case currently we're trying to capture thumbnail for atleast 1.5 sec
        // hoping that it's complete by then
        mCurrentCaptureFilter = 0;
        if (mCaptureResultHandler != null) {
            if (mCaptureMode == CAPTURE_MODE_THUMBNAIL) {
                mCaptureResultHandler.post(mFilterTriggerRunnable);
            } else {
                mCaptureResultHandler.post(mFilterCaptureRunnable);
            }
        }
    }

    public void stopCapture() {
        if (mCameraCapturer != null) {
            mCameraCapturer.setCaptureFrequency(10);
            mRenderer.setCaptureEnabled(false);
            mCameraCapturer.setCaptureEnabled(false);
            mCameraCapturer.setFilter(0);
        }
    }

    public void seekTo(float progress) {
        if (mMoviePlayer != null) {
            mMoviePlayer.seekTo((long) (mMoviePlayer.getDuration() * progress));
        }
    }

    public void pickOverlay(float x, float y) {
        mRenderer.pickDrawable(x, y);
    }

    public void moveOverlay(float x, float y) {
        mRenderer.moveDrawable(x, y);
    }

    public void stopFilterThumbnail() {
        mCaptureMode &= ~CAPTURE_MODE_FILTER;
        if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
    }

    public void stopBackgroundCapture() {
        mRenderer.setVideoBackground((Bitmap) null);
        mCaptureMode &= ~CAPTURE_MODE_BACKGROUND;
        if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
    }

    public void setScale(float scale) {
        mRenderer.setScale(scale);
    }

    // spinner selected
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        final int filterNum = spinner.getSelectedItemPosition();

        AppLibrary.log_d(TAG, "onItemSelected: " + filterNum);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeFilterMode(filterNum);
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void setState(String state, String message, boolean enabled, boolean screen) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        if (!state.isEmpty()) {
            bundle.putString("ButtonText", state);
        }
        if (!message.isEmpty()) {
            bundle.putString("Message", message);
        }
        bundle.putBoolean("ButtonEnabled", enabled);
        bundle.putBoolean("Screen", screen);
        msg.setData(bundle);
        this.handler.sendMessage(msg);
    }

    private void start() {
        this.isError = false;
        this.tempPauseVideoTS = 0;
        this.pausedAudioDuration = 0;
        this.rtmpErrorCounter = 0;
        this.publishing = true;
        try {
//            String videoFps = Integer.toString(this.mCameraPreviewFrameRate);
//            this.videoBitrate = mSettings.videoBitRate;
            String url = this.rtmpUrl;
            String audioChannels = "1";   // #channels : 1 - mono, 2 - stereo
            String audioSamplingRate = Integer.toString(mSettings.audioSamplingRate);
            String audioBitRate = Integer.toString(mSettings.audioBitRate * 1024);

            //Read smallest Type1 NAL from preferences
//            readSmallestFoundType1NALFromPreferences();

/*            if (url != null && url.startsWith("rtmp://")) {
                url = url.substring(7);
            }
            int colon = url.indexOf(58);
            int slash1 = url.indexOf(47);
            if (slash1 == -1) {
                AppLibrary.log_e(TAG, "Bad Url");
                throw new Exception("Bad URL");
            } else {
                int slash2 = url.indexOf(47, slash1 + 1);
                if (slash2 == -1) {
                    AppLibrary.log_e(TAG, "Bad Url");
                    throw new Exception("Bad URL");
                } else {
                    String host;
                    int port;
                    if (colon == -1 || colon >= slash1) {
                        host = url.substring(0, slash1);
                        port = 1935;
                    } else {
                        host = url.substring(0, colon);
                        port = Integer.parseInt(url.substring(colon + 1, slash1));
                    }
                    String app = url.substring(slash1 + 1, slash2);
                    String playpath = url.substring(slash2 + 1);
                    AppLibrary.log_d(TAG, "host='" + host + "'; port=" + port + "; app='" + app + "'; playpath='" + playpath + "'");
                    this.audioBytes = 0;
                    this.videoBytes = 0;
                    this.lastByteUpdateTime = System.currentTimeMillis();
                    if (LIVE_STREAM) {
                        try {
                            this.rtmp = new RtmpMuxer(host, port, app, playpath);
                            this.rtmp.setCallback(this);
                            this.rtmp.prepare();
                            this.rtmp.start();
                        } catch (Exception e) {   //Handle all the RTMP errors without affecting the video/ audio part
                            e.printStackTrace();
                            if (rtmp != null)
                                rtmp.isRTMPError();  //To handle exceptions in rtmp.prepare()
                            else
                                onRtmpError("Unknown host");
                        }
                    } else
                        this.rtmp = null;*/

            if (RECORD_STREAM) {
                if (mLastRecordedFilepath == null) {
                    Date date = new Date();
                    DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd-HH-mm-ss");
                    String fileName = dateFormat.format(date) + "_"
                            + ".mp4";

                    fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                    mLastRecordedFilepath = this.mOutputDir + "/" + fileName;
                }
                this.recordingStarted = false;
                this.sendFramesToRecorder = false;
                Log.d(TAG, "Creating Recording Muxer: " + " Video width: " + mMoviePlayer.getVideoWidth() + " height: " + mMoviePlayer.getVideoHeight());
                this.mRecordingMuxer = new RecordingMuxer(mLastRecordedFilepath,
                        mMoviePlayer.getVideoWidth(),
                        mMoviePlayer.getVideoHeight(),
                        this.mSettings.videoBitRate * 1024,
                        this.mSettings.getVideoFrameRate(),
                        this.mSettings.audioSamplingRate,
                        1,
                        this.mSettings.audioBitRate * 1024,
                        this.mContext);
                this.mRecordingMuxer.prepare();
                this.mRecordingMuxer.setCallback(this);
            }
//                    if (this.camera != null) {
//                        int bitRate = Integer.parseInt(videoBitRate);
//                        this.mVideoCodec = new IVideoCodec(this.camera, this.previewSize.width, this.previewSize.height, Integer.parseInt(videoFps), bitRate);
//                        this.mVideoCodec.setCallback(this);
//                        this.mVideoCodec.setSurface(this.holder.getSurface());
//                        this.mVideoCodec.prepare();
//                        this.mVideoCodec.start();
//                    }
            //  AudioCodec
//            boolean isWiredHeadsetOn = false;
//            try {
//                AudioManager am1 = (AudioManager) ((Activity) mContext).getSystemService(Context.AUDIO_SERVICE);
//                isWiredHeadsetOn = am1.isWiredHeadsetOn();
//                AppLibrary.log_i(TAG, "Wired Headset status is: " + isWiredHeadsetOn);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
/*
                    this.audioCodec = new IAudioCodec(Integer.parseInt(audioChannels), Integer.parseInt(audioSamplingRate), Integer.parseInt(audioBitRate), isWiredHeadsetOn);
                    this.audioCodec.setCallback(this);
                    this.audioCodec.prepare();
                    this.audioCodec.start();*/
/*                    this.mGLSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {*/
            // notify the renderer that we want to change the encoder's state
            mDummyRenderer.changeRecordingState(true);
/*                        }
                    });*/
            setState("Stop", "live", true, true);
/*                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            onAudioError(e.toString()); //OnAudioError and onVideoError are the same - Goes back to Dashboard safely
        }
    }

    /* filter and camera */
    public void startCamera() {
        mZoomLevel = 1.0f;
        mSmartFocusMode = true;

        if (this.camera == null) {

//            AppLibrary.log_i(TAG, "effective sizes: " + this.layout.getMeasuredHeight());

            newOpenCamera();
            if (this.camera == null) {  //Error handled in ResumeView for first time camera opening, and restart controller for swap camera issues
                Toast.makeText(mContext, "Sorry, unable to open camera. Please try again!", Toast.LENGTH_SHORT).show();
                return;
            } else {
                int cameraId = mSettings.getCameraId();
                CameraProperties cameraProperties = mCameraProperties.get(cameraId);
                if (cameraProperties == null || !cameraProperties.stored) {
                    boolean status = initCameraProperties(camera, cameraId);
                    if (status) storeCameraPropertiesToSharedPreferences(cameraId);
                    else {  //If there was any issue with initializing the camera properties of the camera, setting default values
                        CameraProperties defaultCameraProperties = getCameraPropertiesFromSharedPreferences(cameraId, true);
                        if (defaultCameraProperties != null) {
                            Log.d(TAG, "Properties for camera " + cameraId + " being set as default");
                            mCameraProperties.put(cameraId, defaultCameraProperties);
                        }
                    }
                }
                queryCameraProperties(cameraId);
            }

            setCameraConfiguration();

            if (mSmartFocusMode && ((mSettings.getCameraId() % 2) == 0)) {
                startListeningSensors();
            }

            String focusmode;

            if (!((mSettings.getCameraId() % 2) == 0)) {
                focusmode = Camera.Parameters.FOCUS_MODE_FIXED;
            } else {
                focusmode = smoothFocusMode;
            }

            Camera.Parameters params = camera.getParameters();
            if (params != null) {
                try {
                    params.setFocusMode(focusmode);
//                    if(params.isAutoWhiteBalanceLockSupported())
//                        params.setAutoWhiteBalanceLock(true);
                    if (!(params.getMinExposureCompensation() == 0 && params.getMaxExposureCompensation() == 0))
                        params.setExposureCompensation(0);
                    if (params.getAntibanding() != null)
                        params.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
                    params.setRecordingHint(true);
                    params.set("video-size", previewSize.width + "x" + previewSize.height);
                    params.set("preview-size", previewSize.width + "x" + previewSize.height);

                    Log.d(TAG, "Final camera settings - width: " + previewSize.width + " height: " + previewSize.height);
                    camera.setParameters(params);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            // Set the preview aspect ratio.
            mCameraPreviewWidth = previewSize.width;
            mCameraPreviewHeight = previewSize.height;
            Log.d(TAG, "setCameraPreviewSize");
            mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);

            if (this.mSurfaceTexture != null) {
                try {
                    this.camera.setPreviewTexture(this.mSurfaceTexture);
//                    this.mSurfaceTexture = null;
                    this.camera.startPreview();
                } catch (Exception ioe) {
                    this.mSurfaceTexture = null;
                    onAudioError(ioe.toString());
                    AppLibrary.log_e(TAG, "Camera start preview failed at start Camera()");
                    return;
                }
            } else {
                AppLibrary.log_e(TAG, "Needed to set camera's preview texture at startCamera() but surface texture is null!");
                return;
            }
        }
    }


    private void oldOpenCamera() {
        int cameraId;
        if (mSettings != null)
            cameraId = mSettings.getCameraId();
        else {
            BroadCastSettings.initialize(mContext);
            mSettings = BroadCastSettings.MEDIUM_QUALITY;
            cameraId = mSettings.getCameraId();
        }
        String str2 = "Start camera #%d";
        Object[] objArr = new Object[1];
        objArr[0] = Integer.valueOf(cameraId);
        AppLibrary.log_d(TAG, String.format(str2, objArr));

        try {
            this.camera = Camera.open(cameraId);
            mCameraFocusCallback = new CameraFocusCallback();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    private void newOpenCamera() {
        if (mHandlerThread == null) {
            mHandlerThread = new CameraHandlerThread();
        }

        synchronized (mHandlerThread) {
            mHandlerThread.openCamera();
        }
    }


    public class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;
        public boolean mIsRunning = false;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    oldOpenCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                AppLibrary.log_i(TAG, "wait was interrupted");
            }
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private boolean hasFlash() {
        if (this.camera == null) {
            return false;
        }

        Camera.Parameters parameters = camera.getParameters();

        if (parameters.getFlashMode() == null) {
            return false;
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes == null || supportedFlashModes.isEmpty() || supportedFlashModes.size() == 1 && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
            return false;
        }

        return true;
    }

    private void setCameraConfiguration() {
//        if (VERBOSE)
//            AppLibrary.log_d(TAG, "Setting camera orientation...PreviewLayout " + layout.getWidth() + "x" + layout.getHeight() + " " + mGLSurfaceView.getWidth() + "x" + mGLSurfaceView.getHeight());
        String str2;
        Object[] objArr;
        if (this.camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Display display = this.mContext.getWindowManager().getDefaultDisplay();
            orientationResult = display.getRotation();

            Camera.getCameraInfo(0, info);
            int degrees = 0;
            switch (display.getRotation()) {
                case 0:
                    degrees = 0;
                    break;
                case 1:
                    degrees = 90;
                    break;
                case 2:
                    degrees = 180;
                    break;
                case 3:
                    degrees = 270;
                    break;
            }
            if (info.facing == 1) {
                result = (360 - ((info.orientation + degrees) % 360)) % 360;
            } else {
                result = ((info.orientation - degrees) + 360) % 360;
            }
            mCameraRotation = result;
            Camera.Parameters params;
            try {
                params = this.camera.getParameters();
                this.camera.setDisplayOrientation(result);
            } catch (Exception exception) {
                Log.w(TAG, "Trying to get camera params on a released camera");
                return;
            }
            AppLibrary.log_d(TAG, "Result " + result);

            this.previewSize = params.getPreviewSize();

            str2 = "Default size %dx%d";
            objArr = new Object[2];
            objArr[0] = Integer.valueOf(this.previewSize.width);
            objArr[1] = Integer.valueOf(this.previewSize.height);
//                AppLibrary.log_d(TAG, String.format(str2, objArr));
            try {
                String[] size = this.mCameraPreviewSize.split("x");
                str2 = "Setting preview size %sx%s";
                objArr = new Object[2];
                objArr[0] = size[0];
                objArr[1] = size[1];
                AppLibrary.log_d(TAG, String.format(str2, objArr));
                int width = Integer.parseInt(size[0]);
                int height = Integer.parseInt(size[1]);

                AppLibrary.log_d(TAG, "sizes: " + width + " : " + height);
                if (!(width == 0 || height == 0)) {
                    params.setPreviewSize(width, height);
                    this.previewSize.width = width;
                    this.previewSize.height = height;
                    this.camera.setParameters(params);
                }
            } catch (Exception e4) {
                e4.printStackTrace();
                AppLibrary.log_e(TAG, "Could not set size from preferences");
            }
                /*this.layout.setAspectRatio(((double) this.previewSize.width) / ((double) this.previewSize.height));
                tagObj.screenOrientation = orientationResult;*/

            try {
                params = this.camera.getParameters();
                this.mFilter = FilterManager.getInstance(params);
//                this.camera.setParameters(params);
/*                if (smoothFocusMode == Camera.Parameters.FOCUS_MODE_AUTO) {
                    camera.autoFocus(mCameraFocusCallback);
                }*/
            } catch (Exception e5) {
                AppLibrary.log_e(TAG, "Failed to set filters fragment");
            }

            try {
                params = this.camera.getParameters();
//              NewBroadCastPage.flashmode = hasFlash();

                CameraActivity.flashMode = false; //Temporarily until flash filter crashes are handled
                if (CameraActivity.flashMode) {
                    params.setFlashMode("off");
                    this.camera.setParameters(params);
                }
            } catch (Exception e6) {
                AppLibrary.log_e(TAG, "Failed to set flash mode");
            }

            AppLibrary.log_e(TAG, "Colorfilter: " + CameraActivity.flashMode);

//                tagObj.screenOrientation = orientationResult;
//                this.layout.setAspectRatio(((double) this.previewSize.width) / ((double) this.previewSize.height));

            AppLibrary.log_d("Result in CameraConfig:", String.valueOf(result));

//            try {
//                params = this.camera.getParameters();
//                params.set("recording-hint", "true");
//                String vstabSupported = params.get("video-stabilization-supported");
//                if ("true".equals(vstabSupported))
//                    params.set("video-stabilization", "true");
//                this.camera.setParameters(params);
//            } catch (Exception e6) {
//                AppLibrary.log_e(TAG, "Failed to set recording hint or video stabilization");
//            }

//            if(result == 90)
//            {
//                this.layout.setX(-(PreviewLayout.WidthDiff/2));
//                this.layout.setY(0);
//            }
//
//            if(result == 270)
//            {
//                this.layout.setX(-(PreviewLayout.WidthDiff/2));
//                this.layout.setY(0);
//            }
//
//            if(result == 0)
//            {
//                this.layout.setX(0);
//                this.layout.setY(-(PreviewLayout.HeightDiff/2));
//            }
//            if(result == 180)
//            {
//                this.layout.setX(0);
//                this.layout.setY(-(PreviewLayout.HeightDiff / 2));
//            }

            //  now do a local broadcast about new camera preview started
            MasterClass.getEventBus().post(new BroadCastSignals.CameraPreviewStartedSignal());
//            } catch (IOException e6) {
//                AppLibrary.log_e(TAG, "Error setting camera preview: " + e6.getMessage());
//            }
        }
    }

    public static void rotateNV21(byte[] input, byte[] output, int width, int height, int rotation) {
        boolean swap = (rotation == 90 || rotation == 270);
        boolean yflip = (rotation == 90 || rotation == 180);
        boolean xflip = (rotation == 270 || rotation == 180);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yflip) {
                    yi = h - yi - 1;
                }
                if (xflip) {
                    xi = w - xi - 1;
                }
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                h = (h >> 1);
                // adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                // and here
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }
    }

    public void updateCameraOrientation() {
        mDummyRenderer.pauseUpdates(true);
        if (VERBOSE)
            AppLibrary.log_d(TAG, "Updating camera orientation...");
//        String str2;
//        Object[] objArr;
        if (this.camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Display display = this.mContext.getWindowManager().getDefaultDisplay();
            orientationResult = display.getRotation();

            Camera.getCameraInfo(0, info);
            int degrees = 0;
            switch (display.getRotation()) {
                case 0:
                    degrees = 0;
                    break;
                case 1:
                    degrees = 90;
                    break;
                case 2:
                    degrees = 180;
                    break;
                case 3:
                    degrees = 270;
                    break;
            }
            if (info.facing == 1) {
                result = (360 - ((info.orientation + degrees) % 360)) % 360;
            } else {
                result = ((info.orientation - degrees) + 360) % 360;
            }
/*            Camera.Parameters params = this.camera.getParameters();
            this.camera.setDisplayOrientation(result);*/
            AppLibrary.log_e(TAG, "Result " + result);
            mCameraRotation = result;

//            this.previewSize = params.getPreviewSize();
//            str2 = "Default size %dx%d";
//            objArr = new Object[2];
//            objArr[0] = Integer.valueOf(this.previewSize.width);
//            objArr[1] = Integer.valueOf(this.previewSize.height);
//            AppLibrary.log_e(TAG, String.format(str2, objArr));
//            try {
//                String [] size = this.videoSize.split("x");
//                str2 = "Setting preview size %sx%s";
//                objArr = new Object[2];
//                objArr[0] = size[0];
//                objArr[1] = size[1];
//                AppLibrary.log_d(TAG, String.format(str2, objArr));
//                int width = Integer.parseInt(size[0]);
//                int height = Integer.parseInt(size[1]);
//
//                AppLibrary.log_d(TAG, "sizes: " + width + " : " + height);
//                if (!(width == 0 || height == 0)) {
//                    params.setPreviewSize(width, height);
//                    this.previewSize.width = width;
//                    this.previewSize.height = height;
//                    this.camera.setParameters(params);
//                }
//            } catch (Exception e4) {
//                e4.printStackTrace();
//                AppLibrary.log_e(TAG, "Could not set size from preferences");
//            }

            AppLibrary.log_e("Result in CameraConfig:", String.valueOf(result));

//            if(isRendering) {
//                this.preview.queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        // Tell the renderer that it's about to be paused so it can clean up.
//                        mRenderer.notifyPausing();
//                    }
//                });
//                preview.onPause();
//                preview.onResume();
//                final int mCameraPreviewWidth = previewSize.width;
//                final int mCameraPreviewHeight = previewSize.height;
//                this.preview.queueEvent(new Runnable() {
//                    @Override
//                    public void run() {
//                        mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
//                    }
//                });
//            }
        }
    }

    private void stop(String message) {
        if (this.publishing) {
            AppLibrary.log_d(TAG, "Controller Stop:: " + message);
//            if (this.recordingStarted && this.mSound != null)
//                mSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
            if (upload != null) {
                if (upload.getState() != TransferState.COMPLETED)
                    MasterClass.getTransferUtility().cancel(upload.getId()); //Cancel if not completed
                upload = null;
            }

            if (mRtmpMuxerAsyncStartStopThread != null) {
                mRtmpMuxerAsyncStartStopThread.getHandler().removeCallbacksAndMessages(null);
                mRtmpMuxerAsyncStartStopThread.interrupt();
                mRtmpMuxerAsyncStartStopThread = null;
            }

            //Save smallest Type1 NAL in preferences
//            saveSmallestFoundType1NALInPreferences();
            this.smallestFoundType1NAL = null;

            mDummyRenderer.changeRecordingState(false);
/*            if (this.mGLSurfaceView != null) {
                this.mGLSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        // notify the renderer that we want to change the encoder's state
                        mRenderer.changeRecordingState(false);
                    }
                });
            }*/
            if (this.audioCodec != null) {
                this.audioCodec.stop();
                this.audioCodec = null;
            }
            //For recording
            if (this.isRecorderRunning()) {
                this.mRecordingMuxer.stop();
                File recFile = new File(this.mRecordingMuxer.getOutputFilePath());
                if (recFile != null && recFile.exists())
                    mLastRecordedFilepath = this.mRecordingMuxer.getOutputFilePath();
                else
                    mLastRecordedFilepath = "";
                this.mRecordingMuxer = null;
                AppLibrary.log_d(TAG, "Recorded file:: " + mLastRecordedFilepath);
            }
            this.publishing = false;
            this.isCameraPaused = false;
            this.doAudioCheck = false;
            this.tempPauseVideoTS = 0;
            this.pausedAudioDuration = 0;
            this.lastSentVideoTS = 0;
            this.lastSentAudioTS = 0;
            this.audioCorrectionCounter = 0;
            this.bitrateReductionsConsecutiveCounter = 0;
            this.continueWithoutBitrateChecks = false;
            this.lastSentType5NAL = null;
            this.AudioStartTimestampDelta = 0;
            this.rtmpErrorCounter = 0;

            this.recordingStarted = false;
            this.sendFramesToRecorder = false;

            //For music playback
            if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
                if (this.mMusicPlayer != null) {
                    this.mMusicPlayer.stop();
                    this.mMusicPlayer = null;
                }
            }

            //For RTMP
            if (this.rtmp != null) {
                if (this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning)
                    AppLibrary.log_e(TAG, "RtmpMuxer stop is already in process!");
                else {
//                    status = ((CameraActivity) mContext).mStatus;
//
//                    if (status == CameraActivity.BroadCastStatus.STATE_BROADCAST_LIVE ||
//                            status == CameraActivity.BroadCastStatus.STATE_BROADCAST_STOP)
//                        this.rtmp.stop();
//                    else
//                        this.rtmp.flushStop();

//                    this.rtmp = null;
                }
            }
            setState("Publish", message, true, false);
            resetStreamingCutoffs();   //Resets Threshold and per cycle increase/drop of bitrates
        }
    }

    private void resetStreamingCutoffs() {
        THRESHOLD_HIGHEST_VIDEO_BITRATE = 450;
        BITRATE_DROP_PER_CYCLE = 40;
        BITRATE_INCREASE_PER_CYCLE = 25;
    }


    /*
    Callback interface when RTMP streaming is stopped. To add check to send StopBroadcast request only after emptying queue
    */
    public void stopRTMPStreaming(boolean stopBroadcast) {
        if (stopBroadcast && LIVE_STREAM) {
            if (this.rtmp != null && mContext != null) {
                AppLibrary.log_d(TAG, "Stop broadcast request for LIVE without upload dialog");
//                ((CameraActivity) mContext).transitionStopBroadcast();
//                ((NewBroadCastPage) mContext).enableWatchNow();  //Signals that its safe to enable Watch Now
            }
            this.rtmp = null;
        }

        if (mContext != null && LIVE_STREAM) {
//            ((CameraActivity) mContext).removeRTMPSubscriber();  //Removes the RTMP subscriber from NGINX RTMP
        }
    }

    public boolean getRTMPStatus() {
        if (this.rtmp != null)
            return true;
        else
            return false;
    }

    public void stopCamera() {

        if (mVisionFeaturesEnabled) {
            stopFaceDetection();
        }
        if (this.camera != null) {
            if (mSensorHandler != null) {
                mSensorHandler.removeCallbacksAndMessages(null);
            }
            if (mSmartFocusMode && ((mSettings == null) || ((mSettings.getCameraId() % 2) == 0))) {
                stopListeningSensors();
            }
            mSmartFocusMode = false;

            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }

    private void updateBytes() {
        long t = System.currentTimeMillis();
        if (t >= this.lastByteUpdateTime + 1000) {
            this.lastByteUpdateTime = t;
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putBoolean("Bytes", true);
            msg.setData(bundle);
            this.handler.sendMessage(msg);
        }
    }

    private void waitThread() {
        if (this.thread != null) {
            try {
                AppLibrary.log_d(TAG, "Waiting thread");
                this.thread.join();
                AppLibrary.log_d(TAG, "Thread exited");
            } catch (InterruptedException e) {
            }
        }
    }

    public void destroyThread() {
        this.thread = null;
    }

    private void asyncStart() {
        if (this.thread != null && this.thread.isAlive()) {
            AppLibrary.log_e(TAG, "Busy");
        } else if (!this.publishing) {
            setState("Stop", "live", false, true);
            this.thread = new Thread(this);
            this.thread.start();
        } else {
            AppLibrary.log_i(TAG, "Something went wrong");
        }
    }

    private void asyncStop(String message) {
        AppLibrary.log_d(TAG, "AsyncStop. " + message);
        if (this.thread != null && this.thread.isAlive()) {
            AppLibrary.log_e(TAG, "Busy");
        } else if (this.publishing) {
            String str = "Publish";
            if (message.isEmpty()) {
                message = "live";
            }
            setState(str, message, false, false);
            this.thread = new Thread(this);
            this.thread.start();
        }
    }

    public void asyncStopRtmpPublishing() {
        AppLibrary.log_d(TAG, "AsyncStopRtmpPublishing");
        if ((this.thread != null && this.thread.isAlive()) ||
                (this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning)) {
            AppLibrary.log_e(TAG, "Thread is already performing a RTMP/non-RTMP start/stop action");
        } else {
            if (this.publishing && this.rtmp != null) {
                if (this.mRtmpMuxerAsyncStartStopThread == null)
                    this.mRtmpMuxerAsyncStartStopThread = new CameraHandlerThread();
                this.mRtmpMuxerAsyncStartStopThread.mIsRunning = true;
                this.mRtmpMuxerAsyncStartStopThread.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (rtmp != null) {
//                            status = ((CameraActivity) mContext).mStatus;
//                            if (status == CameraActivity.BroadCastStatus.STATE_BROADCAST_LIVE ||
//                                    status == CameraActivity.BroadCastStatus.STATE_BROADCAST_STOP)
//                                rtmp.stop();
//                            else
//                                rtmp.flushStop();
//                            AppLibrary.log_d(TAG, "RTMP publishing stopped for continuing with record and upload!");
//
//                            if(mRtmpMuxerAsyncStartStopThread!=null)
//                                mRtmpMuxerAsyncStartStopThread.mIsRunning = false;
                        }
                    }
                });
            }
        }
    }


    public void asyncStartRtmpPublishing() {
        AppLibrary.log_d(TAG, "AsyncStartRtmpPublishing");
        if (this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning) {
            AppLibrary.log_e(TAG, "Thread is already performing an RTMP start/stop action");
        } else if (this.publishing && this.rtmp != null) //Reuse the same rtmp object
        {
            if (this.mRtmpMuxerAsyncStartStopThread == null)
                this.mRtmpMuxerAsyncStartStopThread = new CameraHandlerThread();
//          this.mRtmpMuxerAsyncStartStopThread.mIsRunning = true; //Don't pause asyncStop because of this action
            this.mRtmpMuxerAsyncStartStopThread.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    if (publishing && rtmp != null)
//                        ((CameraActivity) mContext).removeRTMPSubscriber();  //Removes the RTMP subscriber from NGINX RTMP. Starting Streaming is done in response of this function

                    if (mRtmpMuxerAsyncStartStopThread != null)
                        mRtmpMuxerAsyncStartStopThread.mIsRunning = false;
                }
            }, 1000);  //Hoping for internet connection back in 1 second
        }
    }

    public void restartStreaming() //Called from the response of remove RTMP subscriber request
    {
        if (this.mRtmpMuxerAsyncStartStopThread != null) {  //Possibility of this thread becoming null after calling asyncStop()
            this.mRtmpMuxerAsyncStartStopThread.mIsRunning = true;
            this.mRtmpMuxerAsyncStartStopThread.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (rtmp != null && publishing) { //Just being doubly sure incase something happens in the delay time
                        try {
                            rtmp.prepare();  //Using the same rtmp object to restart socket connection and start again
                            rtmp.start();  //Start clears queues also
                            rtmp.startBitrateChecks();
                        } catch (Exception e) {
                            e.printStackTrace();
                            rtmp.isRTMPError();  //To handle exceptions in rtmp.prepare()
                        }
                    }
                    mRtmpMuxerAsyncStartStopThread.mIsRunning = false;
                    AppLibrary.log_d(TAG, "RTMP publishing restarted. Recovered from an error :)");
                }
            }, 200);
        }
    }

    public void startRecording() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Play sound irrelevant of recording because streaming must definitely be going
//        mSound.play(MediaActionSound.START_VIDEO_RECORDING);
        if (this.RECORD_STREAM && this.mRecordingMuxer != null) {
            try {
                this.recordingStarted = true;
                this.mRecordingMuxer.start();
                AppLibrary.log_d(TAG, "RecordingMuxer started!");
            } catch (Exception ioe) {
                this.recordingStarted = false;
                AppLibrary.log_e(TAG, "Unable to start RecordingMuxer! " + ioe.getMessage());
            }
        }
    }

    public void continueWithoutPopups() {
        this.continueWithoutBitrateChecks = true;
        if (this.rtmp != null)
            this.rtmp.stopBitrateChecks();
    }

    /* Codec & Muxer callbacks */
    public void onAudioError(String message) {
        isError = true;
        AppLibrary.log_e(TAG, message);
        asyncStop(message);
        MasterClass.getEventBus().post(new BroadCastSignals.ErrorSignal());
    }

    public void onAudioHeader(byte[] header, int numChannels, int sampleRate, int sampleSize) {
        Log.d(TAG, "onaudioheader");
        if (this.rtmp != null)
            this.rtmp.setAudioHeader(header, numChannels, sampleRate, sampleSize);
        if (this.RECORD_STREAM) {
            if (this.mRecordingMuxer != null) {
                MediaFrame aFrame = new MediaFrame();
                aFrame.buffer = header;
                aFrame.type = RecordingMuxer.MediaFrameType.AUDIOHEADER;
                this.mRecordingMuxer.postFrame(aFrame); //Default timestamp is 0 if not specified
            }
        }
    }

    public void onAudioFrame(byte[] buffer, long ts) {
        if(VERBOSE) {
            Log.d(TAG, "onaudioframe");
        }
//        ts = this.timeSync.processAudio(ts);
//        if (lastSentVideoTS <= 3) {  //Don't send audio before sending video SPS & PPS & first frame (as there is a jump from 1st to 2nd frame)
//            this.lastSentAudioTS = ts;
//            return;
//        } else if (AudioStartTimestampDelta == 0) {
//            AudioStartTimestampDelta = ts - lastSentVideoTS; //Initial time that audio starts streaming before video, when streaming first starts
//            AppLibrary.log_d(TAG, "Setting AudioStartTimeStampDelta as " + AudioStartTimestampDelta + " with lastSentVideoTS as " + lastSentVideoTS);
//            return; //Skip the audio frame at the risk of sending 0 timestamp
//        }

//        if (isCameraPaused) //If camera is paused, don't send audio until the first video frame is sent again
//            return;
//        else if (doAudioCheck) {
//            audioCorrectionCounter++;
//            pausedAudioDuration += ts - AudioStartTimestampDelta - tempPauseVideoTS - (1024000 / this.mSettings.audioSamplingRate); //Calculated the cumulative paused AudioDuration to subtract from ts
////            pausedAudioDuration += ts - AudioStartTimestampDelta - tempPauseVideoTS; //Calculated the cumulative paused AudioDuration to subtract from ts
//            doAudioCheck = false;
//        }

        if (this.isPublishing()) {
            ts = ts - pausedAudioDuration - AudioStartTimestampDelta;
            if (ts <= 0) //Paranoia
                ts = 1;
            this.lastSentAudioTS = ts;
            if (this.rtmp != null) {
                RtmpMuxer.Frame frame = new RtmpMuxer.Frame();
                frame.type = RtmpMuxer.FrameType.AUDIO;
                frame.buffer = buffer;
                frame.pts = ts;
                frame.dts = ts;
                this.audioBytes += (long) buffer.length;
//                updateBytes();
                this.rtmp.postFrame(frame);
            }
            sendAudioFrameToRecordingMuxer(buffer, ts);
        }
    }

    private void sendAudioFrameToRecordingMuxer(byte[] buffer, long ts) {
        if (this.RECORD_STREAM) {
            if (this.mRecordingMuxer != null && this.sendFramesToRecorder) {
                MediaFrame aFrame = new MediaFrame();
                aFrame.buffer = buffer;
                aFrame.timestamp = ts;
                aFrame.type = RecordingMuxer.MediaFrameType.AUDIO;
                this.mRecordingMuxer.postFrame(aFrame);
            }
        }
    }

    @Override
    public void addDummyTrack() {
        mRecordingMuxer.addDummyTrack();
    }

    @Override
    public void addTrack(MediaFormat mediaFormat) {
        mRecordingMuxer.addTrack(mediaFormat);
    }

    @Override
    public void sendDirectAudioToRecordingMuxer(int trackIndex,
                                                ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
        MediaFrame mediaFrame = new MediaFrame();
        mediaFrame.buffer = buffer.array();
        mediaFrame.bufferInfo = bufferInfo;
        mediaFrame.timestamp = bufferInfo.presentationTimeUs;
        mediaFrame.type = RecordingMuxer.MediaFrameType.AUDIO;

        if(VERBOSE) {
            Log.d(TAG, "audio timestamp: " + bufferInfo.presentationTimeUs);
        }
//        mediaFrame.trackIndex = trackIndex;
        if (mRecordingMuxer != null) {
            if (mediaFrame.timestamp > -1) {
                mRecordingMuxer.postFrame(mediaFrame);
            } else {
                Log.d(TAG, "Skipping audio frame with timestamp: " + mediaFrame.timestamp);
            }
        } else {
            Log.w(TAG, "RecordingMuxer is null!");
        }
    }

    public void onVideoError(String message) { //Called when SPS and PPS cannot be retrieved from encoder
        isError = true;
        AppLibrary.log_e(TAG, message);
        asyncStop(message);
        MasterClass.getEventBus().post(new BroadCastSignals.ErrorSignal());
    }

    public void onVideoNal(byte[] buffer, long pts, long dts) {
        if (!isPublishing()) return;
        long d = pts - dts; //Do we need this now? Can't say
//      long tempActualDTS = this.lastSentVideoTS + pts;
//      long ts = this.timeSync.processVideo(tempActualDTS);
//      this.lastSentVideoTS += ts;

        this.lastSentVideoTS += pts;

//        if (isCameraPaused) {
//            tempPauseVideoTS = this.lastSentVideoTS; //Revised benchmark to start streaming
//            isCameraPaused = false;
//            doAudioCheck = true;
//        }

        int type = buffer[0] & 31;
        if (VERBOSE) {
            Log.d(TAG, "-----------------------------------Posting video frame! " + type + " Length " + buffer.length + " " + pts + " " + lastSentVideoTS);
        }
        if (this.rtmp != null) {
            RtmpMuxer.Frame frame = new RtmpMuxer.Frame();
            frame.type = RtmpMuxer.FrameType.VIDEO;
            frame.buffer = buffer;

//            --->Commenting out old audio-only mode concept of sending smallest Type5 Nal
//            if (RtmpMuxer.SMART_BITRATE_MODE_ENABLED) {
//                if (type == 1) {
//                    if (this.smallestFoundType1NAL == null ||
//                            buffer.length < this.smallestFoundType1NAL.length)
//                        this.smallestFoundType1NAL = buffer;
//                    if (this.mIsAudioOnlyMode) {
//                        frame.buffer = this.smallestFoundType1NAL;
//                        //Can be used for making decoder ignore NAL
//                        //                    byte header = buffer[0];
//                        //                    header = (byte) (header | (byte) (0 << 1));
//                        //                    header = (byte) (header | (byte) (0 << 2));
//                        //                    buffer[0] = header;
//                    }
//
//                } else if (type == 5) {
//                    if (mIsAudioOnlyMode) {
//                        if (this.continueWithoutBitrateChecks) {
//                            this.continueWithoutBitrateChecks = false;
//                            this.mIsAudioOnlyMode = false;
//                            this.mAudioModeKeyframeCounter = 0;
//                        }
//                        if (this.lastSentType5NAL == null)
//                            this.lastSentType5NAL = buffer;
//                        if (this.mAudioModeKeyframeCounter != 0) {
//                            frame.buffer = lastSentType5NAL;
//                        } else {
//                            this.lastSentType5NAL = buffer;
//                        }
//                        this.mAudioModeKeyframeCounter = (this.mAudioModeKeyframeCounter + 1) % AUDIO_MODE_KEYFRAME_INTERVAL;
//                    }
//                }
//            }
            if (type == 7 || type == 8) {
                this.rtmp.pauseBitrateChecks(2000);  //Realistically start bitrate check timers only after SPS & PPS has been sent
                this.rtmp.setSPSPPS(type, frame); //Store SPS/PPS for error checks
            }

            frame.pts = this.lastSentVideoTS + d;
            frame.dts = this.lastSentVideoTS;
            this.videoBytes += (long) buffer.length;
//            updateBytes();
            this.rtmp.postFrame(frame);
        }
        //TODO: Remove this test
        if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
            if (this.lastSentVideoTS >= this.tempMusicStartPosition && this.lastSentVideoTS < this.tempMusicStopPosition && this.mMusicPlayer == null) {//Start around 2 secs after streaming start
                this.mMusicPlayer = new BackgroundMusicPlayer();
                this.mMusicPlayer.setMediaFile(this.tempMusicFile);
                this.mMusicPlayer.start();
            }

            if (this.lastSentVideoTS >= this.tempMusicStopPosition && this.mMusicPlayer != null) {
                this.mMusicPlayer.stop();
                this.mMusicPlayer = null;
            }
//            if(this.lastSentVideoTS>=this.tempMusicStopPosition && this.lastSentVideoTS<=2*this.tempMusicStartPosition+this.tempMusicStartPosition && this.mMusicPlayer!=null) {
//                this.mMusicPlayer.stop();
//            }
//            if(this.lastSentVideoTS>=2*this.tempMusicStartPosition+this.tempMusicStartPosition && this.mMusicPlayer!=null) {
//                this.mMusicPlayer.setMediaFile(this.temMusicFile1);
//                this.mMusicPlayer.start();
//            }
//            if(this.lastSentVideoTS>=2*this.tempMusicStopPosition && this.mMusicPlayer!=null) {
//                this.mMusicPlayer.stop();
//            }
        }
        sendVideoFramesToRecordingMuxer(buffer, this.lastSentVideoTS, type);
    }

    void sendVideoFramesToRecordingMuxer(byte[] buffer, long ts, int type) {
        ts *= 1000;
        boolean doSend = false;
        if (this.RECORD_STREAM) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            if (this.mRecordingMuxer != null) {
                if (type == 7 || type == 8) {
                    doSend = true;
                } else {
                    if (!this.sendFramesToRecorder) {
                        if (this.recordingStarted) {
                            if (type == 5) {
                                this.sendFramesToRecorder = true;
                                doSend = true;
                            }
                        }
                    } else {
                        doSend = true;
                    }
                }
                if (type == 9) {
                    RecordingMuxer.MediaFrame vFrame = new RecordingMuxer.MediaFrame();
                    vFrame.type = RecordingMuxer.MediaFrameType.EOS;
                    this.mRecordingMuxer.postFrame(vFrame);
//                    if(VERBOSE) {
                    Log.d(TAG, "send video frame EOS");
//                    }
                } else {
                    if (doSend) {
                        // Differentiate flow of EOS and Video type I/V frames.
                        RecordingMuxer.MediaFrame vFrame = new RecordingMuxer.MediaFrame();
                        vFrame.buffer = buffer;
                        vFrame.timestamp = ts;
                        bufferInfo.presentationTimeUs = ts;

                        vFrame.bufferInfo = bufferInfo;
                        vFrame.type = RecordingMuxer.MediaFrameType.VIDEO;
                        this.mRecordingMuxer.postFrame(vFrame);
//                    if(VERBOSE) {
                        Log.d(TAG, "send video frame timestamp: " + ts + "frame: " + vFrame + " type: " + type);
//                    }
                    }
                }
            }
        }
    }

    public void onRtmpDecreaseBitrate(final float dropRate) {
//        if (!RtmpMuxer.SMART_BITRATE_MODE_ENABLED) return;
//        if (!this.isPublishing()) return;
//        if (continueWithoutBitrateChecks) return;
//
//        if (this.videoBitrate < THRESHOLD_LOWEST_VIDEO_BITRATE) {
//            if (!CameraActivity.isBroadcastStarted)
//                return;   //Prevents upload dialog from showing up before pressing "Start Broadcast" and lets the timers keep running!
//
//            this.bitrateReductionsConsecutiveCounter = 3; //Once it reaches lower threshold, reduce upper threshold
//
//            RtmpMuxer.mIsBitrateLowerThresholdForCurrentSession = true;     //Sets bitrate lower threshold to avoid further timer checks
//            ((CameraActivity) mContext).handleOnRtmpErrorDelayed();
//            if (this.rtmp != null)
//                this.rtmp.stopBitrateCheckTimers();
//
//            AppLibrary.log_d(TAG, "Bitrate lower threshold reached. Current bitrate is " + this.videoBitrate);
//            return;
//        }
//
////        videoBitrate = (int)Math.abs(videoBitrate* (1-(dropRate/100)));  //Decreasing bitrate by % droprate
//        videoBitrate-=BITRATE_DROP_PER_CYCLE;
//        if(videoBitrate<145)
//            videoBitrate = 145; //Handles case of extreme network jitter like 52% drop from bitrate 204
//
//        if(mRenderer!=null && (videoBitrate<700)) {
//            mRenderer.changeBitrate(videoBitrate);
//
//            AppLibrary.log_d(TAG, "Conveyed bitrate decrease of " + BITRATE_DROP_PER_CYCLE+ " to renderer. New bitrate is " + videoBitrate);
//
//            this.rtmp.pauseBitrateChecks(2000);  //Give the network some time to recover before starting bitrate check timers again
//            this.bitrateReductionsConsecutiveCounter++;
//
//            if (this.videoBitrate < THRESHOLD_HIGHEST_VIDEO_BITRATE)
//                RtmpMuxer.mIsBitrateUpperThresholdForCurrentSession = false;   //Resets Upper threshold flag since bitrate has decreased. Rare case, but just for safety!
//        }
////        status = ((NewBroadCastPage) mContext).mStatus;
////        if (status != NewBroadCastPage.BroadCastStatus.STATE_BROADCAST_LIVE) {
////            if (this.rtmp != null)
////                this.rtmp.stopBitrateCheckTimers();
////            return;
////        }
////        if (!this.mIsAudioOnlyMode) {
////            this.mIsAudioOnlyMode = true;
////            if (this.rtmp != null) {
////                this.rtmp.toggleBitrateChecks();
////                ((NewBroadCastPage) mContext).startDelayedInternetCheckCountDownTimer();
////            }
////            AppLibrary.log_d(TAG, "Audio only mode started!");
////        }
    }

    public void onRtmpIncreaseBitrate() {
//        if (!RtmpMuxer.SMART_BITRATE_MODE_ENABLED) return;
//        if (!this.isPublishing()) return;
//        if (continueWithoutBitrateChecks)
//            return;  //Rare possibility as this flag is used when internet is bad, but just in case!
//
//        if(bitrateReductionsConsecutiveCounter>0) {
//            if(bitrateReductionsConsecutiveCounter>=2)
//                THRESHOLD_HIGHEST_VIDEO_BITRATE -= 15;      //If two reductions in a row, then decrease upper threshold
//            bitrateReductionsConsecutiveCounter = 0;
//
//            if(THRESHOLD_HIGHEST_VIDEO_BITRATE < 350)   //But don't let the upper threshold fall below 350
//                THRESHOLD_HIGHEST_VIDEO_BITRATE = 350;
//
//            BITRATE_DROP_PER_CYCLE-=5;          //Cumulatively keep decreasing the bitrate drop per cycle of decrease, to help converge to an optimum bandwidth
//            BITRATE_INCREASE_PER_CYCLE-=2;      //Cumulatively keep decreasing the bitrate increase per cycle of decrease, to help converge to an optimum bandwidth
//
//            if(BITRATE_DROP_PER_CYCLE<20)       //Minimum drop per cycle set as 20
//                BITRATE_DROP_PER_CYCLE = 20;
//
//            if(BITRATE_INCREASE_PER_CYCLE<10)   //Minimum increase per cycle set as 10
//                BITRATE_INCREASE_PER_CYCLE = 10;
//        }
//
//        if (this.videoBitrate > THRESHOLD_HIGHEST_VIDEO_BITRATE) {
//            RtmpMuxer.mIsBitrateUpperThresholdForCurrentSession = true; //Sets bitrate threshold to avoid further timer checks
//            if (this.rtmp != null)
//                this.rtmp.stopBitrateCheckTimers();
//
//            AppLibrary.log_d(TAG, "Bitrate upper threshold reached. Current bitrate is " + this.videoBitrate);
//            return;
//        }
//
//        videoBitrate += BITRATE_INCREASE_PER_CYCLE;
//        if (mRenderer != null && (videoBitrate > 145)) {
//            mRenderer.changeBitrate(videoBitrate);
//            AppLibrary.log_d(TAG, "Conveyed bitrate increase of " + BITRATE_INCREASE_PER_CYCLE + " to renderer. New bitrate is " + videoBitrate);
//
//            if (this.videoBitrate > THRESHOLD_LOWEST_VIDEO_BITRATE)
//                RtmpMuxer.mIsBitrateLowerThresholdForCurrentSession = false;  //Resets Lower threshold flag if increased bitrate is greater than threshold
//        }
////        if (this.bitrateReductionsConsecutiveCounter < MAX_BITRATE_REDUCTIONS_COUNT) {
////            this.continueWithoutBitrateChecks = true;
////            status = ((NewBroadCastPage) mContext).mStatus;
////            if (status == NewBroadCastPage.BroadCastStatus.STATE_BROADCAST_LIVE)
////                this.bitrateReductionsConsecutiveCounter++;
////            if (this.rtmp != null) {
////                ((NewBroadCastPage) mContext).stopDelayedInternetCheckCountDownTimer();
////                if (this.bitrateReductionsConsecutiveCounter >= MAX_BITRATE_REDUCTIONS_COUNT) {
////                    this.rtmp.makeNormalModePermanentForCurrentSession();
////                    ((NewBroadCastPage) mContext).showDelayedContinueWithUploadDialog();
////                }
////                this.rtmp.toggleBitrateChecks();
////                AppLibrary.log_d(TAG, "Normal streaming mode resumed! " + this.bitrateReductionsConsecutiveCounter);
////            }
////        }
    }

    public void onRtmpError(String message) {
        AppLibrary.log_e(TAG, message);
        if ((this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning) /*||
                ((CameraActivity) mContext).mStatus == CameraActivity.BroadCastStatus.STATE_RECORD_WITH_UPLOAD_LATER ||
                ((CameraActivity) mContext).mStatus == CameraActivity.BroadCastStatus.STATE_TRANSITIONINNG_RECORD_WITH_UPLOAD_LATER*/)
            return;
        if (this.rtmp != null) {
            rtmpErrorCounter++;
            AppLibrary.log_e(TAG, "rtmpErrorCounter is " + rtmpErrorCounter);

            this.rtmp.stopBitrateChecks(); //Stopping bitrate checks
            this.rtmp.forceCloseConnection();
            if (rtmpErrorCounter > 4 || message.contains("QUIT")) { //Recover from RTMP errors atleast 5 times, else show upload popup
                this.rtmp = null; //Make rtmp null
//                ((CameraActivity) mContext).handleOnRtmpErrorDelayed(); //Upload popup
                //asyncStop(message); //Do not stop, it can lead to crash maybe?
            } else {
                asyncStartRtmpPublishing();
            }
        }
    }

    public void BitrateIncreaseForUploadMode() {
        if (mRenderer != null)
            mRenderer.changeBitrate(1024);
    }

    public void onRecordingError(String message) {
        AppLibrary.log_e(TAG, String.format("RecordingMuxer error, need to stop: %s", message));
        if (this.mRecordingMuxer != null) {
            //ToDo: Probably show some toast here to user
            this.mRecordingMuxer.flushStop();
            this.mRecordingMuxer = null;
            this.mLastRecordedFilepath = "";
        }
        AppLibrary.log_e(TAG, "RecordingMuxer nullified");
    }

    public void run() {
        if (this.publishing) {
            stop("");
        } else {
            start();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        AppLibrary.log_d(TAG, String.format("Surface changed width=%d, height=%d", new Object[]{Integer.valueOf(width), Integer.valueOf(height)}));
////        setCameraConfiguration();
//
//
//        if (this.publishing && NewBroadCastPage.isBroadcastStarted == false) {
//            waitThread();
//            asyncStop("");
//            waitThread();
//            stopCamera();
//            startCamera();
////            asyncStart();
//            AppLibrary.log_d("Publishing Tag", "If is called in Surface Changed");
//        }
//
//        else if (this.publishing && NewBroadCastPage.isBroadcastStarted == true)
//        {
//            waitThread();
//            asyncStop("");
//            waitThread();
//            stopCamera();
//            AppLibrary.log_d("Publishing Tag", "If is called in Surface Changed");
//            AppLibrary.log_d("First Time", "Value in IF surface changed, Swiped Up True:-> " + NewBroadCastPage.firsttime);
//        }
//
//        else
//        {
////            waitThread();
////            stopCamera();
////            startCamera();
////            if(!NewBroadCastPage.firsttime && NewBroadCastPage.isBroadcastStopped!= true) {
////                new android.os.Handler().postDelayed(new Runnable() {
////                    @Override
////                    public void run() {
////                        waitThread();
////                        asyncStart();
////                    }
////                }, 1000);
////                AppLibrary.log_d("Publishing Tag", "Async Task Surface Changed");
////            }
//
        setCameraConfiguration();
//            AppLibrary.log_d("Publishing Tag", "Else is called in Surface Changed");
//            AppLibrary.log_d("First Time", "Value in ELSE surface changed:-> " + NewBroadCastPage.firsttime);
//
//        }
//        if(NewBroadCastPage.isBroadcastStopped != true)
//            NewBroadCastPage.firsttime = false;
//
///*orientationResult = this.mContext.getWindowManager().getDefaultDisplay().getRotation();
//tagObj.screenOrientation = orientationResult;
//AppLibrary.log_d(TAG, "OrientationResult: " + orientationResult);*/
    }

    public void surfaceCreated(SurfaceHolder holder) {
        AppLibrary.log_d(TAG, "Surface created");
//        startCamera();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
//        AppLibrary.log_d(TAG, "Surface destroyed");
//        waitThread();
//        asyncStop("");
//        waitThread();
//        stopCamera();
    }

    /*class CustomTagObject {
        public String videoSize;
        public int screenOrientation;

    }*/

    public void prepare() {
        AppLibrary.log_i(TAG, "This is called: " + orientationResult);
        /*if(this.mContext.getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90 ||
                this.mContext.getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270) {*/
//        this.width = mContext.getWindowManager().getDefaultDisplay().getWidth();
//        this.height = mContext.getWindowManager().getDefaultDisplay().getHeight();
//        }

        this.textMessage.setTextColor(Color.parseColor("#FF0000"));
        this.holder = this.mGLSurfaceView.getHolder();
//        this.holder.addCallback(this);
        this.handler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle.containsKey("Message") && RecordingActionControllerKitkat.this.textMessage != null) {
//                    RecordingActionControllerKitkat.this.textMessage.setText(bundle.getString("Message"));
                }
                if (bundle.containsKey("Screen") && RecordingActionControllerKitkat.this.mContext != null) {
                    if (bundle.getBoolean("Screen")) {
                        RecordingActionControllerKitkat.this.mContext.getWindow().addFlags(128);
                    } else {
                        RecordingActionControllerKitkat.this.mContext.getWindow().clearFlags(128);
                    }
                }
                if (bundle.containsKey("Bytes") && RecordingActionControllerKitkat.this.textMessage != null) {
                    RecordingActionControllerKitkat.this.textMessage.setText(String.format("audio: %dK, video: %dK", new Object[]{Long.valueOf(RecordingActionControllerKitkat.this.audioBytes / 1024), Long.valueOf(RecordingActionControllerKitkat.this.videoBytes / 1024)}));
                }
            }
        };

        mRenderer.flagForReset();

        mSmartFocusMode = true;
        if (mCaptureResultHandler == null) {
            mCaptureResultHandlerThread = new CustomHandlerThread(this, "CaptureResultHandlerThread");
            mCaptureResultHandler = mCaptureResultHandlerThread.getHandler();
        }
    }

    public void startPublishing() {
//        updateCameraOrientation();
        if (!isPublishing()) {
            waitThread();
            asyncStart();
        }
    }

    public void resumeView() throws Exception {
        AppLibrary.log_d(TAG, "onResume");
        if (this.mGLSurfaceView == null) return;

        // Only create this thread and handler if it hasn't been created before.
        // This has been done, because there are multiple entries into this function.
        if (mCaptureResultHandlerThread == null) {
            mCaptureResultHandlerThread = new CustomHandlerThread(this, "CaptureResultHandlerThread");
            mCaptureResultHandler = mCaptureResultHandlerThread.getHandler();
        }

        waitThread();
//        startCamera(); //This startCamera doesn't start preview, just opens the camera

        this.mGLSurfaceView.onResume();
        if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
            if (this.mMusicPlayer != null)
                this.mMusicPlayer.resume();
        }

        Display display = this.mContext.getWindowManager().getDefaultDisplay();
        orientationResult = display.getRotation();
//        if (orientationResult % 180 == 0) {
//            mVideoBackgroundEnabled = true;
//        }
        setBackgroundVideoEnabled(mVideoBackgroundEnabled);
        AppLibrary.log_d(TAG, "onResume complete: " + mCameraPreviewWidth + "x" + mCameraPreviewHeight + " " + this);
    }

    public void setBackgroundVideoEnabled(boolean videoBackgroundEnabled) {
        mVideoBackgroundEnabled = videoBackgroundEnabled;
        if (mCaptureResultHandler == null) {
            Log.w(TAG, "setBackgroundVideoEnabled " + mVideoBackgroundEnabled + " but mCaptureResultHandler is null");
            return;
        }
        mCaptureResultHandler.removeCallbacks(mCaptureVideoBackgroundRunnable);
        mCaptureResultHandler.removeCallbacks(mCaptureVideoBackgroundDisableRunnable);

        if (mVideoBackgroundEnabled) {
            mCaptureResultHandler.postDelayed(mCaptureVideoBackgroundRunnable, 0);
        } else {
            mCaptureResultHandler.postDelayed(mCaptureVideoBackgroundDisableRunnable, 300);
        }
    }

    public void pauseView() {
        pauseView(false);
    }

    public void pauseView(boolean force) {
        AppLibrary.log_d(TAG, "onPause -- releasing camera");
        if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
            if (this.mMusicPlayer != null)
                this.mMusicPlayer.pause();
        }
//        waitThread();
//        stopCamera();

        if (this.publishing) {
            isCameraPaused = true;
            if (this.rtmp != null)
                this.rtmp.stopBitrateChecks();  //Stop bitrate check timers while exiting the app
        }
        if (force || (mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) == 0) {
            if (mMoviePlayer != null) {
                mMoviePlayer.pause(true);
/*            mMoviePlayer.requestStop();
            mPlayTask.waitForStop();*/
            }
/*        if (mPlayTask != null) {
            mPlayTask.waitForStop();
        }*/
            mRenderer.pause();
        }

        if (this.mGLSurfaceView != null) {
            this.mSurfaceTexture = null; //Just removes the reference of the surface texture in RecordingActionControllerKitkat class
            this.mGLSurfaceView.onPause();
        }

        if (mCaptureResultHandlerThread != null) {
            mCaptureResultHandler.removeCallbacksAndMessages(null);
            mCaptureResultHandlerThread.quit();
            mCaptureResultHandlerThread = null;
            mCaptureResultHandler = null;
        }
        if (mCameraCapturer != null) {
            mCameraCapturer.release();
        }
        if (force || (mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) == 0) {
            if (mDummyRenderer != null) {
                mDummyRenderer.release();
            }
        }

        AppLibrary.log_d(TAG, "onPause complete " + this.publishing);
    }

    public boolean isPublishing() {
        return this.publishing;
    }

    public void actionPublish() {
        if (this.publishing) {
            Log.d(TAG, "actionPublish :: Publishing, so stopping");
            waitThread();
            asyncStop("stopped");

        } else {
            Log.w(TAG, "actionPublish :: NOT Publishing, so starting");
            waitThread();
            asyncStart();
        }
    }

    public void destroy() {
        if(mMoviePlayer != null) {
            mMoviePlayer.requestStop();
        }
        if(mPlayTask != null) {
            mPlayTask.waitForStop();
        }
        mRenderer.destroy();
        if (mCameraCapturer != null) {
            mCameraCapturer.destroy();
            mCameraCapturer = null;
        }
        mCameraHandler.invalidateHandler();     // paranoia

//        mRenderer.setCameraPreviewRunning(false);
        if (mHandlerThread != null) {
            mHandlerThread.getHandler().removeCallbacksAndMessages(null);
            mHandlerThread.interrupt();
            mHandlerThread = null;
        }
        if (mRtmpMuxerAsyncStartStopThread != null) {
            mRtmpMuxerAsyncStartStopThread.getHandler().removeCallbacksAndMessages(null);
            mRtmpMuxerAsyncStartStopThread.interrupt();
            mRtmpMuxerAsyncStartStopThread = null;
        }

//        BroadCastSettings.destroy();
        FilterManager.destroy();
//        this.mSettings = null;
//        this.layout = null;
        this.mGLSurfaceView = null;
        this.textMessage = null;
        this.mContext = null;
        this.mFilter = null;
        this.mTempFilter = null;
        this.mSound = null;
    }

    /* filters */
    private FilterManager mFilter;
    private FilterManager mTempFilter;

    public FilterManager getFilter() {
        AppLibrary.log_i(TAG, "get filter call");
        if (mFilter != null) {
            return FilterManager.getInstance(mFilter);
        }
        AppLibrary.log_i(TAG, "filters null");
        return this.mFilter;
    }

    public void setNewFilter(FilterManager mSetFilter) {
        if (mFilter == null || mSetFilter == null) {
            AppLibrary.log_e(TAG, "Filters are not initialized yet.");
            return;
        }
        AppLibrary.log_i(TAG, "setting new filter");

        Camera.Parameters params = this.camera.getParameters();

        if (this.camera != null && params != null) {

            if (CameraActivity.PreRecordFilters && mSetFilter.getColorEffect() != null) {
                if (mTempFilter == null) {
                    params.setColorEffect(mSetFilter.getColorEffect());
                    AppLibrary.log_i(TAG, "color effect done " + mFilter.getColorEffect() + ":" + mSetFilter.getColorEffect());
                } else if (!mTempFilter.getColorEffect().equals(mSetFilter.getColorEffect())) {
                    params.setColorEffect(mSetFilter.getColorEffect());
                    AppLibrary.log_i(TAG, "color effect done " + mFilter.getColorEffect() + ":" + mSetFilter.getColorEffect());
                }
            }

            if (CameraActivity.flashMode && mSetFilter.getFlashMode() != null) {

                if (mTempFilter == null) {
                    params.setFlashMode(mSetFilter.getFlashMode());
                    AppLibrary.log_i(TAG, "Flash mode done 1 " + mFilter.getFlashMode() + ":" + mSetFilter.getFlashMode());
                } else if (!mTempFilter.getFlashMode().equals(mSetFilter.getFlashMode())) {
                    params.setFlashMode(mSetFilter.getFlashMode());
                    AppLibrary.log_i(TAG, "Flash mode done 2" + mFilter.getFlashMode() + ":" + mSetFilter.getFlashMode());
                }
            }

            try {
                this.camera.setParameters(params);
            } catch (Exception e) {
                AppLibrary.log_e(TAG, "Exception in setting filters: " + e.toString());
                e.printStackTrace();
            }
        }
        mTempFilter = FilterManager.getInstance(mSetFilter);
    }

    public void setStreamKey(String key, String format) {
        if (this.publishing) return;

        String quality = BroadCastSettings.formatFromQuality(format);
        mSettings = BroadCastSettings.getInstance(quality);
//        String IP = ((CameraActivity) mContext).getRTMPIP();

//        String [] size = this.videoSize.split("x");
//        int extraHeight = Integer.parseInt(size[1])*width/Integer.parseInt(size[0]) - height;
//        tagObj.videoSize = this.videoSize;
//        tagObj.screenOrientation = orientationResult;
//        this.layout.setTag(tagObj);
//        this.layout.setY(-extraHeight/2); 52.74.84.209/
//        this.rtmpUrl = "rtmp://119.9.91.60/live" + format + "/" + key;

//        this.rtmpUrl = "rtmp://52.74.184.138/live" + format + "_v1/" + key;

//        if (IP != null)
//            this.rtmpUrl = "rtmp://" + IP + "/live" + format + "_v1/" + key;
//        else
//            this.rtmpUrl = "rtmp://52.17.111.51/live" + format + "_v1/" + key; //Europe Server
//        this.rtmpUrl = "rtmp://52.74.184.138/live" + format + "_v1/" + key;
    }

    BroadCastSignals.RestartBroadCastSignal mRSSignal;

    public void sendRestartSignal(BroadCastSignals.RestartBroadCastSignal rsSignal) {
        mRSSignal = rsSignal;
        mCaptureMode |= CAPTURE_MODE_CURRENT_FRAME;
        if (mCameraCapturer != null) {
            mCameraCapturer.setCaptureFrequency(2);
            mCameraCapturer.setCaptureEnabled(true);
            mRenderer.setCaptureFilter(-1); // -1 sets to current filter used for preview/video.
            // setCaptureFilter should always be called after calling setCaptureEnabled

            mRenderer.setCaptureEnabled(true);
        } else {
//            stopCameraSwapAnimation(null);
            restartController();
        }
    }

    private void actualRestartController(BroadCastSignals.RestartBroadCastSignal rsSignal) {
        if (rsSignal == null) {
            Log.e(TAG, "RSSignal is null");
            return;
        }
        String quality = rsSignal.getQuality();
        boolean changeCamera = rsSignal.isChangeCamera();
        if (changeCamera && mSettings.deviceCameraCount() <= 1) return;
        if (quality == null && !changeCamera)
            return;

        AppLibrary.log_d(TAG, "stopping camera");

        if (!changeCamera) {
            if (mCameraCapturer != null)
                mCameraCapturer.release();

            waitThread();

            this.mGLSurfaceView.onPause(); //Losing the GL surfaceView context and recreating again - Can be avoided
        }

        stopCamera();

        AppLibrary.log_d(TAG, "loading new settings");
        if (quality != null)
            mSettings = BroadCastSettings.getInstance(quality);
        mSettings.swapCamera(changeCamera);

        if (changeCamera) {
            mZoomLevel = 1.0f;
            mRenderer.setIsFlipped(((mSettings.getCameraId() % 2) == 0) ? false : true);
            mRenderer.setScale(1.0f);
        }
        if (mRSSignal.getStreamFormat() != null && mRSSignal.getStreamKey() != null)
            setStreamKey(mRSSignal.getStreamKey(), mRSSignal.getStreamFormat());

        AppLibrary.log_d(TAG, "restarting camera with settings");

        if (!changeCamera) {
            this.mGLSurfaceView.onResume();
        }

        mRenderer.setCameraChanged(true);

        startCamera();

        if (changeCamera) {
            Log.d(TAG, "Camera changed, so sending new render params");
            mRenderer.setRenderParams();
        }
        if (this.camera == null) {
//            ((CameraActivity) mContext).switchCameraClicked(); //Send signal to switch back to the previous camera if the camera doesn't open
        }

        AppLibrary.log_d(TAG, "restart done, now start broadcasting");
    }

    private void restartController() {
        actualRestartController(mRSSignal);
        mRSSignal = null;
    }

    public void restartController(BroadCastSignals.RestartBroadCastSignal rsSignal) {
        if (mSettings.deviceCameraCount() <= 1) return;
        sendRestartSignal(rsSignal);
    }

    private void queueRendererResuming() {
        // Set the preview aspect ratio.
        final int mCameraPreviewWidth = previewSize.width;
        final int mCameraPreviewHeight = previewSize.height;
        this.mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
            }
        });
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        AppLibrary.log_d(TAG, "handleSetSurfaceTexture");
        if (isError)
            return; //Return silently if there is an error in streaming - Streaming errors are handled in onAudioError/ onVideoError
/*        st.setOnFrameAvailableListener(this);*/
        if (this.camera == null)
            this.mSurfaceTexture = st;
        else {
            try {
                this.camera.setPreviewTexture(st);
                mSurfaceTexture = st;
//                if (mSmartFocusMode && ((mSettings.getCameraId() % 2) == 0)) {
//                    startListeningSensors();
//                }
//
//                String focusmode;
//
//                if(!((mSettings.getCameraId() % 2) == 0)) {
//                    focusmode = Camera.Parameters.FOCUS_MODE_FIXED;
//                } else {
//                    focusmode = smoothFocusMode;
//                }
//
//                Camera.Parameters params = camera.getParameters();
//                if (params != null) {
//                    try {
//                        if(!(params.getMinExposureCompensation()== 0 && params.getMaxExposureCompensation()==0))
//                            params.setExposureCompensation(0);
////                        if(params.isAutoWhiteBalanceLockSupported())
////                            params.setAutoWhiteBalanceLock(true);
////                        if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
////                            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
////                            Rect areaRect1 = new Rect(-500, -500, 500, 500);    // specify an area in center of image
////                            meteringAreas.add(new Camera.Area(areaRect1, 1000)); // set weight to 100%
////                            params.setMeteringAreas(meteringAreas);
////                        }
//                        if(params.getAntibanding() != null)
//                            params.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
//                        params.setRecordingHint(true);
//                        Camera.Size size = params.getPreviewSize(); //New preview sizes already set in setCameraConfiguration
//                        if(size!=null) {
//                            params.set("video-size", size.width + "x" + size.height);
//                            params.set("preview-size", size.width + "x" + size.height);
//                        }
//                        params.setFocusMode(focusmode);
//                        camera.setParameters(params);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }

                this.camera.startPreview();
            } catch (Exception ioe) {
                this.mSurfaceTexture = null;
                onAudioError(ioe.toString());
                AppLibrary.log_e(TAG, "Camera start preview failed at handleSetSurfaceTexture()");
                return;
            }
        }

        if (mVisionFeaturesEnabled) {
            setupFaceDetection();
        }
    }

    public void onEncoderCreated() {
        sVideoEncoder.getEncoder().setCallback(this);
    }

    @Override
    public void onPictureTakenFinished(Bitmap bitmap) {
        mCapturedBitmap = bitmap;
        mWaterMarkBitmap=(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.stamp_image)).copy(Bitmap.Config.ARGB_8888, true);;
        if(bitmap == null || bitmap.isRecycled()) return;
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageEventListener.onCaptureBitmap(mCapturedBitmap,mWaterMarkBitmap);
                mCapturedBitmap = null;
            }
        });
    }

    @Override
    public void rendererReady() {
        if(mMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            Log.d(TAG, "mSourceMediaFile: " + mSourceMediaFile);
            if(mSourceMediaFile != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                mCapturedBitmap = BitmapFactory.decodeFile(mSourceMediaFile,options);
                Log.d(TAG, "Bitmap obtained for source media file: " + mCapturedBitmap);
                mRenderer.setImageBackground(mCapturedBitmap);
            }
//            mRenderer.addDrawable(0.0f, 0.0f, 1.0f, 1.0f, mCapturedBitmap);
//            mRenderer.requestRender(false);
        }
    }

    private String getSmallestFoundType1NALPreferencesKey() {
        if (this.mCameraPreviewWidth == 0 ||
                this.mCameraPreviewHeight == 0 ||
                this.mSettings == null ||
                this.mSettings.videoBitRate == 0 ||
                this.mCameraPreviewFrameRate == 0)
            return "";
        String preferencesKey = "SmallestType1NAL-" +
                this.mCameraPreviewWidth +
                "x" +
                this.mCameraPreviewHeight +
                "-" +
                this.mSettings.videoBitRate +
                "-" +
                this.mCameraPreviewFrameRate;
        return preferencesKey;
    }

    private void readSmallestFoundType1NALFromPreferences() {
        if (getSmallestFoundType1NALPreferencesKey().isEmpty()) return;
        String b64SmallestFoundType1NAL = preferences.getString(getSmallestFoundType1NALPreferencesKey(), null);
        if (b64SmallestFoundType1NAL == null || b64SmallestFoundType1NAL.isEmpty()) {
            this.smallestFoundType1NAL = null;
            return;
        }
        smallestFoundType1NAL = Base64.decode(b64SmallestFoundType1NAL, Base64.NO_WRAP);
        AppLibrary.log_d(TAG, "Read smallestFoundType1NAL! NAL size: " + smallestFoundType1NAL.length);
    }

    private void saveSmallestFoundType1NALInPreferences() {
        if (getSmallestFoundType1NALPreferencesKey().isEmpty() ||
                smallestFoundType1NAL == null) return;
        byte[] smallestFoundType1NALCopy = smallestFoundType1NAL;
        readSmallestFoundType1NALFromPreferences();
        if (smallestFoundType1NAL != null &&
                smallestFoundType1NALCopy.length >= smallestFoundType1NAL.length)
            return;
        String b64SmallestFoundType1NAL = Base64.encodeToString(smallestFoundType1NALCopy, 0,
                smallestFoundType1NALCopy.length, Base64.NO_WRAP);
        if (b64SmallestFoundType1NAL == null && b64SmallestFoundType1NAL.isEmpty()) return;
        editor.putString(getSmallestFoundType1NALPreferencesKey(), b64SmallestFoundType1NAL);
        editor.commit();
        AppLibrary.log_d(TAG, "Saved smallestFoundType1NAL! NAL size: " + smallestFoundType1NALCopy.length);
    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p/>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<RecordingActionControllerKitkat> mWeakActivity;

        public CameraHandler(RecordingActionControllerKitkat activity) {
            mWeakActivity = new WeakReference<RecordingActionControllerKitkat>(activity);
        }

        public RecordingActionControllerKitkat getRecordingActionControllerKitkat() {
            return mWeakActivity.get();
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakActivity.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            AppLibrary.log_d(TAG, "CameraHandler [" + this + "]: what=" + what);

            RecordingActionControllerKitkat activity = mWeakActivity.get();
            if (activity == null) {
                AppLibrary.log_e(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    activity.startPublishing();
                    activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
//            activity.mCameraHandler.postDelayed(activity.mCameraFocusRunnable, 30000);
        }
    }

    private long FOCUS_INTERVAL = 3000;

    private void startListeningSensors() {

        if (Build.VERSION.SDK_INT > 18) {
            mSensorManager.registerListener(mSensorEventListener, mAcceleroSensor, SENSOR_UPDATE_INTERVAL * 1000, SENSOR_UPDATE_INTERVAL * 250, mSensorHandler);
        } else {
            mSensorManager.registerListener(mSensorEventListener, mAcceleroSensor, SENSOR_UPDATE_INTERVAL * 1000, mSensorHandler);
        }
        mPrevTimeStamp = System.nanoTime();
    }

    private void stopListeningSensors() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    private void initiateSensors() {
        mSensorManager = ((SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE));
        mAcceleroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        this.mSensorEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {
                boolean moving = false;
                moving = mMoving;
                if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

/*                    if (mPrevTimeStamp == 0.0f) {
                        mPrevTimeStamp = sensorEvent.timestamp;
                        return;
                    }*/

                    mCurrTimeStamp = sensorEvent.timestamp;

                    mLinearAcceleration = sensorEvent.values.clone();
                    // alpha is calculated as t / (t + dT)
                    // with t, the low-pass filter's time-constant
                    // and dT, the event delivery rate

                    final float alpha = 0.55f;

                    gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

                    mLinearAcceleration[0] = sensorEvent.values[0] - gravity[0];
                    mLinearAcceleration[1] = sensorEvent.values[1] - gravity[1];
                    mLinearAcceleration[2] = sensorEvent.values[2] - gravity[2];

                    mRawVel[0] += mLinearAcceleration[0] * (mCurrTimeStamp - mPrevTimeStamp) * 0.000001f;
                    mRawVel[1] += mLinearAcceleration[1] * (mCurrTimeStamp - mPrevTimeStamp) * 0.000001f;
                    mRawVel[2] += mLinearAcceleration[2] * (mCurrTimeStamp - mPrevTimeStamp) * 0.000001f;

                    mPrevTimeStamp = mCurrTimeStamp;

                    velGravity[0] = alpha * velGravity[0] + (1 - alpha) * mRawVel[0];
                    velGravity[1] = alpha * velGravity[1] + (1 - alpha) * mRawVel[1];
                    velGravity[2] = alpha * velGravity[2] + (1 - alpha) * mRawVel[2];

                    mVel[0] = mRawVel[0] - velGravity[0];
                    mVel[1] = mRawVel[1] - velGravity[1];
                    mVel[2] = mRawVel[2] - velGravity[2];

                    if (((Math.abs(mLinearAcceleration[0]) + Math.abs(mLinearAcceleration[1]) + Math.abs(mLinearAcceleration[2])) < 0.3f)) {
                        mVel[0] = 0;
                        mRawVel[0] = 0;
                        mVel[1] = 0;
                        mRawVel[1] = 0;
                        mVel[2] = 0;
                        mRawVel[2] = 0;
                    }

                    float absVel = (float) Math.sqrt(mVel[0] * mVel[0] + mVel[1] * mVel[1] + mVel[2] * mVel[2]);

                    if (absVel > VEL_HIGH_THRESHOLD) {
                        if (!moving) {
                            mSensorHandler.removeCallbacksAndMessages(null);
                            mSensorHandler.postDelayed(mSmoothFocusRunnable, 300);
                            VEL_HIGH_THRESHOLD = DEFAULT_VEL_HIGH_THRESHOLD;
                            moving = true;
                        }
                    } else if (moving && (absVel < VEL_LOW_THRESHOLD)) {
                        mSensorHandler.removeCallbacksAndMessages(null);
                        mSensorHandler.postDelayed(mSharpOnceFocusRunnable, 300);
                        moving = false;
                    }
                    mMoving = moving;
                }
            }
        };
    }

    Runnable mCameraFocusRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - mLastCameraFocusTime >= FOCUS_INTERVAL) {
                if (camera != null) {
                    Camera.Parameters params = camera.getParameters();
                    if (params != null) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

                        Log.d(TAG, "Timer elapsed :: trying to set focus mode to fixed");

/*                    boolean isExposureLockSupported = params.isAutoExposureLockSupported();
                    if (isExposureLockSupported) {
                        Log.d(TAG, "Auto focus exposure lock IS supported");
                        params.setAutoExposureLock(false);
                    }*/
                        try {
                            camera.setParameters(params);
                            mLastCameraFocusTime = System.currentTimeMillis();
                        } catch (Exception exception) {
                            Log.e(TAG, "Not able to set focus for the camera");
                        }
                    }
                }
            }
        }
    };

    public boolean isRecorderRunning() {
        if (this.mRecordingMuxer != null &&
                !this.mRecordingMuxer.errored())
            return true;
        return false;
    }

    public Thread isThreadWorking() {
        return this.thread;
    }


    Runnable mFilterTriggerRunnable = new Runnable() {
        @Override
        public void run() {
            mCaptureMode |= CAPTURE_MODE_FILTER;
        }
    };

    public static long BACKGROUND_CAPTURE_INTERVAL = 3500;

    Runnable mCaptureVideoBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            if ((mCaptureMode & (CAPTURE_MODE_FILTER | CAPTURE_MODE_THUMBNAIL | CAPTURE_MODE_BACKGROUND)) > 0) {
                mCaptureResultHandler.postDelayed(this, 500);
                return;
            }

            if (mRenderer == null) {
                return;
            }

            if (mCameraCapturer == null) {
                if (OFFSCREEN_ENABLED) {
                    Log.d(TAG, "Capture Video background runnable : CameraCapturer is null Trying again");
                    mCaptureResultHandler.postDelayed(this, 100);
                }
                return;
            }

            mCaptureMode |= CAPTURE_MODE_BACKGROUND;

            mCameraCapturer.setCaptureFrequency(3);
            mRenderer.setCaptureFilter(-1); // -1 sets to current filter used for preview/video
            mCameraCapturer.setCaptureEnabled(true);
            mRenderer.setCaptureEnabled(true);
            mCaptureResultHandler.postDelayed(this, BACKGROUND_CAPTURE_INTERVAL);
        }
    };

    Runnable mCaptureVideoBackgroundDisableRunnable = new Runnable() {
        @Override
        public void run() {

            if (mCameraCapturer == null) return;

            stopBackgroundCapture();
        }
    };

    Runnable mFilterCaptureRunnable = new Runnable() {
        @Override
        public void run() {

            if (mCameraCapturer == null) return;

            mCaptureMode |= CAPTURE_MODE_FILTER;
            mCameraCapturer.setCaptureFrequency(3);
            if (!mCameraCapturer.isCaptureEnabled())
                mCameraCapturer.resetBitmapQueue();
            mCameraCapturer.setCaptureEnabled(true);
            mCameraCapturer.setFilter(mCurrentCaptureFilter + 1);
            // setFilter always has to be called after calling setCaptureEnabled in case, a filter needs to be forced,
            // as setCapturedEnabled will always set filter o default whivh is natural (0)
            mRenderer.setCaptureEnabled(true);
        }
    };

    Runnable mGIFCaptureRunnable = new Runnable() {
        @Override
        public void run() {

            if (mCameraCapturer == null) return;

            mCaptureMode |= CAPTURE_MODE_GIF;
            mCameraCapturer.setCaptureFrequency(3);
            mCameraCapturer.setCaptureEnabled(true);
            // setFilter always has to be called after calling setCaptureEnabled in case, a filter needs to be forced,
            // as setCapturedEnabled will always set filter o default whivh is natural (0)
            mRenderer.setCaptureEnabled(true);
        }
    };

    Runnable mImageCaptureLifecycleRunnable = new Runnable() {
        @Override
        public void run() {
            if ((mCaptureMode & CAPTURE_MODE_THUMBNAIL) > 0) {
                mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_THUMBNAIL));
                mCaptureMode &= ~CAPTURE_MODE_THUMBNAIL;
                if (mCaptureMode == CAPTURE_MODE_NONE) stopCapture();
            }
        }
    };

    public void OldCaptureImage(final String ImgName) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String name = date + "_" + today.hour + today.minute + today.second + ".jpg";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        this.file = new File(dir, name);
        if (this.camera != null) {
            this.camera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Parameters parameters = camera.getParameters();
                    int format = parameters.getPreviewFormat();

                    //YUV formats require more conversion
                    if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {

                        AppLibrary.log_d("Track", "Format of camera preview is" + format);
                        int w = parameters.getPreviewSize().width;
                        int h = parameters.getPreviewSize().height;
                        Log.d(TAG, "preview width : " + w + " height " + h);
                        // Get the YuV image
                        YuvImage yuv_image = new YuvImage(data, format, w, h, null);
                        // Convert YuV to Jpeg
                        Rect rect = new Rect(0, 0, w, h);
                        ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                        yuv_image.compressToJpeg(rect, 100, output_stream);
                        byte[] bytI = output_stream.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytI, 0, bytI.length);
                        Matrix matrix = new Matrix();
                        if (mSettings.getCameraId() == 0)
                            matrix.postRotate(90);
                        else
                            matrix.postRotate(-90);
                        Bitmap outputBitmap;
                        if ((h - 640) <= 0) {
                            outputBitmap = Bitmap.createBitmap(bitmap, (w - 480) / 2, 0, 480, h, matrix, true);
                        } else {
                            outputBitmap = Bitmap.createBitmap(bitmap, (w - 480) / 2, (h - 640) / 2, 480, 640, matrix, true);
                        }
                        ByteArrayOutputStream output_stream2 = new ByteArrayOutputStream();
                        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output_stream2);
                        byte[] byt = output_stream2.toByteArray();
                        FileOutputStream outStream = null;
                        try {
                            // Write to SD Card

//                                File file = createFileInSDCard(FOLDER_PATH, "Image_"+System.currentTimeMillis()+".jpg");
                            //Uri uriSavedImage = Uri.fromFile(file);
                            outStream = new FileOutputStream(file);
                            outStream.write(byt);
                            outStream.close();
                            AppLibrary.log_d("OneShotPreviewCallback", "Successfully wrote file");

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        } finally {
                        }

                        if (file != null)
                            uploadThumbnail(ImgName);

                    } else {
                        AppLibrary.log_d("Track", "Unknown Format of camera preview is" + format);
                    }
                }

            });
        }
    }


    public void CaptureImage(final String ImgName) {

        if (mCameraCapturer == null || mCaptureResultHandler == null) { // If Camera Capturer is null, Resort to Preview Capture callback
            OldCaptureImage(ImgName);
            return;
        }

        Log.d(TAG, "Starting Image Capture for thumbnail");
        mThumbnailNonStaticBitmap = null;
        mThumbnailStaticBitmap = null;

        mCaptureResultHandler.postDelayed(mImageCaptureLifecycleRunnable, (long) (1.5f * 1000));
        mCaptureMode |= CAPTURE_MODE_THUMBNAIL;
        if (mCapturedBitmapConsumerQueue != null) {
            mCapturedBitmapConsumerQueue.clear();
        }
        mSensorHandler.postDelayed(mSharpOnceFocusRunnable, 0);

        mThumbnailCaptureImageName = ImgName;
        Log.d(TAG, "thumbnail image name set as " + mThumbnailCaptureImageName);

        mCameraCapturer.setCaptureFrequency(6);
        if (mCameraCapturer.isCaptureEnabled())
            mCameraCapturer.resetBitmapQueue();
        mCameraCapturer.setCaptureEnabled(true);
        mCameraCapturer.setFilter(0);

        mRenderer.setCaptureEnabled(true);

    }

    private void uploadThumbnail(final String ImgName) {
        CameraActivity.eventThumbnailUrl = "event/mobile/" + ImgName + ".jpg";
        upload = MasterClass.getTransferUtility().upload("instalively.data", "event/mobile/" + ImgName + ".jpg", file);

        upload.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                AppLibrary.log_e(TAG, "On State changed" + transferState.toString());
                if ((transferState == TransferState.COMPLETED) && publishing) {
//                    try {
//                        thumbnailUploadRequest(ImgName);
//                    } catch (JSONException e1) {
//                        e1.printStackTrace();
//                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                AppLibrary.log_e(TAG, "On Progress changed: " + bytesCurrent + "out of " + bytesTotal);
            }

            @Override
            public void onError(int i, Exception e) {
                AppLibrary.log_e(TAG, "Upload Thumbnail S3Client error: " + e.getMessage());
//                if (mContext!=null && publishing)
//                    ((CameraActivity) mContext).SocialMediaPost();
            }
        });
//                try {
//                    upload.waitForCompletion();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    AppLibrary.log_d("Track", "UPLOAD error: " + e);
//                    upload.abort();
//                    upload = null;
//                    if(mContext!=null)
//                        ((NewBroadCastPage) mContext).SocialMediaPost();
//                    return;
//                } catch (AmazonS3Exception s3Exception) {
//                    if(mContext!=null)
//                        ((NewBroadCastPage) mContext).SocialMediaPost();
//                    return;
//                } catch (AmazonClientException clientException) {
//                    AppLibrary.log_e(TAG, "Some AmazonCLient error! " + clientException.getMessage());
//                    if(mContext!=null)
//                        ((NewBroadCastPage) mContext).SocialMediaPost();
//                    return;
//                } catch (Exception e) {
//                    AppLibrary.log_e(TAG, "Unknown Amazon exception" +e.getMessage());
//                    if(mContext!=null)
//                        ((NewBroadCastPage) mContext).SocialMediaPost();
//                    return;
//                }
//                try {
//                    thumbnailUploadRequest(ImgName);
//                } catch (JSONException e1) {
//                    e1.printStackTrace();
//                }
    }

//    public void thumbnailUploadRequest(String ImgName) throws JSONException {
//        if (CameraActivity.isBroadcastStarted & mContext!=null) {
//            String imagePath = "event/mobile/" + ImgName + ".jpg";
//            List<NameValuePair> pairs = new ArrayList<>();
//            pairs.add(new BasicNameValuePair("imagePath", imagePath));
//            pairs.add(new BasicNameValuePair("streamId", ((CameraActivity) mContext).getStreamId()));
//            pairs.add(new BasicNameValuePair("bucket", "instalively.data"));
//
//            AppLibrary.log_d(TAG, "image path is:- " + imagePath);
//            AppLibrary.log_d(TAG, "stream Id is:- " + ((CameraActivity) mContext).getStreamId());
//            RequestManager.makePostRequest(mContext, RequestManager.THUMBNAIL_UPLOAD_REQUEST, RequestManager.THUMBNAIL_UPLOAD_RESPONSE, null, pairs, thumbnailUploadCallback);
//        }
//    }


    private RequestManager.OnRequestFinishCallback thumbnailUploadCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            if (success) {
                JSONObject object = (JSONObject) response;
                try {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        String followResponse = object.getString("value");
                        AppLibrary.log_d(TAG, "Thumbnail Upload Success, Response:- " + followResponse);
                    } else {
                        //do nothing
                        AppLibrary.log_d(TAG, "Thumbnail Upload Error, Response:- " + object.getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                //request error
            }

//            if(mContext!=null) //Immaterial of the response status
//                ((CameraActivity) mContext).SocialMediaPost(); //Signal that we're ready to post on social media
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

    };

    private void setupOutputDirectoryForRecordedFile() {
        //ToDo: Make this cleaner!
        //Search for external SD card and check if it is writable
        //If no writable external SD card is found get Movies directory from getExternalStoragePublicDirectory
//        File outputDirectory;
//        String foundDirPath = System.getenv("SECONDARY_STORAGE");
//        if (foundDirPath != null && !foundDirPath.isEmpty()) {
//            outputDirectory = new File(foundDirPath);
//            if (outputDirectory != null && outputDirectory.exists()) {
//                foundDirPath += "/Movies/InstaLively";
//                outputDirectory = new File(foundDirPath);
//                outputDirectory.mkdirs();
//                if (!outputDirectory.exists() || !outputDirectory.canWrite())
//                    foundDirPath = "";
//            } else
//                foundDirPath = "";
//        }
//        if (foundDirPath == null || foundDirPath.isEmpty()) {
//            outputDirectory = new File("/mnt/");
//            if (outputDirectory != null && outputDirectory.exists() && outputDirectory.isDirectory()) {
//                String[] dirList = outputDirectory.list();
//                for (int i = 0; i < dirList.length; i++) {
//                    String dir = dirList[i].toLowerCase();
//                    if (((dir.contains("sd") && dir.contains("card")) ||
//                            (dir.contains("sd") && dir.contains("ext")) ||
//                            (dir.contains("ext") && dir.contains("card"))) &&
//                            dir != "sdcard0") { //sdcard0 is phone storage, we can get that path in later conditions
//                        File checkDir = new File(outputDirectory.getPath() + "/" + dirList[i]);
//                        if (checkDir != null && checkDir.canWrite()) { //This check not fool proof
//                            foundDirPath = outputDirectory.getPath() + "/" + dirList[i] + "/Movies/InstaLively";
//                            outputDirectory = new File(foundDirPath);
//                            outputDirectory.mkdirs();
//                            if (!outputDirectory.exists() || !outputDirectory.canWrite())
//                                foundDirPath = "";
//                            else
//                                break;
//                        }
//                    }
//                }
//            }
//        }
//        if (foundDirPath == null || foundDirPath.isEmpty())
//            foundDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath() + "/InstaLively";
//        outputDirectory = new File(foundDirPath);
//        outputDirectory.mkdirs();
//        if (!outputDirectory.exists() || !outputDirectory.canWrite())
//            foundDirPath = "";
//        if (foundDirPath == null || foundDirPath.isEmpty()) {
//            foundDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/InstaLively";
//            outputDirectory = new File(foundDirPath);
//            outputDirectory.mkdirs();
//        }
//        mOutputDir = outputDirectory.getPath();

        String mediaStoragePath = AppLibrary.getCreatedMediaDirectory(MasterClass.getGlobalContext());
        File mediaDir = new File(mediaStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();

        mOutputDir = mediaStoragePath;
    }

//    private void checkAndRenameRecordedFile(String recordingFilePath) {
//        if(mContext==null) return;
//        String eventName = ((CameraActivity) mContext).getEventName();
//        String eventId = ((CameraActivity) mContext).getEventId();
//        if (eventName.isEmpty() || eventId.isEmpty() || eventName == null || eventId == null)
//            return;
//
//        if (recordingFilePath != null && !recordingFilePath.isEmpty()) {
//            if (!recordingFilePath.contains(eventId)) return;
//            File recordedFile = new File(recordingFilePath);
//            if (recordedFile.exists()) {
////                String fileName = "ThisIsATestNameLetsSeeIfThisWorksOrNotDunnoWhatToDo";
////                String fileName = "123456789123456789123456789123456789";
//                String fileName = recordingFilePath;
//                eventName = eventName.replaceAll("[^a-zA-Z0-9.-]", "_");
//                fileName = fileName.replaceAll(eventId, eventName);
//                if (recordedFile.renameTo(new File(fileName)))
//                    mLastRecordedFilepath = fileName;
////                while(!recordedFile.renameTo(new File(recordedFile.getParentFile(), fileName+".mp4")) && fileName.length()>2) {
////                    fileName = fileName.substring(0, fileName.length()-2);
////                }
////                AppLibrary.log_d(TAG, "File renamed:: " + recordedFile.getParentFile().getPath()+"/"+fileName+".mp4");
//            }
//        }
//    }

    public String getLastRecordedFilepath() {
        if (mLastRecordedFilepath != null && !mLastRecordedFilepath.isEmpty()) {
            File file = new File(mLastRecordedFilepath);
            if (file != null && file.exists()) {
                return mLastRecordedFilepath;
            }
        }
        return "";
    }

    public String getRecordingDirectory() {
        return this.mOutputDir;
    }

    @Override
    public void setCameraControlCallback(StreamingActionControllerKitKat.CameraControlCallback cameraControlCallback) {

    }

    @Override
    public void setFilterControlCallback(StreamingActionControllerKitKat.FilterControlCallback filterControlCallback) {

    }

    @Override
    public void setDoubleShotCallback(CameraShotCallback cameraShotCallback) {

    }

    public RecordingMuxer getRecordingMuxer() {
        return this.mRecordingMuxer;
    }

    private boolean prepareForPlayback() {
        if (mSourceMediaFile == null) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Not able to find any video file!", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
        try {
            if(mMoviePlayer == null) {
                mMoviePlayer = new MoviePlayer(mSourceMediaFile, mSurfaceTexture, mRenderTargetType, mContext);
            }
            mMoviePlayer.setFrameReceiver(this);
            mMoviePlayer.setSurfaceTexture(mSurfaceTexture);
            mMoviePlayer.setRenderTargetType(mRenderTargetType);
            mMoviePlayer.resetCounters();

            mRenderer.setCameraPreviewSize(mMoviePlayer.getVideoWidth(), mMoviePlayer.getVideoHeight());
            mRenderer.setRenderParams();
        } catch (Exception ioe) {
            Log.e(TAG, "Unable to play movie", ioe);
//            mDecoderSurface.release();
//            playbackStopped();
            closePlayer();
            return false;
        }
        return true;
    }

    public void toggleVideoPlaybackSpeed(int speedState) {
        mMoviePlayer.requestTogglePlaybackSpeed(speedState);
    }

    private boolean startPlayback() {
        if(mPlayTask == null) {
            mPlayTask = new MoviePlayer.PlayTask(mMoviePlayer, this);
        }
        if((mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) > 0){
            mPlayTask.setLoopMode(false);
        } else {
            mPlayTask.setLoopMode(true);
        }
        mPlayTask.execute();
        return true;
    }

    public void setSurfaceTexture(InputSurface surface) {
//        mSurfaceTexture = surfaceTexture;
//        surfaceTexture.setOnFrameAvailableListener(this);
        mDecoderSurface = surface;
//        initDecoder(mDecoderSurface);
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
//        mSurfaceTexture = surfaceTexture;
//        surfaceTexture.setOnFrameAvailableListener(this);

        mSurfaceTexture = surfaceTexture;
//        mDecoderSurface = new InputSurface(decoderSurface);
//        initDecoder(mDecoderSurface);
    }

    @Override
    public void playbackStarted(InputSurface surface) {
        setSurfaceTexture(surface);
        startPlayback();
        if ((mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) > 0) {
            startPublishingStream();
            startRecording();
        }
    }

    @Override
    public void playbackStarted(SurfaceTexture surfaceTexture) {
        if (!canPlay && (mRenderTargetType & MoviePlayer.RENDER_TARGET_DISPLAY) > 0) return; //Handles case when shareMediaFragment is active and encoding is completed before pressing home button

        setSurfaceTexture(surfaceTexture);

        boolean preparePlaybackSuccessful =
                prepareForPlayback(); //Prepare MediaExtractor and validate input video source

        if(!preparePlaybackSuccessful) {
            Log.w(TAG, "prepare Playback NOT successful");
            return;
        }
        if ((mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) > 0) {
            startPublishingStream(); // Set up Muxer and get ready to receive frames.
            startRecording(); // Start Muxer thread to receive frames
        }
        startPlayback();
        //Extract -> decode -> render [-> encode] loop in a separate Thread
        if ((mRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) > 0 && mVideoEventListener != null)
            mVideoEventListener.onRecordingStarted();
    }

    @Override
    public void requestRender(boolean updateTs) {
        mRenderer.requestRender(updateTs);
    }

    @Override
    public void requestRender(boolean updateFrame, long timeStampNs) {
        mRenderer.requestRender(updateFrame, timeStampNs);
    }

    @Override
    public void setFrameTimestamp(long timeStampNs) {
        mRenderer.setFrameTimestamp(timeStampNs);
    }

    @Override
    public void playerReady() {
        requestPlayback = false;
        startVideoRendering();
    }

    public void actionToggle() {
        //  this will start/stop streaming.
        actionPublish();
    }

    public void startPublishingStream() {
        if(VERBOSE) {
            Log.d(TAG, "startPublishingStream");
        }
        if (!isPublishing()) {
            actionToggle();
        }
    }

    private void stopPublishingStream() {
        if(VERBOSE) {
            Log.d(TAG, "stopPublishingStream");
        }
        if (isPublishing()) {
            actionToggle();
        }
    }

    public void playbackStopped() {
        Log.d(TAG, "Playback stopped");
        if(mRenderTargetType > 0) {
            playerReady();
        }
    }

    @Override
    public void playbackStopped(int renderTargetType, boolean videoSuccess) {
        mPrevRenderTargetType = renderTargetType;
        mRenderer.setPlaybackStopped(true);
        if((mPrevRenderTargetType & MoviePlayer.RENDER_TARGET_VIDEO) > 0) {
            stopPublishingStream();
            // Do the following only if the MoviePlayer stopped for reasons other than
            // requestStartVideoRendering (called by the user).
            if (!requestPlayback) {
                Log.d(TAG, "Setting render target type to display");
                setRenderTargetType(MoviePlayer.RENDER_TARGET_DISPLAY);
                stopAndCleanupPlayer();
                if(canPlay) {
                    playerReady();
                }
            }
            if(videoSuccess) {
                mVideoLifecycleListener.videoSaved();
            }
        }
    }

    @Override
    public void closePlayer() {
        Log.d(TAG, "Closing player");
        mVideoLifecycleListener.closeVideoPlayback();
    }
}
