package com.pulseapp.android.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.appsflyer.AppsFlyerLib;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.Splash;
import com.pulseapp.android.adapters.ChatAdapter;
import com.pulseapp.android.adapters.LetterTileProvider;
import com.pulseapp.android.adapters.MainFragmentPagerAdapter;
import com.pulseapp.android.adapters.SliderMessageListAdapter;
import com.pulseapp.android.adapters.SliderMyMomentAdapter;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.broadcast.MoviePlayer;
import com.pulseapp.android.broadcast.RecordingActionControllerKitkat;
import com.pulseapp.android.downloader.ChatCameraStickerDownloader;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.AddFriendFragment;
import com.pulseapp.android.fragments.AllChatListFragment;
import com.pulseapp.android.fragments.AroundYouFragment;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.BirthdayFragment;
import com.pulseapp.android.fragments.BlockedFragment;
import com.pulseapp.android.fragments.CameraFragment;
import com.pulseapp.android.fragments.CanvasFragment;
import com.pulseapp.android.fragments.ChangeNameFragment;
import com.pulseapp.android.fragments.ChatFragment;
import com.pulseapp.android.fragments.CreateManageGroupFragment;
import com.pulseapp.android.fragments.DashBoardFragment;
import com.pulseapp.android.fragments.FriendSelectionFragment;
import com.pulseapp.android.fragments.FriendsFragment;
import com.pulseapp.android.fragments.InstitutionEditFragment;
import com.pulseapp.android.fragments.MomentListFragment;
import com.pulseapp.android.fragments.PrivacyPolicy;
import com.pulseapp.android.fragments.ReceiveFromFragment;
import com.pulseapp.android.fragments.SettingsFragment;
import com.pulseapp.android.fragments.SettingsFragment.OnDataLoadedCallback;
import com.pulseapp.android.fragments.ShareMediaFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.fragments.ViewChatMediaFragment;
import com.pulseapp.android.fragments.ViewMomentsFragment;
import com.pulseapp.android.fragments.ViewMyMediaFragment;
import com.pulseapp.android.fragments.ViewPublicMomentFragment;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.models.MediaModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.PublicMomentModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.stickers.EmoticonFragment;
import com.pulseapp.android.uploader.mediaUpload;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BaseActivity;
import com.pulseapp.android.util.BitmapWorkerClass;
import com.pulseapp.android.util.CapturedMediaController;
import com.pulseapp.android.util.CustomViewPager;
import com.pulseapp.android.util.MiuiPermissionHandler;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by karthik on 2/29/2016
 * This is the broadcast page: base to all actions in this screen
 *
 * @hint press ctrl+shift+NumPad- and look for functions
 */
