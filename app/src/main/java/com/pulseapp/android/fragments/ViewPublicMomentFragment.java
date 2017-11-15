package com.pulseapp.android.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.login.LoginResult;
import com.pulseapp.android.ExoPlayer.EventLogger;
import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.customViews.PieView;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.downloader.PlaylistController;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.MediaTimelineView;
import com.pulseapp.android.util.OnScreenshotTakenListener;
import com.pulseapp.android.util.RoundedTransformation;
import com.pulseapp.android.util.SmartViewPage;
import com.pulseapp.android.util.ViewPageCloseCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by user on 5/12/2016.
 */

public class ViewPublicMomentFragment extends BaseFragment implements OnScreenshotTakenListener,
        EventLogger.OnPlayerStateChanged, ViewPageCloseCallback, BaseFragment.FollowPopupListener {

    private final String TAG = getClass().getSimpleName();
    public ViewControlsCallback viewControlsCallback;
    private View rootView;
    private ImageView momentImageView;
    private int currentMediaType;
    private MomentModel.Media currentMediaModel;
    private boolean isCurrentMediaPaused = false;
    private boolean isFirstTime = true;
    private SmartViewPage viewPage;
    private int CURRENT_INDEX_OF_MEDIA = -1;
    private View progressBar;
    private boolean fixedTimer = false;
    //    public ViewpageTimer viewpageTimer;
    final int DEFAULT_AUTO_PLAY_DURATION = 15000;
    public PieView pieView;
    public MediaTimelineView mediaTimelineView;

    public DynamicDownloader.MomentType momentType;
    private final int PERMISSION_ACCESS_DIALER = 1;

    private void setMomentType(String momentType) {
        if (momentType.equals("friend"))
            this.momentType = DynamicDownloader.MomentType.FRIEND_MOMENT;
        else if (momentType.equals("public"))
            this.momentType = DynamicDownloader.MomentType.PUBLIC_MOMENT;
        else if (momentType.equals("follower"))
            this.momentType = DynamicDownloader.MomentType.FOLLOWER_MOMENT;

    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @SuppressLint("LongLogTag")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;

        ((CameraActivity) getActivity()).onExitContributionToPublicStream();

        Log.d(TAG, "onCreateCalled");
        if (nearbyDownloader == null)
            nearbyDownloader = DynamicDownloader.getInstance(getActivity());
        final Object[] momentsData = nearbyDownloader.getMomentsData(getArguments().getString(AppLibrary.MOMENT_ID));
        publicMomentModel = (MomentModel) momentsData[0];
        publicMomentMediaList = (ArrayList<MomentModel.Media>) momentsData[1];
        setMomentType(getArguments().getString(AppLibrary.MOMENT_TYPE));
        setFollowPopupListener(this);
        MasterClass.snapshotDetector.clearMediaList();

        if (publicMomentModel == null || publicMomentMediaList == null) {
            Log.e(TAG, "error playing stream null playlist");
            viewControlsCallback.onCloseMomentsFragment(getActivity(), this.publicMomentModel, false, false);
        } else {
            initLastMedia();
        }
    }

    @SuppressLint("LongLogTag")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateViewCalled");
        rootView = inflater.inflate(R.layout.public_moment_details_fragment, container, false);

        if (savedInstanceState != null) return rootView;
        if (publicMomentModel == null || publicMomentMediaList == null) return rootView;

//        ((CameraActivity) getActivity()).toggleFullScreen(true);
        initializeViewObjects(rootView);
        initializeMomentViewObjects();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Window w = getActivity().getWindow();
//            AppLibrary.goTrulyFullscreen(w);
//        }

        return rootView;
    }


    private PlaylistController playlistController;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) return;
        if (publicMomentModel == null || publicMomentMediaList == null) return;

        playlistController = PlaylistController.getInstance(getActivity());
        playlistController.setViewPublicMomentFragment(this);

        // There are 3 cases essentially :
        // Case 1 - Normal timers as per media object duration
        // Case 2 - Fixed timer between 3-15 which will make sure every media plays for that time (immaterial of the length of the pic/ video)
        // Case 3 - No timer for any media. Each media proceeds on click, and videos loop infinitely

        if (publicMomentModel.fixedTimer == 0) { // Case 1 as explained above
            fixedTimer = false;
            playlistController.ignoreTimer = false;
            // Do nothing, use default timers
        } else if (publicMomentModel.fixedTimer <= 15) { // Case 2 as explained above
            playlistController.ignoreTimer = false;
            fixedTimer = true;
        } else if (publicMomentModel.fixedTimer > 15) { // Case 3 as explained above
            fixedTimer = false;
            playlistController.ignoreTimer = true;
//            viewpageTimer.setVisibility(View.GONE);
            pieView.setVisibility(View.GONE);
            mediaTimelineView.setVisibility(View.GONE);
            rootView.findViewById(R.id.nameLayout).setPadding(AppLibrary.convertDpToPixels(context, 16), AppLibrary.convertDpToPixels(context, 13), 0, 0);
        }

        //Calculate revised total duration
        long time = 0;
        for (int i = CURRENT_INDEX_OF_MEDIA + 1; i < publicMomentMediaList.size(); i++) { //Here, current index of media is actually last watched index, hence +1
            if (!fixedTimer)
                time += publicMomentMediaList.get(i).duration == 0 ? DEFAULT_AUTO_PLAY_DURATION : publicMomentMediaList.get(i).duration;
            else
                time += publicMomentModel.fixedTimer * 1000;
        }
        viewControlsCallback.setTotalTime(time);

        playNextMedia();

        final TextView name = (TextView) view.findViewById(R.id.createrName);
