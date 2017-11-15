package com.pulseapp.android.broadcast;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.org.rajawali3d.Object3D;
import android.org.rajawali3d.animation.Animation;
import android.org.rajawali3d.animation.Animation3D;
import android.org.rajawali3d.animation.EllipticalOrbitAnimation3D;
import android.org.rajawali3d.animation.RotateOnAxisAnimation;
import android.org.rajawali3d.animation.TranslateAnimation3D;
import android.org.rajawali3d.lights.ALight;
import android.org.rajawali3d.lights.DirectionalLight;
import android.org.rajawali3d.lights.SpotLight;
import android.org.rajawali3d.loader.LoaderOBJ;
import android.org.rajawali3d.materials.Material;
import android.org.rajawali3d.materials.methods.DiffuseMethod;
import android.org.rajawali3d.materials.methods.SpecularMethod;
import android.org.rajawali3d.math.Matrix4;
import android.org.rajawali3d.math.vector.Vector3;
import android.org.rajawali3d.primitives.Cube;
import android.org.rajawali3d.primitives.Torus;
import android.org.rajawali3d.renderer.RajawaliRenderer;
import android.org.rajawali3d.util.RajLog;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextPaint;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.gles.Drawable2d;
import com.pulseapp.android.gles.OpenGLRenderer;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.stickers.ThemeAsset;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import com.instalively.android.stickers.StickerModel;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;

/**
 * Created by Karthik on 29-Feb-2016
 */

