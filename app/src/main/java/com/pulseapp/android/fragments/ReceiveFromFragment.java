package com.pulseapp.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.FriendsExceptionAdapter;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by indianrenters on 7/29/16.
 */
public class ReceiveFromFragment extends BaseFragment {

    private RadioButton radioButtonAllFriends, radioButtonAnyone, radioButtonAllExcept;
    private HashMap<String, SocialModel.Friends> friends;
    private RecyclerView mRecyclerView;
    private FriendsExceptionAdapter mFriendsExceptionAdapter;
    private String receiveFromString = "";

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_receive_from, container, false);
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
        friends = mFireBaseHelper.getSocialModel().friends;
        radioButtonAllFriends = (RadioButton) rootView.findViewById(R.id.rb_all_friends);
        radioButtonAnyone = (RadioButton) rootView.findViewById(R.id.rb_anyone);
        radioButtonAllExcept = (RadioButton) rootView.findViewById(R.id.rb_all_friends_except);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.friends_list);

        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Montserrat-Regular.otf");
        radioButtonAllExcept.setTypeface(font);
        radioButtonAllFriends.setTypeface(font);
        radioButtonAnyone.setTypeface(font);

        setupActionBar(rootView.findViewById(R.id.action_bar));
        setUpViews();
    }

    private void setUpViews() {

        radioButtonAnyone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    receiveFromString = buttonView.getText().toString();
                }
            }
        });

        radioButtonAllFriends.setChecked(true);
        radioButtonAllFriends.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    receiveFromString = buttonView.getText().toString();
                }
            }
        });

        if(friends!=null){
            mFriendsExceptionAdapter = new FriendsExceptionAdapter(getActivity(), friends);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mFriendsExceptionAdapter);

            radioButtonAllExcept.setVisibility(View.VISIBLE);

            radioButtonAllExcept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        receiveFromString = buttonView.getText().toString();
                        showList();
                    } else hideList();
                }
            });
        }

    }

    private void setupActionBar(View actionBar) {
//        actionBar.findViewById(R.id.action_bar_IV_1).setVisibility(View.GONE);
        ImageView back = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        back.setImageResource(R.drawable.back_svg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO sync with firebase
                getActivity().onBackPressed();
            }
        });
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("RECEIVE MESSAGE FROM");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(Utils.dp2Px(72));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();


    }

    public void showList() {
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void hideList() {
        mRecyclerView.setVisibility(View.GONE);
    }


    @Override
    public void onDestroyView() {
        // TODO save changes to firebase
        if(!receiveFromString.isEmpty())
            ((CameraActivity)getActivity()).updateReceiveFromOnSettingsFragment(receiveFromString);
        super.onDestroyView();
    }
}