@SuppressWarnings("NewApi")
public class CameraActivity extends BaseActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener,
        VideoEditorFragment.ViewControlsCallback, CameraFragment.ViewControlsCallback,
        OnClickListener, OnPageChangeListener, FragmentManager.OnBackStackChangedListener,
        ShareMediaFragment.ViewControlsCallback, FireBaseHelper.MediaModelCallback, FireBaseKEYIDS,
        EmoticonFragment.SwipeListener, mediaUpload.UploadStatusCallbacks,
        SliderMessageListAdapter.ViewControlsCallback, ChatAdapter.ViewControlCallbacks,
        SliderMyMomentAdapter.ViewControlsCallback, DashBoardFragment.ViewControlsCallback,
        ViewMyMediaFragment.ViewControlsCallback, ChatFragment.ViewControlsCallback,
        ViewChatMediaFragment.ViewControlsCallback, MomentListFragment.ViewControlsCallback,
        AroundYouFragment.ViewControlsCallback, ViewMomentsFragment.ViewControlsCallback,
        OnDataLoadedCallback, FriendsFragment.OnDataLoadedCallback, BaseFragment.FacebookController {

    private static final String TAG = "CameraActivity";
    public static final int CUTOFF_SDK = 18;
    public static String eventThumbnailUrl;
    public static boolean isAppForeground = false;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    public static boolean flashMode = false;
    public static boolean PreRecordFilters = false;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec //1 hr
    private static int FATEST_INTERVAL = 5000; // 5 sec //1 min
    private static int DISPLACEMENT = 100; // 10 meters //1 km

    public static final boolean IGNORE_BACK_STACK_LISTENER = false;

    //For using application's context globally
    public static Context applicationContext = null;
    public static LinkedHashMap<String, CustomMomentModel> contributableMoments = new LinkedHashMap<>();

    private boolean VERBOSE = false;

    private ImageView filterImageView;
    private String addressText;

    LetterTileProvider tileProvider;
    int tileSize;
    Resources res;

    /* Event Object Data to access required params */
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private SharedPreferences prefs;
    private Display display;
    boolean isInternetPresent = false;

    LocationManager lm;
    private Location mLastLocation;
    private double mlatitude, mlongitude;

    // Google client to interact with Google API
    public GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private RecordingActionControllerKitkat controller;

    private CustomViewPager viewPager;
    private ImageView cameraSwapButton, flashLayout;

    //    public boolean isNearByMoment;
    private int mPreviousFragment;
    private int mCurrentFragment;

    public boolean isScrolling = false;
    public static Object mVideoLock = new Object();

    private static final int FRAGMENT_CAMERA = 0;
    private static final int FRAGMENT_CANVAS = 1;
    private static final int FRAGMENT_VIDEO_EDITOR = 2;
    private static final int FRAGMENT_SHARE_MEDIA = 3;
    private static final int FRAGMENT_NEW_GROUP = 4;
    private static final int FRAGMENT_DASHBOARD = 5;

    private static final int FRAGMENT_CHAT = 6;
    private static final int FRAGMENT_VIEW_CHAT_MEDIA = 8;
    private static final int FRAGMENT_ALL_CHAT_LIST = 9;
    private static final int FRAGMENT_VIEW_MY_MEDIA = 10;
    private static final int FRAGMENT_CREATE_MANAGE_GROUP = 11;
    private static final int FRAGMENT_ADD_FRIEND = 12;
    private static final int VIEW_MOMENTS_FRAGMENT = 13;
    private static final int ADD_FRIEND_FROM_SHARE = 14;
    private static final int FRAGMENT_SETTINGS = 15;


    private FireBaseHelper fireBaseHelper;
    private GestureDetector gestureDetector;
    public boolean isCanvasFragmentActive = false;
    private FacebookLoginCallback facebookLoginCallback;

    private MomentModel institutionMomentData;
    private UserModel.InstitutionData institutionData;
    private AccessTokenTracker accessTokenTracker;

    public CameraActivity() {
        super.registerForInAppSignals(true);
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    String handleFromIntent;
    boolean isImageIntent = false;
    CallbackManager callbackManager;

    private void registerCallbackManager() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult != null) {
                    String accessToken = loginResult.getAccessToken().getToken();
                    AppLibrary.log_d(TAG, "Permissions granted -" + AccessToken.getCurrentAccessToken().getPermissions());
                    AppLibrary.log_i("OnSuccess, New Facebook Access Token -", accessToken);

                    prefs.edit().putString(AppLibrary.FACEBOOK_ACCESS_TOKEN, accessToken).commit();

                    if (facebookLoginCallback != null)
                        facebookLoginCallback.onSuccessfulLoginCallback(loginResult);
                } else {
                    AppLibrary.log_d(TAG, "On Success, Login result not found");
                    BaseFragment.showShortToastMessage("Sorry! Something went wrong");

                    if (facebookLoginCallback != null)
                        facebookLoginCallback.onErrorCallback();
                }
            }

            @Override
            public void onCancel() {
                AppLibrary.log_d(TAG, "On Cancel");
                BaseFragment.showShortToastMessage("Please provide post permissions to share on facebook");

                if (facebookLoginCallback != null)
                    facebookLoginCallback.onErrorCallback();
            }

            @Override
            public void onError(FacebookException exception) {
                AppLibrary.log_d(TAG, "On Error");
                exception.printStackTrace();
                BaseFragment.showShortToastMessage("Sorry! Something went wrong");

                if (facebookLoginCallback != null)
                    facebookLoginCallback.onErrorCallback();
            }
        });

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                // Set the access token using
                // currentAccessToken when it's loaded or set.
                AccessToken.setCurrentAccessToken(currentAccessToken);
                if (AccessToken.getCurrentAccessToken()!=null && AccessToken.getCurrentAccessToken().getToken() != null
                        && fireBaseHelper.getMyUserId() != null) {
                    HashMap<Object, Object> postObject = new HashMap<>();
                    postObject.put(TYPE, 1);
                    postObject.put(USER_ID, fireBaseHelper.getMyUserId());
                    postObject.put(TOKEN, AccessToken.getCurrentAccessToken().getToken());
                    fireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, postObject);
                }
            }
        };

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        boolean fromIntent = false;
        registerCallbackManager();

        if (intent != null && intent.getExtras() != null && AppLibrary.checkStringObject(intent.getExtras().getString("action")) != null) { //For notifications and other intents

            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
                //No intents to execute when activity launched from history
            } else if (intent.getExtras().getString("action").contains("openImage")) {
                isImageIntent = true;
            } else {
                fromIntent = true;
            }
        }

        setContentView(R.layout.camera_activity);

        if (savedInstanceState != null) { //After being destroyed in the background
            Log.d(TAG, "OnCreate savedInstance not null");
            Intent i = null;
            i = MasterClass.getGlobalContext().getPackageManager().getLaunchIntentForPackage(MasterClass.getGlobalContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            intent = AppLibrary.processIntent(intent, this);

            if (intent != null && intent.getExtras() != null && AppLibrary.checkStringObject(intent.getExtras().getString("action")) != null) {
                Bundle bundle = intent.getExtras();
                i.putExtras(bundle);
            }
            startActivity(i);
            return;
        }

        initResources(fromIntent);
        initializeViewObjects(fromIntent, intent); //Passing fromIntent as true prevents camera from opening, and opens dashboard directly
        initControllers();

        /* instantiating sticker downloader here */
      //  ChatCameraStickerDownloader.getChatCameraStickerDownloader(this);
      //  mFireBaseHelper = FireBaseHelper.getInstance(this);

        if ((fromIntent || isImageIntent) && intent.getExtras() != null && intent.getExtras().getString("action") != null) {
            onNewIntent(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current  state
        savedInstanceState.putString("myStaticVariable", "Test");
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_OPEN_VIA_NOTIFICATION);
        Log.d(TAG, "OnNewIntent called");

        String Uri = null;

        if (intent != null && intent.getExtras() != null && AppLibrary.checkStringObject(intent.getExtras().getString("action")) != null) {
            switch (intent.getExtras().getString("action")) {
                case "friendRequestReceived":
                    requestAddFriendFragmentOpen(); //Handled inside Dashboard fragment
                    break;
                case "friendRequestAccepted":
                    SliderMessageModel friendModel = (SliderMessageModel) intent.getSerializableExtra("model");
                    loadChatFragment(friendModel, true);
                    break;
                case "groupRequestReceived":
                    requestAddFriendFragmentOpen(); //Handled inside Dashboard fragment
                    break;
                case "mediaMessage":
                    SliderMessageModel model = (SliderMessageModel) intent.getSerializableExtra("model");
                    loadChatFragment(model, true);
                    break;
                case "chatMessage":
                    SliderMessageModel sliderMessageModel = (SliderMessageModel) intent.getSerializableExtra("model");
                    loadChatFragment(sliderMessageModel, true);
                    break;
                case "openProfilePopup":
                    if (Uri == null)
                        Uri = intent.getExtras().getString("url");

                    int i = Uri.lastIndexOf("/");
                    String l = Uri.substring(i + 1);

                    if (l.contains("?target_url")) //Via app links deep linking
                        l = l.substring(0, l.indexOf("?"));

                    if (i > 0)
                        getUserDataFromHandle(l);
                    break;
                case "openStream":
                    if (Uri == null)
                        Uri = intent.getExtras().getString("url");

                    int j = Uri.lastIndexOf("/");
                    String s = Uri.substring(j + 1);

                    if (s.contains("?target_url")) //Via app links deep linking
                        s = s.substring(0, s.indexOf("?"));

                    if (j > 0)
                        getStreamDataFromHandle(s);
                    break;

                case "openImage":
                    final String filepath = intent.getExtras().getString("uri");
                    if (filepath != null && !filepath.isEmpty()) {
                        isImageIntent = true;
                        (new Handler()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                launchVideoEditorFragment(filepath);
                            }
                        }, 1000);
                    }
                    break;
                default:
                    break;
            }

            if (!isImageIntent && viewPager != null && viewPager.getCurrentItem() == 1) { //In case the app is in background in camera mode
                viewPager.setCurrentItem(0);
                mCurrentFragment = FRAGMENT_DASHBOARD;
            } else if (isImageIntent && viewPager != null && viewPager.getCurrentItem() == 0) {
                viewPager.setCurrentItem(1);
                mCurrentFragment = FRAGMENT_CAMERA;
            }

            intent.replaceExtras(new Bundle()); //Removes all intents after consumption
            intent.setAction("");
            intent.setData(null);
            intent.setFlags(0);
            isImageIntent = false;
        }
    }

    private FireBaseHelper mFireBaseHelper;

    private void initControllers() {
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    private void initResources(boolean fromIntent) {
        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        this.applicationContext = getApplicationContext();
        res = this.getResources();
        tileSize = res.getDimensionPixelSize(R.dimen.letter_tile_size);
        tileProvider = new LetterTileProvider(this);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        fireBaseHelper = FireBaseHelper.getInstance(this);

        if (fromIntent)
            mCurrentFragment = FRAGMENT_DASHBOARD; //As app is opening with dashboard
        else
            mCurrentFragment = FRAGMENT_CAMERA; //As app is opening with camera
    }

    private void initializeViewObjects(boolean fromIntent, Intent intent) {
        viewPager = (CustomViewPager) findViewById(R.id.viewPager);
        viewPager.setCameraActivity(this);
        viewPager.setOffscreenPageLimit(2);//Need to keep this 2 to avoid canvas fragment
        //from getting garbage collected
        viewPager.setAdapter(new MainFragmentPagerAdapter(getSupportFragmentManager(), fromIntent, intent));
        viewPager.setOnPageChangeListener(this);
        if (fromIntent)
            viewPager.setCurrentItem(0);
        else
            viewPager.setCurrentItem(1);

        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { //By default, all events are intercepted by this listener first
                CanvasFragment canvasFragment = getCanvasFragment();
                CameraFragment cameraFragment = getCameraFragment();

                if (canvasFragment != null && isCanvasFragmentActive) {
                    canvasFragment.dispatchTouchEvent(event);
                    if (canvasFragment.isCanvasLocked()) //If locked, then prevent swipe
                        return true;
                } else if (cameraFragment != null) {
                    cameraFragment.handleTouchEvent(event);
                    if (event.getPointerCount() > 1 || !viewPager.canSwipe) { //To prevent swiping while scaling pre-recording
                        return true;
                    }
                }
                return false;
            }
        });

    }

    public void checkFullScreen() {
        if (mCurrentFragment == FRAGMENT_CANVAS || mCurrentFragment == FRAGMENT_CAMERA || mCurrentFragment == FRAGMENT_VIDEO_EDITOR)
            toggleFullScreen(true);
        else
            toggleFullScreen(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " onResumeCalled ");
        checkFullScreen();
        isAppForeground = true;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Splash.isCameraOpenChecker = false;
        checkPlayServices();

        try {
            if (controller != null) {
                if (getVideoEditorFragment() != null && (getShareMediaFragment() == null ? true : !getShareMediaFragment().isVisible())) //Set can play to true only if its in video editor fragment
                    controller.setCanPlay(true);

                controller.resumeView();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, " onPauseCalled ");

        if (controller != null) {
            controller.pauseView();
            controller.setCanPlay(false);
        }

        DynamicDownloader.notifyScrollDetectedInDashboard();

        AppEventsLogger.deactivateApp(this);
    }

    public static final String ADD_FRIEND_FROM_SHARE_TAG = AddFriendFragment.class.getSimpleName() + "_fromShareMedia";

    @Override
    public void onBackPressed() {
        if (getVideoEditorFragment() != null && getVideoEditorFragment().mSavingVideo) {
            Log.d(TAG, "video is being saved, hence blocked UI");
            return;
        }

        //Handling all back button states
        if (getVideoEditorFragment() != null && (getShareMediaFragment() == null ? true : !getShareMediaFragment().isVisible())) {
            if (getVideoEditorFragment().backButton != null && getVideoEditorFragment().backButton.getVisibility() == View.VISIBLE) {
                getVideoEditorFragment().backButton.performClick();
                return;
            } else if (getVideoEditorFragment().checkAndShowPopup()) {
                return;
            }
        } else if (getVideoEditorFragment() == null && getCameraFragment() != null && getCameraFragment().isVisible() && viewPager.getCurrentItem() == 1) {
            if (getCameraFragment().backButton != null && getCameraFragment().backButton.getVisibility() == View.VISIBLE) {
                getCameraFragment().backButton.performClick();
                return;
            }
        }

        //chatFragment
        if (chatFragment != null && getFragmentInBackStack(ViewChatMediaFragment.class.getSimpleName()) == null
                && chatFragment.onBackPressed()) {
            Log.d(TAG, " chat fragment consumed backPress");
            return;
        }

        //chatListFragment
        if (allChatListFragment != null && allChatListFragment.onBackPressed()) {
            Log.d(TAG, " closed search with this back event");
            return;
        }

        //dashboard
        FragmentManager fm = getSupportFragmentManager();
        Log.d(TAG, " backStack entry count at onBackPress " + fm.getBackStackEntryCount());
        if (fm.getBackStackEntryCount() == 0) {//no back stack entry only dashboard visible
            if (getDashboardFragment() != null && getDashboardFragment().onBackPressed()) {
                Log.d(TAG, "onBackPressed ; dashboard fragment consumed back press ");
                return;
            }
        } else if (fm.getBackStackEntryCount() == 1) {
            Log.d(TAG, " entry == 1 " + fm.getBackStackEntryAt(0).getName());
            if (fm.getBackStackEntryAt(0).getName() != null)
                if (fm.getBackStackEntryAt(0).getName().equals(ChatFragment.class.getSimpleName()) ||
                        fm.getBackStackEntryAt(0).getName().equals(AllChatListFragment.class.getSimpleName()) ||
                        fm.getBackStackEntryAt(0).getName().equals(AddFriendFragment.class.getSimpleName()) ||
                        fm.getBackStackEntryAt(0).getName().equals(ViewChatMediaFragment.class.getSimpleName())) {
                    Log.d(TAG, " coming back to dashboard fragment ");
                    mCurrentFragment = FRAGMENT_DASHBOARD;
                }
        } else if (fm.getBackStackEntryCount() > 1) {
            if (fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName() != null) {
                if (fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName().equals(ADD_FRIEND_FROM_SHARE_TAG)) {
                    Log.d(TAG, " coming back to share screen");
                    mCurrentFragment = FRAGMENT_SHARE_MEDIA;
                } else if (fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName().equalsIgnoreCase(ShareMediaFragment.class.getSimpleName())) {
                    ShareMediaFragment frag = (ShareMediaFragment) fm.findFragmentByTag(ShareMediaFragment.class.getSimpleName());
                    if (frag.onBackPressed())
                        return;
                } else if (fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName().equalsIgnoreCase(FriendSelectionFragment.class.getSimpleName())) {
                    ((FriendSelectionFragment) fm.findFragmentByTag(FriendSelectionFragment.class.getSimpleName())).addNewCustomListToFireBase(FriendSelectionFragment.FROM_IME_BACK);
                    return;
                }

            }
        }


        if (IGNORE_BACK_STACK_LISTENER) {
            super.onBackPressed();
            return;
        }


        if (mCurrentFragment == FRAGMENT_VIDEO_EDITOR) {
            mPreviousFragment = mCurrentFragment;
            mCurrentFragment = FRAGMENT_CAMERA;
            checkAndRemoveEditorFragment();
        } else if (mCurrentFragment == FRAGMENT_NEW_GROUP) {
            mPreviousFragment = mCurrentFragment;
            mCurrentFragment = FRAGMENT_CAMERA; // Not necessary.. but not being used right now.
            getSupportFragmentManager().popBackStack();
        } else if (mCurrentFragment == FRAGMENT_SHARE_MEDIA) {
            ShareMediaFragment shareMediaFragment = getShareMediaFragment();
            if (shareMediaFragment != null) {
                int activeView = shareMediaFragment.getCurrentActiveView();
                if (activeView == 0) {
                    // not active popups or view
                    mPreviousFragment = mCurrentFragment;

                    if (!isCanvasFragmentActive)
                        mCurrentFragment = FRAGMENT_VIDEO_EDITOR;
                    else
                        mCurrentFragment = FRAGMENT_CANVAS;

                    getSupportFragmentManager().popBackStack();
                } else {
                    shareMediaFragment.updateCurrentActiveView();
                }
            }
        } else if (isCanvasFragmentActive && getCanvasFragment() != null && getCanvasFragment().isUiBlocked()) {
            Log.d(TAG, "onBackPressed: consumed back press on canvas fragment processing ");

        } else if (isCanvasFragmentActive && getCanvasFragment().isCanvasLocked()) {
            getCanvasFragment().undoEditChanges();

        } else {
            super.onBackPressed();
        }
    }

    public void minimizeEditor() {
        VideoEditorFragment fragment = getVideoEditorFragment();

        if (fragment != null && getEmoticonFragment() != null && getEmoticonFragment().textInFocus == true) {
            getEmoticonFragment().minimizeEditor();
        }
    }

    private boolean checkAndRemoveEditorFragment() {
        AppLibrary.log_d(TAG, "Removing video editor fragment");
        VideoEditorFragment fragment = getVideoEditorFragment();
        if (fragment != null)
            fragment.destroyRenderer();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
//        viewPager.setCurrentItem(1);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        Log.d(TAG, " onDestroyCalled ");
        isAppForeground = false;
        if (controller != null) {
            controller = null;
        }

        if (accessTokenTracker != null)
            accessTokenTracker.stopTracking();

        if (!CameraFragment.mVisible) { //Reset visibility if onDestroy is called
            CameraFragment.mVisible = true;
        }
        AnalyticsManager.getInstance().flush();
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal event) {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Should never be called as the app is locked at portrait
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, " onStopCalled ");
        isAppForeground = false;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_EXIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, " onStartCalled ");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (callbackManager != null)
            callbackManager.onActivityResult(requestCode, resultCode, data);

        //Add code to pass the activity result to fragments
        Fragment fragment = (Fragment) getVideoEditorFragment();
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        } else {
            if (getDashboardFragment() != null) {
                Fragment aroundYoufragment = getDashboardFragment().aroundYouFragment;
                if (aroundYoufragment != null) {
                    aroundYoufragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }

        //For the invite in Add Friends Fragment
        MasterClass.callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    // generic function to check if GPS is enabled or not
    public boolean isGpsEnabled() {
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return false;
        } else {
            return true;
        }
    }

    public void getAddress(final Location location) {
        Geocoder geo = new Geocoder(this, Locale.getDefault());

        if (Geocoder.isPresent()) {
            try {
                List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    addressText = String.format("%s", address.getLocality());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     */
    public void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(this,
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     */
    @SuppressWarnings("MissingPermission")
    public void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            AppLibrary.log_d(TAG, "GoogleLocationClient Connected");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                mlatitude = mLastLocation.getLatitude();
                mlongitude = mLastLocation.getLongitude();
                AppLibrary.log_d(TAG, "Location : " + "latitude- " + mlatitude + " longitude- " + mlongitude);

                getAddress(mLastLocation); //If address is required

            } else {
                //location object null
                //default location is previous location
                AppLibrary.log_d(TAG, "Location object null");
            }
        } else {
            AppLibrary.log_d(TAG, "GoogleLocationClient not Connected, or is Streaming");
        }
    }

    public Location getLastLocation() {
        return mLastLocation;
    }

    public double getLatitude() {
        return mlatitude;
    }

    public double getLongitude() {
        return mlongitude;
    }

    /* Stopping location updates */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        AppLibrary.log_i("Connection", "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
        //startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        //Not sure if we should use new lat long if location changed
    }

    public void startMediaUpload(String pendingMediaId, MediaModel mediaModel) {
        if (mediaModel == null) return;

        String mediaPath = mediaModel.url;
        if (mediaPath != null && !mediaPath.isEmpty()) {
            String videoPath = mediaPath;
            String name = "media";
            File file = new File(videoPath);
            String uniqueUploadName = prefs.getString(AppLibrary.USER_LOGIN, "Name") + System.currentTimeMillis(); //Always unique
            mediaUpload mediaUploader = new mediaUpload(CameraActivity.this, uniqueUploadName, pendingMediaId, mediaModel);

            if (file != null && file.exists()) {
                AppLibrary.log_e(TAG, "Starting media upload");
                mediaUploader.startUpload(file, name);
            } else {
                AppLibrary.log_e(TAG, "Media upload - file doesn't exist");
                //ToDo - Handle case when file doesn't exist
            }
        } else {
            AppLibrary.log_e(TAG, "Parse object is null in startMediaUpload");
            // something went wrong
        }
    }

    private void saveToLocalData(boolean facebookPost, boolean postAnonymous, long timeDuration, int action_type, HashMap<String, String> selectedRoomsForMoment, String mediaPath, HashMap<String, Integer> momentList, HashMap<String, Integer> roomList, int expiryType, String mediaText, String pendingMediaKey) {
        FireBaseHelper firebase = FireBaseHelper.getInstance(this);
        long createdAt = firebase.getUpdatedAt();
        MediaModel mediaModel = new MediaModel(mediaText, prefs.getString(AppLibrary.USER_LOGIN, ""), mediaPath, AppLibrary.getMediaType(mediaPath), true, false, createdAt);
        mediaModel.duration = timeDuration;
        mediaModel.expiryType = expiryType;
        mediaModel.userId = firebase.getMyUserId();
        mediaModel.anonymous = postAnonymous;
        mediaModel.facebookPost = facebookPost;
        mediaModel.source = ANDROID;
        HashMap<String, Object> userDetail = new HashMap<String, Object>();
        userDetail.put(NAME, firebase.getMyUserModel().name);
        userDetail.put(HANDLE, firebase.getMyUserModel().handle);
        mediaModel.userDetail = userDetail;
        mediaModel.privacy = new MediaModel.Privacy(action_type, selectedRoomsForMoment);
        mediaModel.addedTo = new MediaModel.AddedTo(momentList, roomList);
        if (mediaModel.addedTo.moments != null && mediaModel.addedTo.moments.size() > 0) {
            HashMap<String, Integer> momentSet = mediaModel.addedTo.moments;
            mediaModel.momentDetails = new HashMap<>();
            for (Map.Entry<String, Integer> entrySet : momentSet.entrySet()) {
                if (contributableMoments.containsKey(entrySet.getKey())) {
                    PublicMomentModel momentModel = new PublicMomentModel(entrySet.getKey(), contributableMoments.get(entrySet.getKey()).autoModerate, contributableMoments.get(entrySet.getKey()).thumbnailUrl, contributableMoments.get(entrySet.getKey()).name);
                    mediaModel.momentDetails.put(entrySet.getKey(), momentModel);
                }
            }
        }
        String pendingMediaId = fireBaseHelper.updatePendingUploads(mediaModel, pendingMediaKey);
        startMediaUpload(pendingMediaId, mediaModel);
    }

    public static Context getApplicationContextPublic() {
        return applicationContext;
    }

    public void putNewProfileImage(Bitmap bitmap, String userid, boolean triggerLike) {
        //Add new bitmap fetched from background
    }

    public void createBitmapInBackground(Bitmap profileBitmap, String userId) {
        if (VERBOSE)
            AppLibrary.log_d(TAG, "Inside createLikeBitmap, Bitmap is -" + profileBitmap);

        //Creates like bitmap in the background - Any form of background bitmap creation can be done in this function
        BitmapWorkerClass task = new BitmapWorkerClass(profileBitmap, this, userId, false);
        task.execute();
    }

    private CameraFragment getCameraFragment() {
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + 1);
        // based on the current position you can then cast the page to the correct
        // class and call the method:
        if (page != null) {
            return (CameraFragment) page;
        } else {
            return null;
        }
    }

    public DashBoardFragment getDashboardFragment() {
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + 0);
        // based on the current position you can then cast the page to the correct
        // class and call the method:
        if (page != null) {
            return (DashBoardFragment) page;
        } else {
            return null;
        }
    }

    private VideoEditorFragment getVideoEditorFragment() {
        return (VideoEditorFragment) getSupportFragmentManager().findFragmentByTag("videoEditorFragment");
    }

    private EmoticonFragment getEmoticonFragment() {
        Fragment page = getSupportFragmentManager().findFragmentByTag("emoticonFragment");

        if (page != null)
            return (EmoticonFragment) page;
        else
            return null;
    }


    private CanvasFragment getCanvasFragment() {
        CameraFragment cameraFragment = getCameraFragment();
        if (cameraFragment != null) {
            Fragment fragment = cameraFragment.getChildFragmentManager().findFragmentByTag("canvasFragment");
            if (fragment != null)
                return (CanvasFragment) fragment;
            else
                return null;
        } else {
            return null;
        }
    }

    private ShareMediaFragment getShareMediaFragment() {
        return (ShareMediaFragment) getSupportFragmentManager().findFragmentByTag(ShareMediaFragment.class.getSimpleName());
    }

    private void updateTextEditorViews() {
        findViewById(R.id.topLayerLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.cameraControlsLayout).setVisibility(View.GONE);
        findViewById(R.id.textOptionLayout).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
    }

    @Override
    public void launchVideoEditorFragment(Bitmap bitmap) {
        contributableMoments.clear();
//        isMyStreamsChecked = null;
        savedStateOfShareMediaFragment = new SavedStateOfShareMediaFragment(null, false, false);
        findViewById(R.id.topLayerLayout).setVisibility(View.GONE);
        mCurrentFragment = FRAGMENT_VIDEO_EDITOR;
        mPreviousFragment = mCurrentFragment - 1;

        CameraFragment cameraFragment = getCameraFragment();
        if (cameraFragment != null) {
            cameraFragment.releaseCamera();
        }

        CapturedMediaController.getInstance().setCapturedBitmap(bitmap);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        VideoEditorFragment videoEditorFragment = new VideoEditorFragment();
        videoEditorFragment.setPublicMomentModel(publicStreamContribution);
        fragmentTransaction.replace(R.id.fragmentContainer, videoEditorFragment, "videoEditorFragment");
        fragmentTransaction.addToBackStack(VideoEditorFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.CLICK_PICTURE);
    }

    @Override
    public void launchVideoEditorFragment(String uri) {
        contributableMoments.clear();
        savedStateOfShareMediaFragment = new SavedStateOfShareMediaFragment(null, false, false);

        if (findViewById(R.id.topLayerLayout) == null)
            return; //Handling awkward states of recreation

        findViewById(R.id.topLayerLayout).setVisibility(View.GONE);
        mCurrentFragment = FRAGMENT_VIDEO_EDITOR;
        mPreviousFragment = mCurrentFragment - 1;

        if (AppLibrary.getMediaType(uri) == AppLibrary.MEDIA_TYPE_IMAGE || AppLibrary.getMediaType(uri) == AppLibrary.MEDIA_TYPE_VIDEO) {
            CameraFragment cameraFragment = getCameraFragment();
            if (cameraFragment != null)
                cameraFragment.releaseCamera();
        }

        toggleFullScreen(true);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        VideoEditorFragment fragment = new VideoEditorFragment();
        fragment.setPublicMomentModel(publicStreamContribution);
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.SELECTED_MEDIA_PATH, uri);
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragmentContainer, fragment, "videoEditorFragment");
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();

        if (AppLibrary.getMediaType(uri) == AppLibrary.MEDIA_TYPE_IMAGE) {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SELECT_GALLERY_IMAGE);
        } else if (AppLibrary.getMediaType(uri) == AppLibrary.MEDIA_TYPE_VIDEO) {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.RECORD_VIDEO);
        }
    }

    public static SavedStateOfShareMediaFragment savedStateOfShareMediaFragment;

    public static class SavedStateOfShareMediaFragment { //Maintains the share states of the share media fragment
        public Boolean isMyStreamsChecked;//null will represent that my stream hasn't been clicked even once otherwise
        //it would have been true or false
        public boolean isAnonymitySelected;
        public boolean isFacebookButtonChecked;

        public SavedStateOfShareMediaFragment(Boolean isMyStreamsChecked, boolean isAnonymitySelected, boolean isFacebookButtonChecked) {
            this.isMyStreamsChecked = isMyStreamsChecked;
            this.isAnonymitySelected = isAnonymitySelected;
            this.isFacebookButtonChecked = isFacebookButtonChecked;
        }
    }

    void autoCheckMarkStreams() {
        CustomMomentModel publicStreamContribution = getPublicStreamContribution();
        if (publicStreamContribution != null) {//if coming form public contribution
            for (Map.Entry<String, CustomMomentModel> entry : CameraActivity.contributableMoments.entrySet()) {
                if (entry.getKey().equals(publicStreamContribution.momentId)) {
                    entry.getValue().isChecked = true;
                    break;
                }
            }
        } else {//normal flow not from public stream
            for (Map.Entry<String, CustomMomentModel> entry : CameraActivity.contributableMoments.entrySet()) {
                if (BaseFragment.isThisMyInstitutionId(entry.getKey())) {
                    entry.getValue().isChecked = true;
                    break;
                }
            }
        }
    }

    @Override
    public void onLaunchCanvasFragment() {
        contributableMoments.clear();
        savedStateOfShareMediaFragment = new SavedStateOfShareMediaFragment(null, false, false);

        isCanvasFragmentActive = true;
        mCurrentFragment = FRAGMENT_CANVAS;
        updateTextEditorViews();

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.OPEN_CANVAS);
    }

    public boolean isCanvasFragmentActive() {
        return isCanvasFragmentActive;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        DynamicDownloader.notifyScrollDetectedInDashboard();
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentFragment = position; // the positions must correspond to the Fragment Enumeration.
        Log.d(TAG, "viewPager onPageSelected " + position);

        if (position == 0)
            mCurrentFragment = FRAGMENT_DASHBOARD;
        else
            mCurrentFragment = FRAGMENT_CAMERA;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

        // remove any tuts if visible
        getCameraFragment().removeTuts();

        Log.d(TAG, "viewPager onPageScrollStateChanged " + state);
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            isScrolling = true;

            if (mCurrentFragment == FRAGMENT_DASHBOARD) {
                toggleFullScreen(true);
                if (!isCanvasFragmentActive)
                    startCameraPreview(CAMERA_PREVIEW_START_DELAY);
            }

        } else if (state == ViewPager.SCROLL_STATE_IDLE) {

            isScrolling = false;

            if (mCurrentFragment == FRAGMENT_DASHBOARD) {
                toggleFullScreen(false);
                if (!isCanvasFragmentActive) {
                    stopCameraPreview();
                    MasterClass.getEventBus().post(new BroadCastSignals.DoubleShotSignal(false));
                } else if (getCanvasFragment() != null) {
                    getCanvasFragment().revealView.setVisibility(View.GONE);
                    getCameraFragment().onHideCanvasFragmentWithoutCameraPreview();
                }
            } else { //Refresh moments list when swiped to camera screen
                if (getDashboardFragment() != null) {
                    if (getDashboardFragment().aroundYouFragment != null)
                        getDashboardFragment().aroundYouFragment.refreshPositionsOfSeenMoments();
                    if (getDashboardFragment().getFriendsMomentsFragment() != null)
                        getDashboardFragment().getFriendsMomentsFragment().refreshSeenUnseenOnCameraSwipe();
                }
            }
        }
    }

    void setMCurrentFragment(int backCount) {
        //todo replace with switch case
        if (backCount == 0) {
            if (viewPager.getCurrentItem() == 0)
                mCurrentFragment = FRAGMENT_DASHBOARD;
            else if (!isCanvasFragmentActive)
                mCurrentFragment = FRAGMENT_CAMERA;

        } else { // backCount > 0 , quering top most element
            String tag = (getSupportFragmentManager().getBackStackEntryAt(backCount - 1).getName());
            if (tag == null) {
                Log.e(TAG, " fragment tag not provided during transaction");
                return;
            }
            if (tag.equals(ChatFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_CHAT;
                mPreviousFragment = -1;
            } else if (tag.equals(AllChatListFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_ALL_CHAT_LIST;
                mPreviousFragment = -1;
            } else if (tag.equals(ViewChatMediaFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_VIEW_CHAT_MEDIA;
                mPreviousFragment = -1;
            } else if (tag.equals(CreateManageGroupFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_CREATE_MANAGE_GROUP;
                mPreviousFragment = -1;
            } else if (tag.equals(AddFriendFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_ADD_FRIEND;
                mPreviousFragment = -1;
            } else if (tag.equals(ViewMyMediaFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_VIEW_MY_MEDIA;
                mPreviousFragment = -1;
            } else if (tag.equals(ViewMomentsFragment.class.getSimpleName())) {
                mCurrentFragment = VIEW_MOMENTS_FRAGMENT;
                mPreviousFragment = -1;
            } else if (tag.equals(ShareMediaFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_SHARE_MEDIA;
                mPreviousFragment = -1;
            } else if (tag.equals(ADD_FRIEND_FROM_SHARE_TAG)) {
                mCurrentFragment = ADD_FRIEND_FROM_SHARE;
                mPreviousFragment = -1;
            } else if (tag.equals(SettingsFragment.class.getSimpleName())) {
                mCurrentFragment = FRAGMENT_SETTINGS;
                mPreviousFragment = -1;
            }
        }
    }

    @Override
    public void onBackStackChanged() {
        if (IGNORE_BACK_STACK_LISTENER)
            return;

        // remove tuts if any
        getCameraFragment().removeTuts();

        int backCount = getSupportFragmentManager().getBackStackEntryCount();
        Log.d(TAG, "onBackStackChanged: backStack count " + backCount);
        for (int i = 0; i < getSupportFragmentManager().getBackStackEntryCount(); i++) {
            Log.d(TAG, "onBackStackChanged: fragment " + getSupportFragmentManager().getBackStackEntryAt(i).getName());
        }

        DynamicDownloader.notifyScrollDetectedInDashboard(); //Clear waiting list just in case

        Log.d(TAG, " going to set mcurrentFragment ");
        setMCurrentFragment(backCount);
        Log.d(TAG, " setting mCurrentFragment to .. " + mCurrentFragment);

        Log.d(TAG, "OnBackStackChanged :: " + "previous frag: " + mPreviousFragment + " current frag: " + mCurrentFragment);
        if (isCanvasFragmentActive) {
            // Do Nothing
        } else if (mPreviousFragment >= mCurrentFragment) {
            CameraFragment cameraFragment = getCameraFragment();
            VideoEditorFragment videoEditorFragment = getVideoEditorFragment();
            switch (mCurrentFragment) {
                case FRAGMENT_CAMERA:
                    if (cameraFragment != null) {
                        cameraFragment.setVisibility(true);
                        cameraFragment.onResume();
                    }
                    break;
                case FRAGMENT_VIDEO_EDITOR:
                    if (videoEditorFragment != null) {
                        videoEditorFragment.setCanPlay(true);
                        videoEditorFragment.resumeMediaView();
                    }
                    break;
                case FRAGMENT_SHARE_MEDIA:
                    mCurrentFragment = FRAGMENT_VIDEO_EDITOR;
                    break;
                case FRAGMENT_NEW_GROUP:
                    mCurrentFragment = FRAGMENT_SHARE_MEDIA;
                    break;
                default:
                    Log.d(TAG, "Unknown fragment");
            }
        } else {
            CameraFragment cameraFragment = getCameraFragment();
            VideoEditorFragment videoEditorFragment = getVideoEditorFragment();
            switch (mCurrentFragment) {
                case FRAGMENT_CAMERA:
                    if (cameraFragment != null) {
                        cameraFragment.setVisibility(true);
                        cameraFragment.onResume();
                    }
                    break;
                case FRAGMENT_VIDEO_EDITOR:
                    if (cameraFragment != null) {
                        cameraFragment.setVisibility(false);
                        cameraFragment.translatePreview();
                    }
                    break;
                case FRAGMENT_SHARE_MEDIA:
                    if (videoEditorFragment != null) {
                        videoEditorFragment.setCanPlay(false);
                    }
            }
        }
    }

    @Override
    public void clearEditorFragment() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        });
    }

    public void toggleFullScreen(final boolean goFullScreen) {

        if (goFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override//from videoEditor
    public void launchShareMediaFragment(String mediaPath, String mediaText) {
        mPreviousFragment = mCurrentFragment;
        mCurrentFragment = FRAGMENT_SHARE_MEDIA;
        Bundle bundle = new Bundle();
        bundle.putString("mediaPath", mediaPath);
        bundle.putString(AppLibrary.MEDIA_TEXT, mediaText);
        ShareMediaFragment fragment = new ShareMediaFragment();
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, fragment, ShareMediaFragment.class.getSimpleName()).addToBackStack(ShareMediaFragment.class.getSimpleName()).commitAllowingStateLoss();
    }

    @Override//from canvas
    public void launchShareFragment(String absolutePath, String s) {
        launchShareMediaFragment(absolutePath, s);
    }

    @Override
    public void setViewpagerSwipable(boolean enable) {
        if (viewPager != null)
            viewPager.setCanSwipe(enable);
    }

    @Override
    public CustomViewPager getParentViewPager() {
        return viewPager;
    }

    @Override
    public GLSurfaceView getCameraView() {
        return (GLSurfaceView) findViewById(R.id.surfaceView);
    }

    @Override
    public void onCanvasFragmentHide(boolean isCameraActive) {
        isCanvasFragmentActive = false;
        CanvasFragment.getContributableStreams = true; //Reset to check again if Canvas fragment gets active again

        if (isCameraActive)
            mCurrentFragment = FRAGMENT_CAMERA;
        else
            mCurrentFragment = FRAGMENT_DASHBOARD;
    }

    @Override
    public void onNewGroupButtonClicked() {
        mCurrentFragment = FRAGMENT_NEW_GROUP;
    }

    @Override
    public void uploadMediaToFireBase(final boolean facebookPost, final boolean postAnonymous, final int action_type, final HashMap<String, String> selectedRoomsForMoment, final String mediaPath, final HashMap<String, Integer> momentList, final HashMap<String, Integer> roomList, final int expiryType, final String mediaText, @Nullable final CustomMomentModel publicStreamContribution) {
        if (isCanvasFragmentActive && getCanvasFragment().isCanvasLocked()) { //Clear canvas fragment if active, and reset to camera fragment
            getCanvasFragment().undoEditChanges();
            getCanvasFragment().revealView.setVisibility(View.GONE);
            getCameraFragment().onHideCanvasFragmentWithoutCameraPreview();
        }
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        viewPager.setCurrentItem(0);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long createdAt = FireBaseHelper.getInstance(getApplicationContext()).getUpdatedAt();
                final MediaModel mediaModel = new MediaModel(mediaText, prefs.getString(AppLibrary.USER_LOGIN, ""), mediaPath, AppLibrary.getMediaType(mediaPath), true, false, createdAt);
                mediaModel.expiryType = expiryType;
                mediaModel.anonymous = postAnonymous;
                mediaModel.facebookPost = facebookPost;
                mediaModel.privacy = new MediaModel.Privacy(action_type, selectedRoomsForMoment);
                mediaModel.addedTo = new MediaModel.AddedTo(momentList, roomList);
                final String pendingMediaId = mFireBaseHelper.getPendingMediaKey();
                synchronized (mVideoLock) {
                    try {
                        if (controller != null && !controller.isPublishing()) {
                            // Don't wait, video already encoded or picture already created
                            Log.d(TAG, "Item ready before sharing");
                        } else if (controller != null && controller.isPublishing() && mediaPath.toLowerCase().endsWith(".mp4")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUploadingStatus(mediaModel, pendingMediaId, UPLOADING_NOT_READY_VIDEO);
                                }
                            });
                            mVideoLock.wait(15000); //Wait for at max 15 seconds
                            Log.d(TAG, "Video encoding ongoing before sharing");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    File savedFile = new File(mediaPath); //Being doubly sure that the file now exists at the path, else will crash
                    if (savedFile.exists()) {
                        long timeInMillisec = 0;
                        if (AppLibrary.getMediaType(mediaPath) == AppLibrary.MEDIA_TYPE_VIDEO) {
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            String time = null;
                            try {
                                retriever.setDataSource(MasterClass.context, Uri.fromFile(savedFile));
                                time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (time != null && !time.isEmpty()) {
                                timeInMillisec = Long.parseLong(time);
                                Log.d(TAG, "Video time is " + timeInMillisec);
                            } else {
                                timeInMillisec = 15000;
                            }
                        } else {
                            timeInMillisec = 5000;
                        }

                        saveToLocalData(facebookPost, postAnonymous, timeInMillisec, action_type, selectedRoomsForMoment, mediaPath, momentList, roomList, expiryType, mediaText, pendingMediaId);
                    }

                    if (controller != null) { //As we need to do this post encoding the video
                        Log.e(TAG, "Destroying Movie Player and renderer");
                        controller.pauseView();
                        controller.setRenderTargetType(0);
                        controller.stopAndCleanupPlayer(true);
                        if (controller.isPublishing())
                            controller.actionPublish();

                        controller.destroy();
                        controller = null;
                        MoviePlayer.ENABLE_AUDIO = true; //Reset any audio changes
                    }
                }
            }
        };
        MasterClass.uploadHandler.post(r);

        if (publicStreamContribution != null) {
            Runnable runnable = new Runnable() {
                public void run() {
                    if (momentList.containsKey(publicStreamContribution.momentId))
                        BaseFragment.showShortToastMessage("Contributed to " + publicStreamContribution.name);

                    goBackToStream();
                    Log.d(TAG, "uploadMediaToFireBase: " + publicStreamContribution.name);
                }
            };
            this.runOnUiThread(runnable);
        } else Log.d(TAG, "uploadMediaToFireBase: null");
    }


    @SuppressWarnings("deprecation")
    @Override
    public void fetchContributableMoments(Double latitude, Double longitude) {
        AppLibrary.log_d(TAG, "Getting Contributable moments");

        if (mFireBaseHelper.getMyUserModel().miscellaneous != null) {
            institutionData = mFireBaseHelper.getMyUserModel().miscellaneous.institutionData;
        }

        institutionMomentData = mFireBaseHelper.getMyInstituteModel();

        if (institutionMomentData != null && institutionData != null && institutionMomentData.flags != null && institutionMomentData.flags.type == AppLibrary.INSTITUTION_ACTIVE) {
            CustomMomentModel customMomentModel = new CustomMomentModel(institutionData.momentId, institutionMomentData.autoModerate,
                    institutionMomentData.thumbnailUrl, institutionMomentData.name);
            customMomentModel.totalViews = institutionMomentData.totalViews;
            contributableMoments.put(institutionData.momentId, customMomentModel);
        }

        if (!isInternetAvailable(false) || mFireBaseHelper.getMyUserModel() == null) {
            Log.e(TAG, "Not hitting api; no internet OR no user model");
            return;
        }

        final List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("user", mFireBaseHelper.getMyUserId()));

        if (latitude != null && longitude != null) {
            pairs.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            pairs.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
        }


        RequestManager.makePostRequest(CameraActivity.this, RequestManager.GET_CONTRIBUTABLE_MOMENT_REQUEST, RequestManager.GET_CONTRIBUTABLE_MOMENT_RESPONSE,
                null, pairs, contributableMomentResponse);
    }

    RequestManager.OnRequestFinishCallback contributableMomentResponse = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        Log.d(TAG, "getContributable moments Success, response -" + object.getString("value"));
                        if (object.getJSONArray("value") != null && object.getJSONArray("value").length() > 0) {
                            JSONArray momentArray = object.getJSONArray("value");
                            for (int i = 0; i < momentArray.length(); i++) {
                                JSONObject jsonObject = momentArray.getJSONObject(i);
                                final CustomMomentModel customMomentModel = new CustomMomentModel(jsonObject.getString(MONGO_ID), false, jsonObject.getString(THUMBNAIL), jsonObject.getString(NAME));
                                customMomentModel.autoModerate = jsonObject.getBoolean(AUTO_MODERATE);
                                customMomentModel.totalViews = jsonObject.getLong(TOTAL_VIEWS);

                                if (contributableMoments.get(customMomentModel.momentId) == null) {
                                    contributableMoments.put(customMomentModel.momentId, customMomentModel);
                                } else {
                                    boolean isChecked = contributableMoments.get(customMomentModel.momentId).isChecked;
                                    contributableMoments.put(customMomentModel.momentId, customMomentModel);
                                    customMomentModel.isChecked = isChecked;
                                }
                            }

                            writeContributableMomentsToFireBase(contributableMoments);

                            ShareMediaFragment shareMediaFragment = getShareMediaFragment();
                            if (shareMediaFragment != null)
                                shareMediaFragment.updateCustomMomentView();
                            else
                                autoCheckMarkStreams(); //To auto-check mark public stream or institution
                        }
                    } else {
                        Log.e(TAG, "getContributable moments Error, response -" + object.getString("value"));
                    }
                } else {
                    Log.e(TAG, "getContributable moments Error, response -" + object);
                    // request failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "getContributable moments JsonException " + e);
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };

    private void writeContributableMomentsToFireBase(LinkedHashMap<String, CustomMomentModel> contributableMomentsFetchedFromServer) {
        DatabaseReference newFireBase = mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{mFireBaseHelper.getMyUserId(), CONTRIBUTABLE_MOMENTS});
        newFireBase.child(UPDATED_AT).setValue(System.currentTimeMillis());
        newFireBase = newFireBase.child(MOMENTS);
        newFireBase.setValue(null);
        for (Map.Entry<String, CustomMomentModel> entry : contributableMomentsFetchedFromServer.entrySet()) {
            CustomMomentModel model = contributableMomentsFetchedFromServer.get(entry.getKey());

            DatabaseReference childRef = newFireBase.child(model.momentId);
            childRef.child(AUTO_MODERATE).setValue(model.autoModerate);
            childRef.child(TOTAL_VIEWS).setValue(model.totalViews);
            childRef.child(NAME).setValue(model.name);
            childRef.child(THUMBNAIL).setValue(model.thumbnailUrl);
        }
    }

    private ChatFragment chatFragment;
    public static SliderMessageModel currentRoomBeingOpened;

    public void loadChatFragmentOnDoubleTap(String roomId) {
        SliderMessageModel messageModel = fireBaseHelper.getMessageList().get(roomId);
        if (messageModel != null)
            loadChatFragment(messageModel, false);
        else Toast.makeText(this, "Sorry, unable to open chat", Toast.LENGTH_SHORT).show();
    }

    public void loadChatFragment(SliderMessageModel data, boolean moveSliderToMiddle) {//pos not required; data has complete info

        if (AppLibrary.MESSAGING_DISABLED) return;

        if (currentRoomBeingOpened != null) {
            Log.e(TAG, " ignoring multiple request to openChat Fragment");
            return;
        }

        chatFragment = new ChatFragment();
        if (data == null)
            throw new RuntimeException(" cannot openChats without sliderModel ");

        currentRoomBeingOpened = data;

        if (moveSliderToMiddle && getDashboardFragment() != null)
            getDashboardFragment().dismissSliderOnChatOpen();

        Log.d(TAG, " opening room with Id " + data);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter_res_id_exit, R.anim.exit_res_id_exit,
                R.anim.exit_res_id_start, R.anim.enter_res_id_start);
        fragmentTransaction.add(R.id.chatFrame, chatFragment, ChatFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ChatFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public ChatFragment getChatFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ChatFragment.class.getSimpleName());
        if (fragment != null)
            return (ChatFragment) fragment;
        else
            return null;
    }

    public void setRecordingActionControllerReference(RecordingActionControllerKitkat controller) {
        this.controller = controller;
    }

    private AllChatListFragment allChatListFragment;

    public void openFriendListFragment() {
        allChatListFragment = new AllChatListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, allChatListFragment, AllChatListFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(AllChatListFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void loadCreateGroupFragment() {
        CreateManageGroupFragment fragment = new CreateManageGroupFragment();
        Bundle data = new Bundle();
        data.putInt(AppLibrary.GROUP_ACTION, AppLibrary.GROUP_ACTION_CREATE);
        fragment.setArguments(data);

        Log.d(TAG, " opening create group fragment ");
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, fragment, CreateManageGroupFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(CreateManageGroupFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void loadViewMyMediaFragment(String mediaId, String momentId) {
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.MEDIA_ID, mediaId);
        bundle.putString(AppLibrary.MOMENT_ID, momentId);
        ViewMyMediaFragment fragment = new ViewMyMediaFragment();
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, fragment, ViewMyMediaFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ViewMyMediaFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onCloseViewMyMediaFragment() {
        try {
            if (getFragmentInBackStack(ViewMyMediaFragment.class.getSimpleName()) != null) {
                boolean popped = getSupportFragmentManager().popBackStackImmediate();
                Log.d(TAG, "onCloseViewMyMediaFragment: popped " + popped);
            } else Log.d(TAG, "onCloseViewMyMediaFragment: fragment is null");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            AppLibrary.goBackFromTrulyFullScreen(w);
        }

        checkFullScreen();
    }

    @Override
    public void onDeleteMyMedia() {
        DashBoardFragment fragment = getDashboardFragment();
        if (fragment != null) {
            fragment.onDeleteMyMomentMedia();
        }
    }

    @Override
    public void onPendingMediaListLoaded(MediaModel mediaModel, String pendingId) {
        startMediaUpload(pendingId, mediaModel);
    }

    @Override
    public void onPendingMediaForMessageLoaded(MediaModel mediaModel, String pendingId, String roomId, String messageId) {
        startMediaUpload(pendingId, mediaModel);
        mFireBaseHelper.updateMediaMessageUploadStarting(roomId, pendingId);
    }

    public void scrollToCameraFragment() {
        viewPager.setCurrentItem(1, true);
        this.toggleFullScreen(true);
        startCameraPreview(CAMERA_PREVIEW_START_DELAY);
    }

    final int CAMERA_PREVIEW_START_DELAY = 0;//millisecs

    public void stopCameraPreview() {
        CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + "1");
        cameraFragment.pauseCameraPreview();
    }

    public void startCameraPreview(int delay) {
        final CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + "1");
        cameraFragment.resumeCameraPreview();
    }

    public void requestAddFriendFragmentOpen() {
        final DashBoardFragment dashBoardFragment = (DashBoardFragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + "0");
        if (dashBoardFragment != null)
            dashBoardFragment.loadAddFriendFriendFragment();
    }

    @Override
    public void onSwipeLeft() {
        VideoEditorFragment videoEditorFragment = getVideoEditorFragment();
        if (videoEditorFragment != null) {
            videoEditorFragment.filterSwipeLeft();
        }
    }

    @Override
    public void onSwipeRight() {
        VideoEditorFragment videoEditorFragment = getVideoEditorFragment();
        if (videoEditorFragment != null) {
            videoEditorFragment.filterSwipeRight();
        }
    }

    @Override
    public void updateUploadingStatus(final MediaModel mediaModel, final String mediaId, final int status) {

        if (status == MEDIA_UPLOADING_COMPLETE)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.UPLOADING_COMPLETE);
        else if (status == MEDIA_UPLOADING_FAILED)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.UPLOADING_FAILED);

        final DashBoardFragment fragment = getDashboardFragment();
        if (fragment != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragment.updateMediaUploadStatus(mediaModel, mediaId, status);
                }
            });
        }
    }

    @Override
    public void onResumeUpload(String roomId) {
        // resume failed upload of only that media which the user clicked to retry
        if (fireBaseHelper != null && roomId != null) {
            fireBaseHelper.setMediaModelCallback(this);
            fireBaseHelper.fetchPendingUploadsForRoom(roomId);
        }
    }

    @Override
    public void onUploadRetryClicked(String mediaId) {
        if (fireBaseHelper != null && mediaId != null) {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.RETRY_UPLOAD);

            fireBaseHelper.setMediaModelCallback(this);
            fireBaseHelper.fetchPendingUploadsForMedia(mediaId);
        }
    }

    @Override
    public void updateLatestMediaTime(String time, String momentId) {
        DashBoardFragment fragment = getDashboardFragment();
        if (fragment != null) {
            fragment.updateLatestMediaTime(time, momentId);
        }
    }

    @Override
    public void onUploadRetryClickedForAllMediaInMoments() {
        if (fireBaseHelper != null) {
            fireBaseHelper.setMediaModelCallback(this);
            fireBaseHelper.fetchPendingUploadsForMyMoments();
        }
    }

    @Override
    public void onUploadRetryClickedForAllMediaMessages() {
        if (fireBaseHelper != null) {
            fireBaseHelper.setMediaModelCallback(this);
            fireBaseHelper.fetchPendingUploadsForAllRoom();
        }
    }

    public void onUnSeenItemsChanges(int unSeenCount) {
        CameraFragment cameraFragment = getCameraFragment();
        if (cameraFragment != null && !this.isPublicStreamContributionMode()) {
            cameraFragment.updateUnSeenItemsCount(unSeenCount);
        }
    }

    @Override
    public void onLoadViewChatMediaFragment(String roomId, int roomType, String mediaId, String messageId) {
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.MEDIA_ID, mediaId);
        bundle.putString(AppLibrary.ROOM_ID, roomId);
        bundle.putString(AppLibrary.MESSAGE_ID, messageId);
        bundle.putInt(AppLibrary.ROOM_TYPE, roomType);
        ViewChatMediaFragment viewChatMediaFragment = new ViewChatMediaFragment();
        viewChatMediaFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.viewChatMomentContainer, viewChatMediaFragment, ViewChatMediaFragment.class.getSimpleName()).
                addToBackStack(ViewChatMediaFragment.class.getSimpleName()).commitAllowingStateLoss();
    }

    @Override
    public void onRoomOpen(String roomId) {
        DashBoardFragment fragment = getDashboardFragment();
        if (fragment != null) {
            fragment.updateOnRoomOpen(roomId);
        }
    }

    @Override
    public LinkedHashMap<String, RoomsModel.Messages> getDownloadedChatMediaMap(String mediaId, String messageId) {
        ChatFragment fragment = getChatFragment();
        if (fragment != null) {
            return fragment.getDownloadedChatMediaMap(mediaId, messageId);
        } else {
            return null;
        }
    }

    @Override
    public void onCloseViewChatMediaFragment() {
        /*ChatFragment fragment = getChatFragment();
        if (fragment != null){
            onBackPressed();
        }*/

        Log.d(TAG, "onCloseViewChatMediaFragment requested close ViewChatMediaFragment ");

        try {
            if (getFragmentInBackStack(ViewChatMediaFragment.class.getSimpleName()) != null) {
                boolean popped = getSupportFragmentManager().popBackStackImmediate();
                Log.d(TAG, "onCloseViewChatMediaFragment: popped " + popped);
            } else Log.d(TAG, "onCloseViewChatMediaFragment: fragment is null");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            AppLibrary.goBackFromTrulyFullScreen(w);
        }

        checkFullScreen();

    }

    public boolean popFragmentInBackStack(String tag) {
        if (getFragmentInBackStack(tag) != null) {
            boolean popped = getSupportFragmentManager().popBackStackImmediate();
            Log.d(TAG, "popFragmentInBackStack: popped " + popped);
            return popped;
        } else {
            Log.d(TAG, "popFragmentInBackStack: fragment is null");
            return false;
        }
    }

    /**
     * @param fragmentTag the String supplied while fragment transaction
     * @return
     */
    private Fragment getFragmentInBackStack(String fragmentTag) {
        return getSupportFragmentManager().findFragmentByTag(fragmentTag);

    }

    @Override
    public void onOpenMedia(String messageId, String mediaId, int position) {
        ChatFragment fragment = getChatFragment();
        if (fragment != null) {
            chatFragment.onOpenMedia(messageId, mediaId, position);
        }
    }

    @Override
    public void onLoadViewMomentFragment(String momentId, int momentStatus) {
        loadViewMomentFragment(momentId, momentStatus);

    }

    public void loadViewMomentFragment(String momentId, int momentStatus) {
        ViewMomentsFragment viewMomentsFragment = new ViewMomentsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.MOMENT_ID, momentId);
        bundle.putInt(AppLibrary.MOMENT_STATUS, momentStatus);
        viewMomentsFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewMomentsFragmentContainer, viewMomentsFragment, ViewMomentsFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ViewMomentsFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onLoadViewMomentFragment(String momentId, int momentStatus, String momentType) {
        ViewMomentsFragment viewMomentsFragment = new ViewMomentsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.MOMENT_ID, momentId);
        bundle.putInt(AppLibrary.MOMENT_STATUS, momentStatus);
        bundle.putString(AppLibrary.MOMENT_TYPE, momentType);
        viewMomentsFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewMomentsFragmentContainer, viewMomentsFragment, ViewMomentsFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ViewMomentsFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }


    public void loadViewPublicMomentFragment(String momentId, int momentStatus, String momentType) {

        if (momentType != null && momentType.contains("friend"))
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FRIEND_STREAM_OPEN, AnalyticsEvents.STREAM_ID, momentId);
        else if (momentType != null && momentType.contains("public"))
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.PUBLIC_STREAM_OPEN, AnalyticsEvents.STREAM_ID, momentId);
        else if (momentType != null && momentType.contains("follower"))
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FOLLOWER_STREAM_OPEN, AnalyticsEvents.STREAM_ID, momentId);

        ViewPublicMomentFragment viewMomentsFragment = new ViewPublicMomentFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AppLibrary.MOMENT_ID, momentId);
        bundle.putInt(AppLibrary.MOMENT_STATUS, momentStatus);
        bundle.putString(AppLibrary.MOMENT_TYPE, momentType);
        viewMomentsFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewMomentsFragmentContainer, viewMomentsFragment, ViewPublicMomentFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ViewPublicMomentFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onCloseViewMomentsFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ViewMomentsFragment.class.getSimpleName());

        try {
            if (fragment != null)
                getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            AppLibrary.goBackFromTrulyFullScreen(w);
        }

        checkFullScreen();

        if (getFragmentInBackStack(ViewMomentsFragment.class.getSimpleName()) != null) {

            boolean popped = false;
            try {
                popped = getSupportFragmentManager().popBackStackImmediate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "onCloseViewMomentsFragment: popped " + popped);

        } else
            Log.d(TAG, "onCloseViewMomentsFragment: fragment is null");
    }

    public void onCloseViewPublicMomentsFragment(boolean jumpToCamera) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(ViewPublicMomentFragment.class.getSimpleName());
        try {
            if (fragment != null)
                getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            AppLibrary.goBackFromTrulyFullScreen(w);
        }

        //todo not working without delay....!
        if (!jumpToCamera)
            checkFullScreen();
        else
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleFullScreen(true);
                }
            }, 200);


        if (getFragmentInBackStack(ViewPublicMomentFragment.class.getSimpleName()) != null) {
            boolean popped = false;
            try {
                popped = getSupportFragmentManager().popBackStackImmediate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, "onCloseViewPublicMomentsFragment: popped " + popped);
        } else
            Log.d(TAG, "onCloseViewPublicMomentsFragment: fragment is null");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getVideoEditorFragment() == null) {
                        event.startTracking();
                        return true;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getVideoEditorFragment() == null) {
                        event.startTracking();
                        return true;
                    }
                }
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void openSettingsFragment() {
        SettingsFragment settingsFragment = new SettingsFragment();
        // Add bundle data for profile --
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, settingsFragment, SettingsFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(SettingsFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void openChangeNameFragment() {
        ChangeNameFragment changeNameFragment = new ChangeNameFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, changeNameFragment, ChangeNameFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ChangeNameFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void updateNameInSettingsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SettingsFragment settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.class.getSimpleName());
        settingsFragment.updateNameOnUI();
    }

    public void updateBirthdayOnSettingsFragment(String s) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SettingsFragment settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.class.getSimpleName());
        settingsFragment.updateBirthdayOnUI(s);
    }

    public void openBirthdayFragment() {
        BirthdayFragment birthdayFragment = new BirthdayFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, birthdayFragment, BirthdayFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(BirthdayFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void openPrivacyPolicyFragment() {
        PrivacyPolicy privacyPolicy = new PrivacyPolicy();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, privacyPolicy, PrivacyPolicy.class.getSimpleName());
        fragmentTransaction.addToBackStack(PrivacyPolicy.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();

    }

    public void openReceiveFromFragment() {

        ReceiveFromFragment receiveFromFragment = new ReceiveFromFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, receiveFromFragment, ReceiveFromFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(ReceiveFromFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();

    }

    public void updateReceiveFromOnSettingsFragment(String s) {
        SettingsFragment settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
        settingsFragment.updateReceiveFromOnUI(s);
    }

    public void openBlockedFragment() {
        BlockedFragment blockedFragment = new BlockedFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, blockedFragment, BlockedFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(BlockedFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void openFriendsFragment() {
        FriendsFragment friendsFragment = new FriendsFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, friendsFragment, FriendsFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(FriendsFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getVideoEditorFragment() == null) {
                        getCameraFragment().checkAndStartVideo();
                        return true;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getVideoEditorFragment() == null) {
                        getCameraFragment().checkAndStartVideo();
                        return true;
                    }
                }
                return false;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getCameraFragment().mRecordVideoOngoing && getVideoEditorFragment() == null) {
                        getCameraFragment().onRecordVideoStopped();
                        return true;
                    } else if (getCameraFragment() != null && !getCameraFragment().mRecordVideoOngoing && getVideoEditorFragment() == null) {
                        getCameraFragment().onImageCaptureRequest();
                        return true;
                    }
                }
                return false;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mCurrentFragment == FRAGMENT_CAMERA && !isScrolling) {
                    if (getCameraFragment() != null && getCameraFragment().mRecordVideoOngoing && getVideoEditorFragment() == null) {
                        getCameraFragment().onRecordVideoStopped();
                        return true;
                    } else if (getCameraFragment() != null && !getCameraFragment().mRecordVideoOngoing && getVideoEditorFragment() == null) {
                        getCameraFragment().onImageCaptureRequest();
                        return true;
                    }
                }
                return false;
        }
        return super.onKeyUp(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    void getUserDataFromHandle(String handle) {
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("handle", handle));
        pairs.add(new BasicNameValuePair("userId", FireBaseHelper.getInstance(this).getMyUserId()));
        RequestManager.makePostRequest(this, RequestManager.GET_PROFILE_DATA_REQUEST, RequestManager.GET_PROFILE_DATA_RESPONSE,
                null, pairs, getProfileFromHandleCallBack);
    }

    private RequestManager.OnRequestFinishCallback getProfileFromHandleCallBack = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                Log.d(TAG, "getProfileFromHandleCallBack:==== " + response);
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        if (!object.getJSONObject("value").getString("_id").equals(fireBaseHelper.getMyUserId()))
                            BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(CameraActivity.this, object.getJSONObject("value").getString("_id"),
                                    object.getJSONObject("value").getString("name"),
                                    object.getJSONObject("value").getString("imageUrl"),
                                    object.getJSONObject("value").getString("handle"));
                        AppLibrary.log_d(TAG, "getProfileFromHandleCallBack Success, response -" + object.getString("value"));
                    } else {
                        BaseFragment.showShortToastMessage("Sorry! The user does not exist.");
                        AppLibrary.log_d(TAG, "getProfileFromHandleCallBack Error, response -" + object.getString("value"));
                    }
                } else {
                    // request failed
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


    @SuppressWarnings("deprecation")
    void getStreamDataFromHandle(String handle) {
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("handle", handle));
        RequestManager.makePostRequest(this, RequestManager.GET_STREAM_DATA_REQUEST, RequestManager.GET_STREAM_DATA_RESPONSE,
                null, pairs, getStreamFromHandleCallBack);
    }

    private RequestManager.OnRequestFinishCallback getStreamFromHandleCallBack = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                Log.d(TAG, "getStreamFromHandleCallBack:==== " + response);
                if (success) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        if (!object.getJSONObject("value").getString("_id").equals(fireBaseHelper.getMyUserId())) {
                            String description = null;
                            try {
                                description = object.getJSONObject("value").getString("handle");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            boolean isArticle = false;
                            try {
                                isArticle = object.getJSONObject("value").getBoolean("isArticle");
                            } catch (JSONException e) {
                                Log.e(TAG, " article field not provided by server assuming not an article");
                                e.printStackTrace();
                            }
                            BaseFragment.getBaseFragmentInstance().showStreamPopup(CameraActivity.this, object.getJSONObject("value").getString("_id"),
                                    object.getJSONObject("value").getString("name"),
                                    object.getJSONObject("value").getString("thumbnail"),
                                    object.getJSONObject("value").getString("handle"), description, isArticle);
                        }
                        AppLibrary.log_d(TAG, "getStreamFromHandleCallBack Success, response -" + object.getString("value"));
                    } else {
                        BaseFragment.showShortToastMessage("Sorry! The stream has expired.");
                        AppLibrary.log_d(TAG, "getStreamFromHandleCallBack Error, response -" + object.getString("value"));
                    }
                } else {
                    // request failed
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

    private boolean isDestroyed;

    public boolean isInternetAvailable(boolean showNoInternetToast) {
        try {
            isInternetPresent = isNetworkAvailable(this);
            if (!isInternetPresent) {
                if (showNoInternetToast)
                    Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isInternetPresent;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String IS_COMING_FROM_STICKERS = "isComingFromStickers";

    public void openInstitutionEditFragment(boolean isComingFromStickers) {
        InstitutionEditFragment institutionEditFragment = new InstitutionEditFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        Bundle b = new Bundle();
        b.putBoolean(IS_COMING_FROM_STICKERS, isComingFromStickers);
        institutionEditFragment.setArguments(b);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, institutionEditFragment, InstitutionEditFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(InstitutionEditFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void updateInstitutionOnSettingFragment(String name, String momentId) {
        if (name != null) {
            SettingsFragment settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
            if (settingsFragment != null) {
                settingsFragment.updateInstitutionName(name);
            }
            mFireBaseHelper.getInstituteMomentModel(momentId);
        }
    }

    public void updateFollowersList(boolean follow) {
        final DashBoardFragment fragment = getDashboardFragment();
        if (fragment != null) {
            fragment.updateFollowersList(follow);
        }
    }

    @Override
    public HashMap<String, SocialModel.Friends> getFollowedList() {
        SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
        if (fragment != null)
            return fragment.getFollowedUsersList();
        else return null;
    }

    @Override
    public void updateSocialCount() {
        SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
        if (fragment != null) {
            fragment.updateSocialCount();
        }
    }

    @Override
    public void onFollowedListLoaded(HashMap<String, SocialModel.Friends> followedListHashMap) {
        FriendsFragment friendsFragment = (FriendsFragment) getSupportFragmentManager().findFragmentByTag(FriendsFragment.class.getSimpleName());
        if (friendsFragment != null)
            friendsFragment.onFollowedListLoaded(followedListHashMap);
    }

    @Override
    public void doFacebookLogin(List<String> permissions, FacebookLoginCallback facebookLoginCallback) {
        this.facebookLoginCallback = facebookLoginCallback;
        LoginManager.getInstance().logInWithPublishPermissions(this,
                permissions);
    }

    public interface SharePopupCallbacks {
        void onShareFacebookClicked();

        void onShareIntentClicked();

        void onPopupDismiss();

        void onWatsAppShareClicked();
    }

    public interface FacebookLoginCallback {
        void onSuccessfulLoginCallback(LoginResult loginResult);

        void onErrorCallback();
    }

    /**
     * publicStreamContribution must be non null only when contribution is happening while watching
     */
    private CustomMomentModel publicStreamContribution;

    public CustomMomentModel getPublicStreamContribution() {
        return publicStreamContribution;
    }

    public void onExitContributionToPublicStream() {
        this.publicStreamContribution = null;

        if (getCameraFragment() != null)
            getCameraFragment().togglePublicContributionMode(false);
    }

    public boolean isPublicStreamContributionMode() {
        return this.publicStreamContribution != null;
    }

    public void onContributeToPublicStream(MomentModel momentModel) {
        publicStreamContribution = generateCustomMomentModelFromMomentModel(momentModel);

        if (getCameraFragment() != null)
            getCameraFragment().togglePublicContributionMode(true);

        viewPager.setCurrentItem(1);
        toggleFullScreen(true);
        startCameraPreview(CAMERA_PREVIEW_START_DELAY);
    }

    private CustomMomentModel generateCustomMomentModelFromMomentModel(MomentModel momentModel) {
        return new CustomMomentModel(momentModel.momentId, momentModel.autoModerate, momentModel.imageUrl, momentModel.name);
    }

    public void goBackToStream() {
        this.viewPager.setCurrentItem(0, false);
        this.loadViewPublicMomentFragment(publicStreamContribution.momentId, READY_TO_VIEW_MOMENT, "public");
        this.onExitContributionToPublicStream();
    }
    @Override
    protected void onPermissionsGranted(String[] permissions) {

    }
}