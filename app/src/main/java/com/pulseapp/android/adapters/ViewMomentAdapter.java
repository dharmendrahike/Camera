package com.pulseapp.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pulseapp.android.fragments.UpcomingMomentsFragment;
import com.pulseapp.android.fragments.ViewMomentDetailsFragment;

/**
 * Created by user on 5/12/2016.
 */
public class ViewMomentAdapter extends FragmentPagerAdapter{

    private int mCount = 2;
    private boolean isNearby;

    public void setIsNearby(boolean nearby) {
        isNearby = nearby;
    }

    public ViewMomentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position){
            case 0:
                fragment = new ViewMomentDetailsFragment();
                ((ViewMomentDetailsFragment) fragment).setIsNearBy(isNearby);
                break;
            case 1:
                fragment = new UpcomingMomentsFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }
}