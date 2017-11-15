/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pulseapp.android.broadcast;

import android.graphics.Bitmap;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.pulseapp.android.gles.Caches;
import com.pulseapp.android.gles.Drawable2d;
import com.pulseapp.android.gles.EglCore;
import com.pulseapp.android.gles.OpenGLRenderer;
import com.pulseapp.android.gles.WindowSurface;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 *     call TextureMovieEncoder#frameAvailable().
 * </ul>
 *
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "TextureMovieEncoder";
    private static final boolean VERBOSE = false;
    private static final boolean SHOW_FRAME_LOGS = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_UPDATE_FLIP_STATE = 5;
    private static final int MSG_QUIT = 6;

    private static final int MSG_SET_FILTER = 7;
    private static final int MSG_SET_ZOOM = 8;
    private static final int MSG_CHANGE_ENCODER = 9;
    private static final int MSG_SET_STAGNANT_FRAME = 10;

    private static final int MSG_UPDATE_PREVIEW_PARAMS = 11;
    private static final int MSG_RESET_DRAWABLE_PROPERTIES = 12;
    private static final int MSG_UPDATE_VIDEO_BACKGROUND = 13;
    private static final int MSG_FLIP_ANIMATION = 14;
    private static final int MSG_UPDATE_CAMERA_PREVIEW_SIZE = 15;

    private static final int MSG_ADD_DRAWABLE = 20;
    private static final int MSG_REMOVE_DRAWABLE = 21;
    private static final int MSG_DESTROY_RENDERER = 22;

    private static final int MSG_SET_BLEND_FACTOR = 31;

    private Callback callback;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private OpenGLRenderer mGLRenderer;
    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    public static final Object mRenderFence = new Object(); // For synchronisation between rendering and updateTexImage

    private boolean mReady;
    private boolean mRunning;
    private long lastTS = 0;
    private long frameCounter = 0;
    private boolean mIsFlipped = false;
    private int mEncoderWidth = 0;
    private int mEncoderHeight = 0;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private long mFrameCount = 0;
    private int mIncomingWidth;
    private int mIncomingHeight;
    private float [] mSTMatrix;
    private long mTimeStamp;

    private int mCurrentFilter;

    private boolean mIsCamera;
    private Renderer mRenderController;
    private Caches mCache;
    private InputStream inputStream;

    private HashMap<Integer,Bitmap> mFilterBitmapHashMap=new HashMap<>();

    public void resetDrawableProperties() {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET_DRAWABLE_PROPERTIES));
        }
    }

    public void destroyRenderer() {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DESTROY_RENDERER));
        }
    }

    public void setRenderController(Renderer surfaceRenderer) {
        mRenderController = surfaceRenderer;
    }

    public static interface Callback {
        void onEncoderCreated();
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     *       with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final int mFrameRate;
        final boolean mIsCamera;
        final EGLContext mEglContext;

        public EncoderConfig(int width, int height, int bitRate, int frameRate,
                EGLContext sharedEglContext, boolean isCamera) {
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mFrameRate = frameRate;
            mIsCamera = isCamera;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " @"+mFrameRate+
                    "fps to '" + "' ctxt=" + mEglContext;
        }
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
        }
    }

    public void notifyPausing() {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
        }
    }
    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        synchronized (mReadyFence) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
                mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
                // We don't know when these will actually finish (or even start).  We don't want to
                // delay the UI thread though, so we return immediately.
            }
        }
    }


    public void changeEncoder(int bitrate) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_CHANGE_ENCODER, bitrate, 0, null));
        }
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        synchronized (mReadyFence) {
            if(mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
            }
        }
    }

    public void setIsStagnantFrame(boolean isStagnantFrame) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_STAGNANT_FRAME, (isStagnantFrame ? 1 : 0), 0));
        }
    }

    public void setBlendFactor(float blendFactor) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_BLEND_FACTOR, (int)(blendFactor * 1024), 0));
        }
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(/*float [] stMatrix, long timeStamp*/) {
        if(VERBOSE) {
            Log.d(TAG, "frameAvailable");
        }
        synchronized (mReadyFence) {
            if (!mReady) {
                if(VERBOSE) {
                    Log.d(TAG, "Encoder not ready yet!");
                }
                return;
            }

/*        synchronized (this) {
            mSTMatrix = stMatrix;
            mTimeStamp = timeStamp;
        }*/
            if (mHandler != null) {
                if(VERBOSE) {
                    Log.d(TAG, "Proceeding to render the frame!");
                }
                mHandler.removeMessages(MSG_FRAME_AVAILABLE);
//        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, (int)((timeStamp >> 32) & 0xFFFFFFFF), (int)(timeStamp & 0xFFFFFFFF), stMatrix));
                mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE));
            } else {
                Log.d(TAG, "Handler is null. So skipping the frame!");
            }
        }
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    public  void setFilter(int filter) {
        mCurrentFilter = filter;
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, filter, 0, null));
        }
    }

    public void addDrawable(int layer, Drawable2d drawable) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_DRAWABLE, layer, 0, drawable));
        }
    }

    public void removeDrawables(int layer) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_REMOVE_DRAWABLE, layer, 0));
        }
    }

