package com.pulseapp.android.adapters;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pulseapp.android.fragments.CameraFragment;
import com.pulseapp.android.fragments.DashBoardFragment;

/**
 * Created by abc on 3/8/2016.
 */
public class MainFragmentPagerAdapter extends FragmentPagerAdapter {

    private static final int mCount = 2;
    private boolean fromIntent;
    private String action;

    public MainFragmentPagerAdapter(FragmentManager fm, boolean fromIntent,Intent intent) {
        super(fm);
        this.fromIntent = fromIntent;
        if (fromIntent) {
            this.action = intent.getExtras().getString("action");
        }
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position){
            case 0 :
                fragment = new DashBoardFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean("fromIntent",fromIntent);
                if (fromIntent)
                    bundle.putString("action",action);
                fragment.setArguments(bundle);
                break;
            case 1 :
                Bundle newBundle = new Bundle();
                fragment = new CameraFragment();
                newBundle.putBoolean("fromIntent",fromIntent);
                fragment.setArguments(newBundle);
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }
}
