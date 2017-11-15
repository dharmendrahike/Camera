package com.pulseapp.android.broadcast;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.fragments.CameraFragment;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by admin on 1/27/2015.
 */
public class FiltersFragment extends Fragment {

    private View rootView;
    private FrameLayout layersContainer;
    private RelativeLayout buttonsContainer;
    private LayoutInflater mInflater;
    private Activity mContext;
    private FilterManager mFilter;
    private int width;

    //  buttons
    private View currentShowingButton;
    private View currentShowingView;
    private int highlightColor = R.color.hightlight_color;
    private int unHighlightColor = R.color.white;

    //  filters
    private int filterHighlightColor = R.color.instalively_color_primary;

    boolean[] mEnabled = null;
    TextView[] mFilterTextViews = null;
    ImageView[] mFilterImageViews = null;
    View[] mFilterUnderlineViews = null;
    private View.OnClickListener mFilterButtonClickListener;
    private View.OnTouchListener mFilterTouchListener;

    boolean [] mAnimDone = null;
    private Object[] mFilterNames = null;
    PreviewModeChangeListener mPreviewModeChangeListener;

    public static int filterCount = FilterManager.FILTER_COUNT;
    private LinearLayout[] mFilterLayouts;
    private float smallScale = 1.0f;
    private float bigScale = 1.15f;
    private static int mCurrentFilter = FilterManager.FILTER_NATURAL;
    private static String TAG = "FiltersFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.mInflater = inflater;
        rootView = inflater.inflate(R.layout.ffilters_layout, container, false);
        return rootView;
    }

    public void makeVisible(boolean visible) {
        rootView.setVisibility((visible ? View.VISIBLE : View.GONE));
        for(int i = 0; i < filterCount; i++) {
            mFilterLayouts[i].setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void renderFilterThumbnails() {
//        for(int i = 0; i < filterCount; i++) {
            mPreviewModeChangeListener.triggerFilterThumbnail(0, filterCount);
//        }
    }

    void stopFilterThumbnails() {
        mPreviewModeChangeListener.stopFilterThumbnail();
    }

    void createFiltersLayout() {
        Log.d(TAG, "Create filters layout");
//        rootView.setOnClickListener(mFilterButtonClickListener);
        LinearLayout filtersContainer = (LinearLayout) rootView.findViewById(R.id.filters_container);
//        LayoutInflater inflater = LayoutInflater.from(rootView.getContext());

        int statePressed = android.R.attr.state_selected;
        int[] statesPressed = new int[]{statePressed};
        int[] statesNormal = new int[]{-statePressed};

        for (int i = 0; i < filterCount; i++) {
            LinearLayout filterBox = (LinearLayout) mInflater.inflate(R.layout.single_filter_layout, filtersContainer, false);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) filterBox.getLayoutParams();
            params.weight = 1.0f;

            TextView filterNameView = (TextView) filterBox.findViewById(R.id.filter_name);
            filterNameView.setText((String) mFilterNames[i]);
            mFilterTextViews[i] = filterNameView;

            ImageView filterIconView = (ImageView) filterBox.findViewById(R.id.filter_icon);
            StateListDrawable drawable = new StateListDrawable();

            filterIconView.setImageDrawable(drawable);

            mFilterImageViews[i] = filterIconView;

            View filterUnderlineView = filterBox.findViewById(R.id.filter_underline);
            filterUnderlineView.setVisibility(View.INVISIBLE);

            mFilterUnderlineViews[i] = filterUnderlineView;

            filtersContainer.addView(filterBox);
            filterBox.setOnClickListener(mFilterButtonClickListener);
            mFilterLayouts[i] = filterBox;

        }
        makeVisible(false);

        if(mFilterImageViews!=null && mFilterImageViews.length>0) {
//            renderFilterThumbnails();
        }
        else
            CameraActivity.PreRecordFilters = false;
    }

    public void setFilterThumbnail(Bitmap bitmap, int filter) {
        Log.d(TAG, "setting filter thumbnail for the view");
        if(isAdded() && mFilterImageViews != null) {
            ImageView filterView = mFilterImageViews[filter];
            if (filterView != null) {
                filterView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                if(mCurrentFilter == filter) {
                    enableFilterViews(mCurrentFilter);
                }
            } else {
                renderFilterThumbnails();
            }
        } else {

        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = getActivity();
        mPreviewModeChangeListener = (CameraFragment)getParentFragment();
//        mPreviewModeChangeListener = (PreviewModeChangeListener) getActivity();
        width = this.mContext.getWindowManager().getDefaultDisplay().getWidth();
        highlightColor = mContext.getResources().getColor(R.color.hightlight_color);
        unHighlightColor = mContext.getResources().getColor(R.color.white);
        filterHighlightColor = mContext.getResources().getColor(R.color.filter_highlight_color);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        buttonsContainer.setPadding(width/10, 0, width/10, 0);
//        ((LinearLayout.LayoutParams)rootView.findViewById(R.id.filters_separator).getLayoutParams()).setMargins(width / 10, 0, width / 10, 0);
        Bundle saved = getArguments();
        mFilter = (FilterManager) saved.getSerializable("filter_instance");
        mFilterNames = (Object[]) mFilter.getAvailColorEffects().toArray();
        filterCount = mFilterNames.length;

        if(filterCount==0)
        {
            CameraActivity.PreRecordFilters = false;
            return;
        }

        if(mEnabled == null) {
            mEnabled = new boolean[filterCount];
//            mFilterButtons = new ImageView[filterCount];
            mFilterTextViews = new TextView[filterCount];
            mFilterImageViews = new ImageView[filterCount];
            mFilterUnderlineViews = new View[filterCount];
            mFilterLayouts = new LinearLayout[filterCount];
            mAnimDone = new boolean[filterCount];
            for(int i = 0; i < filterCount; i++) {
                mAnimDone[i] = true;
            }
        }
        setFListeners();
        mCurrentFilter = FilterManager.FILTER_NATURAL;
        createFiltersLayout();
        enableFilterViews(mCurrentFilter);
        //loadImages();
    }

    private final String ANTI_BONDING = "anti_bonding";
    private final String WHITE_BALANCE = "white_balance";
    private final String COLOR_EFFECT = "color_effect";
    private final String SCENE_MODE = "scene_mode";
    private final String FOCUS_MODE = "focus_mode";
    private final String FLASH_MODE = "flash_mode";

    private void loadImages() {
        int imageParam = width / 12;
        int filterGap = 3 * width / 80;

        ArrayList<String> params = new ArrayList<>();
        LinearLayout filterContainer = null;
        String filterType = null;
        String presentFilter = null;
        Random mRandom = new Random();

        params = mFilter.getAvailColorEffects();
        filterContainer = (LinearLayout) layersContainer.findViewById(R.id.color_effect_container);
        filterType = COLOR_EFFECT;
        presentFilter = mFilter.getColorEffect();
    }

    public void changeViewMode(boolean showZoomMode) {
        /*if (showZoomMode) {

            AppLibrary.log_e("ChangeViewMode", "currentShowingView true: " + R.id.zoom_container);
            if (currentShowingView != null && currentShowingView.getId() == R.id.zoom_container) {
                return;
            }
            buttonsContainer.setVisibility(View.GONE);
            rootView.findViewById(R.id.filters_separator).setVisibility(View.GONE);

            View container = layersContainer.findViewById(R.id.zoom_container);
            container.setVisibility(View.VISIBLE);
            currentShowingView.setVisibility(View.INVISIBLE);
            currentShowingView = container;

            View v = buttonsContainer.findViewById(R.id.zoom);
            ((TextView)v).setTextColor(highlightColor);
            ((TextView) currentShowingButton).setTextColor(unHighlightColor);
            currentShowingButton = v;

        } else */
        {
            AppLibrary.log_e("ChangeViewMode", "currentShowingView false: " + currentShowingView.getId());
            if (currentShowingView != null && currentShowingView.getId() != R.id.zoom_container) {
                return;
            }

            buttonsContainer.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.filters_separator).setVisibility(View.VISIBLE);

            View container = layersContainer.findViewById(R.id.ce_container);
            container.setVisibility(View.VISIBLE);
            currentShowingView.setVisibility(View.INVISIBLE);
            currentShowingView = container;

            View v = buttonsContainer.findViewById(R.id.color_effects);
            ((TextView) v).setTextColor(highlightColor);
            ((TextView) currentShowingButton).setTextColor(unHighlightColor);
            currentShowingButton = v;
        }
    }

    boolean animDone = false;

    private void setFListeners() {
        mFilterButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int j = -1;
                for (int i = 0; i < filterCount; i++) {
                    if (v.equals(mFilterLayouts[i]) || v.equals(mFilterImageViews[i]) || v.equals(mFilterTextViews[i])) {
                        int viewId = v.getId();
                        j = i;
                        break;
                    }
                }
                //Do nothing, if the button is already selected
                if (j < 0 || mEnabled[j] == true) {
                    return;
                }
                mCurrentFilter = j;
                mPreviewModeChangeListener.setFilter(mCurrentFilter, true);
                enableFilterViews(mCurrentFilter);
            }
        };

        mFilterTouchListener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
            Log.d("FilterFragment", "On Touch received for: " + v);
                int j = -1;
                for (int i = 0; i < filterCount; i++) {
                    if (v.equals(mFilterLayouts[i]) || v.equals(mFilterImageViews[i]) || v.equals(mFilterTextViews[i])) {
                        int viewId = v.getId();
                        j = i;
                        break;
                    }
                }
                //Do nothing, if the button is already selected
                if (j < 0 || mEnabled[j] == true) {
                    return false;
                }
                if(mCurrentFilter != j) {
                    mCurrentFilter = j;
                    mPreviewModeChangeListener.setFilter(mCurrentFilter, true);
                    enableFilterViews(mCurrentFilter);
                }
                return true;
            }
        };
    }

    private  void enableFilterViews(int j) {
        for (int i = 0; i < filterCount; i++) {
            if (i != j) {
                mEnabled[i] = false;
                mFilterImageViews[i].setSelected(mEnabled[i]);

                if(mAnimDone[i] == true) {
                    ScaleAnimation anim = new ScaleAnimation(bigScale, smallScale, bigScale, smallScale, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                    anim.setDuration(150);
                    anim.setFillAfter(true);

                    mFilterImageViews[i].startAnimation(anim);
                    mFilterTextViews[i].startAnimation(anim);
                    mFilterImageViews[i].setPivotX(0);
                    mFilterImageViews[i].setPivotY(0);

                    mFilterTextViews[i].setTextColor(Color.LTGRAY);

                    mFilterUnderlineViews[i].setVisibility(View.INVISIBLE);
                    mAnimDone[i] = false;
                }
            } else {
                mEnabled[i] = true;
                mFilterImageViews[i].setSelected(mEnabled[i]);
                mFilterImageViews[i].setPivotX(0);
                mFilterImageViews[i].setPivotY(0);

                mFilterTextViews[i].setTextColor(Color.WHITE);

                mFilterUnderlineViews[i].setVisibility(View.VISIBLE);

                final int k = i;
                ScaleAnimation anim = new ScaleAnimation(smallScale, bigScale, smallScale, bigScale, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(300);
                anim.setFillAfter(true);

                animDone = false;
                mAnimDone[i] = true;

                mFilterTextViews[i].startAnimation(anim);
                mFilterImageViews[i].startAnimation(anim);
            }
        }

    }

    @Override
    public void onDestroyView() {
//        layersContainer.removeAllViews();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
//        layersContainer = null;
        mContext = null;
        mInflater = null;
        mFilter = null;
        super.onDestroy();
    }
}
