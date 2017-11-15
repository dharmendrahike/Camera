package com.pulseapp.android.apihandling;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.Connectivity;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okio.BufferedSink;
import okio.Okio;

/**
 * Created by admin on 1/13/2015.
 */
public class RequestManager {
    private static final String TAG = "RequestManager";

    public static final int FACEBOOK_LOGIN_REQUEST = 3001;
    public static final int NEARBY_MOMENTS_REQUEST = 3002;
    public static final int LOCATION_TEMPLATES_REQUEST = 3003;
    public static final int MOMENTS_AROUND_YOU_REQUEST = 3004;
    public static final int ADD_USER_HANDLE_REQUEST = 3005;
    public static final int UPDATE_ON_BOARDING_STATUS_REQUEST = 3006;
    public static final int UPDATE_TOKEN_REQUEST = 3007;
    public static final int SEARCH_REQUEST = 3008;
    public static final int GET_CONTRIBUTABLE_MOMENT_REQUEST = 3009;
    public static final int GET_PROFILE_DATA_REQUEST = 3010;
    public static final int GET_STREAM_DATA_REQUEST = 3011;
    public static final int GET_MEDIA_THUMBNAIL_REQUEST = 3012;
    public static final int FACEBOOK_UPDATE_TOKEN_REQUEST = 3013;


    public static final int FACEBOOK_LOGIN_RESPONSE = 5001;
    public static final int NEARBY_MOMENTS_RESPONSE = 5002;
    public static final int LOCATION_TEMPLATE_RESPONSE = 5003;
    public static final int MOMENTS_AROUND_YOU_RESPONSE = 5004;
    public static final int ADD_USER_HANDLE_RESPONSE = 5005;
    public static final int UPDATE_ONBOARDING_STATUS_RESPONSE = 5006;
    public static final int UPDATE_TOKEN_RESPONSE = 5007;
    public static final int SEARCH_RESPONSE = 5008;
    public static final int GET_CONTRIBUTABLE_MOMENT_RESPONSE = 5009;
    public static final int GET_PROFILE_DATA_RESPONSE = 5010;
    public static final int GET_STREAM_DATA_RESPONSE = 5011;
    public static final int GET_MEDIA_THUMBNAIL_RESPONSE = 5012;
    public static final int FACEBOOK_UPDATE_TOKEN_RESPONSE = 5013;

    //  private static Context mContext;
    private static SharedPreferences prefs;

    private static OkHttpClient client;

    /* request url types, should end with _REQUEST */
    public static final int USER_VERIFY_PHONE_REQUEST = 3000;


    /* object types, should end with _DETAILS or _LIST */
    public static final int USER_PHONE_VERIFIED_DETAILS = 5000;

    // Request related constants
    public static final String S3_IMAGE_BUCKET_PATH_PREFIX =
            "https://s3-ap-southeast-1.amazonaws.com/instalively.images/"; //Used for viewing images, so don't change

    public static String APP_VERSION_CODE;

    public static void initialize(Context context) {
        //  mContext = context;
        prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);

        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            APP_VERSION_CODE = Integer.toString(pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        client = new OkHttpClient();
        //Optional Parameters
//      client.setConnectTimeout(10, TimeUnit.SECONDS);
//      client.setWriteTimeout(10, TimeUnit.SECONDS);
//      client.setReadTimeout(30, TimeUnit.SECONDS);
    }

    public static void makeGetRequest(Context context, int url_type, int object_type, List<NameValuePair> pairs, OnRequestFinishCallback mCallback) {
        int count = 0;
        do {
            if (Connectivity.isConnected(context)) {
                if (pairs == null)
                    pairs = new ArrayList<>();
                new GetRequestTask(url_type, object_type, pairs, mCallback, context).executeOnExecutor(THREAD_POOL_EXECUTOR_API);
                return;
            } else {
                count++;
            }
        } while (count <= 4);
    }

    public static void makePostRequest(Context context, int url_type, int object_type, List<NameValuePair> getParams, List<NameValuePair> pairs, OnRequestFinishCallback mCallback) {
        int count = 0;
        do {
            if (Connectivity.isConnected(context)) {
                if (getParams == null)
                    getParams = new ArrayList<>();
                if (pairs == null)
                    pairs = new ArrayList<>();
                new PostRequestTask(url_type, object_type, getParams, pairs, mCallback).executeOnExecutor(THREAD_POOL_EXECUTOR_API);
                return;
            } else {
                count++;
            }
        } while (count <= 4);
    }