@SuppressLint("LongLogTag")
@TargetApi(18)
public class StreamingActionControllerKitKat extends StreamingActionController implements
        SurfaceTexture.OnFrameAvailableListener,
        OnItemSelectedListener,
        TextureMovieEncoder.Callback,
        VideoEncoderCore.Callback,
        RecordingMuxer.Callback,
        CameraCapture.CameraCaptureCallback {
    protected static final String TAG = "StreamingActionControllerKitKat";
    private static final boolean VERBOSE = false;
    private static final boolean RECORD_STREAM = true;
    private static final boolean FLIP_FRONT_CAMERA = false; // false : WYSIWYG
    public static boolean LIVE_STREAM = false;
    private final GestureDetector mGestureListener;
    private final ScaleGestureDetector mScaleListener;
    private final boolean OFFSCREEN_ENABLED = false;
    private int mEncoderWidth;
    private int mEncoderHeight;
    private GLSurfaceView preview;
    //    private TimeSync timeSync;
    private CameraHandler mCameraHandler;
    private CameraSurfaceRenderer mRenderer;
    private volatile SurfaceTexture mSurfaceTexture = null; //Handle with care
    private long lastSentVideoTS = 0;
    private long lastSentAudioTS = 0;
    private boolean isCameraPaused = false;
    private boolean doAudioCheck = false;
    private int audioCorrectionCounter = 0;
    public long pausedAudioDuration = 0;
    private RecordingMuxer mRecordingMuxer = null;
    private String mOutputDir = "";
    private static String mLastRecordedFilepath = "";
    private boolean recordingStarted = false;
    private boolean sendFramesToRecorder = false;
    private MediaActionSound mSound = null;
    private TransferObserver upload;
    private int videoBitrate = 0;
    private CameraHandlerThread mHandlerThread = null;
    private CameraHandlerThread mSensorThread = null;
    private Handler mSensorHandler;
    public long AudioStartTimestampDelta = 0;
    private long tempPauseVideoTS = 0;
    private int rtmpErrorCounter = 0;
    private boolean isError = false;

    /*
    Thread for stopping and destroying RtmpMuxer
     */
    private CameraHandlerThread mRtmpMuxerAsyncStartStopThread = null;


    //Audio-only mode related
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
                    Camera.Parameters params = camera.getParameters();
                    if (params != null) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        camera.setParameters(params);
                    }
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
    private BarcodeDetector barcodeDetector;
    private boolean BARCODE_MODE_ENABLED = false;
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
    private String mThumbnailPath;
    private String mCaptureImagePath;
    private int mFilterTexture;
    private int mCremaTexture;

    private boolean mDoubleShotOn = false;
    private int mDoubleShotMode;

    private int DOUBLE_SHOT_MODE_BACK = 0;
    private int DOUBLE_SHOT_MODE_FRONT = 1;
    private int DOUBLE_SHOT_MODE_COUNT = 2;

    CameraShotCallback mCameraShotCallback;
    CameraControlCallback mCameraControlCallback;
    FilterControlCallback mFilterControlCallback;

    FaceLandmarkSet mDetectedFaceLandmarks;

    private static final int FILTER_TYPE_FACE_LANDMARK_POS = 1;
    private static final int FILTER_TYPE_FACE_BOUNDS_POS = 2;
    public static final Object mRecorderLock = new Object();
    private boolean mCameraChanged;
    private volatile boolean mStopped;

    @Override
    public void processCaptureFrame() {
        if (mCaptureResultHandler != null) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_HANDLE_CAPTURE));
        }
    }

    @Override
    public void processCaptureFrame(int tag) {
        if (mCaptureResultHandler != null) {
            mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_HANDLE_CAPTURE_FILTER, tag, 0));
        }
    }



    public void handleCaptureFrame() {
        if (VERBOSE)
            Log.d(TAG, "capture mode is " + mCaptureMode);
        Bitmap bitmap = mCapturedBitmapProducerQueue.poll();
        if (VERBOSE)
            Log.d(TAG, "Camera Capture callback processCaptureFrame with bitmap " + bitmap);

        if (bitmap == null) {
            if (VERBOSE)
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
                    if (VERBOSE)
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
            if (VERBOSE)
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
                        if (VERBOSE)
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
                if (VERBOSE)
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

    public void enableDoubleShot(boolean enable) {
        mDoubleShotOn = enable;
        mDoubleShotMode = DOUBLE_SHOT_MODE_BACK;
    }

    public void set3DPosX(float v) {
        mRenderer.setPosX(v);
    }

    public void set3DPosY(float v) {
        mRenderer.setPosY(v);
    }

    public void set3DPosZ(float v) {
        mRenderer.setPosZ(v);
    }

    public void signalSwapCamera() {
        mSettings.swapCamera(true);
    }

    public int decrementFilter() {
        mRenderer.decrementFilter();
        return -1;//// TODO: 6/6/16
    }

    public int incrementFilter() {
        mRenderer.incrementFilter();
        return -1;//// TODO: 6/6/16
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (mHandlerThread != null)
            mHandlerThread.setSurfaceTexture(surfaceTexture);
    }

    public interface CameraControlCallback {
        void onSwitchCamera();

        void afterSwitchCamera();
    }

    public interface FilterControlCallback {
        void onHandleFilterThumbnail(Bitmap bitmap, int filter);
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

        CustomHandlerThread(StreamingActionControllerKitKat controller, String tag) {
            super(tag);
            start();
            mHandler = new CustomHandler(controller, getLooper());
        }

        public Handler getHandler() {
            return mHandler;
        }
    }


    class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
        @Override
        public Tracker<Barcode> create(Barcode barcode) {
            return new MyBarcodeTracker();
        }
    }

    class MyBarcodeTracker extends Tracker<Barcode> {
        @Override
        public void onUpdate(Detector.Detections<Barcode> detectionResults, Barcode barcode) {
            // Access detected barcode values
            if (detectionResults.getDetectedItems().size() > 0) {
                Log.d(TAG, "Detected barcode. Format: " + barcode.format + " value: " + barcode.displayValue);
            }
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
        private WeakReference<StreamingActionControllerKitKat> mCameraCaptureWeakRef;

        public CustomHandler(StreamingActionControllerKitKat controller) {
            mCameraCaptureWeakRef = new WeakReference<StreamingActionControllerKitKat>(controller);
        }

        public CustomHandler(StreamingActionControllerKitKat controller, Looper looper) {
            super(looper);
            mCameraCaptureWeakRef = new WeakReference<StreamingActionControllerKitKat>(controller);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            StreamingActionControllerKitKat controller = mCameraCaptureWeakRef.get();
            if (controller == null) {
                Log.w(TAG, "CameraCapture handleMessage: cameraCapturer is null");
                return;
            }

            switch (what) {
                case MSG_PROCESS_FRAME:
                    controller.handleProcessFaceFrame((Bitmap) obj);
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

    private void handleProcessFaceFrame(Bitmap bitmap) {
        if (mFaceDetector != null) {
            if (mPotentialFaceBitmap != null) {
                // Release it to the queue before losing its reference
                mCapturedBitmapConsumerQueue.offer(bitmap);
            }
            mPotentialFaceBitmap = bitmap;
            if (mFaceDetector.isOperational()) {
                Log.d(TAG, "onPreviewFrame :: sending to Face Detector");
                try {
                    mFaceDetector.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());

                    if (BARCODE_MODE_ENABLED && barcodeDetector != null)
                        barcodeDetector.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());
                } catch (Exception e) {
                    mCapturedBitmapConsumerQueue.offer(bitmap);
                    e.printStackTrace();
                }
            } else {
                if (BARCODE_MODE_ENABLED && barcodeDetector != null)
                    barcodeDetector.receiveFrame(new Frame.Builder().setBitmap(bitmap).build());

                mCapturedBitmapConsumerQueue.offer(bitmap);
                Log.d(TAG, "onPreviewFrame :: Face Detector seems to have been released");
            }
        } else {
            mCapturedBitmapConsumerQueue.offer(bitmap);
        }
    }

    private void handleProcessFilterImage(Bitmap bitmap, int filter) {
        if (VERBOSE) Log.d(TAG, "handleProcessFilterImage for filter " + filter);
        mFilterControlCallback.onHandleFilterThumbnail(bitmap, filter);
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

    private void setupFaceAndBarcodeDetection() {
        Log.d(TAG, "setupFaceAndBarcodeDetection :: Camera preview width : " + mCameraPreviewWidth + " height : " + mCameraPreviewHeight);

        Context context = mContext.getApplicationContext();

        if (context == null)
            return;

        mDetectedFaceLandmarks = new FaceLandmarkSet();

        mFaceDetector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setProminentFaceOnly(true)
                .build();

        mFaceDetector.setProcessor(new LargestFaceFocusingProcessor(mFaceDetector, new FaceTracker()));

        barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory();
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

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

    private void extractFaceLandmarks(Face face) {
        int type;
        int newX;
        int newY;
        float XscaleFactor = (float) mRenderer.mPreviewWidth / mCameraCapturer.mOutgoingWidth;
        float YscaleFactor = (float) mRenderer.mPreviewHeight / mCameraCapturer.mOutgoingHeight;

        if (!face.getLandmarks().isEmpty()) {
            mDetectedFaceLandmarks.updateLandmarks(face.getLandmarks());

            for (int i = 0; i < face.getLandmarks().size(); i++) {
                Log.d(TAG, "Number of landmarks detected is " + face.getLandmarks().size());
                Landmark landMark = face.getLandmarks().get(i);
                type = landMark.getType();
                switch (type) {
                    case Landmark.BOTTOM_MOUTH:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "BottomMouth Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setBottomMouthPosition(newX, newY);
                        break;
                    case Landmark.LEFT_CHEEK:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "LeftCheek Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setLeftCheekPosition(newX, newY);
                        break;
                    case Landmark.LEFT_EAR:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "LeftEar Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setLeftEarPosition(newX, newY);
                        break;
                    case Landmark.LEFT_EYE:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "LeftEye Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setLeftEyePosition(newX, newY);
                        break;
                    case Landmark.LEFT_MOUTH:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "LeftMouth Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setLeftMouthPosition(newX, newY);
                        break;
                    case Landmark.NOSE_BASE:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
/*                        synchronized (pos) {*/
/*                        mRenderer.setPosZ(0.15f);
                        mRenderer.setPosX((float) newX / mRenderer.mPreviewWidth);
                        mRenderer.setPosY((float)newY / mRenderer.mPreviewHeight);*/
/*                        }*/
                        Log.d(TAG, "Nose Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setNosePosition(newX, newY);
                        break;
                    case Landmark.RIGHT_CHEEK:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "RightCheek Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setRightCheekPosition(newX, newY);
                        break;
                    case Landmark.RIGHT_EAR:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "RightEar Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setRightEarPosition(newX, newY);
                        break;
                    case Landmark.RIGHT_EYE:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "RightEye Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setRightEyePosition(newX, newY);
                        break;
                    case Landmark.RIGHT_MOUTH:
                        newX = (int) (XscaleFactor * landMark.getPosition().x);
                        newY = (int) (YscaleFactor * landMark.getPosition().y);
                        Log.d(TAG, "RightMouth Landmark x: " + newX + " y: " + newY);
                        mFaceUpdateListener.setRightMouthPosition(newX, newY);
                        break;
                    default:
                        break;
                }
            }
            mFaceUpdateListener.updateFacePos();
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
        public void onUpdate(Detector.Detections<Face> detections, final Face face) {

            Log.d(TAG, "CameraSource tracker posting update to handler");

            int faceWidth = (int) ((float) mRenderer.mPreviewWidth / mCameraCapturer.mOutgoingWidth * face.getWidth());
            int faceHeight = (int) ((float) mRenderer.mPreviewHeight / mCameraCapturer.mOutgoingHeight * face.getHeight());
            int newX = (int) ((float) mRenderer.mPreviewWidth * (face.getPosition().x / mCameraCapturer.mOutgoingWidth));
            int newY = (int) ((float) mRenderer.mPreviewHeight * (face.getPosition().y / mCameraCapturer.mOutgoingHeight));
            mFaceUpdateListener.setFaceBounds(newX, newY, faceWidth, faceHeight);
            mFaceUpdateListener.updateFacePos(face.getEulerY(), face.getEulerZ(), face.getIsSmilingProbability());

            mRenderer.setRotY(face.getEulerY());
            mRenderer.setRotZ(face.getEulerZ());
            mRenderer.setPosZ((float) face.getHeight() / mRenderer.mPreviewHeight * 2);
            mRenderer.setPosX((float) (newX + faceWidth / 2) / mRenderer.mPreviewWidth);
            mRenderer.setPosY((float) (newY + faceHeight / 2) / mRenderer.mPreviewHeight);

            if (mCameraHandler != null) {
                mCameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        extractFaceLandmarks(face);
                    }
                });
            }

            if (((mCaptureMode & CAPTURE_MODE_THUMBNAIL) > 0) && (mThumbnailFaceBitmap == null) && (mPotentialFaceBitmap != null)) {
                mThumbnailFaceBitmap = mPotentialFaceBitmap.copy(Bitmap.Config.ARGB_8888, false);
                mCapturedBitmapConsumerQueue.offer(mPotentialFaceBitmap);
                mPotentialFaceBitmap = null;
                if (mCaptureResultHandler != null) {
                    mCaptureResultHandler.sendMessage(mCaptureResultHandler.obtainMessage(MSG_PROCESS_THUMBNAIL));
                }
            }
        }

    }

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
                if (captureImageRequested) {
                    takePicture();
                    captureImageRequested = false;
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
                                    if (captureImageRequested) {
                                        takePicture();
                                        captureImageRequested = false;
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

    int[] focusIntensity = new int[]{800};

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        int[] mLatestUpEvent = new int[2];
        int[] mPreviousDownEvent = null;
        int doubleTapDistance = AppLibrary.convertDpToPixels(mContext, 5);
        private final int SWIPE_THRESHOLD = 70;
        private final int SWIPE_VELOCITY_THRESHOLD = 30;

        @Override
        public boolean onDown(MotionEvent e) {
            if (publishing) { //While recording, tap on the screen to focus
                int x = (int) e.getX();
                int y = (int) e.getY();

                if (mPreviousDownEvent != null) { //Check for double tap while recording
                    boolean isDoubleTapped = checkifDoubleTapped(x, y, (int) System.currentTimeMillis());
                    mPreviousDownEvent[0] = x;
                    mPreviousDownEvent[1] = y;
                    mPreviousDownEvent[2] = (int) System.currentTimeMillis();
                    if (isDoubleTapped) return false;
                } else {
                    mPreviousDownEvent = new int[3];
                    mPreviousDownEvent[0] = x;
                    mPreviousDownEvent[1] = y;
                    mPreviousDownEvent[2] = (int) System.currentTimeMillis();
                }

                Log.d(TAG, "auto focus TAP received at " + x + " " + y);
                MasterClass.getEventBus().post(new BroadCastSignals.FocusSignal(x, y));

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
            }
            return false;
        }

        private boolean checkifDoubleTapped(int x, int y, int time) {
            int DoubleTapTime = 500;
            int timeDifference = time - mPreviousDownEvent[2];

            if (timeDifference < DoubleTapTime) {
                int X = mPreviousDownEvent[0];
                int Y = mPreviousDownEvent[1];
                Rect rect = new Rect(X - doubleTapDistance * 2, Y - doubleTapDistance * 2, X + doubleTapDistance * 2, Y + doubleTapDistance * 2);
                //double tap distance*2 'cos we're now calculating difference in down events, and previously it was between down/up events

                if (rect.contains(x, y)) {
                    Log.d(TAG, "real double tap while recording");
                    restartController(new BroadCastSignals.RestartBroadCastSignal(null, true));
                    return true;
                } else {
                    Log.d(TAG, "fake double tap while recording");
                }
            }

            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            Log.d(TAG, "auto focus TAP received at " + x + " " + y);
            MasterClass.getEventBus().post(new BroadCastSignals.FocusSignal(x, y));

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
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD && !isPublishing()) {
                        if (diffX > 0) {
//                            onSwipeRight();
                        } else {
//                            result = true;
//                            onSwipeLeft();
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD && !isPublishing()) {
                    if (diffY > 0) {
                        result = true;
                        toggleDoubleShot(false);
                    } else {
                        result = true;
                        toggleDoubleShot(true);
                    }
                }

            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, " replace try catch " + exception);
            }
            return result;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mLatestUpEvent[0] = (int) e.getRawX();
            mLatestUpEvent[1] = (int) e.getRawY();
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            return false;
//        }

        @Override
        public boolean onDoubleTap(MotionEvent e) { //e gives the second down event of the double tap. And this callback gets called on the 2nd down event

            if (mLatestUpEvent != null) {
                int x = mLatestUpEvent[0];
                int y = mLatestUpEvent[1];
                Rect rect = new Rect(x - doubleTapDistance, y - doubleTapDistance, x + doubleTapDistance, y + doubleTapDistance);

                if (rect.contains((int) e.getRawX(), (int) e.getRawY())) {
                    Log.d(TAG, "real double tap at :" + e.getRawX() + " " + e.getRawY());
                    restartController(new BroadCastSignals.RestartBroadCastSignal(null, true));
                } else {
                    Log.d(TAG, "fake double tap at :" + e.getRawX() + " " + e.getRawY());
                    return false;
                }
            } else {
                restartController(new BroadCastSignals.RestartBroadCastSignal(null, true));
            }
            return true;
        }
    }

    public void toggleDoubleShot(boolean enable) {
        MasterClass.getEventBus().post(new BroadCastSignals.DoubleShotSignal(enable));
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

    /* user actions */
    public Activity mContext;

    public StreamingActionControllerKitKat(Activity context, GLSurfaceView sv, TextView textview) {
        this.textMessage = textview;
        this.mContext = context;
        this.publishing = false;
        this.preview = sv;
        mCameraChanged = false;
//        this.timeSync = new TimeSync();
        this.mSound = new MediaActionSound();
        mCameraLock = new ReentrantLock();

        preferences = context.getSharedPreferences(AppLibrary.BroadCastSettings, Context.MODE_PRIVATE);
        editor = preferences.edit();
        BroadCastSettings.initialize(context);
        mSettings = BroadCastSettings.HD_QUALITY_RECORDING;

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

//        AppLibrary.log_i("Camera", "StreamingActionControllerKitKat pre-starting Camera");
        if (!preferences.contains(SharedPreferencesIdentity + SPI_PreviewWidth + 0)) {
            configureCameraPreferences();
        } else {
            AppLibrary.log_i("Camera", "Camera sizes from preferences....");

            for (int i = 0; i < 3; i++) {
                CameraProperties cameraProperties = getCameraPropertiesFromSharedPreferences(i, false);
                if (cameraProperties != null) {
                    Log.d(TAG, "Properties for camera " + i + " retrieved from Shared preferences");
                    mCameraProperties.put(i, cameraProperties);
                }
            }
        }

        if (mCameraProperties != null && mCameraProperties.get(0) != null)  //QueryCamera properties only if cameraProperties object has been set
            queryCameraProperties(0);

        preview.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder,
                mEncoderWidth,
                mEncoderHeight,
                mSettings.videoBitRate * 1024,
                mCameraPreviewFrameRate, mContext);
        mRenderer.setTargetView(preview);
        mRenderer.setController(this);
        mRenderer.setContext(mContext);
        preview.setRenderer(mRenderer);
        preview.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        if (OFFSCREEN_ENABLED) {
            mCameraCapturer = new CameraCapture(this,mContext);
            mCameraCapturer.setSwappable((Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1));
        }
        mDummyRenderer = new DummySurfaceRenderer();
        mCaptureResultHandlerThread = new CustomHandlerThread(this, "CaptureResultHandlerThread");
        mCaptureResultHandler = mCaptureResultHandlerThread.getHandler();

        mRenderer.setCameraCapturer(mCameraCapturer);
        mRenderer.setDummyRenderer(mDummyRenderer);

        mCurrentCaptureFilter = 0;

        mSensorThread = new CameraHandlerThread();
        mSensorThread.setPriority(Thread.MIN_PRIORITY);
        mSensorHandler = mSensorThread.getHandler();

        mGestureListener = new GestureDetector(mContext, new CustomGestureListener());
        mScaleListener = new ScaleGestureDetector(mContext, new ScaleDetectorListener());
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (!((CameraActivity) mContext).isScrolling) {
                    return handleOnTouch(event);
                } else
                    return true; //Do nothing
            }
        });

        if (mSmartFocusMode) {
            initiateSensors();
        }

        if (OFFSCREEN_ENABLED) {
            mVisionFeaturesEnabled = true;
        } else {
            mVisionFeaturesEnabled = false;
        }
    }

    private void configureCameraPreferences() {
/*        handleConfigureCameraPreferences();*/
        if (mHandlerThread == null) {
            mHandlerThread = new CameraHandlerThread();
        }
        mHandlerThread.configureCameraPreferences();
    }

    private void handleConfigureCameraPreferences() {
        AppLibrary.log_i("Camera", "Camera opening....");
        Camera mCamera = null;
        try {
            int cameraId = mSettings.getCameraId();
            mCamera = Camera.open(cameraId);
            if (mCamera != null) {  //Paranoia
                boolean status = initCameraProperties(mCamera, cameraId);
                if (status) storeCameraPropertiesToSharedPreferences(cameraId);

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

        } catch (Exception e) {
            AppLibrary.log_i("Camera", "Failed to open Camera");
            e.printStackTrace();
        }
    }

    public boolean handleOnTouch(MotionEvent event) {

        if (event.getPointerCount() < 2) { //For single-tap & double-tap, pass only the relevant events to avoid errors
            MotionEvent mEvent = event;
            mEvent.setLocation(event.getRawX(), event.getRawY());
            mGestureListener.onTouchEvent(mEvent);
        } else if (publishing) //While recording video, pass all events for tap to focus
            mGestureListener.onTouchEvent(event);

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

        if (!status) {
            Log.d(TAG, "Configuring camera. Screwed up");
            return false; //Something went wrong while initializing properties. Returning silently in the hope for something better than an apocalypse
        }
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
        preview.queueEvent(new Runnable() {
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
            this.videoBitrate = mSettings.videoBitRate;
            String audioChannels = "1";   // #channels : 1 - mono, 2 - stereo
            String audioSamplingRate = Integer.toString(mSettings.audioSamplingRate);
            String audioBitRate = Integer.toString(mSettings.audioBitRate * 1024);

            if (LIVE_STREAM) {
                String url = this.rtmpUrl;

                if (url.startsWith("rtmp://")) {
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
                    }
                }
            }

            if (RECORD_STREAM) {
                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd-HH-mm-ss");
                String fileName = dateFormat.format(date) + "_" + System.currentTimeMillis()
                        + ".mp4";

                fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                mLastRecordedFilepath = this.mOutputDir + "/" + fileName;
                this.recordingStarted = false;
                this.sendFramesToRecorder = false;
                this.mRecordingMuxer = new RecordingMuxer(mLastRecordedFilepath,
                        mEncoderWidth,
                        mEncoderHeight,
                        this.mSettings.videoBitRate * 1024,
                        this.mCameraPreviewFrameRate,
                        this.mSettings.audioSamplingRate,
                        1,
                        this.mSettings.audioBitRate * 1024,
                        this.mContext);
                this.mRecordingMuxer.prepare();
                this.mRecordingMuxer.setCallback(this);
                if (!LIVE_STREAM)
                    startRecording();
            }

            //  AudioCodec
            boolean isWiredHeadsetOn = false;
            try {
                AudioManager am1 = (AudioManager) ((CameraActivity) mContext).getSystemService(Context.AUDIO_SERVICE);
                isWiredHeadsetOn = am1.isWiredHeadsetOn();
                AppLibrary.log_i(TAG, "Wired Headset status is: " + isWiredHeadsetOn);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.audioCodec = new IAudioCodec(Integer.parseInt(audioChannels), Integer.parseInt(audioSamplingRate), Integer.parseInt(audioBitRate), isWiredHeadsetOn);
            this.audioCodec.setCallback(this);
            this.audioCodec.prepare();
            this.audioCodec.start();
            this.preview.queueEvent(new Runnable() {
                @Override
                public void run() {
                    // notify the renderer that we want to change the encoder's state
                    mRenderer.changeRecordingState(true);
                }
            });
            setState("Stop", "live", true, true);

        } catch (Exception e) {
            e.printStackTrace();
            onAudioError(e.toString()); //OnAudioError and onVideoError are the same - Goes back to Dashboard safely
        }
    }

    /* filter and camera */
    synchronized public void startCamera() {
        mZoomLevel = 1.0f;
        mRenderer.setScale(1.0f);
//        mSmartFocusMode = true;

//        mCameraLock.lock();
        try {
            if (this.camera == null) {
                newOpenCameraSync();
//                newOpenCamera();
                if (this.camera == null) {  //Error handled in ResumeView for first time camera opening, and restart controller for swap camera issues
                    Toast.makeText(mContext, "Sorry, unable to open camera. Please try again!", Toast.LENGTH_SHORT).show();
//                    mCameraLock.unlock();
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
//                        if (params.getAntibanding() != null)
//                            params.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
                        params.setRecordingHint(true);
                        params.set("video-size", previewSize.width + "x" + previewSize.height);
                        params.set("preview-size", previewSize.width + "x" + previewSize.height);

                        Log.d(TAG, "Final camera settings - width: " + previewSize.width + " height: " + previewSize.height);
                        camera.setParameters(params);
                    } catch (Exception e) {
                        e.printStackTrace();
//                        mCameraLock.unlock();
                    }
                }

                // Set the preview aspect ratio.
                mCameraPreviewWidth = previewSize.width;
                mCameraPreviewHeight = previewSize.height;
                if (!mCameraChanged) {
                    if (VERBOSE)
                        Log.d(TAG, "setCameraPreviewSize");
                    mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
                    mRenderer.setRenderParams();
//                    mRenderer.prepareCameraCapture();
                } else {
                    if (VERBOSE)
                        Log.d(TAG, "setCameraPreviewSize, just indicating");
                    // Size should be set in frameAvailable fcn, on receiving the first frame
                    // after camera swap
                    mRenderer.indicateCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
                    mCameraChanged = false;
                }

                if (this.mSurfaceTexture != null) {
                    try {
                        this.camera.setPreviewTexture(this.mSurfaceTexture);
//                    this.mSurfaceTexture = null;
                        this.camera.startPreview();
                        mRenderer.setCameraStarted(true);
                    } catch (Exception ioe) {
                        this.mSurfaceTexture = null;
                        onCameraError(ioe.toString());
                        AppLibrary.log_e(TAG, "Camera start preview failed at start Camera()");
//                        mCameraLock.unlock();
                        return;
                    }
                } else {
                    mRenderer.createSurfaceTexture();
                    AppLibrary.log_e(TAG, "Needed to set camera's preview texture at startCamera() but surface texture is null!");
//                    mCameraLock.unlock();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            mCameraLock.unlock();
        }
    }


    private void oldOpenCamera() {
        int cameraId;
        if (mSettings != null)
            cameraId = mSettings.getCameraId();
        else {
            BroadCastSettings.initialize(mContext);
            mSettings = BroadCastSettings.HD_QUALITY_RECORDING;
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

    private void newOpenCameraSync() {
        mHandlerThread.openCameraSync();
    }

    private void newOpenCamera() {
        if (mHandlerThread == null) {
            mHandlerThread = new CameraHandlerThread();
        }

        synchronized (mHandlerThread) {
            mHandlerThread.openCamera();
        }
    }

    public void pauseCameraPreview() {

        try {
            if (camera != null) {
                if (mHandlerThread != null)
                    mHandlerThread.stopCameraPreview();

                if (mSensorHandler != null) {
                    mSensorHandler.removeCallbacksAndMessages(null);
                }
                if (mSmartFocusMode && ((mSettings == null) || ((mSettings.getCameraId() % 2) == 0))) {
                    stopListeningSensors();
                }
            }
            //Don't do it yet. Give the control to Renderer to do it after drawing the last frame
//            mRenderer.trimMemory();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    synchronized public void resumeCameraPreview() {

        if (camera != null) {
            try {
                mZoomLevel = 1.0f;
                mRenderer.setScale(1.0f);
                if (mHandlerThread != null)
                    mHandlerThread.startCameraPreview();
                mRenderer.allocTempResources();

                if (mSmartFocusMode && ((mSettings.getCameraId() % 2) == 0)) {
                    startListeningSensors();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                resumeView();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        synchronized public void stopCameraSync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopCamera();
                    notifyCameraClosed();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                AppLibrary.log_i(TAG, "wait was interrupted");
            }
        }

        public void stopCameraAsync() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopCamera();
                }
            });
        }

        public void startCameraPreview() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    /** @ above line
                     *  E/UncaughtException: java.lang.RuntimeException: startPreview failed
                     at android.hardware.Camera.startPreview(Native Method)
                     at com.pulseapp.android.broadcast.StreamingActionControllerKitKat$CameraHandlerThread$3.run(StreamingActionControllerKitKat.java:2133)
                     at android.os.Handler.handleCallback(Handler.java:733)
                     at android.os.Handler.dispatchMessage(Handler.java:95)
                     at android.os.Looper.loop(Looper.java:136)
                     at android.os.HandlerThread.run(HandlerThread.java:61)
                     */
                    try {
                        camera.setPreviewTexture(mSurfaceTexture);
                        camera.startPreview();
                    } catch (Exception e) {
                    }
                }
            });
        }

        public void stopCameraPreview() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    camera.stopPreview();
                }
            });
        }

        synchronized public void startCameraSync() throws NullPointerException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startCamera(); //This startCamera doesn't start preview, just opens the camera

                    if (camera == null)  //Since starting camera is synchronous, check for camera null object
                        try {
                            throw new Exception("Camera didn't start in ResumeView :(");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                AppLibrary.log_i(TAG, "wait was interrupted");
            }
        }

        public void startCameraAsync() throws NullPointerException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startCamera(); //This startCamera doesn't start preview, just opens the camera

                    if (camera == null)  //Since starting camera is synchronous, check for camera null object
                        try {
                            throw new Exception("Camera didn't start in ResumeView :(");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            });
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        synchronized void notifyCameraClosed() {
            notify();
        }

        void configureCameraPreferences() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleConfigureCameraPreferences();
                }
            });
        }

        // Should be called from camera handler thread.
        void openCameraSync() {
            oldOpenCamera();
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

        public void handleSetSurfaceTextur(SurfaceTexture surfaceTexture) {
            handleSetSurfaceTexture(surfaceTexture);
        }

        synchronized public void setSurfaceTexture(final SurfaceTexture surfaceTexture) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleSetSurfaceTextur(surfaceTexture);
                    notifySurfaceTextureSet();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                AppLibrary.log_i(TAG, "wait was interrupted");
            }
        }

        synchronized private void notifySurfaceTextureSet() {
            notify();
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
        String str2;
        Object[] objArr;
        if (this.camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Display display = this.mContext.getWindowManager().getDefaultDisplay();
            orientationResult = display.getRotation();

            Camera.getCameraInfo((mSettings != null ? mSettings.getCameraId() : 0), info);
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
//                    params.setPictureSize(width, height);
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

            AppLibrary.log_e(TAG, "Colorfilter: " + CameraActivity.PreRecordFilters);

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


/*            if (this.preview != null) {
                this.preview.queueEvent(new Runnable() {
                    @Override
                    public void run() {*/
            // notify the renderer that we want to change the encoder's state
            mDummyRenderer.changeRecordingState(false);
/*                    }
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
            this.publishing = false; // Publishing hould be set to false after stopping the Muxer.
            this.recordingStarted = false;
            this.sendFramesToRecorder = false;
            this.isCameraPaused = false;
            this.doAudioCheck = false;
            this.tempPauseVideoTS = 0;
            this.pausedAudioDuration = 0;
            this.lastSentVideoTS = 0;
            this.lastSentAudioTS = 0;
            this.audioCorrectionCounter = 0;
            this.bitrateReductionsConsecutiveCounter = 0;
            this.continueWithoutBitrateChecks = false;
            this.AudioStartTimestampDelta = 0;
            this.rtmpErrorCounter = 0;


            //For music playback
            if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
                if (this.mMusicPlayer != null) {
                    this.mMusicPlayer.stop();
                    this.mMusicPlayer = null;
                }
            }

            //For RTMP - Temporarily commenting out code
            if (this.rtmp != null) {
//                if (this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning)
//                    AppLibrary.log_e(TAG, "RtmpMuxer stop is already in process!");
//                else {
//                    CameraActivity.status = ((CameraActivity) mContext).mStatus;
//
//                    if (status == CameraActivity.BroadCastStatus.STATE_BROADCAST_LIVE ||
//                            status == CameraActivity.BroadCastStatus.STATE_BROADCAST_STOP)
//                        this.rtmp.stop();
//                    else
//                        this.rtmp.flushStop();
//                }
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
                //Call Stop Broadcast Request
            }
            this.rtmp = null;
        }

        if (mContext != null && LIVE_STREAM) {
            //Removes the RTMP subscriber from NGINX RTMP
        }
    }

    public boolean getRTMPStatus() {
        if (this.rtmp != null)
            return true;
        else
            return false;
    }

    synchronized public void stopCamera() {

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
//            mSmartFocusMode = false;

//            mCameraLock.lock();
            try {
                this.camera.stopPreview();
                mRenderer.setCameraStarted(false);
                this.camera.release();
                this.camera = null;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                mCameraLock.unlock();
            }
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
                            //---> Temporarily commenting out code

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
                    if (publishing && rtmp != null)
                        //Remove the RTMP subscriber from NGINX RTMP. Starting Streaming is done in response of this function

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
    public void onCameraError(String message) {
        isError = true;
        AppLibrary.log_e(TAG, message);
        MasterClass.getEventBus().post(new BroadCastSignals.ErrorSignal());
    }

    /* Codec & Muxer callbacks */
    public void onAudioError(String message) {
        isError = true;
        AppLibrary.log_e(TAG, message);
        asyncStop(message);
        MasterClass.getEventBus().post(new BroadCastSignals.ErrorSignal());
    }

    public void onAudioHeader(byte[] header, int numChannels, int sampleRate, int sampleSize) {
        if (this.rtmp != null)
            this.rtmp.setAudioHeader(header, numChannels, sampleRate, sampleSize);
        if (this.RECORD_STREAM) {
            if (this.mRecordingMuxer != null) {
                RecordingMuxer.MediaFrame aFrame = new RecordingMuxer.MediaFrame();
                aFrame.buffer = header;
                aFrame.type = RecordingMuxer.MediaFrameType.AUDIOHEADER;
                this.mRecordingMuxer.setAudioHeader(aFrame.buffer);
//                this.mRecordingMuxer.postFrame(aFrame); //Default timestamp is 0 if not specified
            }
        }
    }

    public void onAudioFrame(byte[] buffer, long ts) {
//        ts = this.timeSync.processAudio(ts);
        if (lastSentVideoTS <= 3) {  //Don't send audio before sending video SPS & PPS & first frame (as there is a jump from 1st to 2nd frame)
            this.lastSentAudioTS = ts;
            return;
        } else if (AudioStartTimestampDelta == 0) {
            AudioStartTimestampDelta = ts - lastSentVideoTS; //Initial time that audio starts streaming before video, when streaming first starts
            AppLibrary.log_d(TAG, "Setting AudioStartTimeStampDelta as " + AudioStartTimestampDelta + " with lastSentVideoTS as " + lastSentVideoTS);
            return; //Skip the audio frame at the risk of sending 0 timestamp
        }
//
//        if (isCameraPaused) //If camera is paused, don't send audio until the first video frame is sent again
//            return;
//        else if (doAudioCheck) {
//            audioCorrectionCounter++;
//            pausedAudioDuration += ts - AudioStartTimestampDelta - tempPauseVideoTS - (1024000/this.mSettings.audioSamplingRate); //Calculated the cumulative paused AudioDuration to subtract from ts
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
                RecordingMuxer.MediaFrame aFrame = new RecordingMuxer.MediaFrame();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.presentationTimeUs = ts * 1000;
                bufferInfo.size = buffer.length;
                aFrame.bufferInfo = bufferInfo;
                aFrame.buffer = buffer;
                aFrame.timestamp = ts * 1000;
                aFrame.type = RecordingMuxer.MediaFrameType.AUDIO;

                if (VERBOSE)
                    Log.d(TAG, "send audio frame timestamp: " + aFrame.timestamp + "frame: " + aFrame);

                this.mRecordingMuxer.postFrame(aFrame);
            }
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
                    } else
                        doSend = true;
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
                        if (VERBOSE) {
                            Log.d(TAG, "send video frame timestamp: " + ts + "frame: " + vFrame + " type: " + type);
                        }
                    }
                }
            }
        }
    }

    public void onRtmpDecreaseBitrate(final float dropRate) {
        if (!RtmpMuxer.SMART_BITRATE_MODE_ENABLED) return;
        if (!this.isPublishing()) return;
        if (continueWithoutBitrateChecks) return;

        if (this.videoBitrate < THRESHOLD_LOWEST_VIDEO_BITRATE) {
            //---> Temporarily commented out

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
        }

        videoBitrate -= BITRATE_DROP_PER_CYCLE;
        if (videoBitrate < 145)
            videoBitrate = 145; //Handles case of extreme network jitter like 52% drop from bitrate 204

        if (mRenderer != null && (videoBitrate < 700)) {
            mRenderer.changeBitrate(videoBitrate);

            AppLibrary.log_d(TAG, "Conveyed bitrate decrease of " + BITRATE_DROP_PER_CYCLE + " to renderer. New bitrate is " + videoBitrate);

            this.rtmp.pauseBitrateChecks(2000);  //Give the network some time to recover before starting bitrate check timers again
            this.bitrateReductionsConsecutiveCounter++;

            if (this.videoBitrate < THRESHOLD_HIGHEST_VIDEO_BITRATE)
                RtmpMuxer.mIsBitrateUpperThresholdForCurrentSession = false;   //Resets Upper threshold flag since bitrate has decreased. Rare case, but just for safety!
        }
    }

    public void onRtmpIncreaseBitrate() {
        if (!RtmpMuxer.SMART_BITRATE_MODE_ENABLED) return;
        if (!this.isPublishing()) return;
        if (continueWithoutBitrateChecks)
            return;  //Rare possibility as this flag is used when internet is bad, but just in case!

        if (bitrateReductionsConsecutiveCounter > 0) {
            if (bitrateReductionsConsecutiveCounter >= 2)
                THRESHOLD_HIGHEST_VIDEO_BITRATE -= 15;      //If two reductions in a row, then decrease upper threshold
            bitrateReductionsConsecutiveCounter = 0;

            if (THRESHOLD_HIGHEST_VIDEO_BITRATE < 350)   //But don't let the upper threshold fall below 350
                THRESHOLD_HIGHEST_VIDEO_BITRATE = 350;

            BITRATE_DROP_PER_CYCLE -= 5;          //Cumulatively keep decreasing the bitrate drop per cycle of decrease, to help converge to an optimum bandwidth
            BITRATE_INCREASE_PER_CYCLE -= 2;      //Cumulatively keep decreasing the bitrate increase per cycle of decrease, to help converge to an optimum bandwidth

            if (BITRATE_DROP_PER_CYCLE < 20)       //Minimum drop per cycle set as 20
                BITRATE_DROP_PER_CYCLE = 20;

            if (BITRATE_INCREASE_PER_CYCLE < 10)   //Minimum increase per cycle set as 10
                BITRATE_INCREASE_PER_CYCLE = 10;
        }

        if (this.videoBitrate > THRESHOLD_HIGHEST_VIDEO_BITRATE) {
            RtmpMuxer.mIsBitrateUpperThresholdForCurrentSession = true; //Sets bitrate threshold to avoid further timer checks
            if (this.rtmp != null)
                this.rtmp.stopBitrateCheckTimers();

            AppLibrary.log_d(TAG, "Bitrate upper threshold reached. Current bitrate is " + this.videoBitrate);
            return;
        }

        videoBitrate += BITRATE_INCREASE_PER_CYCLE;
        if (mRenderer != null && (videoBitrate > 145)) {
            mRenderer.changeBitrate(videoBitrate);
            AppLibrary.log_d(TAG, "Conveyed bitrate increase of " + BITRATE_INCREASE_PER_CYCLE + " to renderer. New bitrate is " + videoBitrate);

            if (this.videoBitrate > THRESHOLD_LOWEST_VIDEO_BITRATE)
                RtmpMuxer.mIsBitrateLowerThresholdForCurrentSession = false;  //Resets Lower threshold flag if increased bitrate is greater than threshold
        }
    }

    public void onRtmpError(String message) {
// --> Temporarily commented

//        AppLibrary.log_e(TAG, message);
//        if ((this.mRtmpMuxerAsyncStartStopThread != null && this.mRtmpMuxerAsyncStartStopThread.mIsRunning) ||
//                ((CameraActivity) mContext).mStatus == CameraActivity.BroadCastStatus.STATE_RECORD_WITH_UPLOAD_LATER ||
//                ((CameraActivity) mContext).mStatus == CameraActivity.BroadCastStatus.STATE_TRANSITIONINNG_RECORD_WITH_UPLOAD_LATER)
//            return;
//        if (this.rtmp != null) {
//            rtmpErrorCounter++;
//            AppLibrary.log_e(TAG, "rtmpErrorCounter is " + rtmpErrorCounter);
//
//            this.rtmp.stopBitrateChecks(); //Stopping bitrate checks
//            this.rtmp.forceCloseConnection();
//            if(rtmpErrorCounter>4 || message.contains("QUIT")){ //Recover from RTMP errors atleast 5 times, else show upload popup
//                this.rtmp = null; //Make rtmp null
//                ((CameraActivity) mContext).handleOnRtmpErrorDelayed(); //Upload popup
//                //asyncStop(message); //Do not stop, it can lead to crash maybe?
//            } else {
//                asyncStartRtmpPublishing();
//            }
//        }
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
            synchronized (mRecorderLock) {
                mRecorderLock.notifyAll();
            }
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
        this.textMessage.setTextColor(Color.parseColor("#FF0000"));
        this.holder = this.preview.getHolder();
        this.holder.addCallback(this);
        this.handler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle.containsKey("Message") && StreamingActionControllerKitKat.this.textMessage != null) {
                    StreamingActionControllerKitKat.this.textMessage.setText(bundle.getString("Message"));
                }
                if (bundle.containsKey("Screen") && StreamingActionControllerKitKat.this.mContext != null) {
                    if (bundle.getBoolean("Screen")) {
                        StreamingActionControllerKitKat.this.mContext.getWindow().addFlags(128);
                    } else {
                        StreamingActionControllerKitKat.this.mContext.getWindow().clearFlags(128);
                    }
                }
                if (bundle.containsKey("Bytes") && StreamingActionControllerKitKat.this.textMessage != null) {
                    StreamingActionControllerKitKat.this.textMessage.setText(String.format("audio: %dK, video: %dK", new Object[]{Long.valueOf(StreamingActionControllerKitKat.this.audioBytes / 1024), Long.valueOf(StreamingActionControllerKitKat.this.videoBytes / 1024)}));
                }
            }
        };

        mRenderer.flagForReset();
