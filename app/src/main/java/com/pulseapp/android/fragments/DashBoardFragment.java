package com.pulseapp.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.adapters.SliderMessageListAdapter;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.customViews.CircularTextView;
import com.pulseapp.android.customViews.LockableScrollView;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.floatingbutton.FloatingActionButton;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.modelView.DummyDataGenerator;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.modelView.MediaModelView;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.models.MediaModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.models.ViewerDetails;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.EditTextBackEvent;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by deepankur on 14/4/16.
 */

public class DashBoardFragment extends BaseFragment implements FireBaseHelper.OnMyStreamsLoaded,
        FireBaseHelper.onMyMomentMediaDownloadStatusModified, FireBaseHelper.InstituteLoadedCallback, FriendsMomentsFragment.ViewControlsCallback {

    private final int BOTTOM = 0, MIDDLE = 1, TOP = 2, MOVING = -1;
    private final int MAXIMUM_ANIMATION_DURATION = 150;
    private final float MAXIMUM_TINT_ALPHA = 0.9f;
    private boolean mRecyclerTouchDownTriggered;//control variable ; touch down is sometimes consumed
    private long tintFingerDownTime;
    private float mFirstHitPoint;
    private ObjectAnimator anim;
    private boolean mDragLockGained;
    private View swipeDetectorView;
    private RecyclerView mRecyclerView;
    private LinearLayout noMessagesLayout;
    private LinearLayout sliderLayout;
    private TextView messageTextView, messageHeaderTextView;
    private FloatingActionButton messageFAB;
    private ImageView sliderArrowIv;
    private ImageView friendFragmentIv;
    private LinearLayout friendFragmentLayout;
    private LockableScrollView lockableScrollView;
    private EditTextBackEvent mSearchEditText;
    private int displayHeight;
    private FrameLayout tintedFrame;
    private RelativeLayout contentFrame/*, myMomentHeaderRl*/;
    private boolean isAnimating;
    private String TAG = this.getClass().getSimpleName();
    private RelativeLayout topBar;
    private int barHeight;
    private int mCurrentSliderPosition = BOTTOM;
    private float bottomBoundary, middleBoundary, topBoundary;//fixme always Zero bottom boundary ??
    private Context mContext;
    private long sliderAnimationDuration = MAXIMUM_ANIMATION_DURATION;
    private Handler mTintHandler;
    private boolean mPauseHandler = true;
    private long mTimeFingerDown;
    private Runnable mTintRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mPauseHandler) {
                mTintHandler.postDelayed(this, 30);
                resetTint();
            }
        }
    };

    private ViewControlsCallback viewControlsCallback;
    private LinkedHashMap<String, MediaModel> pendingMediaInMessageRoom;
    private String sendTo;
    private ImageView searchIv;
    private ImageView messageIv;
    private ImageView backIv;
    private ImageView clearSearchIv, dashboardSettingsIv;
    private String action;
    private View dummyView;
    private FrameLayout searchFrame;
    private TextView unreadCountTextView;
    private CircularTextView unreadRequestCountTextView;
    private View myStreamsView;
    private View myInstituteStream;
    private ImageView myInstituteStreamMomentSliderArrowIv;
    private TextView myInstituteStreamsTextView;
    private ImageView myInstituteMomentsIV;
    private LinearLayout institutesStreamStatsLinearLayout;
    private TextView myInstituteStreamName;
    private String activeMoment;
    private RelativeLayout unseenMomentsHeaderLayout;

    @Override
    public void onPause() {
        super.onPause();
        mPauseHandler = true;
        if (mTintHandler != null) {
            mTintHandler.removeCallbacksAndMessages(null);
        }
    }

    public void dismissSliderOnChatOpen() {
        mSearchEditText.clearFocus();
        mSearchEditText.setText("");
        animateSlider(middleBoundary);
        scaleFAB(1f);
    }

    /**
     * @return true if this fragment can consume back press;
     * false otherwise
     */
    public boolean onBackPressed() {
        if (rootView == null) return false;

        if (!AppLibrary.MESSAGING_DISABLED && getRunTimeSliderPosition() == MIDDLE) {
            animateSlider(true);
            return true;
        }

        if (searchFrame != null && searchFrame.getVisibility() == View.VISIBLE) {
            exitSearchMode();
            return true;
        }

        return false;
    }

    private float mPositionFingerDown;
    private Animator.AnimatorListener animationListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            isAnimating = true;
            Log.d(TAG, "onAnimationStart");
            mPauseHandler = false;

            sliderLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            tintedFrame.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mTintHandler.post(mTintRunnable);

            if ((mCurrentSliderPosition == BOTTOM || mCurrentSliderPosition == TOP) && messageFAB.getVisibility() == View.VISIBLE)
                scaleFAB(0f);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            isAnimating = false;
            Log.d(TAG, "onAnimationEnd");
            mPauseHandler = true;
            int pos = getRunTimeSliderPosition();
            if (pos == BOTTOM) {
                tintedFrame.setAlpha(0);
                sliderArrowIv.setScaleY(1);
                updateMessageFooterText();  // bottom pos
            } else {
                sliderArrowIv.setScaleY(-1);    // middle pos
                updateMessageFooterText();
                updateUnreadValue();
            }

            sliderLayout.setLayerType(View.LAYER_TYPE_NONE, null);
            tintedFrame.setLayerType(View.LAYER_TYPE_NONE, null);

            if (mCurrentSliderPosition == MIDDLE && messageFAB.getVisibility() == View.INVISIBLE)
                scaleFAB(1f);

        }

        @Override
        public void onAnimationCancel(Animator animation) {
            isAnimating = false;
            Log.d(TAG, "onAnimationCancel");
            mPauseHandler = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private void updateUnreadValue() {
        mFireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{mFireBaseHelper.getMyUserId(), UNREAD_ROOMS}).removeValue();
        unreadCountTextView.setVisibility(View.GONE);
    }

    private View.OnClickListener sliderClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onclick " + v);
            switch (v.getId()) {
                case R.id.backIV:
                    onImeBackPress();
                    toggleSoftKeyboard(getActivity(), mSearchEditText, false);
                    break;
                case R.id.searchIV:
                    mSearchEditText.setVisibility(View.VISIBLE);
                    clearSearchIv.setVisibility(View.GONE);//clear here todo
                    backIv.setVisibility(View.VISIBLE);
                    mSearchEditText.setGravity(Gravity.NO_GRAVITY);
                    mSearchEditText.setFocusableInTouchMode(true);
                    boolean focusGained = mSearchEditText.requestFocus();
                    Log.d(TAG, "focus gained " + focusGained);
                    mSearchEditText.setCursorVisible(focusGained);
                    toggleSoftKeyboard(getActivity(), mSearchEditText, true);

                    if (mEditTextHasFocus)
                        animateSlider(topBoundary);
                    break;
                case R.id.messageIV:
                    int pos = getRunTimeSliderPosition();
                    if (pos == MIDDLE) {
//                        scaleFAB(1f);
                        animateSlider(bottomBoundary);
                    } else if (pos == BOTTOM) {
//                        scaleFAB(0f);
                        animateSlider(middleBoundary);
                    }
                    break;
                case R.id.clearSearchIV:
                    mSearchEditText.setText("");
                    break;
            }
        }
    };

    private void updateMessageFooterText() {
        int pos = getRunTimeSliderPosition();
        if (pos == BOTTOM) {
            if (pendingMediaInMessageRoom != null && pendingMediaInMessageRoom.size() > 0) {
                if (sendTo != null) {
                    messageTextView.setText(sendTo);
                } else {
                    messageTextView.setText(getString(R.string.message_sending_failed_text));
                }
            } else {
                messageTextView.setText(getString(R.string.message_footer_text));
            }
        } else {
            messageTextView.setText(getString(R.string.message_footer_text));
        }
        sliderMessageListAdapter.setMessagesList(messageArrayList);
        sliderMessageListAdapter.notifyDataSetChanged();
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (v.getId()) {
                case R.id.message_list_recycler_view:
                    Log.d(TAG, "recycler x " + event.getX() + " y " + event.getY());

                    if (!AppLibrary.MESSAGING_DISABLED) {
                        try {
                            mRecyclerView.onTouchEvent(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        swipeDetectorView.dispatchTouchEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.tintedFrameLayout:
                    if (getRunTimeSliderPosition() != BOTTOM)
                        handleTintTouch(event);
                    else {
                        try {
                            contentFrame.dispatchTouchEvent(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                default:
                    throw new RuntimeException();
            }
        }
    };
    private float mRecyclerRecordedDownY;
    private int mRecyclerDisplacement;
    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            mRecyclerDisplacement += dy;
        }
    };
    private boolean mTranslatingFirstTime;
    private boolean mEditTextHasFocus;

    private void handleTintTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            tintFingerDownTime = System.currentTimeMillis();
            Log.d(TAG, "tintTime down " + tintFingerDownTime);
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.d(TAG, " tintTime up" + (System.currentTimeMillis() - tintFingerDownTime));
            animateSlider(true);
        }
        Log.d(TAG, " tintTouch" + event.getAction());
    }

    //    ArrayList<RoomsModel> mMessageRoomArrayList;
    LinkedHashMap<String, SliderMessageModel> messageArrayList;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            return;

        Bundle bundle = getArguments();
        action = bundle.getString("action");
        Log.d(TAG, "onCreate called");
        mContext = getActivity();
        mTintHandler = new Handler();
        swipeDetectorView = new View(getActivity());
        mFireBaseHelper.setOnMyMomentMediaDownloadStatusModified(this);
        mFireBaseHelper.setMyStreamListener(this);
        mFireBaseHelper.loadMyMoment();
        mFireBaseHelper.setInstituteLoadedCallback(this);
        messageArrayList = mFireBaseHelper.getMessageList();
        sliderMessageListAdapter = new SliderMessageListAdapter(getActivity(), null, new RecyclerViewClickInterface() {
            @Override
            public void onItemClick(int position, Object data) {
                //close keyboard and then open fragment
                toggleSoftKeyboard(getActivity(), mSearchEditText, false);
                ((CameraActivity) getActivity()).loadChatFragment((SliderMessageModel) data, true);
            }
        });
        sliderMessageListAdapter.setMessagesList(messageArrayList);
        initSliderData();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.layoutInflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        if (mFireBaseHelper == null || savedInstanceState != null)
            return rootView;

        myUserModel = mFireBaseHelper.getMyUserModel();
        if (myUserModel == null) return rootView;
        initContributedMomentViews();
        unseenMomentsHeaderLayout = (RelativeLayout) rootView.findViewById(R.id.unseenMomentsHeaderLayout);
        unseenMomentLinearLayout = (LinearLayout) rootView.findViewById(R.id.ll1);
        dummyView = rootView.findViewById(R.id.dummyView);
//        seenStreamsTv = (TextView) rootView.findViewById(R.id.seenStreamsTV);
        Log.d(TAG, "onCreateView called");
//        dashboardSettingsIv = (ImageView) rootView.findViewById(R.id.iv_dashboard_settings);
        initSearchView(rootView);
        initActionBar(rootView.findViewById(R.id.action_bar));
        addViewTreeObserver(rootView.findViewById(R.id.action_bar));
        loadNearByFragment();
//        loadMoments();
        loadFriendsMoment();
        friendFragmentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (searchFrame.getVisibility() == View.VISIBLE) {
                    toggleAllFriendSearchSearchMode(false);
                    if (searchView != null)
                        searchView.onActionViewCollapsed();
                } else {
                    unreadRequestCountTextView.setVisibility(View.GONE);
                    loadAddFriendFriendFragment();
                }
            }
        });
        View noMessageView = rootView.findViewById(R.id.include_no_message_layout);
        noMessageView.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAddFriendFriendFragment();
            }
        });
        messageTextView = (TextView) rootView.findViewById(R.id.sliderTV);
        unreadCountTextView = (TextView) rootView.findViewById(R.id.unreadCountTextView);
        messageHeaderTextView = (TextView) rootView.findViewById(R.id.messageTV);

        if (!AppLibrary.MESSAGING_DISABLED) {
            initRecyclerView(rootView);
            toggleMessagesEmptyStates();
        }

        lockableScrollView = (LockableScrollView) rootView.findViewById(R.id.scrollView);
        lockableScrollView.setOnScrollChangeListener(scrollChangeListener);
        lockableScrollView.setMotionEventChangeListener(motionEventChangeListener);
        handleSearchEditText(rootView);


        sliderLayout = (LinearLayout) rootView.findViewById(R.id.sliderLayout);
        if (!AppLibrary.MESSAGING_DISABLED)
            sliderLayout.setVisibility(View.VISIBLE);
        else {
//            ((TextView) rootView.findViewById(R.id.tv_two)).setText("Tap to view Streams from Friends");
        }
        messageFAB = (FloatingActionButton) rootView.findViewById(R.id.message_FAB);
