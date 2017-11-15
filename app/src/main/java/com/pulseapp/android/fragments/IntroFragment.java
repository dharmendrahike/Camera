package com.pulseapp.android.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.signals.BroadCastSignals;


/**
 * Created by abc on 12/1/2015.
 */
public class IntroFragment extends BaseFragment {

    private int mCurrentPage;
    private String layer;


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        mCurrentPage = data.getInt("current_page", 0);
        layer = data.getString("layer", null);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.intro_fragment_layout, container, false);
        setFonts(rootView);
        if (mCurrentPage == 0) {
            ((RelativeLayout) rootView.findViewById(R.id.background)).setBackgroundColor(Color.parseColor("#e89738"));
//            ((ImageView) rootView.findViewById(R.id.fragment_image)).setImageResource(R.drawable.mobile_1_result);
            ((TextView) rootView.findViewById(R.id.titleText)).setText("DISCOVER");
            ((TextView) rootView.findViewById(R.id.descriptionText)).setText("Discover videos near you and see what's popular on\nInstaLively right now.");
        } else if (mCurrentPage == 1) {
            ((RelativeLayout) rootView.findViewById(R.id.background)).setBackgroundColor(Color.parseColor("#f15c56"));
//            ((ImageView) rootView.findViewById(R.id.fragment_image)).setImageResource(R.drawable.mobile_2_result);
            ((TextView) rootView.findViewById(R.id.titleText)).setText("STREAM");
            ((TextView) rootView.findViewById(R.id.descriptionText)).setText("Go live or record a video with a tap, and share\nyour videos on social media.");
        } else if (mCurrentPage == 2) {
            ((RelativeLayout) rootView.findViewById(R.id.background)).setBackgroundColor(Color.parseColor("#ad62a7"));
//            ((ImageView) rootView.findViewById(R.id.fragment_image)).setImageResource(R.drawable.mobile_3_result);
            ((TextView) rootView.findViewById(R.id.titleText)).setText("INTERACT");
            ((TextView) rootView.findViewById(R.id.descriptionText)).setText("Interact with creators in real time. Follow your\nfavourite ones and re-stream their videos.");
        }


        toggleViews(rootView);
        return rootView;
    }





    private void setFonts(View rootView) {

        Typeface latoBold, latoRegular;
        latoBold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Bold.ttf");
        latoRegular = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Lato-Regular.ttf");
        ((TextView) rootView.findViewById(R.id.titleText)).setTypeface(latoBold);
        ((TextView) rootView.findViewById(R.id.descriptionText)).setTypeface(latoRegular);
    }

    private void toggleViews(View rootView) {

        setViewAndChildrenVisibility(rootView, false);
        toggleVisibility((rootView.findViewById(R.id.rl_intro_login)), true);
        switch (layer) {
            case "mobile":
                toggleVisibility((rootView.findViewById(R.id.fragment_image)), true);
                toggleVisibility((rootView.findViewById(R.id.titleText)), true);
                toggleVisibility((rootView.findViewById(R.id.bottomLayout)), true);
                toggleVisibility((rootView.findViewById(R.id.descriptionText)), true);
                break;
            case "element":
                break;
            case "color":
                toggleVisibility((rootView.findViewById(R.id.background)), true);
                break;
        }

    }

}