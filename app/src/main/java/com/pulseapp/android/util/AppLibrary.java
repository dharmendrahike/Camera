package com.pulseapp.android.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.util.StringUtils;
import com.facebook.login.LoginManager;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.SliderMessageModel;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Created by admin on 1/12/2015.
 */
public class AppLibrary implements FireBaseKEYIDS {


    public static final boolean PRODUCTION_MODE = BuildConfig.IS_PRODUCTION;
    public static final boolean IGNORE_UNRECOGNIZED_FIELD = true;
    /**
     * Turning on the boolean IGNORE_UNRECOGNIZED_FIELD :--
     * Unrecognised fields (not found/not found public in a class) should be gracefully ignored
     * in the concerned classes and hence no exception should be thrown
     */
    public static final boolean MESSAGING_DISABLED = true;
    public static final String MOMENT_STATUS = "moment_status";
    public static final String MOMENT_TYPE = "moment_type";
    public static final String ROOM_ID = "room_id";
    public static final String COMMONPREF = "common_preferences";
    public static final String INSTITUTE_HIDDEN_BY_USER = "institute_hidden_by_user";
    public static final String DONT_SHOW_DIALOG_AGAIN = "dont_show_dialog_again";
    //    public static final String CAM_TUT_SHOWN = "cam_tut_shown_flag";
    public static final String FILTER_TUT_SHOWN = "filter_tut_shown_flag";
    public static final String STICKER_TUT_SHOWN = "sticker_tut_shown";
    public static final String LOCATION_TUT_SHOWN = "location_tut_shown_flag";
    public static String imageThumbnail = "";

    public static final String FIRST_TIME_TUTORIAL = "first_time_tutorial_count";

    public static boolean IS_CAMERA_STICKER_SHOWN_IN_THIS_SESSION = false;
    public static final String CAMERA_TUTORIAL = "new_camera_tutorial_count";
    public static final int CAMERA_TUTORIAL_COUNT = 10;

    public static boolean IS_EMOJI_DISCOVER_TUTORIAL_SHOWN_IN_THIS_SESSION = false;
    public static final String EMOJI_DISCOVER_TUTORIAL = "new_emoji_tutorial_count";
    public static final int EMOJI_DISCOVER_TUTORIAL_COUNT = 5;


