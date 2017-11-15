package com.pulseapp.android.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.ChooseUserNameFragment;
import com.pulseapp.android.fragments.OnBoardingAddFriendFragment;
import com.pulseapp.android.fragments.SelectInstitutionFragment;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.BaseActivity;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deepankur on 6/22/16.
 */
public class OnBoardingActivity extends BaseActivity implements FireBaseKEYIDS {

    private String TAG = this.getClass().getSimpleName();
    private String userName;
    private SharedPreferences prefs;
    private static final int PERMISSION_ACCESS_CAMERA_MICROPHONE = 0;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        loadInstitutionData();
        setContentView(R.layout.activity_on_boarding);
        userName = getIntent().getStringExtra(AppLibrary.USER_NAME);
        int onBoardStatus = getIntent().getIntExtra(AppLibrary.USER_ONBOARDING_STATUS, 3);

        if (onBoardStatus == ON_BOARDING_NOT_STARTED)
            loadSelectUserNameFragment();
        if (onBoardStatus == USER_NAME_SELECTION_DONE)
            loadAddFriendsFragment();
        if (onBoardStatus == FRIENDS_INVITING_DONE && prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED, true))
            loadAddInstituteFragment();
        if (onBoardStatus == INSTITUTION_PROVING_DONE || (onBoardStatus == FRIENDS_INVITING_DONE && !prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED, true))) { // Might Never used, just being safe
            requestCameraPermissionsAndProceed();
        }
    }


    public void loadSelectUserNameFragment() {//pos not required; data has complete info
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_ONBOARDING_STARTING);
        Bundle b = new Bundle();
        b.putString(AppLibrary.USER_NAME, userName);
        ChooseUserNameFragment chooseUserNameFragment = new ChooseUserNameFragment();
        chooseUserNameFragment.setArguments(b);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.onBoardingFrameHolder, chooseUserNameFragment, ChooseUserNameFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void onUserNameSelectedSuccessfully() {
        AnalyticsManager.getInstance().createUserAfterSignUp(getApplicationContext(), FireBaseHelper.getInstance(this).getMyUserModel().handle, FireBaseHelper.getInstance(this).getMyUserModel().gender, FireBaseHelper.getInstance(this).getSettingsModel());
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_ONBOARDING_USERNAME_DONE);
        updateOnBoardingStatusInPreference(USER_NAME_SELECTION_DONE);
        updateOnBoardingStatusInServer(USER_NAME_SELECTION_DONE);
        loadAddFriendsFragment();
    }

    private void loadAddFriendsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.onBoardingFrameHolder, new OnBoardingAddFriendFragment(), ChooseUserNameFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void onFriendsSelectionDone(String userIdsToWhichRequestSent) {
        updateOnBoardingStatusInPreference(FRIENDS_INVITING_DONE);
        updateOnBoardingStatusInServer(FRIENDS_INVITING_DONE,userIdsToWhichRequestSent);

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_ONBOARDING_FRIEND_SELECTION_DONE);

        if (prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED, true))
            loadAddInstituteFragment();
        else {
            requestCameraPermissionsAndProceed();
        }
    }


    private void loadAddInstituteFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.onBoardingFrameHolder, new SelectInstitutionFragment(), ChooseUserNameFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * @param hasSelected     whether anything selected on not
     * @param institutionData null if not selected, obj otherwise
     * @param query           the last status of the Edit text
     */
    public void onInstitutionSelectionComplete(boolean hasSelected, UserModel.InstitutionData institutionData, String query) {
        String myUserId = FireBaseHelper.getInstance(this).getMyUserId();
        if (hasSelected) {
            FireBaseHelper.getInstance(this).sendInstituteChangeRequest(institutionData);
//          FireBaseHelper.getInstance(this).getNewFireBase(ANCHOR_USERS, new String[]{myUserId, MISCELLANEOUS, INSTUTUTION_DATA}).setValue(institutionData);
        } else {
            if (query.trim().length() > 0) {
                DatabaseReference firebase = FireBaseHelper.getInstance(getApplicationContext()).getNewFireBase(ANCHOR_QUERIED_INST, null);
                firebase.push().setValue(new UserModel.QueriedInstitution(myUserId, query));
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.INSTITUTION_NOT_FOUND);
            }
        }
        updateOnBoardingStatusInPreference(INSTITUTION_PROVING_DONE);
        requestCameraPermissionsAndProceed(); //Request camera permissions before starting activity
        updateOnBoardingStatusInServer(INSTITUTION_PROVING_DONE);
    }


    @Override
    protected void onPermissionsGranted(String[] permissions) {
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.APP_ONBOARDING_SUCCESSFUL);
        Intent mIntent = new Intent(OnBoardingActivity.this, CameraActivity.class);
        startActivity(mIntent);
        finish();
    }


    @SuppressLint("CommitPrefEdits")
    void updateOnBoardingStatusInPreference(int onBoardStatus) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(AppLibrary.USER_ONBOARDING_STATUS, onBoardStatus);
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // For the invite in OnBoardingAddFriendFragment
        MasterClass.callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @SuppressWarnings("deprecation")
    void updateOnBoardingStatusInServer(int onBoardingStatus) {
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("userId", FireBaseHelper.getInstance(this).getMyUserId()));
        pairs.add(new BasicNameValuePair("onboarding_status", String.valueOf(onBoardingStatus)));
        RequestManager.makePostRequest(this, RequestManager.UPDATE_ON_BOARDING_STATUS_REQUEST, RequestManager.UPDATE_ONBOARDING_STATUS_RESPONSE,
                null, pairs, onBoardingStatusCallback);
    }

    @SuppressWarnings("deprecation")
    void updateOnBoardingStatusInServer(int onBoardingStatus, @Nullable String commaSeparatedUserIds) {
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("userId", FireBaseHelper.getInstance(this).getMyUserId()));
        pairs.add(new BasicNameValuePair("onboarding_status", String.valueOf(onBoardingStatus)));
        if (commaSeparatedUserIds != null && commaSeparatedUserIds.length() > 0) {
            Log.d(TAG, "updateOnBoardingStatusInServer: send request " + commaSeparatedUserIds);
            pairs.add(new BasicNameValuePair("sentIds", commaSeparatedUserIds));
        }
        RequestManager.makePostRequest(this, RequestManager.UPDATE_ON_BOARDING_STATUS_REQUEST, RequestManager.UPDATE_ONBOARDING_STATUS_RESPONSE,
                null, pairs, onBoardingStatusCallback);
    }

    private RequestManager.OnRequestFinishCallback onBoardingStatusCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                Log.d(TAG, " server says " + response);
                final JSONObject object = (JSONObject) response;
                if (success) {
                    Log.d(TAG, object.getString("value"));
                } else {
                    Log.e(TAG, "onBoardingStatusCallback Error, response -" + object);
                    // request failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "onBoardingStatusCallback JsonException " + e);

            }
        }

        @Override
        public boolean isDestroyed() {
            return isDestroyed;
        }
    };

    boolean isDestroyed;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }

    static ArrayList<UserModel.InstitutionData> institutionDatas = new ArrayList<>();

    void loadInstitutionData() {
        FireBaseHelper.getInstance(this).loadAllInstitutesData();
        institutionDatas = FireBaseHelper.getInstance(this).getAllInstitutesData();
    }

    public static ArrayList<UserModel.InstitutionData> getInstitutionDetails() {
        return institutionDatas;
    }
}
