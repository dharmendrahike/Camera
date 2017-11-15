package com.pulseapp.android.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.ViewMomentAdapter;
//import com.pulseapp.android.downloader.NearbyMomentDownloader;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.UpcomingMomentsFragment;
import com.pulseapp.android.fragments.ViewMomentDetailsFragment;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.CustomViewPager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by user on 5/14/2016.
 */
public class ViewMomentActivity extends FragmentActivity implements
        ViewMomentDetailsFragment.ViewControlsCallback, UpcomingMomentsFragment.ViewControlsCallback, FireBaseKEYIDS {

    private static final String TAG = "ViewMomentActivity";
    private static final int AUTO_PLAY_THRESHOLD_TIME = 7;
    public boolean isNearByMoment;

    private GestureDetector gestureDetector;
    private CustomViewPager pager;
    public LinkedHashMap<String, MomentModel> momentListMap;
    private ArrayList<Map.Entry<String, MomentModel>> momentList;
    private ListIterator<Map.Entry<String, MomentModel>> momentIterator;
    private FireBaseHelper fireBaseHelper;
    private CountDownTimer timer;
    private long timerTime;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_moment_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toggleFullScreen(true);
        String momentId = getIntent().getStringExtra(AppLibrary.MOMENT_ID);
        String momentType = getIntent().getStringExtra(AppLibrary.MOMENT_TYPE);
        momentStatus = getIntent().getIntExtra(AppLibrary.MOMENT_STATUS, 0);
        fireBaseHelper = FireBaseHelper.getInstance(this);
        isNearByMoment = false;
        if (momentType != null && momentType.equals("nearby")) {
            isNearByMoment = true;
            if (momentListMap == null) momentListMap = new LinkedHashMap<>();
//            momentListMap.put(momentId, NearbyMomentDownloader.getNearByMoment(momentId));
        } else {
            if (momentStatus == SEEN_MOMENT) {
                momentListMap = fireBaseHelper.openSeenMoment(momentId);
            } else if (momentStatus == READY_TO_VIEW_MOMENT) {
                momentListMap = fireBaseHelper.openMoment(momentId);
            }
        }
        momentList = new ArrayList<>(momentListMap.entrySet());
        momentIterator = momentList.listIterator();
        pager = (CustomViewPager) findViewById(R.id.momentViewPager);
        pager.setAdapter(new ViewMomentAdapter(getSupportFragmentManager()));
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
    public int getMomentStatus() {
        return momentStatus;
    }

    @Override
    public void stopAutoPlay() {
        autoPlayHandler.removeCallbacksAndMessages(null);

        if (timer!=null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void resumeAutoPlay() {
        if(timer!=null) {
            timer.cancel();
            timer = null;
        }

        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(timerTime, 1000) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    @Override
    public void onCloseMomentsFragment() {

    }

    private void updateFragmentViews(int position) {
        if (position == 0) {
            ViewMomentDetailsFragment fragment = getViewMomentDetailsFragment();
            if (fragment != null) {
                fragment.onSwipeRightAction();
            }
        } else if (position == 1) {
            UpcomingMomentsFragment fragment = getUpcomingMomentsFragment();
            if (fragment != null) {
                fragment.initializeViewObjects(null);
            }
        }
    }

    public void startAutoPlay(long time) {
        if(timer!=null) {
            timer.cancel();
            timer = null;
        }
        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(AUTO_PLAY_THRESHOLD_TIME * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
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
            momentIterator = momentList.listIterator();
            pager.setCurrentItem(1);
        } else {
            finish();
        }
    }

    private ViewMomentDetailsFragment getViewMomentDetailsFragment() {
        return (ViewMomentDetailsFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.momentViewPager + ":" + 0);
    }

    private UpcomingMomentsFragment getUpcomingMomentsFragment() {
        return (UpcomingMomentsFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.momentViewPager + ":" + 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayer.getInstance().onDestroy();
        toggleFullScreen(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void toggleFullScreen(final boolean goFullScreen) {
        if (goFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    public ListIterator<Map.Entry<String, MomentModel>> getMomentListIterator() {
        return momentIterator;
    }

    @Override
    public void setTotalTime(long totalTime) {
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

}