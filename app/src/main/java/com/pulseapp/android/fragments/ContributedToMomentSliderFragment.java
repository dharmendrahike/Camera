package com.pulseapp.android.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.SliderMyMomentAdapter;
import com.pulseapp.android.modelView.MediaModelView;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.LinkedHashMap;

/**
 * Created by deepankur on 26/4/16.
 */
public class ContributedToMomentSliderFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private LinkedHashMap<String, MediaModelView> mediaArrayList;
    private String TAG = this.getClass().getSimpleName();
    private View tint_1_View, tint_2_View;
    RelativeLayout headerOverLayView;
    private String momentId;

    /**
     * @param momentId indicated the moment for which the fragment is being open
     */
    public void setMomentId(String momentId) {
        this.momentId = momentId;
        if (myMomentAdapter != null)
            myMomentAdapter.setMomentId(momentId);
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState!=null) return;

        mediaArrayList = new LinkedHashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.contributed_media_overlay, container, false);

        if (savedInstanceState!=null) return rootView;

        initRecyclerView(rootView);
        initAnimationViews(rootView);
        return rootView;
    }

    /**
     * @param mediaList Set this only while populating / redrawing the list from
     *                  {@link DashBoardFragment#onContributionDataLoaded}
     */
    public void setMediaList(LinkedHashMap<String, MediaModelView> mediaList) {
        this.mediaArrayList = mediaList;
        if (myMomentAdapter != null) {
            myMomentAdapter.setMediaList(mediaArrayList);
            myMomentAdapter.notifyDataSetChanged();
        }
    }

    ImageView currentIndicator;

    public void setCurrentIndicator(ImageView currentIndicator) {
        this.currentIndicator = currentIndicator;
    }

    public void setUpperTintMargins(int upperTintMargin, int headerHeight) {
        if (adjustForShadows) {
            tint_1_View.getLayoutParams().height = upperTintMargin + convertDpToPixels(0);
            tint_1_View.requestLayout();
            headerOverLayView.getLayoutParams().height = convertDpToPixels(56);
        } else {
            tint_1_View.getLayoutParams().height = upperTintMargin;
            tint_1_View.requestLayout();
            headerOverLayView.getLayoutParams().height = headerHeight;
        }

    }

    boolean adjustForShadows = true;
    private SliderMyMomentAdapter myMomentAdapter;

    private void initRecyclerView(View rootView) {
        myMomentAdapter = new SliderMyMomentAdapter(getActivity(), mediaArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = (RecyclerView) rootView.findViewById(R.id.mediasRecycler);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(myMomentAdapter);
        myMomentAdapter.notifyDataSetChanged();
    }

//    public void updateMediaStatus(String mediaId, int uploadStatus) {
//        if (mediaArrayList.containsKey(mediaId)) {
//            mediaArrayList.get(mediaId).status = uploadStatus;
//        }
//        myMomentAdapter.setMediaList(mediaArrayList);
//        myMomentAdapter.notifyDataSetChanged();
//    }

    public void onDownloadStatusChanges(String momentId, String mediaId, int status) {
        if (mediaArrayList.containsKey(mediaId)) {
            mediaArrayList.get(mediaId).status = status;
        }
        myMomentAdapter.setMediaList(mediaArrayList);
        myMomentAdapter.notifyDataSetChanged();
    }

    private final int SLIDER_ANIM_DURATION = 200;
    final int displacement = 1000;

    boolean mIsAnimating;
    View headerView1, headerView2;


    private void initAnimationViews(View rootView) {
        tint_1_View = rootView.findViewById(R.id.tint1View);
        tint_2_View = rootView.findViewById(R.id.tint2View);
        headerOverLayView = (RelativeLayout) rootView.findViewById(R.id.headerRL);

        headerView1 = rootView.findViewById(R.id.header_View1);
        headerView2 = rootView.findViewById(R.id.header_View2);


        headerView1.getLayoutParams().width = AppLibrary.convertDpToPixels(context, 16);
        headerView2.getLayoutParams().width = AppLibrary.convertDpToPixels(context, 16);

        tint_1_View.setOnTouchListener(overlayTouchListener);
        tint_2_View.setOnTouchListener(overlayTouchListener);
        headerOverLayView.setOnTouchListener(overlayTouchListener);
    }

    protected enum AnimationDirection {SLIDE_UP, SLIDE_DOWN}

    public void animateRecycler(final AnimationDirection direction) {

        if (mIsAnimating) {
            Log.e(TAG, "animating under progress; ignoring");
            return;
        }

        mIsAnimating = true;
        float[] slide_from_to;
        float[] alpha_from_to;
        if (direction == AnimationDirection.SLIDE_UP) {
            slide_from_to = new float[]{0f, -displacement};
            alpha_from_to = new float[]{1f, 0f};
        } else {
            slide_from_to = new float[]{-displacement, 0f};
            alpha_from_to = new float[]{0f, 1f};
        }

        tint_1_View.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        tint_2_View.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        headerView1.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        headerView2.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        recyclerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        ObjectAnimator view1alpha = ObjectAnimator.ofFloat(tint_1_View, "alpha", alpha_from_to[0], alpha_from_to[1]).setDuration(SLIDER_ANIM_DURATION);
        ObjectAnimator view2alpha = ObjectAnimator.ofFloat(tint_2_View, "alpha", alpha_from_to[0], alpha_from_to[1]).setDuration(SLIDER_ANIM_DURATION);

        ObjectAnimator header1alpha = ObjectAnimator.ofFloat(headerView1, "alpha", alpha_from_to[0], alpha_from_to[1]).setDuration(SLIDER_ANIM_DURATION);
        ObjectAnimator header2alpha = ObjectAnimator.ofFloat(headerView2, "alpha", alpha_from_to[0], alpha_from_to[1]).setDuration(SLIDER_ANIM_DURATION);

        ObjectAnimator animator = ObjectAnimator.ofFloat(recyclerView, "translationY", slide_from_to[0], slide_from_to[1]).setDuration(SLIDER_ANIM_DURATION);
        if (direction == AnimationDirection.SLIDE_UP) {
            ObjectAnimator.ofFloat(currentIndicator, "scaleY", 1f, -1f).setDuration(SLIDER_ANIM_DURATION).start();

            ((View) currentIndicator.getTag()).findViewById(R.id.streamStatsLL).setVisibility(View.GONE);
            ((View) currentIndicator.getTag()).findViewById(R.id.myStreamsText).setVisibility(View.VISIBLE);
        }

        view1alpha.start();
        view2alpha.start();

        header1alpha.start();
        header2alpha.start();

        animator.start();

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
                if (actionListener != null && direction == AnimationDirection.SLIDE_UP)
                    actionListener.onCloseMomentsFragment();

                tint_1_View.setLayerType(View.LAYER_TYPE_NONE, null);
                tint_2_View.setLayerType(View.LAYER_TYPE_NONE, null);
                headerView1.setLayerType(View.LAYER_TYPE_NONE, null);
                headerView2.setLayerType(View.LAYER_TYPE_NONE, null);
                recyclerView.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
                if (actionListener != null && direction == AnimationDirection.SLIDE_UP)
                    actionListener.onCloseMomentsFragment();

                tint_1_View.setLayerType(View.LAYER_TYPE_NONE, null);
                tint_2_View.setLayerType(View.LAYER_TYPE_NONE, null);
                headerView1.setLayerType(View.LAYER_TYPE_NONE, null);
                headerView2.setLayerType(View.LAYER_TYPE_NONE, null);
                recyclerView.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    MomentSliderActionListener actionListener;

    public void setActionListener(MomentSliderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public interface MomentSliderActionListener {

        void onCloseMomentsFragment();
    }

    final int TINT_OVERLAY = 11, HEADER_OVERLAY = 22;
    private View.OnTouchListener overlayTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP)
                animateRecycler(AnimationDirection.SLIDE_UP);
            return true;
        }
    };
}