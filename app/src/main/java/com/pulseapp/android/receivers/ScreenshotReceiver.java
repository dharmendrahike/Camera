package com.pulseapp.android.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pulseapp.android.uploader.mediaUpload;

/**
 * Created by Karthik on 1/25/16.
 */
public class ScreenshotReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenshotReceiver";

    private NotificationManager mNotificationManager;

    public ScreenshotReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when this BroadcastReceiver receives an Intent broadcast.
        Log.e(TAG, "Screenshot Received");
    }
}