/*    public void addBitmapDrawable(int x, int y, Bitmap bitmap) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_DRAWABLE, x, y, bitmap));
        }
    }*/

    public void setZoom(float scale) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        //Multiple scale by 10000 here (since it's a float), but looks like we can only pass an int
        //And on the receiver's side, divide by 10000
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_ZOOM, (int) (scale * 10000), 0, null));
    }

    public void setPreviewRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PREVIEW_PARAMS, width, height));
        }
    }

    public void updateVideoBackground(Bitmap bitmap) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIDEO_BACKGROUND, bitmap));
        }
    }

    public void updateVideoBackground(float [] colors) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_VIDEO_BACKGROUND, colors));
        }
    }

    public void fadeAnimation(Bitmap bitmap, boolean fadeIn) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_FLIP_ANIMATION, (fadeIn ? 1 : 0), 0, bitmap));
        }
    }

    public void setFlipState(boolean flip) {
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_FLIP_STATE, (flip ? 1 : 0), 0));
        }
    }

    public void setCameraParams(int incomingWidth, int incomingHeight) {
        mIncomingWidth = incomingWidth;
        mIncomingHeight = incomingHeight;
        if(mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_CAMERA_PREVIEW_SIZE, incomingWidth, incomingHeight));
        }
    }

    public void resetLifecycle() {
        resetDrawableProperties();
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
/*                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);*/
                    encoder.handleFrameAvailable();
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_SET_FILTER:
                    encoder.handleSetFilter(inputMessage.arg1);
                    break;
                case MSG_SET_ZOOM:
                    encoder.handleSetScale(((float) inputMessage.arg1) / 10000);
                    break;
                case MSG_SET_STAGNANT_FRAME:
                    encoder.handleSetStagnantFrame(inputMessage.arg1 > 0);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_UPDATE_PREVIEW_PARAMS:
                    encoder.handleSetPreviewParams(inputMessage.arg1, inputMessage.arg2);
                    break;
                case MSG_RESET_DRAWABLE_PROPERTIES:
                    encoder.handleResetDrawableProperties();
                    break;
                case MSG_UPDATE_VIDEO_BACKGROUND:
                    if(inputMessage.obj instanceof Bitmap) {
                        encoder.handleUpdateVideoBackground((Bitmap) inputMessage.obj);
                    } else {
                        encoder.handleUpdateVideoBackground((float[]) inputMessage.obj);
                    }
                    break;
                case MSG_FLIP_ANIMATION:
                    encoder.handleFlipAnimation((Bitmap)inputMessage.obj, (inputMessage.arg1 > 0) ? true : false);
                    break;
                case MSG_UPDATE_FLIP_STATE:
                    encoder.handleSetFlip((inputMessage.arg1 > 0) ? true : false);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                case MSG_DESTROY_RENDERER:
                    encoder.handleDestroyRenderer();
                    break;
                case MSG_UPDATE_CAMERA_PREVIEW_SIZE:
                    encoder.handleUpdateCameraPreviewSize(inputMessage.arg1, inputMessage.arg2);
                    break;
                case MSG_CHANGE_ENCODER:
                    encoder.changeEncoderSettings(inputMessage.arg1);
                    break;
                case MSG_ADD_DRAWABLE:
                    encoder.handleAddDrawable(inputMessage.arg1, ((Drawable2d) inputMessage.obj));
                    break;
                case MSG_REMOVE_DRAWABLE:
                    encoder.handleRemoveDrawable(inputMessage.arg1);
                    break;
                case MSG_SET_BLEND_FACTOR:
                    encoder.handleSetBlendFactor(inputMessage.arg1);
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleDestroyRenderer() {
        if(mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
    }

    private void handleSetPreviewParams(int width, int height) {
        Log.w(TAG, "setPreviewParams: " + " width: " + width + " height: " + height);
        Log.w(TAG, "Encoder Params: " + " width: " + mEncoderWidth + " height: " + mEncoderHeight);

        if(mGLRenderer != null) {
            mGLRenderer.setPreviewRenderParams(width, height);
            mGLRenderer.setRenderParams(mEncoderWidth, mEncoderHeight);
        }
    }

    private void handleResetDrawableProperties() {
        mGLRenderer.resetDrawableProperties();
    }

    private void handleUpdateVideoBackground(Bitmap bitmap) {
        if(mGLRenderer != null) {
            mGLRenderer.updateVideoBackground(bitmap);
        }
    }

    private void handleUpdateVideoBackground(float [] colors) {
        if(mGLRenderer != null) {
            mGLRenderer.updateVideoBackground(colors);
        }
    }

    private void handleFlipAnimation(Bitmap bitmap, boolean fadeIn) {
        if(!fadeIn) {
            mGLRenderer.fadeOutAnimation(bitmap);
        } else {
            mGLRenderer.fadeInAnimation();
        }
    }

    public void setFilterBitmap(HashMap<Integer,Bitmap> filterBitmapHashMap, InputStream inputStream){
        mFilterBitmapHashMap=filterBitmapHashMap;
        this.inputStream=inputStream;
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mFrameNum = 0;
        mFrameCount = 0;
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate, config.mFrameRate, config.mIsCamera);
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     */
    private void handleFrameAvailable(/*float [] stMatrix, long timeStamp*/) {

        float[] stMatrix;
        long timeStamp;

/*        synchronized (this) {
            stMatrix = mSTMatrix.clone();
            timeStamp = mTimeStamp;
        // If time stamps don't match, that means
        }*/

        if (VERBOSE && SHOW_FRAME_LOGS) Log.d(TAG, "handleFrameAvailable " + mFrameCount);
        mFrameCount++;
        mVideoEncoder.drainEncoder(false);
        if (mGLRenderer == null) return;

//        mGLRenderer.setExternalTextureTransformMatrix(stMatrix);
//        mGLRenderer.setSurfaceTextureTimestamp(timeStamp);

        if (mGLRenderer.getSurfaceTexture() == null) {
            Log.w(TAG, "Returning without rendering. SurfaceTexture is NULL");
            return;
        }
        synchronized (mRenderFence) {
            mGLRenderer.setFilterBitmapRenderer(mFilterBitmapHashMap,inputStream);
            boolean frameRender = mGLRenderer.drawFrame();
            if (!frameRender) {
                Log.e(TAG, "Skip rendering encoder frame, because renderer faced an issue");
                return;
            }

            if(VERBOSE) {
                Log.d(TAG, "Rendering Timestamp: " + mCache.mSurfaceTextureTimestamp);
            }
            mInputWindowSurface.setPresentationTime(mCache.mSurfaceTextureTimestamp);
            try {
                if(MoviePlayer.mRenderLock != null) {
                    synchronized (MoviePlayer.mRenderLock) {
                        MoviePlayer.mRenderLock.notify();
                    }
                }
            } catch (IllegalMonitorStateException e) {
                e.printStackTrace();
            }
            mInputWindowSurface.swapBuffers();
        }
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        //Log.d(TAG, "handleSetTexture " + id);
        mTextureId = id;
    }

    private void handleSetFilter(int filter) {
        if(mGLRenderer != null) {
            mGLRenderer.setExternalTextureFilter(filter, true);
        }
    }

    private void handleSetFlip(boolean flip) {
        mIsFlipped = flip;
        if(mGLRenderer != null) {
            mGLRenderer.setExternalTextureFlip(mIsFlipped);
        }
    }

    private void handleSetScale(float scale) {
        if(mGLRenderer != null) {
            mGLRenderer.setExternalTextureZoom(scale);
        }
    }

    private void handleSetStagnantFrame(boolean isStagnantFrame) {
        if(mGLRenderer != null) {
            mGLRenderer.setIsStagnantFrame(isStagnantFrame);
        }
    }

    private void handleUpdateCameraPreviewSize(int incomingWidth, int incomingHeight) {
        if(mGLRenderer != null) {
            mGLRenderer.setCameraParams(incomingWidth, incomingHeight);
        }
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        if(mInputWindowSurface != null) {
            mInputWindowSurface.releaseEglSurface();
        }

        if(mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }

        if (mEglCore != null) {
            mEglCore.release();
        }
        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE|EglCore.FLAG_PBUFFER);

        if(mInputWindowSurface!=null)
            mInputWindowSurface.recreate(mEglCore);
        else
            mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);

        mInputWindowSurface.makeCurrent();

        // Create a separater GLRenderer for the video encoder surface
        // And give it hint so that it knows what to do special about it
        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.VIDEO, mRenderController);
        mCache = mGLRenderer.getCacheInstance();
