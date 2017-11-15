package com.pulseapp.android.gles;

/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

/**
 * Created by bajaj on 23/1/16.
 */
public class OutputSurface {
    /**
     * Holds state associated with a Surface used for MediaCodec decoder output.
     * <p>
     * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
     * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
     * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
     * texture with updateTexImage, then render the texture with GL to a pbuffer.
     * <p>
     * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
     * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
     * we just draw it on whatever surface is current.
     * <p>
     * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
     * can potentially drop frames.
     */
    private static final String TAG = "OutputSurface";
    private static final boolean VERBOSE = false;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;
    /**
     *
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
    public OutputSurface(SurfaceTexture surfaceTexture, EGLContext eglContext) {
        mSurfaceTexture = surfaceTexture;
        mEGLContext = eglContext;
//        eglSetup(800, 800);
//        makeCurrent();
        setup();
    }

    /**
     * Creates an OutputSurface using the current EGL context.  Creates a Surface that can be
     * passed to MediaCodec.configure().
     */
    public OutputSurface(SurfaceTexture surfaceTexture) {
        setup();
    }
    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private void setup() {
        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        // This doesn't work if OutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        mSurface = new Surface(mSurfaceTexture);
    }

    /**
     * Clears the playback surface to black.
     */
    private void clearSurface(Surface surface) {
        // We need to do this with OpenGL ES (*not* Canvas -- the "software render" bits
        // are sticky).  We can't stay connected to the Surface after we're done because
        // that'd prevent the video encoder from attaching.
        //
        // If the Surface is resized to be larger, the new portions will be black, so
        // clearing to something other than black may look weird unless we do the clear
        // post-resize.
        EglCore eglCore = new EglCore();
        WindowSurface win = new WindowSurface(eglCore, surface, false);
        win.makeCurrent();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        win.swapBuffers();
        win.release();
        eglCore.release();
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private void eglSetup(int width, int height) {
//        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + version + " EGLConfig");
            return;
        }
        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        if(mEGLContext == null) {
            mEGLContext = EGL14.EGL_NO_CONTEXT;
        }
        android.opengl.EGLContext context = EGL14.eglCreateContext(mEGLDisplay, configs[0], mEGLContext,
                attrib_list, 0);
        checkEglError("eglCreateContext");
        mEGLContext = context;
        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }
    }

    public void partialRelease() {
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (EGL14.eglGetCurrentContext().equals(mEGLContext)) {
            // Clear the current context and surface to ensure they are discarded immediately.
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
        }
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        if (EGL14.eglGetCurrentContext().equals(mEGLContext)) {
            // Clear the current context and surface to ensure they are discarded immediately.
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
        }
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
        //mEGL.eglTerminate(mEGLDisplay);
        mSurface.release();
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();
        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
        mSurface = null;
        mSurfaceTexture = null;
    }
    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        checkEglError("before makeCurrent");
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }
    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }

        mSurfaceTexture.updateTexImage();
    }

    /**
     * Checks for EGL errors.
     */
    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }
}