//        mSmartFocusMode = true;
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
        AppLibrary.log_d(TAG, "onResume -- acquiring camera");
        if (this.preview == null) return;

        // Only create this thread and handler if it hasn't been created before.
        // This has been done, because there are multiple entries into this function.
        if (mCaptureResultHandlerThread == null) {
            mCaptureResultHandlerThread = new CustomHandlerThread(this, "CaptureResultHandlerThread");
            mCaptureResultHandler = mCaptureResultHandlerThread.getHandler();
        }

        waitThread();
        if (mHandlerThread == null) {
            mHandlerThread = new CameraHandlerThread();
        }
        mHandlerThread.startCameraAsync();

/*        startCamera(); //This startCamera doesn't start preview, just opens the camera

        if (this.camera == null)  //Since starting camera is synchronous, check for camera null object
            throw new Exception("Camera didn't start in ResumeView :(");*/

        if (mVisionFeaturesEnabled) {
            setupFaceAndBarcodeDetection();
            toggleFaceDetection(true); //Has to be true actually
            if (mSettings.getCameraId() % 2 == 0) //Back camera only for barcode detection
                toggleBarcodeDetection(false); //Has to be true actually
            else
                toggleBarcodeDetection(false);
        }

        if (this.publishing) {
            if (this.rtmp != null) {
                this.rtmp.clearQueues(); //Clear any old video/ audio after pressing home button
                this.rtmp.startBitrateChecks();  //Start bitrate check timers again
                this.rtmp.pauseBitrateChecks(4000); //Pause bitrate checks for a while until streaming becomes stable again
            }
        }

        this.preview.onResume();
        if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
            if (this.mMusicPlayer != null)
                this.mMusicPlayer.resume();
        }

        Display display = this.mContext.getWindowManager().getDefaultDisplay();
        orientationResult = display.getRotation();
        if (orientationResult % 180 == 0) {
            mVideoBackgroundEnabled = true;
        }
        setBackgroundVideoEnabled(mVideoBackgroundEnabled);
        mStopped = false;
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
        AppLibrary.log_d(TAG, "onPause -- releasing camera");

        if (BackgroundMusicPlayer.BACKGROUND_MUSIC_PLAYBACK_ENABLED) {
            if (this.mMusicPlayer != null)
                this.mMusicPlayer.pause();
        }
