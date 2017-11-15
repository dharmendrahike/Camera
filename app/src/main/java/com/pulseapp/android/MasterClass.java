package com.pulseapp.android;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.crashlytics.android.Crashlytics;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.fragments.CameraFragment;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.Foreground;
import com.pulseapp.android.util.InstallationController;
import com.pulseapp.android.util.SnapshotDetector;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusBuilder;
import io.fabric.sdk.android.Fabric;

/**
 * Created by admin on 1/21/2015.
 */

//@ReportsCrashes(formKey = "", // will not be used
//        formUri = "http://mandrillapp.com/api/1.0/messages/send.json"
//)
public class MasterClass extends Application {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    public static final String TWITTER_KEY = "fvFHkRmtOpZctpkEOQtijYk4g";
    public static final String TWITTER_SECRET = "wjyWVuOQT6RB4QX7vv0FWJ8bLrMXTbYEIZoSzRtL4bzYFz9UJT";
    public static final String parseAppId = "z6BDgdcjdSW76N4tm6eYHmpkhXjG3tsiTmQyC16Z";
    public static final String parseClientKey = "MpYL0XuO8QuofurdgCQ940m6wqtqyrsUIE8UYNOd";
    private static final String TAG = "MasterClass";

    private static EventBus mEventBus;
    private static String deviceId;
    public static TransferUtility transferUtility;
    private static AmazonS3Client s3Client;
    private static AmazonS3Client s3Client_US;
    public static TransferUtility transferUtility_US;
    private static CognitoCachingCredentialsProvider sCredProvider;
    private static HandlerThread uploadWaiter;
    public static android.os.Handler uploadHandler;
    public static CallbackManager callbackManager;
    public static SnapshotDetector snapshotDetector;
    public static Context context;

    private static Thread.UncaughtExceptionHandler mDefaultUEH;
    private Thread.UncaughtExceptionHandler mCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // Custom logic goes here

            // This will make Crashlytics do its job
            Log.d(TAG, "uncaughtException: 1 " + ex);
            if (notifyApplicationCrashed())
                addPendingIntent(MasterClass.getGlobalContext());
            mDefaultUEH.uncaughtException(thread, ex);
            Log.d(TAG, "uncaughtException: 2 " + ex);
        }
    };

    void addPendingIntent(Context activity) {

        Intent intent = new Intent(activity, Splash.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                MasterClass.getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        //Following code will restart your application after 2 seconds
        AlarmManager mgr = (AlarmManager) MasterClass.getInstance().getBaseContext()
                .getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
                pendingIntent);

    }

    /**
     * @return true if the application should be auto-started false otherwise
     */
    private boolean notifyApplicationCrashed() {
        boolean restartApp = false;
        final int MAX_CRASH_ALLOWED_FOR_AUTO_START_WITHIN_REFERENCE_TIME = 3;
        SharedPreferences prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        Set<String> set = prefs.getStringSet(AppLibrary.APP_CRASH_SET, new HashSet<String>());

        long five_minutes = 5 * 60 * 1000;
        Log.d(TAG, "notifyApplicationCrashed: size " + set.size());
        long lastCrashTime = 0;
        int numberOfTimesApplicationCrashedInReferenceTime = 0;
        for (String s : set) {
            long l = Long.parseLong(s);
            Log.d(TAG, "notifyApplicationCrashed: " + l);
            if (l > lastCrashTime)
                lastCrashTime = l;
            if (System.currentTimeMillis() - l < five_minutes) {
                ++numberOfTimesApplicationCrashedInReferenceTime;
            }
        }
        if ((System.currentTimeMillis() - lastCrashTime) > five_minutes) {
            set.clear();
            restartApp = true;
            Log.d(TAG, "notifyApplicationCrashed: clearing set");
        } else if (numberOfTimesApplicationCrashedInReferenceTime <= MAX_CRASH_ALLOWED_FOR_AUTO_START_WITHIN_REFERENCE_TIME) {
            restartApp = true;
            Log.d(TAG, "notifyApplicationCrashed: restarting");
        }

        /**
         * Creating new set as
         * http://stackoverflow.com/questions/14034803/misbehavior-when-trying-to-store-a-string-set-using-sharedpreferences/14034804#14034804
         */
        Set<String> newStrSet = new HashSet<>();
        newStrSet.add(String.valueOf(System.currentTimeMillis()));
        newStrSet.addAll(set);
        prefs.edit().putStringSet(AppLibrary.APP_CRASH_SET, newStrSet).commit();

        return restartApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        instace = this;
        String userId = getSharedPreferences(AppLibrary.APP_SETTINGS, 0).getString(AppLibrary.USER_LOGIN, "");
        if (userId != null && !userId.isEmpty())
            Crashlytics.setUserIdentifier(userId);

        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler);

        AnalyticsManager.getInstance().init(this);

        CameraFragment.mVisible = true;

