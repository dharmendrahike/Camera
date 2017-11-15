package com.pulseapp.android.util;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.pulseapp.android.MasterClass;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.signals.BroadCastSignals;

/**
 * Created by admin on 1/29/2015.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private boolean registerForEvents = false;

    public abstract void onEvent(BroadCastSignals.BaseSignal eventSignal);

    protected void registerForInAppSignals(boolean flag) {
        this.registerForEvents = flag;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.registerForEvents && this instanceof CameraActivity) {
            MasterClass.getEventBus().register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        if (this.registerForEvents && this instanceof CameraActivity) {
            MasterClass.getEventBus().unregister(this);
        }
        super.onDestroy();
    }

    private static final int PERMISSION_ACCESS_CAMERA_MICROPHONE = 0;

    private boolean processingPermissions = false;

    /**
     * The method will ignore any further calls until the permissions are accepted/rejected by the system.
     * After which It will it will either take us to the settings or call {@link #onPermissionsGranted(String[])}
     */
    protected void requestCameraPermissionsAndProceed() {
        if (processingPermissions) return;
        processingPermissions = true;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            if (currentPopCount < MAX_POPUP_ATTEMPTS) {
                ++currentPopCount;
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                        PERMISSION_ACCESS_CAMERA_MICROPHONE);
            } else {
                takeMeToSettings();
            }
        } else {
            this.onPermissionsGranted(null);
        }
    }

    private final String TAG = BaseActivity.class.getSimpleName();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ");
        String s = "", g = "";
        for (String permission : permissions) {
            s += permission + " ";
        }
        for (int grantResult : grantResults) {
            g += grantResult + " ";
        }
        Log.d(TAG, "onRequestPermissionsResult: permissions " + s + " grantResult " + g);
        switch (requestCode) {
            case PERMISSION_ACCESS_CAMERA_MICROPHONE:
                processingPermissions = false;
                if (AppLibrary.verifyPermissions(grantResults)) {
                    this.onPermissionsGranted(null);
                } else {
                    requestCameraPermissionsAndProceed();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private final static int MAX_POPUP_ATTEMPTS = 3;
    private int currentPopCount = 0;


    private void takeMeToSettings() {
        Toast.makeText(this, "Please provide Camera and Audio permissions in the settings ", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * @param permissions currently not being used as the method is only called
     *                    when {@link #android.Manifest.permission.CAMERA} &
     *                    {@link #Manifest.permission.RECORD_AUDIO} are granted
     */
    protected abstract void onPermissionsGranted(String[] permissions);
}
