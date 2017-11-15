package com.pulseapp.android.gles;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by bajaj on 15/9/15.
 */
public class GlErrorUtil extends GlUtil {

    private static GlUtil mInstance;

    protected GlErrorUtil() {
        super();
    }

    public static GlUtil getInstance() {
        if(mInstance == null) {
            mInstance = new GlErrorUtil();
        }
        return mInstance;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public boolean checkGlError(String op) {
        int error = GLES20.glGetError();
        boolean err = false;
        while (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            error = GLES20.glGetError();
            err = true;
        }
        return err;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public boolean checkGlError(String op, boolean crash) {
        int error = GLES20.glGetError();
        boolean err = false;
        while (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            error = GLES20.glGetError();
            err = true;
//sahil.bajaj@instalively.com
//Don't throw an error on glError. Just print it for debugging and Let the caller handle the rest.
            if(crash) {
                throw new RuntimeException(msg);
            }
        }
        return  err;
    }
}
