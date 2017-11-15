package com.pulseapp.android.broadcast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Region;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.pulseapp.android.gles.EglCore;
import com.pulseapp.android.gles.OffscreenSurface;
import com.pulseapp.android.gles.OpenGLRenderer;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by bajaj on 14/10/15.
 */
public class CameraCapture {
    private static final String TAG = "CameraCapture";
    private CustomHandlerThread mHandlerThread = null;

    private boolean VERBOSE = false;
    private boolean SHOW_FRAME_LOGS = false;

    public static final Object mRenderFence = new Object(); // For synchronisation between rendering and updateTexImage

    private static final int MSG_PREPARE_RENDERER = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_UPDATE_FLIP_STATE = 5;
    private static final int MSG_QUIT = 6;

    private static final int MSG_SET_FILTER = 7;
    private static final int MSG_SET_CAPTURE_FREQUENCY = 8;

    private static final int MSG_CAPTURE_ENABLED = 11;
    private static final int MSG_RELEASE_RENDERER = 12;
    private static final int MSG_DESTROY_RENDERER = 13;
    private static final int MSG_SET_ZOOM = 14;
    private static final int MSG_UPDATE_CAMERA_PREVIEW_SIZE = 15;

    private static final int MSG_UPDATE_PREVIEW_PARAMS = 21;

    private static final int MSG_RESET_QUEUE = 22;
    private static final int MSG_SET_SWAPPABLE = 23;

    Handler mHandler = null;

    OffscreenSurface mRenderSurface = null;
    private EglCore mEglCore;
    private OpenGLRenderer mGLRenderer;
    private EGLContext mSharedContext;

    protected int mOutgoingWidth = 0;
    protected int mOutgoingHeight = 0;

    private int mDefaultWidth = 0;
    private int mDefaultHeight = 0;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private int mIncomingWidth = 0;
    private int mIncomingHeight = 0;

    private float [] mSTMatrix;
    private long mTimeStamp;

    /*    IntBuffer[] mPixelBuffers;*/

    private int mFrameCount;

    private CameraCaptureCallback mCaptureCallback;
    private boolean mCaptureEnabled;
    private int mCurrentBufferIndex;
    private int mFilter;

    private boolean []mFilterCaptured;

    int mCaptureFrequency = 2;
    private Context mContext;

    // Early releases of marshmallow come with a bug where eglSwapBuffers on PBuffer surface causes deadlocks if context is shared
    // So for now, for marshmallow, we'll just not swap the buffers. And let glReadPixels handle everything.
    boolean mSwappable = false;
    private Renderer mRenderController;

    public CameraCapture(CameraCaptureCallback captureCallback,Context context) {
        mCaptureCallback = captureCallback;
        this.mContext=context;
        // Setting this to true by default.
        // Need to see whether to set it to false and wait for the controller to set it to true
        // Since this is being used to capture the frame rendered to offscreen surface, Need to see what could be the usecase for that.
        // That we need to render and not capture the rendered frame.... What the hell are we rendering it for then! :-|
        mCaptureEnabled = true;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        mHandlerThread = new CustomHandlerThread(this, "CameraCaptureTHread");
        mHandler = mHandlerThread.getHandler();

        // 480x360 is enough for image recognition as well as thumbnail
        // Neither too high to cause lags nor too low for detection algorithms
        mOutgoingWidth = 200;
        mOutgoingHeight = 356;

        mDefaultWidth = 200;
        mDefaultHeight = 356;

        mFilter = 0;
        mFilterCaptured = new boolean[FilterManager.FILTER_COUNT];
        for(int i = 0; i < mFilterCaptured.length; i++) {
            mFilterCaptured[i] = false;
        }
    }

    public void setRenderController(Renderer surfaceRenderer) {
        mRenderController = surfaceRenderer;
    }

    /**
     * Handles encoder state change requests.  The handler is created on the Camera Capture thread.
     */
    private static class CustomHandler extends Handler {
        private WeakReference<CameraCapture> mCameraCaptureWeakRef;

        public CustomHandler(CameraCapture cameraCapture) {
            mCameraCaptureWeakRef = new WeakReference<CameraCapture>(cameraCapture);
        }

