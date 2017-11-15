package com.pulseapp.android.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.AddFriendFromFBAdapter;
import com.pulseapp.android.adapters.FriendRequestAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.floatingbutton.FloatingActionButton;
import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by deepankur on 16/4/16.
 */
public class AddFriendFragment extends BaseFragment {
    private ArrayList<FaceBookFriendsModel> faceBookFriendsModels;
    private RecyclerView mFriendRequestRecycler, mAddFriendRecycler;
    private TextView viewMoreRequestTV, viewMoreFriendsTV;
    private View groupRequestFrame;
    private View rootView;
    //    private FloatingActionButton searchFab;
    private String TAG = getClass().getSimpleName();
    View addByUserName;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    void toggleSearchMode(boolean enterSearchMode) {
        rootView.findViewById(R.id.scrollView).setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);

        rootView.findViewById(R.id.searchFrame_addFriend).setVisibility(enterSearchMode ? View.VISIBLE : View.GONE);
        searchView.setVisibility(enterSearchMode ? View.VISIBLE : View.GONE);

        rootView.findViewById(R.id.titleTV).setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            return;

        faceBookFriendsModels = mFireBaseHelper.getSuggestedFriends();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_friend, container, false);

        if (mFireBaseHelper == null || savedInstanceState != null) return rootView;

        if (mFireBaseHelper.getSocialModel()==null) return rootView;

        rootView.findViewById(R.id.addFriendsRL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.INVITE_FRIENDS);

                if (AppLibrary.isPackageInstalled("com.facebook.orca", context))
                    startMessenger();
                else
                    showShortToastMessage("Please install Facebook Messenger to invite friends");
            }
        });
        searchFrame = (FrameLayout) rootView.findViewById(R.id.searchFrame_addFriend);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //else dashboard consumes touch and open
                //this fragment again due to touch
                return true;
            }
        });
        addByUserName = rootView.findViewById(R.id.addByUserName);
        addByUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchView == null)
                    initSearchView(rootView);
                toggleSearchMode(true);
                searchView.onActionViewExpanded();
                searchView.callOnClick();
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.USER_SEARCH);
            }
        });

        if (faceBookFriendsModels == null || faceBookFriendsModels.size() == 0)
            rootView.findViewById(R.id.addFromFacebookLL).setVisibility(View.GONE);
//        searchLayout = (LinearLayout) rootView.findViewById(R.id.searchLL);
        initActionBar(rootView.findViewById(R.id.action_bar));
        rootView.findViewById(R.id.shareUserName).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchShareIntent();
            }
        });
        loadGroupRequestFragment();
        mFriendRequestRecycler = (RecyclerView) rootView.findViewById(R.id.friend_request_recycler);
        mFriendRequestRecycler.setNestedScrollingEnabled(false);
        initFacebookRecycler(rootView);
        initFriendRequestRecycler(rootView);
        viewMoreRequestTV = (TextView) rootView.findViewById(R.id.view_more_requestTV);
        viewMoreFriendsTV = (TextView) rootView.findViewById(R.id.viewMoreFriendsTV);
        groupRequestFrame = rootView.findViewById(R.id.groupRequestFrame);
        return rootView;
    }

    private SearchView searchView;
    private FrameLayout searchFrame;
    private SearchFragment searchFragment;

    private void initSearchView(final View rootView) {
        searchFrame = (FrameLayout) rootView.findViewById(R.id.searchFrame_addFriend);
        searchFrame.setVisibility(View.VISIBLE);
        searchView = (SearchView) rootView.findViewById(R.id.action_bar).findViewById(R.id.action_bar_search_view);
        searchView.setVisibility(View.VISIBLE);
        if (searchFragment == null)
            searchFragment = new SearchFragment();
        searchFragment.searchSource = SearchFragment.SearchSource.ADD_FRIEND;
        searchFragment.initSearchView(rootView);
        searchFragment.registerSearchView(searchView);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.searchFrame_addFriend, searchFragment, SearchFragment.class.getSimpleName() + " add friend");
