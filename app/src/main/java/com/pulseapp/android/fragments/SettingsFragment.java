package com.pulseapp.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.customViews.CircleImageView;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.nio.DoubleBuffer;
import java.util.HashMap;

/**
 * Created by indianrenters on 7/27/16.
 */
public class SettingsFragment extends BaseFragment {

    private UserModel myUserModel;
    private SettingsModel mSettingsModel;
    private CircleImageView profileImage;
    private SwitchCompat pushNotification, travelMode, autoSaveMode;
    private RelativeLayout nameContainer, birthdayContainer, receiveFromContainer, friendsContainer, institutionContainer;
    private LinearLayout privacyPolicyContainer;
    private TextView userName, userHandle, momentsCount, friendsCount, userNameProfile, userHandleProfile, userBirthday, appVersion, receiveFrom, clearConversation, institution;
    private final String TAG = SettingsFragment.class.getSimpleName();
    private TextView blocked, logout, support, instituteIndicator;
    private View separator3, separator9, separator10;
    private SharedPreferences prefs;
    private HashMap<String, SocialModel.Friends> followedListHashMap;
    private OnDataLoadedCallback onDataLoadedCallback;

    public void updateSocialCount() {
        SocialModel socialModel = mFireBaseHelper.getSocialModel();
        friendsCount.setText((socialModel.relations == null) ? "0" : String.valueOf(socialModel.relations.size()));
    }

