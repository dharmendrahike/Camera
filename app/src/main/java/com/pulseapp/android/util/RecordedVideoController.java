package com.pulseapp.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.VerboseLogUtil;
//import com.google.common.base.Stopwatch;
import com.pulseapp.android.ExoPlayer.*;
import com.pulseapp.android.R;
import com.pulseapp.android.broadcast.StreamingActionControllerKitKat;
//import com.instalively.android.fragments.CreateEventFragment;

//import net.frakbot.jumpingbeans.JumpingBeans;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

//import org.json.JSONException;
//
//import java.io.File;
//import java.net.CookieHandler;
//import java.net.CookieManager;
//import java.net.CookiePolicy;

/**
 * Created by Morph on 7/30/2015.
 */
final public class RecordedVideoController implements
        ExoPlayer.Listener, ExoPlayer.CaptionListener, ExoPlayer.Id3MetadataListener,
        AudioCapabilitiesReceiver.Listener,TextureView.SurfaceTextureListener {

    public static RecordedVideoController recorderVideoController;
    public static final int TYPE_MP4 = 3;
    private static final String TAG = "RecordedVideoController";
    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;

//    private static final CookieManager defaultCookieManager;
//    static {
//        defaultCookieManager = new CookieManager();
//        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
//    }

    private com.pulseapp.android.util.EventLogger eventLogger;
    public static CustomMediaController mediaController;
    private com.pulseapp.android.ExoPlayer.AspectRatioFrameLayout videoFrame;
    private SurfaceView surface_view;

    private ExoPlayer player;
    private boolean playerNeedsPrepare;

    private long playerPosition;
    private boolean enableBackgroundAudio;

    private Uri contentUri;
    public int contentType;
    private Surface surface;
    private Runnable mRunnable;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;
    private Context mContext;
    private View rootView;
    private boolean isPaused = false;
    private TextureView textureView;
    public  boolean isFullScreen = false;
    public boolean transition_to_complete = false;
    private boolean reInitialize = false;
    public boolean repreparing = false;
//    private Stopwatch stopwatch = null;
    private android.os.Handler mHandler = new android.os.Handler();
    private android.os.Handler RestartHandler = new android.os.Handler();
//    private JumpingBeans jumpingBeans;


    private RecordedVideoController(){}

    public static RecordedVideoController getInstance(){
        if (recorderVideoController == null){
            recorderVideoController = new RecordedVideoController();
        }
        return recorderVideoController;
    }

    public void initializeVideoController(Context context, final View root){
        String uri = StreamingActionControllerKitKat.getLastRecordedFilepath();
        rootView = root;
        Log.d(TAG,"File Url - "+uri);
        contentUri =  Uri.parse(uri);
        contentType = 3;
        mContext = context;

//        jumpingBeans = JumpingBeans.with((TextView)rootView.findViewById(R.id.playerStateText))
//                .appendJumpingDots()
//                .build();

//        videoFrame = (com.instalively.android.ExoPlayer.AspectRatioFrameLayout) root.findViewById(R.id.video_frame);
        videoFrame.setAspectRatio(1.777778f);
        mediaController = new CustomMediaController(mContext);
        mediaController.setAnchorView(videoFrame);
//        rootView.findViewById(R.id.replayButton).setOnClickListener(new View.OnClickListener() {
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

//        rootView.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                player.getPlayerControl().start();
//                rootView.findViewById(R.id.playButton).setVisibility(View.GONE);
//                mediaController.updatePausePlay();
//            }
//        });
//        root.setVisibility(View.VISIBLE);
//        textureView = (TextureView) rootView.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);
        if(textureView.isAvailable()) {
            onSurfaceTextureAvailable(textureView.getSurfaceTexture(), 0, 0);
        }
//        root.findViewById(R.id.surface_view).setVisibility(View.VISIBLE);
//        YoYo.with(Techniques.FadeIn).duration(500).playOn(rootView);

//        CreateEventFragment.setPopupTag();

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(mContext.getApplicationContext(), this);

//        root.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
////                    toggleControlsVisibility();
//                    System.out.println("ONTouch view");
//                    if (isPaused) {
//                        audioCapabilitiesReceiver.register();
//                        isPaused = false;
//                    }else {
//                        audioCapabilitiesReceiver.unregister();
//                        isPaused = true;
//                    }
//                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    System.out.println("Action Up motion");
////                    view.performClick();
//                }
//                return true;
//            }
//        });
//        rootView.findViewById(R.id.video_frame).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (player != null) {
//                    if (isPaused) {
//                        player.getPlayerControl().start();
//                        isPaused = false;
//                    } else {
//                        if (player.getPlayerControl().canPause())
//                            player.getPlayerControl().pause();
//                        isPaused = true;
//                    }
//                }
//            }
//        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    return mediaController.dispatchKeyEvent(event);
                }
                return false;
            }
        });
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mediaController.hide();
//				rootView.findViewById(R.id.pauseButton).setVisibility(View.GONE);
//				rootView.findViewById(R.id.playButton).setVisibility(View.GONE);
            }
        };