//        waitThread();
//        stopCamera();
        if (mHandlerThread != null)
            mHandlerThread.stopCameraSync();

        if (this.publishing) {
            isCameraPaused = true;
            if (this.rtmp != null)
                this.rtmp.stopBitrateChecks();  //Stop bitrate check timers while exiting the app
        }

        if (this.preview != null) {
            this.mSurfaceTexture = null; //Just removes the reference of the surface texture in StreamingActionControllerKitkat class
/*            this.preview.queueEvent(new Runnable() {
                @Override
                public void run() {
                    // Tell the renderer that it's about to be paused so it can clean up.
                    mRenderer.notifyPausing();
                    mStopped = true;
                    synchronized (CameraSurfaceRenderer.mStopLock) {
                        CameraSurfaceRenderer.mStopLock.notify();
                    }
                }
            });
            synchronized (CameraSurfaceRenderer.mStopLock) {
                while (!mStopped) {
                    try {
                        CameraSurfaceRenderer.mStopLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }*/
            mStopped = true;
            mRenderer.pause();
            this.preview.onPause();
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
        if (mDummyRenderer != null) {
            mDummyRenderer.release();
        }

        AppLibrary.log_d(TAG, "onPause complete " + this.publishing);
    }

    public boolean isPublishing() {
        return this.publishing;
    }

    public void actionPublish() {
        if (this.publishing) {
            waitThread();
            asyncStop("stopped");

        } else {
            waitThread();
            asyncStart();
        }
    }

    public void destroy() {
        mRenderer.releaseRenderer();
        if (mCameraCapturer != null) {
            mCameraCapturer.destroy();
            mCameraCapturer = null;
        }
        stopCamera();
        mCameraHandler.invalidateHandler();     // paranoia

//        mRenderer.setCameraPreviewRunning(false);
        if (mHandlerThread != null) {
            mHandlerThread.getHandler().removeCallbacksAndMessages(null);
            try {
                mHandlerThread.join(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandlerThread = null;
        }
        if (mRtmpMuxerAsyncStartStopThread != null) {
            mRtmpMuxerAsyncStartStopThread.getHandler().removeCallbacksAndMessages(null);
            mRtmpMuxerAsyncStartStopThread.interrupt();
            mRtmpMuxerAsyncStartStopThread = null;
        }

        BroadCastSettings.destroy();
        FilterManager.destroy();
//        this.mSettings = null;
        this.preview = null;
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

        //--> Temporarily commented

//        String IP = ((CameraActivity) mContext).getRTMPIP();
//
//        if (IP != null)
//            this.rtmpUrl = "rtmp://" + IP + "/live" + format + "_v1/" + key;
//        else
//            this.rtmpUrl = "rtmp://52.17.111.51/live" + format + "_v1/" + key; //Europe Server

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

            this.preview.onPause(); //Losing the GL surfaceView context and recreating again - Can be avoided
        }

//        stopCamera();
        mHandlerThread.stopCameraSync();

        AppLibrary.log_d(TAG, "loading new settings");
        if (quality != null)
            mSettings = BroadCastSettings.getInstance(quality);
        mSettings.swapCamera(changeCamera);

        if (changeCamera) {
            mZoomLevel = 1.0f;
            if (!FLIP_FRONT_CAMERA) {
                mRenderer.indicateIsFlipped(false);
            } else {
                mRenderer.indicateIsFlipped(((mSettings.getCameraId() % 2) == 0) ? false : true);
            }
            mRenderer.setScale(1.0f);
        }
        if (mRSSignal.getStreamFormat() != null && mRSSignal.getStreamKey() != null)
            setStreamKey(mRSSignal.getStreamKey(), mRSSignal.getStreamFormat());

        AppLibrary.log_d(TAG, "restarting camera with settings");

        if (!changeCamera) {
            this.preview.onResume();
        }

        mCameraChanged = true;
        mRenderer.setCameraChanged(true);

//        startCamera();
        mHandlerThread.startCameraSync();

//        mCameraChanged = false;
        if (changeCamera) {
            if (mVisionFeaturesEnabled) {
                setupFaceAndBarcodeDetection();
                toggleFaceDetection(true);
                if (mSettings.getCameraId() % 2 == 0) //Back camera only for barcode detection
                    toggleBarcodeDetection(false); //Has to be true actually
                else
                    toggleBarcodeDetection(false);
            }
        }

        if (this.camera == null) {
            if (mCameraControlCallback != null)
                mCameraControlCallback.onSwitchCamera(); //Send signal to switch back to the previous camera if the camera doesn't open
        } else {
            if (changeCamera) {
                if (mCameraControlCallback != null)
                    mCameraControlCallback.afterSwitchCamera();
            }
        }

        AppLibrary.log_d(TAG, "restart done, now start broadcasting");
    }

    @Override
    public void setCameraControlCallback(CameraControlCallback cameraControlCallback) {
        mCameraControlCallback = cameraControlCallback;
    }

    @Override
    public void setFilterControlCallback(FilterControlCallback filterControlCallback) {
        mFilterControlCallback = filterControlCallback;
    }

    @Override
    public void setDoubleShotCallback(CameraShotCallback cameraShotCallback) {
        mCameraShotCallback = cameraShotCallback;
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
        this.preview.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
            }
        });
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    synchronized private void handleSetSurfaceTexture(SurfaceTexture st) {
        AppLibrary.log_d(TAG, "handleSetSurfaceTexture");
        if (isError)
            return; //Return silently if there is an error in streaming - Streaming errors are handled in onAudioError/ onVideoError
        st.setOnFrameAvailableListener(this);
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
                onCameraError(ioe.toString());
                AppLibrary.log_e(TAG, "Camera start preview failed at handleSetSurfaceTexture()");
                return;
            }
        }

        if (mVisionFeaturesEnabled) {
            setupFaceAndBarcodeDetection();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
//        if (VERBOSE) AppLibrary.log_d(TAG, "ST onFrameAvailable");

        mRenderer.frameAvailable();
        mDummyRenderer.frameAvailable();
//        this.preview.requestRender();
    }

    public void onEncoderCreated() {
        sVideoEncoder.getEncoder().setCallback(this);
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
        private WeakReference<StreamingActionControllerKitKat> mWeakActivity;

        public CameraHandler(StreamingActionControllerKitKat activity) {
            mWeakActivity = new WeakReference<StreamingActionControllerKitKat>(activity);
        }

        public StreamingActionControllerKitKat getStreamingActionControllerKitkat() {
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

            StreamingActionControllerKitKat activity = mWeakActivity.get();
            if (activity == null) {
                AppLibrary.log_e(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
//                    activity.startPublishing();
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


    public interface FaceUpdateListener {
        void setFaceBounds(int x, int y, int width, int height);

        void setLeftEyePosition(int x, int y);

        void setRightEyePosition(int x, int y);

        void setNosePosition(int x, int y);

        void setRightEarPosition(int x, int y);

        void setLeftEarPosition(int x, int y);

        void setLeftCheekPosition(int x, int y);

        void setRightCheekPosition(int x, int y);

        void setLeftMouthPosition(int x, int y);

        void setRightMouthPosition(int x, int y);

        void setBottomMouthPosition(int x, int y);

        void updateFacePos(float yaw, float roll, float smileProbability);

        void updateFacePos();
    }

    public void setFaceUpdateListener(FaceUpdateListener faceUpdateListener) {
        mFaceUpdateListener = faceUpdateListener;
    }

    FaceUpdateListener mFaceUpdateListener;

    private void toggleFaceDetection(boolean enable) {
        if (mCameraCapturer == null) return;

        if (enable) {
            mCaptureMode |= CAPTURE_MODE_FACE;
        } else {
            mCaptureMode &= ~CAPTURE_MODE_FACE;
        }
        mCameraCapturer.setCaptureFrequency(1);

        if (!mCameraCapturer.isCaptureEnabled())
            mCameraCapturer.resetBitmapQueue();

        mCameraCapturer.setCaptureEnabled(enable);
        mCameraCapturer.setFilter(-1);
        // setFilter always has to be called after calling setCaptureEnabled in case, a filter needs to be forced,
        // as setCapturedEnabled will always set filter o default whivh is natural (0)
        mRenderer.setCaptureEnabled(enable);
    }

    private void toggleBarcodeDetection(boolean toggle) {
        BARCODE_MODE_ENABLED = toggle;
    }

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

    private static int CAPTURE_IMAGE_MODE_ONESHOTPREVIEW = 1;
    private static int CAPTURE_IMAGE_MODE_TAKEPICTURE = 2;
    private static int CAPTURE_IMAGE_M0DE_READPIXELS = 3;

    private int CAPTURE_IMAGE_PREFERRED_MODE = CAPTURE_IMAGE_M0DE_READPIXELS;

    boolean captureImageRequested = false;

    // Image resolution and processing will be different for thumbnail case
    private static int ONESHOT_CALLBACK_CAPTURE_THUMBNAIL = 0x01;
    private static int ONESHOT_CALLBACK_CAPTURE_IMAGE = 0x02;
    private int mOneShotCallbackResult;

    public void OldCaptureImage(final String ImgName) {
        mThumbnailPath = ImgName;
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String name = date + "_" + today.hour + today.minute + today.second + ".jpg";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        this.file = new File(dir, name);
        if (this.camera != null) {
            mOneShotCallbackResult = ONESHOT_CALLBACK_CAPTURE_THUMBNAIL;
            this.camera.setOneShotPreviewCallback(mOneShotPreviewCallback);
        }
    }

    public void takePicture() {
//      camera.takePicture(shutterCallback, null, pictureCallback);
//      camera.takePicture(shutterCallback, null, imageDataCallback, null);

//        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
//        ListIterator<Camera.Size> it = sizes.listIterator();
//        Camera.Size size1 = null;
//
//        while(it.hasNext()) {
//            size1 = it.next();
//            Log.e(TAG, "Supported picture sizes are :" + size1.width + " " + size1.height);
//
//            double aspectRatio = size1.width/size1.height;
//
//            if ((aspectRatio == mCameraPreviewAspectRatio) && (size1.width<=previewSize.width)) {
//                Log.e(TAG, "Bingo! Picture size :" + size1.width + " " + size1.height);
//                break;
//            }
//        }

        Camera.Parameters params = camera.getParameters();
//      params.setPictureSize(1280, 720);
        params.setJpegQuality(100);
//        params.setZoom(0);
//      params.setPreviewSize(1920, 1080);
        camera.setParameters(params);

        Camera.Size size = camera.getParameters().getPictureSize();
        Log.e(TAG, "Picture size by camera is :" + size.width + " " + size.height);
        Log.e(TAG, "Picture quality by camera is :" + camera.getParameters().getJpegQuality());

        try {
            camera.takePicture(null, null, null, jpegCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void captureImage(final String captureFilePath) {
//        mCaptureImagePath = captureFilePath;
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String name = date + "_" + today.hour + today.minute + today.second + "_LIVE.jpg";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        this.file = new File(dir, name);
        mCaptureImagePath = file.getAbsolutePath();
        if (this.camera != null) {
            if (CAPTURE_IMAGE_PREFERRED_MODE == CAPTURE_IMAGE_MODE_TAKEPICTURE) {
//                mImageCaptureRequest = true;
//                camera.cancelAutoFocus();
//                camera.autoFocus(mCameraFocusCallback);

                takePicture();

            } else if (CAPTURE_IMAGE_PREFERRED_MODE == CAPTURE_IMAGE_MODE_ONESHOTPREVIEW) {
                mOneShotCallbackResult = ONESHOT_CALLBACK_CAPTURE_IMAGE;
                this.camera.setOneShotPreviewCallback(mOneShotPreviewCallback);
            } else if (CAPTURE_IMAGE_PREFERRED_MODE == CAPTURE_IMAGE_M0DE_READPIXELS) {
                mRenderer.mImageCaptureRequest = true;
                pauseCameraPreview();

                //Just to make sure, there is atleast 1 draw call hereafter.
                preview.requestRender();
            } else {
                Log.e(TAG, "Take picture method not configured");
            }
        }
    }

    PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = new File(mCaptureImagePath);
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }

//            onPictureTakenFinished();

        }
    };

    private void processPictureTaken(Bitmap bitmap, String mediaPath) {
        if (mDoubleShotOn) {
            if (mDoubleShotMode == DOUBLE_SHOT_MODE_BACK) {
//                ((NewBroadCastPage) mContext).switchCameraClicked(); //Send signal to switch back to the previous camera if the camera doesn't open
                mCameraShotCallback.onBackCapture(bitmap, mediaPath);
            } else {
                mCameraShotCallback.onFrontCapture(bitmap, mediaPath);
            }

            mDoubleShotMode++;
            if (mDoubleShotMode >= DOUBLE_SHOT_MODE_COUNT) {
                mDoubleShotMode = DOUBLE_SHOT_MODE_BACK;
                mDoubleShotOn = false;
            }
        } else {
            mCameraShotCallback.onCameraCapture(bitmap, mediaPath);
        }
    }

    @Override
    public void onPictureTakenFinished(final Bitmap bitmap) {
        onPictureTakenFinished(bitmap, mCaptureImagePath);
    }

    @Override
    public void rendererReady() {
        //TODO Use this whenever required.
    }

    private void onPictureTakenFinished(Bitmap bitmap, final String mediaPath) {
        if (bitmap == null || bitmap.isRecycled()) return;
        final Bitmap outBitmap = bitmap.createBitmap(bitmap);
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                processPictureTaken(outBitmap, mediaPath);
            }
        });

/*        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.startPreview();
            }
        }, 500);*/
//        camera.startPreview();
    }


    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd");
            //Sleep to let the camera settle (only needed by few phones)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    PictureCallback imageDataCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
            mOneShotCallbackResult = ONESHOT_CALLBACK_CAPTURE_IMAGE;
            mOneShotPreviewCallback.onPreviewFrame(data, camera);
        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - jpeg");
            mOneShotCallbackResult = ONESHOT_CALLBACK_CAPTURE_IMAGE;
            mOneShotPreviewCallback.onPreviewFrame(data, camera);
//            onPictureTakenFinished();
        }
    };

    Camera.PreviewCallback mOneShotPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            int format = parameters.getPreviewFormat();
            byte[] bytI = null;
            //YUV formats require more conversion
            if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {

                AppLibrary.log_d("Track", "Format of camera preview is" + format);
                int w = parameters.getPreviewSize().width;
                int h = parameters.getPreviewSize().height;
                Log.d(TAG, "preview width : " + w + " height " + h);
                // Get the YuV image
                if (CAPTURE_IMAGE_PREFERRED_MODE == CAPTURE_IMAGE_MODE_ONESHOTPREVIEW) {
                    YuvImage yuv_image = new YuvImage(data, format, w, h, null);
                    // Convert YuV to Jpeg
                    Rect rect = new Rect(0, 0, w, h);
                    ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                    yuv_image.compressToJpeg(rect, 100, output_stream);
                    bytI = output_stream.toByteArray();
                } else {
                    bytI = data;
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytI, 0, bytI.length);
//                float scaleFactorX = AppLibrary.getDeviceParams(mContext, "width")/(float)bitmap.getHeight();
//                float scaleFactorY = AppLibrary.getDeviceParams(mContext, "height")/(float)bitmap.getWidth();

                AppLibrary.log_d(TAG, "The size of the bitmap is :" + bitmap.getWidth() + " " + bitmap.getHeight());

//                float scaleFactor = Math.min(scaleFactorX,scaleFactorY);
                float scaleFactor = 1.0f;
                Matrix matrix = new Matrix();
                matrix.postScale(1.0f, 1.0f);
                if (mSettings.getCameraId() == 0)
                    matrix.postRotate(90);
                else {
                    matrix.postRotate(-90);
                    matrix.postScale(-1, 1);
                }
                matrix.postScale(scaleFactor, scaleFactor);

                Bitmap outputBitmap;

                // Handle output bitmap according to the use of the output bitmap
                if (mOneShotCallbackResult == ONESHOT_CALLBACK_CAPTURE_THUMBNAIL) {
                    if ((h - 640) <= 0) {
                        outputBitmap = Bitmap.createBitmap(bitmap, (w - 480) / 2, 0, 480, h, matrix, true);
                    } else {
                        outputBitmap = Bitmap.createBitmap(bitmap, (w - 480) / 2, (h - 640) / 2, 480, 640, matrix, true);
                    }
                } else {
                    if (mDoubleShotOn) {
                        if (mDoubleShotMode == DOUBLE_SHOT_MODE_BACK) {
                            outputBitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 4, 0, bitmap.getWidth() / 2, bitmap.getHeight(), matrix, false);
                        } else {
                            outputBitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 4, 0, bitmap.getWidth() / 2, bitmap.getHeight(), matrix, false);
                        }
                    } else {
                        outputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
//                        outputBitmap = bitmap;
                    }
                }

                onPictureTakenFinished(outputBitmap, mCaptureImagePath);

                bitmap.recycle();

