package com.pulseapp.android.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.pulseapp.android.R;
import com.pulseapp.android.Splash;
import com.pulseapp.android.adapters.IntroPagerAdapter;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.fragments.EditProfileFragment;
import com.pulseapp.android.fragments.OnBoardingCategoriesFragment;
import com.pulseapp.android.util.AppLibrary;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FacebookLoginActivity extends FragmentActivity {

    private ViewPager pager;
    private CallbackManager callbackManager;
    private SharedPreferences prefs;
    private boolean isDone = false;
    private String TAG = "FacebookLoginActivity";
    private ProfileTracker profileTracker;
    private boolean isDestroyed = false;
    public  Dialog progressDialog;
    
    public Boolean isCircleBuild = false;
    private int  finalPixel = 0;
    private Boolean isWidthCalculated = false;
    private static int viewPagerWidth;
    private static LinearLayout circles;
    private HorizontalScrollView scrollViewElement,scrollViewColor;
    private LinearLayout imageContainer,colorContainer;
    private ViewTreeObserver vtoElement,vtoColor;
    private Boolean isInitialScrollDone = false;
    private RelativeLayout updatePopup,shadowLayout;
    private int requiredVersion = 0;

    @Override
    public void onCreate(Bundle savedBundleInstance){
        super.onCreate(savedBundleInstance);
        setContentView(R.layout.activity_facebook_login);
        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
//        updatePopup = (RelativeLayout) findViewById(R.id.popupRoot);
//        updatePopup.findViewById(R.id.updateButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
//                try {
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//                } catch (android.content.ActivityNotFoundException e) {
//                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
//                }
//            }
//        });
        shadowLayout = (RelativeLayout) findViewById(R.id.shadowLayout) ;
        Splash.isCameraOpenChecker = true;
        if (getIntent().hasExtra("login_step")){
            if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL,"")) != null) {
//                Picasso.with(FacebookLoginActivity.this)
//                        .load(prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, ""))
//                        .transform(new BlurTransformation(FacebookLoginActivity.this, 20))
//                        .placeholder(R.drawable.profile_placeholder)
//                        .fit().centerCrop()
//                        .into((ImageView) findViewById(R.id.blurredBackground));
                findViewById(R.id.blurredBackground).setVisibility(View.VISIBLE);
            } else {
//                Picasso.with(FacebookLoginActivity.this)
//                        .load(R.drawable.profile_placeholder)
//                        .fit()
//                        .centerCrop()
//                        .into((ImageView) findViewById(R.id.blurredBackground));
                findViewById(R.id.blurredBackground).setVisibility(View.VISIBLE);
            }
            if (getIntent().getStringExtra("login_step").equals("0")){
                // edit user profile and select categories
                launchEditProfileFragment();
            } else if (getIntent().getStringExtra("login_step").equals("1")){
                // edit profile done , so let the user select categories
                launchOnBoardingCategoriesFragment();
            }
        }
        initializeViewObjects();
        initializeAnimation();

        if (!isCircleBuild) {
            isCircleBuild = true;
            buildCircles();
            setIndicator(0);
        }

        vtoElement.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (!isInitialScrollDone)
                    scrollViewElement.smoothScrollTo(viewPagerWidth * 2, 0);
                isInitialScrollDone = true;
            }
        });

        vtoColor.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (!isInitialScrollDone)
                    scrollViewElement.smoothScrollTo(viewPagerWidth * 2, 0);
                isInitialScrollDone = true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requiredVersion != 0) {
            if (requiredVersion > getAppVersion(this)) {
                showUpdatePopup();
            } else {
                hideUpdatePopup();
            }
        }
    }

    private void showUpdatePopup() {
        shadowLayout.setVisibility(View.VISIBLE);
        updatePopup.setVisibility(View.VISIBLE);
        YoYo.with(Techniques.SlideInUp).duration(300).playOn(updatePopup);
    }

    private void hideUpdatePopup() {
        shadowLayout.setVisibility(View.GONE);
        updatePopup.setVisibility(View.GONE);
    }

    private void initializeAnimation() {
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new IntroPagerAdapter(getSupportFragmentManager(), "mobile"));
        scrollViewElement = (HorizontalScrollView) findViewById(R.id.element_scroll_view);
        scrollViewColor = (HorizontalScrollView) findViewById(R.id.color_scroll_view);
        imageContainer = (LinearLayout) findViewById(R.id.imageContainerLL);
        colorContainer = (LinearLayout) findViewById(R.id.colorContainerLL);
        scrollViewElement.setFillViewport(true);
        scrollViewColor.setFillViewport(true);
        vtoElement = scrollViewElement.getViewTreeObserver();
        vtoColor = scrollViewColor.getViewTreeObserver();
        
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (!isWidthCalculated) {
                    viewPagerWidth = pager.getWidth();
                    addElementImages();
                    isWidthCalculated = true;
                }
                finalPixel = calculatePixel(position, positionOffset, positionOffsetPixels);
                int scrollingPixel = viewPagerWidth * 2 - finalPixel;
                scrollViewElement.scrollTo(scrollingPixel, 0);
                scrollViewColor.scrollTo(finalPixel, 0);
            }

            @Override
            public void onPageSelected(int position) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + pager.getCurrentItem());
                setIndicator(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        break;
                    case ViewPager.SCROLL_STATE_IDLE:
                        break;
                }
            }
        });
    }
    
    private void buildCircles() {
        circles = LinearLayout.class.cast(findViewById(R.id.dotsLayout_activity));
        for (int i = 0; i < 3; i++) {
            ImageView circle = new ImageView(this);
//            circle.setImageResource(R.drawable.pagination_dot_small);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 0, 5, 0);
            circle.setLayoutParams(params);
            circle.setAdjustViewBounds(true);
            circles.addView(circle);
        }
    }

    public void setIndicator(int index) {
        if (index < 3) {
            for (int i = 0; i < 3; i++) {
                ImageView circle = (ImageView) circles.getChildAt(i);
                if (i == index) {
//                    circle.setImageResource(R.drawable.pagination_dot_big);
                } else {
//                    circle.setImageResource(R.drawable.pagination_dot_small);
                }
            }
        }
    }

    private void addElementImages() {
        for (int i = 0; i < 3; i++) {
            ImageView elementImage = new ImageView(this);
            ImageView colorImage = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(viewPagerWidth, ViewGroup.LayoutParams.MATCH_PARENT);

            switch (i) {
                case 0:
                    colorImage.setBackgroundColor(Color.parseColor("#e89738"));
//                    elementImage.setBackgroundResource(R.drawable.bg_3_result);
                    break;
                case 1:
                    colorImage.setBackgroundColor(Color.parseColor("#f15c56"));
//                    elementImage.setBackgroundResource(R.drawable.bg_2_result);
                    break;
                case 2:
                    colorImage.setBackgroundColor(Color.parseColor("#ad62a7"));
//                    elementImage.setBackgroundResource(R.drawable.bg_1_result);
                    break;
            }
            elementImage.setLayoutParams(layoutParams);
            colorImage.setLayoutParams(layoutParams);

            imageContainer.addView(elementImage);
            colorContainer.addView(colorImage);
        }
    }

    private int calculatePixel(int position, float positionOffset, int positionOffsetPixels) {
        int resultingPixel = 0;
        switch (position) {
            case 0:
                resultingPixel = positionOffsetPixels;
                break;
            case 1:
                resultingPixel = viewPagerWidth + positionOffsetPixels;
                break;
            case 2:
                resultingPixel = (2 * viewPagerWidth);
                break;
        }
        return resultingPixel;
    }

    private void initializeViewObjects(){
        // setting progress dialog properties
        progressDialog = new Dialog(FacebookLoginActivity.this);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressDialog.setCanceledOnTouchOutside(false);
        callbackManager = CallbackManager.Factory.create();
        findViewById(R.id.facebookLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.facebookLayout).setClickable(false);
                doFacebookLogin();
            }
        });

