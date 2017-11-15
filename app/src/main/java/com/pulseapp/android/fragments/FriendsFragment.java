package com.pulseapp.android.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.FriendsListAdapter;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by indianrenters on 8/3/16.
 */
public class FriendsFragment extends BaseFragment implements FriendFollowerFragment.OnDataLoadedCallback{

    private LinearLayout noFriendsLayout;
    private ViewPager viewPager;
    List<FriendFollowerFragment> fragmentList;
    private HashMap<String, SocialModel.Friends> friendsListHashMap;
    public HashMap<String, SocialModel.Friends> followedListHashMap;
    private TabLayout tabLayout;
    private OnDataLoadedCallback onDataLoadedCallback;
    private View actionBar;
    List<String> titleArray = new ArrayList<>();

    public void onFollowedListLoaded(HashMap<String, SocialModel.Friends> followedListHashMap) {
        // provide the adapter with followed details
        setupViews();
    }

    public interface OnDataLoadedCallback{
        HashMap<String, SocialModel.Friends> getFollowedList();
        void updateSocialCount();
    }


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onDataLoadedCallback = (OnDataLoadedCallback) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);

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
        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        tabLayout = (TabLayout) rootView.findViewById(R.id.tabLayout);
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.white));
        tabLayout.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.white));
        noFriendsLayout = (LinearLayout) rootView.findViewById(R.id.no_friends);
        actionBar = rootView.findViewById(R.id.action_bar);
        setupActionBar(actionBar);
        setupViews();
    }

    private void changeTabsFont() {
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = vg.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
            int tabChildsCount = vgTab.getChildCount();
            for (int i = 0; i < tabChildsCount; i++) {
                View tabViewChild = vgTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    fontPicker = FontPicker.getInstance(context);
                    Typeface font = fontPicker.getMontserratRegular();
                    ((TextView) tabViewChild).setTypeface(font, Typeface.NORMAL);
                }
            }
        }
    }

    private void updateSocialCountsInTabs(){
        ViewGroup vg = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = vg.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup vgTab = (ViewGroup) vg.getChildAt(j);
            int tabChildsCount = vgTab.getChildCount();
            for (int i = 0; i < tabChildsCount; i++) {
                View tabViewChild = vgTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    if (((String)((TextView) tabViewChild).getText()).contains("Friends") && friendsListHashMap != null) {
                        ((TextView) tabViewChild).setText(String.valueOf(friendsListHashMap.size()) + " Friends");
                    } else if (((String)((TextView) tabViewChild).getText()).contains("Following") && followedListHashMap != null) {
                        ((TextView) tabViewChild).setText(String.valueOf(followedListHashMap.size()) + " Following");
                    }
                }
            }
        }
    }

    private void setupViews() {
        if(mFireBaseHelper.getSocialModel() != null){
            fragmentList.clear();
            titleArray.clear();
            friendsListHashMap = mFireBaseHelper.getSocialModel().friends;
            if (friendsListHashMap != null) {
                FriendFollowerFragment friendFollowerFragment = new FriendFollowerFragment();
                Bundle bundle = new Bundle();
                bundle.putString(AppLibrary.CURRENT_PAGE,FRIEND);
                friendFollowerFragment.setArguments(bundle);
                fragmentList.add(friendFollowerFragment);
                titleArray.add("Friends");
            }
            if (onDataLoadedCallback.getFollowedList() != null && onDataLoadedCallback.getFollowedList().size() > 0) {
                FriendFollowerFragment friendFollowerFragment = new FriendFollowerFragment();
                Bundle bundle = new Bundle();
                bundle.putString(AppLibrary.CURRENT_PAGE,FOLLOWED);
                friendFollowerFragment.setArguments(bundle);
                fragmentList.add(friendFollowerFragment);
                titleArray.add("Following");
                followedListHashMap = onDataLoadedCallback.getFollowedList();
                if (friendsListHashMap == null)
                    ((TextView) actionBar.findViewById(R.id.titleTV)).setText("FOLLOWING");
            } else {
                if (friendsListHashMap == null) {
                    noFriendsLayout.setVisibility(View.VISIBLE);
                    noFriendsLayout.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadAddFriendFragment();
                        }
                    });
                } else {
                    ((TextView) actionBar.findViewById(R.id.titleTV)).setText("FRIENDS");
                }
            }

            if (fragmentList.size() > 0)
                viewPager.setAdapter(new FriendFollowerAdapter(getChildFragmentManager(), fragmentList));
            if (fragmentList.size() == 2) {
                tabLayout.setupWithViewPager(viewPager);
                ((TextView) actionBar.findViewById(R.id.titleTV)).setText("MY CONNECTIONS");
                changeTabsFont();
                updateSocialCountsInTabs();
                tabLayout.setVisibility(View.VISIBLE);
            } else {
                tabLayout.setVisibility(View.GONE);
            }
        } else {
            noFriendsLayout.setVisibility(View.VISIBLE);
            noFriendsLayout.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadAddFriendFragment();
                }
            });
        }
    }

    private void loadAddFriendFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, new AddFriendFragment(), AddFriendFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(AddFriendFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void setupActionBar(View actionBar) {
        ImageView back = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(Utils.dp2Px(72));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
    }

    @Override
    public HashMap<String, SocialModel.Friends> getFriendList() {
        return friendsListHashMap;
    }

    @Override
    public HashMap<String, SocialModel.Friends> getFollowedList() {
        return followedListHashMap;
    }

    @Override
    public void updateSocialCount() {
        onDataLoadedCallback.updateSocialCount();
        updateSocialCountsInTabs();
    }

    private class FriendFollowerAdapter extends FragmentPagerAdapter{

        List<FriendFollowerFragment> fragmentList;

        private FriendFollowerAdapter(FragmentManager fm, List<FriendFollowerFragment> fragmentList) {
            super(fm);
            this.fragmentList = fragmentList;
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleArray.get(position);
        }
    }

}