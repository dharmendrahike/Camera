package com.pulseapp.android.fragments;


import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.util.Pair;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.login.LoginResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.FriendListAdapter;
import com.pulseapp.android.adapters.MomentListAdapter;
import com.pulseapp.android.adapters.RecentFriendListAdapter;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.modelView.MediaModelView;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.PrivacyPopup;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by abc on 4/12/2016.
 */
public class ShareMediaFragment extends BaseFragment implements View.OnClickListener,
        FriendListAdapter.ViewControlsCallback, RecentFriendListAdapter.ViewControlsCallback
        , MomentListAdapter.ViewControlsCallback, FireBaseHelper.CustomFriendListCallBack, CreateManageGroupFragment.ViewControlsCallback, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "ShareMediaFragment";

    private static final int RECENT_LIST_SIZE = 1;

    private RecyclerView recentRoomListRecyclerView, momentRecyclerView, roomListRecyclerView;
    private LinearLayout newGroupButton;
    private ViewControlsCallback viewControlsCallback;
    private List<ListRoomView> friendsList;
    private List<ListRoomView> recentFriendList, allFriendList;
    private Map<String, MomentModel> modelMap;
    private View rootView;
    private String mediaPath;
    private HashMap<String, Integer> selectedMomentList;
    private HashMap<String, Integer> selectedRoomList;

    private int expiryType = 1;
    private String mediaText = null;
    private int actionType, currentActiveView;

    private SettingsModel settingsModel;
    private HashMap<String, String> selectedRoomForMyMomentShare;
    private Spinner spinner;
    private LinearLayout customFriendsLayout;
    private LinearLayout shareLayout;
    private TextView shareText;
    private ImageView shareImageView;
    private String lastUsedPrivacy;
    private SwitchCompat nonAnonymous;
    private TextView switchTv;
    private RelativeLayout anonymContainer;
    private boolean postAnonymous = false;
    private LinearLayout postFacebookLayout;
    private AppCompatCheckBox facebookRadio;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;

        Log.d(TAG, "onCreate");
        Bundle bundle = getArguments();
        mediaPath = bundle.getString("mediaPath");
        mediaText = bundle.getString(AppLibrary.MEDIA_TEXT);
        selectedRoomForMyMomentShare = new HashMap<>();
        actionType = PrivacyPopup.ALL_FRIENDS_AND_FOLLOWERS;
        allFriendList = mFireBaseHelper.getListRoomView();
        recentFriendList = new ArrayList<>();
        friendsList = new ArrayList<>();
        if (allFriendList != null) {
            int maxRecentCount = (allFriendList.size() > RECENT_LIST_SIZE) ? RECENT_LIST_SIZE : allFriendList.size();
            if (maxRecentCount >= RECENT_LIST_SIZE) {
                for (int i = 0; i < allFriendList.size(); i++) {
                    if (i <= RECENT_LIST_SIZE)
                        recentFriendList.add(allFriendList.get(i));
                    else friendsList.add(allFriendList.get(i));
                }
            } else {
                recentFriendList = allFriendList;
            }
        }
        mFireBaseHelper.setCustomFriendListCallback(this);
        selectedMomentList = new HashMap<>();
        selectedRoomList = new HashMap<>();
        createCustomizeFriendList();
        createCustomRecentFriendList();
    }

    private void createCustomRecentFriendList() {
        Collections.sort(recentFriendList, new Comparator<ListRoomView>() {
            @Override
            public int compare(ListRoomView lhs, ListRoomView rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.share_media_fragment, container, false);

        if (savedInstanceState != null) return rootView;

        ((CameraActivity) getActivity()).toggleFullScreen(false);

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;//prevent bottom layer from consuming touch events
            }
        });
        initActionBar(rootView.findViewById(R.id.action_bar));
        initializeViewObjects(rootView);
        initPrivacyPopup(rootView);
        return rootView;
    }

    private void initPrivacyPopup(View rootView) {
        privacyPopup = (PrivacyPopup) rootView.findViewById(R.id.pricacyPopup);
        privacyPopup.onSettingsDataChanged(settingsModel);
        privacyPopup.setVisibility(View.GONE);
        privacyPopup.setPopupListener(new PrivacyPopup.PopupListener() {

            @Override
            public void onUpdatePrivacyTextView(String text, int resourceId) {
                if (resourceId == R.drawable.privacy_friendsexcept_svg)
                    shareText.setText("Friends except: " + text);
                else shareText.setText(text);
                shareImageView.setImageResource(resourceId);
            }

            @Override
            public void onOpeneditExistingList(String listId) {
                loadFriendSelectionFragment(PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW, listId);
            }

            @Override
            public void onShareWithAllFriends() {

            }

            @Override
            public void onShareWithAllFriendsAndFollowers() {

            }

            @Override
            public void onOpenExcludedFriends() {
                loadFriendSelectionFragment(PrivacyPopup.FRIENDS_EXCEPT_ROW, null);
            }

            @Override
            public void onCreateCustomListFriend() {
                loadFriendSelectionFragment(PrivacyPopup.CREATE_NEW_LIST_ROW, null);
            }

            @Override
            public void onSelectionDone() {

            }

            @Override
            public void updatePrivacy() {
                updateLastUsedPrivacy();
            }

            @Override
            public void onDismissPopup() {
                privacyPopup.setVisibility(View.GONE);
            }
        });

        privacyPopup.setLastUsedChecked();
    }

    ImageView addFriendIv;
    PrivacyPopup privacyPopup;

    public boolean onBackPressed() {
        if (privacyPopup.getVisibility() == View.VISIBLE) {
            lastUsedPrivacy = privacyPopup.getLastUsedPrivacy();
            privacyPopup.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    private void loadAddFriendFriendFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        String TAG = CameraActivity.ADD_FRIEND_FROM_SHARE_TAG;
        fragmentTransaction.add(R.id.fragmentContainer, new AddFriendFragment(), TAG);
        fragmentTransaction.addToBackStack(TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);

        addFriendIv = ((ImageView) actionBar.findViewById(R.id.action_bar_IV_4));
        addFriendIv.setImageResource(R.drawable.add_friends_svg);
        addFriendIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAddFriendFriendFragment();
            }
        });

        ((ImageView) actionBar.findViewById(R.id.action_bar_IV_1)).setImageResource(R.drawable.back_svg);
        actionBar.findViewById(R.id.action_bar_IV_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        ((ImageView) actionBar.findViewById(R.id.action_bar_IV_3)).setImageResource(R.drawable.search_svg);
        (actionBar.findViewById(R.id.action_bar_IV_3)).setVisibility(View.GONE); //ToDo - Remove when implemented

        TextView header = (TextView) actionBar.findViewById(R.id.titleTV);
        header.setAllCaps(false);
        header.setText("Share for "+mFireBaseHelper.getMediaExpiryTime()+"hr");
        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
    }

    private void createCustomizeFriendList() {
        ArrayList<ListRoomView> tempList = new ArrayList<>(friendsList);
        friendsList.clear();
        Collections.sort(tempList, new Comparator<ListRoomView>() {
            @Override
            public int compare(ListRoomView lhs, ListRoomView rhs) {
                return lhs.normalizedFirstChar - rhs.normalizedFirstChar;
            }
        });

        int currentChar = -1111;//ascii
        for (ListRoomView roomView : tempList) {//single pass iteration; nullify redundant objects
            if (roomView.normalizedFirstChar != currentChar) {
                friendsList.add(null);//null indicating the header view; else normal shit
                currentChar = roomView.normalizedFirstChar;//resetting isSeparator  here
            }
            friendsList.add(roomView);//adding always
        }

    }

    private LinkedHashMap<String, MediaModelView> sortMyStreams(LinkedHashMap<String, MediaModelView> list) {
        LinkedHashMap<String, MediaModelView> newSortedList = new LinkedHashMap<>();
        if (list != null) {
            List<Map.Entry<String, MediaModelView>> entryList = new LinkedList<>(list.entrySet());
            Collections.sort(entryList, new Comparator<Map.Entry<String, MediaModelView>>() {
                @Override
                public int compare(Map.Entry<String, MediaModelView> ele1,
                                   Map.Entry<String, MediaModelView> ele2) {
                    return (int) (ele2.getValue().createdAt - ele1.getValue().createdAt);
                }
            });
            for (Map.Entry<String, MediaModelView> entry : entryList) {
                newSortedList.put(entry.getKey(), entry.getValue());
            }
        }
        return newSortedList;
    }

    AppCompatCheckBox checkbox;

    private void updateMyMomentView(View rootView) {
        RelativeLayout rl = (RelativeLayout) rootView.findViewById(R.id.myMomentView);
        rl.requestFocus();
        checkbox = (AppCompatCheckBox) rootView.findViewById(R.id.myMomentItemRadio);
        rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = !checkbox.isChecked();
                checkbox.setChecked(checked);
//                onMomentSelected(mFireBaseHelper.getMyUserModel().momentId, MY_MOMENT, checked);
                onMomentToggled();
                CameraActivity.savedStateOfShareMediaFragment.isMyStreamsChecked = checked;
            }
        });
        LinearLayout privacyView = (LinearLayout) rootView.findViewById(R.id.privacySettingView);
        shareText = (TextView) privacyView.findViewById(R.id.tv_shareText);
        shareImageView = (ImageView) privacyView.findViewById(R.id.iv_shareImage);
        privacyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (false) return;//// TODO: 7/25/16 privacy popup here