    private static Object NewParseResponse(JSONObject object, int object_type) throws JSONException {
        JSONObject jsonObject = object;
        switch (object_type) {
            case FACEBOOK_LOGIN_RESPONSE:
                return jsonObject;
            case NEARBY_MOMENTS_RESPONSE:
                return jsonObject;
            case LOCATION_TEMPLATE_RESPONSE:
                return jsonObject;
            case MOMENTS_AROUND_YOU_RESPONSE:
                return jsonObject;
            case ADD_USER_HANDLE_RESPONSE:
                return jsonObject;
            case UPDATE_ONBOARDING_STATUS_RESPONSE:
                return jsonObject;
            case UPDATE_TOKEN_RESPONSE:
                return jsonObject;
            case SEARCH_RESPONSE:
                return jsonObject;
            case GET_CONTRIBUTABLE_MOMENT_RESPONSE:
                return jsonObject;
            case GET_PROFILE_DATA_RESPONSE:
                return jsonObject;
            case GET_STREAM_DATA_RESPONSE:
                return jsonObject;
            case GET_MEDIA_THUMBNAIL_RESPONSE:
                return jsonObject;
            case FACEBOOK_UPDATE_TOKEN_RESPONSE:
                return jsonObject;
            default:
                return jsonObject;
        }
    }


    private static String getUrlFromType(int url_type) {
        String url = AppLibrary.SERVER_HOST_URL + APP_VERSION_CODE + "/";
        switch (url_type) {

            case FACEBOOK_LOGIN_REQUEST:
                url += "facebookLogin";
                break;
            case NEARBY_MOMENTS_REQUEST:
                url += "getMomentsAroundYou";
                break;
            case LOCATION_TEMPLATES_REQUEST:
                url += "getNearByTemplates";
                break;
            case MOMENTS_AROUND_YOU_REQUEST:
                url += "getMomentsAroundYou";
                break;
            case ADD_USER_HANDLE_REQUEST:
                url += "addUserHandle";
                break;
            case UPDATE_ON_BOARDING_STATUS_REQUEST:
                url += "updateOnboardingStatus";
                break;
            case UPDATE_TOKEN_REQUEST:
                url += "updateNotificationId";
                break;
            case GET_CONTRIBUTABLE_MOMENT_REQUEST:
                url += "getNearByContributableMoments";
                break;
            case SEARCH_REQUEST:
                url+="searchReorder";
                break;
            case GET_PROFILE_DATA_REQUEST:
                url+="getProfileData";
                break;
            case GET_STREAM_DATA_REQUEST:
                url+="getMomentData";
                break;
            case GET_MEDIA_THUMBNAIL_REQUEST:
                url+="getMediaThumbnail";
                break;
            case FACEBOOK_UPDATE_TOKEN_REQUEST:
                url += "updateFacebookToken";
                break;
            default:
                break;
        }
        return url + "?";
    }

    /* hiding constructor */
    private RequestManager() {
    }

    /* helper functions */
    private static JSONObject convertJSONObject(InputStream is) throws JSONException {
        String json = convertInputStreamToString(is);
        AppLibrary.log_d(TAG, "response: " + json);
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject;
    }