//                camera.startPreview();

//                ByteArrayOutputStream output_stream2 = new ByteArrayOutputStream();
//                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output_stream2);
//                byte[] byt = output_stream2.toByteArray();
//                FileOutputStream outStream = null;
//                try {
//                    // Write to SD Card
//
////                                File file = createFileInSDCard(FOLDER_PATH, "Image_"+System.currentTimeMillis()+".jpg");
//                    //Uri uriSavedImage = Uri.fromFile(file);
//                    outStream = new FileOutputStream(file);
//                    outStream.write(byt);
//                    outStream.close();
//                    AppLibrary.log_d("OneShotPreviewCallback", "Successfully wrote file");
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    return;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return;
//                } finally {
//                }

                if ((mOneShotCallbackResult == ONESHOT_CALLBACK_CAPTURE_THUMBNAIL) && (file != null))
                    uploadThumbnail(mThumbnailCaptureImageName);

            } else {
                AppLibrary.log_d("Track", "Unknown Format of camera preview is" + format);
            }
        }

    };

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
        upload = MasterClass.getTransferUtility().upload("instalively.data", "event/mobile/" + ImgName + ".jpg", file);

        upload.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                AppLibrary.log_e(TAG, "On State changed" + transferState.toString());
                if ((transferState == TransferState.COMPLETED) && publishing) {
//                            try {
////                                thumbnailUploadRequest(ImgName);
//                            } catch (JSONException e1) {
//                                e1.printStackTrace();
//                            }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                AppLibrary.log_e(TAG, "On Progress changed: " + bytesCurrent + "out of " + bytesTotal);
            }

            @Override
            public void onError(int i, Exception e) {
                AppLibrary.log_e(TAG, "Upload Thumbnail S3Client error: " + e.getMessage());
            }
        });
    }

