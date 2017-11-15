package com.pulseapp.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.BlockedListAdapter;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;

/**
 * Created by indianrenters on 8/1/16.
 */
public class BlockedFragment extends BaseFragment {

    private RecyclerView mRecyclerView;
    private BlockedListAdapter mBlockedListAdapter;
    private HashMap<String, SettingsModel.BlockedUserModel> blockedUsers;
    private SettingsModel mSettingsModel;
    private LinearLayout noBlockFriends;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blocked_friends, container, false);
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
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView_blocked_list);
        mSettingsModel = mFireBaseHelper.getSettingsModel();
        noBlockFriends = (LinearLayout) rootView.findViewById(R.id.ll_no_block_friends);

        setUpActionBar(rootView.findViewById(R.id.action_bar));
        setupRecyclerViewItems();
    }

    private void setupRecyclerViewItems() {
        if (mSettingsModel != null) {
            blockedUsers = mSettingsModel.blockedList;
            if (blockedUsers == null) {
                noBlockFriends.setVisibility(View.VISIBLE);
                return;
            }
            mBlockedListAdapter = new BlockedListAdapter(getActivity(), blockedUsers);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mBlockedListAdapter);
        } else noBlockFriends.setVisibility(View.VISIBLE);
    }


    public void setUpActionBar(View actionBar) {
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
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("BLOCKED");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.setMarginStart(Utils.dp2Px(72));
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
    }

    @Override
    public void onDestroyView() {
//        TODO save or update firebase if needed
        super.onDestroyView();
    }
}
