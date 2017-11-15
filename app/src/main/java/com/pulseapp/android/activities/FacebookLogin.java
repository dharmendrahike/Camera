package com.pulseapp.android.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.GenericViewPagerAdapter;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.PulseIntroFragment;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BaseActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by abc on 4/18/2016.
 */
@SuppressWarnings("ConstantConditions")
public class FacebookLogin extends BaseActivity implements FireBaseKEYIDS {

    private static final String TAG = "FacebookLogin";
    private CallbackManager callbackManager;
    private SharedPreferences prefs;
    private boolean isDestroyed = false;
    public static boolean isActivityLaunched = false;
    private ArrayList<Fragment> fragmentsList;
    private GenericViewPagerAdapter adapter;
    //    private TextView skipTv;
    private ViewPager viewPager;
    private LinearLayout dotsContainer;
    int initialCurrentPage = 0;
//    private static final int PERMISSION_ACCESS_CAMERA_MICROPHONE = 0;

    public void onSkipClicker() {
        if (fragmentsList.size() == 5)
            removeFirstFragment();
        viewPager.setCurrentItem(fragmentsList.size() - 1, true);
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ((ImageView) dotsContainer.getChildAt(i)).setImageResource(i == fragmentsList.size() - 1 ? R.drawable.selected_fill_svg : R.drawable.selected_svg);
        }
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.facebook_login);
        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        callbackManager = CallbackManager.Factory.create();

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_STARTED);

        ((ProgressBar)findViewById(R.id.progressBar))
                .getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);

        fragmentsList = new ArrayList<>();

        String[] intro = this.getResources().getStringArray(R.array.pulse_intro);
        String[] subIntro = this.getResources().getStringArray(R.array.pulse__sub_intro);

        for (int i = 0; i < 5; i++) {
            Bundle data = new Bundle();
            data.putInt("page_no", i);
            if (i > 0 && i < 4) {
                data.putString("intro", intro[i - 1]);
                data.putString("subIntro", subIntro[i - 1]);
            }
            Fragment fragment = new PulseIntroFragment();
            fragment.setArguments(data);
            fragmentsList.add(fragment);
        }
        adapter = new GenericViewPagerAdapter(getSupportFragmentManager(), fragmentsList);
        viewPager = (ViewPager) findViewById(R.id.facebook_login_view_pager);
        viewPager.setOffscreenPageLimit(5);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected " + position);
                initialCurrentPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d(TAG, "onPageScrollStateChanged " + state);
                if (state == ViewPager.SCROLL_STATE_IDLE && initialCurrentPage != 0)
                    if (fragmentsList.size() == 5) {
                        removeFirstFragment();
                    }
            }
        });

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @SuppressLint("CommitPrefEdits")
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        if (loginResult != null) {
                            String accessToken = loginResult.getAccessToken().getToken();
                            AppLibrary.log_d(TAG, "Permissions granted -" + AccessToken.getCurrentAccessToken().getPermissions());
                            AppLibrary.log_i("OnSuccess, Facebook Access Token -", accessToken);
                            AppLibrary.log_i("OnSuccess, FACEBOOK_ID", loginResult.getAccessToken().getUserId());

                            prefs.edit().putString(AppLibrary.FACEBOOK_ACCESS_TOKEN, accessToken).commit();
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_LOGIN_STATE, true).commit();
                            prefs.edit().putString(AppLibrary.FACEBOOK_ID, loginResult.getAccessToken().getUserId()).commit();
                            prefs.edit().putBoolean(AppLibrary.BIRTHDAY_PERMISSION, true).commit();

                            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_FACEBOOK_RESPONSE);

                            postFacebookLoginRequest();
                        } else {
                            AppLibrary.log_d(TAG, "On Success, Login result not found");
                            Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
                            ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();
                        }
                    }

                    @Override
                    public void onCancel() {
                        AppLibrary.log_d(TAG, "On Cancel");
                        Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
//                        findViewById(R.id.facebookLayout).setClickable(true);
                        ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();

                    }

                    @Override
                    public void onError(FacebookException exception) {
                        AppLibrary.log_d(TAG, "On Error");
//                        findViewById(R.id.facebookLayout).setClickable(true);
                        ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();
                        Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                    }
                });
    }