//        AppsFlyerLib.getInstance().setAppsFlyerKey("Vw8FQXJGNRvnHq2QzLnnME");
//        AppsFlyerLib.sendTracking(getApplicationContext());

        deviceId = InstallationController.id(this);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        AppEventsLogger.activateApp(this);

        if (snapshotDetector == null)
            snapshotDetector = new SnapshotDetector(new Handler());

        Foreground.init(this);

        if (context == null)
            context = getApplicationContext();

        if (uploadWaiter == null) {
            uploadWaiter = new HandlerThread("uploadWaiter");
            uploadWaiter.start();
            uploadHandler = new android.os.Handler(uploadWaiter.getLooper());
        }
//        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
//        Fabric.with(this, new Twitter(authConfig), new Crashlytics(),new CrashlyticsNdk());

        /* start Event bus for signaling */
        if (mEventBus == null)
            buildEventBus();
        
        /* initialize context loaders/Managers */
        RequestManager.initialize(this);

//        /* initialize parse */
//        Parse.enableLocalDatastore(getApplicationContext());
//        initializeParse(this);

        /* initialize service to track app killing */
//        startService(new Intent(getApplicationContext(), KillAppService.class));

        /* initialize Cognito for Uploads */
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),    // get the context for the current activity
                    "us-east-1:5d48bd03-d736-4a6c-9f0b-c4761abe73f5",    /* Identity Pool ID */
                    Regions.US_EAST_1           /* Region for your identity pool*/

            );
            s3Client = new AmazonS3Client(sCredProvider);
            s3Client.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
//          S3ClientOptions s3ClientOptions = S3ClientOptions.builder().setAccelerateModeEnabled(true).build(); //To enable when accelerate mode is enabled
//          s3Client.setS3ClientOptions(s3ClientOptions);
            transferUtility = new TransferUtility(s3Client, getApplicationContext());

            s3Client_US = new AmazonS3Client(sCredProvider);
            transferUtility_US = new TransferUtility(s3Client_US, getApplicationContext());
        }

        if (!AppLibrary.PRODUCTION_MODE) {
            // Enable all thread strict mode policies
            StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder();

            // Detect everything that's potentially suspect
            threadPolicyBuilder.detectAll();

            // Crash the whole process on violation
            threadPolicyBuilder.penaltyDeath();

            // Log detected violations to the system log
            threadPolicyBuilder.penaltyLog();

            // Crash the whole process on any network usage.
            threadPolicyBuilder.penaltyDeathOnNetwork();


            StrictMode.ThreadPolicy threadPolicy = threadPolicyBuilder.build();
            StrictMode.setThreadPolicy(threadPolicy);

            // Enable all VM strict mode policies
            StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder();

            // Detect everything that's potentially suspect
            vmPolicyBuilder.detectAll();

            // Log detected violations to the system log
            vmPolicyBuilder.penaltyLog();

            StrictMode.VmPolicy vmPolicy = vmPolicyBuilder.build();
            StrictMode.setVmPolicy(vmPolicy);
        }
    }

    public static EventBus getEventBus() {
        if (mEventBus == null)
            buildEventBus();
        return mEventBus;
    }

    public static TransferUtility getTransferUtility() {
        return transferUtility;
    }

    public static TransferUtility getTransferUtility_US() {
        return transferUtility_US;
    }

    public static Context getGlobalContext() {
        return context;
    }


//    private void initializeParse(Context context) {
//        Parse.initialize(context, parseAppId, parseClientKey);
//    }
//
//    public static void saveParseObject() {
//        String userId = prefs.getString(AppLibrary.USER_LOGIN, null);
//        String userEmail = prefs.getString(AppLibrary.USER_LOGIN_EMAIL, null);
//        String userName = prefs.getString(AppLibrary.USER_NAME, null);
//        String deviceName = android.os.Build.MODEL;
//        String appVersion = RequestManager.APP_VERSION_CODE;
//
//        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
//
//        if (AppLibrary.checkStringObject(userId) != null)
//            installation.put("userId", userId);
//
//        if (AppLibrary.checkStringObject(userEmail) != null)
//            installation.put("email", userEmail);
//
//        if (AppLibrary.checkStringObject(userName) != null)
//            installation.put("fullName", userName);
//
//        if (AppLibrary.checkStringObject(deviceName) != null)
//            installation.put("deviceModel", deviceName);
//
//        if (AppLibrary.checkStringObject(appVersion) != null)
//            installation.put("app_version", appVersion);
//
//        if (AppLibrary.checkStringObject(deviceId) != null)
//            installation.put("deviceId", deviceId);
//
//        installation.saveInBackground();
//    }
//
//    public static Object getParseObject(String key) {
//        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
//        return installation.get(key);
//    }
//
//    public static void saveParseObject(String key, Object value) {
//        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
//        installation.put(key, value);
//        installation.saveInBackground();
//    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private static void buildEventBus() {
        EventBusBuilder builder = EventBus.builder();
        builder.eventInheritance(true);
        builder.logNoSubscriberMessages(true);
        builder.logSubscriberExceptions(true);
        builder.sendNoSubscriberEvent(true);
        builder.sendSubscriberExceptionEvent(true);
        builder.throwSubscriberException(true);

        mEventBus = builder.build();
    }

//    private static Context mContext;

    private static MasterClass instace;

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static MasterClass getInstance() {
        return instace;
    }

}