    private static String convertInputStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /* scheduler & task */
    private static final BlockingQueue<Runnable> sPoolWorkQueueApi = new LinkedBlockingQueue<Runnable>(1);
    private static ThreadFactory sThreadFactoryApi = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    };
    private static final int mCorePoolSize = 3;
    private static final int mMaxPoolSize = 20;
    private static final Executor THREAD_POOL_EXECUTOR_API = new ThreadPoolExecutor(mCorePoolSize, mMaxPoolSize, 1,
            TimeUnit.SECONDS, sPoolWorkQueueApi, sThreadFactoryApi, new ThreadPoolExecutor.DiscardPolicy());

    private static class PostRequestTask extends AsyncTask<Object, Void, Object> {
        int url_type;
        int object_type;
        List<NameValuePair> pairs;
        List<NameValuePair> postParams;
        OnRequestFinishCallback mCallback;

        public PostRequestTask(int url_type, int object_type, List<NameValuePair> pairs, List<NameValuePair> postParams, OnRequestFinishCallback mCallback) {
            this.url_type = url_type;
            this.object_type = object_type;
            this.pairs = pairs;
            this.postParams = postParams;
            this.mCallback = mCallback;
        }

        @Override
        protected Object doInBackground(Object... params) {
            String url_string = getUrlFromType(url_type);

            if (this.pairs == null) //If a null object is passed, create a new array list
                this.pairs = new ArrayList<>();

//            pairs.add(new BasicNameValuePair("user", prefs.getString("user_login_id", "")));
//            pairs.add(new BasicNameValuePair("src", "android"));
//            pairs.add(new BasicNameValuePair("v", "2"));
//            pairs.add(new BasicNameValuePair("app_v", APP_VERSION_CODE));

            if (!pairs.isEmpty()) {
                for (NameValuePair pair : pairs) {
                    url_string += "&" + pair.getName() + "=" + pair.getValue();
                }
            }

            Response response = null;
            Object parsedResponse = null;
            FormEncodingBuilder form = new FormEncodingBuilder();
            RequestBody formBody;

            if (postParams != null && !postParams.isEmpty()) {
                for (NameValuePair nvp : this.postParams) {
                    if (nvp != null) {
                        String name = nvp.getName();
                        String value = nvp.getValue();
                        form.add(name, value);
                    }
                }
                formBody = form.build();
            } else {
                formBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ""); //Pass empty string as post params
            }

            Request request = new Request.Builder()
                    .url(url_string)
                    .post(formBody)
                    .build();

            try {
                response = client.newCall(request).execute();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if (response == null)
                return null;

            try {
                JSONObject Jobject = new JSONObject(response.body().string());
                parsedResponse = NewParseResponse(Jobject, object_type);
            } catch (Exception e) {
                AppLibrary.error_log(TAG, "Parse error", e);
            }

            return parsedResponse;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (!mCallback.isDestroyed()) {
                mCallback.onBindParams(result != null, result);
            }
        }
    }

    private static class GetRequestTask extends AsyncTask<Object, Void, Object> {
        int url_type;
        int object_type;
        List<NameValuePair> pairs;
        OnRequestFinishCallback mCallback;
        Context context;

        public GetRequestTask(int url_type, int object_type, List<NameValuePair> pairs, OnRequestFinishCallback mCallback, Context context) {
            this.url_type = url_type;
            this.object_type = object_type;
            this.pairs = pairs;
            this.mCallback = mCallback;
            this.context = context;
        }

        @Override
        protected Object doInBackground(Object... params) {
//            String url_string = "";
//
//            if(url_type != 3061)
            String url_string = getUrlFromType(url_type);
//            else {
//                for (NameValuePair pair : pairs) {
//                    url_string = pair.getValue(); //The value contains the url of the file to download
//                }
//                AppLibrary.log_d(TAG, "URL of file in request manager is" + url_string);
//            }

            if (this.pairs == null)  //If a null object is passed, create a new array list
                this.pairs = new ArrayList<>();
//
//            if (url_type != 3046 && url_type != 3050 && url_type != 3054 && url_type != 3061) {   //Don't add misc parameters for remove RTMP subscriber request
//                pairs.add(new BasicNameValuePair("user", prefs.getString("user_login_id", "")));
//                pairs.add(new BasicNameValuePair("src", "android"));
//                pairs.add(new BasicNameValuePair("v", "2"));
//                pairs.add(new BasicNameValuePair("app_v", APP_VERSION_CODE));
//            }

            if (!pairs.isEmpty()) {
                for (NameValuePair pair : pairs) {
                    url_string += "&" + pair.getName() + "=" + pair.getValue();
                }
            }

            Response response = null;
            Object parsedResponse = null;

            Request request = new Request.Builder()
                    .url(url_string)
                    .build();

            try {
                response = client.newCall(request).execute();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if (response == null)
                return null;

            if (url_type == 3046) //For RTMP remove subscriber request, don't enter NewParseResponse function
                return true;

            if (url_type == 3061) {
                File downloadedFile = new File(this.context.getCacheDir(), "URL");

                try {
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                    sink.writeAll(response.body().source());
                    sink.close();
                    return downloadedFile;
                } catch (Exception e) {
                    return null;
                }
            }

            try {
                JSONObject Jobject = new JSONObject(response.body().string());
                parsedResponse = NewParseResponse(Jobject, object_type);
            } catch (Exception e) {
                AppLibrary.error_log(TAG, "Parse error", e);
            }

            return parsedResponse;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (!mCallback.isDestroyed()) {
                mCallback.onBindParams(result != null, result);
            }
        }
    }

    public interface OnRequestFinishCallback {
        public void onBindParams(boolean success, Object response);

        public boolean isDestroyed();
    }
}
