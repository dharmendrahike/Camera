package com.pulseapp.android.gles;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by bajaj on 2/12/15.
 */
public class FBO {
    private GlErrorUtil mGLUtil;

    // Used for off-screen rendering.
    private int mOffscreenTexture;
    private int mFramebuffer;
    private int mDepthBuffer;
    private int mOutgoingWidth;
    private int mOutgoingHeight;
    private static final String TAG = "FBO";

    private static final boolean VERBOSE = false;

    /**
     * Prepares the off-screen framebuffer.
     */
    public void prepareFramebuffer(int width, int height) {
        if(VERBOSE) Log.d(TAG, "Preparing Frame Buffer Object");
        mOutgoingWidth = width;
        mOutgoingHeight = height;

        mGLUtil = (GlErrorUtil) GlErrorUtil.getInstance();
        mGLUtil.checkGlError("prepareFramebuffer start");

        int[] values = new int[1];

        // Create a texture object and bind it.  This will be the color buffer.
        GLES20.glGenTextures(1, values, 0);
        mGLUtil.checkGlError("glGenTextures");
        mOffscreenTexture = values[0];   // expected > 0

        for(int i = 0; i < 1; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
            mGLUtil.checkGlError("glBindTexture " + mOffscreenTexture);

            setupTextureParams();

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        mGLUtil.checkGlError("glGenFramebuffers");
        mFramebuffer = values[0];    // expected > 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        mGLUtil.checkGlError("glBindFramebuffer " + mFramebuffer);
        // Create a depth buffer and bind it.
        GLES20.glGenRenderbuffers(1, values, 0);
        mGLUtil.checkGlError("glGenRenderbuffers");
        mDepthBuffer = values[0];    // expected > 0
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer);
        mGLUtil.checkGlError("glBindRenderbuffer " + mDepthBuffer);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height);
        mGLUtil.checkGlError("glRenderbufferStorage");

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, mDepthBuffer);
        mGLUtil.checkGlError("glFramebufferRenderbuffer");

        for(int i = 0; i < 1; i++) {
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
            mGLUtil.checkGlError("glFramebufferTexture2D");
        }

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        mGLUtil.checkGlError("prepareFramebuffer done");
        if(VERBOSE) Log.d(TAG, "Prepare FBO done");
    }

    private void setupTextureParams() {
        // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mOutgoingWidth, mOutgoingHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        mGLUtil.checkGlError("glTexParameter");
    }

    public void release() {
        int[] values = new int[1];
        // Dummy container for holding handles (needed for calling glDelete*)

        if (mOffscreenTexture > 0) {
            values[0] = mOffscreenTexture;
            GLES20.glDeleteTextures(1, values, 0);
            mOffscreenTexture = -1;
        }
        if (mFramebuffer > 0) {
            values[0] = mFramebuffer;
            GLES20.glDeleteFramebuffers(1, values, 0);
            mFramebuffer = -1;
        }
        if (mDepthBuffer > 0) {
            values[0] = mDepthBuffer;
            GLES20.glDeleteRenderbuffers(1, values, 0);
            mDepthBuffer = -1;
        }
        if(VERBOSE) Log.d(TAG, "FBO released");
    }

    public int getTextureId() {
        return mOffscreenTexture;
    }

    public void startDraw() {
        startDraw(0);
    }

    /**
     * index refers to the index of color attachment here... but right now, we're using different fbos
     * instead of binding multiple color attachments to the same fbo
     * @param index
     */
    public void startDraw(int index) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
        // clear buffers
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, mOutgoingWidth, mOutgoingHeight);
        mGLUtil.checkGlError("glBindFramebuffer");
    }

    public void finishDraw() {
        // Blit to display.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int getWidth() {
        return mOutgoingWidth;
    }

    public int getHeight() {
        return mOutgoingHeight;
    }
}