//        profileTracker = new ProfileTracker() {
//            @Override
//            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
//                Profile.setCurrentProfile(currentProfile);
//                if(!isDone) { //Prevents multiple calls of getFacebookPages in some devices
//                    isDone = true;
//                    prefs.edit().putString(AppLibrary.USER_PROFILE_PIC_URL,Profile.getCurrentProfile().getProfilePictureUri(50, 50).toString()).commit();
//                    AppLibrary.log_i("Facebook Profile Pic Url -", Profile.getCurrentProfile().getProfilePictureUri(50, 50).toString());
//                    prefs.edit().putString(AppLibrary.USER_NAME,Profile.getCurrentProfile().getName()).commit();
//                    AppLibrary.log_d(TAG, "Facebook User Name -" + Profile.getCurrentProfile().getName());
//                    prefs.edit().putString(AppLibrary.FACEBOOK_ID,Profile.getCurrentProfile().getId()).commit();
//                    AppLibrary.log_d(TAG, "Facebook Id -" + Profile.getCurrentProfile().getId());
//
//                }
//            }
//        };
//
//        profileTracker.startTracking();

        LoginManager.getInstance().registerCallback(callbackManager,
        new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult != null) {
                    String accessToken = loginResult.getAccessToken().getToken();
                    AppLibrary.log_d(TAG, "Permissions granted -" + AccessToken.getCurrentAccessToken().getPermissions());
                    AppLibrary.log_i("OnSuccess, Facebook Access Token -", accessToken);
                    prefs.edit().putString(AppLibrary.FACEBOOK_ACCESS_TOKEN, accessToken).commit();
                    prefs.edit().putBoolean(AppLibrary.FACEBOOK_LOGIN_STATE, true).commit();
                    progressDialog.show();
                    prefs.edit().putString(AppLibrary.FACEBOOK_ID,loginResult.getAccessToken().getUserId()).commit();
//                  postFacebookLoginRequest();
                } else {
                    AppLibrary.log_d(TAG, "On Success, Login result not found");
                    Toast.makeText(FacebookLoginActivity.this,"Sorry! Something went wrong",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                AppLibrary.log_d(TAG, "On Cancel");
                Toast.makeText(FacebookLoginActivity.this,"Sorry! Something went wrong",Toast.LENGTH_SHORT).show();
                findViewById(R.id.facebookLayout).setClickable(true);
            }

            @Override
            public void onError(FacebookException exception) {
                AppLibrary.log_d(TAG,"On Error");
                findViewById(R.id.facebookLayout).setClickable(true);
                Toast.makeText(FacebookLoginActivity.this,"Sorry! Something went wrong",Toast.LENGTH_SHORT).show();
                exception.printStackTrace();
            }
        });
    }

    private void doFacebookLogin() {
        AppLibrary.log_d(TAG, "Initiate Facebook Login");
        LoginManager.getInstance().logInWithReadPermissions(this,
                Arrays.asList("public_profile", "email", "user_friends"));
    }

