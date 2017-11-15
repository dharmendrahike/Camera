package com.pulseapp.android.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.pulseapp.android.fragments.IntroFragment;

/**
 * Created by abc on 12/1/2015.
 */
public class IntroPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    String layer = null;


    public IntroPagerAdapter(FragmentManager fm, String layer) {
        super(fm);
        this.layer = layer;
    }

    @Override
    public Fragment getItem(int arg0) {
        if (layer.contains("element"))
            arg0 = getInvertedPostion(arg0);

        IntroFragment myFragment = new IntroFragment();
        Bundle data = new Bundle();
        data.putInt("current_page", arg0);
        data.putString("layer", layer);
        myFragment.setArguments(data);
        return myFragment;
    }

    private int getInvertedPostion(int arg0) {
        int resultingPosition = 0;
        switch (arg0) {
            case 0:
                resultingPosition = 2;
                break;
            case 1:
                resultingPosition = 1;
                break;
            case 2:
                resultingPosition = 0;
                break;
        }

        return resultingPosition;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

}