//                fragmentTransaction.addToBackStack(SearchFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, " onClose ");
                searchView.onActionViewCollapsed();
                toggleSearchMode(false);
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    private void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.VISIBLE);
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);

        ((ImageView) actionBar.findViewById(R.id.action_bar_IV_1)).setImageResource(R.drawable.back_svg);
        (actionBar.findViewById(R.id.action_bar_IV_1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchFrame.getVisibility() == View.VISIBLE)
                    toggleSearchMode(false);
                else
                    getActivity().onBackPressed();
            }
        });

        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("ADD FRIENDS");
        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
        ((RelativeLayout.LayoutParams) searchFrame.getLayoutParams()).topMargin = (AppLibrary.convertDpToPixels(context, 56) + AppLibrary.getStatusBarHeight(getActivity()));
        titleTv = ((TextView) actionBar.findViewById(R.id.titleTV));
    }

    private TextView titleTv;

    private void launchShareIntent() {
        if (mFireBaseHelper.getMyUserModel()==null) return;

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SHARE_MY_USERNAME,AnalyticsEvents.USER_HANDLE,mFireBaseHelper.getMyUserModel().handle);

        String shareBody = "Add Me on Pulse -\nhttps://mypulse.tv/add/" + mFireBaseHelper.getMyUserModel().handle;
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Add Me on Pulse");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
    }

    private AddFriendFromFBAdapter addFriendFromFBAdapter;

    private void initFacebookRecycler(final View rootView) {
        if (addFriendFromFBAdapter == null)
            addFriendFromFBAdapter = new AddFriendFromFBAdapter(faceBookFriendsModels, getActivity(), new RecyclerViewClickInterface() {
                @Override
                public void onItemClick(int pendingSizeOfDataStructure, Object data) {
                    if (pendingSizeOfDataStructure == 0)
                        rootView.findViewById(R.id.addFromFacebookLL).setVisibility(View.GONE);
                }
            });
        mAddFriendRecycler = (RecyclerView) rootView.findViewById(R.id.add_from_facebook_recycler);
        mAddFriendRecycler.setNestedScrollingEnabled(false);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mAddFriendRecycler.setLayoutManager(mLayoutManager);
        mAddFriendRecycler.setAdapter(addFriendFromFBAdapter);
    }

    private FriendRequestAdapter adapter;

    private void initFriendRequestRecycler(final View rootView) {
        mFriendRequestRecycler = (RecyclerView) rootView.findViewById(R.id.friend_request_recycler);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mFriendRequestRecycler.setLayoutManager(mLayoutManager);
        final HashMap<String, SocialModel.RequestReceived> friendRequestMap = mFireBaseHelper.getPendingFriendRequests();
        if (friendRequestMap == null || friendRequestMap.size() == 0) {
            rootView.findViewById(R.id.friendRequestHeaderTV).setVisibility(View.GONE);
            rootView.findViewById(R.id.nofriendRequestHeader).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.friendRequestHeaderTV).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.nofriendRequestHeader).setVisibility(View.GONE);
            ((TextView) rootView.findViewById(R.id.friendRequestHeaderTV)).setText("FRIEND REQUESTS "  + "[" + friendRequestMap.size()+ "]");
        }
        if (friendRequestMap == null) return;
        adapter = new FriendRequestAdapter((HashMap<String, SocialModel.RequestReceived>) friendRequestMap.clone(), getActivity(), new RecyclerViewClickInterface() {
            @Override
            public void onItemClick(int sizeOfTheDataStructure, Object optionalInfo) {
                adapter.refreshData();
                addFriendFromFBAdapter.notifyDataSetChanged();
                if (sizeOfTheDataStructure == 0) {
                    rootView.findViewById(R.id.friendRequestHeaderTV).setVisibility(View.GONE);
                    rootView.findViewById(R.id.nofriendRequestHeader).setVisibility(View.VISIBLE);
                } else {
                    rootView.findViewById(R.id.friendRequestHeaderTV).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.nofriendRequestHeader).setVisibility(View.GONE);
                    ((TextView) rootView.findViewById(R.id.friendRequestHeaderTV)).setText("FRIEND REQUESTS " + friendRequestMap.size() + "[" + "]");

                }
            }
        });
        mFriendRequestRecycler.setAdapter(adapter);
    }


    private GroupRequestFragment groupRequestFragment;

    public void loadGroupRequestFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.groupRequestFrame, new GroupRequestFragment(), "GroupRequestFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    CallbackManager callbackManager;
    ShareDialog shareDialog;

    void initContent() {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://developers.facebook.com"))
                .build();
    }

    public void startMessenger() {
        MessageDialog dialog = new MessageDialog(getActivity());
        dialog.registerCallback(MasterClass.callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
//                if (autoForward)
//                    ((OnBoardingActivity) getActivity()).onFriendsSelectionDone();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException e) {
                e.printStackTrace();
            }
        });

        if (MessageDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentTitle("Add Me on Pulse")
                    .setContentDescription("Pulse - Live in the moment")
                    .setContentUrl(Uri.parse("https://mypulse.tv/add/" + FireBaseHelper.getInstance(getActivity()).getMyUserModel().handle))
                    .build();
            dialog.show(linkContent);
        }
    }

    public void refreshSocialData(HashMap<String,SocialModel.RequestReceived> requests) {
        // refresh social data changes
        adapter = null;
        initFriendRequestRecycler(rootView);
    }

    public void refreshFacebookFriendsSuggestions() {
        addFriendFromFBAdapter = null;
        initFacebookRecycler(rootView);
    }
}