//    private void toggleSkipButtonVisibility() {
//        if (viewPager.getCurrentItem() == fragmentsList.size() - 1)
//            skipTv.setVisibility(View.GONE);
//        else skipTv.setVisibility(View.VISIBLE);
//    }

    private void removeFirstFragment() {
        fragmentsList.remove(0);
        viewPager.setAdapter(new GenericViewPagerAdapter(getSupportFragmentManager(), fragmentsList));
        viewPager.clearOnPageChangeListeners();
        viewPager.addOnPageChangeListener(finalViewPagerListener);
        buildCircles();
    }

    ViewPager.OnPageChangeListener finalViewPagerListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            float[] from_to_y;
            if (position == fragmentsList.size() - 1) {
                from_to_y = new float[]{0, 500};
            } else {
                from_to_y = new float[]{dotsContainer.getTranslationY(), 0};
                for (int i = 0; i < dotsContainer.getChildCount(); i++) {
                    ((ImageView) dotsContainer.getChildAt(i)).setImageResource(i == position ? R.drawable.selected_fill_svg : R.drawable.selected_svg);
                }
            }
            ObjectAnimator.ofFloat(dotsContainer, "translationY", from_to_y).setDuration(500).start();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    public void onLoginButtonClicked() {
        doFacebookLogin();
    }


    private void buildCircles() {
        dotsContainer = (LinearLayout) findViewById(R.id.dotsContainerLL);
        dotsContainer.setVisibility(View.VISIBLE);
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ((ImageView) dotsContainer.getChildAt(i)).setImageResource(viewPager.getCurrentItem() == i ? R.drawable.selected_fill_svg : R.drawable.selected_svg);

        }
//        for (int i = 0; i < 4; i++) {
//            ImageView circle = new ImageView(this);
//            circle.setImageResource(1 == 0 ? R.drawable.clear : R.drawable.add);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//            params.setMargins(5, 0, 5, 0);
//            circle.setLayoutParams(params);
//            circle.setAdjustViewBounds(true);
//            dotsContainer.addView(circle);
//        }
    }

    @SuppressWarnings("deprecation")
    private void postFacebookLoginRequest() {
        List<NameValuePair> pairs = new ArrayList<>();
        String deviceId = AppLibrary.getDeviceId(this);
        if (deviceId != null && !deviceId.equals(""))
            pairs.add(new BasicNameValuePair("deviceId", deviceId));
//        String registrationToken = FirebaseInstanceId.getInstance().getToken();
//        AppLibrary.log_d(TAG, "Got registration token as -" + registrationToken);
//        if (registrationToken != null)
//            pairs.add(new BasicNameValuePair("notificationId", registrationToken));
        pairs.add(new BasicNameValuePair("deviceName", AppLibrary.getDeviceName()));
        pairs.add(new BasicNameValuePair("token", prefs.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN, "")));
        pairs.add(new BasicNameValuePair("facebookId", prefs.getString(AppLibrary.FACEBOOK_ID, "")));
        RequestManager.makePostRequest(this, RequestManager.FACEBOOK_LOGIN_REQUEST, RequestManager.FACEBOOK_LOGIN_RESPONSE,
                null, pairs, postLoginCallback);
        findViewById(R.id.progressView).setVisibility(View.VISIBLE);
    }

    int onBoardingStatus;

    private boolean isInstitutionNeeded = true;
    private RequestManager.OnRequestFinishCallback postLoginCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        AppLibrary.log_d(TAG, "Facebook Post Login Success, response -" + object.getString("value"));

                        onBoardingStatus = ON_BOARDING_NOT_STARTED;
                        try {
                            onBoardingStatus = object.getInt("onboarding_status");
                            isInstitutionNeeded = object.getBoolean("check_institution");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {

                        }

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(AppLibrary.USER_LOGIN, object.getString("value"));//putting userId in prefs
                        editor.putInt(AppLibrary.USER_ONBOARDING_STATUS, onBoardingStatus);//putting on board status in prefs
                        editor.putBoolean(AppLibrary.INSTITUTION_NEEDED, isInstitutionNeeded);//putting institutes needed flag to prefs
                        editor.commit();

                        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_API_RESPONSE);

