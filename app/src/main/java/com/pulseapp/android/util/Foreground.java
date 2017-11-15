package com.pulseapp.android.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by Karthik on 7/6/16.
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

    private static Foreground instance;
    private static int mScreensCounter = 0;
    private static String TAG = "Foreground";

    public static void init(Application app){
        if (instance == null){
            instance = new Foreground();
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static Foreground get() {
        return instance;
    }

    public static int getScreenCounter(){
        return mScreensCounter;
    }

    private Foreground(){}

    @Override
    public void onActivityDestroyed(Activity activity) {
        mScreensCounter--;

        if (mScreensCounter == 0) {
            //... Application is Off
            AppLibrary.log_d(TAG,"Application killed");
        }

        if (mScreensCounter < 0) {
            mScreensCounter = 0;
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mScreensCounter++;
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }
}