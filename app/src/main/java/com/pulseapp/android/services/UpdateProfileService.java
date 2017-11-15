package com.pulseapp.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;

import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.util.AppLibrary;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by abc on 12/15/2015.
 */
public class UpdateProfileService extends IntentService {

    private static final String TAG = "UpdateProfileService";
    private SharedPreferences preferences;
    private String updatedUserName,updatedBioInfo;


    public UpdateProfileService(){
        super(UpdateProfileService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        updateProfileInfo(intent);
    }

//    private void updateProfileInfo(Intent intent){
//        List<NameValuePair> pairs = new ArrayList<>();
//        if (intent.hasExtra("updatedUserName")) {
//            updatedUserName = intent.getStringExtra("updatedUserName");
//            pairs.add(new BasicNameValuePair("fullName", updatedUserName));
//        }
//        if (intent.hasExtra("updateBioInfo")) {
//            updatedBioInfo = intent.getStringExtra("updateBioInfo");
//            pairs.add(new BasicNameValuePair("bio",updatedBioInfo));
//        }
//        pairs.add(new BasicNameValuePair("login_step","1"));
//        RequestManager.makePostRequest(getApplicationContext(), RequestManager.UPDATE_USER_DETAILS_REQUEST, RequestManager.UPDATE_USER_DETAILS_RESPONSE, null, pairs, userProfileUpdateCallback);
//    }

    private RequestManager.OnRequestFinishCallback userProfileUpdateCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            if (success) {
                JSONObject object = (JSONObject) response;
                try {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        AppLibrary.log_d("shwstppr", "User Profile Update!");
                        preferences = getApplicationContext().getSharedPreferences(AppLibrary.APP_SETTINGS,0);
                        if (AppLibrary.checkStringObject(updatedUserName) != null)
                            preferences.edit().putString(AppLibrary.USER_NAME,updatedUserName).commit();
                        if (AppLibrary.checkStringObject(updatedBioInfo) != null)
                            preferences.edit().putString(AppLibrary.FACEBOOK_BIO_INFO, updatedBioInfo).commit();
                    } else {
                        //do nothing
                        AppLibrary.log_e("shwstppr", "Error updating user profile!" + object.getString("value"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                //request error
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

    };
}