//        messageFAB.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.chat_svg));
//        messageFAB.setImageResource(R.drawable.chat_svg);

        messageFAB.setScaleX(0);
        messageFAB.setScaleY(0);
        messageFAB.setVisibility(View.INVISIBLE);
        messageFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CameraActivity) getActivity()).openFriendListFragment();
            }
        });

        sliderArrowIv = (ImageView) rootView.findViewById(R.id.sliderArrowIV);
        tintedFrame = (FrameLayout) rootView.findViewById(R.id.tintedFrameLayout);
//        initMyMomentSlider(rootView);
        tintedFrame.setVisibility(View.VISIBLE);
        tintedFrame.setOnTouchListener(touchListener);
        contentFrame = (RelativeLayout) rootView.findViewById(R.id.mainContent);
        topBar = (RelativeLayout) rootView.findViewById(R.id.topBarLayout);


        totalScreenShotTv = (TextView) myStreamsView.findViewById(R.id.streamTotalScreenShotTV);
        totalViewsTv = (TextView) myStreamsView.findViewById(R.id.streamTotalViewTV);

        searchIv = (ImageView) rootView.findViewById(R.id.searchIV);
        messageIv = (ImageView) rootView.findViewById(R.id.messageIV);
        backIv = (ImageView) rootView.findViewById(R.id.backIV);
        clearSearchIv = (ImageView) rootView.findViewById(R.id.clearSearchIV);

//        messageIv.setOnClickListener(sliderClickListener);
        searchIv.setOnClickListener(sliderClickListener);
        backIv.setOnClickListener(sliderClickListener);
        clearSearchIv.setOnClickListener(sliderClickListener);

        addViewTreeObserver(tintedFrame);
        addViewTreeObserver(topBar);
//        addViewTreeObserver(myMomentHeaderRl);
        addViewTreeObserver(lockableScrollView);

        topBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, " currentHeight " + sliderLayout.getY());
                Log.d(TAG, " touching top bar action" + event.getAction());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    if (messageFAB.getVisibility() == View.VISIBLE)
                        scaleFAB(0f);

                    mSearchEditText.clearFocus();
                    toggleSoftKeyboard(getActivity(), mSearchEditText, false);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPauseHandler = false;
                    mTintHandler.post(mTintRunnable);
                    mDragLockGained = true;
                    mTimeFingerDown = System.currentTimeMillis();
                    mPositionFingerDown = pixelsToDp(sliderLayout.getY());
                    mTranslatingFirstTime = true;
                    Log.d(TAG, " topBarDown" + event.getAction() + " y " + event.getY());
                }
                if (mDragLockGained && event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d(TAG, " topBarMove" + event.getAction() + " y " + event.getY());
                    if (mTranslatingFirstTime) {
                        mFirstHitPoint = event.getY();
                        Log.d(TAG, " first hit point " + mFirstHitPoint);
                    }
                    doTranslation(event);
                    mTranslatingFirstTime = false;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, " topBarUp" + event.getAction() + " y " + event.getY());
                    mPauseHandler = true;
                    mDragLockGained = false;
                    float distance = pixelsToDp(sliderLayout.getY()) - mPositionFingerDown;
                    if (Math.abs(distance) < 20) {//dp
                        getRunTimeSliderPosition();
                        if (messageTextView.getText().toString().equals(context.getResources().getString(R.string.message_sending_failed_text))
                                && getRunTimeSliderPosition() == BOTTOM) {
                            retrySendingMessage();
                        } else
                            animateSlider(true);
                        return true;
                    }
                    float speed = (distance / (System.currentTimeMillis() - mTimeFingerDown)) * 1000;
                    Log.d(TAG, " distance " + distance + " speed " + speed + " dp/second");

                    long mDownUpTime = System.currentTimeMillis() - mTimeFingerDown;
                    if (mDownUpTime < 500)
                        settlingAction(speed, event);
                    else delayedSettlingAction(speed, event);
                }
                return false;
            }
        });

