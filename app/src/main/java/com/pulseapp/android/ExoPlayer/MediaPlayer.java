/*
   * Copyright (C) 2014 The Android Open Source Project
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *      http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
package com.pulseapp.android.ExoPlayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.DebugTextViewHelper;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.VerboseLogUtil;
import com.pulseapp.android.ExoPlayer.DemoPlayer.RendererBuilder;
import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.Map;

//import com.google.android.exoplayer.metadata.GeobMetadata;
//import com.google.android.exoplayer.metadata.PrivMetadata;
//import com.google.android.exoplayer.metadata.TxxxMetadata;
//import com.google.common.base.Stopwatch;
//import net.frakbot.jumpingbeans.JumpingBeans;

/**
 * An activity that plays media using {@link DemoPlayer}.
 */
public class MediaPlayer implements OnClickListener,
        DemoPlayer.Listener, DemoPlayer.CaptionListener, DemoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener, TextureView.SurfaceTextureListener {

    public static final int TYPE_DASH = 0;
    public static final int TYPE_SS = 1;
    public static final int TYPE_HLS = 2;
    public static final int TYPE_OTHER = 3;

    public static final String CONTENT_TYPE_EXTRA = "content_type";
    public static final String CONTENT_ID_EXTRA = "content_id";

    private static final String TAG = "MediaPlayer";
    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;

    private static final CookieManager defaultCookieManager;
    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private EventLogger eventLogger;
    private CustomMediaController mediaController;
    private View debugRootView;
    private View shutterView;
    private AspectRatioFrameLayout videoFrame;
    private SurfaceView surfaceView;
    private TextureView textureView;
    public Surface surface;
    private TextView debugTextView;
    private TextView playerStateTextView;
    private SubtitleLayout subtitleLayout;
    private Button videoButton;
    private Button audioButton;
    private Button textButton;
    private Button retryButton;
    public boolean isLooping;

    private DemoPlayer player;
    private DebugTextViewHelper debugViewHelper;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private boolean enableBackgroundAudio;

    private Uri contentUri;
    public int contentType;
    public boolean isStopped = false;
    private String contentId;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;
    private Context mContext;
    private View rootView;
    public static MediaPlayer activity;
    private android.os.Handler mHandler = new android.os.Handler();
    private android.os.Handler RestartHandler = new android.os.Handler();
//    private Stopwatch stopwatch = null;
//    private JumpingBeans jumpingBeans;
    private Fragment currentFragment = null;

    private MediaPlayer(){}

    public static MediaPlayer getInstance(){
        if (activity == null){
            activity = new MediaPlayer();
        }
        return activity;
    }

    public void resetPlayer(Fragment fragment,Context context, View root, String url) {
        this.contentType = 3;
        this.contentUri = Uri.parse(url);
        this.mContext = context;
        this.currentFragment = fragment;
        this.rootView = root;
    }

    public void initializePlayer(Fragment fragment,Context context, View root, String url){
        this.contentType = 3;
        this.contentUri = Uri.parse(url);
        this.mContext = context;
        this.currentFragment = fragment;
        this.rootView = root;

//        jumpingBeans = JumpingBeans.with((TextView)rootView.findViewById(R.id.playerStateText))
//                .appendJumpingDots()
//                .build();

//        rootView.findViewById(R.id.replayButton).setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                repreparing = false;
//                rootView.findViewById(R.id.replayButton).setClickable(false);
//                playerPosition = 0L;
//                player.seekTo(0);
//                player.getPlayerControl().start();
//                rootView.findViewById(R.id.replayButton).setVisibility(View.GONE);
//                mediaController.updatePausePlay();
//            }
//        });

//        rootView.findViewById(R.id.playButton).setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                player.getPlayerControl().start();
//                rootView.findViewById(R.id.playButton).setVisibility(View.GONE);
//                mediaController.updatePausePlay();
//            }
//        });

//		rootView.findViewById(R.id.pauseButton).setOnClickListener(clickListener);
//
//		rootView.findViewById(R.id.playButton).setOnClickListener(clickListener);

//		rootView.setOnTouchListener(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View view, MotionEvent motionEvent) {
//				if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//					toggleControlsVisibility();
//				} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//					view.performClick();
//				}
//				return true;
//			}
//		});
//		rootView.setOnKeyListener(new OnKeyListener() {
//			@Override
//			public boolean onKey(View v, int keyCode, KeyEvent event) {
//				if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
//					return mediaController.dispatchKeyEvent(event);
//				}
//				return false;
//			}
//		});

        if (mContext==null) return;

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(mContext.getApplicationContext(), this);

        //    shutterView = findViewById(R.id.shutter);
        //    debugRootView = findViewById(R.id.controls_root);

        videoFrame = (AspectRatioFrameLayout) rootView.findViewById(R.id.video_frame);
//        mediaController = new CustomMediaController(mContext);
//        mediaController.setAnchorView((ViewGroup)root.findViewById(R.id.seekBarAnchor));

//        CookieHandler currentHandler = CookieHandler.getDefault();
//        if (currentHandler != defaultCookieManager) {
//            CookieHandler.setDefault(defaultCookieManager);
//        }

        textureView = (TextureView) rootView.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);
        if(textureView.isAvailable()) {
            onSurfaceTextureAvailable(textureView.getSurfaceTexture(), 0, 0);
        }
        //Old usage of surfaceView
//      surfaceView = (SurfaceView) rootView.findViewById(R.id.surface_view);
//		surfaceView.getHolder().addCallback(this);
        //    debugTextView = (TextView) findViewById(R.id.debug_text_view);

        //    playerStateTextView = (TextView) findViewById(R.id.player_state_view);
        //    subtitleLayout = (SubtitleLayout) findViewById(R.id.subtitles);

//		mediaPlayer.setOnPreparedListener(this);

		onResume();

//        mRunnable = new Runnable() {
//            @Override
//            public void run() {
//                mediaController.hide();
////				rootView.findViewById(R.id.pauseButton).setVisibility(View.GONE);
////				rootView.findViewById(R.id.playButton).setVisibility(View.GONE);
//            }
//        };
    }

    public void pauseCurrentMedia(){
        if (player != null){
            if (player.getPlayerControl().canPause()){
                player.getPlayerControl().pause();
            }
        }
    }

    public Bitmap getBitmap() {
        if(textureView != null && textureView.isAvailable())
            return textureView.getBitmap();
        else
            return null;
    }

    public void setIsLooping(boolean isLooping) {
        this.isLooping = isLooping;
    }

    public void resumeCurrentMedia(){
        if (player != null){
            player.getPlayerControl().start();
        }
    }