//        if (true) {
        if (publicMomentModel.contributableNoLocation || super.isThisMyInstitutionId(publicMomentModel.momentId)) {
            name.setVisibility(View.VISIBLE);
            final String previousText = name.getText().toString();
            final String CONTRIBUTE_TEXT = "\n+ Contribute ";
            SpannableString content = new SpannableString(previousText + CONTRIBUTE_TEXT);
            content.setSpan(new UnderlineSpan(), previousText.length() + 3, content.length() - 1, 0);
            content.setSpan(new RelativeSizeSpan((11 / 12f)), previousText.length() + 3, content.length(), 0); // set size
            name.setText(content);
            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CameraActivity) getActivity()).onContributeToPublicStream(publicMomentModel);
                    viewControlsCallback.onCloseMomentsFragment(getActivity(), publicMomentModel, completePlaylistWatched, true);
                }
            });
        }
    }

    private void initializeViewObjects(View rootView) {
        MasterClass.snapshotDetector.setListener(this);
//        rootView.findViewById(R.id.closeButton).setOnClickListener(this);
        momentImageView = (ImageView) rootView.findViewById(R.id.momentImage);
        final GestureDetector gestureDetector = new GestureDetector(getActivity(), new CustomGestureListener());
        viewPage = ((SmartViewPage) rootView.findViewById(R.id.touch_frame));
        viewPage.setRawEndPoints(getActivity());
        viewPage.setListener(this);
        progressBar = rootView.findViewById(R.id.progressBar);

        ((ProgressBar) rootView.findViewById(R.id.progressBarLoader))
                .getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);

        progressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {//blocking gestures ie. single taps when the progessbar is on
                return true;
            }
        });

//        viewControlsCallback.setGestureDetector(gestureDetector);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean touchResult = true;
                boolean result = gestureDetector.onTouchEvent(event);

                if (isCurrentMediaPaused && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    resumeCurrentMedia();
                }

                return touchResult || result;
            }
        });
