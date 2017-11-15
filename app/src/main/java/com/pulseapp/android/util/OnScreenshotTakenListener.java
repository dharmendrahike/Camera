package com.pulseapp.android.util;

import android.net.Uri;

import java.util.HashMap;

/**
 * Created by user on 5/27/2016.
 */
public interface OnScreenshotTakenListener {
    void onScreenshotTaken(HashMap<String,String> mediaDetails);
}
