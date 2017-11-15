package com.pulseapp.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.util.Pair;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.util.AppLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Vaibhav on 03/09/16.
 */
public class AnalyticsManager {

    private static AnalyticsManager mInstance;
    private final String MIXPANEL_PROJECT_TOKEN = "8305d909c22a4d3a38bce59bbe6fbdb7";

    private MixpanelAPI mMixPanel;
    private FirebaseAnalytics mFirebaseAnalytics;

    private Boolean mShouldRun = null;

    private AnalyticsManager(){}

    public static AnalyticsManager getInstance(){
        if (mInstance == null){
            mInstance = new AnalyticsManager();
        }
        return mInstance;
    }

    public void init(Context appContext){
        if (!shouldRunAnalytics())
            return;

        mMixPanel = MixpanelAPI.getInstance(appContext, MIXPANEL_PROJECT_TOKEN);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(appContext);
    }

    public void flush(){
        if (mMixPanel != null){
            mMixPanel.flush();
        }
    }

    public void trackEvent (String eventName, ArrayList<Pair<String, String>> keyValProperties) {
        if (!shouldRunAnalytics())
            return;

        if (mMixPanel != null && mFirebaseAnalytics != null){
            Bundle propBundle = null;
            CustomEvent answersCustomEvent = new CustomEvent(eventName);
            if (keyValProperties != null) {
                JSONObject properties = null;
                for (Pair<String, String> keyVal : keyValProperties){
                    if (propBundle == null)
                        propBundle = new Bundle();
                    propBundle.putString(keyVal.first, keyVal.second);
                    answersCustomEvent.putCustomAttribute(keyVal.first, keyVal.second);
                    try {
                        if (properties == null)
                            properties = new JSONObject();
                        properties.put(keyVal.first, keyVal.second);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (properties != null){
                    mMixPanel.track(eventName, properties);
                    Answers.getInstance().logCustom(new CustomEvent(eventName));
                }
            }
            else {
                mMixPanel.track(eventName);
            }
            Answers.getInstance().logCustom(answersCustomEvent);
//            mFirebaseAnalytics.logEvent(eventName, propBundle);
        }
    }

    /**
     *
     * @param eventName
     * @param keyValProperties Accepts key value pairs for the data to be associated with the given event name, can be left empty
     */
    public void trackEvent(String eventName, String... keyValProperties){
        if (!shouldRunAnalytics())
            return;

        if (mMixPanel != null && mFirebaseAnalytics != null){
            // This variable is used to keep track of what is key and what is value in properties
            boolean isKey = true;
            String key = null;
            ArrayList<Pair<String, String>> keyValPairs = null;

            for (String prop : keyValProperties){
                if (isKey) {
                    key = prop;
                    isKey = false;
                }
                else {
                    if (keyValPairs == null)
                        keyValPairs = new ArrayList<>();
                    keyValPairs.add(new Pair(key, prop));
                    isKey = true;
                    key = null;
                }
            }
            trackEvent(eventName, keyValPairs);
        }
    }

    public void startEventTimer(String eventName){
        if (mMixPanel != null){
            mMixPanel.timeEvent(eventName);
        }
    }

    public void stopEventTimer(String eventName){
        trackEvent(eventName);
    }

    public void createUserAfterSignUp(Context context, String handleName, String gender,SettingsModel model){ // On first-time login
        if (mMixPanel != null){
            mMixPanel.alias(handleName, null);
            mMixPanel.getPeople().identify(handleName);
            mMixPanel.getPeople().set("$name",handleName);
            mMixPanel.getPeople().set("Gender", gender);
            mMixPanel.getPeople().set("Handle", handleName);
            if (model!=null && model.birthday!=null)
                mMixPanel.getPeople().set("Birthday", model.birthday);

            SharedPreferences prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
            prefs.edit().putBoolean(AppLibrary.ANALYTICS_IDENTIFIED, true).commit();
        }
    }

    public void identifyUserWithHandleName(Context context, String handleName){ // on second-time login
        if (mMixPanel != null){
            mMixPanel.identify(handleName);
            mMixPanel.getPeople().identify(handleName);
            mMixPanel.getPeople().set("$name",handleName);

            SharedPreferences prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
            prefs.edit().putBoolean(AppLibrary.ANALYTICS_IDENTIFIED, true).commit();
        }
    }

    private boolean shouldRunAnalytics(){
        if (mShouldRun == null) {
            if (BuildConfig.BUILD_TYPE.equals("release"))
                mShouldRun = true;
            else
                mShouldRun = false;
        }
        return mShouldRun;
    }

}
