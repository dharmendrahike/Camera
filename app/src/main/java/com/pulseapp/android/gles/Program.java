/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.pulseapp.android.gles;

import android.opengl.GLES20;
import android.util.Log;

import java.util.HashMap;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Program {
    private static final String TAG = GlUtil.TAG;

    private RenderParams mRenderParams;
    private boolean mUse;

    public int vertices;
    public int texCoords;

    private boolean mHasColorUniform;
    private boolean mHasSampler = false;

    // Handles to the GL program and various components of it.
    public int mProgramHandle;
    private int muMVPMatrixLoc;

    HashMap<String, Integer> mAttributes;
    HashMap<String, Integer> mUniforms;

    private int mColorUniform;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private int muIsLivelyModeLoc;

    private int mTextureTarget;

    private float[] mTexOffset;
    private float mColorAdjust;
    private boolean mIsLivelyMode = false;

    private int generateProgram(String vertexShader, String fragmentShader) {
        mTextureTarget = GLES20.GL_TEXTURE_2D;
        mProgramHandle = Caches.createProgram(vertexShader, fragmentShader);
        if (mProgramHandle == 0) {
            Log.e(TAG, "Error while creating Program");
        } else {
            Log.d(TAG, "Created program " + mProgramHandle);
        }
        return mProgramHandle;
    }

    public Program(String vertexShader, String fragmentShader) {
        mUniforms = new HashMap<String, Integer>();
        mAttributes = new HashMap<String, Integer>();

        generateProgram(vertexShader, fragmentShader);
    }

    /**
     * Releases the program.
     * <p/>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Enables/disables the Lively Mode.  This is used to find adjacent texels when filtering.
     * Recommended for moderate lighting conditions
     * Still need to see, whether this works for extremely low lighting and if not, what does.
     */
    public void setLivelyMode(boolean isLivelyMode) {
        Log.d(TAG, "Setting Lively mode to " + (isLivelyMode ? "TRUE" : "FALSE"));
        mIsLivelyMode = isLivelyMode;
    }

    int addAttrib(final String name) {
        int slot = GLES20.glGetAttribLocation(mProgramHandle, name);
        Caches.mGlUtil.checkGlError("glGetAttribLocation");
        if (slot < 0) {
            Log.e(TAG, "getAttribLocation return -1");
        }
        mAttributes.put(name, slot);
        return slot;
    }

    int bindAttrib(final String name, int bindingSlot) {
        GLES20.glBindAttribLocation(mProgramHandle, bindingSlot, name);
        mAttributes.put(name, bindingSlot);
        return bindingSlot;
    }

    int getAttrib(final String name) {
        if (mAttributes.containsKey(name)) {
            return mAttributes.get(name);
        }
        return addAttrib(name);
    }

    int addUniform(final String name) {
        int slot = GLES20.glGetUniformLocation(mProgramHandle, name);
        Caches.mGlUtil.checkGlError("addUniform :: glGetUniformLocation");
        if (slot < 0) {
            Log.e(TAG, "getUniformLocation return -1 for :: " + name);
        }
        mUniforms.put(name, slot);
        return slot;
    }

    int getUniform(final String name) {
        if (mUniforms.containsKey(name)) {
            return mUniforms.get(name);
        }
        return addUniform(name);
    }

    void setColor(final float r, final float g, final float b, final float a) {
        if (!mHasColorUniform) {
            mColorUniform = getUniform("color");
            mHasColorUniform = true;
        }
        GLES20.glUniform4f(mColorUniform, r, g, b, a);
    }

    void use() {
        GLES20.glUseProgram(mProgramHandle);
        Caches.mGlUtil.checkGlError("use() :: glUseProgram");
        mUse = true;
    }

    boolean isInUse() {
        return mUse;
    }

    void remove() {
        mUse = false;
    }

}
