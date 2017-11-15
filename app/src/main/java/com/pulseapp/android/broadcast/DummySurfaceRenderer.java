package com.pulseapp.android.broadcast;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.pulseapp.android.gles.Caches;
import com.pulseapp.android.gles.EglCore;
import com.pulseapp.android.gles.OffscreenSurface;
import com.pulseapp.android.gles.OpenGLRenderer;

import java.lang.ref.WeakReference;

/**
 * Created by bajaj on 15/1/16.
 */
public class DummySurfaceRenderer {
    private static final String TAG = "DummySurfaceRenderer";
    private CustomHandlerThread mHandlerThread = null;

    private boolean VERBOSE = false;
    private boolean SHOW_FRAME_LOGS = false;

    private Object mUpdateTextureFence = new Object(); // For synchronisation between rendering and updateTexImage

    private static final int MSG_PREPARE_RENDERER = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 3;

    private static final int MSG_PAUSE_UPDATE = 4;
    private static final int MSG_RELEASE_RENDERER = 5;
    private static final int MSG_CHANGE_RECORDING_STATE = 6;
    private static final int MSG_NOTIFY_PAUSING = 7;

    private Object mNotifyPausingFence;
    private volatile boolean mNotifyPaused;

    Handler mHandler = null;

    float [] mSTMatrix = new float[16];

    OffscreenSurface mRenderSurface = null;
    private EglCore mEglCore;
    //        private OpenGLRenderer mGLRenderer;
    private EGLContext mSharedContext;

    private int mOutgoingWidth = 0;
    private int mOutgoingHeight = 0;

    private int mDefaultWidth = 0;
    private int mDefaultHeight = 0;

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private int mIncomingWidth = 0;
    private int mIncomingHeight = 0;

    private long mTimeStamp;

    /*    IntBuffer[] mPixelBuffers;*/

    private int mFrameCount;

    //        private CameraCaptureCallback mCaptureCallback;
    private boolean mCaptureEnabled;
    private int mCurrentBufferIndex;
    private int mFilter;

    private boolean []mFilterCaptured;

    int mCaptureFrequency = 2;

    // Early releases of marshmallow come with a bug where eglSwapBuffers on PBuffer surface causes deadlocks if context is shared
    // So for now, for marshmallow, we'll just not swap the buffers. And let glReadPixels handle everything.
    boolean mSwappable = false;

    private RenderCallback mRenderCallback;
    Caches mCache;

    private boolean mUpdate;
    private boolean mUseOwnContext;
    private boolean mNotifyPausing;
    private Renderer mRenderController;

    public DummySurfaceRenderer() {

        // Setting this to true by default.
        // Need to see whether to set it to false and wait for the controller to set it to true
        // Since this is being used to capture the frame rendered to offscreen surface, Need to see what could be the usecase for that.
        // That we need to render and not capture the rendered frame.... What the hell are we rendering it for then! :-|
        mCaptureEnabled = true;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        mUpdate = true;
        mHandlerThread = new CustomHandlerThread(this, "DummySurfaceRendererTHread");
        mHandler = mHandlerThread.getHandler();

        //1x1 frame buffer just enough to be able to create a context :D
        mOutgoingWidth = 1;
        mOutgoingHeight = 1;

        mDefaultWidth = 1;
        mDefaultHeight = 1;

        mFilter = 0;
        mFilterCaptured = new boolean[FilterManager.FILTER_COUNT];
        for(int i = 0; i < mFilterCaptured.length; i++) {
            mFilterCaptured[i] = false;
        }

        mNotifyPausingFence = new Object();
        mNotifyPausing = false;
        mNotifyPaused = false;
    }

    public void setRenderController(RenderCallback renderCallback) {
        mRenderCallback = renderCallback;
        mRenderController = (Renderer) renderCallback;
    }

