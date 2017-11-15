package com.pulseapp.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pulseapp.android.signals.BroadCastSignals;

/**
 * Created by deepankur on 29/4/16.
 */
public class MessageFriendListFragment extends BaseFragment {
    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(android.R.layout.two_line_list_item, container, false);
        return rootView;
    }
}