        public CustomHandler(CameraCapture cameraCapture, Looper looper) {
            super(looper);
            mCameraCaptureWeakRef = new WeakReference<CameraCapture>(cameraCapture);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            CameraCapture cameraCapturer = mCameraCaptureWeakRef.get();
            if (cameraCapturer == null) {
                Log.w(TAG, "CameraCapture handleMessage: cameraCapturer is null");
                return;
            }

            switch (what) {
                case MSG_PREPARE_RENDERER:
                    cameraCapturer.handlePrepareCameraCapture((EGLContext) obj);
                    break;
                case MSG_FRAME_AVAILABLE:
/*                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    cameraCapturer.handleFrameAvailable((float[]) obj, timestamp);*/
                    cameraCapturer.handleFrameAvailable();
                    break;
                case MSG_CAPTURE_ENABLED:
                    cameraCapturer.handleCaptureEnabled((inputMessage.arg1 != 0));
                    break;
                case MSG_UPDATE_PREVIEW_PARAMS:
                    cameraCapturer.handleSetPreviewParams(inputMessage.arg1, inputMessage.arg2);
                    break;
                case MSG_SET_FILTER:
                    cameraCapturer.handleSetFilter(inputMessage.arg1);
                    break;
                case MSG_SET_CAPTURE_FREQUENCY:
                    cameraCapturer.handleSetCaptureFrequency(inputMessage.arg1);
                    break;
                case MSG_SET_ZOOM:
                    cameraCapturer.handleSetScale(((float) inputMessage.arg1) / 10000);
                    break;
                case MSG_UPDATE_CAMERA_PREVIEW_SIZE:
                    cameraCapturer.handleUpdateCameraPreviewSize(inputMessage.arg1, inputMessage.arg2);
                    break;
                case MSG_RESET_QUEUE:
                    cameraCapturer.handleResetQueue();
                    break;
                case MSG_RELEASE_RENDERER:
                    cameraCapturer.handleRelease();
                    break;
                case MSG_DESTROY_RENDERER:
                    cameraCapturer.handleDestroy();
                    break;
                case MSG_SET_SWAPPABLE:
                    cameraCapturer.handleSetSwappable((inputMessage.arg1 > 0) ? true : false);
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private static class CustomHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CustomHandlerThread(CameraCapture cameraCapture, String tag) {
            super(tag);
            start();
            mHandler = new CustomHandler(cameraCapture, getLooper());
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private void handlePrepareCameraCapture(EGLContext sharedContext) {
        Log.d(TAG, "handlePrepareCameraCapture");
        if(sharedContext != null) {
            mSharedContext = sharedContext;
        }

        mFrameCount = 0;
        mFilter = 0;

        try {
            handleRelease();

            mEglCore = new EglCore(sharedContext, EglCore.FLAG_PBUFFER);
        } catch (EglCore.EGLBadContextException exception) {
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mRenderSurface = new OffscreenSurface(mEglCore, mOutgoingWidth, mOutgoingHeight);
//        mRenderSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mRenderSurface.makeCurrent();

        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.OFFSCREEN, mRenderController);
        mGLRenderer.addExternalSourceDrawable(true);

        mGLRenderer.setCameraParams(mIncomingWidth, mIncomingHeight);
        mGLRenderer.setPreviewRenderParams(mPreviewWidth, mPreviewHeight);
        mGLRenderer.setRenderParams(mOutgoingWidth, mOutgoingHeight);

        LinkedBlockingQueue<Bitmap> [] capturedBitmapQueues = mGLRenderer.prepareCaptureBitmaps();
        mCaptureCallback.setBitmapQueues(capturedBitmapQueues);

    }

    private void handleRelease() {
        if (mRenderSurface != null) {
            mRenderSurface.release();
            mRenderSurface = null;
        }
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    public void handleSetSwappable(boolean swappable) {
        mSwappable = swappable;
    }

    private void handleDestroy() {
        handleRelease();
        mHandler = null;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }

/*        mPixelBuffers[0] = null;
        mPixelBuffers[1] = null;
        mPixelBuffers = null;*/
    }

    private void handleCaptureEnabled(boolean captureEnabled) {
        mCaptureEnabled = captureEnabled;
        mFilter = 0;
        for(int i = 0; i < mFilterCaptured.length; i++) {
            mFilterCaptured[i] = false;
        }
    }

    public void prepareCameraCapture(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PREPARE_RENDERER, mOutgoingWidth, mOutgoingHeight, sharedContext));
    }

    public void setSwappable(boolean swappable) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SWAPPABLE, swappable ? 1 : 0, 0));
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_RENDERER));
    }

    public void destroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_DESTROY_RENDERER));
    }

    private void handleFrameAvailable(/*float [] stMatrix, long timeStamp*/) {

        if (SHOW_FRAME_LOGS && VERBOSE) Log.d(TAG, "handleFrameAvailable ");

        float[] stMatrix;
        long timeStamp;

//        synchronized (this) {
/*        stMatrix = mSTMatrix.clone();
        timeStamp = mTimeStamp;*/
        // If time stamps don't match, that means
//        }

        mFrameCount++;

        // Because first 1 or 2 frames from the camera might be black
//        if ((mCaptureFrequency>1) && (((mFrameCount-(mCaptureFrequency+1)/2) % mCaptureFrequency) != 0)) {
        if ( (mCaptureFrequency>1) && (((mFrameCount-(mCaptureFrequency+1)/2) < 0) || (((mFrameCount-(mCaptureFrequency+1)/2) % mCaptureFrequency) != 0)) ) {
            return;
        }

        if(mGLRenderer == null) {
            Log.d(TAG, "Capture Renderer not yet initialized, Returning!");
            return;
        }

/*        mGLRenderer.setExternalTextureTransformMatrix(stMatrix);
        mGLRenderer.setSurfaceTextureTimestamp(timeStamp);*/

        if(mGLRenderer.getSurfaceTexture() == null) return;
        synchronized (mRenderFence) {
            boolean frameRender = mGLRenderer.drawFrame();
            if (!frameRender) {
                Log.e(TAG, "Skip rendering encoder frame, because renderer faced an issue");
                return;
            }

            GLES20.glFinish();
/*        if(mSwappable) {
            mRenderSurface.swapBuffers();
        }*/
        }

        if(mCaptureEnabled) {
            mCurrentBufferIndex = mFrameCount % 2;
            drainRenderer();
        }
    }

    private void handleSetPreviewParams(int width, int height) {
        int pWidth = mDefaultWidth;
        int pHeight = mDefaultHeight;
        if(width > height) {
            pWidth = mDefaultHeight;
            pHeight = mDefaultWidth;
        }

        mOutgoingWidth = pWidth;
        mOutgoingHeight = pHeight;

        if(mGLRenderer == null) return;

        mGLRenderer.setPreviewRenderParams(width, height);
        mGLRenderer.setRenderParams(mOutgoingWidth, mOutgoingHeight);
    }

    public boolean isCaptureEnabled() {
        return mCaptureEnabled;
    }

    public void setCaptureEnabled(boolean captureEnabled) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_CAPTURE_ENABLED, (captureEnabled ? 1 : 0), 0));
        }
    }

    public void resetBitmapQueue() {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET_QUEUE));
        }
    }

    private void drainRenderer() {
        if(VERBOSE)
            Log.d(TAG, "drain Renderer and send frame to caller");
/*        IntBuffer pixelBuf = mPixelBuffers[mCurrentBufferIndex];*/
        if(mGLRenderer == null) return;

        boolean success = mGLRenderer.readDisplay(/*pixelBuf*/);
        if(!success) {
            Log.w(TAG, "Captured bitmap is NULL, Returning silently");
            return;
        } else {
            int filter = mFilter - 1;
            if (mFilter > 0) {
                if(!mFilterCaptured[filter]) {
                    if (VERBOSE) Log.d(TAG, "Captured FILTER image thumbnail for filter " + filter);
                    mCaptureCallback.processCaptureFrame(filter);
                    mFilterCaptured[filter] = true;
                }
            } else {
                mCaptureCallback.processCaptureFrame();
            }
        }
/*        String foundDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + "/InstaLively/";
        File dir = new File(foundDirPath);
        dir.mkdirs();
        File file = new File(dir, "CVImage" + mFrameCount + ".jpeg");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

/*    private void handleSetPreviewParams(int width, int height) {
        mGLRenderer.setPreviewRenderParams(width, height);
        mGLRenderer.setRenderParams(mEncoderWidth, mEncoderHeight);
    }*/

    private void handleSetFilter(int filter) {
        mFilter = filter;
        if(mGLRenderer != null) {
            if(filter > 0) {
                mGLRenderer.setExternalTextureFilter(filter - 1, true);
            } else {
                mGLRenderer.setExternalTextureFilter(0, true);
            }
        }
    }

    private void handleSetCaptureFrequency(int frequency) {
        mCaptureFrequency = frequency;
    }

    private void handleResetQueue() {
        if (mGLRenderer != null) {
            mGLRenderer.clearCaptureBitmaps();
        }
    }

    private void handleSetScale(float scale) {
        if (mGLRenderer != null) {
            mGLRenderer.setExternalTextureZoom(scale);
        }
    }

    private void handleUpdateCameraPreviewSize(int incomingWidth, int incomingHeight) {
        if(mGLRenderer != null) {
            mGLRenderer.setCameraParams(incomingWidth, incomingHeight);
        }
    }

    public void frameAvailable(float [] stMatrix, long timeStamp) {

//        synchronized (this) {
/*        mSTMatrix = stMatrix;
        mTimeStamp = timeStamp;*/
//        }
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE));
        }
    }

    public void setPreviewRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PREVIEW_PARAMS, mPreviewWidth, mPreviewHeight, null));
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void setCaptureFrequency(int frequency) {
        //ToDo - Change frequency here
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_CAPTURE_FREQUENCY, 1, 0, null));
    }

    public  void setFilter(int filter) {
        if(VERBOSE) Log.d(TAG, "Setting camera capture filter to " + filter);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, filter, 0));
    }

    public void setZoom(float scale) {
        //Multiple scale by 10000 here (since it's a float), but looks like we can only pass an int
        //And on the receiver's side, divide by 10000
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_ZOOM, (int) (scale * 10000), 0, null));
    }


    public void setCameraParams(int incomingWidth, int incomingHeight) {
        mIncomingWidth = incomingWidth;
        mIncomingHeight = incomingHeight;
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_CAMERA_PREVIEW_SIZE, mIncomingWidth, mIncomingHeight));
        }
    }

    static interface CameraCaptureCallback {
//        public void processCaptureFrame(Bitmap bitmap);
        public void processCaptureFrame(int tag);
        public void processCaptureFrame();
        public void setBitmapQueues(LinkedBlockingQueue<Bitmap>  [] bitmapQueues);
    }
}
