package com.pulseapp.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.pulseapp.android.fragments.ChildEmojiPagerFragment;

import java.util.ArrayList;

/**
 * Created by deepankur on 21/3/16.
 */

public class EmojiPagerAdapter extends FragmentPagerAdapter {
    private final String TAG = getClass().getSimpleName();
    private ArrayList<ChildEmojiPagerFragment> fragmentList;

    public EmojiPagerAdapter(FragmentManager fm, ArrayList<ChildEmojiPagerFragment> fragmentList) {
        super(fm);
        this.fragmentList = fragmentList;
        Log.d(TAG, "EmojiPagerAdapter: instantiated");
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }


    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