//        mGLRenderer.setRenderer(mRenderController);
        mGLRenderer.addExternalSourceDrawable(true);

        handleUpdateCameraPreviewSize(mIncomingWidth, mIncomingHeight);
        handleSetPreviewParams(mPreviewWidth, mPreviewHeight);
        mVideoEncoder.resync(); //Convey to encoder that the next frame would be from the new surfaceTexture
    }

    private void handleAddDrawable(int layer, Drawable2d drawable) {
        float aspectRatio = (float) mEncoderWidth / mEncoderHeight;
        float previewAspectRatio = (float) mPreviewWidth / mPreviewHeight;
        if(mGLRenderer != null) {
            float scale = mGLRenderer.getVerticalVideoScale();
            if(scale != 1.0f) {
                drawable.multiplyScale(scale * previewAspectRatio, scale * previewAspectRatio, 1.0f);
                drawable.multiplyTranslate(1 / aspectRatio / aspectRatio, scale, 1.0f);
            } else {
                drawable.multiplyTranslate(1.0f, 1.0f/previewAspectRatio, 1.0f);
            }
            if(layer == OpenGLRenderer.DRAWABLES_ANIMATABLE) {
                mGLRenderer.addAnimDrawable(drawable);
            } else {
                mGLRenderer.addDrawable(layer, drawable);
            }
        }
    }

    private void handleRemoveDrawable(int layer) {
        if (mGLRenderer != null) {
            mGLRenderer.removeDrawables(layer);
        }
    }

    private void handleSetBlendFactor(int blendFactor) {
        if(mGLRenderer != null) {
            mGLRenderer.setBlendFactor((float)blendFactor/1024);
        }
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate, int frameRate, boolean isCamera) {
        try {
            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, frameRate);
            if(this.callback != null) {
                this.callback.onEncoderCreated();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        mEncoderWidth = mVideoEncoder.getWidth();
        mEncoderHeight = mVideoEncoder.getHeight();

        try {
            mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        } catch (EglCore.EGLBadContextException exception) {
            Log.w(TAG, "EGL Bad context from the previous instance");
            return;
        }

        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();
        // Create a separater GLRenderer for the video encoder surface
        // And give it hint so that it knows what to do special about it
        if(mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }

        mIsCamera = isCamera;
        mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.VIDEO, mRenderController);
        mCache = mGLRenderer.getCacheInstance();

        mGLRenderer.addExternalSourceDrawable(mIsCamera);
        mGLRenderer.setExternalTextureFilter(mCurrentFilter, true);
        handleUpdateCameraPreviewSize(mIncomingWidth, mIncomingHeight);
        handleSetPreviewParams(mPreviewWidth, mPreviewHeight);
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if(mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        final int width = mInputWindowSurface.getWidth();
        int xpos = (posn * 4) % (width - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, 100, 100);
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private void changeEncoderSettings(int bitrate)
    {
        if(mVideoEncoder!=null)
            mVideoEncoder.changeBitrate(bitrate);
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public VideoEncoderCore getEncoder() {
        return  mVideoEncoder;
    }
}