//    public void thumbnailUploadRequest(String ImgName) throws JSONException {
//        if (mContext!=null) {
//            String imagePath = "event/mobile/" + ImgName + ".jpg";
//            List<NameValuePair> pairs = new ArrayList<>();
//            pairs.add(new BasicNameValuePair("imagePath", imagePath));
//            pairs.add(new BasicNameValuePair("bucket", "instalively.data"));
//
//            AppLibrary.log_d(TAG, "image path is:- " + imagePath);
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
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

    };

    private void setupOutputDirectoryForRecordedFile() {
//        //ToDo: Make this cleaner!
//        //Search for external SD card and check if it is writable
//        //If no writable external SD card is found get Movies directory from getExternalStoragePublicDirectory
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
        String mediaStoragePath = AppLibrary.getCreatedMediaDirectory(MasterClass.getGlobalContext());
        File mediaDir = new File(mediaStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();

        mOutputDir = mediaStoragePath;
    }

    private void checkAndRenameRecordedFile(String recordingFilePath) {
        if (mContext == null) return;
        String eventName = "";
        String eventId = "";
        if (eventName.isEmpty() || eventId.isEmpty() || eventName == null || eventId == null)
            return;

        if (recordingFilePath != null && !recordingFilePath.isEmpty()) {
            if (!recordingFilePath.contains(eventId)) return;
            File recordedFile = new File(recordingFilePath);
            if (recordedFile.exists()) {
                String fileName = recordingFilePath;
                eventName = eventName.replaceAll("[^a-zA-Z0-9.-]", "_");
                fileName = fileName.replaceAll(eventId, eventName);
                if (recordedFile.renameTo(new File(fileName)))
                    mLastRecordedFilepath = fileName;
//                while(!recordedFile.renameTo(new File(recordedFile.getParentFile(), fileName+".mp4")) && fileName.length()>2) {
//                    fileName = fileName.substring(0, fileName.length()-2);
//                }
//                AppLibrary.log_d(TAG, "File renamed:: " + recordedFile.getParentFile().getPath()+"/"+fileName+".mp4");
            }
        }
    }

    public static String getLastRecordedFilepath() {
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

    public RecordingMuxer getRecordingMuxer() {
        return this.mRecordingMuxer;
    }

    private static class FaceLandmarkSet {
        // Max no. of facial landmarks
        private int mSize = 20;

        private static final float[] err_input = new float[]{3, 3};

        // Associative Arrays with key as the *type* of facial landmark
        float[][] mFaceLandmarks, mFaceLandmarksPrev;

        float[][] mEstInputPrev, mEstInput;

        float[][] mEstInputErrPrev, mEstInputErr;

        float[][] mKGain;

        public FaceLandmarkSet() {
            this(20);
        }

        public FaceLandmarkSet(int size) {
            mSize = size;
            init();
        }

        private void init() {
            mFaceLandmarks = new float[mSize][2];
            mFaceLandmarksPrev = new float[mSize][2];

            mEstInput = new float[mSize][2];
            mEstInputPrev = new float[mSize][2];

            mEstInputErr = new float[mSize][2];
            mEstInputErrPrev = new float[mSize][2];

            mKGain = new float[mSize][2];

            for (int i = 0; i < mSize; i++) {
                mFaceLandmarksPrev[i] = new float[]{-1, -1};
                mFaceLandmarks[i] = new float[]{-1, -1};

                mEstInput[i] = new float[]{-1, -1};
                mEstInputPrev[i] = new float[]{-1, -1};

                mEstInputErr[i] = new float[]{0, 0};
                mEstInputErrPrev[i] = new float[]{0, 0};

                mKGain[i] = new float[]{0, 0};
            }
        }

        // Right now we assume that all the landmarks are received.
        // If not, we end up using the previous values. Let's see how that works for us.
        public void updateLandmarks(List<Landmark> landmarks) {
            for (Landmark landmark : landmarks) {
                int type = landmark.getType();
                mFaceLandmarksPrev[type][0] = mFaceLandmarks[landmark.getType()][0];
                mFaceLandmarksPrev[type][0] = mFaceLandmarks[landmark.getType()][1];

                mFaceLandmarks[type][0] = (int) landmark.getPosition().x;
                mFaceLandmarks[type][0] = (int) landmark.getPosition().y;

                filterValue(type);
            }
        }

        private void filterValue(int type) {
            float[] input_prev = mFaceLandmarksPrev[type];
            float[] input = mFaceLandmarks[type];

            float[] est_input = mEstInput[type];
            float[] est_input_prev = mEstInputPrev[type];

            float[] est_input_err = mEstInputErr[type];
            float[] est_input_err_prev = mEstInputErrPrev[type];

            float[] kGain = mKGain[type];

/*            mKGain[type] = divide(est_input_err, add(est_input_err, err_input));
            mEstInput[type] = mEstInputPrev[type];*/

            for (int i = 0; i < 2; i++) {
                kGain[i] = est_input_err[i] / (est_input_err[i] + err_input[i]);
                est_input[i] = est_input_prev[i] + kGain[i] * (input[i] - est_input_prev[i]);
                est_input_err[i] = (1 - kGain[i]) * est_input_err_prev[i];
            }
        }
    }
}

class PointM extends PointF {
    public static PointF add(PointF a, PointF b) {
        PointF c = new PointF();
        c.x = a.x + b.x;
        c.y = a.y + b.y;
        return c;
    }

    public static PointF subtract(PointF a, PointF b) {
        PointF c = new PointF();
        c.x = a.x - b.x;
        c.y = a.y - b.y;
        return c;
    }

    public static PointF multiply(PointF a, PointF b) {
        PointF c = new PointF();
        c.x = a.x * b.x;
        c.y = a.y * b.y;
        return c;
    }

    public static PointF divide(PointF a, PointF b) {
        PointF c = new PointF();
        c.x = (int) ((float) a.x / b.x);
        c.y = (int) ((float) a.y / b.y);
        return c;
    }

    public static PointF multiply(PointF a, float b) {
        PointF c = new PointF();
        c.x = (int) (a.x * b);
        c.y = (int) (a.y * b);
        return c;
    }
}

/**
 * Renderer object for our GLSurfaceView.
 * <p/>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
@TargetApi(18)

/**
 * Renderer object for our GLSurfaceView.
 * <p>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
class CameraSurfaceRenderer extends RajawaliRenderer implements GLSurfaceView.Renderer, DummySurfaceRenderer.RenderCallback {
    private static final String TAG = "CameraSurfaceRenderer";
    private static final boolean VERBOSE = false;

    public static final Object mRenderFence = new Object(); // For synchronisation between rendering and updateTexImage

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    public static Object mStopLock = new Object();
    private CameraCapture mCameraCapturer;

    private StreamingActionControllerKitKat.CameraHandler mCameraHandler;
    private TextureMovieEncoder mVideoEncoder;

    int mEncoderWidth = 640;
    int mEncoderHeight = 480;
    int mVideoBitrate = 384 * 1024;
    int mFrameRate = 25;

    private OpenGLRenderer mGLRenderer = null;

    private float[] mSTMatrix = new float[16];
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private int mFrameCount;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    private int mCurrentFilter;
    private int mNewFilter;

    private long mFrameTimestampNs;
    private boolean mSurfaceCreated;

    private Context mContext;
    private boolean mIsFlipped;
    private GLSurfaceView mGLSurfaceView;
    private long mTimeStamp = 0;
    private boolean mIsStagnantFrame;
    private boolean mStagnantSettled;
    private int mStagnantCount = 0;
    private boolean mCameraPreviewRunning;
    private boolean mCaptureEnabled;
    private boolean mResetRenderer;
    private float mZoomLevel;

    private GenericHandlerThread mGenericHandlerThread;
    private Handler mHandler;
    private boolean mFadeOutAnimation;
    private boolean mFadeInAnimation;
    protected int mPreviewWidth;
    protected int mPreviewHeight;
    private volatile boolean mCameraChanged;
    private long mInitialTime;

    //    private ArrayList<StickerModel> mThemeStickers;
    private ArrayList<ThemeModel> mThemeStickers;
    private DummySurfaceRenderer mDummyRenderer;

    private Vector3 defaultPos;
    private Vector3 pos = new Vector3();
    private Vector3 rot = new Vector3();

    public DirectionalLight light;
    public SpotLight spotLight;
    public Object3D sphere;
    public Context context;
    public android.org.rajawali3d.cameras.Camera camera;

    private Animation3D mCameraAnim, mLightAnim;
    private Object3D mObjectGroup;

    TranslateAnimation3D translateAnimation;
    private boolean maxReached;
    private double x;
    volatile boolean mImageCaptureRequest;
    private StreamingActionController mController;

    // Camera may actually have been started, but this indicates
    // whether it has been given the SurfaceTexture to write frames to.
    private boolean mCameraStarted;

    private void init() {
        init(false);
    }

    private void init(boolean registerForResources) {

        RajLog.i("Rajawali | Anchor Steam | v1.0 ");
        //RajLog.i("THIS IS A DEV BRANCH CONTAINING SIGNIFICANT CHANGES. PLEASE REFER TO CHANGELOG.md FOR MORE INFORMATION.");

        mRecordingEnabled = false;
        mFrameTimestampNs = -1;
        mSurfaceCreated = false;
        mCameraStarted = false;
    }

    /**
     * Constructs CameraSurfaceRenderer.
     * <p/>
     *
     * @param cameraHandler Handler for communicating with UI thread
     * @param movieEncoder  video encoder object
     */
    public CameraSurfaceRenderer(StreamingActionControllerKitKat.CameraHandler cameraHandler,
                                 TextureMovieEncoder movieEncoder,
                                 int encoderWidth, int encoderHeight,
                                 int videoBitrate, int frameRate, Context context) {
        super(context);
        mContext = context;
        mCameraHandler = cameraHandler;
        mVideoEncoder = movieEncoder;
        mVideoEncoder.setRenderController(this);
        mEncoderWidth = encoderWidth;
        mEncoderHeight = encoderHeight;
        mVideoBitrate = videoBitrate;
        setFrameRate(frameRate);
        mTextureId = -1;

        mRecordingStatus = -1;
        mRecordingEnabled = false;
        mFrameCount = -1;

        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
        // We could preserve the old filter mode, but currently not bothering.
        mCurrentFilter = -1;
        mNewFilter = StreamingActionControllerKitKat.FILTER_NONE;

        mResetRenderer = false;
        mZoomLevel = 1.0f;

        mGenericHandlerThread = new GenericHandlerThread("CameraSwap");
        mHandler = mGenericHandlerThread.getHandler();

        mCameraChanged = false;
        mIsFlipped = false;

        mImageCaptureRequest = false;
        init();
    }

    @Override
    protected void initScene() {
//        mTextureManager.reset();
        LoaderOBJ objParser = new LoaderOBJ(this, R.raw.bottle_obj);
//        LoaderFBX objParser = new LoaderFBX(this, R.raw.ty);
/*        try {
            objParser.parse();
            mObjectGroup = objParser.getParsedObject();
            mObjectGroup.isContainer(false);
            Log.d(TAG, "VECTOR: " + mObjectGroup.getScale().toString());
            Log.d(TAG, "position: " + mObjectGroup.getPosition().toString());
            mObjectGroup.setPosition(0.0, 0.0, -5.0);
            mObjectGroup.setScale(0.12);
            {
                Material material = new Material();
                material.setSpecularMethod(new SpecularMethod.Phong());
                material.setDiffuseMethod(new DiffuseMethod.Lambert());
//            material.addTexture(new Texture("earthColors", R.drawable.broadcaste_screen_filter_none));
                material.setColor(Color.argb(200, 250, 100, 100));
                material.setColorInfluence(4.7f);
                material.enableLighting(true);
                mObjectGroup.setMaterial(material);
            }
            mObjectGroup.setRotation(1.0f, 0.0f, 0.0f, 90);
            getCurrentScene().addChild(mObjectGroup);

*//*            getCurrentScene().addChild(mObjectGroup.getChildAt(0));
            getCurrentScene().addChild(mObjectGroup.getChildAt(1));*//*
            int childCount = mObjectGroup.getNumChildren();
            for(int i = 0; i < childCount; i++) {
                Log.d(TAG, "OBJGroup Child: " + mObjectGroup.getChildAt(i).getName());
*//*                if(i == 1) {
                    mObjectGroup.getChildAt(i).rotate(1, 0, 0, 90);
                    mObjectGroup.getChildAt(i).setPosition(0.0, 10, 0);
                    mObjectGroup.getChildAt(i).setScale(0.5);
                    Log.d(TAG, "child geometry: " + mObjectGroup.getChildAt(i).getGeometry().toString());
                    Log.d(TAG, "child vol: " +mObjectGroup.getChildAt(i).getTransformedBoundingVolume());
                    Log.d(TAG, "child pos: " + mObjectGroup.getChildAt(i).getPosition());
                    Log.d(TAG, "child scale: " + mObjectGroup.getChildAt(i).getScale());
                }*//*
//                mObjectGroup.getChildAt(i).setPosition(1.5*i, 1.7*i, 0);
            }*/

        translateAnimation = new TranslateAnimation3D(new Vector3(0.0, 0, 0.0), new Vector3(1.0, 0, 0.0));
        translateAnimation.setRepeatMode(Animation.RepeatMode.REVERSE_INFINITE);
        translateAnimation.setDurationMilliseconds(2500);
        translateAnimation.setTransformable3D(mObjectGroup);

        mCameraAnim = new RotateOnAxisAnimation(Vector3.Axis.Z, 360);
        mCameraAnim.setDurationMilliseconds(8000);
        mCameraAnim.setRepeatMode(Animation.RepeatMode.INFINITE);
//            mCameraAnim.setTransformable3D(mObjectGroup);
        /*} catch (ParsingException e) {
            e.printStackTrace();
        }*//* catch (ATexture.TextureException e) {
            e.printStackTrace();
        }*/

        light = new DirectionalLight(1.0f, 0.4f, -0.5f); // set the direction
        light.setColor(1.0f, 0.4f, 0.3f);
        light.setPower(8.0f);
        spotLight = new SpotLight(-1f, 0.2f, -1.0f);
        spotLight.setColor(Color.WHITE);
        spotLight.setFalloff(0.7f);
        spotLight.setPower(1.6f);

        mLightAnim = new EllipticalOrbitAnimation3D(new Vector3(0, 0, -2),
                new Vector3(7, 10, 4), Vector3.getAxisVector(Vector3.Axis.X), 0,
                360, EllipticalOrbitAnimation3D.OrbitDirection.CLOCKWISE);
        mLightAnim.setRepeatMode(Animation.RepeatMode.INFINITE);
        mLightAnim.setDurationMilliseconds(3500);
        mLightAnim.setTransformable3D(light);

/*        try {*/
        Material material = new Material();
        material.setSpecularMethod(new SpecularMethod.Phong());
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
//            material.addTexture(new Texture("earthColors", R.drawable.broadcaste_screen_filter_none));
        material.setColor(Color.argb(200, 250, 100, 100));
        material.setColorInfluence(0.7f);
        material.enableLighting(true);
//            sphere = new Cylinder(10, 5f, 24, 24);

//            sphere = new Sphere(0.5f, 100, 100);
        sphere = new Cube(0.6f);
//        sphere = new NPrism(12, 0.1, 0.5f, 0.96, 0.8);
        defaultPos = new Vector3(0.25f, 0.3f, 0f);
        sphere.setPosition(0.25f, 0.3f, -2f);
        sphere.setScale(1.0);
//        sphere.rotate(1, 0, 0, 90);
        sphere.setMaterial(material);
        Torus torus = new Torus(0.4f, 0.2f, 80, 40);
        Material material2 = new Material();
//            material2.addTexture(new Texture("kuchbhi", R.drawable.slide_4));
        material2.setColorInfluence(0.7f);
        material2.setColor(Color.argb(255, 100, 255, 0));
        torus.setMaterial(material2);
        torus.setPosition(0.1f, 0.2f, 0.2f);
        getCurrentScene().addLight(light);
        getCurrentScene().addLight(spotLight);
        final List<ALight> lights = new ArrayList<>();
        lights.add(light);
        getCurrentScene().addChild(sphere);
        // getCurrentScene().addChild(torus);
        /*} catch (ATexture.TextureException e){
            e.printStackTrace();
        }*/
        getCurrentScene().registerAnimation(translateAnimation);
        getCurrentScene().registerAnimation(mCameraAnim);
        getCurrentScene().registerAnimation(mLightAnim);

        //translateAnimation.play();
//        mCameraAnim.play();
        mLightAnim.play();
        Log.d(TAG, "3D Render Scene created");
        configureCamera();
    }

    // Duplicate View and projection matrices from 2D renderer's camera to 3D camera
    private void configureCamera() {
        android.org.rajawali3d.cameras.Camera camera = getCurrentCamera();
        setCameraPosition(camera);
        setCameraLookAt(camera);
        setCameraProjection(camera);
    }

    private void setCameraProjection(android.org.rajawali3d.cameras.Camera camera) {
        camera.setProjectionMatrix(new Matrix4(mGLRenderer.getProjectionMatrix()));
    }

    private void setCameraLookAt(android.org.rajawali3d.cameras.Camera camera) {
        camera.setLookAt(new Vector3(doubleArray(mGLRenderer.getCameraLookAt())));
    }

    private void setCameraPosition(android.org.rajawali3d.cameras.Camera camera) {
        camera.setPosition(new Vector3(doubleArray(mGLRenderer.getCameraPosition())));
    }

    float scaleFactor = 1.0f;

    public void setPosX(float v) {
        pos.x = /*defaultPos.x + */scaleFactor * (v/* - 0.5*/);
//        sphere.setPosition(pos);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
    }

    public void setPosY(float v) {
        pos.y = /*defaultPos.y + */scaleFactor * (v/* - 0.5*/);
//        sphere.setPosition(pos);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
    }

    public void setPosZ(float v) {
        pos.z = 2 * /*defaultPos.z  + */(v/* - 0.5*/);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
//        sphere.setPosition(pos);
    }

    public void setRotY(float v) {
        rot.y = v;
    }

    public void setRotZ(float v) {
        rot.z = v;
    }

    public void pause() {
        mSurfaceCreated = false;
        mDummyRenderer.notifyPausing();
    }

    void destroyRenderer() {
        destroyRenderer(false);
    }

    void destroyRenderer(boolean reset) {
        if (mGLRenderer != null) {
            mGLRenderer.destroy(reset);
            mGLRenderer = null;
        }
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p/>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mVideoEncoder != null) {
            mVideoEncoder.notifyPausing();
        }
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        destroyRenderer();
        mIncomingWidth = mIncomingHeight = -1;
    }

    public void notifyResuming() {
        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.PREVIEW, this);
    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
        // If playback is going on, then the next call to render will take care of changing Encoder state (ending mostly)
        // Otherwise, we take matters in our hands.
        if (!mRecordingEnabled) {
            changeEncoderState();
        }
    }

    @Override
    public void handleNotifyPausing() {
        notifyPausing();
    }

    public void setVideoBackground(Bitmap bitmap) {
        mVideoEncoder.updateVideoBackground(bitmap);

//        mGLRenderer.updateVideoBackground(bitmap);
    }

    public void setVideoBackground(float[] colors) {
        mVideoEncoder.updateVideoBackground(colors);

//        mGLRenderer.updateVideoBackground(bitmap);
    }

    /**
     * Changes the filter that we're applying to the camera preview.
     */
    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }

    public void setFrameRate(int frameRate) {
        mFrameRate = frameRate;
    }

    public void indicateCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p/>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;

        if (mIncomingWidth < 0 || mIncomingHeight < 0) {
            Log.w(TAG, "Incoming width/height < 0. So returning");
            return;
        }
        Log.d(TAG, "setCameraPreviewSize");
        // Ideally This needs to be called after onSurfaceCreated.
        // In case this gets triggered before onSurfaceCreated (which is where we actually create GLRenderer and prepare all Buffers (Caches.java)
        // We simply save the camera preview parameters  (mostly width and height), which we
        // use it later in onSurfaceChanged where we would have already created GLRenderer who's
        // the one using these parameters
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetCameraPreviewSize();
            }
        });
        if (mVideoEncoder != null) {
            mVideoEncoder.setCameraParams(mIncomingWidth, mIncomingHeight);
        }
        if (mCameraCapturer != null) {
            mCameraCapturer.setCameraParams(mIncomingWidth, mIncomingHeight);
        }
        mIncomingSizeUpdated = true;
    }

    private void handleSetCameraPreviewSize() {
        if (mGLRenderer != null) {
            mGLRenderer.setCameraParams(mIncomingWidth, mIncomingHeight);
        }
    }

    public void setIsFlipped(boolean isFlipped) {
        mIsFlipped = isFlipped;
        mVideoEncoder.setFlipState(mIsFlipped);
/*        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    Log.d(TAG, "Setting flip to " + (mIsFlipped ? "true" : "false"));
                    mGLRenderer.setExternalTextureFlip(mIsFlipped);
                }
            }
        });*/

    }

    public boolean getIsFlipped() {
        return mIsFlipped;
    }

    Runnable mCameraFlipAnimationFadeRunnable = new Runnable() {

        @Override
        public void run() {
//            mVideoEncoder.flipAnimation();

//            if(mFadeOutAnimation) {
//                mHandler.postDelayed(this, 25);
            Log.d(TAG, "requested rendering");
            mGLSurfaceView.requestRender();
/*            } else {
                mHandler.removeCallbacks(mCameraFlipAnimationFadeRunnable);
            }*/
        }
    };

