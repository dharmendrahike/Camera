package com.pulseapp.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.appsflyer.AppsFlyerLib;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.activities.FacebookLogin;
import com.pulseapp.android.activities.OnBoardingActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BaseActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Karthik on 2/29/2016
 */
public class Splash extends BaseActivity implements FireBaseKEYIDS {

    private static final String TAG = "I/Splash";

    private SharedPreferences prefs;
    public static boolean isCameraOpenChecker;
    public static boolean isActivityLaunched;
    private static final int PERMISSION_ACCESS_CAMERA_MICROPHONE = 0;
    private static FireBaseHelper fireBaseHelper;
    Intent intent;
    CallbackManager callbackManager;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @SuppressLint("CommitPrefEdits")
    @SuppressWarnings("PointlessBooleanExpression")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppsFlyerLib.getInstance().startTracking(getApplication(),"Vw8FQXJGNRvnHq2QzLnnME");
        AppsFlyerLib.getInstance().setUseHTTPFalback(true);
        AppsFlyerLib.getInstance().sendDeepLinkData(this);

        /*AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_OPEN);*/

        isCameraOpenChecker = true;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            setContentView(R.layout.splash);
    }

    private void startOnBoardingActivity(int onBoardingStatus, String username) {
        Intent intent = new Intent(Splash.this, OnBoardingActivity.class);
        intent.putExtra(AppLibrary.USER_ONBOARDING_STATUS, 3);
        intent.putExtra(AppLibrary.USER_NAME, "dharmendra");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);

        intent = getIntent();

        intent = AppLibrary.processIntent(intent, this);
        proceedWithEnteringTheApp();

        /**
         * if the userId  is  there start activity on basis of onboarding status
         */
    /*    if ((prefs.getString(AppLibrary.USER_LOGIN, null) != null)) {
            // Force push firebase token, if it exist
            if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.FCM_TOKEN, "")) != null) {
                HashMap<Object, Object> postObject = new HashMap<>();
                postObject.put(USER_ID, prefs.getString(AppLibrary.USER_LOGIN, ""));
                postObject.put(NOTIFICATION_ID, prefs.getString(AppLibrary.FCM_TOKEN, ""));
                FireBaseHelper.getInstance(getApplicationContext()).postFireBaseRequest(UPDATE_TOKEN, postObject);
            }

            AppsFlyerLib.getInstance().setCustomerUserId(prefs.getString(AppLibrary.USER_LOGIN, null)); //To cross-reference using UserID

            Log.d(TAG, "user Id already there in prefs " + prefs.getString(AppLibrary.USER_LOGIN, null));
            FireBaseHelper.myUserId = prefs.getString(AppLibrary.USER_LOGIN, null);

            if (AccessToken.getCurrentAccessToken() != null && !prefs.getBoolean(AppLibrary.BIRTHDAY_PERMISSION, false)) { //Check if birthday permissions are there for already logged-in users
                Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
                boolean isBirthday = false;
                for (String s : permissions) {
                    if (s.contains("user_birthday")) {
                        isBirthday = true;
                    }
                }
                *//*if (isBirthday) { //Already provided birthday permissions but probably not updated on server
                    updateFacebookToken();
                    Log.d(TAG, "Already provided birthday permissions but not updated on server");
                }*//* *//*else {
                    callbackManager = CallbackManager.Factory.create();
                    LoginManager.getInstance().registerCallback(callbackManager,
                            new FacebookCallback<LoginResult>() {
                                @SuppressLint("CommitPrefEdits")
                                @Override
                                public void onSuccess(LoginResult loginResult) {
                                    if (loginResult != null) {
                                        String accessToken = loginResult.getAccessToken().getToken();
                                        AppLibrary.log_d(TAG, "Permissions granted -" + AccessToken.getCurrentAccessToken().getPermissions());
                                        AppLibrary.log_i("OnSuccess, New Facebook Access Token -", accessToken);

                                        prefs.edit().putString(AppLibrary.FACEBOOK_ACCESS_TOKEN, accessToken).commit();
                                        updateFacebookToken();
                                    } else {
                                        AppLibrary.log_d(TAG, "On Success, Login result not found");
                                        BaseFragment.showShortToastMessage("Sorry! Something went wrong");
                                    }
                                }

                                @Override
                                public void onCancel() {
                                    AppLibrary.log_d(TAG, "On Cancel");
                                    BaseFragment.showShortToastMessage("Please provide birthday permissions for a better app experience");

                                    proceedWithEnteringTheApp();
                                }

                                @Override
                                public void onError(FacebookException exception) {
                                    AppLibrary.log_d(TAG, "On Error");
                                    exception.printStackTrace();

                                    proceedWithEnteringTheApp();
                                }
                            });
                    LoginManager.getInstance().logInWithReadPermissions(this,
                            Arrays.asList("public_profile", "email", "user_friends","user_birthday"));
                }
            }*//* *//*else {
                //Already provided birthday permissions
                proceedWithEnteringTheApp();
            }*//*
            } *//*else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent mIntent = new Intent(Splash.this, FacebookLogin.class);
                    startActivity(mIntent);
                    finish();
                }
            }, 1500);
        }*//*
        }*/
    }

    private void updateFacebookToken() {
        BaseFragment.showShortToastMessage("Thanks for updating permissions! ReLogging in...");
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("userId", FireBaseHelper.myUserId));
        pairs.add(new BasicNameValuePair("token", prefs.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN, "")));
        RequestManager.makePostRequest(this, RequestManager.FACEBOOK_UPDATE_TOKEN_REQUEST, RequestManager.FACEBOOK_UPDATE_TOKEN_RESPONSE,
                null, pairs, updateAccessTokenCallback);
    }

    private RequestManager.OnRequestFinishCallback updateAccessTokenCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        prefs.edit().putBoolean(AppLibrary.BIRTHDAY_PERMISSION, true).commit();
                        proceedWithEnteringTheApp();
                    } else {
                        AppLibrary.log_d(TAG, "Facebook Post Login Error, response -" + object.getString("value"));
                        proceedWithEnteringTheApp();
                    }
                } else {
                    proceedWithEnteringTheApp();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                proceedWithEnteringTheApp();
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };


    private void proceedWithEnteringTheApp() {

        requestCameraPermissionsAndProceed();
        startOnBoardingActivity(3, "dharmendra");
        /*final int onBoardingStatus = prefs.getInt(AppLibrary.USER_ONBOARDING_STATUS, 0);

        if (fireBaseHelper == null || fireBaseHelper.getSocialModel() == null || fireBaseHelper.getMyUserModel() == null) {
            fireBaseHelper = FireBaseHelper.getInstance(getApplicationContext());
            checkIfLatestApplication();
            fireBaseHelper.updateAppStatus();

            if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.FCM_TOKEN,"")) == null) { //For updating FCM token for latest users

                String token = null;
                try {
                    token = FirebaseInstanceId.getInstance().getToken();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (token != null) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(AppLibrary.FCM_TOKEN, token);//FCM token updated in shared preferences
                    editor.commit();

                    HashMap<Object, Object> postObject = new HashMap<>();
                    postObject.put(USER_ID, prefs.getString(AppLibrary.USER_LOGIN, ""));
                    postObject.put(NOTIFICATION_ID, prefs.getString(AppLibrary.FCM_TOKEN, ""));
                    FireBaseHelper.getInstance(getApplicationContext()).postFireBaseRequest(UPDATE_TOKEN, postObject);
                }
            }

            fireBaseHelper.setFireBaseReadyListener(new FireBaseHelper.FireBaseReadyListener() {
                @Override
                public void onDataLoaded(SocialModel socialModel, UserModel userModel) {
                    Log.d(TAG, " socialModel " + socialModel + " userModel " + userModel);

                    if (socialModel != null && userModel != null && !isActivityLaunched) {
                        isActivityLaunched = true;
                        if (onBoardingStatus == INSTITUTION_PROVING_DONE || (onBoardingStatus==FRIENDS_INVITING_DONE && !prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED,true))) {//startCameraActivity; onboarding already done
                            requestCameraPermissionsAndProceed(); //Request camera permissions before starting activity
                        } else {//startOnBoardingActivity
                            startOnBoardingActivity(onBoardingStatus, userModel.name);
                        }
                    }
                }
            });
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (onBoardingStatus == INSTITUTION_PROVING_DONE || (onBoardingStatus==FRIENDS_INVITING_DONE && !prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED,true))) {//startCameraActivity; onboarding already done
                        requestCameraPermissionsAndProceed(); //Request camera permissions before starting activity
                    } else {//startOnBoardingActivity
                        startOnBoardingActivity(onBoardingStatus, fireBaseHelper.getMyUserModel().name);
                    }
                }
            }, 700);
        }*/
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }



    static boolean latestApplication = false;

    private void checkIfLatestApplication() {
        fireBaseHelper.getNewFireBase(ANCHOR_APP_SETTINGS, null).keepSynced(true);
        fireBaseHelper.getNewFireBase(ANCHOR_APP_SETTINGS, null).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {

                    int localVersionCode = BuildConfig.VERSION_CODE;
                    String localVersionName = BuildConfig.VERSION_NAME;
                    float localVersionNameFloat = Float.parseFloat(localVersionName);
                    int localVersionNameInt= (int)localVersionNameFloat;

                    Integer MinimumVersionName = dataSnapshot.child("MinimumVersionName").getValue(Integer.class);
                    Integer MinimumVersionCode = dataSnapshot.child("MinimumVersionCode").getValue(Integer.class);

                    if ((localVersionNameInt >= MinimumVersionName)) {
                        if (localVersionCode >= MinimumVersionCode) {
                            //everyThingOk
                            latestApplication = true;
                        } else {//staleApplication
                            kickOutUser(MasterClass.getGlobalContext());
                        }

                    } else {//stale application
                        kickOutUser(MasterClass.getGlobalContext());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (callbackManager!=null)
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    void kickOutUser(Context context) {
        BaseFragment.showShortToastMessage("Please update the app and reLogin");
        isActivityLaunched = true;

        deleteCache(this);
        context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0).edit().clear().commit();

        String appPackageName = getApplicationContext().getPackageName(); // getPackageName() from Context or Activity object

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (Exception e) {
            e.printStackTrace();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }

        finish();
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            Log.e(TAG, "deleteCache");
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    @Override
    public void onPermissionsGranted(String[] permissions) {
        Intent mIntent = new Intent(Splash.this, CameraActivity.class);
        if (intent != null && intent.getExtras() != null && AppLibrary.checkStringObject(intent.getExtras().getString("action")) != null) {
            Bundle bundle = intent.getExtras();
            mIntent.putExtras(bundle);
        }

        /*if (!prefs.getBoolean(AppLibrary.ANALYTICS_IDENTIFIED, false)) { //For the existing users
            AnalyticsManager.getInstance().createUserAfterSignUp(getApplicationContext(), FireBaseHelper.getInstance(this).getMyUserModel().handle, FireBaseHelper.getInstance(this).getMyUserModel().gender, FireBaseHelper.getInstance(this).getSettingsModel());
        }*/

        startActivity(mIntent);
        finish();
    }
}