//    private void postFacebookLoginRequest(){
//        List<NameValuePair> pairs = new ArrayList<>();
//        String deviceId = AppLibrary.getDeviceId(this);
//        if (deviceId != null && !deviceId.equals(""))
//            pairs.add(new BasicNameValuePair("deviceId",deviceId));
//        pairs.add(new BasicNameValuePair("deviceName", AppLibrary.getDeviceName()));
//        if (AppLibrary.checkStringObject(prefs.getString(AppLibrary.USER_LOGIN,"")) != null)
//            pairs.add(new BasicNameValuePair("previousId",prefs.getString(AppLibrary.USER_LOGIN,"")));
//        pairs.add(new BasicNameValuePair("token",prefs.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN,"")));    // compulsory field
//        pairs.add(new BasicNameValuePair("facebookId",prefs.getString(AppLibrary.FACEBOOK_ID,"")));     // compulsory field
//        AppLibrary.log_d(TAG, " FacebookLogin Fields -: " +
//                ", Device id -" + deviceId +
//                ", Device name -" + AppLibrary.getDeviceName() +
//                ", Previous id -" + prefs.getString(AppLibrary.USER_LOGIN, "") +
//                ", Facebook Token -" + prefs.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN, "") +
//                ", Facebook id -" + prefs.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN, ""));
//        RequestManager.makePostRequest(this, RequestManager.FACEBOOK_LOGIN_REQUEST, RequestManager.FACEBOOK_LOGIN_RESPONSE,
//                null, pairs, postLoginCallback);
//    }

    private RequestManager.OnRequestFinishCallback postLoginCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try{
                JSONObject object = (JSONObject)response;
                if (success){
                    if (object.getString("error").equalsIgnoreCase("false")){
                        AppLibrary.log_d(TAG, "Facebook Post Login Success, response -" + object.getString("value"));
                        storeLoginData(object.optJSONObject("value"));
                        storeAppVersion();
                        Set friendsPermissionSet = new HashSet(Arrays.asList("user_friends"));
                        Set publishPermissionSet = new HashSet(Arrays.asList("publish_actions"));
                        Set publishPagesPermissionSet = new HashSet(Arrays.asList("manage_pages", "publish_pages"));
                        if(AccessToken.getCurrentAccessToken().getPermissions().containsAll(friendsPermissionSet)){
                            AppLibrary.log_d(TAG, "friends access granted");
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_FRIENDS_PERMISSION,true).commit();
//                            FacebookController.getInstance(FacebookLoginActivity.this).fetchFacebookFriends();
                        } else {
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_FRIENDS_PERMISSION,false).commit();
                        }
                        if (AccessToken.getCurrentAccessToken().getPermissions().containsAll(publishPagesPermissionSet)) {
                            AppLibrary.log_d(TAG, "Granted publish and manage pages permissions");
                            if (AccessToken.getCurrentAccessToken().getPermissions().containsAll(publishPermissionSet)) {
                                prefs.edit().putBoolean(AppLibrary.FACEBOOK_PUBLISH_PERMISSION, true).commit();
                                AppLibrary.log_d(TAG, "Granted publish Action permissions");
                            }
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_PUBLISH_PAGES_PERMISSION, true).commit();
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_SHARE_STATE, true).commit();
                        } else if (AccessToken.getCurrentAccessToken().getPermissions().containsAll(publishPermissionSet)) {
                            AppLibrary.log_d(TAG, "Granted publish Action permissions");
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_PUBLISH_PERMISSION, true).commit();
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_SHARE_STATE,true).commit();
                        } else {
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_PUBLISH_PERMISSION, false).commit();
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_PUBLISH_PAGES_PERMISSION, false).commit();
                            prefs.edit().putBoolean(AppLibrary.FACEBOOK_SHARE_STATE,false).commit();
                        }