/*    Runnable mCameraFlipAnimationFadeOutRunnable = new Runnable() {

        @Override
        public void run() {
//            mVideoEncoder.flipAnimation();

            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mGLRenderer != null) {
                        mFadeOutAnimation = mGLRenderer.fadeAnimation(false, false);
                        if(!mFadeOutAnimation) {
                            mHandler.removeCallbacks(mCameraFlipAnimationFadeOutRunnable);
                        }
                    }
                }
            });
            if(mFadeOutAnimation) {
                mHandler.postDelayed(this, 25);
                mGLSurfaceView.requestRender();
            } else {
                mHandler.removeCallbacks(mCameraFlipAnimationFadeOutRunnable);
            }
        }
    };

    Runnable mCameraFlipAnimationFadeInRunnable = new Runnable() {

        @Override
        public void run() {
//            mVideoEncoder.handleFlipAnimation();
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mGLRenderer != null) {
                        mFadeInAnimation = mGLRenderer.fadeAnimation(false, true);
                    }
                }
            });

            if(mFadeInAnimation) {
                mHandler.postDelayed(this, 25);
//                mGLSurfaceView.requestRender();
            } else {
                mHandler.removeCallbacks(mCameraFlipAnimationFadeInRunnable);
            }
        }
    };*/

    public void setCameraChanged(boolean cameraChanged) {
        mCameraChanged = cameraChanged;
    }

    public void stopCameraSwapAnimation(final Bitmap bitmap) {
        Log.d(TAG, "fade out animation request ");

//        mVideoEncoder.fadeAnimation(bitmap, false);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    Log.d(TAG, "fade Out animation actually start ");
                    mGLRenderer.fadeOutAnimation(bitmap);
                }
            }
        });
        mGLSurfaceView.requestRender();
    }

    public void startCameraSwapAnimation() {

        Log.d(TAG, "fade in animation request ");

//        mVideoEncoder.fadeAnimation(null, true);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    Log.d(TAG, "fade In animation actually start ");
                    mGLRenderer.fadeInAnimation();
                }
            }
        });
        /*
        if (!fadeIn) {
            mFadeOutAnimation = true;
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mHandler.removeCallbacks(mCameraFlipAnimationFadeRunnable);
                    if (mGLRenderer != null) {
                        mGLRenderer.fadeAnimation(fadeIn);
                    }
                    mGLSurfaceView.requestRender();
                }
            });
            mHandler.postDelayed(mCameraFlipAnimationFadeRunnable, 0);
        } else {
            mHandler.removeCallbacks(mCameraFlipAnimationFadeRunnable);

            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mGLRenderer != null) {
                        mGLRenderer.fadeAnimation(fadeIn);
                        mFadeInAnimation = true;
                    }
                    mHandler.postDelayed(mCameraFlipAnimationFadeRunnable, 0);
                }
            });
        }*/
    }

/*    public void startCameraFlipAnimation(boolean fadeIn) {
//        mVideoEncoder.flipAnimation();
//        mFlipAnimation = true;

        if(!fadeIn) {
            mFadeOutAnimation = true;
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mHandler.removeCallbacks(mCameraFlipAnimationFadeInRunnable);
                    if (mGLRenderer != null) {
                        mGLRenderer.fadeAnimation(true, false);
                    }
                }
            });
            mHandler.postDelayed(mCameraFlipAnimationFadeOutRunnable, 0);
        } else {
            mHandler.removeCallbacks(mCameraFlipAnimationFadeOutRunnable);

            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.fadeAnimation(true, true);
                    mFadeInAnimation = true;
                }
                mHandler.postDelayed(mCameraFlipAnimationFadeInRunnable, 0);
                }
            });
        }
    }*/

    public void setCameraCapturer(CameraCapture cameraCapturer) {
        mCameraCapturer = cameraCapturer;
        if (mCameraCapturer != null) {
            mCameraCapturer.setRenderController(this);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");

        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled && mRecordingStatus == CameraSurfaceRenderer.RECORDING_ON) {
            Log.d(TAG, "Recording Resumed in Surface Created");
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            Log.d(TAG, "Recording Starting from scratch in Surface Created");
            mRecordingStatus = RECORDING_OFF;
        }
        OpenGLRenderer.removeStaticDrawables();

        destroyRenderer(mResetRenderer);

        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.PREVIEW, this);
        mGLRenderer.addExternalSourceDrawable(true);

        if (mDummyRenderer != null) {
            mDummyRenderer.prepareCameraCapture(EGL14.eglGetCurrentContext());
        }

        if (!mCameraStarted) {
            Log.d(TAG, "OnSurfaceCreated: Camera not yet started... so creating SurfaceTexture");
            handleCreateSurfaceTexture();
        } else {
            Log.d(TAG, "OnSurfaceCreated: Camera already yet started... so skipping creating SurfaceTexture");
        }
        mSurfaceCreated = true;

//        onRenderSurfaceCreated(config, gl, 0, 0);
//        setFrameRate(20);
    }

    void createSurfaceTexture() {
        if (!mSurfaceCreated) {
            Log.d(TAG, "Surface not created yet. So skipping creating SurfaceTexture");
            return;
        } else {
            Log.d(TAG, "Surface already created. So creating SurfaceTexture");
        }
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
                    handleCreateSurfaceTexture();
                }
            }
        });
    }

    private void handleCreateSurfaceTexture() {

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        SurfaceTexture prevSurfaceTexture = OpenGLRenderer.getSurfaceTexture(this);
        if (prevSurfaceTexture != null) {
            prevSurfaceTexture.release();
        }
        mSurfaceTexture = mGLRenderer.createSurfaceTexture(this);
        mGLRenderer.setSurfaceTexture(mSurfaceTexture);

        updateSurfaceTexture();
    }

    /**
     * Pass it to Camera as preview texture by calling setPreviewTexture
     */
    void updateSurfaceTexture() {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = OpenGLRenderer.getSurfaceTexture(this);
        }
        if (mSurfaceTexture == null) {
            return;
        }
        setCameraStarted(true);
        ((StreamingActionControllerKitKat) mController).setSurfaceTexture(mSurfaceTexture);
        // Tell the UI thread to enable the camera preview.