//                        FirebaseAuth.getInstance().signInWithCustomToken(object.getString("token")).addOnCompleteListener(FacebookLogin.this, new OnCompleteListener<AuthResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<AuthResult> task) {
//                                try {

                                    FireBaseHelper.myUserId = object.getString("value");
                                    Log.d(TAG, "changed fireBase user id to " + FireBaseHelper.myUserId);

                                    Crashlytics.setUserIdentifier(FireBaseHelper.myUserId);
//                                    AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_FIREBASE_AUTH);

                                    FireBaseHelper.getInstance(getApplicationContext()).setFireBaseReadyListener(new FireBaseHelper.FireBaseReadyListener() {
                                        @Override
                                        public void onDataLoaded(SocialModel socialModel, UserModel userModel) {
                                            Log.d(TAG, " socialmodel " + socialModel + " userModel " + userModel);
                                            if (socialModel != null && userModel != null && !isActivityLaunched) {
                                                isActivityLaunched = true;
                                                FireBaseHelper.getInstance(getApplicationContext()).updateAppStatus();
                                                FireBaseHelper.getInstance(getApplicationContext()).updateDeviceName();

                                                if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.FCM_TOKEN,"")) != null) {
                                                    HashMap<Object, Object> postObject = new HashMap<>();
                                                    postObject.put(USER_ID, prefs.getString(AppLibrary.USER_LOGIN, ""));
                                                    postObject.put(NOTIFICATION_ID, prefs.getString(AppLibrary.FCM_TOKEN, ""));
                                                    FireBaseHelper.getInstance(getApplicationContext()).postFireBaseRequest(UPDATE_TOKEN, postObject);
                                                }
                                                else {
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

                                                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_SUCCESSFUL);

                                                if (onBoardingStatus>=USER_NAME_SELECTION_DONE) //Already selected username before
                                                    AnalyticsManager.getInstance().identifyUserWithHandleName(getApplicationContext(),userModel.handle);

                                                if (onBoardingStatus == FireBaseKEYIDS.INSTITUTION_PROVING_DONE || (onBoardingStatus==FRIENDS_INVITING_DONE && !prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED,true))) {
                                                    requestCameraPermissionsAndProceed(); //Request camera permissions before starting activity
                                                } else {
                                                    startOnBoardingActivity(onBoardingStatus, userModel.name);
                                                }
                                            }
                                        }
                                    });
                            } else {
                                AppLibrary.log_d(TAG, "Facebook Post Login Error");
                                Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
                                ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();
                            }
                    } else {
                        AppLibrary.log_d(TAG, "Facebook Post Login Error");
                        Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
                        ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();
                    }

            } catch (JSONException e) {
                AppLibrary.log_d(TAG, "Facebook Post Login Error");
                Toast.makeText(FacebookLogin.this, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
                ((PulseIntroFragment) fragmentsList.get(fragmentsList.size() - 1)).enableButtonOnLoginFailed();
                e.printStackTrace();
            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

//    private void requestCameraPermissionsAndProceed() {
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
//                    PERMISSION_ACCESS_CAMERA_MICROPHONE);
//        } else {
//            startActivity(new Intent(FacebookLogin.this, CameraActivity.class));
//            finish();
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSION_ACCESS_CAMERA_MICROPHONE:
//                if (AppLibrary.verifyPermissions(grantResults)) {
//                    startActivity(new Intent(FacebookLogin.this, CameraActivity.class));
//                    finish();
//                } else {
//                    requestCameraPermissionsAndProceed();
//                }
//                break;
//
//            default:
//                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }



    private void startOnBoardingActivity(int onBoardingStatus, String username) {
        Intent intent = new Intent(FacebookLogin.this, OnBoardingActivity.class);
        intent.putExtra(AppLibrary.USER_ONBOARDING_STATUS, onBoardingStatus);
        intent.putExtra(AppLibrary.USER_NAME, username);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void doFacebookLogin() {
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_LOGIN_CLICK);

        LoginManager.getInstance().logInWithReadPermissions(this,
                Arrays.asList("public_profile", "email", "user_friends","user_birthday"));
//        LoginManager.getInstance().logInWithPublishPermissions(this,
//                Arrays.asList("manage_pages"));
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
        isDestroyed = true;
    }

    @Override
    protected void onPermissionsGranted(String[] permissions) {
        startActivity(new Intent(FacebookLogin.this, CameraActivity.class));
        finish();
    }
}