//    public void removeJumpingBeans(){
//        if (jumpingBeans != null)
//            jumpingBeans.stopJumping();
//
//        jumpingBeans = null;
//    }
//
//    public void appendJumpingBeans(){
//        if (jumpingBeans == null)
//            jumpingBeans = JumpingBeans.with((TextView)rootView.findViewById(R.id.playerStateText))
//                    .appendJumpingDots()
//                    .build();
//    }



//	private OnClickListener clickListener = new OnClickListener() {
//		@Override
//		public void onClick(View v) {
//			if (player.getPlayerControl().isPlaying()){
//				player.getPlayerControl().pause();
//				isPaused = true;
//				rootView.findViewById(R.id.pauseButton).setVisibility(View.GONE);
//				rootView.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
//			}else {
//				if (hasEnded){
//					player.seekTo(0L);
//					hasEnded = false;
//					AppLibrary.log_d(TAG,"Video replay");
//				}
//				player.getPlayerControl().start();
//				isPaused = false;
//				rootView.findViewById(R.id.pauseButton).setVisibility(View.VISIBLE);
//				rootView.findViewById(R.id.playButton).setVisibility(View.GONE);
//			}
//		}
//	};

    public void onResume() {
        AppLibrary.log_d(TAG, "Resuming Player, registering Audio Capabilities");
        // The player will be prepared on receiving audio capabilities.
        try {
            if (audioCapabilitiesReceiver != null)
                audioCapabilitiesReceiver.register();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        releasePlayer(false);
        try {
            AppLibrary.log_d(TAG, "Un registering Audio Capabilities");
            if (audioCapabilitiesReceiver != null)
                audioCapabilitiesReceiver.unregister();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void onPlayerRelease(){
        releasePlayer(true);
        try {
            AppLibrary.log_d(TAG, "Un registering Audio Capabilities");
            if(audioCapabilitiesReceiver != null)
                audioCapabilitiesReceiver.unregister();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        AppLibrary.log_d(TAG, "Releasing player");
        releasePlayer(true);
        if (RestartHandler != null)
            RestartHandler.removeCallbacksAndMessages(null);
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
        surfaceRelease();
        activity = null;
    }

    @Override
    public void onClick(View view) {
        if (view == retryButton) {
            preparePlayer();
        }
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        AppLibrary.log_d(TAG, "AudioCapabilities changed");
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            AppLibrary.log_d(TAG,"AudioCapabilities changed, Releasing and re-preparing the player");
            this.audioCapabilities = audioCapabilities;
            releasePlayer(true);
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    private RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(mContext, "InstaLively");
        switch (contentType) {
            case TYPE_SS:
                //        return new SmoothStreamingRendererBuilder(this, userAgent, contentUri.toString(),
                //            new SmoothStreamingTestMediaDrmCallback());
            case TYPE_DASH:
                //        return new DashRendererBuilder(this, userAgent, contentUri.toString(),
                //            new WidevineTestMediaDrmCallback(contentId), audioCapabilities);
            case TYPE_HLS:
                return new HlsRendererBuilder(mContext, userAgent, contentUri.toString(), audioCapabilities);

            case TYPE_OTHER:
                return new ExtractorRendererBuilder(mContext, userAgent, contentUri);

            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    private void preparePlayer() {
        if (player == null) {
            AppLibrary.log_d(TAG,"Preparing Player");
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setCaptionListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
//            mediaController.setMediaPlayer(player.getPlayerControl());
//            mediaController.setEnabled(true);
            eventLogger = new EventLogger(currentFragment,textureView, mContext,rootView);
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            AppLibrary.log_d(TAG,"Player needs prepare - "+playerNeedsPrepare);
            player.prepare();
            playerNeedsPrepare = false;
        }
        if(surface != null)
            player.setSurface(surface);
        player.setPlayWhenReady(true);
    }

    private void releasePlayer(boolean releaseComplete) {
        if (player != null) {
            AppLibrary.log_d(TAG,"Releasing Player");
            if (releaseComplete) {
                playerPosition = 0L;
                player.seekTo(playerPosition);
            } else {
                playerPosition = player.getCurrentPosition();
            }
            player.release();
            player = null;
            if (eventLogger != null) {
                eventLogger.endSession();
                eventLogger = null;
            }
        }
    }

    public void replay() {
        playerPosition = 0L;
        player.seekTo(0);
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
//                if(mRunnable!=null && mHandler!=null)
//                    mHandler.removeCallbacks(mRunnable);
////                if (stopwatch != null && playWhenReady){
////                    // stopping the stopwatch when the video is buffering
////                    stopTimer();
//////                    if ((stopwatch.elapsedMillis()/1000) >= YoutubePlayerActivity.video_time){
//////                        //video completed
//////                        AppLibrary.log_d(TAG, "Video Completed ,elapsed time -" + stopwatch.elapsedMillis() / 1000);
//////                        transition_to_complete = true;
//////                        ((YoutubePlayerActivity)mContext).getVideoStreamUrl();
//////                    }
////                }
//                if(transition_to_complete && playWhenReady) {
//                    AppLibrary.log_d(TAG,"Stream ended");
////                    onStreamEnded();
//                    rePreparePlayer();
//                    transition_to_complete = false;
//                }
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                AppLibrary.log_d(TAG,"player state ended");
//                if(playWhenReady) {
//                    playerPosition = 0L;
//                    player.seekTo(0);
//                }
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
//                if (stopwatch == null && contentType == 2 && YoutubePlayerActivity.video_time != 0 && playWhenReady && !transition_to_complete){
//                    // initializing the stopwatch timer for the first time when we have the video time, starts timer also
////                    initializeTimer();
//                }else if (stopwatch != null && playWhenReady){
//                    // starting the timer when the player is ready
//                    startTimer();
//                }else if (stopwatch != null && !playWhenReady){
//                    // handles the case when the player is being paused
//                    stopTimer();
//                }
                break;
            default:
                text += "unknown";
                break;
        }
        AppLibrary.log_d(TAG, text);
    }

//    public void initializeTimer(){
//        AppLibrary.log_d(TAG,"Initializing timer");
////        stopwatch = new Stopwatch();
//        Timer timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
////                AppLibrary.log_d(TAG,"Timer running -"+stopwatch.elapsedMillis()/1000);
//            }
//        }, 0, 1000);
//
//        startTimer(); //Starts the timer after initializing
//    }
//
//    public void stopTimer(){
////        if (stopwatch != null && stopwatch.isRunning()) {
////            AppLibrary.log_d(TAG,"Stopping timer,elapsed Time -"+stopwatch.elapsedMillis()/1000);
////            stopwatch.stop();
////        }
//    }

//    public void startTimer(){
////        if (stopwatch != null && !stopwatch.isRunning()) {
////            AppLibrary.log_d(TAG,"Starting timer,elapsed Time -"+stopwatch.elapsedMillis()/1000);
////            stopwatch.start();
////        }
//    }

//    public void onStreamEnded(){
//        eventLogger.updateEndedState();
//    }

    @Override
    public void onError(Exception e) {
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            int stringId = Util.SDK_INT < 18 ? R.string.drm_error_not_supported
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? R.string.drm_error_unsupported_scheme : R.string.drm_error_unknown;
            Toast.makeText(mContext.getApplicationContext(), stringId, Toast.LENGTH_LONG).show();
        }
        playerNeedsPrepare = true;
//        showControls();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        AppLibrary.log_d(TAG, "On Video size changed");
        videoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);

//        ViewMomentDetailsFragment.hideImageView();
//        videoFrame.setDeviceParameters(AppLibrary.getDeviceParams((YoutubePlayerActivity)mContext,"width"),AppLibrary.getDeviceParams((YoutubePlayerActivity) mContext, "height"));
//        rootView.findViewById(R.id.video_frame).setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

//        if(repreparing) {
////            eventLogger.showReplayAssets();
//            AppLibrary.log_d(TAG, "Showing replay assets");
//        } else {
////            new android.os.Handler().postDelayed(new Runnable() {
////                @Override
////                public void run() {  //Since Video doesn't start playing immediately, remove the overlay with a delay
////                    rootView.findViewById(R.id.playerThumbnail).setVisibility(View.GONE);
////                    ((YoutubePlayerActivity)mContext).startBlurTransition();
////                    removeJumpingBeans();
////                    rootView.findViewById(R.id.playerState).setVisibility(View.GONE);
////                }
////            }, 1000);
//        }

        // enabling swipe and touch listeners
//        ((YoutubePlayerActivity)mContext).enableVideoTouchListeners();

    }
//
//    // User controls
//
//    private void updateButtonVisibilities() {
//        retryButton.setVisibility(playerNeedsPrepare ? View.VISIBLE : View.GONE);
//        videoButton.setVisibility(haveTracks(DemoPlayer.TYPE_VIDEO) ? View.VISIBLE : View.GONE);
//        audioButton.setVisibility(haveTracks(DemoPlayer.TYPE_AUDIO) ? View.VISIBLE : View.GONE);
//        textButton.setVisibility(haveTracks(DemoPlayer.TYPE_TEXT) ? View.VISIBLE : View.GONE);
//    }

    private boolean haveTracks(int type) {
        return player != null && player.getTrackCount(type) > 0;
    }

    public void showVideoPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        configurePopupWithTracks(popup, null, DemoPlayer.TYPE_VIDEO);
        popup.show();
    }

    public void showAudioPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
        final MenuItem backgroundAudioItem = menu.findItem(0);
        backgroundAudioItem.setCheckable(true);
        backgroundAudioItem.setChecked(enableBackgroundAudio);
        OnMenuItemClickListener clickListener = new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item == backgroundAudioItem) {
                    enableBackgroundAudio = !item.isChecked();
                    return true;
                }
                return false;
            }
        };
        configurePopupWithTracks(popup, clickListener, DemoPlayer.TYPE_AUDIO);
        popup.show();
    }

    public void showTextPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        configurePopupWithTracks(popup, null, DemoPlayer.TYPE_TEXT);
        popup.show();
    }

    public void showVerboseLogPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose);
        menu.setGroupCheckable(Menu.NONE, true, true);
        menu.findItem((VerboseLogUtil.areAllTagsEnabled()) ? 1 : 0).setChecked(true);
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 0) {
                    VerboseLogUtil.setEnableAllTags(false);
                } else {
                    VerboseLogUtil.setEnableAllTags(true);
                }
                return true;
            }
        });
        popup.show();
    }

    private void configurePopupWithTracks(PopupMenu popup,
                                          final OnMenuItemClickListener customActionClickListener,
                                          final int trackType) {
        if (player == null) {
            return;
        }
        int trackCount = player.getTrackCount(trackType);
        if (trackCount == 0) {
            return;
        }
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return (customActionClickListener != null
                        && customActionClickListener.onMenuItemClick(item))
                        || onTrackItemClick(item, trackType);
            }
        });
        Menu menu = popup.getMenu();
        // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0)
        menu.add(MENU_GROUP_TRACKS, DemoPlayer.DISABLED_TRACK + ID_OFFSET, Menu.NONE, R.string.off);
        if (trackCount == 1 && TextUtils.isEmpty(player.getTrackName(trackType, 0))) {
            menu.add(MENU_GROUP_TRACKS, DemoPlayer.PRIMARY_TRACK + ID_OFFSET, Menu.NONE, R.string.on);
        } else {
            for (int i = 0; i < trackCount; i++) {
                menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE, player.getTrackName(trackType, i));
            }
        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
        menu.findItem(player.getSelectedTrackIndex(trackType) + ID_OFFSET).setChecked(true);
    }

    private boolean onTrackItemClick(MenuItem item, int type) {
        if (player == null || item.getGroupId() != MENU_GROUP_TRACKS) {
            return false;
        }
        player.selectTrack(type, item.getItemId() - ID_OFFSET);
        return true;
    }