    public void changeRecordingState(boolean recordingState) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_CHANGE_RECORDING_STATE, (recordingState ? 1 : 0), 0));
    }

    public void notifyPausing() {
        if(mNotifyPausing) return;
        mNotifyPaused = false;
        mNotifyPausing = true;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_NOTIFY_PAUSING));
        synchronized (mNotifyPausingFence) {
            while (!mNotifyPaused) {
                try {
                    mNotifyPausingFence.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mNotifyPausing = false;
        Log.d(TAG, "notifyPausing successful");
    }

    /**
     * Handles encoder state change requests.  The handler is created on the Camera Capture thread.
     */
    private static class CustomHandler extends Handler {
        private WeakReference<DummySurfaceRenderer> mCameraCaptureWeakRef;
        private boolean VERBOSE = false;

        public CustomHandler(DummySurfaceRenderer surfaceRenderer) {
            mCameraCaptureWeakRef = new WeakReference<DummySurfaceRenderer>(surfaceRenderer);
        }

        public CustomHandler(DummySurfaceRenderer dummyRenderer, Looper looper) {
            super(looper);
            mCameraCaptureWeakRef = new WeakReference<DummySurfaceRenderer>(dummyRenderer);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            DummySurfaceRenderer surfaceRenderer= mCameraCaptureWeakRef.get();
            if (surfaceRenderer == null) {
                Log.w(TAG, "DummySurfaceRenderer handleMessage: surfaceRenderer is null");
                return;
            }

            switch (what) {
                case MSG_PREPARE_RENDERER:
                    surfaceRenderer.handlePrepareCameraCapture((EGLContext) obj);
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timeStampNs = (((long)inputMessage.arg1) << 32) | ((long)inputMessage.arg2 & 0xffffffffL);
//                    long timeStampNs = (((long)inputMessage.arg1) << INT_BITS) | (long)inputMessage.arg2;
                    if (VERBOSE)
                        Log.d(TAG, "Renderer: timestamp received is: " + timeStampNs);
                    surfaceRenderer.handleFrameAvailable(timeStampNs);
                    break;
                case MSG_PAUSE_UPDATE:
//                        surfaceRenderer.handlePauseUpdates(inputMessage.arg1);
                    break;
                case MSG_RELEASE_RENDERER:
                    surfaceRenderer.handleRelease();
                    break;
                case MSG_CHANGE_RECORDING_STATE:
                    surfaceRenderer.handleChangeRecordingState(inputMessage.arg1 > 0);
                    break;
                case MSG_NOTIFY_PAUSING:
                    surfaceRenderer.handleNotifyPausing();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleChangeRecordingState(boolean recordingState) {
        mRenderCallback.changeRecordingState(recordingState);
    }

    private void handleNotifyPausing() {
        mRenderCallback.handleNotifyPausing();
        mNotifyPaused = true;
        synchronized (mNotifyPausingFence) {
            mNotifyPausingFence.notifyAll();
        }
    }

    public EGLContext getContext() {
        return mSharedContext;
    }

    private void handlePauseUpdates(int update) {
        if(update > 0) {
            mUpdate = false;
        } else {
            mUpdate = true;
        }
    }

    public void pauseUpdates(boolean update) {
        if(update) {
            mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MSG_PAUSE_UPDATE, (update ? 1 : 0), 0));
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_PAUSE_UPDATE, (update ? 1 : 0), 0));
        }
    }

    private static class CustomHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CustomHandlerThread(DummySurfaceRenderer dummyRenderer, String tag) {
            super(tag);
            start();
            mHandler = new CustomHandler(dummyRenderer, getLooper());
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private void handlePrepareCameraCapture(EGLContext sharedContext) {
        Log.d(TAG, "handlePrepareCameraCapture");
        if(sharedContext != null) {
            mSharedContext = sharedContext;
            mUseOwnContext = false;
        } else {
            mUseOwnContext = true;
            mSharedContext = EGL14.EGL_NO_CONTEXT;
        }

        mFrameCount = 0;
        mFilter = 0;

        try {
            handleRelease();

            mEglCore = new EglCore(sharedContext, EglCore.FLAG_PBUFFER);
            if(mUseOwnContext) {
                mSharedContext = mEglCore.getContext();
            }
        } catch (EglCore.EGLBadContextException exception) {
            exception.printStackTrace();
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mRenderSurface = new OffscreenSurface(mEglCore, mOutgoingWidth, mOutgoingHeight);
        mRenderSurface.makeCurrent();

        mUpdate = true;
        mRenderCallback.contextReady(mSharedContext);
        mCache = OpenGLRenderer.getCacheInstance(mRenderController);
    }

    private void handleRelease() {
        if (mRenderSurface != null) {
            mRenderSurface.release();
            mRenderSurface = null;
        }
/*            if (mGLRenderer != null) {
                mGLRenderer.destroy();
                mGLRenderer = null;
            }*/
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    private void handleDestroy() {
        handleRelease();
        mHandler = null;

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
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

    private void handleFrameAvailable() {
        handleFrameAvailable(-1);
    }

    private void handleFrameAvailable(long timeStampNs) {
        if (SHOW_FRAME_LOGS && VERBOSE) Log.d(TAG, "handleFrameAvailable ");
        if(!mUpdate) {
            Log.d(TAG, "Returning from handleFrameAvailable without updateTexImage");
        } else {
//            Log.d(TAG, "Calling updateTexImage");
            synchronized (CameraCapture.mRenderFence) {
                //TODO CameraSurfaceRenderer to be replaced by SurfaceRenderer even for live camera
                synchronized (CameraSurfaceRenderer.mRenderFence) {
                    synchronized (SurfaceRenderer.mRenderFence) {
                        synchronized (TextureMovieEncoder.mRenderFence) {
                            SurfaceTexture surfaceTexture = mCache.getSurfaceTexture();
                            // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
                            // was there before.
                            try {
                                surfaceTexture.updateTexImage();
                            } catch (Exception e) {
                                Log.e(TAG, "updateTexImage failed");
                                e.printStackTrace();
                                return;
                            }
                            if(timeStampNs >= 0l) {
                                mCache.mSurfaceTextureTimestamp = timeStampNs;
//                                OpenGLRenderer.setSurfaceTextureTimestamp(timeStampNs);
                            } else {
                                mCache.mSurfaceTextureTimestamp = surfaceTexture.getTimestamp();
 //                               OpenGLRenderer.setSurfaceTextureTimestamp(surfaceTexture.getTimestamp());
                            }
                            surfaceTexture.getTransformMatrix(mSTMatrix);
                            mCache.mSurfaceTextureTransform = mSTMatrix;
//                            OpenGLRenderer.setExternalTextureTransformMatrix(mRenderController, mSTMatrix);
                        }
                    }
                }
            }
        }
        //Now is the time to trigger rendering on all threads responsible for rendering to various surfaces.
        mRenderCallback.textureAvailable();
    }

    public boolean isCaptureEnabled() {
        return mCaptureEnabled;
    }

/*        public void setCaptureEnabled(boolean captureEnabled) {
            if(mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CAPTURE_ENABLED, (captureEnabled ? 1 : 0), 0));
            }
        }*/

    public void frameAvailable() {
        frameAvailable(-1);
    }

    private static final int INT_BITS = 32;
    private static final int INT_MASK = 0xFFFFFFFF;

    public void frameAvailable(long timeStampNs) {
        if (VERBOSE)
            Log.d(TAG, "Renderer: timestamp sent is: " + timeStampNs);
        if(mHandler.hasMessages(MSG_FRAME_AVAILABLE)) {
            Log.d(TAG, "Still has frame message in queue... bad sign");
        }
        mHandler.removeMessages(MSG_FRAME_AVAILABLE);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int)(timeStampNs >> 32), (int)timeStampNs));
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void clearPendingFrameQueue() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public void release() {
        mUpdate = false;

        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_RENDERER));
    }

    public interface RenderCallback {
        void contextReady(EGLContext eglContext);
        void textureAvailable();

        void changeRecordingState(boolean recordingState);

        void handleNotifyPausing();
    }
}