//                onMomentSelected(mFireBaseHelper.getMyUserModel().momentId, MY_MOMENT);
                onSettingsSelected(v);
            }
        });
        ImageView myMomentsIV = (ImageView) rootView.findViewById(R.id.momentImage);
        LinkedHashMap<String, MediaModelView> myStreamsList = sortMyStreams(mFireBaseHelper.getMyStreams());
        if (myStreamsList != null && myStreamsList.size() > 0) {
            List<Map.Entry<String, MediaModelView>> entryList =
                    new ArrayList<Map.Entry<String, MediaModelView>>(myStreamsList.entrySet());
            Map.Entry<String, MediaModelView> firstEntry =
                    entryList.get(0);
            if (firstEntry != null) {
                if (AppLibrary.getMediaType(firstEntry.getValue().url) == AppLibrary.MEDIA_TYPE_IMAGE) {
                    Picasso.with(getActivity()).load(new File(firstEntry.getValue().url)).transform(new RoundedTransformation()).centerCrop().fit().into(myMomentsIV);
                } else {
                    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(new File(firstEntry.getValue().url).getAbsolutePath(), MediaStore.Images.Thumbnails.MICRO_KIND);
                    if (bitmap != null) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
                        drawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
                        myMomentsIV.setImageDrawable(drawable);
                    } else {
                        Picasso.with(getActivity()).load(R.drawable.moment_circle_background).transform(new RoundedTransformation()).centerCrop().fit().into(myMomentsIV);
                    }
                }
            }
        }
    }

    public void updateCustomMomentView() {
        if (getActivity() == null) return;

        String publicContributionMomentId = null;
        int size = CameraActivity.contributableMoments.size();
        if (((CameraActivity) getActivity()).getPublicStreamContribution() != null)
            publicContributionMomentId = ((CameraActivity) getActivity()).getPublicStreamContribution().momentId;

        if (size == 2 &&
                CameraActivity.contributableMoments.containsKey(FireBaseHelper.getInstance(context).getMyUserModel().getMyInstitutionId())
                && CameraActivity.contributableMoments.containsKey(publicContributionMomentId)) {
            loadMomentFromFireBase();
        } else if (size == 1 &&
                (CameraActivity.contributableMoments.containsKey(FireBaseHelper.getInstance(context).getMyUserModel().getMyInstitutionId())
                        || CameraActivity.contributableMoments.containsKey(publicContributionMomentId))) {
            loadMomentFromFireBase();
        } else if (size == 0) {
            loadMomentFromFireBase();
        } else {
            setAdapter();
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    boolean isThreeDaysOld(Long createdAt) {
        if (createdAt == null || createdAt == 0)
            return false;
        long threeDays = 1000 * 60 * 60 * 24 * 3;
        long timePassedSinceWritingReference = System.currentTimeMillis() - createdAt;

        if (timePassedSinceWritingReference >= threeDays)
            return true;
        return false;
    }

    private void loadMomentFromFireBase() {
        DatabaseReference newFireBase = mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, CONTRIBUTABLE_MOMENTS});
        newFireBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isThreeDaysOld(dataSnapshot.child(UPDATED_AT).getValue(Long.class))) {
                    setAdapter();
                    Log.i(TAG, "onDataChange: moment seems old not populating");
                    return;
                }
                dataSnapshot = dataSnapshot.child(MOMENTS);
                if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                    setAdapter();
                    return;
                }

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    final CustomMomentModel customMomentModel = new CustomMomentModel(child.getKey(), false, child.child(THUMBNAIL).getValue(String.class), child.child(NAME).getValue(String.class));

                    if (child.child(AUTO_MODERATE).exists() && child.child(AUTO_MODERATE) != null)
                        customMomentModel.autoModerate = child.child(AUTO_MODERATE).getValue(Boolean.class);
                    if (child.child(TOTAL_VIEWS).exists() && child.child(TOTAL_VIEWS) != null)
                        customMomentModel.totalViews = child.child(TOTAL_VIEWS).getValue(Integer.class);

                    addToStream(customMomentModel);
                }
                setAdapter();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * @param customMomentModel will be added if necessary ie doesn't exist already
     */
    private void addToStream(CustomMomentModel customMomentModel) {
        Set<String> strings = CameraActivity.contributableMoments.keySet();//for additional check to avoid duplicate entry in the data structure
        if (!strings.contains(customMomentModel.momentId)) {
            CustomMomentModel publicStreamContribution = null;
            if (((CameraActivity) getActivity())!=null)
                publicStreamContribution = ((CameraActivity) getActivity()).getPublicStreamContribution();

            if (publicStreamContribution != null) {//if coming form public contribution
                for (Map.Entry<String, CustomMomentModel> entry : CameraActivity.contributableMoments.entrySet()) {
                    if (entry.getKey().equals(publicStreamContribution.momentId)) {
                        entry.getValue().isChecked = true;
                        break;
                    }
                }
            } else {//normal flow not from public stream
                for (Map.Entry<String, CustomMomentModel> entry : CameraActivity.contributableMoments.entrySet()) {
                    if (isThisMyInstitutionId(entry.getKey())) {
                        entry.getValue().isChecked = true;
                        break;
                    }
                }
            }
            CameraActivity.contributableMoments.put(customMomentModel.momentId, customMomentModel);
        }
    }

    private void setAdapter() {
        if (getActivity() == null) return;
        customFriendsLayout.setVisibility(View.VISIBLE);
        if (selectedMomentList != null)
            selectedMomentList.clear();
        momentRecyclerView.setAdapter(new MomentListAdapter(this, getActivity(), CameraActivity.contributableMoments));
        this.onMomentToggled();//need it to refresh the data structure

        initFaceBookAndAnonymousHistoryState(); //Maintains the state of Facebook and Anonymous selection
    }


    private void initializeViewObjects(View rootView) {
        updateMyMomentView(rootView);
        customFriendsLayout = (LinearLayout) rootView.findViewById(R.id.customMomentsLayout);
        spinner = (Spinner) rootView.findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);
        populateSpinner();
        newGroupButton = (LinearLayout) rootView.findViewById(R.id.newGroupLayout);

        roomListRecyclerView = (RecyclerView) rootView.findViewById(R.id.friendListRecyclerView);
        recentRoomListRecyclerView = (RecyclerView) rootView.findViewById(R.id.recentRecyclerView);
        momentRecyclerView = (RecyclerView) rootView.findViewById(R.id.momentRecyclerView);

        if (AppLibrary.MESSAGING_DISABLED) {
            newGroupButton.setVisibility(View.GONE);
            recentRoomListRecyclerView.setVisibility(View.GONE);
            roomListRecyclerView.setVisibility(View.GONE);
            rootView.findViewById(R.id.message_heading).setVisibility(View.GONE);
            rootView.findViewById(R.id.message_subheading).setVisibility(View.GONE);
        }

        roomListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        momentRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recentRoomListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        roomListRecyclerView.setNestedScrollingEnabled(false);
        momentRecyclerView.setNestedScrollingEnabled(false);
        recentRoomListRecyclerView.setNestedScrollingEnabled(false);

        shareLayout = (LinearLayout) rootView.findViewById(R.id.shareLayout);
        postFacebookLayout = (LinearLayout) rootView.findViewById(R.id.postFacebookLayout);
        facebookRadio = (AppCompatCheckBox) postFacebookLayout.findViewById(R.id.facebookItemRadio);
        postFacebookLayout.setOnClickListener(null);

        if (!((CameraActivity) getActivity()).isPublicStreamContributionMode() && (CameraActivity.savedStateOfShareMediaFragment.isMyStreamsChecked == null || CameraActivity.savedStateOfShareMediaFragment.isMyStreamsChecked)) {//non public contributionMode mode
            rootView.findViewById(R.id.myMomentView).performClick();
//            CameraActivity.isMyStreamsChecked = true;
        } else {
            if (CameraActivity.savedStateOfShareMediaFragment.isMyStreamsChecked != null && CameraActivity.savedStateOfShareMediaFragment.isMyStreamsChecked) {
                rootView.findViewById(R.id.myMomentView).performClick();
            }
        }
        anonymContainer = (RelativeLayout) rootView.findViewById(R.id.anonym_container);
        nonAnonymous = (SwitchCompat) rootView.findViewById(R.id.switch_non_anonymously);
        switchTv = (TextView) rootView.findViewById(R.id.tv_switch);
        nonAnonymous.setOnCheckedChangeListener(this);
        anonymContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nonAnonymous.toggle();
            }
        });


        if (friendsList != null) {
            recentRoomListRecyclerView.setAdapter(new RecentFriendListAdapter(this, getActivity(), recentFriendList));
            ((RecentFriendListAdapter) recentRoomListRecyclerView.getAdapter()).baseFragment = this;
            roomListRecyclerView.setAdapter(new FriendListAdapter(this, getActivity(), friendsList));
            ((FriendListAdapter) roomListRecyclerView.getAdapter()).baseFragment = this;
        }
        newGroupButton.setOnClickListener(this);
        shareLayout.setOnClickListener(this);
        updateCustomMomentView();
    }

    void initFaceBookAndAnonymousHistoryState() {
        if (CameraActivity.savedStateOfShareMediaFragment.isAnonymitySelected)
            anonymContainer.performClick();

        if (CameraActivity.savedStateOfShareMediaFragment.isFacebookButtonChecked)
            postFacebookLayout.performClick();
    }


    private View.OnClickListener facebookListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (selectedMomentList.size() > 0 || selectedRoomList.size() > 0) {

                if (facebookRadio.isChecked()) {
                    facebookRadio.setChecked(false);
                    notifyFacebookTapped(false);
                } else {
                    // check for facebook permissions
                    Set publishPermissionSet = new HashSet(Arrays.asList("publish_actions"));
                    if (AccessToken.getCurrentAccessToken() != null && AccessToken.getCurrentAccessToken().getPermissions().containsAll(publishPermissionSet)) {
                        // provided publish permissions
                        facebookRadio.setChecked(true);
                        notifyFacebookTapped(true);

                    } else {
                        if (facebookController != null)
                            facebookController.doFacebookLogin(Arrays.asList("publish_actions"), new CameraActivity.FacebookLoginCallback() {
                                @Override
                                public void onSuccessfulLoginCallback(LoginResult loginResult) {
                                    facebookRadio.setChecked(true);
                                    notifyFacebookTapped(true);

                                }

                                @Override
                                public void onErrorCallback() {
                                    facebookRadio.setChecked(false);
                                    notifyFacebookTapped(false);
                                }
                            });
                    }
                }
            } else {
                showShortToastMessage("Please select atleast one stream to post to Facebook");
            }
        }
    };

    private void notifyFacebookTapped(boolean isChecked) {
        Log.d(TAG, "notifyFacebookTapped: " + isChecked);
        CameraActivity.savedStateOfShareMediaFragment.isFacebookButtonChecked = isChecked;
    }

    private void populateSpinner() {
        ArrayList<String> types = new ArrayList<>();
        types.add("View once");
        types.add("View for 24hr");

        final ArrayAdapter<String> adapter = new TimerArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, types);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected " + position);
                selectedIndex = position;
                expiryType = position + 2;//todo change this to one when  uncommenting quick peek
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    FriendSelectionFragment friendSelectionFragment;

    public void loadFriendSelectionFragment(int actionType, String listId) {
        friendSelectionFragment = new FriendSelectionFragment();
        Bundle data = new Bundle();
        data.putInt("action", actionType);
        friendSelectionFragment.setArguments(data);
        if (actionType == PrivacyPopup.FRIENDS_EXCEPT_ROW)
            friendSelectionFragment.setIgnoredFriendList(context, this.mFireBaseHelper, this.settingsModel);
        else if (actionType == PrivacyPopup.CREATE_NEW_LIST_ROW)
            friendSelectionFragment.setCreateNewFriendList(context, this.mFireBaseHelper);
        else if ((actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW)) {
            friendSelectionFragment.editCustomList(context, this.mFireBaseHelper, this.settingsModel, listId);
        }
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, friendSelectionFragment, FriendSelectionFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(FriendSelectionFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();

        friendSelectionFragment.setFriendSelectionCallback(new FriendSelectionFragment.FriendSelectionCallback() {
            @Override
            public void onExit() {
                if (privacyPopup != null)
                    privacyPopup.selectRowItem(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
            }
        });
    }

    private int selectedIndex;

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            switchTv.setText("CONTRIBUTE ANONYMOUSLY TO:");
            postAnonymous = true;
        } else {
            switchTv.setText("CONTRIBUTE TO:");
            postAnonymous = false;
        }
        CameraActivity.savedStateOfShareMediaFragment.isAnonymitySelected = isChecked;
    }

    class TimerArrayAdapter extends ArrayAdapter<String> {
        ArrayList<String> types;

        TimerArrayAdapter(Context context, int resource, ArrayList<String> types) {
            super(context, resource);
            this.types = types;
        }

        @Override
        public int getCount() {
            return types.size();
        }


        @Override
        public View getDropDownView(int position, View convertView,
                                    @NonNull ViewGroup parent) {
            // TODO Auto-generated method stub
            return getCustomView(false, position, convertView, parent);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            return getCustomView(true, position, convertView, parent);
        }

        /**
         * @param isSelected  whether the spinner is in selected or dropdown mode
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        private View getCustomView(boolean isSelected, int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LinearLayout ll = new LinearLayout(context);
                int padding = AppLibrary.convertDpToPixels(context, 8);
                ll.setPadding(padding, padding, padding, padding);
                MontserratRegularTextView textView = new MontserratRegularTextView(context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                textView.setTextColor(position == selectedIndex ? context.getResources().getColor(R.color.pulse_light_violet) : context.getResources().getColor(R.color.black));
                textView.setAlpha(position == selectedIndex ? 0.53f : 1f);
                textView.setText(types.get(position));
                ll.addView(textView);
                if (isSelected) {
                    ll.setGravity(Gravity.END);
//                    textView.setGravity(Gravity.END);
                } else {
                    ll.setGravity(Gravity.START);
//                    textView.setGravity(Gravity.START);
                }
                convertView = ll;

            } else {
                final TextView textView = (TextView) ((LinearLayout) convertView).getChildAt(0);
                textView.setText(types.get(position));
                textView.setTextColor(position == selectedIndex ? context.getResources().getColor(R.color.pulse_light_violet) : context.getResources().getColor(R.color.black));
                textView.setAlpha(position == selectedIndex ? 0.53f : 1f);
            }
            return convertView;
        }

    }


    private void updateIgnoredList() {
        selectedRoomForMyMomentShare.clear();
        if (settingsModel != null && settingsModel.ignoredList != null) {
            HashMap<String, SettingsModel.MemberDetails> details = settingsModel.ignoredList;
            boolean isValueUpdated = false;
            for (Map.Entry<String, SettingsModel.MemberDetails> entry : details.entrySet()) {
                if (shareText != null && !isValueUpdated) {
                    shareText.setText(getResources().getString(R.string.except_friends_text) + " " + entry.getValue().name);
                    isValueUpdated = true;
                }
                selectedRoomForMyMomentShare.put(entry.getKey(), entry.getValue().roomId);
            }
        }
    }

    private void updateSelectedRoomsList(String customListId) {
        selectedRoomForMyMomentShare.clear();
        if (settingsModel != null && settingsModel.customFriendList != null) {
            if (shareText != null) {
                if (settingsModel.customFriendList.get(customListId).name != null)
                    shareText.setText(settingsModel.customFriendList.get(customListId).name);
                else shareText.setText("");
            }
            HashMap<String, SettingsModel.MemberDetails> details = settingsModel.customFriendList.get(customListId).members;
            for (Map.Entry<String, SettingsModel.MemberDetails> entry : details.entrySet()) {
                selectedRoomForMyMomentShare.put(entry.getKey(), entry.getValue().roomId);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mFireBaseHelper == null || getActivity() == null) return;

        ((CameraActivity) getActivity()).toggleFullScreen(true);
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.newGroupLayout:
                launchNewGroupFragment(AppLibrary.GROUP_ACTION_CREATE);
                break;
            case R.id.shareLayout:
                updateLastUsedPrivacy();
                uploadAndShareMedia();
                break;
//            case R.id.rootPopupView:
//                hideSettingsPopup();
//                break;
//            case R.id.doneButton:
//                saveSettingChanges();
//                break;
        }
    }

    private void updateLastUsedPrivacy() {
        lastUsedPrivacy = privacyPopup.getLastUsedPrivacy();
        if (lastUsedPrivacy != null) {
            mFireBaseHelper.sendLastUsedPrivacyOptionToFireBase(lastUsedPrivacy);
        }
    }


    private void launchNewGroupFragment(int groupAction) {
        Bundle bundle = new Bundle();
        bundle.putInt(AppLibrary.GROUP_ACTION, groupAction);
        CreateManageGroupFragment fragment = new CreateManageGroupFragment();
        rootView.findViewById(R.id.roomListContainer).setVisibility(View.VISIBLE);
        fragment.setArguments(bundle);
        getChildFragmentManager().beginTransaction()
                .add(R.id.roomListContainer, fragment, "newGroupFragment").addToBackStack(null).commitAllowingStateLoss();
    }

    private void uploadAndShareMedia() {
        shareLayout.setVisibility(View.GONE);

        ArrayList<Pair<String, String>> array = new ArrayList<>();
        if (selectedMomentList != null) { //For Analytics
            if (selectedMomentList.containsValue(MY_MOMENT)) {
                array.add(new Pair(AnalyticsEvents.POST_TO_MY_STREAM, mFireBaseHelper.getMyUserModel().name));
                if (privacyPopup != null && privacyPopup.getLastUsedPrivacy() != null)
                    array.add(new Pair(AnalyticsEvents.POST_TO_MY_STREAM_PRIVACY, privacyPopup.getLastUsedPrivacy()));
            }

            if (selectedMomentList.containsValue(CUSTOM_MOMENT)) {

                if (mFireBaseHelper.getMyUserModel().miscellaneous != null && mFireBaseHelper.getMyUserModel().miscellaneous.institutionData != null) {
                    String institutionMomentId = mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId;
                    if (selectedMomentList.containsKey(institutionMomentId)) {
                        array.add(new Pair(AnalyticsEvents.POST_TO_COLLEGE_STREAM, mFireBaseHelper.getMyUserModel().miscellaneous.institutionData.name));
                        for (Map.Entry<String, Integer> entry : selectedMomentList.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value.equals(CUSTOM_MOMENT) && !key.contains(institutionMomentId)) {
                                array.add(new Pair(AnalyticsEvents.POST_TO_PUBLIC_STREAM, AnalyticsEvents.POST_TO_PUBLIC_STREAM));
                                break;
                            }
                        }
                    } else {
                        array.add(new Pair(AnalyticsEvents.POST_TO_PUBLIC_STREAM, AnalyticsEvents.POST_TO_PUBLIC_STREAM));
                    }
                } else {
                    array.add(new Pair(AnalyticsEvents.POST_TO_PUBLIC_STREAM, AnalyticsEvents.POST_TO_PUBLIC_STREAM));
                }

                if (postAnonymous)
                    array.add(new Pair(AnalyticsEvents.POST_TO_PUBLIC_PRIVACY, "Anonymous"));
                else
                    array.add(new Pair(AnalyticsEvents.POST_TO_PUBLIC_PRIVACY, "Public"));
            }
        }
        if (facebookRadio.isChecked())
            array.add(new Pair(AnalyticsEvents.POST_TO_FACEBOOK, "Facebook"));

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.POST_MEDIA, array);

        if (privacyPopup.getCurrentSelectionTag().rowType == PrivacyPopup.FRIENDS_EXCEPT_ROW) {
            updateIgnoredList();
            actionType = PrivacyPopup.FRIENDS_EXCEPT_ROW;
        } else if (privacyPopup.getCurrentSelectionTag().rowType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW) {
            updateSelectedRoomsList(privacyPopup.getCurrentSelectionTag().customListId);
            actionType = PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW;
        } else if (privacyPopup.getCurrentSelectionTag().rowType == PrivacyPopup.ALL_FRIEND_ROW) {
            actionType = PrivacyPopup.ALL_FRIEND_ROW;
        } else if (privacyPopup.getCurrentSelectionTag().rowType == PrivacyPopup.ALL_FRIENDS_AND_FOLLOWERS) {
            actionType = PrivacyPopup.ALL_FRIENDS_AND_FOLLOWERS;
        }
        AppLibrary.log_d(TAG, " Facebook checked -" + facebookRadio.isChecked());
        viewControlsCallback.uploadMediaToFireBase(facebookRadio.isChecked(), postAnonymous, actionType, selectedRoomForMyMomentShare, mediaPath, selectedMomentList, selectedRoomList, expiryType, mediaText, ((CameraActivity) getActivity()).getPublicStreamContribution());
    }

    @Override
    public void onFriendItemClicked(ListRoomView friendItem, boolean isChecked) {

        if (isChecked) {
            if (friendItem.type == FRIEND_ROOM)
                selectedRoomList.put(friendItem.roomId, FRIEND_ROOM);
            else
                selectedRoomList.put(friendItem.roomId, GROUP_ROOM);
        } else {
            selectedRoomList.remove(friendItem.roomId);
        }

        if ((selectedMomentList.size() > 0 || selectedRoomList.size() > 0))
            shareLayout.setVisibility(View.VISIBLE);
        else
            shareLayout.setVisibility(View.GONE);
    }

    @Override
    public void onRecentFriendItemClicked(ListRoomView friendItem, boolean isChecked) {
        if (isChecked) {
            if (friendItem.type == FRIEND_ROOM)
                selectedRoomList.put(friendItem.roomId, FRIEND_ROOM);
            else
                selectedRoomList.put(friendItem.roomId, GROUP_ROOM);
        } else {
            selectedRoomList.remove(friendItem.roomId);
        }

        if ((selectedMomentList.size() > 0 || selectedRoomList.size() > 0))
            shareLayout.setVisibility(View.VISIBLE);
        else
            shareLayout.setVisibility(View.GONE);
    }

//    @Override
//    public void onMomentSelected(String momentId, int source, boolean isSelected) {
//        if (isSelected)
//            selectedMomentList.put(momentId, source);
//        else
//            selectedMomentList.remove(momentId);
//
//        if ((selectedMomentList.size() > 0 || selectedRoomList.size() > 0)) {
//            shareLayout.setVisibility(View.VISIBLE);
//            postFacebookLayout.setClickable(true);
//            postFacebookLayout.setAlpha(1f);
//            postFacebookLayout.setOnClickListener(facebookListener);
//        } else {
//            shareLayout.setVisibility(View.GONE);
//            postFacebookLayout.setClickable(false);
//            postFacebookLayout.setAlpha(0.72f);
//            facebookRadio.setChecked(false);
//            postFacebookLayout.setOnClickListener(null);
//        }
//    }

    @Override
    public void onMomentToggled() {
        if (selectedMomentList != null)
            selectedMomentList.clear();
        else selectedMomentList = new HashMap<>();
        Log.d(TAG, "onMomentToggled: " + selectedMomentList.size());

        {//first doing for my stream in the scope
            if (checkbox.isChecked()) {
                selectedMomentList.put(mFireBaseHelper.getMyUserModel().momentId, MY_MOMENT);
            }
        }

        int publicMomentCount = 0;
        {//then for public moments
            for (Map.Entry<String, CustomMomentModel> entry : CameraActivity.contributableMoments.entrySet()) {
                if (entry.getValue().isChecked) {
                    selectedMomentList.put(entry.getValue().momentId, CUSTOM_MOMENT);
                    ++publicMomentCount;
                }
            }

            Log.d(TAG, "onMomentToggled: " + publicMomentCount + " checked right now");

            if ((selectedMomentList.size() > 0 || selectedRoomList.size() > 0)) {
                shareLayout.setVisibility(View.VISIBLE);
                postFacebookLayout.setClickable(true);
                postFacebookLayout.setAlpha(1f);
                postFacebookLayout.setOnClickListener(facebookListener);
            } else {
                shareLayout.setVisibility(View.GONE);
//                postFacebookLayout.setClickable(false);
                postFacebookLayout.setAlpha(0.72f);
                notifyFacebookTapped(false);
                postFacebookLayout.setOnClickListener(facebookListener);
                facebookRadio.setChecked(false);
            }

        }
    }

    @Override
    public void onSettingsSelected(View view) {
        privacyPopup.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCustomFriendListLoaded(SettingsModel settingsModel) {
        this.settingsModel = settingsModel;
        if (privacyPopup != null)
            privacyPopup.onSettingsDataChanged(settingsModel);
        Log.d(TAG, "setting model " + settingsModel);
    }

    public void updateCurrentActiveView() {

    }

    public int getCurrentActiveView() {
        return currentActiveView;
    }


    @Override
    public void onCreateGroupClicked(ListRoomView listRoomView) {
        recentFriendList.add(0, listRoomView);
        recentRoomListRecyclerView.getAdapter().notifyDataSetChanged();
        roomListRecyclerView.getAdapter().notifyDataSetChanged();
        Fragment fragment = getChildFragmentManager().findFragmentByTag("newGroupFragment");
        if (fragment != null) {
            getChildFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    public interface ViewControlsCallback {
        void onNewGroupButtonClicked();

        void uploadMediaToFireBase(boolean checked, boolean postAnonymous, int action_type,
                                   HashMap<String, String> selectedRoomsForMoment,
                                   String mediaPath,
                                   HashMap<String, Integer> momentList,
                                   HashMap<String, Integer> roomList,
                                   int expiryType,
                                   String mediaText,
                                   @Nullable CustomMomentModel publicStreamContribution
        );
    }
}