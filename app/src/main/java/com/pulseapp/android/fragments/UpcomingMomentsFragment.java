package com.pulseapp.android.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.BlurBuilder;
import com.pulseapp.android.util.RoundedTransformation;
import com.pulseapp.android.util.SmartViewPage;
import com.pulseapp.android.util.ViewPageCloseCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by user on 5/14/2016.
 */
public class UpcomingMomentsFragment extends Fragment implements View.OnClickListener,ViewPageCloseCallback{

    private static final String TAG = "UpcomingMomentsFragment";
    private ViewControlsCallback viewControlsCallback;
    private ListIterator<Map.Entry<String, MomentModel>> momentIterator;
    private View rootView;
    private SmartViewPage viewPage;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback)getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        momentIterator = viewControlsCallback.getMomentListIterator();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.upcoming_moments_fragment,container,false);
        initializeViewObjects(null);
        return rootView;
    }

    public void initializeViewObjects(Bitmap activeBitmap) {
        ImageView backgroundImageView = (ImageView) rootView.findViewById(R.id.backgroundImage);
        backgroundImageView.setDrawingCacheEnabled(true);
        backgroundImageView.setDrawingCacheQuality(ImageView.DRAWING_CACHE_QUALITY_HIGH);
        if (activeBitmap != null){
            Bitmap blurredBitmap = BlurBuilder.blur(getActivity(),activeBitmap);
            backgroundImageView.setImageBitmap(blurredBitmap);
        }


        viewPage = ((SmartViewPage) rootView.findViewById(R.id.touch_frame));
        viewPage.setListener(this);

        momentIterator = viewControlsCallback.getMomentListIterator();
        rootView.findViewById(R.id.firstMomentLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.secondMomentLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.thirdMomentLayout).setVisibility(View.GONE);
        rootView.findViewById(R.id.fourthMomentLayout).setVisibility(View.GONE);
        if (rootView != null) {
            int i = 0;
            while (momentIterator.hasNext()) {
                final MomentModel momentModel = momentIterator.next().getValue();
                i++;
                if (i == 1) {
                    ((TextView) rootView.findViewById(R.id.firstMomentName)).setText(momentModel.name);
                    for (Map.Entry<String, MomentModel.Media> entry : momentModel.medias.entrySet()) {
                        Picasso.with(getActivity()).load(new File(entry.getValue().url)).transform(new RoundedTransformation()).into(((ImageView) rootView.findViewById(R.id.firstMomentImageView)));
                        break;
                    }
                    rootView.findViewById(R.id.firstMomentLayout).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.firstMomentLayout).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewControlsCallback.launchMomentDetailsFragment(momentModel.momentId);
                        }
                    });
                } else if (i == 2) {
                    ((TextView) rootView.findViewById(R.id.secondMomentName)).setText(momentModel.name);
                    for (Map.Entry<String, MomentModel.Media> entry : momentModel.medias.entrySet()) {
                        Picasso.with(getActivity()).load(new File(entry.getValue().url)).transform(new RoundedTransformation()).into(((ImageView) rootView.findViewById(R.id.secondMomentImageView)));
                        break;
                    }
                    rootView.findViewById(R.id.secondMomentLayout).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.secondMomentLayout).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewControlsCallback.launchMomentDetailsFragment(momentModel.momentId);
                        }
                    });
                } else if (i == 3) {
                    ((TextView) rootView.findViewById(R.id.thirdMomentName)).setText(momentModel.name);
                    for (Map.Entry<String, MomentModel.Media> entry : momentModel.medias.entrySet()) {
                        Picasso.with(getActivity()).load(new File(entry.getValue().url)).transform(new RoundedTransformation()).into(((ImageView) rootView.findViewById(R.id.thirdMomentImageView)));
                        break;
                    }
                    rootView.findViewById(R.id.thirdMomentLayout).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.thirdMomentLayout).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewControlsCallback.launchMomentDetailsFragment(momentModel.momentId);
                        }
                    });
                } else if (i == 4) {
                    ((TextView) rootView.findViewById(R.id.fourthMomentName)).setText(momentModel.name);
                    for (Map.Entry<String, MomentModel.Media> entry : momentModel.medias.entrySet()) {
                        Picasso.with(getActivity()).load(new File(entry.getValue().url)).transform(new RoundedTransformation()).into(((ImageView) rootView.findViewById(R.id.fourthMomentImageView)));
                        break;
                    }
                    rootView.findViewById(R.id.fourthMomentLayout).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.fourthMomentLayout).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            viewControlsCallback.launchMomentDetailsFragment(momentModel.momentId);
                        }
                    });
                    break;
                }

            }

            while (i != 0) {
                momentIterator.previous();
                i--;
            }
        }
//        viewControlsCallback.startAutoPlay();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.closeButton:
//                getActivity().finish();
                viewControlsCallback.onCloseMomentsFragment();
                break;
        }
    }

    @Override
    public void onMinimize() {
        viewControlsCallback.onCloseMomentsFragment();
    }

    public interface ViewControlsCallback{
        ListIterator<Map.Entry<String, MomentModel>> getMomentListIterator();

        void launchMomentDetailsFragment(String momentId);

        void startAutoPlay(long time);

        void onCloseMomentsFragment();
    }

}