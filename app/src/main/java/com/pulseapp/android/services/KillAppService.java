package com.pulseapp.android.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Karthik on 1/26/16.
 */
public class KillAppService extends Service {

    private final String TAG = "KillAppService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("KillAppService", "Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("KillAppService", "Service Destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("KillAppService", "END");
//        CameraActivity.stopBroadcastOnLiveStartingSilently(); //If app is killed in livestarting state
//        CameraActivity.stopBroadcastOnLiveSilently(); //If app is killed in live state
//        NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.cancelAll();
//        Intent broadcast = new Intent();
//        broadcast.setPackage("com.instalively.android");
//        broadcast.setClass(getApplicationContext(),ScreenshotReceiver.class);
//        broadcast.putExtra("Dead", true); //Random extras
//        sendBroadcast(broadcast);
        stopSelf();
    }
}