//        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
//                StreamingActionControllerKitKat.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged camera" + width + "x" + height);

        // Incase setCameraPreviewSize is called before this (i.e. onSurfaceChanged), so we wouldn't have passed Camera Preview size to the GLRenderer.
        // Now is the time for that.
        handleSetCameraPreviewSize();

        handleSetRenderParams(width, height);

        if (mCameraCapturer != null) {
            mCameraCapturer.prepareCameraCapture(EGL14.eglGetCurrentContext());
        }
        mDummyRenderer.pauseUpdates(false);

        if (mResetRenderer) {
            Log.d(TAG, "Resetting renderer properties");
            resetRendererLifeCycle();
            mZoomLevel = 1.0f;
            setScale(mZoomLevel);
            mResetRenderer = false;
        } else {
            removeAnimatableDrawables();
            addOverlays(mThemeStickers);
            setFilter(mCurrentFilter, true);
            setScale(mZoomLevel);
        }

        //Call this after GL setup (camera) has been done by Renderer (via handleSetRenderParams)
        //onRenderSurfaceSizeChanged(gl, width, height);
    }

    public void addColorDrawable(final float x, final float y, final float scaleX, final float scaleY) {
        if (VERBOSE) {
            Log.d(TAG, "Adding color drawable");
        }
        mGLRenderer.addColorDrawable(OpenGLRenderer.DRAWABLES_ANIMATABLE, x, y, scaleX, scaleY, null, true);
    }

    public void onSurfaceDestroyed() {
        //Passing SurfaceTexture as null, as it is anyway not being used
        //onRenderSurfaceDestroyed(null);
    }

    private void removeAnimatableDrawables() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
                    mGLRenderer.removeDrawables(OpenGLRenderer.DRAWABLES_ANIMATABLE);
                }
            }
        });
        //Video encoder setRenderParams should be triggered after PReview GL Renderer's,
        // because that's what sets the parameters required by the Encoder's renderer.
        mVideoEncoder.removeDrawables(OpenGLRenderer.DRAWABLES_ANIMATABLE);
    }

    Runnable mPrepareCameraCapturerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCameraCapturer != null) {
                mCameraCapturer.prepareCameraCapture(EGL14.eglGetCurrentContext());
            }
        }
    };

    public void setRenderParams() {
        setRenderParams(mPreviewWidth, mPreviewHeight);
    }

    private void setRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetRenderParams();
            }
        });
    }

    private void handleSetRenderParams() {
        handleSetRenderParams(mPreviewWidth, mPreviewHeight);
    }

    private void handleSetRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;

        if (mGLRenderer != null) {
            //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
            mGLRenderer.setRenderParams(mPreviewWidth, mPreviewHeight);
        }
        //Video encoder setRenderParams should be triggered after PReview GL Renderer's,
        // because that's what sets the parameters required by the Encoder's renderer.
        mVideoEncoder.setPreviewRenderParams(width, height);

        if (mCameraCapturer != null) {
            mCameraCapturer.setPreviewRenderParams(width, height);
        }


    }

    public void flagForReset() {
        mResetRenderer = true;
    }

    private void resetRendererLifeCycle() {
        mCurrentFilter = FilterManager.FILTER_NATURAL;
        mVideoEncoder.resetLifecycle();

        if (mGLRenderer != null) {
            mGLRenderer.resetLifecycle();
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (VERBOSE) Log.d(TAG, "onDrawFrame ");
        boolean showBox = false;

/*        if (mIsStagnantFrame) {
            if (!mStagnantSettled) {
                ++mStagnantCount;
//                mTimeStamp += 20 * (long)1000000;
                if (mStagnantCount >= 4) {
                    mStagnantCount = 0;
                    mStagnantSettled = true;
                }
            }
        } else {
            mStagnantSettled = false;
        }*/

        if (mGLRenderer == null || !mGLRenderer.isRenderParamsInitialized())

        {

            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.d(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }
        updateBlendFactor();

        // Draw the video frame.
//        mSurfaceTexture.getTransformMatrix(mSTMatrix);
//        mTimeStamp = mSurfaceTexture.getTimestamp();

//        mGLRenderer.setExternalTextureTransformMatrix(mSTMatrix);
//        mGLRenderer.setSurfaceTextureTimestamp(mTimeStamp);

        synchronized (mRenderFence) {
            // Synchronise with Video rendering, because both might be happening concurrently.
            synchronized (TextureMovieEncoder.mRenderFence) {
              //  mGLRenderer.setFilterTextures(texture1,SurfaceRenderer.mInkWellTexture);
                mGLRenderer.drawFrame();
                GLES20.glFinish();
/*            IntBuffer buf = IntBuffer.allocate(1);
            unused.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);*/
//            onRenderFrame(unused);
            }
        }
        if (mImageCaptureRequest) {
            Bitmap bitmap = captureBitmap();
            mController.onPictureTakenFinished(bitmap);
// Don't trim the memory here.. because the user might click save again
//            trimMemory();
            mImageCaptureRequest = false;
        }
//        changeScene();
    }

    private void changeScene() {
        if (maxReached) {
            x -= 0.01;
            if (x <= -0.0f) {
                maxReached = false;
            }
        } else {
            x += 0.01;
            if (x >= 1.0f) {
                maxReached = true;
            }
        }

        try {

            double width = pos.z; //0.5f; //screen %age width
            width /= sphere.getScaleX();

            float projectionDist = mGLRenderer.getCameraPosition()[2];

            Vector3 position = screenToWorld((float) pos.x * mPreviewWidth, (float) pos.y * mPreviewHeight, mPreviewWidth, mPreviewHeight, projectionDist, (float) width);

//            sphere.setScreenCoordinates((float)pos.x * mPreviewWidth, (float) pos.y * mPreviewHeight, mPreviewWidth, mPreviewHeight, projectionDist);
//            Vector3 position = sphere.getPosition().clone();

            sphere.setDoubleSided(true);
//            sphere.setRotation(1, 0, 0, 1.5);
            Matrix4 rotMatrix = new Matrix4();
            rotMatrix.identity();
            rotMatrix.setToRotation(rot.y, 0, rot.z);
/*            sphere.setRotation(0, 1, 0, rot.y);
            sphere.setRotation(0, 0, 1, -rot.z);*/
            sphere.setRotation(rotMatrix);
//            Vector3 position = new Vector3(0.0f, 0.0f, -2.0f);
            if (VERBOSE) {
                Log.d(TAG, "width: " + (float) width + " final position: " + (float) position.x + " " + (float) position.y + " " + (float) position.z);
                Log.d(TAG, "Rotation: " + "y: " + rot.y + " z: " + rot.z);
            }
            sphere.setPosition(position);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static double[] doubleArray(float[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    public static float[] floatArray(double[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) input[i];
        }
        return output;
    }

    private Vector3 screenToWorld(float x, float y, int viewportWidth, int viewportHeight, float projectionDepth) {
        return screenToWorld(x, y, viewportWidth, viewportHeight, projectionDepth, 1.0f);
    }

    private Vector3 screenToWorld(float x, float y, int viewportWidth, int viewportHeight, float projectionDepth, float width) {
        float[] r1 = new float[16];
        int[] viewport = new int[]{0, 0, viewportWidth, viewportHeight};

        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);

        android.opengl.Matrix.setIdentityM(modelViewMatrix, 0);
        android.opengl.Matrix.multiplyMM(modelViewMatrix, 0, mGLRenderer.getViewMatrix(), 0, modelMatrix, 0);

        GLU.gluUnProject(x, viewportHeight - y, 0.0f, modelViewMatrix, 0, mGLRenderer.getProjectionMatrix(), 0, viewport, 0, r1, 0);
        // Since model matrix is identity, ModelView should be same as View matrix.
//        GLU.gluUnProject(x, viewportHeight - y, 0.0f, doubleArray(mGLRenderer.getViewMatrix()), 0, doubleArray(mGLRenderer.getProjectionMatrix()), 0, viewport, 0, r1, 0);
//        GLU.gluUnProject(x, viewportHeight - y, 0.0f, modelMatrix, 0, doubleArray(mGLRenderer.getProjectionMatrix()), 0, viewport, 0, r1, 0);

        //take the normalized vector from the resultant projection and the camera, and then project by the desired distance from the camera.
        Vector3 result = new Vector3(r1[0], r1[1], r1[2]);
//        result.z = -result.z;
        if (VERBOSE) {
            Log.d(TAG, "result: " + result.toString());
        }
        Vector3 cameraPos = getCurrentCamera().getPosition().clone();
        result.subtract(cameraPos);
        if (VERBOSE) {
            Log.d(TAG, "result: camera position: " + cameraPos.toString());
            Log.d(TAG, "result: offset from camera: " + result.toString());
        }
        result.normalize();
        result.multiply(projectionDepth / width);
//        result.z /= width;
        result.add(cameraPos);
        if (VERBOSE) {
            Log.d(TAG, "final result: " + result.toString());
        }
        return result;
    }

    private void changeEncoderState() {

        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled)

        {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            this.mEncoderWidth,
                            this.mEncoderHeight,
                            this.mVideoBitrate,
                            this.mFrameRate,
                            EGL14.eglGetCurrentContext(),
                            true));
                    mVideoEncoder.setZoom(mZoomLevel);
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());

                    removeAnimatableDrawables();
                    addOverlays(mThemeStickers);

                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else

        {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }
    }

    public void setCameraPreviewRunning(boolean previewRunning) {
        mCameraPreviewRunning = previewRunning;
        if (!mCameraPreviewRunning) {
            if (mCameraCapturer != null) {
                mCameraCapturer.destroy();
            }
        }
    }

    public void setController(StreamingActionController controller) {
        mController = controller;
    }

    public void setContext(Context context) {
        mContext = context;

//        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.watermark);
//        OpenGLRenderer.addBitmap(bitmap, true);
//        OpenGLRenderer.addBitmapDrawable(true, bitmap);
    }

    /**
     * Draws a red box in the corner.
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final String text) {
        Bitmap bitmap = createTextBitmap(text, 50);
        Log.d(TAG, "Created Text bitmap of size : " + bitmap.getWidth() + " x " + bitmap.getHeight());
        addDrawable(x, y, scaleX, scaleY, bitmap);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final Bitmap bitmap) {
        addDrawable(x, y, scaleX, scaleY, bitmap, false);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final Bitmap bitmap, boolean onlyEncoder) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap to be added is null");
            return;
        }
        if (!onlyEncoder) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "finally addding new drawable 1");
                    if (mGLRenderer != null) {
                        Log.d(TAG, "finally addding new drawable 2");
                        mGLRenderer.addAnimatableBitmapDrawable(x, y, scaleX, scaleY, bitmap);
                    }
                }
            });
        }
        Drawable2d drawable = OpenGLRenderer.createBitmapDrawable(bitmap);
        drawable.setTranslate(new float[]{x, y, 0.0f});
        drawable.setScale(scaleX, (scaleY * bitmap.getHeight()) / bitmap.getWidth(), 1.0f);
        mVideoEncoder.addDrawable(OpenGLRenderer.DRAWABLES_ANIMATABLE, drawable);
        Log.d(TAG, "Adding overlay drawable at " + x + "x" + y);
    }

    public void addDrawable(final float x, final float y, final int resId) {
        Log.d(TAG, "adding new drawable");

        //Temporary code for testing text
//        if (resId == R.drawable.rec_thumbsup) {
////            addDrawable("Hi Karthik! :D");
//
//            final float scaleX = 0.3f, scaleY = 0.3f;
//            final float X = 2 * (x / mPreviewWidth) - 1.0f;
//            final float Y = 1.0f - 2.0f * (y / mPreviewHeight);
//            addDrawable(X, Y, scaleX, scaleY, "Hi Karthik");
//            return;
//        }

        final Bitmap bitmap = (OpenGLRenderer.getBitmap(resId) == null) ? BitmapFactory.decodeResource(mContext.getResources(), resId) : OpenGLRenderer.getBitmap(resId);
        if (OpenGLRenderer.getBitmap(resId) == null) {
            OpenGLRenderer.addBitmap(resId, bitmap);
        }
        final float scaleX = 0.3f, scaleY = 0.3f;
        final float X = 2 * (x / mPreviewWidth) - 1.0f;
        final float Y = 1.0f - 2.0f * (y / mPreviewHeight);

        addDrawable(X, Y, scaleX, scaleY, bitmap);
    }

    public void addOverlays(ArrayList<ThemeModel> themeModels) {
        removeAnimatableDrawables();
        if (themeModels == null) return;
        mThemeStickers = themeModels;
        ThemeModel themeModel;
        if (mPreviewWidth > mPreviewHeight) {
            // Landscape
            themeModel = themeModels.get(1);
        } else {
            // Portrait
            themeModel = themeModels.get(0);
        }
        for (ThemeAsset themeAsset : themeModel.getAssets()) {
            //   addDrawable(themeAsset.mMarginLeft+themeAsset.mWidth/2, themeAsset.mMarginTop+themeAsset.mHeight/2, themeAsset.mWidth, themeAsset.mHeight, themeAsset.mStickerId, true, true);
            addUrlDrawables(themeAsset.mBitmap, themeAsset.mUrl, themeAsset.mMarginLeft + themeAsset.mWidth / 2, themeAsset.mMarginTop + themeAsset.mHeight / 2, themeAsset.mWidth, themeAsset.mHeight, themeAsset.mStickerId, true, true);
        }
    }

/*    public void addOverlays(ArrayList<StickerModel> stickerModels) {
        if(stickerModels == null) return;
        removeAnimatableDrawables();
        mThemeStickers = stickerModels;
        for(StickerModel stickerModel : mThemeStickers) {
            addDrawable(stickerModel.mPositionX, stickerModel.mPositionY, stickerModel.mWidth, stickerModel.mHeight, stickerModel.mResId);
        }
    }*/

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final int resId) {
        addDrawable(x, y, scaleX, scaleY, resId, false, false);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final int resId, boolean isNormalized, boolean onlyEncoder) {
        Log.d(TAG, "adding new drawable");
//        Log.d(TAG, "x: " + x + " y: " + y + " scaleX: " + scaleX + " scaleY: " + scaleY);
        final Bitmap bitmap = (OpenGLRenderer.getBitmap(resId) == null) ? BitmapFactory.decodeResource(mContext.getResources(), resId) : OpenGLRenderer.getBitmap(resId);
        if (OpenGLRenderer.getBitmap(resId) == null) {
            OpenGLRenderer.addBitmap(resId, bitmap);
        }
        final float X, Y, ScaleX, ScaleY;
        if (!isNormalized) {
            X = 2 * (x / mPreviewWidth) - 1.0f;
            Y = 1.0f - 2.0f * (y / mPreviewHeight);
            ScaleX = scaleX / mPreviewWidth;
            ScaleY = ScaleX;
        } else {
            X = 2 * x - 1.0f;
            Y = 1.0f - 2.0f * y;
            ScaleX = scaleX;
            ScaleY = ScaleX;
        }
//        Log.d(TAG, "Final x: " + X + " y: " + Y + " scaleX: " + ScaleX + " scaleY: " + ScaleY);
        addDrawable(X, Y, ScaleX, ScaleY, bitmap, onlyEncoder);
    }

    public void addUrlDrawables(Bitmap bitmap, String url, final float x, final float y, final float scaleX, final float scaleY, final String resId, boolean isNormalized, boolean onlyEncoder) {
        Log.d(TAG, "adding new drawable");
//        Log.d(TAG, "x: " + x + " y: " + y + " scaleX: " + scaleX + " scaleY: " + scaleY);
        if (OpenGLRenderer.getUrlBitmap(url) == null) {
            OpenGLRenderer.setUrlBitmap(url, bitmap);
        }
        final float X, Y, ScaleX, ScaleY;
        if (!isNormalized) {
            X = 2 * (x / mPreviewWidth) - 1.0f;
            Y = 1.0f - 2.0f * (y / mPreviewHeight);
            ScaleX = scaleX / mPreviewWidth;
            ScaleY = ScaleX;
        } else {
            X = 2 * x - 1.0f;
            Y = 1.0f - 2.0f * y;
            ScaleX = scaleX;
            ScaleY = ScaleX;
        }
//        Log.d(TAG, "Final x: " + X + " y: " + Y + " scaleX: " + ScaleX + " scaleY: " + ScaleY);
        addDrawable(X, Y, ScaleX, ScaleY, bitmap, onlyEncoder);

    }

    public void addDrawable(String text) {
        Bitmap bitmap = createTextBitmap(text, 50);
        Log.d(TAG, "Created Text bitmap of size : " + bitmap.getWidth() + " x " + bitmap.getHeight());
        addDrawable(0.5f, 0.5f, 1.0f, 1.0f, bitmap);
    }

    private Bitmap createTextBitmap(String text, int textSize) {
        // Get text dimensions
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG
                | Paint.LINEAR_TEXT_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        // Create bitmap and canvas to draw to
        Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        // Draw background
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG
                | Paint.LINEAR_TEXT_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(50, 20, 20, 0));
        c.drawPaint(paint);

        // Draw text
        c.drawText(text, 0, text.length(), bounds.width() / 2, bounds.height(), textPaint);

        return b;
    }

    public void setTargetView(GLSurfaceView preview) {
        mGLSurfaceView = preview;
    }

    public void setFilter(final int filterMask, final boolean enable) {
        mCurrentFilter = filterMask;
        mVideoEncoder.setFilter(filterMask);

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.setExternalTextureFilter(filterMask, enable);
                }
            }
        });
    }

    public void setCaptureFilter(int filterMask) {
        if (filterMask < 0) {
            filterMask = mCurrentFilter + 1; // Recall the 1 offset for Capture filters :P
        }
        if (mCameraCapturer != null) {
            mCameraCapturer.setFilter(filterMask);
        }
    }

    public void setScale(final float scale) {
        mZoomLevel = scale;
        mVideoEncoder.setZoom(scale);

//TODO critical
//REmove this function call
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.setExternalTextureZoom(scale);
                }
            }
        });
    }

    public void changeBitrate(int bitrate) {
        if (mVideoEncoder != null)
            mVideoEncoder.changeEncoder(bitrate);
    }

    public Rect getTransformedFocusRect(int x, int y, int[] focusIntensity) {
        if (mGLRenderer == null) return null;
        return mGLRenderer.getTransformedFocusRect(x, y, focusIntensity);
    }

    Drawable2d mPickedDrawable = null;

    public void moveDrawable(float x, float y) {
        final float X = 2 * (x / mPreviewWidth)/* - 1.0f*/;
        final float Y = /*1.0f*/ -2.0f * (y / mPreviewHeight);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    if (mPickedDrawable != null) {
                        mPickedDrawable.translate(X, Y);
                    }
                }
            }
        });
    }

    public void pickDrawable(float x, float y) {
        final float X = 2 * (x / mPreviewWidth) - 1.0f;
        final float Y = 1.0f - 2.0f * (y / mPreviewHeight);
        Log.d(TAG, "Query drawable at " + X + " " + Y);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    Drawable2d drawable = mGLRenderer.pickDrawable(new PointF(X, Y));
                    if (drawable == null) {
                        Log.w(TAG, "No drawable returned");
                        return;
                    }
                    mPickedDrawable = drawable;
                }
            }
        });
    }

    public void setIsStagnantFrame(boolean isStagnantFrame) {
        mIsStagnantFrame = isStagnantFrame;
/*        mStagnantSettled = false;*/
        mVideoEncoder.setIsStagnantFrame(mIsStagnantFrame);

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.setIsStagnantFrame(mIsStagnantFrame);
                }
            }
        });
    }

    public void setCaptureEnabled(boolean captureEnabled) {
        mCaptureEnabled = captureEnabled;
        if (mCameraCapturer == null) {
            mCaptureEnabled = false;
        }
    }

    public void setInitialTime(long initialTime) {
        mInitialTime = initialTime;
    }

    private void updateBlendFactor() {
        final float blendFactor = Math.max(0.0f, Math.min(1.0f, (float) ((double) (System.currentTimeMillis() - mInitialTime) / StreamingActionControllerKitKat.BACKGROUND_CAPTURE_INTERVAL)));
        mVideoEncoder.setBlendFactor(blendFactor);
//        mGLRenderer.setBlendFactor(blendFactor);
    }

    public boolean frameAvailable() {
        if (mCameraChanged) {
            if (VERBOSE)
                Log.d(TAG, "Camera changed, so sending new render params");
            setCameraPreviewSize(mIncomingWidth, mIncomingHeight);
            setRenderParams();
            prepareCameraCapture();
            setIsFlipped(mIsFlipped);
            if (mCameraCapturer != null) {
                startCameraSwapAnimation();
            }
            mFadeOutAnimation = false;
            return false;
        }
        return true;
    }

    public void releaseRenderer() {
        mThemeStickers = null;
        mController = null;
    }

    public void setDummyRenderer(DummySurfaceRenderer dummyRenderer) {
        mDummyRenderer = dummyRenderer;
        if (mDummyRenderer != null) {
            mDummyRenderer.setRenderController(this);
        }
    }

    public DummySurfaceRenderer getDummyRenderer() {
        return mDummyRenderer;
    }

    @Override
    public void contextReady(EGLContext eglContext) {
        //TODO
    }

    // Called after updateTexImage to finally render the latest frame
    @Override
    public void textureAvailable() {
        //Request each thread to render the frame to its own surface.
        changeEncoderState();

        // Do not render the first frame after Camera Swap (Paranoia) Risks: Wrong aspect ratio etc
        if (mCameraChanged) {
            // Also set it to false here.
            mCameraChanged = false;
            return;
        }
        // Do whatever it takes to stop Rendering if the user has pressed Capture button
        if (!mImageCaptureRequest) {
            mGLSurfaceView.requestRender();
        }
        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(/*mSTMatrix, mTimeStamp*/);

        // Signal the Offscreen Renderer to render the frame, if required.
        if (mCaptureEnabled) {
            mCameraCapturer.frameAvailable(mSTMatrix, mTimeStamp);
        }
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    public Bitmap captureBitmap() {
        return mGLRenderer.createBitmapFromGLSurface();
    }

    public void decrementFilter() {
        setFilter(--mCurrentFilter, true);
    }

    public void incrementFilter() {
        setFilter(++mCurrentFilter, true);
    }

    public void allocTempResources() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.allocTempResources();
                }
            }
        });
    }

    public void trimMemory() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mGLRenderer.trimMemory();
            }
        });
    }

    public void prepareCameraCapture() {
        mGLSurfaceView.queueEvent(mPrepareCameraCapturerRunnable);
    }

    public void setCameraStarted(boolean cameraStarted) {
        mCameraStarted = cameraStarted;
    }

    public void indicateIsFlipped(boolean isFlipped) {
        mIsFlipped = isFlipped;
    }

    public class GenericHandlerThread extends HandlerThread {
        Handler mHandler = null;
        public boolean mIsRunning = false;

        GenericHandlerThread(String tag) {
            super("GenericHandlerThread" + tag);
            start();
            mHandler = new Handler(getLooper());
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

}
