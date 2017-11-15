package com.pulseapp.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;


import java.util.ArrayList;

/**
 * Created by deepankur on 6/22/16.
 */
public class GenericViewPagerAdapter extends FragmentStatePagerAdapter {
    ArrayList<Fragment> fragmentList;

    public GenericViewPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragmentList) {
        super(fm);
        this.fragmentList = fragmentList;
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }
    @Override
    public int getItemPosition(Object object){
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