//        viewpageTimer = ((ViewpageTimer) rootView.findViewById(R.id.timer));
//        viewpageTimer.setAngle(360);
//        viewpageTimer.setEraserSize(0);

        pieView = (PieView) rootView.findViewById(R.id.pieView);
        mediaTimelineView = (MediaTimelineView) rootView.findViewById(R.id.timeLineView);
    }

    private void initializeMediaDetails() {
        if (this.momentType == DynamicDownloader.MomentType.PUBLIC_MOMENT && currentMediaModel != null && currentMediaModel.userId != null && !currentMediaModel.userId.equals(mFireBaseHelper.getMyUserId()) && !currentMediaModel.anonymous) {
            if (currentMediaModel.userDetail == null || AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(IMAGE_URL)) == null || AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(NAME)) == null) {
                rootView.findViewById(R.id.createrInfoLayout).setVisibility(View.GONE);
                rootView.findViewById(R.id.createrGradient).setVisibility(View.GONE);
            } else {
                rootView.findViewById(R.id.createrInfoLayout).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.createrGradient).setVisibility(View.VISIBLE);
                if (currentMediaModel.userDetail != null && AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(IMAGE_URL)) != null)
                    Picasso.with(getActivity()).load((String) currentMediaModel.userDetail.get(IMAGE_URL)).transform(new RoundedTransformation()).noFade().into((ImageView) rootView.findViewById(R.id.createrImage));

                if (mFireBaseHelper.getSocialModel().relations == null || mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId) == null) {
                    // not a follower
                    ((ImageView) rootView.findViewById(R.id.followImage)).setImageResource(R.drawable.follow_user);
                    if (currentMediaModel.userDetail != null && currentMediaModel.userDetail.get(NAME) != null)
                        ((TextView) rootView.findViewById(R.id.userName)).setText(String.valueOf(currentMediaModel.userDetail.get(NAME)));
                    rootView.findViewById(R.id.followText).setVisibility(View.VISIBLE);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.createrInfoLayout).setTag(false);
                    // number of followers
                    if (currentMediaModel.userDetail != null && currentMediaModel.userDetail.get(SOCIAL_COUNT) != null && (long) currentMediaModel.userDetail.get(SOCIAL_COUNT) > 0) {
                        ((TextView) rootView.findViewById(R.id.followers)).setText(String.valueOf(currentMediaModel.userDetail.get(SOCIAL_COUNT)) + " Followers");
                        rootView.findViewById(R.id.followers).setVisibility(View.VISIBLE);
                    } else
                        rootView.findViewById(R.id.followers).setVisibility(View.GONE);
                } else if (mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId).equalsIgnoreCase(FRIEND) || mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId).equalsIgnoreCase(FOLLOWED)) {
                    // follower
                    rootView.findViewById(R.id.followText).setVisibility(View.GONE);
                    rootView.findViewById(R.id.createrInfoLayout).setTag(true);
//                    ((ImageView) rootView.findViewById(R.id.followImage)).setImageResource(R.drawable.followed_user);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setVisibility(View.GONE);
                    if (currentMediaModel.userDetail != null && currentMediaModel.userDetail.get(NAME) != null)
                        ((TextView) rootView.findViewById(R.id.userName)).setText(String.valueOf(currentMediaModel.userDetail.get(NAME)));
                    rootView.findViewById(R.id.followers).setVisibility(View.GONE);
                }

                rootView.findViewById(R.id.createrInfoLayout).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean following = (boolean) v.getTag();
                        // open popup to unfollow
                        if (following && currentMediaModel.userDetail != null && AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(NAME)) != null
                                && AppLibrary.checkStringObject(currentMediaModel.userId) != null && AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(IMAGE_URL)) != null
                                && AppLibrary.checkStringObject((String) currentMediaModel.userDetail.get(HANDLE)) != null) {
                            if (mFireBaseHelper.getSocialModel().relations != null && mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId) != null && mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId).equalsIgnoreCase(FRIEND)) {
                                pauseCurrentMedia();
                                showGenericProfilePopup(getActivity(), String.valueOf(currentMediaModel.userDetail.get(NAME)), (String) currentMediaModel.userDetail.get(IMAGE_URL), String.valueOf(currentMediaModel.userDetail.get(HANDLE)));
                            } else if (mFireBaseHelper.getSocialModel().relations != null && mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId) != null && mFireBaseHelper.getSocialModel().relations.get(currentMediaModel.userId).equalsIgnoreCase(FOLLOWED)) {
                                pauseCurrentMedia();
                                showFollowPopup(currentMediaModel.createdAt, getActivity(), currentMediaModel.userId, String.valueOf(currentMediaModel.userDetail.get(NAME)),
                                        (String) currentMediaModel.userDetail.get(IMAGE_URL), String.valueOf(currentMediaModel.userDetail.get(HANDLE)),
                                        true, false);
                            }
                        } else {
                            // follow fire base code
                            rootView.findViewById(R.id.createrInfoLayout).setTag(true);
                            rootView.findViewById(R.id.followText).setVisibility(View.GONE);
                            ((ImageView) rootView.findViewById(R.id.followImage)).setImageResource(R.drawable.followed_user);
                            ((ImageView) rootView.findViewById(R.id.followImage)).setVisibility(View.VISIBLE);
                            ((CameraActivity) getActivity()).updateFollowersList(true);
                            rootView.findViewById(R.id.followers).setVisibility(View.GONE);
                            mFireBaseHelper.followUser(currentMediaModel.userId, currentMediaModel.createdAt);
                        }
                    }
                });
            }
        } else {
            rootView.findViewById(R.id.createrInfoLayout).setVisibility(View.GONE);
            rootView.findViewById(R.id.createrGradient).setVisibility(View.GONE);
        }
    }

    private void initializeMomentViewObjects() {
        ((TextView) rootView.findViewById(R.id.createrName)).setText(this.momentType == DynamicDownloader.MomentType.FRIEND_MOMENT ? FriendsMomentsFragment.getMomentNameById(publicMomentModel.momentId) : publicMomentModel.name);
        rootView.findViewById(R.id.createdTime).setVisibility((this.momentType == DynamicDownloader.MomentType.FRIEND_MOMENT) || (this.momentType == DynamicDownloader.MomentType.FOLLOWER_MOMENT) ? View.VISIBLE : View.GONE);
        if (this.momentType == DynamicDownloader.MomentType.PUBLIC_MOMENT) {
            rootView.findViewById(R.id.shareIv).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.shareIv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pauseCurrentMedia();
                    showFacebookSharePopup(new CameraActivity.SharePopupCallbacks() {
                        @Override
                        public void onShareFacebookClicked() {
                            if (facebookController != null && AccessToken.getCurrentAccessToken() != null
                                    && AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions")) {
                                // provided publish permissions
                                if (mFireBaseHelper.getMyUserId() != null && publicMomentModel != null
                                        && publicMomentModel.momentId != null
                                        && AccessToken.getCurrentAccessToken().getToken() != null
                                        && currentMediaModel.mediaId != null && publicMomentModel.handle != null) {
                                    HashMap<Object, Object> postObject = new HashMap<Object, Object>();
                                    postObject.put(MOMENT_ID, publicMomentModel.momentId);
                                    postObject.put(HANDLE, publicMomentModel.handle);
                                    postObject.put(TYPE, 3);
                                    postObject.put(MEDIA_ID, currentMediaModel.mediaId);
                                    postObject.put(USER_ID, mFireBaseHelper.getMyUserId());
                                    postObject.put(TOKEN, AccessToken.getCurrentAccessToken().getToken());
                                    mFireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, postObject);
                                    if (isInternetAvailable(false))
                                        BaseFragment.showShortToastMessage("Shared on Facebook");
                                    else
                                        BaseFragment.showShortToastMessage("Will be shared on Facebook when online");
                                } else {
                                    BaseFragment.showShortToastMessage("Sorry! Something went wrong");
                                }
                                resumeCurrentMedia();
                            } else {
                                facebookController.doFacebookLogin(Arrays.asList("publish_actions"), new CameraActivity.FacebookLoginCallback() {
                                    @Override
                                    public void onSuccessfulLoginCallback(LoginResult loginResult) {
                                        if (mFireBaseHelper.getMyUserId() != null && publicMomentModel != null
                                                && publicMomentModel.momentId != null
                                                && loginResult.getAccessToken().getToken() != null
                                                && currentMediaModel.mediaId != null && publicMomentModel.handle != null) {
                                            HashMap<Object, Object> postObject = new HashMap<Object, Object>();
                                            postObject.put(TYPE, 3);
                                            postObject.put(MOMENT_ID, publicMomentModel.momentId);
                                            postObject.put(HANDLE, publicMomentModel.handle);
                                            postObject.put(MEDIA_ID, currentMediaModel.mediaId);
                                            postObject.put(USER_ID, mFireBaseHelper.getMyUserId());
                                            postObject.put(TOKEN, loginResult.getAccessToken().getToken());
                                            mFireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, postObject);
                                            if (isInternetAvailable(false))
                                                BaseFragment.showShortToastMessage("Shared on Facebook");
                                            else
                                                BaseFragment.showShortToastMessage("Will be shared on Facebook when online");
                                        } else {
                                            BaseFragment.showShortToastMessage("Sorry! Something went wrong");
                                        }
                                        resumeCurrentMedia();
                                    }

                                    @Override
                                    public void onErrorCallback() {
                                        resumeCurrentMedia();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onShareIntentClicked() {
                            if (publicMomentModel != null && publicMomentModel.handle != null && publicMomentModel.name != null)
                                launchMediaShareIntent(publicMomentModel.handle, publicMomentModel.name, null, currentMediaModel.type, currentMediaModel.url);
                        }

                        @Override
                        public void onPopupDismiss() {
                            resumeCurrentMedia();
                        }

                        @Override
                        public void onWatsAppShareClicked() {
                            if (publicMomentModel != null && publicMomentModel.handle != null && publicMomentModel.name != null)
                                launchMediaShareIntent(publicMomentModel.handle, publicMomentModel.name, AppLibrary.WHATSAPP_PACKAGE_NAME, currentMediaModel.type, currentMediaModel.url);
                        }
                    });
                }
            });
        } else {
            rootView.findViewById(R.id.shareIv).setVisibility(View.GONE);
        }
        AppLibrary.log_d(TAG, "Currently Watching Moment Id -" + publicMomentModel.momentId);
    }

    MomentModel publicMomentModel;
    ArrayList<MomentModel.Media> publicMomentMediaList;

    private void pauseCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().pauseCurrentMedia();
            viewControlsCallback.stopAutoPlay();
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            viewControlsCallback.stopAutoPlay();
        }
        isCurrentMediaPaused = true;
    }

    private void resumeCurrentMedia() {
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            MediaPlayer.getInstance().resumeCurrentMedia();
            viewControlsCallback.resumeAutoPlay();
        } else if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
            viewControlsCallback.resumeAutoPlay();
        }
        isCurrentMediaPaused = false;
    }


    @SuppressLint("LongLogTag")
    public void notifyMediaLoaded(MomentModel.Media downloadedMedia) {
        Log.d(TAG, " mediaLoaded at playtime " + downloadedMedia.mediaId);
        if (downloadedMedia == waitingForMedia) {
            Log.d(TAG, " resuming playlist " + downloadedMedia.mediaId);
            this.progressBar.setVisibility(View.GONE);
            playNextMedia();
        }
    }

    public MomentModel.Media waitingForMedia = null;


    @SuppressLint("LongLogTag")
    public void playNextMedia() {
//      Log.d(TAG, "playNextMedia");
        waitingForMedia = null;
        currentMediaModel = getNextMedia();
        if (currentMediaModel == null) {//media is null implying playlist finished; hence close the fragment
            viewControlsCallback.onCloseMomentsFragment(getActivity(), this.publicMomentModel, this.completePlaylistWatched, false);
        } else {//media model is not null
            Log.d(TAG, "next mediaUrl " + (new File(currentMediaModel.url)).exists() + " " + currentMediaModel.url);

            lastWatchedIndexOfMoments.put(publicMomentModel, CURRENT_INDEX_OF_MEDIA - 1);

            nearbyDownloader.notifySwitchToNextMedia(publicMomentModel, CURRENT_INDEX_OF_MEDIA);

            String text = AppLibrary.timeAccCurrentTime(currentMediaModel.createdAt);
            if (!text.isEmpty() && !text.contains("Now")) text = text + " ago";
            ((TextView) rootView.findViewById(R.id.createdTime)).setText(text);
            initializeMediaDetails();

            if (isMediaPlayerTurnedOn) {
                MediaPlayer.getInstance().onPlayerRelease();
                isMediaPlayerTurnedOn = false;
            }

            if (currentMediaModel.status == MEDIA_DOWNLOAD_NOT_STARTED || currentMediaModel.status == DOWNLOADING_MOMENT) {//pausing everything
                waitingForMedia = currentMediaModel;
                progressBar.setVisibility(View.VISIBLE);
                viewControlsCallback.stopAutoPlay();
                resetIndexToPreviousMedia();
                Log.d(TAG, " pausing playlist as " + currentMediaModel.mediaId + " has status " + currentMediaModel.status);
            } else if (currentMediaModel.status == MEDIA_DOWNLOAD_COMPLETE || currentMediaModel.status == MEDIA_VIEWED) {

                currentMediaModel.status = MEDIA_VIEWED;

                String url = currentMediaModel.url;

                if (url == null || url.isEmpty() || currentMediaModel.mediaId == null)
                    playNextMedia(); //Error check - skip corrupt media

                mFireBaseHelper.openNearbyMedia(publicMomentModel.momentId, currentMediaModel.mediaId, null, null, 0, 0);

                int mediaType = AppLibrary.getMediaType(url);

                if (!fixedTimer) { //Handles both ignore-timer and normal timer case
                    viewControlsCallback.startAutoPlay(mediaType == AppLibrary.MEDIA_TYPE_IMAGE ?
                            //for image
                            (currentMediaModel.duration == 0 ? DEFAULT_AUTO_PLAY_DURATION : currentMediaModel.duration) :
                            //for video
                            currentMediaModel.duration == 0 ? DEFAULT_AUTO_PLAY_DURATION : currentMediaModel.duration
                    );
                } else {
                    viewControlsCallback.startAutoPlay(publicMomentModel.fixedTimer * 1000);
                }

                if (mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                    isVideoPlayed = true;
                    isMediaPlayerTurnedOn = true;
                    currentMediaType = AppLibrary.MEDIA_TYPE_VIDEO;
                    File file = new File(url);
                    if (MediaPlayer.getInstance().surface == null) {
                        MediaPlayer.getInstance().initializePlayer(this, getActivity(), rootView, file.getAbsolutePath());
                        if (playlistController.ignoreTimer)
                            MediaPlayer.getInstance().isLooping = true;
                        else
                            MediaPlayer.getInstance().isLooping = false;
                    } else { //Already initialized with a surface before
                        MediaPlayer.getInstance().resetPlayer(this, getActivity(), rootView, file.getAbsolutePath());
                        MediaPlayer.getInstance().onResume();
                    }
                } else if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE) {
                    currentMediaType = AppLibrary.MEDIA_TYPE_IMAGE;
                    AppLibrary.log_d(TAG, "Currently displaying media with mediaId -" + currentMediaModel.mediaId);
                    momentImageView.setImageURI(Uri.fromFile(new File(url)));
                    if (momentImageView.getVisibility() == View.GONE) {
                        momentImageView.setVisibility(View.VISIBLE);
                    }
                } else {
                    // error
                    AppLibrary.log_e(TAG, "Error playing next Media");
                }
                if (currentMediaModel != null)
                    MasterClass.snapshotDetector.addToMediaList((int) System.currentTimeMillis(), currentMediaModel.mediaId, currentMediaModel.momentId);
            }
        }

        initCTA();
    }

    private void resetIndexToPreviousMedia() {
        --CURRENT_INDEX_OF_MEDIA;
//        currentMediaModel = publicMomentMediaList.get(CURRENT_INDEX_OF_MEDIA);
    }


    private static LinkedHashMap<MomentModel, Integer> lastWatchedIndexOfMoments = new LinkedHashMap<>();

    /**
     * called while creating the fragment initializes the variables
     * to maintain index
     */
    private void initLastMedia() {
        final Integer recordedIndex = lastWatchedIndexOfMoments.get(publicMomentModel);
        if (recordedIndex == null) {//we go in there for first time on each moment
            for (int i = 0; i < publicMomentMediaList.size(); i++) {
                final MomentModel.Media media = publicMomentMediaList.get(i);
                if (media.status == MEDIA_DOWNLOAD_NOT_STARTED || media.status == MEDIA_DOWNLOAD_COMPLETE) {
                    CURRENT_INDEX_OF_MEDIA = i - 1;
                    lastWatchedIndexOfMoments.put(publicMomentModel, CURRENT_INDEX_OF_MEDIA);
                    break;
                }
            }
        } else {
            CURRENT_INDEX_OF_MEDIA = recordedIndex;
        }
    }

    /**
     * @param momentModel in which the media was added.
     *                    Calling it for a particular moment will remove the recorded index
     *                    and {@link #initLastMedia()}will be compelled to iterate over the media
     *                    list and find the new index to be played
     */
    public static void refreshIndexOfPlaylistOnNewMediaAdded(MomentModel momentModel) {
        lastWatchedIndexOfMoments.put(momentModel, null);
    }

    @SuppressLint("LongLogTag")
    @Nullable//in case the playlist is complete
    //recursive in case of error in the current media of the medias
    private MomentModel.Media
    getNextMedia() {
        if (CURRENT_INDEX_OF_MEDIA == publicMomentMediaList.size() - 1) {//full playlist watched reset the counter
            Log.i(TAG, "seems playlist is complete resetting index ");
            completePlaylistWatched = true;
            lastWatchedIndexOfMoments.put(publicMomentModel, -1);
            CURRENT_INDEX_OF_MEDIA = -1;
            return null;
        }
        ++CURRENT_INDEX_OF_MEDIA;
        final MomentModel.Media media = publicMomentMediaList.get(CURRENT_INDEX_OF_MEDIA);

        int status = media.status;

        if (status == ERROR_DOWNLOADING_MEDIA)
            return getNextMedia();
        else if (status == MEDIA_DOWNLOAD_COMPLETE || status == MEDIA_DOWNLOADING || status == MEDIA_VIEWED || status == MEDIA_DOWNLOAD_NOT_STARTED)
            return media;
        else
            return getNextMedia();
//        throw new RuntimeException("unexpected state" + status);
    }

    DynamicDownloader nearbyDownloader;

    public boolean fileExists(String value) {
        return new File(value).exists();
    }

    public void hideImageView() {
        if (momentImageView != null && momentImageView.getVisibility() == View.VISIBLE)
            momentImageView.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) getParentFragment();
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onResume() {
        Log.d(TAG, "onResumeCalled");
        super.onResume();
        MasterClass.snapshotDetector.start(getActivity());

        if (mFireBaseHelper == null || viewControlsCallback == null) return;

        if (!isFirstTime && !isFaceBookMessengerIntentSessionActive) {
            if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
                MediaPlayer.getInstance().onResume();
            }
            viewControlsCallback.resumeAutoPlay();
        }

        ((CameraActivity) getActivity()).toggleFullScreen(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getActivity().getWindow();
            AppLibrary.goTrulyFullscreen(w);
        }

        isFirstTime = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPauseCalled: ");
        if (mFireBaseHelper == null || viewControlsCallback == null) return;

        MasterClass.snapshotDetector.stop(getActivity());
        if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO)
            MediaPlayer.getInstance().onPause();
        viewControlsCallback.stopAutoPlay();
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//    }