//        dashboardSettingsIv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ((CameraActivity) getActivity()).openSettingsFragment();
//            }
//        });

        topBar.setOnClickListener(null);
        if (action != null && (action.equals("groupRequestReceived") || action.equals("friendRequestReceived")))
            loadAddFriendFriendFragment();
        if (!appUpdateCheckDone)
            checkIfUpdateAvailable();
        return rootView;
    }

    private void checkIfUpdateAvailable() {
        rootView.findViewById(R.id.updateAppIncludedLayout);
        mFireBaseHelper.getNewFireBase(ANCHOR_APP_SETTINGS, new String[]{LATEST_ANDROID_APPLICATION_VERSION}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.exists()) {
                    Integer latestVersionOnPlayStore = dataSnapshot.getValue(Integer.class);
                    int localVersionCode = BuildConfig.VERSION_CODE;
                    if (latestVersionOnPlayStore > localVersionCode) {
                        rootView.findViewById(R.id.updateAppIncludedLayout).setVisibility(View.VISIBLE);
                        initializeApplicationUpdateView(rootView.findViewById(R.id.updateAppIncludedLayout));
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initializeApplicationUpdateView(View updateView) {
        final String appPackageName = getApplicationContext().getPackageName(); // getPackageName() from Context or Activity object
        View update = updateView.findViewById(R.id.updateView);
        TextView nameTv = (TextView) updateView.findViewById(R.id.nameTV);

        String name = mFireBaseHelper.getMyUserModel().name;

//        if (name != null && name.contains(" ") && name.length() > 0 && name.charAt(0) != ' ') {
            try {
                name = name.split(" ")[0];
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }

        nameTv.setText(name + ", we’re always working to improve your Pulse experience. Use the latest version to get new features and improvements. We hope you enjoy it!");
        View close = updateView.findViewById(R.id.closeView);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootView.findViewById(R.id.updateAppIncludedLayout).setVisibility(View.GONE);
                appUpdateCheckDone = true;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (Exception ignored) {

                }
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootView.findViewById(R.id.updateAppIncludedLayout).setVisibility(View.GONE);
                appUpdateCheckDone = true;
            }
        });


    }

    static boolean appUpdateCheckDone = false;

    private void retrySendingMessage() {
        sendTo = "Sending to ";
        if (pendingMediaInMessageRoom != null && pendingMediaInMessageRoom.size() > 0) {
            for (Map.Entry<String, MediaModel> entry : pendingMediaInMessageRoom.entrySet()) {
                MediaModel mediaModel = entry.getValue();
                for (Map.Entry<String, Integer> roomEntry : mediaModel.addedTo.rooms.entrySet()) {
                    if (messageArrayList.containsKey(roomEntry.getKey())) {
                        sendTo += messageArrayList.get(roomEntry.getKey()).displayName + ",";
                        break;
                    }
                }
            }
            messageTextView.setText(sendTo);
        }
        viewControlsCallback.onUploadRetryClickedForAllMediaMessages();
    }


    void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getActivity(), v);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("Settings")) {
                            ((CameraActivity) getActivity()).openSettingsFragment();
                        }
                        return true;
                    }
                });

                popup.show();
            }
        });

        ((ImageView) actionBar.findViewById(R.id.action_bar_IV_4)).setImageResource(R.drawable.more_svg);

        friendFragmentIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        friendFragmentIv.setImageResource(R.drawable.add_friends_svg);
        friendFragmentLayout = (LinearLayout) actionBar.findViewById(R.id.friendFragmentLayout);
        unreadRequestCountTextView = (CircularTextView) actionBar.findViewById(R.id.unreadRequestCountTextView);
        unreadRequestCountTextView.setSolidColor("#ff0000");
        SocialModel socialModel = mFireBaseHelper.getSocialModel();
        if (socialModel != null) {
            int count = 0;
            if (socialModel.pendingGroupRequest != null)
                count += socialModel.pendingGroupRequest.size();
            if (socialModel.requestReceived != null)
                count += socialModel.requestReceived.size();
            if (count > 0) {
                unreadRequestCountTextView.setText(String.valueOf(count));
                unreadRequestCountTextView.setVisibility(View.VISIBLE);
            } else
                unreadRequestCountTextView.setVisibility(View.GONE);
        }

        ((TextView) actionBar.findViewById(R.id.titleTV)).setText("PULSE");
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        actionBar.findViewById(R.id.titleTV).setLayoutParams(params);

        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();

        ((FrameLayout.LayoutParams) searchFrame.getLayoutParams()).topMargin = AppLibrary.convertDpToPixels(context, 56) +
                AppLibrary.getStatusBarHeight(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        mPauseHandler = false;
    }

    private void handleSearchEditText(View rootView) {
        mSearchEditText = (EditTextBackEvent) rootView.findViewById(R.id.searchET);
        mSearchEditText.setTypeface(fontPicker.getMontserratRegular());
        mSearchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mSearchEditText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
//        mSearchEditText.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                mSearchEditText.setGravity(Gravity.NO_GRAVITY);
//                mSearchEditText.setFocusableInTouchMode(true);
//                boolean focusGained = mSearchEditText.requestFocus();
//                mSearchEditText.setCursorVisible(focusGained);
//                toggleSoftKeyboard(getActivity(), mSearchEditText, true);
//                if (mEditTextHasFocus)
//                    animateETonSearchIntended();
//                return true;
//            }
//        });

        mSearchEditText.setOnEditTextImeBackListener(new EditTextBackEvent.EditTextImeBackListener() {
            @Override
            public void onImeBack(EditTextBackEvent ctrl, String text) {
                onImeBackPress();
            }
        });
        mSearchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mEditTextHasFocus = hasFocus;
                mSearchEditText.setGravity(hasFocus ? Gravity.NO_GRAVITY : Gravity.CENTER_VERTICAL);
                float reqDelta = hasFocus ? Math.abs(sliderLayout.getY() - topBoundary) :
                        Math.abs(sliderLayout.getY() - middleBoundary);

                sliderAnimationDuration = 3 * (long) (MAXIMUM_ANIMATION_DURATION *
                        reqDelta / (bottomBoundary - middleBoundary));

                if (hasFocus) {
                    animateETonSearchIntended();
                }
            }
        });
        mSearchEditText.addTextChangedListener(textWatcher);
    }

    void onImeBackPress() {
        sliderAnimationDuration = (long) (MAXIMUM_ANIMATION_DURATION * ((topBoundary - middleBoundary) / bottomBoundary));
        sliderAnimationDuration = sliderAnimationDuration < 0 ? -sliderAnimationDuration : sliderAnimationDuration;
        mSearchEditText.setText("");
        mSearchEditText.clearFocus();
        toggleSoftKeyboard(context, mSearchEditText, false);
        animateSlider(middleBoundary);
        scaleFAB(1f);
    }

    private void animateETonSearchIntended() {
        mCurrentSliderPosition = TOP;
        animateSlider(topBoundary);
    }

    private void doTranslation(MotionEvent event) {
        sliderLayout.setTranslationY(sliderLayout.getTranslationY() + event.getY() - mFirstHitPoint);
    }

    private void resetTint() {
        float alpha = (MAXIMUM_TINT_ALPHA * (bottomBoundary - sliderLayout.getY())) / bottomBoundary;
        if (alpha > MAXIMUM_TINT_ALPHA || alpha < 0) {
            Log.e(TAG, "Alpha gone bonkers with alpha: " + alpha);
        }

        alpha = alpha < 0 ? 0 : alpha;
        alpha = alpha > MAXIMUM_TINT_ALPHA ? MAXIMUM_TINT_ALPHA : alpha;

        tintedFrame.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        tintedFrame.setAlpha(alpha);
    }

    private void settlingAction(float speed, MotionEvent event) {

        float deltaNeededToSettle = speed < 0 ? middleBoundary - sliderLayout.getY() :
                bottomBoundary - sliderLayout.getY();

        Log.d(TAG, " newSettling method : delta " + deltaNeededToSettle);
        sliderAnimationDuration = (long) (MAXIMUM_ANIMATION_DURATION * ((deltaNeededToSettle / (bottomBoundary - middleBoundary))));
        sliderAnimationDuration = sliderAnimationDuration < 0 ? -sliderAnimationDuration : sliderAnimationDuration;
        mCurrentSliderPosition = speed < 0 ? BOTTOM : MIDDLE;
        animateSlider(false);
    }

    private void delayedSettlingAction(float speed, MotionEvent event) {

        float middleBoundaryDistance = sliderLayout.getY() - middleBoundary;
        float bottomBoundaryDistance = sliderLayout.getY() - bottomBoundary;

        middleBoundaryDistance = middleBoundaryDistance < 0 ? -middleBoundaryDistance : middleBoundaryDistance;
        bottomBoundaryDistance = bottomBoundaryDistance < 0 ? -bottomBoundaryDistance : bottomBoundaryDistance;

        float deltaNeededToSettle = middleBoundaryDistance < bottomBoundaryDistance ? middleBoundaryDistance : bottomBoundaryDistance;

        sliderAnimationDuration = (long) (MAXIMUM_ANIMATION_DURATION * ((deltaNeededToSettle / (bottomBoundary - middleBoundary))));
        sliderAnimationDuration = sliderAnimationDuration < 0 ? -sliderAnimationDuration : sliderAnimationDuration;
        mCurrentSliderPosition = middleBoundaryDistance < bottomBoundaryDistance ? BOTTOM : MIDDLE;
        animateSlider(false);
    }

    //while listing

    private void animateSlider(boolean defaultAnimationDuration) {
        Log.d(TAG, " top" + sliderLayout.getY());
        sliderAnimationDuration = sliderAnimationDuration < 0 ?
                -sliderAnimationDuration : sliderAnimationDuration;
        if (defaultAnimationDuration)
            sliderAnimationDuration = MAXIMUM_ANIMATION_DURATION;
        switch (mCurrentSliderPosition) {
            case BOTTOM:
                mCurrentSliderPosition = MIDDLE;
                animateSlider(middleBoundary);
                return;
            case MIDDLE:
                mCurrentSliderPosition = BOTTOM;
                animateSlider(bottomBoundary);
                return;
            case TOP:
                toggleSoftKeyboard(context, mSearchEditText, false);
                mCurrentSliderPosition = BOTTOM;
                animateSlider(bottomBoundary);
                return;
            case MOVING:

                return;
            default:
                throw new RuntimeException();
        }
    }

    private void animateSlider(float endPoint) {
        if (anim == null) {
            anim = ObjectAnimator.ofFloat(sliderLayout, "translationY", endPoint);
            anim.addListener(animationListener);
//            anim.setInterpolator(new BounceInterpolator());
        } else {
            anim.cancel();
        }
        anim.setFloatValues(endPoint);
        anim.setDuration(sliderAnimationDuration);
        anim.start();


        //for new search views
        if (endPoint == bottomBoundary) {
            mSearchEditText.setText("");
            searchIv.setVisibility(View.GONE);
            sliderArrowIv.setVisibility(View.VISIBLE);
            messageHeaderTextView.setVisibility(View.VISIBLE);
            messageTextView.setVisibility(View.VISIBLE);
            messageIv.setVisibility(View.VISIBLE);
            messageIv.setImageResource(R.drawable.notification_svg);
            topBar.setVisibility(View.VISIBLE);
            mSearchEditText.setVisibility(View.GONE);
            backIv.setVisibility(View.GONE);
            clearSearchIv.setVisibility(View.GONE);
            ((CameraActivity) getActivity()).toggleFullScreen(false);
        } else if (endPoint == middleBoundary) {
            searchIv.setVisibility(View.VISIBLE);
            sliderArrowIv.setVisibility(View.GONE);
            topBar.setVisibility(View.VISIBLE);
            messageHeaderTextView.setVisibility(View.VISIBLE);
            messageTextView.setVisibility(View.VISIBLE);
            messageIv.setVisibility(View.VISIBLE);
            messageIv.setImageResource(R.drawable.notification_gray_svg);
            mSearchEditText.setVisibility(View.GONE);
            backIv.setVisibility(View.GONE);
            clearSearchIv.setVisibility(View.GONE);
            ((CameraActivity) getActivity()).toggleFullScreen(false);
        } else if (endPoint == topBoundary) {
            searchIv.setVisibility(View.GONE);
            sliderArrowIv.setVisibility(View.GONE);
            topBar.setVisibility(View.VISIBLE);
            messageHeaderTextView.setVisibility(View.GONE);
            messageTextView.setVisibility(View.GONE);
            messageIv.setVisibility(View.GONE);
            ((CameraActivity) getActivity()).toggleFullScreen(true);
        }

    }

    float fabScaleTo;

    private void scaleFAB(float scaleTo) {
        fabScaleTo = scaleTo;

        long animDuration;
        if (scaleTo >= 0.5f)
            animDuration = 100;
        else
            animDuration = 50;

        messageFAB.setVisibility(View.VISIBLE);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(messageFAB, "scaleX", scaleTo);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(messageFAB, "scaleY", scaleTo);
        scaleDownX.setDuration(animDuration);
        scaleDownY.setDuration(animDuration);
        AnimatorSet scaleFab = new AnimatorSet();
        scaleFab.play(scaleDownX).with(scaleDownY);
        scaleFab.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                messageFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                messageFAB.setVisibility(scaleTo == 0 ? View.GONE : View.VISIBLE);
                if (fabScaleTo == 0) messageFAB.setVisibility(View.INVISIBLE);

                messageFAB.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        scaleFab.start();
    }

    //    int myStreamsHeaderHeight;
    int scrollViewTopDistance;

    private void addViewTreeObserver(final View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (view.getId() == R.id.scrollView) {
                        scrollViewTopDistance = view.getTop();
//                        resetMyMomentMargins(scrollViewTopDistance);
//                        scrollViewTopMargin=scrollViewTopDistance;
                        Log.d(TAG, "addViewTreeObserver scrollViewTopDistance: " + scrollViewTopDistance);
                        ((FrameLayout.LayoutParams) contributedMediaFrame.getLayoutParams()).topMargin = scrollViewTopDistance;
                        return;
                    }
//                    if (view.getId() == R.id.myMomentHeaderRL) {
//                        myStreamsHeaderHeight = view.getHeight();
//                        Log.d(TAG, "addViewTreeObserver myStreamsHeaderHeight: " + myStreamsHeaderHeight);
//                        return;
//                    }

//                    if (view.getId() == R.id.action_bar) {
                    // resetMyMomentMargins(view.getHeight());
//                        return;
//                    }

                    int height = view.getMeasuredHeight();
                    if (view.getId() == R.id.tintedFrameLayout) {
                        displayHeight = height;
                        middleBoundary = (float) (height * 0.3);
                        topBoundary = 0;
//                        sliderLayout.getLayoutParams().height = (int) (0.7 * height);
                        sliderLayout.setPadding(0, 0, 0, (int) middleBoundary);
//                        ((FrameLayout.LayoutParams) sliderLayout.getLayoutParams()).p
                        Log.d(TAG, " vto frame");
                    }

                    if (view.getId() == R.id.topBarLayout) {
                        barHeight = height;
                        Log.d(TAG, " vto RL");
//                        lockableScrollView.setPadding(0, 0, 0, height);
//                        ((RelativeLayout.LayoutParams) lockableScrollView.getLayoutParams()).bottomMargin=height;
                        ((LinearLayout.LayoutParams) dummyView.getLayoutParams()).height = height;
                    }
                    bottomBoundary = displayHeight - barHeight;
                    sliderLayout.setTranslationY((bottomBoundary));//interpolator can work here instead so no hard translations
                    Log.d(TAG, "VTO " + middleBoundary + "<-middle bottom->" + bottomBoundary);
                    /*resetTint();*/
                    tintedFrame.setAlpha(0);
                }
            });
        }
    }

    void toggleSearchMode(boolean enterSearchMode) {
        messageTextView.setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);
        messageHeaderTextView.setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);
        sliderArrowIv.setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);

    }

    private int getRunTimeSliderPosition() {
        float currentDelta = sliderLayout.getY();
        float dropDistanceFromMiddle = currentDelta - middleBoundary;
        if (dropDistanceFromMiddle < 0) dropDistanceFromMiddle = -dropDistanceFromMiddle;

        float dropDistanceFromBottom = currentDelta - bottomBoundary;
        if (dropDistanceFromBottom < 0) dropDistanceFromBottom = -dropDistanceFromBottom;

        return dropDistanceFromBottom > dropDistanceFromMiddle ? MIDDLE : BOTTOM;

    }

    private SliderMessageListAdapter sliderMessageListAdapter;

    private void initRecyclerView(View rootView) {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.message_list_recycler_view);
        mRecyclerView.setVisibility(View.VISIBLE);
        noMessagesLayout = (LinearLayout) rootView.findViewById(R.id.no_messages_layout);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(sliderMessageListAdapter);
        mRecyclerView.addOnScrollListener(scrollListener);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mRecyclerView.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mRecyclerRecordedDownY = event.getY();
                    mRecyclerTouchDownTriggered = true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE && !mRecyclerTouchDownTriggered) {
                    mRecyclerRecordedDownY = event.getY();
                    mRecyclerTouchDownTriggered = true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP &&
                        mRecyclerDisplacement == 0 &&
                        event.getY() > mRecyclerRecordedDownY) {
                    animateSlider(true);
                    mRecyclerTouchDownTriggered = false;
                }
                return true;
            }
        });
    }

    public AroundYouFragment aroundYouFragment;

    // nearBy fragment
    private void loadNearByFragment() {
        if (aroundYouFragment != null) {
            Log.e(TAG, " ignoring multiple instancing of a fragment ");
            return;
        }
        aroundYouFragment = new AroundYouFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.nearByLL, aroundYouFragment, AroundYouFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    //Message room Code begins
    private void initSliderData() {
        mFireBaseHelper.setSliderMessageListListener(new FireBaseHelper.SliderMessageListListener() {
            @Override
            public void onSliderListChanged(String roomId, final LinkedHashMap<String, SliderMessageModel> sliderMessageModels) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageArrayList = sliderMessageModels;
                            sliderMessageListAdapter.setMessagesList(messageArrayList);
                            toggleMessagesEmptyStates();
                            sliderMessageListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }

            @Override
            public void onMessageCountUpdate(final long unreadRoomsCount) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (unreadRoomsCount > 0) {
                                unreadCountTextView.setText(String.valueOf(unreadRoomsCount));
                                unreadCountTextView.setVisibility(View.VISIBLE);
                            } else {
                                unreadCountTextView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        });
    }

    private void toggleMessagesEmptyStates() {
        boolean isMessageListEmpty = messageArrayList == null || messageArrayList.size() == 0;
        if (noMessagesLayout == null || mRecyclerView == null)
            return;
        noMessagesLayout.setVisibility(isMessageListEmpty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isMessageListEmpty ? View.GONE : View.VISIBLE);
    }

    private LinkedHashMap<String, SliderMessageModel> filteredMessageList;

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Log.d(TAG, " textWatcher beforeTextChanged : " + s + " count " + count + " after " + after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.d(TAG, " textWatcher onTextChanged : " + s + " count " + count + " count " + count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            Log.d(TAG, " textWatcher afterTextChanged : " + s);
            Log.d(TAG, " textWatcher filtering started at " + System.currentTimeMillis());
            int length = s.length();
            clearSearchIv.setVisibility(length == 0 ? View.GONE : View.VISIBLE);
            if (s.toString().isEmpty()) {
                sliderMessageListAdapter.setMessagesList(messageArrayList);
                sliderMessageListAdapter.notifyDataSetChanged();
                return;
            }
            if (filteredMessageList == null) filteredMessageList = new LinkedHashMap<>();
            filteredMessageList.clear();
            for (Map.Entry<String, SliderMessageModel> entry : messageArrayList.entrySet()) {
                if (entry.getValue().displayName.toLowerCase().contains(s.toString().toLowerCase())) {
                    filteredMessageList.put(entry.getKey(), entry.getValue());
                }
            }
            sliderMessageListAdapter.setMessagesList(filteredMessageList);
            sliderMessageListAdapter.notifyDataSetChanged();
            Log.d(TAG, " textWatcher filtering done @ " + System.currentTimeMillis());
        }
    };
    //Message Rooms code ends----------------->momentRoom Begins
    MomentListFragment unseenMomentFragment, seenMomentFragment, favouriteMomentFragment;

    private ArrayList<HomeMomentViewModel> sortMomentList(ArrayList<HomeMomentViewModel> rooms) {
        if (rooms != null) {
            Collections.sort(rooms, new Comparator<HomeMomentViewModel>() {
                @Override
                public int compare(HomeMomentViewModel ele1,
                                   HomeMomentViewModel ele2) {
                    return (int) (ele2.updatedAt - ele1.updatedAt);
                }
            });
        }
        return rooms;
    }

    private void loadMoments() {

        if (unseenMomentFragment == null)
            loadUnSeenMomentFragment();

        if (seenMomentFragment == null)
            loadSeenMomentFragment();

     /*   if (favouriteMomentFragment == null)//todo in future
            loadFavoriteMomentFragment();*/

        mFireBaseHelper.setMomentsDataListener(new FireBaseHelper.MomentsDataListener() {
            @Override
            public void onUnseenDataChanged(final ArrayList<HomeMomentViewModel> rooms) {
//                if (rooms == null)
//                    return;
//                final ArrayList<HomeMomentViewModel> sortedRooms = sortMomentList(rooms);
//                if (getActivity() != null) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d(TAG, " onUnseenDataChanged length " + rooms.size());
//                            unseenMomentFragment.setRoomsModelArrayList(sortedRooms);
//                            unseenMomentArrayList = sortedRooms;
//                            toggleSeenMomentFragment();
//                            viewControlsCallback.onUnSeenItemsChanges(rooms.size());
//                        }
//                    });
//                }
            }

            @Override
            public void onSeenDataChanged(final ArrayList<HomeMomentViewModel> rooms) {
//                if (rooms == null)
//                    return;
//                final ArrayList<HomeMomentViewModel> sortedRooms = sortMomentList(rooms);
//                if (getActivity() != null) {
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d(TAG, " onSeenDataChanged length " + rooms.size());
//                            seenMomentFragment.setRoomsModelArrayList(sortedRooms);
//                            seenMomentArayList = sortedRooms;
//                            toggleSeenMomentFragment();
//                        }
//                    });
//                }
            }

            @Override
            public void onFavouritesDataChanged(ArrayList<HomeMomentViewModel> rooms) {
//                favouriteMomentFragment.setRoomsModelArrayList(rooms);
//                   favouriteMomentFragment.setRoomsModelArrayList(dummyDataGenerator.getFavouriteMoments());
            }

            @Override
            public void onUnseenItemStatusChanged(String momentId) {
//                unseenMomentFragment.updateSeenRoomModelArrayList(momentId);
            }

            @Override
            public void onSeenItemStatusChanges(String momentId, int position) {

            }
        });
    }

    private void toggleSeenMomentFragment() {
//        boolean hide = false;
//        seenMomentArayList, unseenMomentArrayList
//        if ((seenMomentArayList != null && seenMomentArayList.size() > 0)||(unseenMomentArrayList.))
        boolean seenMomentsPresent = seenMomentArayList != null && seenMomentArayList.size() > 0;
        boolean unSeenMomentsPresent = unseenMomentArrayList != null && unseenMomentArrayList.size() > 0;
        if (unSeenMomentsPresent) {
//            unseenMomentsHeaderLayout.setVisibility(View.VISIBLE);
            unseenMomentLinearLayout.setVisibility(View.VISIBLE);
        }
        if (seenMomentsPresent && !unSeenMomentsPresent) {
//            unseenMomentsHeaderLayout.setVisibility(View.GONE);
            unseenMomentLinearLayout.setVisibility(View.GONE);
        }
        if (!seenMomentsPresent && !unSeenMomentsPresent) {
//            unseenMomentsHeaderLayout.setVisibility(View.VISIBLE);
            unseenMomentLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    LinearLayout unseenMomentLinearLayout;
    ArrayList<HomeMomentViewModel> seenMomentArayList, unseenMomentArrayList;
    private DummyDataGenerator dummyDataGenerator = DummyDataGenerator.getInstance();


    public void loadAddFriendFriendFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, new AddFriendFragment(), AddFriendFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(AddFriendFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    public Fragment getAddFriendFragment() {
        if (getActivity() != null && getActivity().getSupportFragmentManager() != null)
            return getActivity().getSupportFragmentManager().findFragmentByTag(AddFriendFragment.class.getSimpleName());
        else
            return null;
    }

    public void loadUnSeenMomentFragment() {
        unseenMomentFragment = new MomentListFragment();
        unseenMomentFragment.setAddFriendListener(new MomentListFragment.AddFriendListener() {
            @Override
            public void requestOpenAddFriendFragment() {
                loadAddFriendFriendFragment();
            }
        });
        Bundle data = new Bundle();
        data.putInt(MOMENT_VIEW_TYPE, UNSEEN_FRIEND_MOMENT_RECYCLER);
        unseenMomentFragment.setArguments(data);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.ll1, unseenMomentFragment, "UnseenMomentFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void loadFavoriteMomentFragment() {

        favouriteMomentFragment = new MomentListFragment();
        Bundle data = new Bundle();
        data.putInt(MOMENT_VIEW_TYPE, FAVOURITE_MOMENT_RECYCLER);
        favouriteMomentFragment.setArguments(data);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.ll2, favouriteMomentFragment, "FavouriteMomentFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void loadSeenMomentFragment() {
        seenMomentFragment = new MomentListFragment();
        Bundle data = new Bundle();
        data.putInt(MOMENT_VIEW_TYPE, SEEN_FRIEND_MOMENT_RECYCLER);
        seenMomentFragment.setArguments(data);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.ll3, seenMomentFragment, "SeenMomentFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    private ContributedToMomentSliderFragment contributedToMomentSliderFragment;
    private final int TOP_BAR_HEIGHT = 56;//in dps


    void refreshMyStreamsStats(final String momentId, LinkedHashMap<String, MediaModelView> list) {
        if (list != null && momentId != null) {
            int screenShotCount = 0, totalViewCount = 0;
            for (Map.Entry<String, MediaModelView> entry : list.entrySet()) {
                final MediaModelView value = entry.getValue();
                if (value != null) {
                    if (value.viewerDetails != null) {
                        totalViewCount += value.viewerDetails.size() + value.webViews;
                        for (Map.Entry<String, ViewerDetails> entry1 : value.viewerDetails.entrySet()) {
                            if (entry1.getValue().screenShotted)
                                screenShotCount++;
                        }
                    } else if (value.webViews > 0) {
                        totalViewCount += value.webViews;
                    }
                }
            }
            final int finalScreenShotCount = screenShotCount;
            final int finalTotalViewCount = totalViewCount;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (totalScreenShotTv != null && totalViewsTv != null && momentId.equals(mFireBaseHelper.getMyUserModel().momentId)) {
                        totalScreenShotTv.setText(String.valueOf(finalScreenShotCount));
                        totalViewsTv.setText(String.valueOf(finalTotalViewCount));
                    } else {
                        if (momentViewList != null && momentViewList.containsKey(momentId)) {
                            View itemView = momentViewList.get(momentId);
                            ((TextView) itemView.findViewById(R.id.streamTotalScreenShotTV)).setText(String.valueOf(finalScreenShotCount));
                            ((TextView) itemView.findViewById(R.id.streamTotalViewTV)).setText(String.valueOf(finalTotalViewCount));
                        }
                    }
                }
            });
        }
    }

    private int myMomentMediaListSize = 0;

    @Override
    public void onStreamsLoaded(final LinkedHashMap<String, MediaModelView> list, final LinkedHashMap<String, MediaModel> pendingMyMomentMedia, final LinkedHashMap<String, MediaModel> pendingMediasInMessageRoom) {
        if (getActivity() == null || mFireBaseHelper.getMyUserModel() == null || !this.isAdded())
            return;

        if (rootView == null) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onStreamsLoaded(list, pendingMyMomentMedia, pendingMediasInMessageRoom);
                }
            }, 100);
            return;
        }

        refreshMyStreamsStats(mFireBaseHelper.getMyUserModel().momentId, list);
        updateMomentView(mFireBaseHelper.getMyUserModel().momentId, list, MEDIA_UPLOADING_COMPLETE);

        // update status in message slider footer
        if (pendingMediasInMessageRoom.size() > 0) {
            pendingMediaInMessageRoom = pendingMediasInMessageRoom;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageTextView.setText(getString(R.string.message_sending_failed_text));
                }
            });
            updateMessageSliderForPendingMedia(pendingMediaInMessageRoom);
        }

        // update my stream header views
        myMomentMediaListSize = mFireBaseHelper.getMyStreams().size();
        final LinkedHashMap<String, MediaModelView> sortedList = sortMyStreams(mFireBaseHelper.getMyStreams());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (myMomentSliderArrowIv != null)
                    myMomentSliderArrowIv.setVisibility(myMomentMediaListSize > 0 ? View.VISIBLE : View.GONE);

                if (myStreamsTextView != null && myMomentMediaListSize <= 0) {
                    myStreamsTextView.setText(getString(R.string.add_photo_video));
                    myMomentsIV.setImageResource(R.drawable.my_streams_svg);
                }
                String s = null;
                if (myInstituteStreamMomentSliderArrowIv != null && myInstituteStream != null && (s = (String) myInstituteStream.getTag(R.id.momentName)) != null && mFireBaseHelper.getContributableDownloadedStreams(s).size() == 0) {
                    myInstituteStreamMomentSliderArrowIv.setVisibility(View.GONE);
                }

                // update streams to my moment slider fragment
                if (contributedToMomentSliderFragment != null) {
                    contributedToMomentSliderFragment.setMomentId(mFireBaseHelper.getMyUserModel().momentId);
                    contributedToMomentSliderFragment.setMediaList(sortedList);
                }
            }
        });


        // check for pending media uploads and update status in my stream header view accordingly
        updateForPendingMediaUploads();
    }

    private void updateForPendingMediaUploads() {
        String myMomentId = mFireBaseHelper.getMyUserModel().momentId;
        LinkedHashMap<String, MediaModel> pendingMedias = mFireBaseHelper.getPendingMediaInMyMoment();
        if (pendingMedias != null && pendingMedias.size() > 0) {
            for (final Map.Entry<String, MediaModel> entry : pendingMedias.entrySet()) {
                HashMap<String, Integer> momentList = entry.getValue().addedTo.moments;
                if (momentList != null && momentList.size() > 0) {
                    for (Map.Entry<String, Integer> momentEntry : momentList.entrySet()) {
                        if (momentEntry.getKey().equals(myMomentId)) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myStreamsTextView.setText(getString(R.string.posting_media_failed_text));
                                }
                            });
                        } else {
                            final String contributedMomentId = momentEntry.getKey();
                            if (momentViewList.containsKey(contributedMomentId) && momentViewList.get(contributedMomentId) != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        View itemView = momentViewList.get(contributedMomentId);
                                        ((TextView) itemView.findViewById(R.id.myStreamsText)).setText(getString(R.string.posting_media_failed_text));
                                    }
                                });
                            } else {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateMomentViewFromLocalData(contributedMomentId, entry.getKey(), entry.getValue());
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateMomentViewFromLocalData(String momentId, String mediaId, MediaModel mediaModel) {
        if (AppLibrary.checkStringObject(momentId) != null) {
            View itemView = layoutInflater.inflate(R.layout.contributed_moment_tab, null);
            final ImageView momentImageView = ((ImageView) itemView.findViewById(R.id.myMomentsIV));
            final TextView name = (TextView) itemView.findViewById(R.id.momentName);
            ((TextView) itemView.findViewById(R.id.myStreamsText)).setText(getString(R.string.posting_media_failed_text));
            if (mediaModel != null && mediaModel.momentDetails != null && mediaModel.momentDetails.containsKey(momentId)) {
                if (AppLibrary.checkStringObject(mediaModel.momentDetails.get(momentId).thumbnailUrl) != null)
                    Picasso.with(getActivity()).load(mediaModel.momentDetails.get(momentId).thumbnailUrl)
                            .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().transform(new RoundedTransformation()).into(momentImageView);
                if (AppLibrary.checkStringObject(mediaModel.momentDetails.get(momentId).name) != null) {
                    String customMarquee = Utils.getCustomMarquee(mediaModel.momentDetails.get(momentId).name, 30);
                    name.setText(customMarquee);
                }
            } else {
                mFireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId}).keepSynced(true);
                mFireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        MomentModel momentModel = dataSnapshot.getValue(MomentModel.class);
                        if (AppLibrary.checkStringObject(momentModel.thumbnailUrl) != null)
                            Picasso.with(getActivity()).load(momentModel.thumbnailUrl)
                                    .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().transform(new RoundedTransformation()).into(momentImageView);
                        if (AppLibrary.checkStringObject(momentModel.name) != null)
                            name.setText(momentModel.name);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
            }
            if (!momentViewList.containsKey(momentId)) {
                View arrow = itemView.findViewById(R.id.momentSliderIndicator);
                arrow.setOnClickListener(contributedMomentClickListener);
                itemView.setOnClickListener(contributedMomentClickListener);

                itemView.setTag(momentId);
                arrow.setTag(itemView);
                momentViewList.put(momentId, itemView);
//            contributionLl.addView(itemView);
                notifyContributedTabsAdded(itemView, false);
            }
        }
    }

    private LinkedHashMap<String, MediaModelView> sortMyStreams(LinkedHashMap<String, MediaModelView> list) {
        LinkedHashMap<String, MediaModelView> newSortedList = new LinkedHashMap<>();
        if (list != null) {
            List<Map.Entry<String, MediaModelView>> entryList = new LinkedList<>(list.entrySet());
            Collections.sort(entryList, new Comparator<Map.Entry<String, MediaModelView>>() {
                @Override
                public int compare(Map.Entry<String, MediaModelView> ele1,
                                   Map.Entry<String, MediaModelView> ele2) {
                    return (int) (ele2.getValue().createdAt - ele1.getValue().createdAt);
                }
            });
            for (Map.Entry<String, MediaModelView> entry : entryList) {
                newSortedList.put(entry.getKey(), entry.getValue());
            }
        }
        return newSortedList;
    }

    @Override
    public void onMediaInMessageRoomsUpdated(LinkedHashMap<String, MediaModel> pendingMediaInMessageRoom) {
    }

    @Override
    public void onMediaInMomentUpdated(LinkedHashMap<String, MediaModel> pendingMyMomentMedia) {
    }

    int count;

    @Override
    public void onSocialRequestChanges(final HashMap<String, SocialModel.RequestReceived> requests) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (requests != null) {
                        count = requests.size();
                        if (count > 0) {
                            unreadRequestCountTextView.setText(String.valueOf(count));
                            unreadRequestCountTextView.setVisibility(View.VISIBLE);
                        } else {
                            unreadRequestCountTextView.setVisibility(View.GONE);
                        }

                        Fragment addFriendFragment = getAddFriendFragment();
                        if (addFriendFragment != null)
                            ((AddFriendFragment) addFriendFragment).refreshSocialData(requests);
                    }
                }
            });
        }
    }

    @Override
    public void onContributedStreamsLoaded(String momentId, String mediaId, LinkedHashMap<String, CustomMomentModel> momentList, LinkedHashMap<String, LinkedHashMap<String, MediaModelView>> contributionList) {
        onContributionDataLoaded(momentId, mediaId, momentList, contributionList);
    }

    @Override
    public void onFacebookFriendsChanged() {
        final Fragment addFriendFragment = getAddFriendFragment();
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (addFriendFragment != null)
                        ((AddFriendFragment) addFriendFragment).refreshFacebookFriendsSuggestions();
                }
            });
        }
    }

    public void updateMessageSliderForPendingMedia(LinkedHashMap<String, MediaModel> pendingMediaInMessageRoom) {
        for (Map.Entry<String, MediaModel> entry : pendingMediaInMessageRoom.entrySet()) {
            MediaModel mediaModel = entry.getValue();
            if (mediaModel.addedTo.rooms.size() > 0) {
                for (String room : mediaModel.addedTo.rooms.keySet()) {
                    if (messageArrayList != null && messageArrayList.size() > 0) {
                        if (messageArrayList.containsKey(room)) {
                            messageArrayList.get(room).status = messageArrayList.get(room).setStatus(SENDING_FAILED);
                        }
                    }
                }
            }
        }
        sliderMessageListAdapter.notifyDataSetChanged();
    }

    public void updateMediaUploadStatus(MediaModel mediaModel, String mediaId, int uploadStatus) {
        if (mediaModel.addedTo.moments != null && !mediaModel.addedTo.moments.isEmpty()) {
            addAndUpdateMomentsStreamList(uploadStatus, mediaId, mediaModel);
            updateStatusInMomentList(mediaModel, mediaId, uploadStatus);
        }

        if (mediaModel.addedTo.rooms != null && !mediaModel.addedTo.rooms.isEmpty()) {
            for (Map.Entry<String, Integer> entry : mediaModel.addedTo.rooms.entrySet()) {
                if (uploadStatus == MEDIA_UPLOADING_STARTED || uploadStatus == UPLOADING_NOT_READY_VIDEO) {
                    if (sendTo != null && !sendTo.equals("Sending to "))
                        sendTo += ", ";
                    else
                        sendTo = "Sending to ";
                }
                updateSliderMessageModelForRoom(entry.getKey(), uploadStatus);
            }
            sliderMessageListAdapter.notifyDataSetChanged();
        }
    }

    private void updateStatusInMomentList(MediaModel mediaModel, String mediaId, int uploadStatus) {
        if (mediaModel.addedTo.moments != null && mediaModel.addedTo.moments.size() > 0) {
            HashMap<String, Integer> momentList = mediaModel.addedTo.moments;
            for (Map.Entry<String, Integer> entry : momentList.entrySet()) {
                if (entry.getKey().equals(mFireBaseHelper.getMyUserModel().momentId)) {
                    updateMomentView(entry.getKey(), mFireBaseHelper.getMyStreams(), uploadStatus);
                    if (uploadStatus == MEDIA_UPLOADING_STARTED || uploadStatus == UPLOADING_NOT_READY_VIDEO) {
                        myStreamsTextView.setText(getString(R.string.posting_media_text));
                    } else if (uploadStatus == MEDIA_UPLOADING_COMPLETE) {
                    } else if (uploadStatus == MEDIA_UPLOADING_FAILED) {
                        myStreamsTextView.setText(getString(R.string.posting_media_failed_text));
                    }
                } else {
                    if (momentViewList.size() > 0 && momentViewList.get(entry.getKey()) != null) {
                        View view = momentViewList.get(entry.getKey());
                        TextView streamText = (TextView) view.findViewById(R.id.myStreamsText);
                        updateMomentView(entry.getKey(), mFireBaseHelper.getContributedStreams().get(entry.getKey()), uploadStatus);
                        if (uploadStatus == MEDIA_UPLOADING_STARTED || uploadStatus == UPLOADING_NOT_READY_VIDEO) {
                            streamText.setText(getString(R.string.posting_media_text));
                        } else if (uploadStatus == MEDIA_UPLOADING_COMPLETE) {
                        } else if (uploadStatus == MEDIA_UPLOADING_FAILED) {
                            streamText.setText(getString(R.string.posting_media_failed_text));
                        }
                    } else {
                        createAndUpdateMomentView(entry.getKey());
                    }
                    expandOnContributingToStream();
                }
            }
        }
    }

    private void expandOnContributingToStream() {
        expandStacks();
    }

    private void createAndUpdateMomentView(final String momentId) {
        if (AppLibrary.checkStringObject(momentId) != null) {
            LinkedHashMap<String, LinkedHashMap<String, MediaModelView>> momentList = mFireBaseHelper.getContributedStreams();
            momentList.put(momentId, null);
            View itemView = layoutInflater.inflate(R.layout.contributed_moment_tab, null);
            final ImageView momentImageView = ((ImageView) itemView.findViewById(R.id.myMomentsIV));
            final TextView name = (TextView) itemView.findViewById(R.id.momentName);
            ((TextView) itemView.findViewById(R.id.myStreamsText)).setText(getString(R.string.posting_media_text));
            if (CameraActivity.contributableMoments.get(momentId) != null) {
                if (AppLibrary.checkStringObject(CameraActivity.contributableMoments.get(momentId).thumbnailUrl) != null)
                    Picasso.with(getActivity()).load(CameraActivity.contributableMoments.get(momentId).thumbnailUrl)
                            .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().transform(new RoundedTransformation()).into(momentImageView);
                if (AppLibrary.checkStringObject(CameraActivity.contributableMoments.get(momentId).name) != null) {
                    String customMarquee = Utils.getCustomMarquee(CameraActivity.contributableMoments.get(momentId).name, 30);
//                    name.setText(CameraActivity.contributableMoments.get(momentId).name);
                    name.setText(customMarquee);
                }
            } else {
                mFireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId}).keepSynced(true);
                mFireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        MomentModel momentModel = dataSnapshot.getValue(MomentModel.class);
                        if (AppLibrary.checkStringObject(momentModel.thumbnailUrl) != null)
                            Picasso.with(getActivity()).load(momentModel.thumbnailUrl)
                                    .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().transform(new RoundedTransformation()).into(momentImageView);
                        if (AppLibrary.checkStringObject(momentModel.name) != null)
                            name.setText(momentModel.name);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
            }

            if (!momentViewList.containsKey(momentId)) {
                View arrow = itemView.findViewById(R.id.momentSliderIndicator);
                arrow.setOnClickListener(contributedMomentClickListener);
                itemView.setOnClickListener(contributedMomentClickListener);

                itemView.setTag(momentId);
                arrow.setTag(itemView);
                momentViewList.put(momentId, itemView);
                notifyContributedTabsAdded(itemView, false);
            }
        }
    }

    private void updateSliderMessageModelForRoom(String roomId, int status) {
        if (messageArrayList.containsKey(roomId)) {
            if (status == MEDIA_UPLOADING_STARTED || status == UPLOADING_NOT_READY_VIDEO) {
                messageArrayList.get(roomId).status = messageArrayList.get(roomId).setStatus(SENDING_MEDIA);
                sendTo += messageArrayList.get(roomId).displayName;
                messageTextView.setText(sendTo);
            } else if (status == MEDIA_UPLOADING_COMPLETE) {
                sendTo = null;
                messageArrayList.get(roomId).status = messageArrayList.get(roomId).setStatus(SENT_MEDIA);
                messageTextView.setText(getString(R.string.sent_message_text));
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        messageTextView.setText(getString(R.string.message_footer_text));
                    }
                }, 5000);
            } else if (status == MEDIA_UPLOADING_FAILED) {
                messageArrayList.get(roomId).status = messageArrayList.get(roomId).setStatus(SENDING_FAILED);
                messageTextView.setText(getString(R.string.message_sending_failed_text));
            }
        }
    }

    public void addAndUpdateMomentsStreamList(int uploadStatus, String mediaId, MediaModel mediaModel) {
        HashMap<String, Integer> momentMap = mediaModel.addedTo.moments;
        if (momentMap != null && momentMap.size() > 0) {
            LinkedHashMap<String, MediaModelView> streamList = null;
            String momentId;
            for (Map.Entry<String, Integer> entry : momentMap.entrySet()) {
                momentId = entry.getKey();
                if (entry.getKey().equals(mFireBaseHelper.getMyUserModel().momentId)) {
                    if (mFireBaseHelper.getMyStreams().containsKey(mediaId)) {
                        mFireBaseHelper.getMyStreams().get(mediaId).status = uploadStatus;
                        mFireBaseHelper.getMyStreams().get(mediaId).url = mediaModel.url;
                    } else {
                        MediaModelView mediaModelView = new MediaModelView(mediaModel.url, mediaModel.createdAt, mediaModel.type, mediaModel.totalViews,
                                mediaModel.viewerDetails, 0, null, entry.getKey(), mediaId, uploadStatus, mediaModel.mediaText, 0);
                        mediaModelView.mediaState = 0;
                        mFireBaseHelper.getMyStreams().put(mediaId, mediaModelView);
                    }
                    streamList = mFireBaseHelper.getMyStreams();
                    updateMomentView(momentId, streamList, MEDIA_UPLOADING_STARTED);
                } else {
                    LinkedHashMap<String, LinkedHashMap<String, MediaModelView>> contributedMomentList = mFireBaseHelper.getContributedStreams();
                    if (contributedMomentList.containsKey(momentId) && contributedMomentList.get(momentId) != null
                            && contributedMomentList.get(momentId).containsKey(mediaId)) {
                        contributedMomentList.get(momentId).get(mediaId).url = mediaModel.url;
                        contributedMomentList.get(momentId).get(mediaId).status = uploadStatus;
                    } else {
                        MediaModelView mediaModelView = new MediaModelView(mediaModel.url, mediaModel.createdAt, mediaModel.type, mediaModel.totalViews,
                                mediaModel.viewerDetails, 0, null, momentId, mediaId, uploadStatus, mediaModel.mediaText, 0);
                        boolean isAutoModerate = false;
                        if (CameraActivity.contributableMoments.containsKey(momentId))
                            isAutoModerate = CameraActivity.contributableMoments.get(momentId).autoModerate;
                        if (isAutoModerate)
                            mediaModelView.mediaState = MEDIA_ACTIVE;
                        else
                            mediaModelView.mediaState = MEDIA_INACTIVE;
                        if (contributedMomentList.containsKey(momentId) && contributedMomentList.get(momentId) != null) {
                            contributedMomentList.get(momentId).put(mediaId, mediaModelView);
                            streamList = contributedMomentList.get(momentId);
                        } else {
                            LinkedHashMap<String, MediaModelView> mediaList = new LinkedHashMap<>();
                            mediaList.put(mediaId, mediaModelView);
                            contributedMomentList.put(momentId, mediaList);
                            streamList = mediaList;
                        }
                    }
                }
                if (activeMoment != null) {
                    contributedToMomentSliderFragment.setMomentId(activeMoment);
                    if (activeMoment.equals(mFireBaseHelper.getMyUserModel().momentId)) {
                        contributedToMomentSliderFragment.setMediaList(sortMyStreams(mFireBaseHelper.getMyStreams()));
                    } else {
                        LinkedHashMap<String, MediaModelView> list = mFireBaseHelper.getContributedStreams().get(activeMoment);
                        contributedToMomentSliderFragment.setMediaList(sortMyStreams(list));
                    }
                }
            }
        }
    }

    public void updateOnRoomOpen(String roomId) {
        int messageType = messageArrayList.get(roomId).roomOpened();
        mFireBaseHelper.updateSliderStatus(messageType, roomId);
        messageArrayList.get(roomId).status = messageArrayList.get(roomId).setStatus(messageType);
    }

    public void updateLatestMediaTime(String time, String momentId) {
        if (momentId.equals(mFireBaseHelper.getMyUserModel().momentId) && !myStreamsTextView.getText().toString().equals(getString(R.string.posting_media_failed_text)))
            myStreamsTextView.setText(time);
        else if (momentViewList.containsKey(momentId)) {
            View view = momentViewList.get(momentId);
            TextView textView = (TextView) view.findViewById(R.id.myStreamsText);
            if (!textView.getText().toString().equals(getString(R.string.posting_media_failed_text))) {
                textView.setText(time);
            }
        }
    }

    public void onDeleteMyMomentMedia() {
        if (mFireBaseHelper.getMyStreams().size() <= 0)
            if (myStreamsView.findViewById(R.id.momentSliderIndicator).getScaleY() == 1f)
                myStreamsView.findViewById(R.id.momentSliderIndicator).performClick();
        if (mFireBaseHelper.getPendingMediaInMyMoment().size() == 0 && mFireBaseHelper.getMyStreams().size() == 0)
            myStreamsTextView.setText(getString(R.string.add_photo_video));
    }

    @Override
    public void onDownloadStatusChanges(String momentId, String mediaId, int status) {
        // update my moment media download status
        if (momentId.equals(mFireBaseHelper.getMyUserModel().momentId)) {
            // my moment medias
            if (mFireBaseHelper.getMyStreams().containsKey(mediaId)) {
                mFireBaseHelper.getMyStreams().get(mediaId).status = status;
            }
            updateMomentView(mFireBaseHelper.getMyUserModel().momentId, mFireBaseHelper.getMyStreams(), status);
        } else {
            if (mFireBaseHelper.getContributedStreams().containsKey(momentId) && mFireBaseHelper.getContributedStreams().get(momentId).containsKey(mediaId)) {
                mFireBaseHelper.getContributedStreams().get(momentId).get(mediaId).status = status;
            }
        }
        contributedToMomentSliderFragment.onDownloadStatusChanges(momentId, mediaId, status);
    }

    public void updateInstitutionName(MomentModel instituteMomentModel) {

        if (getActivity() == null) return;

        getPreferences().edit().putBoolean(AppLibrary.INSTITUTE_HIDDEN_BY_USER, false).commit();

        View view;
        String momentId = null;
        if ((view = contributionLl.findViewWithTag("mInstituteStream")) != null) {
            contributionLl.removeView(view);
            if (view.getTag(R.id.momentName) != null) {
                momentId = (String) view.getTag(R.id.momentName);
                momentViewList.remove(momentId);
            }
        }

        if (getPreferences().getBoolean(AppLibrary.INSTITUTION_NEEDED, true) && instituteMomentModel != null &&
                instituteMomentModel.flags.type == FireBaseKEYIDS.INSTITUTION_ACTIVE) {
            if ((contributionLl.findViewWithTag("mInstituteStream")) != null) {
                if (myInstituteStream != null) {
                    myInstituteStreamName.setText(Utils.getCustomMarquee(instituteMomentModel.name, 30));
                    Picasso.with(getActivity()).load(instituteMomentModel.thumbnailUrl).transform(new RoundedTransformation()).resize(Utils.dp2Px(40), Utils.dp2Px(40))
                            .centerCrop().into(myInstituteMomentsIV);
                    contributionLl.findViewWithTag("mInstituteStream").setTag(R.id.momentName, instituteMomentModel.momentId);
                    momentViewList.put(instituteMomentModel.momentId, contributionLl.findViewWithTag("mInstituteStream"));
                    if (((String) myInstituteStreamMomentSliderArrowIv.getTag()).equals("collegeNotSelected"))
                        myInstituteStreamMomentSliderArrowIv.setTag(myInstituteStream);
                }
            } else {
                addInstituteStreamIfNeeded(instituteMomentModel);
            }
        } else {
            contributionLl.removeView(myInstituteStream);
            myInstituteStream = null;
        }

        refreshMomentList(instituteMomentModel != null ? instituteMomentModel.momentId : null);
    }

    /*
    remove moment stream if college stream was previously used and now selected as Institution
     */
    private void refreshMomentList(String momentId) {
        if (momentId != null) {
            for (int i = 0; i < contributionLl.getChildCount(); i++) {
                if (contributionLl.getChildAt(i).getTag() == null)
                    continue;
                if ((contributionLl.getChildAt(i).getTag()).equals(momentId)) {
                    contributionLl.removeView(contributionLl.getChildAt(i));
                }
            }

            updateLatestMediaTimeInstitution(momentId);
        }
    }


    private void updateLatestMediaTimeInstitution(final String momentId) {
        if (mFireBaseHelper == null || mFireBaseHelper.getContributedStreams().get(momentId) == null)
            return;

        if (mFireBaseHelper.getFirebaseHelperHandler() != null) {
            mFireBaseHelper.getFirebaseHelperHandler().post(new Runnable() {
                @Override
                public void run() {
                    final LinkedHashMap<String, MediaModelView> streamData = new LinkedHashMap<>(mFireBaseHelper.getContributedStreams().get(momentId));

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (MediaModelView m : streamData.values()) {
                                    if (myInstituteStreamsTextView != null)
                                        myInstituteStreamsTextView.setText(AppLibrary.timeAccCurrentTime(m.createdAt));
                                }
                            }
                        });
                    }
                }
            });
        }
    }


    @Override
    public void onInstituteLoaded(MomentModel instituteMoment) {
        updateInstitutionName(instituteMoment);
    }

    @Override
    public void onHideRecentUpdateHeader() {
        if (unseenMomentsHeaderLayout != null)
            unseenMomentsHeaderLayout.setVisibility(View.GONE);
    }

    @Override
    public void onSettingsClicked() {
        ((CameraActivity) getActivity()).openSettingsFragment();
    }

    @Override
    public void onShowRecentUpdatesHeader() {
        if (unseenMomentsHeaderLayout != null)
            unseenMomentsHeaderLayout.setVisibility(View.VISIBLE);
    }

    public void updateFollowersList(boolean follow) {
        if (getFriendsMomentsFragment() != null)
            getFriendsMomentsFragment().updateFollowersList(follow);
    }

    public interface ViewControlsCallback {
        void onUploadRetryClickedForAllMediaInMoments();

        void onUploadRetryClickedForAllMediaMessages();
    }

    SearchView searchView;

    SearchFragment searchFragment;
    boolean searchFragmentLoaded;

    private void initSearchView(final View rootView) {
        searchFrame = (FrameLayout) rootView.findViewById(R.id.searchFrame);
        searchView = (SearchView) rootView.findViewById(R.id.action_bar).findViewById(R.id.action_bar_search_view);
        searchView.setVisibility(View.VISIBLE);
        if (searchFragment == null)
            searchFragment = new SearchFragment();
        searchFragment.searchSource = SearchFragment.SearchSource.DASHBOARD;
        searchFragment.setDashBoardFragmentReference(this);
        searchFragment.initSearchView(rootView);
        searchFragment.registerSearchView(searchView);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DynamicDownloader.notifyScrollDetectedInDashboard(); //Clear waiting list just in case
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.GLOBAL_SEARCH);

                toggleAllFriendSearchSearchMode(true);
                if (searchFragmentLoaded) return;
                searchFragmentLoaded = true;
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.searchFrame, searchFragment, SearchFragment.class.getSimpleName());
//                fragmentTransaction.addToBackStack(SearchFragment.class.getSimpleName());
                fragmentTransaction.commitAllowingStateLoss();
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, " onClose ");
                searchView.onActionViewCollapsed();
                toggleAllFriendSearchSearchMode(false);
                return true;
            }
        });
    }

    boolean resetNotificationVisibility;

    private void toggleAllFriendSearchSearchMode(boolean enterSearchMode) {
        searchFrame.setVisibility(enterSearchMode ? View.VISIBLE : View.GONE);
        rootView.findViewById(R.id.titleTV).setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);

        if (enterSearchMode)
            ((ImageView) (rootView.findViewById(R.id.action_bar_IV_1))).setImageResource(R.drawable.back_svg);
        else
            ((ImageView) (rootView.findViewById(R.id.action_bar_IV_1))).setImageResource(R.drawable.add_friends_svg);

        rootView.findViewById(R.id.action_bar_IV_4).setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.titleTV).setVisibility(enterSearchMode ? View.GONE : View.VISIBLE);

        if (enterSearchMode) {
            if (unreadRequestCountTextView.getVisibility() == View.VISIBLE) {
                resetNotificationVisibility = true;
                unreadRequestCountTextView.setVisibility(View.GONE);
            } else {
                resetNotificationVisibility = false;
            }
        } else {
            if (resetNotificationVisibility)
                unreadRequestCountTextView.setVisibility(View.VISIBLE);
        }

        if (enterSearchMode)
            searchFragment.refreshDataOnToggle();
    }

    public void exitSearchMode() {
        toggleAllFriendSearchSearchMode(false);
        if (searchView != null)
            searchView.onActionViewCollapsed();
    }


    //    RecyclerView contributedMediaRecycler;
    private FrameLayout contributionLl;
    private FrameLayout notificationBackGroundFl;
    private TextView totalScreenShotTv, totalViewsTv;
    private TextView myStreamsTextView;
    private ImageView myMomentsIV;
    private ImageView myMomentSliderArrowIv;
    private LinearLayout streamStatsLinearLayout;
    private FrameLayout contributedMediaFrame;
    private LinkedHashMap<String, View> momentViewList = new LinkedHashMap<>();
    private UserModel myUserModel;

    private void initContributedMomentViews() {
        contributionLl = (FrameLayout) rootView.findViewById(R.id.contributedMomentsFL);

        notificationBackGroundFl = (FrameLayout) rootView.findViewById(R.id.notificationBackGroundFL);
        contributedMediaFrame = (FrameLayout) rootView.findViewById(R.id.contributedMomentsFrame);

        myStreamsView = layoutInflater.inflate(R.layout.contributed_moment_tab, null);

//        contributionLl.addView(myStreamsView);
        notifyContributedTabsAdded(myStreamsView, false);
        myStreamsView.setTag("mStream");

        if (!getPreferences().getBoolean(AppLibrary.INSTITUTE_HIDDEN_BY_USER, false)) {
            MomentModel instituteMomentModel = null;
            if (myUserModel != null && myUserModel.miscellaneous != null && myUserModel.miscellaneous.institutionData != null)
                instituteMomentModel = mFireBaseHelper.getInstituteMomentModel(myUserModel.miscellaneous.institutionData.momentId);
            addInstituteStreamIfNeeded(instituteMomentModel);
        }
        myMomentSliderArrowIv = (ImageView) myStreamsView.findViewById(R.id.momentSliderIndicator);
        myMomentSliderArrowIv.setTag(myStreamsView);

        myStreamsTextView = (TextView) myStreamsView.findViewById(R.id.myStreamsText);
        myMomentsIV = (ImageView) myStreamsView.findViewById(R.id.myMomentsIV);
        streamStatsLinearLayout = (LinearLayout) myStreamsView.findViewById(R.id.streamStatsLL);

        myStreamsView.setOnClickListener(contributedMomentClickListener);
        myMomentSliderArrowIv.setOnClickListener(contributedMomentClickListener);
        loadContributedToMomentSliderFragment();

        contributedToMomentSliderFragment.setActionListener(new ContributedToMomentSliderFragment.MomentSliderActionListener() {

            @Override
            public void onCloseMomentsFragment() {
                contributedMediaFrame.setVisibility(View.GONE);
                lockableScrollView.setScrollable(true);
            }
        });
    }

    private void addInstituteStreamIfNeeded(MomentModel instituteMomentModel) {
        if (getPreferences().getBoolean(AppLibrary.INSTITUTION_NEEDED, true)) {
            myInstituteStream = layoutInflater.inflate(R.layout.contributed_moment_tab, null);
            myInstituteStream.setTag("mInstituteStream");

            myInstituteStreamName = (TextView) myInstituteStream.findViewById(R.id.momentName);
            myInstituteStreamMomentSliderArrowIv = ((ImageView) myInstituteStream.findViewById(R.id.momentSliderIndicator));
            myInstituteStreamMomentSliderArrowIv.setTag(myInstituteStream);
            myInstituteStreamsTextView = (TextView) myInstituteStream.findViewById(R.id.myStreamsText);
            myInstituteMomentsIV = (ImageView) myInstituteStream.findViewById(R.id.myMomentsIV);
            institutesStreamStatsLinearLayout = (LinearLayout) myInstituteStream.findViewById(R.id.streamStatsLL);
            myInstituteStreamsTextView.setText("+ Contribute to your institution stream");
//            contributionLl.addView(myInstituteStream, contributionLl.getChildCount());
            notifyContributedTabsAdded(myInstituteStream, true);
//            addMarginsToViews(myInstituteStream);
            myInstituteStreamMomentSliderArrowIv.setOnClickListener(contributedMomentClickListener);


            if ((myUserModel.miscellaneous == null || myUserModel.miscellaneous.institutionData == null)
                    && instituteMomentModel == null) {
                myInstituteStreamName.setText("Add your Institution");
                myInstituteStreamMomentSliderArrowIv.setImageResource(R.drawable.more_pulse_color_svg);
                myInstituteStreamMomentSliderArrowIv.setTag("collegeNotSelected");
                myInstituteMomentsIV.setImageResource(R.drawable.institution_svg);
                myInstituteStream.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openInstituteEditFragment();
                    }
                });
            } else if (instituteMomentModel != null) {
                String name = Utils.getCustomMarquee(instituteMomentModel.name, 30);
                myInstituteStreamName.setText(name);
                myInstituteStream.setTag(R.id.momentName, instituteMomentModel.momentId);
                momentViewList.put(instituteMomentModel.momentId, myInstituteStream);
//                myInstituteMomentsIV.setImageResource(R.drawable.institution_color_svg);
                Picasso.with(getActivity()).load(instituteMomentModel.thumbnailUrl).transform(new RoundedTransformation())
                        .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().into(myInstituteMomentsIV);
                myInstituteStream.setOnClickListener(contributedMomentClickListener);
                myInstituteStream.findViewById(R.id.iv_flag).setVisibility(View.VISIBLE);
                updateLatestMediaTimeInstitution(instituteMomentModel.momentId);
                if (mFireBaseHelper.getContributableDownloadedStreams(instituteMomentModel.momentId).size() == 0) {
                    myInstituteStreamMomentSliderArrowIv.setVisibility(View.GONE);
                }
            }


        }
    }

    private void openInstituteEditFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragmentContainer, new InstitutionEditFragment(), InstitutionEditFragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(InstitutionEditFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void loadContributedToMomentSliderFragment() {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        contributedToMomentSliderFragment = contributedToMomentSliderFragment == null ? new ContributedToMomentSliderFragment() : contributedToMomentSliderFragment;
        fragmentTransaction.replace(R.id.contributedMomentsFrame, contributedToMomentSliderFragment, ContributedToMomentSliderFragment.class.getSimpleName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    LayoutInflater layoutInflater;

    public void onContributionDataLoaded(final String momentId, final String mediaId, final LinkedHashMap<String, CustomMomentModel> momentList, final LinkedHashMap<String, LinkedHashMap<String, MediaModelView>> map) {

        if (getActivity() == null || mFireBaseHelper.getMyUserModel() == null) return;

        if (rootView == null || contributionLl == null) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onContributionDataLoaded(momentId, mediaId, momentList, map);
                }
            }, 100);
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!momentViewList.containsKey(momentId)) {
                    View itemView = layoutInflater.inflate(R.layout.contributed_moment_tab, null);
                    ImageView momentImageView = ((ImageView) itemView.findViewById(R.id.myMomentsIV));
                    TextView momentName = (TextView) itemView.findViewById(R.id.momentName);
                    if (momentList.get(momentId) != null) {
                        Picasso.with(getActivity()).load(momentList.get(momentId).thumbnailUrl)
                                .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().transform(new RoundedTransformation()).into(momentImageView);

                        momentName.setText(Utils.getCustomMarquee(momentList.get(momentId).name, 30));
                    }

                    View arrow = itemView.findViewById(R.id.momentSliderIndicator);
                    arrow.setOnClickListener(contributedMomentClickListener);
                    itemView.setOnClickListener(contributedMomentClickListener);

                    itemView.setTag(momentId);
                    arrow.setTag(itemView);
                    momentViewList.put(momentId, itemView);
//                    contributionLl.addView(itemView);
//                    addMarginsToViews(itemView);
                    notifyContributedTabsAdded(itemView, false);
                }
            }
        });
        updateMomentView(momentId, map.get(momentId), MEDIA_UPLOADING_COMPLETE);
        refreshMyStreamsStats(momentId, map.get(momentId));

        updateForPendingMediaUploads();
        if (myInstituteStream != null && momentId.equals(myInstituteStream.getTag(R.id.momentName))) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateLatestMediaTimeInstitution(momentId);
                }
            });
        }
