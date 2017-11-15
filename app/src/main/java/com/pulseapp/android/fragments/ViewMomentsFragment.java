package com.pulseapp.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.ViewMomentAdapter;
import com.pulseapp.android.downloader.DynamicDownloader;
//import com.pulseapp.android.downloader.NearbyMomentDownloader;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.CustomViewPager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by user on 6/22/2016.
 */
public class ViewMomentsFragment extends BaseFragment implements
        ViewMomentDetailsFragment.ViewControlsCallback, UpcomingMomentsFragment.ViewControlsCallback, FireBaseKEYIDS {

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    private static final String TAG = "ViewMomentFragment";
    private static final int AUTO_PLAY_THRESHOLD_TIME = 7;
    public boolean isNearByMoment;

    private GestureDetector gestureDetector;
    private CustomViewPager pager;
    public LinkedHashMap<String, MomentModel> momentListMap;
    private ArrayList<Map.Entry<String, MomentModel>> momentList;
    private ListIterator<Map.Entry<String, MomentModel>> momentIterator;
    private FireBaseHelper fireBaseHelper;
    private CountDownTimer timer;
    private long timerTime, totalTimePerMedia, streamTime;
    private long totalElapsedTimertime = 0;
    private android.os.Handler autoPlayHandler = new Handler();
    private Runnable autoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (pager.getCurrentItem() == 0) {
                ViewMomentDetailsFragment fragment = getViewMomentDetailsFragment();
                if (fragment != null) {
                    fragment.playNextMedia();
                }
            } else if (pager.getCurrentItem() == 1) {
                UpcomingMomentsFragment fragment = getUpcomingMomentsFragment();
                if (fragment != null) {
                    ViewMomentDetailsFragment viewMomentDetailsFragment = getViewMomentDetailsFragment();
                    if (viewMomentDetailsFragment != null) {
                        pager.setOnPageChangeListener(null);
                        pager.setCurrentItem(0);
                        if (momentList.size() <= 1) {
                            pager.setCanSwipe(false);
                        } else {
                            pager.setCanSwipe(true);
                        }
                        viewMomentDetailsFragment.updatePlaylist();
                        pager.setOnPageChangeListener(pageChangeListener);
                    }
                }
            }
        }
    };
    private int momentStatus;
    private ViewControlsCallback viewControlsCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toggleFullScreen(true);
        String momentId = getArguments().getString(AppLibrary.MOMENT_ID);
        String momentType = getArguments().getString(AppLibrary.MOMENT_TYPE);
        momentStatus = getArguments().getInt(AppLibrary.MOMENT_STATUS, 0);
        fireBaseHelper = FireBaseHelper.getInstance(getActivity());
        isNearByMoment = false;
        if (momentType != null && momentType.equals("nearby")) {
            isNearByMoment = true;
            if (momentListMap == null) momentListMap = new LinkedHashMap<>();
            momentListMap.put(momentId, DynamicDownloader.getInstance(context).getNearByMoment(momentId));
        } else {
            if (momentStatus == SEEN_MOMENT || momentStatus == READY_AND_SEEN_MOMENT) {
                momentListMap = fireBaseHelper.openSeenMoment(momentId);
            } else if (momentStatus == READY_TO_VIEW_MOMENT) {
                momentListMap = fireBaseHelper.openReadyMoment(momentId);
            }
        }

        if (momentListMap.size() <= 0 && viewControlsCallback != null)
            viewControlsCallback.onCloseViewMomentsFragment();

        momentList = new ArrayList<>(momentListMap.entrySet());
        momentIterator = momentList.listIterator();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_moments_fragment, container, false);
        pager = (CustomViewPager) rootView.findViewById(R.id.momentViewPager);
        ViewMomentAdapter adapter = new ViewMomentAdapter(getChildFragmentManager());
        adapter.setIsNearby(this.isNearByMoment);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        pager.setOnPageChangeListener(pageChangeListener);
        if (momentList.size() <= 1) {
            pager.setCanSwipe(false);
        } else {
            pager.setCanSwipe(true);
        }
        pager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (pager.getCurrentItem() == 0 && gestureDetector != null) {
                    boolean result = gestureDetector.onTouchEvent(event);
                    if (result && pager.getCurrentItem() == 0) {
                        ViewMomentDetailsFragment fragment = getViewMomentDetailsFragment();
                        if (fragment != null) {
                            if (momentList.size() > 1)
                                fragment.onSwipeRightAction();
                        }
                    }
                }
                return false;
            }
        });
        return rootView;
    }

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            updateFragmentViews(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    private void updateFragmentViews(int position) {
        ViewMomentDetailsFragment viewMomentDetailsFragment = getViewMomentDetailsFragment();
        if (position == 0) {
            if (viewMomentDetailsFragment != null) {
                viewMomentDetailsFragment.onSwipeRightAction();
            }
        } else if (position == 1) {
            UpcomingMomentsFragment fragment = getUpcomingMomentsFragment();
            if (fragment != null) {
                fragment.initializeViewObjects(viewMomentDetailsFragment.getActiveBitmap());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        super.onDestroy();
        MediaPlayer.getInstance().onDestroy();
        toggleFullScreen(false);
    }

    public void toggleFullScreen(final boolean goFullScreen) {
        if (goFullScreen) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    public ListIterator<Map.Entry<String, MomentModel>> getMomentListIterator() {
        List<Map.Entry<String,MomentModel>> momentDetailList = new ArrayList<>();
        for (int i = 0;i < momentList.size();i++){
            momentDetailList.add(momentList.get(i));
        }
        return momentDetailList.listIterator();
    }

    @Override
    public void startAutoPlay(long time) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        autoPlayHandler.removeCallbacksAndMessages(null);
        totalElapsedTimertime += time;
        totalTimePerMedia = time;

        AppLibrary.log_d(TAG, "Starting timer for time -" + totalTimePerMedia);

        timer = new CountDownTimer(totalTimePerMedia, 10) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
                float angle = ((float)timerTime/ totalTimePerMedia)*360;
                float overallAngle =((float)(totalElapsedTimertime-timerTime)/streamTime)*360;

                getViewMomentDetailsFragment().viewpageTimer.setAngle(angle);
                getViewMomentDetailsFragment().viewpageTimer.setEraserSize((float)(overallAngle)/360);
                getViewMomentDetailsFragment().viewpageTimer.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    @Override
    public void transitToUpcomingMoments(int position) {
        AppLibrary.log_d(TAG, "transiting to upcoming moments fragment");
        if (momentList.size() > 1) {
            momentIterator.remove();
            pager.setCurrentItem(1);
        } else {
            // close the fragment
        }
    }

    private ViewMomentDetailsFragment getViewMomentDetailsFragment() {
        return (ViewMomentDetailsFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.momentViewPager + ":" + 0);
    }

    private UpcomingMomentsFragment getUpcomingMomentsFragment() {
        return (UpcomingMomentsFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.momentViewPager + ":" + 1);
    }

    @Override
    public int getMomentStatus() {
        return momentStatus;
    }

    @Override
    public void stopAutoPlay() {
        autoPlayHandler.removeCallbacksAndMessages(null);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void resumeAutoPlay() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(timerTime, 10) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
                float angle = ((float)timerTime/ totalTimePerMedia)*360;
                float overallAngle =((float)(totalElapsedTimertime-timerTime)/streamTime)*360;

                getViewMomentDetailsFragment().viewpageTimer.setAngle(angle);
                getViewMomentDetailsFragment().viewpageTimer.setEraserSize((float)(overallAngle)/360);
                getViewMomentDetailsFragment().viewpageTimer.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    @Override
    public void setTotalTime(long totalTime) {
        streamTime = totalTime;
    }

    @Override
    public void onCloseMomentsFragment() {
        viewControlsCallback.onCloseViewMomentsFragment();
    }

    @Override
    public void launchMomentDetailsFragment(String momentId) {
        momentListMap = fireBaseHelper.openMoment(momentId);
        momentList = new ArrayList<>(momentListMap.entrySet());
        momentIterator = momentList.listIterator();
        if (momentList.size() <= 1) {
            pager.setCanSwipe(false);
        } else {
            pager.setCanSwipe(true);
        }
        ViewMomentDetailsFragment fragment = getViewMomentDetailsFragment();
        if (fragment != null) {
            pager.setOnPageChangeListener(null);
            pager.setCurrentItem(0);
            fragment.updatePlaylist();
            pager.setOnPageChangeListener(pageChangeListener);
        }
    }

    public interface ViewControlsCallback {
        void onCloseViewMomentsFragment();
    }
}