//    @Override
//    public void onStop() {
//        super.onStop();
//    }

    public boolean isViewDestroyed = false;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewDestroyed = true;

        if (mFireBaseHelper == null || getActivity() == null) return;

        ((CameraActivity) getActivity()).toggleFullScreen(false);
    }

    @Override

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.closeButton:
//                if (currentMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
//                    MediaPlayer.getInstance().onDestroy();
//                }
//                viewControlsCallback.onCloseMomentsFragment(getActivity(), this.publicMomentModel, this.completePlaylistWatched);
//                break;
//        }
//    }


    public Bitmap getActiveBitmap() {
        momentImageView.setDrawingCacheEnabled(true);
        momentImageView.buildDrawingCache(true);
        if (currentMediaType == AppLibrary.MEDIA_TYPE_IMAGE)
            return Bitmap.createBitmap(momentImageView.getDrawingCache());
        else
            return MediaPlayer.getInstance().getBitmap();
    }

    @Override
    public void onStateEnded() {
        if (fixedTimer) return;

        MediaPlayer.getInstance().onPlayerRelease();
        playNextMedia();
    }

    @Override
    public void onStateReady() {
        hideImageView();
    }

    @Override
    public void onMinimize() {
        viewControlsCallback.onCloseMomentsFragment(getActivity(), this.publicMomentModel, this.completePlaylistWatched, false);
    }

    @Override
    public void onButtonClicked(View view, String userId, Dialog popup) {
        switch (view.getId()) {
            case R.id.shareImageView:
                break;
            case R.id.followlv:
                boolean following = (boolean) view.getTag();
                if (following) {
                    rootView.findViewById(R.id.followText).setVisibility(View.VISIBLE);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setImageResource(R.drawable.follow_user);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setVisibility(View.VISIBLE);
                    if (currentMediaModel.userDetail != null && currentMediaModel.userDetail.get(SOCIAL_COUNT) != null && (long) currentMediaModel.userDetail.get(SOCIAL_COUNT) > 0) {
                        ((TextView) rootView.findViewById(R.id.followers)).setText(String.valueOf(currentMediaModel.userDetail.get(SOCIAL_COUNT)) + " Followers");
                        rootView.findViewById(R.id.followers).setVisibility(View.VISIBLE);
                    } else
                        rootView.findViewById(R.id.followers).setVisibility(View.GONE);
                } else {
                    rootView.findViewById(R.id.followText).setVisibility(View.GONE);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setImageResource(R.drawable.followed_user);
                    ((ImageView) rootView.findViewById(R.id.followImage)).setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.followers).setVisibility(View.GONE);
                }
                rootView.findViewById(R.id.createrInfoLayout).setTag(!following);
                ((CameraActivity) getActivity()).updateFollowersList(!following);
                break;
        }
    }

    @Override
    public void onPopupDismiss() {
        resumeCurrentMedia();
    }

    private class CustomGestureListener implements GestureDetector.OnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;


        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            onSingleTap();
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onLongHold();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                            result = true;
                        } else {
                            onSwipeLeft();
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }

        public void onSwipeRight() {
        }

        public void onSwipeLeft() {
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }

        public void onSingleTap() {
            if (isCurrentMediaPaused) {
                resumeCurrentMedia();
            } else
                playNextMedia();
            isFaceBookMessengerIntentSessionActive = false;
        }

        public void onLongHold() {
            if (!isCurrentMediaPaused)
                pauseCurrentMedia();
        }
    }

    private boolean completePlaylistWatched;

    public interface ViewControlsCallback {
        void setGestureDetector(GestureDetector gestureDetector);

        void startAutoPlay(long time);

        int getMomentStatus();

        void stopAutoPlay();

        void resumeAutoPlay();

        void onCloseMomentsFragment(Activity cameraActivity, MomentModel momentModel, boolean completePlaylistWatched, boolean jumpToCamera);

        void setTotalTime(long totalTime);
    }

    @Override
    public void onScreenshotTaken(HashMap<String, String> mediaDetails) {
        FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getActivity());
        AppLibrary.log_d("SnapshotDetector", "Screen Shot taken media is -" + mediaDetails.get(MEDIA_ID));
        fireBaseHelper.updateOnScreenShotTaken(null, null, mediaDetails);
    }

    boolean isVideoPlayed, isMediaPlayerTurnedOn;

    public boolean isVideoPlayed() {
        return isVideoPlayed;
    }

    enum CtaType {BROWSER, GENERAL}

    private CtaType ctaType;//as in call to action

    private void whichIntent(MomentModel.Cta cta) {
        String intentAction = cta.androidIntent.intentAction;
        ctaType = intentAction.contains("http") || intentAction.contains("https") ? CtaType.BROWSER : CtaType.GENERAL;
    }

    private void refreshLayoutParams(TextView textView) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
        if (rootView.findViewById(R.id.createrInfoLayout).getVisibility() == View.VISIBLE && rootView.findViewById(R.id.createrGradient).getVisibility() == View.VISIBLE) {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            int padding = convertDpToPixels(16);
            int diff = convertDpToPixels(8);
            textView.setPadding(padding, padding, padding, padding);
            layoutParams.bottomMargin = padding - diff;
            layoutParams.rightMargin = padding;
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            textView.setPadding(0, 0, 0, 0);
            layoutParams.bottomMargin = 0;
            layoutParams.rightMargin = 0;
        }

        textView.setLayoutParams(layoutParams);

    }

    private void initCTA() {
        //first check mandatory because playnext media returns null when playlist expired
        if (currentMediaModel == null || currentMediaModel.cta == null || currentMediaModel.cta.text == null) {
            rootView.findViewById(R.id.viewMoreBtn).setVisibility(View.GONE);
            return;
        } else {
            whichIntent(currentMediaModel.cta);
            rootView.findViewById(R.id.viewMoreBtn).setVisibility(View.VISIBLE);
            TextView textView = (TextView) rootView.findViewById(R.id.viewMoreBtn);
            textView.setText(currentMediaModel.cta.text);
            refreshLayoutParams(textView);

        }
        rootView.findViewById(R.id.viewMoreBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctaType == CtaType.BROWSER) {
                    initCustomTab();
                    customTabsIntent.launchUrl(getActivity(), Uri.parse(currentMediaModel.cta.androidIntent.intentAction));
                } else if (ctaType == CtaType.GENERAL) {
//                    try {
                    launchGeneralIntent(currentMediaModel.cta);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                } else {
                    showShortToastMessage("Some error occurred");
                }
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.PUBLIC_STREAM_CTA);
            }
        });
    }

    private void launchGeneralIntent(MomentModel.Cta cta) {

        Intent intent = new Intent();
        MomentModel.Intent androidIntent = cta.androidIntent;

        intent.setAction(androidIntent.intentAction);

        if (androidIntent.intentType != null)
            intent.setType(androidIntent.intentType);

        if (androidIntent.intentData != null)
            intent.setData(Uri.parse(androidIntent.intentData));

        if (androidIntent.intentExtra != null)
            intent.putExtra(Intent.EXTRA_TEXT, androidIntent.intentExtra);


        if (androidIntent.intentPackage != null && AppLibrary.isPackageInstalled(androidIntent.intentPackage, getActivity())) {
            intent.setPackage(androidIntent.intentPackage);
        }


        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_CALL))
            requestPHonePermissionsAndProceed(intent);
        else {
            try {
                startActivity(intent);
                if (intent.getPackage() != null && intent.getPackage().equals(FB_MESSENGER_PACKAGE_NAME)) {
                    isFaceBookMessengerIntentSessionActive = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                showShortToastMessage("Sorry, some error occurred");
            }
        }
        pauseMediaOnOpeningIntent();
    }

    private boolean isFaceBookMessengerIntentSessionActive;

    private void pauseMediaOnOpeningIntent() {
        if (!isCurrentMediaPaused)
            pauseCurrentMedia();
    }

    private Intent mPendingDialerIntent;

    private void requestPHonePermissionsAndProceed(Intent intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_ACCESS_DIALER);
            mPendingDialerIntent = intent;
        } else {
            startActivity(intent);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_DIALER:
                if (AppLibrary.verifyPermissions(grantResults)) {
                    startActivity(mPendingDialerIntent);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    // Define variables for custom tabs and its builder
    private CustomTabsIntent customTabsIntent;
    private CustomTabsIntent.Builder intentBuilder;

    private void initCustomTab() {
        if (customTabsIntent != null && intentBuilder != null) {//We have already initialized it
            return;
        }
        // Initialize intentBuilder
        intentBuilder = new CustomTabsIntent.Builder();
        // Set toolbar(tab) color of your chrome browser
        intentBuilder.setToolbarColor(ContextCompat.getColor(getActivity(), R.color.pulse_button_color));
        // Define entry and exit animation

        intentBuilder.setStartAnimations(context, R.anim.enter_res_id_start, R.anim.exit_res_id_start);
        intentBuilder.setExitAnimations(context, R.anim.enter_res_id_exit, R.anim.exit_res_id_exit);
        intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(getActivity(), R.color.pulse_row_selected_color));
        // build it by setting up all
        customTabsIntent = intentBuilder.build();
        final String CHROME_PACKAGE_NAME = "com.android.chrome";
        if (AppLibrary.isPackageInstalled(CHROME_PACKAGE_NAME, getActivity())) {
            customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME);
        }
    }
}