//        if (false)//for debugging the views do not remove
//            for (int i = 0; i < 5; i++) {
//                @SuppressLint("InflateParams")
//                View itemView = layoutInflater.inflate(R.layout.contributed_moment_tab, null);
//                View arrow = itemView.findViewById(R.id.momentSliderIndicator);
//                arrow.setTag(itemView);
//                itemView.setTag(null);
//                arrow.setOnClickListener(contributedMomentClickListener);
//                itemView.setOnClickListener(contributedMomentClickListener);
//                ((TextView) itemView.findViewById(R.id.momentName)).setText(i + " stream");
//                contributionLl.addView(itemView);
//            }

    }


    private void updateMomentView(final String momentId, final LinkedHashMap<String, MediaModelView> streamList, final int mediaStatus) {
        if (momentId != null && streamList != null && streamList.size() > 0) {
            LinkedHashMap<String, MediaModelView> sortedStream = sortMyStreams(streamList);
            final Map.Entry<String, MediaModelView> firstEntry;
            List<Map.Entry<String, MediaModelView>> entryList =
                    new ArrayList<>(sortedStream.entrySet());
            firstEntry = entryList.get(0);

            if (getActivity() == null) return;

            if (momentId.equals(mFireBaseHelper.getMyUserModel().momentId)) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (myMomentSliderArrowIv != null)
                            myMomentSliderArrowIv.setVisibility(streamList.size() > 0 ? View.VISIBLE : View.GONE);
                    }
                });
                if (mediaStatus == MEDIA_UPLOADING_COMPLETE) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (myStreamsTextView != null)
                                myStreamsTextView.setText(AppLibrary.timeAccCurrentTime(firstEntry.getValue().createdAt));
                        }
                    });
                }
                if (AppLibrary.checkStringObject(firstEntry.getValue().url) != null) {
                    if (AppLibrary.getMediaType(firstEntry.getValue().url) == AppLibrary.MEDIA_TYPE_IMAGE) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Picasso.with(getActivity()).load(new File(firstEntry.getValue().url)).transform(new RoundedTransformation())
                                        .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().error(R.drawable.moment_circle_background).into(myMomentsIV);
                            }
                        });
                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(context).load(new File(firstEntry.getValue().url)).centerCrop().bitmapTransform(new jp.wasabeef.glide.transformations.CropCircleTransformation(context)).error(R.drawable.moment_circle_background).into(myMomentsIV);
                            }
                        });
                    }
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.with(getActivity()).load(R.drawable.moment_circle_background).transform(new RoundedTransformation())
                                    .resize(Utils.dp2Px(40), Utils.dp2Px(40)).centerCrop().into(myMomentsIV);
                        }
                    });
                }
            } else if (myInstituteStream != null && momentId.equals(myInstituteStream.getTag(R.id.momentName))) {
                if (myInstituteStreamMomentSliderArrowIv != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String s = (String) myInstituteStream.getTag(R.id.momentName);
                            myInstituteStreamMomentSliderArrowIv.setVisibility((mFireBaseHelper.getContributedStreams().get(s)).size() > 0 ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (momentViewList != null && momentViewList.containsKey(momentId)) {
                            View itemView = momentViewList.get(momentId);

                            if (itemView != null) {
                                itemView.findViewById(R.id.momentSliderIndicator).setVisibility(streamList.size() > 0 ? View.VISIBLE : View.GONE);
                                if (mediaStatus == MEDIA_UPLOADING_COMPLETE)
                                    ((TextView) itemView.findViewById(R.id.myStreamsText)).setText(AppLibrary.timeAccCurrentTime(firstEntry.getValue().createdAt));
                            }
                        }
                    }
                });
            }
        }
    }

    private View.OnClickListener customBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_positive:
                    removeInstituteFromMoments();
                    dialog.dismiss();
                    break;
                case R.id.btn_negative:
                    dialog.dismiss();
                    break;
            }
        }
    };

    private void removeInstituteFromMoments() {
        contributionLl.removeView(myInstituteStream);
        myInstituteStream = null;
        getPreferences().edit().putBoolean(AppLibrary.INSTITUTE_HIDDEN_BY_USER, true).commit();
        refreshTopMarginForContributionViews();
    }

    private Dialog dialog;
    private boolean momentSliderArrowClicked;

    private View.OnClickListener contributedMomentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof ImageView) {//arrow indicator
                momentSliderArrowClicked = true;
                if (v.getTag() instanceof String && v.getTag().equals("collegeNotSelected")) {
                    dialog = showCustomDialog(true, getActivity(), getString(R.string.institute_dialog_header), getString(R.string.institute_dialog_body_text)
                            , getString(R.string.institute_dialog_positive_btn), getString(R.string.institute_dialog_negative_btn), customBtnListener);
                } else {
                    View itemView = ((View) v.getTag());
                    ImageView iv = (ImageView) v;
                    toggleUpperSlider(iv, itemView.getTag());
                }
            } else if (v instanceof RelativeLayout) {//itemView
                if (v.getTag() instanceof String && ((String) v.getTag()).equals("mStream")) {
                    lockableScrollView.setScrollY(0);
                    if (mFireBaseHelper.getMyStreams() != null && mFireBaseHelper.getMyStreams().size() > 0) {
                        if (myStreamsTextView.getText().toString().equals(getString(R.string.posting_media_failed_text)))
                            viewControlsCallback.onUploadRetryClickedForAllMediaInMoments();
                        else {
                            if (mFireBaseHelper.getMyDownloadedStreams().size() > 0) {
                                // launch view my media fragment
                                ((CameraActivity) context).loadViewMyMediaFragment(null, mFireBaseHelper.getMyUserModel().momentId);
                            }
                        }
                    } else ((CameraActivity) getActivity()).scrollToCameraFragment();
                } else if (v.getTag() instanceof String && ((String) v.getTag()).equals("mInstituteStream")) {
                    String momentId = (String) v.getTag(R.id.momentName);
                    lockableScrollView.setScrollY(0);
                    if (AppLibrary.checkStringObject(momentId) != null) {
                        if (((TextView) v.findViewById(R.id.myStreamsText)).getText().toString().equals(getString(R.string.posting_media_failed_text)))
                            viewControlsCallback.onUploadRetryClickedForAllMediaInMoments();
                        else {
                            //noinspection StatementWithEmptyBody
                            if (mFireBaseHelper.getContributableDownloadedStreams(momentId).size() > 0) {
                                // launch view my media fragment
                                ((CameraActivity) context).loadViewMyMediaFragment(null, momentId);
                            } else {
                                ((CameraActivity) getActivity()).scrollToCameraFragment();
                            }
                        }
                    }
                } else {//open public streams here
                    String momentId = (String) v.getTag();
                    lockableScrollView.setScrollY(0);
                    if (AppLibrary.checkStringObject(momentId) != null) {
                        if (((TextView) v.findViewById(R.id.myStreamsText)).getText().toString().equals(getString(R.string.posting_media_failed_text)))
                            viewControlsCallback.onUploadRetryClickedForAllMediaInMoments();
                        else {
                            //noinspection StatementWithEmptyBody
                            if (mFireBaseHelper.getContributableDownloadedStreams(momentId).size() > 0) {
                                // launch view my media fragment
                                ((CameraActivity) context).loadViewMyMediaFragment(null, momentId);
                            } else {
                                // do nothing
                            }
                        }
                    } else ((CameraActivity) getActivity()).scrollToCameraFragment();
                }
            }
        }
    };

    private void toggleUpperSlider(ImageView view, Object tag) {
        ((View) view.getTag()).findViewById(R.id.streamStatsLL).setVisibility(View.VISIBLE);
        ((View) view.getTag()).findViewById(R.id.myStreamsText).setVisibility(View.GONE);
        if (contributedToMomentSliderFragment.mIsAnimating) {
            Log.e(TAG, "animation under progress returning");
            return;
        }
        if (contributedMediaFrame.getVisibility() == View.VISIBLE) {
            resetIndicators(view, true);
            contributedToMomentSliderFragment.animateRecycler(ContributedToMomentSliderFragment.AnimationDirection.SLIDE_UP);
            ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f).setDuration(300).start();
        } else {
            resetIndicators(view, false);
            contributedMediaFrame.setVisibility(View.VISIBLE);
            lockableScrollView.setScrollable(false);
            expandStacks();
            contributedToMomentSliderFragment.setUpperTintMargins((((int) ((View) view.getTag()).getTop()) /*- lockableScrollView.getScrollY()*/), ((View) view.getTag()).getHeight());
            contributedToMomentSliderFragment.setCurrentIndicator(view);

            if (tag instanceof String && tag.equals("mStream")) {
                activeMoment = mFireBaseHelper.getMyUserModel().momentId;
                contributedToMomentSliderFragment.setMomentId(mFireBaseHelper.getMyUserModel().momentId);
                contributedToMomentSliderFragment.setMediaList(sortMyStreams(mFireBaseHelper.getMyStreams()));
            } else if (tag instanceof String && tag.equals("mInstituteStream")) {
                String momentId = ((String) contributionLl.findViewWithTag("mInstituteStream").getTag(R.id.momentName));
                activeMoment = momentId;
                contributedToMomentSliderFragment.setMomentId(momentId);
                contributedToMomentSliderFragment.setMediaList(sortMyStreams(mFireBaseHelper.getContributedStreams().get(momentId)));
            } else {//noinspection unchecked
                String momentId = (String) tag;
                activeMoment = momentId;
                LinkedHashMap<String, MediaModelView> contributedList = mFireBaseHelper.getContributedStreams().get(momentId);
                contributedToMomentSliderFragment.setMomentId(momentId);
                contributedToMomentSliderFragment.setMediaList(sortMyStreams(contributedList));
            }
            contributedToMomentSliderFragment.animateRecycler(ContributedToMomentSliderFragment.AnimationDirection.SLIDE_DOWN);
        }
    }

    private void resetIndicators(View indicatorView, boolean defaultPosition) {
        for (int i = 0; i < contributionLl.getChildCount(); i++) {
            ImageView currentIndicator = (ImageView) contributionLl.getChildAt(i).findViewById(R.id.momentSliderIndicator);
            if (currentIndicator != indicatorView)
                currentIndicator.setScaleY(-1f);
            else {
                float[] scales;
                if (defaultPosition)
                    scales = new float[]{1f, -1f};
                else scales = new float[]{-1f, 1f};
                ObjectAnimator.ofFloat(currentIndicator, "scaleY", scales[0], scales[1]).setDuration(300).start();
            }
        }
    }

    /**
     * @param userId corresponding to the moment being opened
     */
    public void onMomentClickedFromSearch(String userId) {

        String momentId = null;
        final HashMap<String, SocialModel.Friends> friends = mFireBaseHelper.getSocialModel().friends;
        if (friends != null)
            for (Map.Entry<String, SocialModel.Friends> entry : friends.entrySet())
                if (entry.getKey().equals(userId)) {
                    momentId = entry.getValue().momentId;//found a friend here
                    break;
                }


        if (momentId != null && getFriendsMomentsFragment() != null) {//if found a friend search
            final boolean ready = getFriendsMomentsFragment().notifyTapOnMoment(momentId, UNSEEN_FRIEND_MOMENT_RECYCLER);

            this.exitSearchMode();
        } else {//didn't find friend  ? search followers now
            if (getFriendsMomentsFragment() != null && getFriendsMomentsFragment().getMomentIdFromUserId(userId) != null) {
                momentId = getFriendsMomentsFragment().getMomentIdFromUserId(userId);
                final boolean ready = getFriendsMomentsFragment().notifyTapOnMoment(momentId, UNSEEN_FOLLOWER_MOMENT_RECYCLER);
            }
            this.exitSearchMode();

        }
    }

    private FriendsMomentsFragment friendsMomentsFragment;

    public FriendsMomentsFragment getFriendsMomentsFragment() {
        return friendsMomentsFragment;
    }

    private void loadFriendsMoment() {
        friendsMomentsFragment = new FriendsMomentsFragment();
        Bundle data = new Bundle();
        data.putInt(MOMENT_VIEW_TYPE, UNSEEN_FRIEND_MOMENT_RECYCLER);
        friendsMomentsFragment.setArguments(data);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.ll1, friendsMomentsFragment, "friendsMomentsFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

//    View lastContributedMomentView;
//    int contributedViewHeight;

    Handler handler = new Handler();

    private void notifyContributedTabsAdded(View itemView, boolean isInstitution) {
//        if (tabcount > 4) return;

        if (isInstitution)
            contributionLl.addView(itemView, 0);
        else contributionLl.addView(itemView, 0);

        addMarginsToViews(itemView);

        int tabcount = contributionLl.getChildCount();
        Log.d(TAG, "notifyContributedTabsAdded: " + contributionLl.getChildCount());
        Log.d(TAG, "notifyContributedTabsAdded: tabcount " + tabcount);
        if (contributionLl.getChildCount() > 2 && lockableScrollView != null && !lockableScrollView.isTouched()) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!momentSliderArrowClicked && !lockableScrollView.isTouched())
                        contractStacks();
                }
            }, 4000);
        }

    }


    private void addMarginsToViews(View itemView) {
        ((FrameLayout.LayoutParams) itemView.getLayoutParams()).leftMargin = AppLibrary.convertDpToPixels(getActivity(), 0);
        ((FrameLayout.LayoutParams) itemView.getLayoutParams()).rightMargin = AppLibrary.convertDpToPixels(getActivity(), 0);
        ((FrameLayout.LayoutParams) itemView.getLayoutParams()).bottomMargin = AppLibrary.convertDpToPixels(getActivity(), 2);

        int TAB_HEIGHT = 60;//inclusive of shadow and shit
        ((FrameLayout.LayoutParams) itemView.getLayoutParams()).height = AppLibrary.convertDpToPixels(getActivity(), TAB_HEIGHT);
        notificationBackGroundFl.requestLayout();
        refreshTopMarginForContributionViews();
        notificationBackGroundFl.requestLayout();
    }

    /**
     * As all the tabs wil be of same dimensions for calculation purposes we take the tab at index 0
     * for calculating the variable tab height
     */
    private void refreshTopMarginForContributionViews() {
        int viewsTopOfCurrentView = 0;
        for (int i = contributionLl.getChildCount() - 1; i >= 0; i--) {
            if (viewsTopOfCurrentView == 0) {//visibly top most view

            } else
                ((FrameLayout.LayoutParams) contributionLl.getChildAt(i).getLayoutParams()).topMargin = convertDpToPixels(EFFECTIVE_TAB_HEIGHT * viewsTopOfCurrentView);
            ++viewsTopOfCurrentView;

            if (i == 0) {//visibly bottom most view
                ((FrameLayout.LayoutParams) contributionLl.getChildAt(i).getLayoutParams()).bottomMargin = -convertDpToPixels(2);
            } else {
                ((FrameLayout.LayoutParams) contributionLl.getChildAt(i).getLayoutParams()).bottomMargin = 0;
            }
        }
    }

    long previousTimeMilli;

    private NestedScrollView.OnScrollChangeListener scrollChangeListener = new NestedScrollView.OnScrollChangeListener() {
        @Override
        public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            mScrolled = true;
            currentScrollY = scrollY;
            mOldScrollY = oldScrollY;
//            Log.d(TAG, " scrollChangeListener  scrollY " + scrollY + " oldScrollY " + oldScrollY);
            long t = System.currentTimeMillis();
//            Log.d(TAG, "onScrollChange: time" + (t - previousTimeMilli));
            previousTimeMilli = t;
            notifyScrolled(scrollY, oldScrollY);
//            hasMyStreamViewDisplaced();
            putInCache(scrollY);

        }
    };
    private LockableScrollView.MotionEventChangeListener motionEventChangeListener = new LockableScrollView.MotionEventChangeListener() {
        @Override
        public void onOnActionChanged(int newAction) {
            Log.d(TAG, "onOnActionChanged: " + BaseFragment.getActionName(newAction));
            if (newAction == MotionEvent.ACTION_UP || newAction == MotionEvent.ACTION_CANCEL) {
                calculateTheSpeedOnFingerUp();
            }
            if (newAction == MotionEvent.ACTION_DOWN) {
                scrollYAtFingerDown = currentScrollY;
            }
        }
    };


    private void notifyScrolled(int scrollY, int oldScrollY) {
        if (contributionLl.getChildCount() == 0)
            return;

        for (int j = 0; j < contributionLl.getChildCount(); j++) {
            contributionLl.getChildAt(j).setTranslationY(getTranslationForTab(j, scrollY));
        }
    }


    private int getTranslationForTab(int index, int scrollY) {
        int translation = 0;

        if (index == 0)
            translation = 0;
        else {
            if (scrollY < getCutOffTranslationForTab(index))
                translation = scrollY;
            else
                translation = getCutOffTranslationForTab(index);
        }
        return translation;
    }

    private int getCutOffTranslationForTab(int index) {
        if (index == 0)
            return 0;
        return convertDpToPixels((EFFECTIVE_TAB_HEIGHT - OVERLAP_DISTANCE) * index);
    }

    private final int EFFECTIVE_TAB_HEIGHT = 60;//item height + distance between bottom card
    private final int OVERLAP_DISTANCE = 6;


    private int getScrollForContractingTheStacks() {
        int displacementFromTop = convertDpToPixels(EFFECTIVE_TAB_HEIGHT + (contributionLl.getChildCount() - 2) * OVERLAP_DISTANCE + 14);
        Log.d(TAG, "getScrollForContractingTheStacks: displacement from top " + displacementFromTop + " totalHeight " + notificationBackGroundFl.getMeasuredHeight() + " count " + contributionLl.getChildCount());
        return notificationBackGroundFl.getMeasuredHeight() - displacementFromTop;
    }


    private int scrollTo;
    Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            lockableScrollView.smoothScrollTo(0, scrollTo);
        }
    };

    public void expandStacks() {
        scrollTo = 0;
        lockableScrollView.postDelayed(scrollRunnable, 100);
    }

    public void contractStacks() {
        scrollTo = getScrollForContractingTheStacks();
        Log.d(TAG, "contractStacks: " + scrollTo);
        lockableScrollView.postDelayed(scrollRunnable, 100);
    }

    private boolean mScrolled = false;
