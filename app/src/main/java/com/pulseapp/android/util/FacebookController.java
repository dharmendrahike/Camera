package com.pulseapp.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.signals.BroadCastSignals;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by abc on 12/4/2015.
 */
public class FacebookController {

    private static FacebookController facebookController;
    private AccessTokenTracker accessTokenTracker;
    private String TAG = "FacebookController";
    private String currentAccessToken = null;
    private static Context mContext;
    private SharedPreferences preferences;

    private FacebookController(){
    }

    public static FacebookController getInstance(Context context){
        mContext = context;
        if (facebookController == null){
            facebookController = new FacebookController();
        }
        return facebookController;
    }

    public void startAccessTokenTracker(){
        if (accessTokenTracker == null){
            accessTokenTracker = new AccessTokenTracker() {
                @Override
                protected void onCurrentAccessTokenChanged(AccessToken oldToken, AccessToken currentToken) {
                    AppLibrary.log_d(TAG,"OnCurrentAccessTokenChanged, "+"Current access token is -"+currentToken.getToken());
                    currentAccessToken = currentToken.getToken();
                    storeInPreferences(currentAccessToken);
                }
            };
        }
        if (!accessTokenTracker.isTracking()) {
            accessTokenTracker.startTracking();
            AppLibrary.log_d(TAG, "Starting Access token tracker");
        }
    }

    public void stopAccessTokenTracker(){
        if (accessTokenTracker != null){
            AppLibrary.log_d(TAG, "Stopping Access token tracker");
            accessTokenTracker.stopTracking();
        }
    }

    public String getCurrentAccessToken(){
        return currentAccessToken;
    }

    public void setCurrentAccessToken(String token){
        currentAccessToken = token;
    }

    private void storeInPreferences(String token){
        preferences = mContext.getSharedPreferences(AppLibrary.APP_SETTINGS,0);
        preferences.edit().putString(AppLibrary.FACEBOOK_ACCESS_TOKEN,token).commit();
    }

//    public void fetchFacebookFriends(){
//        preferences = mContext.getSharedPreferences(AppLibrary.APP_SETTINGS,0);
//        AppLibrary.log_d(TAG,"Fetching Facebook Friends with access token -"+preferences.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN,""));
//        List<NameValuePair> pairs = new ArrayList<>();
//        pairs.add(new BasicNameValuePair("access_token",preferences.getString(AppLibrary.FACEBOOK_ACCESS_TOKEN,"")));
//        RequestManager.makeGetRequest(mContext, RequestManager.FACEBOOK_FRIENDS_REQUEST, RequestManager.FACEBOOK_FRIENDS_RESPONSE,
//                pairs, fetchFriendsCallback);
//    }

    RequestManager.OnRequestFinishCallback fetchFriendsCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                if (response == null)
                    return;
                JSONObject object = (JSONObject) response;

                //Get the instance of JSONArray that contains JSONObjects
                if (object.optJSONArray("data") == null)
                    return;
                JSONArray jsonArray = object.optJSONArray("data");
                final String[] id = new String[jsonArray.length()];
                final String[] name = new String[jsonArray.length()];
                final String[] link = new String[jsonArray.length()];
                //Iterate the jsonArray and print the info of JSONObjects
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    id[i] = jsonObject.optString("id");
                    name[i] = jsonObject.optString("name");
                    JSONObject picture = jsonObject.getJSONObject("picture");
                    JSONObject data = picture.getJSONObject("data");
                    link[i] = data.optString("url");
                }
                StringBuilder idBuilder = new StringBuilder();
                StringBuilder nameBuilder = new StringBuilder();
                StringBuilder pictureBuilder = new StringBuilder();
                for (int i = 0; i < id.length; i++) {
                    idBuilder.append(id[i]).append(",");
                }
                for (int i = 0; i < name.length; i++) {
                    nameBuilder.append(name[i]).append(",");
                }
                for (int i = 0; i < link.length; i++) {
                    pictureBuilder.append(link[i]).append(",");
                }
                // TODO use json array itself from response instead of looping through it to create three separate string arrays
                preferences.edit().putString("FacebookFriendsIdArray", idBuilder.toString()).commit();
                preferences.edit().putString("FacebookFriendsNameArray", nameBuilder.toString()).commit();
                preferences.edit().putString("FacebookFriendsPictureArray", pictureBuilder.toString()).commit();
                Date date = new Date(System.currentTimeMillis());
                preferences.edit().putLong(AppLibrary.FACEBOOK_FRIENDS_LAST_FETCHED,date.getTime()).commit();
                //send broadcast and register it in social fragment to make tag friends layout visible
                MasterClass.getEventBus().post(new BroadCastSignals.FetchFriendsStatus(true));
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };

}
