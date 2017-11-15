package com.pulseapp.android.util;

import android.graphics.Bitmap;

/**
 * Created by abc on 3/28/2016.
 */
public class CapturedMediaController {

    public static CapturedMediaController capturedMediaController;
    private Bitmap mCapturedBitmap = null;

    private CapturedMediaController(){}

    public static CapturedMediaController getInstance(){
        if (capturedMediaController == null){
            capturedMediaController = new CapturedMediaController();
        }
        return capturedMediaController;
    }

    public void setCapturedBitmap(Bitmap bitmap){
        mCapturedBitmap = bitmap;
    }

    public Bitmap getCapturedBitmap(){
        return mCapturedBitmap;
    }

    public void clearBitmap() {
        if(mCapturedBitmap != null) {
            mCapturedBitmap.recycle();
            mCapturedBitmap = null;
        }
    }
}
