package com.pulseapp.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.FriendsListAdapter;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by indianrenters on 10/13/16.
 */

public class FriendFollowerFragment extends BaseFragment implements FireBaseKEYIDS{

    private RecyclerView mRecyclerView;
    private String currentPage;
    private HashMap<String, SocialModel.Friends> friendsListHashMap;
    private OnDataLoadedCallback onDataLoadedCallback;

    public void updateSocialCount() {
        onDataLoadedCallback.updateSocialCount();
    }

    public interface OnDataLoadedCallback{
        HashMap<String, SocialModel.Friends> getFriendList();
        HashMap<String, SocialModel.Friends> getFollowedList();
        void updateSocialCount();
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onDataLoadedCallback = (OnDataLoadedCallback) getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState!=null) return;

        Bundle bundle = getArguments();
        currentPage = bundle.getString(AppLibrary.CURRENT_PAGE);
        if (currentPage.equals(FRIEND)) {
            friendsListHashMap = onDataLoadedCallback.getFriendList();
        } else {
            friendsListHashMap = onDataLoadedCallback.getFollowedList();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.friend_follower_layout,container,false);

        if (savedInstanceState!=null) return rootView;

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        FriendsListAdapter mFriendsListAdapter = new FriendsListAdapter(this, currentPage, getActivity(), friendsListHashMap, getNormalisedKeys(friendsListHashMap));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mFriendsListAdapter);
        return rootView;
    }

    public List<String> getNormalisedKeys(HashMap<String, SocialModel.Friends> hashMap) {
        hashMap = getSortedHashMap(hashMap);
        ArrayList<String> allKeys = new ArrayList<>(hashMap.keySet());
        List<String> newKeys = new ArrayList<>();
        newKeys.add(0, "header");

        for (int i = 0; i < allKeys.size() - 1; i++) {
            if (hashMap.get(allKeys.get(i)).name.charAt(0) != hashMap.get(allKeys.get(i + 1)).name.charAt(0)) {
                newKeys.add(allKeys.get(i));
                newKeys.add("header");
            } else newKeys.add(allKeys.get(i));
        }
        newKeys.add(allKeys.get(allKeys.size() - 1));
        System.out.println(newKeys);

        return newKeys;
    }

    private HashMap<String, SocialModel.Friends> getSortedHashMap(HashMap<String, SocialModel.Friends> hashMap) {
        List<Map.Entry<String, SocialModel.Friends>> list = new LinkedList<>(hashMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, SocialModel.Friends>>() {
            @Override
            public int compare(Map.Entry<String, SocialModel.Friends> lhs, Map.Entry<String, SocialModel.Friends> rhs) {
                return lhs.getValue().name.compareToIgnoreCase(rhs.getValue().name);
            }
        });

        hashMap = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            hashMap.put(list.get(i).getKey(), list.get(i).getValue());
        }
        return hashMap;
    }
}