    /* Preferences related constants */
    public static final String APP_SETTINGS = "app_settings";
    public static final String USER_LOGIN = "user_login_id";
    public static final String BIRTHDAY_PERMISSION = "birthday_permission";
    public static final String ANALYTICS_IDENTIFIED = "analytics_identified";
    public static final String USER_ONBOARDING_STATUS = "user_onBoarded";
    public static final String USER_NAME = "user_name";
    public static final String USER_MOMENT = "user_moment";
    public static final String USER_PHONE_VERIFIED = "user_phone_verified";
    public static final String USER_GENDER = "user_gender";
    public static final String USER_LOGIN_EMAIL = "user_email";
    public static final String USER_PROFILE_PIC_URL = "user_profile_pic_url";
    public static final String ENABLE_LIVE_STREAM_STATUS = "enabled_live_stream";
    public static final String EVENT_NAME = "event_name";
    public static final String EVENT_DESCRIPTION = "event_description";
    public static final String EVENT_LOCATION = "event_location";
    public static final String MOBILE_CHANNEL = "mobile_channel";
    public static final String TWITTER_SHARE_STATE = "twitter_share_state";
    public static final String FACEBOOK_LOGIN_STATE = "facebook_login_state";
    public static final String FACEBOOK_SHARE_STATE = "facebook_share_state";
    public static final String FCM_TOKEN = "fcm_token";
    public static final String NEARBY_STATE = "nearby_state";
    public static final String EVENT_SID = "event_sid";
    public static final String EVENT_UID = "event_userid";
    public static final String EVENT_THUMBNAIL = "event_thumbnail";
    public static final String BroadCastSettings = "broadcast_setting_object";
    public static final String ENABLE_LIVE_STREAM_PREVIOUS_URL = "enable_live_previous_url";
    public static final String CONTACT_UPDATED = "user_contact_updated";
    public static final String CURRENT_STREAM_ID = "current_stream_id";
    public static final String AUTO_FOLLOW = "auto_follow";
    public static final String ActivityStopped = "activity_stopped";
    public static final String GOOGLE_ID = "google_id";
    public static final String GOOGLE_PAGE = "google_page";
    public static final String SENT_TOKEN_TO_SERVER = "sent_updated_token";
    public static final String FACEBOOK_ACCESS_TOKEN = "facebook_access_token";
    public static final String EXPECTED_USER_PHONE_NUMBER = "expected_user_number";
    public static final String GOOGLE_LIVE_STREAM_URL = "google_live_stream_url";
    public static final String FIRST_TIME_USER = "first_time_user";
    public static final String BROADCAST_COMPLETE = "broadcast_complete";
    public static final String FACEBOOK_ID = "facebook_id";
    public static final String FACEBOOK_BIO_INFO = "facebook_bio_info";
    public static final String FACEBOOK_COVER_PICTURE = "facebook_cover_picture";
    public static final String FETCH_FACEBOOK_FRIENDS_URL = "https://graph.facebook.com/me/taggable_friends?limit=5000";
    public static final String FACEBOOK_FRIENDS_PERMISSION = "facebook_friends_permission";
    public static final String FACEBOOK_PUBLISH_PERMISSION = "facebook_publish_permission";
    public static final String FACEBOOK_PUBLISH_PAGES_PERMISSION = "facebook_publish_pages_permission";
    public static final String SELECTED_FACEBOOK_PAGE = "selected_facebook_page";
    public static final String FACEBOOK_FRIENDS_LAST_FETCHED = "facebook_friends_last_fetched";
    public static final String SELECTED_MEDIA_PATH = "selected_media_path";
    public static final String CAPTURED_BITMAP = "captured_bitmap";
    public static final String MEDIA_TEXT = "media_text";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int GROUP_ACTION_CREATE = 1;
    public static final int GROUP_ACTION_MANAGE = 2;
    public static final String GROUP_ACTION = "group_action";
    public static final String MOMENT_ID = "momentId";
    public static final String MEDIA_ID = "mediaId";
    public static final String VERSION = "version";
    public static final String SOURCE = "source";
    public static final String ANDROID = "android";
    public static final String UPDATED_PRIVACY_TO_PUBLIC = "updated_privacy_to_public";
    public static final String CURRENT_PAGE = "current_page";

    public static final String WHATSAPP_PACKAGE_NAME = "com.whatsapp";

    public static final String XIAOMI_AUTO_START_PERMISSION = "tip_xiaomiAutoStart";
    public static final String APP_CRASH_SET = "app_crash_Set";


    /* In app animation constants */
    public static final int FILTERS_SLIDER_ANIMATION_DURATION = 200;

    /* logger */
    private static final boolean sLogIt = true;
    private static int mWidthPixels, mHeightPixels;
    private static File outputDirectory;
    public static String INSTITUTION_NEEDED = "isInstitutionNeeded";

    public static void log_i(String TAG, String message) {
        if (sLogIt) Log.i(TAG, message);
    }

    public static void log_i(String TAG, long value) {
        if (sLogIt) Log.i(TAG, value + "");
    }

    public static void log_d(String TAG, String message) {
        if (sLogIt) Log.d(TAG, message);
    }

    public static void log_e(String TAG, String message) {
        if (sLogIt) Log.e(TAG, message);
    }

    // Google Project Number
    public static final String GOOGLE_PROJECT_ID = "925070894204";
    public static final String REG_ID = "regId";
    public static final String APP_VERSION = "appVersion";

    //Chat setup for notification
    public static final String MESSAGE_KEY = "message";
    public static final String MESSAGE_SENDER = "username";
    public static final String MESSAGE_SENDER_IMAGE = "profileImage";
    public static final String NOTIFICATION_TYPE = "nType";
    public static final String WATCHING_COUNT = "count";
    public static final String PRESENTER = "presenter";

