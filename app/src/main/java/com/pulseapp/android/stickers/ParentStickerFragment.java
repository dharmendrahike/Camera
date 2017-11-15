package com.pulseapp.android.stickers;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.downloader.TemplateDownloader;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.models.LocationTemplateModel;
import com.pulseapp.android.signals.BroadCastSignals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Created by deepankur on 10-12-2015.
 */
public class ParentStickerFragment extends BaseFragment {

    /**
     * Always keep NUMBER_OF_CYCLES as a multiple of 4 so that
     * (NUMBER_OF_CYCLES*PAGE_COUNT)/2 always returns a precise Integer instead of rounding it off
     * Not doing so will mess up up the equation :--pageNumberSelected = (TOTAL_PAGES / 2); , in case
     * PAGE_COUNT is an odd number.
     */
    public static final int NUMBER_OF_CYCLES = 20;
    public static int PAGE_COUNT;
    private final String TAG = "ParentStickerFragment";
    public StickerViewPager stickerPager;
    public int childFragmentInView = 0;
    public String[] themeNames;
    public CustomStickerPagerAdapter adapter;
    public ArrayList<ChildStickerFragment> fragmentList = new ArrayList<>();
    private int actualThemeNumber = 0;//theme no. is from 0 to PAGE_COUNT
    private int pageNumberSelected = 0;
    private TextView animationTv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stickers_parent, container, false);

        if (savedInstanceState!=null) return view;

        stickerPager = (StickerViewPager) view.findViewById(R.id.sticker_pager);
        animationTv = (TextView) view.findViewById(R.id.animateTv);

        int TOTAL_PAGES = PAGE_COUNT * NUMBER_OF_CYCLES;

        Bundle[] bundles = new Bundle[TOTAL_PAGES];
        for (int i = 0; i < TOTAL_PAGES; i++) {
            bundles[i] = new Bundle();
            bundles[i].putInt("bg", i);
            ChildStickerFragment fragment = new ChildStickerFragment();
            fragment.setArguments(bundles[i]);
            fragmentList.add(fragment);
        }

        adapter = new CustomStickerPagerAdapter(getChildFragmentManager(), getActivity());
        adapter.setFragmentList(fragmentList);
        stickerPager.setAdapter(adapter);
        stickerPager.setCurrentItem(TOTAL_PAGES / 2);
        pageNumberSelected = (TOTAL_PAGES / 2);


        stickerPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                childFragmentInView = position;
//                animateThemeName(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        TemplateDownloader.getInstance(context).setFragmentReference(this);
        return view;
    }

    public void setThemeNames(LinkedHashMap<String, LocationTemplateModel> templateModelLinkedHashMap) {
        int i = 0;
        this.themeNames = new String[templateModelLinkedHashMap.size()];
        for (Map.Entry<String, LocationTemplateModel> templateModelEntry : templateModelLinkedHashMap.entrySet()) {
            LocationTemplateModel model = templateModelEntry.getValue();
            if (model != null)
                themeNames[i] = model.name;
            else {
                if (templateModelEntry.getKey().equals(CLEAR_SCREEN))
                    themeNames[i] = templateModelEntry.getKey();
            }
            ++i;
        }
    }

    private void animateThemeName(final int position) {
        Log.d(TAG, " animateThemeName pos " + position);
        animationTv.setText(themeNames[position % themeNames.length]);

        animationTv.setVisibility(View.VISIBLE);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(200);
        fadeOut.setDuration(800);

        AnimationSet animation = new AnimationSet(false); //change to false
        // animation.addAnimation(fadeIn);
        animation.addAnimation(fadeOut);
        animationTv.setAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animationTv.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
        Log.d(TAG, "onDestroyOptionsMenu");
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public int getProgressBarVisibility() {
        if (fragmentList != null && fragmentList.get(stickerPager.getCurrentItem()) != null)
            return fragmentList.get(stickerPager.getCurrentItem()).getProgressBarVisibility();
        return -1;
    }

    public void refreshData() {
        for (ChildStickerFragment c : fragmentList) {
            c.refreshData();
        }
    }

    public boolean isClearScreenSelected(){
        return fragmentList.get(childFragmentInView).isThisClearScreen;
    }
}
