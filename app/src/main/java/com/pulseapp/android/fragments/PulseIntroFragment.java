package com.pulseapp.android.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.FacebookLogin;
import com.pulseapp.android.util.FontPicker;

/**
 * Created by deepankur on 6/22/16.
 */
public class PulseIntroFragment extends Fragment {
    View rootView;
    int layoutId;
    int pageNo;
    String intro, subIntro;
    private String TAG = getClass().getSimpleName();
    FontPicker fontPicker;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        fontPicker = FontPicker.getInstance(getActivity());
        pageNo = data.getInt("page_no");
        if (pageNo == 0 || pageNo == 4)
            layoutId = R.layout.fragment_facebook_login;
        else {
            intro = data.getString("intro");
            subIntro = data.getString("subIntro");
            layoutId = R.layout.fragment_pulse_intro;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(layoutId, container, false);

        if (pageNo == 0 || pageNo == 4) {
            rootView.findViewById(R.id.slideTV).setVisibility(pageNo == 0 ? View.VISIBLE : View.GONE);
            rootView.findViewById(R.id.slideIV).setVisibility(pageNo == 0 ? View.VISIBLE : View.GONE);
            rootView.findViewById(R.id.facebookButtonLL).setVisibility(pageNo == 0 ? View.GONE : View.VISIBLE);
            rootView.findViewById(R.id.skipTV).setVisibility(pageNo == 4 ? View.GONE : View.VISIBLE);
        } else {

            if (pageNo == 1)
                rootView.setBackgroundResource(R.drawable.one);
            if (pageNo == 2)
                rootView.setBackgroundResource(R.drawable.two);
            if (pageNo == 3)
                rootView.setBackgroundResource(R.drawable.three);

            ((TextView) rootView.findViewById(R.id.descriptionTV)).setText(intro);
            ((TextView) rootView.findViewById(R.id.subDescriptionTV)).setText(subIntro);
        }
        if (pageNo == 4)
            rootView.findViewById(R.id.facebookLogin).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rootView.findViewById(R.id.facebookLogin).setClickable(false);
                    ((FacebookLogin) getActivity()).onLoginButtonClicked();
                }
            });
        setFonts(rootView);
        rootView.findViewById(R.id.skipTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FacebookLogin) getActivity()).onSkipClicker();
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    Typeface Museo700, Museo500, MontserratRegular, MontserratSemiBold;

    private void setFonts(View rootView) {
        Museo700 = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Museo700-Regular.otf");
        Museo500 = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Museo500-Regular.otf");
        MontserratRegular = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Montserrat-Regular.otf");
        MontserratSemiBold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Montserrat-SemiBold.otf");
        if (pageNo == 0 || pageNo == 4) {
            ((TextView) rootView.findViewById(R.id.pulseTV)).setTypeface(Museo700);
            ((TextView) rootView.findViewById(R.id.subheaderTV)).setTypeface(MontserratRegular);

            ((TextView) rootView.findViewById(R.id.slideTV)).setTypeface(MontserratRegular);

            ((TextView) rootView.findViewById(R.id.facebookBTN)).setTypeface(MontserratSemiBold);
            ((TextView) rootView.findViewById(R.id.neverPostTV)).setTypeface(Museo500);

        } else {
            ((TextView) rootView.findViewById(R.id.descriptionTV)).setTypeface(Museo700);
            ((TextView) rootView.findViewById(R.id.subDescriptionTV)).setTypeface(Museo500);
        }
        ((TextView) rootView.findViewById(R.id.skipTV)).setTypeface(fontPicker.getMontserratRegular());
    }

    //only for last fragment in the list
    public void enableButtonOnLoginFailed() {
        if (rootView != null) {
            Log.d(TAG, "page no " + pageNo);
            rootView.findViewById(R.id.facebookLogin).setClickable(true);
        }
    }
}