    public interface OnDataLoadedCallback{
        void onFollowedListLoaded(HashMap<String, SocialModel.Friends> followedListHashMap);
    }


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onDataLoadedCallback = (OnDataLoadedCallback) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings_dashboard, container, false);

        if (savedInstanceState!=null) return rootView;

        initializeViews(rootView);
        return rootView;
    }

    private void initializeViews(View rootView) {
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS,0);
        mFireBaseHelper = FireBaseHelper.getInstance(getActivity());
        myUserModel = mFireBaseHelper.getMyUserModel();
        mSettingsModel = mFireBaseHelper.getSettingsModel();
        nameContainer = (RelativeLayout) rootView.findViewById(R.id.rl_name_container);
        birthdayContainer = (RelativeLayout) rootView.findViewById(R.id.rl_birthday_container);
        profileImage = (CircleImageView) rootView.findViewById(R.id.iv_profile_pic);
        userName = (TextView) rootView.findViewById(R.id.tv_user_full_name);
        userHandle = (TextView) rootView.findViewById(R.id.tv_user_handle);
        momentsCount = (TextView) rootView.findViewById(R.id.tv_moments_count);
        friendsCount = (TextView) rootView.findViewById(R.id.tv_friends_count);
        userNameProfile = (TextView) rootView.findViewById(R.id.tv_name);
        userHandleProfile = (TextView) rootView.findViewById(R.id.tv_username);
        userBirthday = (TextView) rootView.findViewById(R.id.tv_birthday);
        appVersion = (TextView) rootView.findViewById(R.id.tv_app_version);
        privacyPolicyContainer = (LinearLayout) rootView.findViewById(R.id.ll_privacy_policy_container);
        receiveFromContainer = (RelativeLayout) rootView.findViewById(R.id.rl_receive_from_container);
        receiveFrom = (TextView) rootView.findViewById(R.id.tv_receive_from);
        pushNotification = (SwitchCompat) rootView.findViewById(R.id.switch_notification);
        travelMode = (SwitchCompat) rootView.findViewById(R.id.switch_travel_data);
        autoSaveMode = (SwitchCompat) rootView.findViewById(R.id.switch_auto_save);
        blocked = (TextView) rootView.findViewById(R.id.tv_blocked_indicator);
        clearConversation = (TextView) rootView.findViewById(R.id.tv_clear_conversations_indicator);
        separator3 = rootView.findViewById(R.id.v_separator3);
        separator9 = rootView.findViewById(R.id.v_separator9);
        friendsContainer = (RelativeLayout) rootView.findViewById(R.id.ll_friends_container);
        institutionContainer = (RelativeLayout) rootView.findViewById(R.id.rl_institution_container);
        institution = (TextView) rootView.findViewById(R.id.tv_institution);
        logout = (TextView) rootView.findViewById(R.id.tv_logout_indicator);
        support = (TextView) rootView.findViewById(R.id.tv_support_indicator);
        instituteIndicator = (TextView) rootView.findViewById(R.id.tv_institution_indicator);
        separator10 = rootView.findViewById(R.id.v_separator10);

        setupActionBar(rootView.findViewById(R.id.action_bar));
        setUpViews();


    }

    private void setupActionBar(View actionBar) {
//        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        ImageView back = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popFragmentFromBackStack();
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("SETTINGS");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.setMarginStart(Utils.dp2Px(72));
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState!=null) return;

        loadFollowers();
    }

    private void loadFollowers() {
        followedListHashMap = new HashMap<>();
        mFireBaseHelper.getNewFireBase(FOLLOWED_ROOM, new String[]{myUserId}).keepSynced(true);
        mFireBaseHelper.getNewFireBase(FOLLOWED_ROOM, new String[]{myUserId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        SocialModel.Friends following = snapshot.getValue(SocialModel.Friends.class);
                        followedListHashMap.put(snapshot.getKey(),following);
                    }
                    // send to friends fragment
                    onDataLoadedCallback.onFollowedListLoaded(followedListHashMap);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setUpViews() {
        if (myUserModel != null) {
            if (myUserModel.imageUrl != null && !myUserModel.imageUrl.isEmpty()) {
                Picasso.with(getActivity()).load(myUserModel.imageUrl).fit().centerCrop().into(profileImage);
            }

            if (myUserModel.name != null && !myUserModel.name.isEmpty()) {
                userName.setText(myUserModel.name);
                userNameProfile.setText(myUserModel.name);
            }

            if (myUserModel.handle != null && !myUserModel.handle.isEmpty()) {
                userHandle.setText("@" + myUserModel.handle);
                userHandleProfile.setText("@" + myUserModel.handle);
            }

            // hardcoding friends and memories count as of now
            momentsCount.setText("2727");
            SocialModel socialModel = mFireBaseHelper.getSocialModel();

            friendsCount.setText((socialModel.relations == null) ? "0" : String.valueOf(socialModel.relations.size()));

            if (mSettingsModel != null && mSettingsModel.birthday != null) {
                userBirthday.setText(mSettingsModel.birthday);
            } else userBirthday.setText("Update Birthday");

            if(prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED,true)){
                if (myUserModel != null) {
                    if (myUserModel.miscellaneous != null && myUserModel.miscellaneous.institutionData != null && myUserModel.miscellaneous.institutionData.name != null){
                        institution.setText(Utils.getCustomMarquee(myUserModel.miscellaneous.institutionData.name, 27));
                    }
                    else {
                        institution.setText("Update Institution");
                    }
                }
            } else {
                institutionContainer.setVisibility(View.GONE);
                separator10.setVisibility(View.GONE);
            }

            birthdayContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).openBirthdayFragment();
                }
            });

            nameContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).openChangeNameFragment();
                }
            });

            if (AppLibrary.MESSAGING_DISABLED) {
                receiveFromContainer.setVisibility(View.GONE);
                clearConversation.setVisibility(View.GONE);
                separator9.setVisibility(View.GONE);
                separator3.setVisibility(View.GONE);

            } else {
                receiveFromContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CameraActivity) getActivity()).openReceiveFromFragment();
                    }
                });

                clearConversation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Voila..! Clear Conversation", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            privacyPolicyContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).openPrivacyPolicyFragment();
                }
            });

            if (mSettingsModel != null) {
                pushNotification.setChecked(mSettingsModel.pushNotification);
                autoSaveMode.setChecked(mSettingsModel.autoSaveMode);
                travelMode.setChecked(mSettingsModel.travelMode);
            }

            blocked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).openBlockedFragment();
                }
            });

            friendsContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).openFriendsFragment();
                }
            });

            logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLogoutPopup(getActivity(), "Are you sure you want to Logout?");
                }
            });

            support.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Pulse App Support");
                    intent.setData(Uri.parse("mailto:contact@instalively.com"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else
                        showCustomToast(getActivity(), R.layout.only_text_toast, Gravity.BOTTOM | Gravity.CENTER, 0, 100, 2000, "No Email apps on your device");
                }
            });

            institutionContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UserModel model = mFireBaseHelper.getMyUserModel();
                    if (model.miscellaneous==null || model.miscellaneous.institutionData==null)
                        ((CameraActivity) getActivity()).openInstitutionEditFragment(false);
                    else
                        showCustomToast(getActivity(), R.layout.only_text_toast, Gravity.BOTTOM | Gravity.CENTER, 0, 100, 2000, "Please contact support to change institute");
                }
            });

            appVersion.setText(BuildConfig.VERSION_NAME);

        }
    }

    void popFragmentFromBackStack() {
        if (getFragmentInBackStack(SettingsFragment.class.getSimpleName()) != null) {
            boolean popped = getActivity().getSupportFragmentManager().popBackStackImmediate();
            Log.d(TAG, "popFragmentFromBackStack: popped " + popped);
        } else Log.d(TAG, "popFragmentFromBackStack: fragment is null");
    }

    /**
     * @param fragmentTag the String supplied while fragment transaction
     * @return null if not found
     */
    private Fragment getFragmentInBackStack(String fragmentTag) {
        return getActivity().getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }

    public void updateNameOnUI() {
        if (userName != null && userNameProfile != null && !userName.getText().toString().equals(myUserModel.name)) {
            userName.setText(myUserModel.name);
            userNameProfile.setText(myUserModel.name);
        }
    }

    public void updateReceiveFromOnUI(String s) {
        if (s != null && receiveFrom != null) {
            receiveFrom.setText(s);
        }
    }

    public void updateBirthdayOnUI(String s) {
        if (s != null && userBirthday != null) {
            userBirthday.setText(s);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFireBaseHelper==null || travelMode==null || autoSaveMode==null || pushNotification==null) return;

        if (mSettingsModel != null) {
            if (pushNotification.isChecked() != mSettingsModel.pushNotification)
                mFireBaseHelper.updatePushNotificationFlagOnFireBase(pushNotification.isChecked());

            if (travelMode.isChecked() != mSettingsModel.travelMode)
                mFireBaseHelper.updateTravelModeFlagOnFireBase(travelMode.isChecked());

            if (autoSaveMode.isChecked() != mSettingsModel.autoSaveMode)
                mFireBaseHelper.updateAutoSaveModeFlagOnFireBase(autoSaveMode.isChecked());

        } else {
            mFireBaseHelper.updateTravelModeFlagOnFireBase(travelMode.isChecked());
            mFireBaseHelper.updateAutoSaveModeFlagOnFireBase(autoSaveMode.isChecked());
            mFireBaseHelper.updatePushNotificationFlagOnFireBase(pushNotification.isChecked());
        }
    }

    public void updateInstitutionName(String name) {
        name = Utils.getCustomMarquee(name, 27);
        if (institution != null) {
            if(institution.getText().toString().equalsIgnoreCase("Update Institution")){
                institution.setText(name);
            } else institution.setText(name);
        }
    }

    public HashMap<String, SocialModel.Friends> getFollowedUsersList() {
        return followedListHashMap;
    }
}
