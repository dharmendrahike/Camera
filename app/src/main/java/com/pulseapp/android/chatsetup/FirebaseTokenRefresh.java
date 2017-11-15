package com.pulseapp.android.chatsetup;

/**
 * Created by Karthik on 5/19/16.
 */


import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.util.AppLibrary;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class FirebaseTokenRefresh extends FirebaseInstanceIdService implements FireBaseKEYIDS{

    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = null;

        String authorizedEntity = "820115401788"; // Sender id from Firebase Developer Console
        String scope = "FCM";

        try {
            refreshedToken = FirebaseInstanceId.getInstance().getToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        SharedPreferences preferences = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        if (AppLibrary.checkStringObject(refreshedToken) != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(AppLibrary.FCM_TOKEN, refreshedToken);//FCM token updated in shared preferences
            editor.commit();
        }

//        sendRegistrationToServer(refreshedToken);
        if (AppLibrary.checkStringObject(preferences.getString(AppLibrary.USER_LOGIN,"")) != null && AppLibrary.checkStringObject(refreshedToken) != null) {
            HashMap<Object, Object> postObject = new HashMap<>();
            postObject.put(USER_ID, preferences.getString(AppLibrary.USER_LOGIN, ""));
            postObject.put(NOTIFICATION_ID, refreshedToken);
            FireBaseHelper.getInstance(getApplicationContext()).postFireBaseRequest(UPDATE_TOKEN, postObject);
        }
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        SharedPreferences preferences = getSharedPreferences(AppLibrary.APP_SETTINGS,0);
        if (AppLibrary.checkStringObject(preferences.getString(AppLibrary.USER_LOGIN,"")) != null){
            List<NameValuePair> pairs = new ArrayList<>();
            pairs.add(new BasicNameValuePair("notificationId",token));
            pairs.add(new BasicNameValuePair("userId",preferences.getString(AppLibrary.USER_LOGIN,"")));
            RequestManager.makePostRequest(getApplicationContext(),RequestManager.UPDATE_TOKEN_REQUEST,RequestManager.UPDATE_TOKEN_RESPONSE,
                    null,pairs,onTokenUpdatedCallback);
        }
    }

    private RequestManager.OnRequestFinishCallback onTokenUpdatedCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            if (success){
                try {
                    JSONObject jsonObject = (JSONObject)response;
                    if (jsonObject.getString("error").equalsIgnoreCase("false")){
                        AppLibrary.log_d(TAG,"Token successfully updated");
                    } else {
                        AppLibrary.log_d(TAG,"Update token failed");
                    }
                } catch (JSONException exception){
                    exception.printStackTrace();
                }
            } else {
                AppLibrary.log_d(TAG,"Update token request failed");
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };
}