    static final SimpleDateFormat outFormatD = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    static final SimpleDateFormat outFormatT = new SimpleDateFormat("h:mma", Locale.US);
    static SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    public static void error_log(String TAG, String message, Exception e) {
        if (e != null) {
            String msg = message + e.getClass().toString();
            msg += Log.getStackTraceString(e);
            Log.e(TAG, msg);
        } else {
            Log.e(TAG, message + " ");
        }
    }

    public static void error_log(String TAG, String message, Error e) {
        if (e != null) {
            String msg = message + e.getClass().toString();
            msg += Log.getStackTraceString(e);
            Log.e(TAG, msg);
        } else {
            Log.e(TAG, message + " ");
        }
    }

    /* HttpConnection */
    public static final boolean USE_COMPRESSION = false;
    //    public static final String SERVER_HOST_URL = "http://test.instalively.co/android/";
//    public static final String SERVER_HOST_URL = "https://www.mypulse.tv/android/";
    public static final String SERVER_HOST_URL = PRODUCTION_MODE ? "https://www.mypulse.tv/android/" : "http://test.instalively.co/android/";

    public static final String SERVER_HOST_SHORT_URL = "http://instalive.me/";
    public static final String IMAGES_HOST_URL = "http://s3-ap-southeast-1.amazonaws.com/instalively.images/";
    public static final String ENABLE_LIVE_STREAMING_URL = "http://www.youtube.com/live_streaming_signup";
    public static final String MediaHostUrl = "https://s3.ap-south-1.amazonaws.com/pulse.resources.india/";
    public static final String MediaHostBucket = "pulse.resources.india";