//                        getEnableLiveStreamingStatus();
                    }else {
                        AppLibrary.log_d(TAG,"Facebook Post Login Error, response -"+object.getString("value"));
                    }
                }else {
                    // request failed
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

    private void storeAppVersion(){
        int appVersion = getAppVersion(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(AppLibrary.APP_VERSION, appVersion);
        editor.commit();
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("RegisterActivity",
                    "I never expected this! Going down, going down!" + e);
            throw new RuntimeException(e);
        }
    }

    private void storeLoginData(JSONObject object){
        try {
            if (object != null) {
                String picture = object.optString("pictureUrl");
                if (AppLibrary.checkStringObject(picture) != null) {
//                    Picasso.with(FacebookLoginActivity.this)
//                            .load(picture).transform(new BlurTransformation(FacebookLoginActivity.this, 20))
//                            .placeholder(R.drawable.profile_placeholder)
//                            .fit()
//                            .centerCrop()
//                            .into((ImageView) findViewById(R.id.blurredBackground));
                    prefs.edit().putString(AppLibrary.USER_PROFILE_PIC_URL, picture).commit();
                } else {
//                    Picasso.with(FacebookLoginActivity.this)
//                            .load(R.drawable.profile_placeholder)
//                            .fit()
//                            .centerCrop()
//                            .into((ImageView) findViewById(R.id.blurredBackground));
                }
                prefs.edit().putString(AppLibrary.USER_LOGIN, object.optString("_id")).commit();
                prefs.edit().putString(AppLibrary.USER_NAME, object.optString("fullName")).commit();
                prefs.edit().putString(AppLibrary.USER_LOGIN_EMAIL, object.optString("email")).commit();
                prefs.edit().putString(AppLibrary.USER_GENDER, object.optString("gender")).commit();
                prefs.edit().putString(AppLibrary.GOOGLE_ID, object.optString("googleId")).commit();
                prefs.edit().putString(AppLibrary.FACEBOOK_BIO_INFO,object.optString("bio")).commit();
                if (AppLibrary.checkStringObject(object.optString("googleAccountType")) != null) {
                    if (object.optString("googleAccountType").contains("page")) {
                        AppLibrary.log_d(TAG,"Entered Google Pages Loop");
                        prefs.edit().putString(AppLibrary.GOOGLE_PAGE, "true").commit();
                        String liveStreamingUrl = "http://m.youtube.com/signin?action_handle_signin=true&next=%2Flive_streaming_signup&pageid=" + object.getString("googleId") + "&authuser=0&feature=channel_switcher&skip_identity_prompt=False";
                        prefs.edit().putString(AppLibrary.GOOGLE_LIVE_STREAM_URL, liveStreamingUrl).commit();
                    } else {
                        AppLibrary.log_d(TAG, "Entered Google Person Loop");
                        String liveStreamingUrl = "http://m.youtube.com/signin?authuser=0&feature=channel_switcher&next=%2Flive_streaming_signup&skip_identity_prompt=False&action_handle_signin=true";
                        prefs.edit().putString(AppLibrary.GOOGLE_PAGE, "false").commit();
                        prefs.edit().putString(AppLibrary.GOOGLE_LIVE_STREAM_URL, liveStreamingUrl).commit();
                    }
                }
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

//    private void getEnableLiveStreamingStatus() {
//        AppLibrary.log_i(TAG, "getEnableLiveStreamingStatus called");
//        RequestManager.makeGetRequest(FacebookLoginActivity.this, RequestManager.ENABLE_LIVE_STREAMING_REQUEST, RequestManager.ENABLE_LIVE_STREAMING_STATUS,
//                null, enableLiveStreamingStatusCallback);
//    }

    private RequestManager.OnRequestFinishCallback enableLiveStreamingStatusCallback = new
            RequestManager.OnRequestFinishCallback() {
                @Override
                public void onBindParams(final boolean success, final Object response) {
                    AppLibrary.log_i(TAG, "enable live streaming status response: " + success + " : " + response);
                    final JSONObject args = (JSONObject) response;
                    try {
                        if (success) {
                            if (args.optJSONObject("flags") != null) {
                                requiredVersion = args.getJSONObject("flags").optInt("forceUpdate");
                                if (requiredVersion != 0 && requiredVersion > getAppVersion(FacebookLoginActivity.this)) {
                                    //force update
                                    AppLibrary.log_d(TAG, "Showing update popup");
                                    showUpdatePopup();
                                    return;
                                }
                            }
                            if (getIntent().hasExtra("fromWeb")) {
//                                Intent viewpageIntent = new Intent(FacebookLoginActivity.this, YoutubePlayerActivity.class);
//                                viewpageIntent.putExtra(AppLibrary.EVENT_SID, getIntent().getStringExtra("streamId"));
//                                viewpageIntent.putExtra("Notification", true);
//                                startActivity(viewpageIntent);
//                                finish();
                            } else {
                                if (args.getString("error").equalsIgnoreCase("false")) {
                                    if (args.getString("value").equalsIgnoreCase("enabled")) {
                                        prefs.edit().putBoolean(AppLibrary.ENABLE_LIVE_STREAM_STATUS, true).commit();
                                    } else if (args.getString("value").equalsIgnoreCase("notEnabled")) {
                                        prefs.edit().putBoolean(AppLibrary.ENABLE_LIVE_STREAM_STATUS, false).commit();
                                    }
                                    progressDialog.dismiss();
                                    if (args.getJSONObject("flags").getInt("login_step") == 0) {
                                        findViewById(R.id.blurredBackground).setVisibility(View.VISIBLE);
                                        launchEditProfileFragment();
                                    } else if (args.getJSONObject("flags").getInt("login_step") == 1){
                                        launchOnBoardingCategoriesFragment();
                                    } else {
                                        Intent mIntent = new Intent(FacebookLoginActivity.this, CameraActivity.class);
                                        startActivity(mIntent);
                                        finish();
                                    }
                                } else {
                                    if (!args.getJSONObject("flags").optBoolean("validFbToken")) {
                                        // popup that token is not valid and re Login
                                        progressDialog.dismiss();
                                        AppLibrary.doFacebookLogOut();
                                    } else {
                                        progressDialog.dismiss();
                                        if (args.getJSONObject("flags").getInt("login_step") == 0) {
                                            launchEditProfileFragment();
                                        } else if (args.getJSONObject("flags").getInt("login_step") == 1){
                                            launchOnBoardingCategoriesFragment();
                                        } else {
                                            Intent mIntent = new Intent(FacebookLoginActivity.this, CameraActivity.class);
                                            startActivity(mIntent);
                                            finish();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public boolean isDestroyed() {
                    return isDestroyed;
                }
            };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }

    private void launchEditProfileFragment() {
        Fragment fragment = new EditProfileFragment();
        FragmentTransaction fragmentTransaction;
        FragmentManager fragmentManager;
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment, "EditProfileFragment");
//        fragmentTransaction.addToBackStack("FaceBookFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void launchOnBoardingCategoriesFragment(){
        Fragment fragment = new OnBoardingCategoriesFragment();
        FragmentTransaction fragmentTransaction;
        FragmentManager fragmentManager;
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment, "OnBoardingCategoriesFragment");
//        fragmentTransaction.addToBackStack("OnBoardingCategoriesFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }
}