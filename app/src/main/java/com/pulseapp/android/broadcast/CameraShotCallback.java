package com.pulseapp.android.broadcast;

import android.graphics.Bitmap;

/**
 * Created by bajaj on 2/3/16.
 */
public interface CameraShotCallback {
    void onBackCapture(Bitmap bitmap,String mediaPath);
    void onFrontCapture(Bitmap bitmap,String mediaPath);
    void onCameraCapture(Bitmap bitmap,String mediaPath);
}