//
//    public void toggleControlsVisibility()  {
//        if ((contentType == 3) || (contentType==2 && isStopped)) {
//            if (!isFullScreen) {
//                if (mediaController.isShowing()) {
//                    mediaController.hide();
//                } else {
//                    showControls();
//                }
//            } else {
//                mediaController.hide();
//            }
//        }
//    }

    public void hideMediaController(){
//        mediaController.hide();
    }

//    private void showControls() {
//        mHandler.removeCallbacks(mRunnable);
//        if (!isFullScreen)
//            mediaController.show(0);
//        mHandler.postDelayed(mRunnable, 4000);
//    }

    // DemoPlayer.CaptionListener implementation

    @Override
    public void onCues(List<Cue> cues) {
        //    subtitleLayout.setCues(cues);
    }

    // DemoPlayer.MetadataListener implementation

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
//        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
//            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
//                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
//                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s",
//                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
//            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
//                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
//                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s",
//                        PrivMetadata.TYPE, privMetadata.owner));
//            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
//                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
//                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
//                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
//                        geobMetadata.description));
//            } else {
//                Log.i(TAG, String.format("ID3 TimedMetadata %s", entry.getKey()));
//            }
//        }
    }

    // SurfaceHolder.Callback implementation

//	@Override
//	public void surfaceCreated(SurfaceHolder holder) {
//		if (player != null) {
//			player.setSurface(holder.getSurface());
//		}
//	}
//
//	@Override
//	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//		// Do nothing.
//	}
//
//	@Override
//	public void surfaceDestroyed(SurfaceHolder holder) {
//		if (player != null) {
//			player.blockingClearSurface();
//		}
//	}

    private void configureSubtitleView() {
        CaptionStyleCompat captionStyle;
        float captionFontScale;
        if (Util.SDK_INT >= 19) {
            captionStyle = getUserCaptionStyleV19();
            captionFontScale = getUserCaptionFontScaleV19();
        } else {
            captionStyle = CaptionStyleCompat.DEFAULT;
            captionFontScale = 1.0f;
        }
        //    subtitleLayout.setStyle(captionStyle);
        //    subtitleLayout.setFontScale(captionFontScale);
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        AppLibrary.log_d(TAG,"On surface texture available");
        if(surface != null) {
            surface.release();
            surface = null;
        }

        surface = new Surface(surfaceTexture);
        if(player == null)
            onResume();
        else if (player != null) {
            player.setSurface(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        //Do Nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (player != null) {
            AppLibrary.log_d(TAG,"On surface texture destroyed");
            player.blockingClearSurface();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        //Do Nothing
    }

    public void rePlayMedia() {
        player.seekTo(0L);
        player.getPlayerControl().start();
    }

    public void surfaceRelease() {
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }
}