    public static InputStream getStream(HttpResponse response) throws IllegalStateException, IOException {
        InputStream iStream = response.getEntity().getContent();
        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            iStream = new GZIPInputStream(iStream);
        }
        return iStream;
    }

    public static boolean isNetworkAvailable(Context c) {
        if (c == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static String TAG = "AppLibrary";

    public static void makeViewVisible(final View v) {
        AlphaAnimation a = new AlphaAnimation(0, 1f);
        a.setDuration(100);
        a.setFillAfter(true);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (v.getVisibility() == View.GONE) {
            Log.d(TAG, "View is invisible, showing it");
            v.startAnimation(a);
        }
    }

    public static void makeViewInvisible(final View v) {
        AlphaAnimation a = new AlphaAnimation(1f, 0);
        a.setDuration(100);
        a.setFillAfter(true);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (v.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "View is visible, hiding it");
            v.startAnimation(a);
        }
    }

    public static void showNoNetworkDialog(final Context mContext) {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);
        builder.setTitle("No network Available");
        builder.setPositiveButton()
        builder.setMessage("");
        builder.create().show();*/
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);

        // Setting Dialog Title
        alertDialog.setTitle("Bad Network");

        // Setting Dialog Message
        alertDialog.setMessage("Oops! Your internet is too slow. Please try with a different network source.");

        // On pressing Settings button
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    public static void showAgreementDialog(Context mContext) {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);
        builder.setTitle("No network Available");
        builder.setPositiveButton()
        builder.setMessage("");
        builder.create().show();*/
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);

        // Setting Dialog Title
        alertDialog.setTitle("Oops");

        // Setting Dialog Message
        alertDialog.setMessage("You have to agree to continue");
        alertDialog.setCancelable(false);
        // On pressing Settings button
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    public static void showServerDownDialog(Context mContext) {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);
        builder.setTitle("No network Available");
        builder.setPositiveButton()
        builder.setMessage("");
        builder.create().show();*/
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);

        // Setting Dialog Title
        alertDialog.setTitle("Oops");

        // Setting Dialog Message
        alertDialog.setMessage("Youtube server is down. Please try again later!");
        alertDialog.setCancelable(false);
        // On pressing Settings button
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    /* services */
    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /* fonts */
    private static final Hashtable<String, Typeface> typefaces = new Hashtable<String, Typeface>();
    public static final String Regular = "fonts/Roboto-Light.ttf";
    public static final String Bold = "fonts/Roboto-Regular.ttf";
    public static final String Icon = "fonts/icons.ttf";

    public static Typeface getTypeface(Context c, String name) {
        synchronized (typefaces) {
            if (!typefaces.containsKey(name)) {
                /*if (name.equals(Regular)) {
                    Typeface t =  Typeface.SANS_SERIF;
                    typefaces.put(name, t);
                    return t;

                } else if (name.equals(Bold)) {
                    Typeface t =  Typeface.SERIF;
                    typefaces.put(name, t);
                    return t;
                }*/

                try {
                    InputStream inputStream = c.getAssets().open(name);
                    File file = createFileFromInputStream(inputStream);
                    if (file == null) {
                        return Typeface.DEFAULT;
                    }
                    Typeface t = Typeface.createFromFile(file);
                    typefaces.put(name, t);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Typeface.DEFAULT;
                }
            }
            return typefaces.get(name);
        }
    }

    public static boolean fileExists(String value) {
        return new File(value).exists();
    }

    private static File createFileFromInputStream(InputStream inputStream) {

        try {
            File f = File.createTempFile("font", null);
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            return f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void hideKeyboard(Context context, EditText et) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    }

    public static void showKeyboard(Context context, EditText et) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, 0);
    }

    public static void toggleKeyboardVisibility(boolean isVisible, Context context, EditText et) {
        if (isVisible)
            hideKeyboard(context, et);
        else
            showKeyboard(context, et);
    }

    public static String TitleCase(String str) {
        if (str != null & !str.isEmpty()) {
            Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
            String array[] = str.toLowerCase().trim().split("\\s+");
            String Title = "";

            for (int i = 0; i < array.length; i++) {
                Matcher m = p.matcher(array[i].substring(0, 1));
                if (!m.find()) {
                    array[i] = array[i].substring(0, 1).toUpperCase() + array[i].substring(1);
                }
            }

            Title = StringUtils.join(" ", array);

            return Title;
        } else
            return ""; //Return empty string to prevent crashes
    }

    public static String capsFirstLetter(String str) {
        if (str != null & !str.isEmpty()) {
            Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(str.substring(0, 1));
            String upperString = str;

            if (!m.find())
                upperString = str.substring(0, 1).toUpperCase() + str.substring(1);

            return upperString;
        } else
            return ""; //Return empty string to prevent crashes
    }


    public static String getFirstName(String str) {
        if (str != null && !str.isEmpty()) {
            String trimmedName = str.trim();
            String[] items = trimmedName.split(" ");
            return items[0];
        } else
            return ""; //Return empty string to prevent crashes
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static boolean isPackageInstalled(String targetPackage, Context context) {
        List<ApplicationInfo> packages;
        packages = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }

    public static String getPackageBasedOnPriority(Context context) {
        if (isPackageInstalled("com.whatsapp", context)) {
            return "com.whatsapp";
        } else if (isPackageInstalled("com.facebook.orca", context)) {
            return "com.facebook.orca";
        }/*else if(isPackageInstalled("com.bsb.hike", context)){
            return "com.bsb.hike";
        }else if(isPackageInstalled("jp.naver.line.android", context)){
            return "jp.naver.line.android";
        }else if(isPackageInstalled("com.tencent.mm", context)){
            return "com.tencent.mm";
        }*/ else if (isPackageInstalled("com.google.android.talk", context)) {
            return "com.google.android.talk";
        } else if (isPackageInstalled("com.android.mms", context)) {
            return "com.android.mms";
        }
        return null;
    }

//    public static int getPackageImg(String packageName) {
//        if (packageName.equalsIgnoreCase("com.whatsapp")) {
//            return R.drawable.icon_whatsapp;
//        } else if (packageName.equalsIgnoreCase("com.facebook.orca")) {
//            return R.drawable.icon_messenger;
//        } else if (packageName.equalsIgnoreCase("com.bsb.hike")) {
//            return R.drawable.icon_hike;
//        } else if (packageName.equalsIgnoreCase("jp.naver.line.android")) {
//            return R.drawable.icon_line;
//        } else if (packageName.equalsIgnoreCase("com.tencent.mm")) {
//            return R.drawable.icon_wechat;
//        } else if (packageName.equalsIgnoreCase("com.google.android.talk")) {
//            return R.drawable.icon_hangout;
//        } else if (packageName.equalsIgnoreCase("com.android.mms")) {
//            return R.drawable.icon_sms;
//        }
//        return 0;
//    }

    @SuppressLint("NewApi")
    public static int getSoftbuttonsbarHeight(Activity activity) {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

    public static int getDeviceParams(Activity activity, String params) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (params.contains("width"))
            return metrics.widthPixels;
        else
            return metrics.heightPixels;
    }

    private static int sDeviceWidth;

    public static int getDeviceWidth(Activity activity) {
        if (sDeviceWidth != 0) return sDeviceWidth;
        sDeviceWidth = getDeviceParams(activity, "width");
        return sDeviceWidth;
    }

    public static float getDeviceAspectRatio(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels / metrics.heightPixels;
    }

    public static int[] getDeviceParams(Activity activity) {
        if ((mWidthPixels == 0) || (mHeightPixels == 0)) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mWidthPixels = metrics.widthPixels;
            mHeightPixels = metrics.heightPixels;
        }
        int[] params = new int[2];
        params[0] = mWidthPixels;
        params[1] = mHeightPixels;
        return params;
    }

    public static int[] getFullScreenDeviceParams(Activity activity) {
        if ((mWidthPixels == 0) || (mHeightPixels == 0)) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            mWidthPixels = metrics.widthPixels;
            mHeightPixels = metrics.heightPixels;
        }
        int[] params = new int[2];
        params[0] = mWidthPixels;
        params[1] = mHeightPixels;
        return params;
    }

    public static void goTrulyFullscreen(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static void goBackFromTrulyFullScreen(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

//    public static String getAddressUrl(Context context, double latitude, double longitude) {
//        String responseText = null;
//        // String latitude = "38.89";
//        // String longitude  = "-77.03";
//        String googleurl = "https://maps.googleapis.com/maps/api/geocode/json?";
////        latlng=40.714224,-73.961452&key=API_KEY
//        log_i("Address", "Latitude is: " + Double.toString(latitude) + "Longitude is:" + Double.toString(longitude));
//        StringBuilder sbuilder = new StringBuilder();
//        sbuilder.append(googleurl);
//
//        sbuilder.append("latlng=" + Double.toString(latitude) + "," + Double.toString(longitude));
//        sbuilder.append("&key=925070894204-mqhllqmpjud3ag6pp61ttg7cgseiefgr.apps.googleusercontent.com&sensor=true");
//        String url = sbuilder.toString();
//
//        log_i("Address", "url is: " + url);
//        try {
//            DefaultHttpClient httpclient = new DefaultHttpClient();
//            HttpPost httppost = new HttpPost(url);
//            HttpResponse httpresponse = httpclient.execute(httppost);
//            HttpEntity httpentity = httpresponse.getEntity();
//            InputStream is = httpentity.getContent();
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(is, "iso-8859-1"), 8);
//            StringBuilder sb = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line + "\n");
//            }
//            is.close();
//            responseText = sb.toString();
//            log_i("Address", responseText);
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return responseText;
//    }

//    public static JSONObject getLocationInfo(String address) {
//        StringBuilder stringBuilder = new StringBuilder();
//        try {
//
//            address = address.replaceAll(" ", "%20");
//
//            HttpPost httppost = new HttpPost("http://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false");
//            HttpClient client = new DefaultHttpClient();
//            HttpResponse response;
//            stringBuilder = new StringBuilder();
//
//
//            response = client.execute(httppost);
//            HttpEntity entity = response.getEntity();
//            InputStream stream = entity.getContent();
//            int b;
//            while ((b = stream.read()) != -1) {
//                stringBuilder.append((char) b);
//            }
//        } catch (ClientProtocolException e) {
//        } catch (IOException e) {
//        }
//
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject = new JSONObject(stringBuilder.toString());
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        return jsonObject;
//    }

    private static List<Address> getAddrByWeb(JSONObject jsonObject) {
        List<Address> res = new ArrayList<Address>();
        try {
            JSONArray array = (JSONArray) jsonObject.get("results");
            for (int i = 0; i < array.length(); i++) {
                Double lon = new Double(0);
                Double lat = new Double(0);
                String name = "";
                try {
                    lon = array.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                    lat = array.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                    name = array.getJSONObject(i).getString("formatted_address");
                    Address addr = new Address(Locale.getDefault());
                    addr.setLatitude(lat);
                    addr.setLongitude(lon);
                    addr.setAddressLine(0, name != null ? name : "");
                    res.add(addr);
                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }

    /**
     * @param date_time epoch in milliseconds
     * @return the formatted string (diff btn now and date_time)
     */
    // Post date/time w.r.t. current time
    public static String timeAccCurrentTime(long date_time) {
        if (date_time == 0)
            return "";
        date_time /= 1000;//converting into seconds
        Date d = new Date();
        long current_time = d.getTime() / 1000;
//       log_i("Current Date time value", "" + current_time);

        long diff_time_min = (current_time - date_time) / 60;
        String str_final_diff;

        if (diff_time_min >= 0 && diff_time_min < 1)
            str_final_diff = "Now";
        else if (diff_time_min < 60) {
            if (diff_time_min == 1)
                str_final_diff = diff_time_min + " min";
            else
                str_final_diff = diff_time_min + " mins";
        } else if (diff_time_min < 1440) {
            long this_diff = diff_time_min / 60;
            if (this_diff == 1)
                str_final_diff = this_diff + " hr";
            else
                str_final_diff = this_diff + " hrs";
        } else if (diff_time_min >= 1440) {
            long this_diff = diff_time_min / 60 / 24;
            if (this_diff == 1)
                str_final_diff = this_diff + " d";
            else
                str_final_diff = this_diff + " d";
        } else if (diff_time_min >= 43200) {
            // should never enter this loop
            long this_diff = diff_time_min / 60 / 24 / 30;
            str_final_diff = this_diff + " m";
        } else {
            // should never enter this loop
            str_final_diff = onlyDateParser(date_time);
        }

        if (str_final_diff.contains("-"))
            str_final_diff = "Just Now";

        return str_final_diff;
    }

    public static long calculateDifferenceInDate(long stored_time) {
        Date date = new Date(System.currentTimeMillis());
        long currentTime = date.getTime();
        return (stored_time - currentTime) / (24 * 60 * 60 * 1000);
    }


    // Display date according to our date display format
    static String onlyTimeParser(long date) {
        String str_final_diff = "";
        try {
            str_final_diff = outFormatT.format(new Date(date * 1000));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return str_final_diff;
    }

    // Display date according to our date display format
    static String onlyDateParser(long date) {
        String str_final_diff = "";
        try {
            str_final_diff = outFormatD.format(new Date(date * 1000));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return str_final_diff;
    }

    public static long convertDateToLong(String str_date) {
        if (checkStringObject(str_date) == null)
            return 0;
        Date date = null;
        originalFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        AppLibrary.log_i("Date", "String date is " + str_date);
        try {
            date = originalFormat.parse(str_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long date_time = date.getTime() / 1000;
        return date_time;
    }

    public static String getDeviceId(Context mContext) {
        return InstallationController.id(mContext);
    }

    public static String getRealPathFromURI(Uri contentUri, Context context) {
        String[] proj = {MediaStore.Images.Media.DATA};
//        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
//        Cursor cursor = loader.loadInBackground();
        if (cursor == null) return null;

        String result = null;
        try {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        } catch (Exception e) {
            e.printStackTrace();
        }

        cursor.close();

        return result;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model == null || manufacturer == null) return null;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    public static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static String checkStringObject(String value) {
        if (value == null || value.equals("null") || value.equals(""))
            return null;
        else
            return value.trim();
    }

    // given a integer input returns the one decimal rounded output
    public static String getStringCount(int value) {
        String stringValue = "";
        DecimalFormat df = new DecimalFormat("0.0");
        if (value <= 1000) {
            stringValue = Integer.toString(value);
        } else if (value > 1000 && value < 100000) {
            stringValue = df.format((value / 1000.0)) + "k";
        } else if (value >= 100000) {
            stringValue = df.format((value / 100000.0)) + "l";
        }
        return stringValue;
    }

    public static void copyLink(Context context, String link) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", link);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show();
    }

    public static void doFacebookLogOut() {
        LoginManager.getInstance().logOut();
    }

    public static int convertDpToPixels(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getMediaType(String path) {
        if (path != null && !path.isEmpty() && (path.toLowerCase().endsWith(".webp") || path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg") || path.toLowerCase().endsWith(".png"))) {
            return 1;
        } else if (path != null && !path.isEmpty() && path.toLowerCase().endsWith(".mp4")) {
            return 2;
        } else {
            return 0;
        }
    }

    public static String getFilesDirectory(Context mContext) {
        return mContext.getFilesDir().getAbsolutePath() + File.separator + "downloadedMedia" + File.separator;
    }

    public static String getCreatedMediaDirectory(Context mContext) {
        return mContext.getFilesDir().getAbsolutePath() + File.separator + "downloadedMedia";
    }

    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static Bitmap getStickerBitmapFromAssets(Context context, String fileName) {
        AssetManager assetManager = context.getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(EMOTICON_ASSET_DIR_NAME + File.separator + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
    }

    public static Intent processIntent(Intent intent, Context context){
        if (!convertIntentActionsToExtras(intent, context) && intent.getExtras() != null && intent.getExtras().getString("type") != null) {
            SharedPreferences prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
            String userId = prefs.getString(AppLibrary.USER_LOGIN, "");
            int type = Integer.valueOf(intent.getExtras().getString("type"));
            String groupName = intent.getExtras().getString("groupName");
            SliderMessageModel model;
            final String title, body, senderName, senderId, from = null;
            title = intent.getExtras().getString("title");
            body = intent.getExtras().getString("body");
            senderName = intent.getExtras().getString("senderName");
            String image = intent.getExtras().getString("image"); //imageUrl
            senderId = intent.getExtras().getString("senderId");

            String url = intent.getExtras().getString("url");
            String roomId = intent.getExtras().getString("roomId");

            switch (type) {
                case 1: //Friend request received
                    FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS, new String[]{userId, REQUEST_RECEIVED}).keepSynced(true);
                    intent.putExtra("action", "friendRequestReceived");
                    break;

                case 2: //Friends request accepted
                    if (AppLibrary.checkStringObject(groupName) == null)
                        model = new SliderMessageModel(senderName, image, roomId, senderId, FRIEND_ROOM, 000, 0, null);
                    else
                        model = new SliderMessageModel(groupName, image, roomId, senderId, GROUP_ROOM, 000, 0, null);
                    intent.putExtra("action", "friendRequestAccepted");
                    intent.putExtra("model", model);
                    break;

                case 3: //Group request received
                    FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS, new String[]{userId, PENDING_GROUP_REQUEST}).keepSynced(true);
                    intent.putExtra("action", "groupRequestReceived");
                    break;

                case 4: //New message
                    //TODO:
                    break;

                case 6: //Notifications sent from the admin dashboard
                    if (url != null && !url.isEmpty()) {
                        if (url.contains("/add/") || url.contains("/profile/")) {
                            intent.putExtra("action", "openProfilePopup");
                            intent.putExtra("url", url);
                        } else if (url.contains("/stream/")) {
                            intent.putExtra("action", "openStream");
                            intent.putExtra("url", url);
                        } else if (url.equalsIgnoreCase("openCamera")) {
                            // Do nothing - not putting an extra action will open camera like default
                        }
                    } else {
                        intent.putExtra("action", "genericNotification"); //Will open dashboard and do nothing
                    }
                    break;
            }
        }
        return intent;
    }

    private static boolean convertIntentActionsToExtras(Intent intent, Context context) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) { //For URLs for adding friend & watching stream
            String Uri = intent.getData().toString().toLowerCase();
            Log.d(TAG, "Intent uri is " + Uri);

            if (Uri != null && !Uri.isEmpty() && (Uri.contains("/add/") || Uri.contains("/profile/"))) {
                intent.putExtra("action", "openProfilePopup");
                intent.putExtra("url", Uri);
            } else if (Uri != null && !Uri.isEmpty() && Uri.contains("/stream/")) {
                intent.putExtra("action", "openStream");
                intent.putExtra("url", Uri);
            }
            return true;

        } else if (intent != null && Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType().startsWith("image/")) {

            if (intent.getExtras() != null && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Uri uri = intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
                String filepath = null;

                if (uri.getScheme().contains("content")) {
                    filepath = AppLibrary.getRealPathFromURI(uri, context);
                } else if (uri.getScheme().contains("file")) {
                    filepath = uri.getPath();
                }

                Log.d(TAG, "Filepath in action send intent is " + filepath);

                if (filepath != null && !filepath.isEmpty()) {
                    intent.putExtra("action", "openImage");
                    intent.putExtra("uri", filepath);
                }
            }
            return true;
        }
        return false;
    }

    public static String setupOutputDirectoryForRecordedFile() {
        //ToDo: Make this cleaner!
        //Search for external SD card and check if it is writable
        //If no writable external SD card is found get Movies directory from getExternalStoragePublicDirectory
        if (outputDirectory == null || outputDirectory.getPath().isEmpty()) {
            String foundDirPath = System.getenv("SECONDARY_STORAGE");
            if (foundDirPath != null && !foundDirPath.isEmpty()) {
                outputDirectory = new File(foundDirPath);
                if (outputDirectory != null && outputDirectory.exists()) {
                    foundDirPath += "/Movies/Pulse";
                    outputDirectory = new File(foundDirPath);
                    outputDirectory.mkdirs();
                    if (!outputDirectory.exists() || !outputDirectory.canWrite())
                        foundDirPath = "";
                } else
                    foundDirPath = "";
            }
            if (foundDirPath == null || foundDirPath.isEmpty()) {
                outputDirectory = new File("/mnt/");
                if (outputDirectory != null && outputDirectory.exists() && outputDirectory.isDirectory()) {
                    String[] dirList = outputDirectory.list();
                    for (int i = 0; i < dirList.length; i++) {
                        String dir = dirList[i].toLowerCase();
                        if (((dir.contains("sd") && dir.contains("card")) ||
                                (dir.contains("sd") && dir.contains("ext")) ||
                                (dir.contains("ext") && dir.contains("card"))) &&
                                dir != "sdcard0") { //sdcard0 is phone storage, we can get that path in later conditions
                            File checkDir = new File(outputDirectory.getPath() + "/" + dirList[i]);
                            if (checkDir != null && checkDir.canWrite()) { //This check not fool proof
                                foundDirPath = outputDirectory.getPath() + "/" + dirList[i] + "/Movies/Pulse";
                                outputDirectory = new File(foundDirPath);
                                outputDirectory.mkdirs();
                                if (!outputDirectory.exists() || !outputDirectory.canWrite())
                                    foundDirPath = "";
                                else
                                    break;
                            }
                        }
                    }
                }
            }
            if (foundDirPath == null || foundDirPath.isEmpty())
                foundDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath() + "/Pulse";
            outputDirectory = new File(foundDirPath);
            outputDirectory.mkdirs();
            if (!outputDirectory.exists() || !outputDirectory.canWrite())
                foundDirPath = "";
            if (foundDirPath == null || foundDirPath.isEmpty()) {
                foundDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/Pulse";
                outputDirectory = new File(foundDirPath);
                outputDirectory.mkdirs();
            }
        }

        return outputDirectory.getPath();
    }

    public static int getNormalisedFirstChar(String displayName) {
        final int HASH = 35;
        if (displayName.length() == 0) {
            return HASH;
        }
        int letter = displayName.charAt(0);
        if (letter >= 97 && letter <= 123) {//if its abc..z
            return letter - 32;
        } else if (!(letter >= 95 && letter <= 122) && !(letter >= 65 && letter <= 90)) {//if its neither ABC..Z nor abc..z
            return HASH;// # for special characters and 123...
        } else return letter;

    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
/*
    */

    /**
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     *//*
    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }*/

}