//    private LruCache<Long, Integer> scrollTracker = new LruCache<>(30);

    private void putInCache(int scrollY) {
//        scrollTracker.put(System.currentTimeMillis(), scrollY);
    }

    private void calculateTheSpeedOnFingerUp() {
//        Long[] longs = scrollTracker.snapshot().keySet().toArray(new Long[scrollTracker.snapshot().size()]);
//        Integer[] ints = scrollTracker.snapshot().values().toArray(new Integer[scrollTracker.snapshot().size()]);
//        long time = longs[longs.length - 1] - longs[longs.length - 2];
//        int distance = ints[ints.length - 1] - ints[ints.length - 2];
//        Log.d(TAG, "calculateTheSpeedOnFingerUp: dist " + distance + " time " + time);
//        lastSpeedOnFingerDown = distance / (double) time;
        Log.d(TAG, "calculateTheSpeedOnFingerUp: speed " + lastSpeedOnFingerDown);
        previousScrollY = 0;
        settleHandler.postDelayed(settleRunnable, SETTLE_POST_DURATION);
    }

    private Handler settleHandler = new Handler();
    private Runnable settleRunnable = new Runnable() {
        @Override
        public void run() {
            settleHandler.removeCallbacksAndMessages(null);

            if (previousScrollY == currentScrollY) {
                expandOrContractStacksIfIfSettled();
            } else {
                previousScrollY = currentScrollY;
                settleHandler.postDelayed(settleRunnable, SETTLE_POST_DURATION);
            }
        }
    };

    private double lastSpeedOnFingerDown;
    private int mOldScrollY;
    private int scrollYAtFingerDown;

    private void expandOrContractStacksIfIfSettled() {
        if (!hasMyStreamViewDisplaced()) {


            int distance = currentScrollY - mOldScrollY;

            if (Math.abs(currentScrollY - scrollYAtFingerDown) > 100)
                if (distance > 0)
                    contractStacks();
                else expandStacks();
//            if (hasMinimumSpeedForFling(lastSpeedOnFingerDown)) {
//                settleDownTheStacksOnBasisOfSpeed(lastSpeedOnFingerDown);
//            } else {
//                settleDownTheStacksOnBasisOfDisplacement();
//            }
        }
    }

    /**
     * @return true if my stream view (the top most one) has gone any visual translation
     * we must not induce artificial scroll if the my stream has displaced (visually)
     */
    private boolean hasMyStreamViewDisplaced() {

        //16 because 8 for top padding and 8 for bottom
        int maxHeight = convertDpToPixels(contributionLl.getChildCount() * EFFECTIVE_TAB_HEIGHT + 16);
        int heightInContractedState = convertDpToPixels(EFFECTIVE_TAB_HEIGHT + (contributionLl.getChildCount() - 1) * OVERLAP_DISTANCE + 8);
        boolean b = currentScrollY > maxHeight - heightInContractedState;
        Log.d(TAG, "hasMyStreamViewDisplaced: " + b);
        return b;
    }

    private boolean hasMinimumSpeedForFling(double speed) {
        return Math.abs(speed) > CUT_OFF_SPEED;
    }

    private int currentScrollY, previousScrollY;
    private final int SETTLE_POST_DURATION = 80;//millisecs
    @SuppressWarnings("FieldCanBeLocal")
    private final double CUT_OFF_SPEED = 0.70000d;


    private void settleDownTheStacksOnBasisOfSpeed(double lastSpeedOnFingerDown) {
        if (lastSpeedOnFingerDown > 0.00) {
            contractStacks();
        } else expandStacks();
    }

    private void settleDownTheStacksOnBasisOfDisplacement() {
        int scrollY = lockableScrollView.getScrollY();
        Log.d(TAG, "settleDownTheStacks: settling stacks at " + scrollY);
        int cutOffScroll = notificationBackGroundFl.getMeasuredHeight() / 2;

        boolean doFakeScroll = false;

        if (scrollY > 0 && scrollY < cutOffScroll) {
            scrollTo = 0;
            doFakeScroll = true;
        } else if (scrollY > cutOffScroll && scrollY < cutOffScroll * 2) {
            scrollTo = getScrollForContractingTheStacks();
            doFakeScroll = true;
        }

        if (doFakeScroll)
            Log.d(TAG, "settleDownTheStacks: posted settle runnable ? " + lockableScrollView.postDelayed(scrollRunnable, 100));
    }


}