//        surface_view = (SurfaceView) root.findViewById(R.id.surface_view);
//        surface_view.getHolder().addCallback(this);
//        onResume();
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
//                .appendJumpingDots()
//                .build();
//    }

    public void configureMediaController(){
//        mediaController.setAnchorView(rootView.findViewById(R.id.video_frame));
    }

    public void reInitializePlayer(boolean isLive,String url){
        if (isLive)
            contentType = 2;
        else
            contentType = 3;

        contentUri = Uri.parse(url);
        reInitialize = true;
    }

    public void rePreparePlayer(){
        if(reInitialize) {
            RestartHandler.removeCallbacksAndMessages(null);
            AppLibrary.log_d(TAG, "Re preparing the player");
            releasePlayer(true);
            preparePlayer();
            reInitialize = false;
        } else {
            AppLibrary.log_d(TAG, "Will try to call go live after 300ms!");
            //Check after 300ms
            RestartHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    rePreparePlayer();
                }
            }, 300);
        }
    }

    public void onResume(){
        AppLibrary.log_d(TAG, "Resuming Player");
        // The player will be prepared on receiving audio capabilities.
        try {
            audioCapabilitiesReceiver.register();
        }catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void onPause(){
        AppLibrary.log_d(TAG, "Pausing Player");
        releasePlayer(false);
        try {
            audioCapabilitiesReceiver.unregister();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public void onDestroy(){
        AppLibrary.log_d(TAG,"Destroy Player");
        releasePlayer(true);
        isFullScreen = false;
        transition_to_complete = false;
        repreparing = false;
//        stopwatch = null;
        reInitialize = false;
        if (RestartHandler != null)
            RestartHandler.removeCallbacksAndMessages(null);
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
    }


    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        AppLibrary.log_d(TAG,"Audio Capabilities Changed");
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            AppLibrary.log_d(TAG,"Re Preparing Player");
            this.audioCapabilities = audioCapabilities;
            releasePlayer(false);
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    // Internal methods

    public ExoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(mContext, "InstaLively");

        switch (contentType) {

            case TYPE_MP4:
                return new ExtractorRendererBuilder(mContext, userAgent, contentUri, new Mp4Extractor());

            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    private void preparePlayer() {
        if (player == null) {
            player = new ExoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setCaptionListener(this);
            player.setMetadataListener(this);
//            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            mediaController.setMediaPlayer(player.getPlayerControl());
            mediaController.setEnabled(true);
            eventLogger = new com.pulseapp.android.util.EventLogger(textureView, mContext,rootView);
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        if(surface!=null)
            player.setSurface(surface);
//        player.setSurface(surface_view.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

//    public void updatePlayerControls(){
//        if (rootView.findViewById(R.id.replayButton).getVisibility() == View.VISIBLE)
//            rootView.findViewById(R.id.replayButton).setVisibility(View.GONE);
//    }

    public void releasePlayer(boolean releaseComplete) {
        if (player != null) {
            //      debugViewHelper.stop();
            //      debugViewHelper = null;
            AppLibrary.log_d(TAG,"Releasing Player");
            if (releaseComplete) {
                playerPosition = 0L;
                player.seekTo(playerPosition);
//				hasEnded = false;
            } else {
                playerPosition = player.getCurrentPosition();
            }
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
            stopTimer();
        }
    }

    // ExoPlayer.Listener implementation
    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case com.google.android.exoplayer.ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                if(mRunnable!=null && mHandler!=null)
                    mHandler.removeCallbacks(mRunnable);
//                if (stopwatch != null && playWhenReady){
//                    // stopping the stopwatch when the video is buffering
//                    stopTimer();
////                    if ((stopwatch.elapsedMillis()/1000) >= YoutubePlayerActivity.video_time){
////                        //video completed
////                        AppLibrary.log_d(TAG, "Video Completed ,elapsed time -" + stopwatch.elapsedMillis() / 1000);
////                        transition_to_complete = true;
////                        ((YoutubePlayerActivity)mContext).getVideoStreamUrl();
////                    }
//                }
                if(transition_to_complete && playWhenReady) {
                    AppLibrary.log_d(TAG,"Stream ended");
                    onStreamEnded();
                    rePreparePlayer();
                    transition_to_complete = false;
                }
                break;
            case com.google.android.exoplayer.ExoPlayer.STATE_ENDED:
                text += "ended";
                AppLibrary.log_d(TAG,"player state ended");
                if(playWhenReady) {
                    playerPosition = 0L;
                    player.seekTo(0);
                    mediaController.doPauseResume();
                }
                break;
            case com.google.android.exoplayer.ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case com.google.android.exoplayer.ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case com.google.android.exoplayer.ExoPlayer.STATE_READY:
                text += "ready";
//                if (stopwatch == null && contentType == 2 && /*YoutubePlayerActivity.video_time != 0 &&*/ playWhenReady && !transition_to_complete){
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

    public void initializeTimer(){
        AppLibrary.log_d(TAG,"Initializing timer");
//        stopwatch = new Stopwatch();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
//                AppLibrary.log_d(TAG,"Timer running -"+stopwatch.elapsedMillis()/1000);
            }
        }, 0, 1000);

        startTimer(); //Starts the timer after initializing
    }

    public void stopTimer(){
//        if (stopwatch != null && stopwatch.isRunning()) {
//            AppLibrary.log_d(TAG,"Stopping timer,elapsed Time -"+stopwatch.elapsedMillis()/1000);
//            stopwatch.stop();
//        }
    }
//
    public void startTimer(){
//        if (stopwatch != null && !stopwatch.isRunning()) {
//            AppLibrary.log_d(TAG,"Starting timer,elapsed Time -"+stopwatch.elapsedMillis()/1000);
//            stopwatch.start();
//        }
    }

    public void onStreamEnded(){
//        eventLogger.updateEndedState();
    }


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
//        rootView.findViewById(R.id.video_frame).setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

        if(repreparing) {
//            eventLogger.showReplayAssets();
            AppLibrary.log_d(TAG, "Showing replay assets");
        } else {
//            new android.os.Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {  //Since Video doesn't start playing immediately, remove the overlay with a delay
//                    rootView.findViewById(R.id.playerThumbnail).setVisibility(View.GONE);
//                    removeJumpingBeans();
//                    rootView.findViewById(R.id.playerState).setVisibility(View.GONE);
//                }
//            }, 1000);
        }

        // enabling swipe and touch listeners
//        ((YoutubePlayerActivity)mContext).enableVideoTouchListeners();

    }

    // User controls


    private boolean haveTracks(int type) {
        return player != null && player.getTrackCount(type) > 0;
    }

    public void showVideoPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        configurePopupWithTracks(popup, null, ExoPlayer.TYPE_VIDEO);
        popup.show();
    }

    public void showAudioPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
        final MenuItem backgroundAudioItem = menu.findItem(0);
        backgroundAudioItem.setCheckable(true);
        backgroundAudioItem.setChecked(enableBackgroundAudio);
        PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item == backgroundAudioItem) {
                    enableBackgroundAudio = !item.isChecked();
                    return true;
                }
                return false;
            }
        };
        configurePopupWithTracks(popup, clickListener, ExoPlayer.TYPE_AUDIO);
        popup.show();
    }

    public void showTextPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        configurePopupWithTracks(popup, null, ExoPlayer.TYPE_TEXT);
        popup.show();
    }

    public void showVerboseLogPopup(View v) {
        PopupMenu popup = new PopupMenu(mContext, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.logging_normal);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.logging_verbose);
        menu.setGroupCheckable(Menu.NONE, true, true);
        menu.findItem((VerboseLogUtil.areAllTagsEnabled()) ? 1 : 0).setChecked(true);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
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
                                          final PopupMenu.OnMenuItemClickListener customActionClickListener,
                                          final int trackType) {
        if (player == null) {
            return;
        }
        int trackCount = player.getTrackCount(trackType);
        if (trackCount == 0) {
            return;
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return (customActionClickListener != null
                        && customActionClickListener.onMenuItemClick(item))
                        || onTrackItemClick(item, trackType);
            }
        });
        Menu menu = popup.getMenu();
        // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0)
        menu.add(MENU_GROUP_TRACKS, ExoPlayer.DISABLED_TRACK + ID_OFFSET, Menu.NONE, R.string.off);
        if (trackCount == 1 && TextUtils.isEmpty(player.getTrackName(trackType, 0))) {
            menu.add(MENU_GROUP_TRACKS, ExoPlayer.PRIMARY_TRACK + ID_OFFSET, Menu.NONE, R.string.on);
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

    public void toggleControlsVisibility()  {
        if (contentType == 3) {
            if (!isFullScreen) {
                if (mediaController.isShowing()) {
                    mediaController.hide();
                } else {
                    showControls();
                }
            } else {
                mediaController.hide();
            }
        }
    }

    public void hideMediaController(){
        mediaController.hide();
    }

    private void showControls() {
        mHandler.removeCallbacks(mRunnable);
        if (!isFullScreen)
            mediaController.show(0);
        mHandler.postDelayed(mRunnable, 4000);
    }

    // ExoPlayer.CaptionListener implementation

    @Override
    public void onCues(List<Cue> cues) {
    }

    // ExoPlayer.MetadataListener implementation

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }

    // SurfaceHolder.Callback implementation
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        if (player != null) {
//            player.setSurface(holder.getSurface());
//        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        // Do nothing.
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        if (player != null) {
//            player.blockingClearSurface();
//        }
//    }

    private void configureSubtitleView() {
//        CaptionStyleCompat captionStyle;
//        float captionFontScale;
//        if (Util.SDK_INT >= 19) {
//            captionStyle = getUserCaptionStyleV19();
//            captionFontScale = getUserCaptionFontScaleV19();
//        } else {
//            captionStyle = CaptionStyleCompat.DEFAULT;
//            captionFontScale = 1.0f;
//        }
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
        surface = new Surface(surfaceTexture);
        if(player==null)
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


}