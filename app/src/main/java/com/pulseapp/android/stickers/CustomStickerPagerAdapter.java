package com.pulseapp.android.stickers;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;


/**
 * Adapter Class for Sliding Pager
 */
public class CustomStickerPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private ArrayList<ChildStickerFragment> fragmentList;

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    public CustomStickerPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    public void setFragmentList(ArrayList<ChildStickerFragment> fragmentList) {
        this.fragmentList = fragmentList;
    }


}