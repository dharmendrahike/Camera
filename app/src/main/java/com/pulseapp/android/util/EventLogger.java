package com.pulseapp.android.util;

/**
 * Created by Morph on 8/3/2015.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec.CryptoException;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.util.VerboseLogUtil;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Logs player events using {@link Log}.
 */
public class EventLogger implements ExoPlayer.Listener, ExoPlayer.InfoListener,
        ExoPlayer.InternalErrorListener {

    private static final String TAG = "EventLogger";
    private static final NumberFormat TIME_FORMAT;
    static {
        TIME_FORMAT = NumberFormat.getInstance(Locale.US);
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
    }

    private long sessionStartTimeMs;
    private long[] loadStartTimeMs;
    private long[] seekRangeValuesUs;
    private TextureView textureView;
    private Context context;
    private Bitmap blurredBitmap;
    private View root;
    private boolean readyCheck = false;
    private boolean endState = false;

    public EventLogger(TextureView textureView, Context context, View root) {
        loadStartTimeMs = new long[ExoPlayer.RENDERER_COUNT];
        this.textureView = textureView;
        this.context = context;
        this.root = root;
    }

    public void startSession() {
        sessionStartTimeMs = SystemClock.elapsedRealtime();
        Log.d(TAG, "start [0]");
    }

    public void endSession() {
        Log.d(TAG, "end [" + getSessionTimeString() + "]");
    }

    // ExoPlayer.Listener

    @Override
    public void onStateChanged(boolean playWhenReady, int state) {
        Log.d(TAG, "state [" + getSessionTimeString() + ", " + playWhenReady + ", "
                + getStateString(state) + "]");
        if(getStateString(state).contains("R"))
            readyCheck = true;  //Checks if Ready state has been reached atleast once for the video
        else if(getStateString(state).contains("E") || getStateString(state).contains("P"))
            readyCheck = false;  //Resets ready check if the video has ended

        if(RecordedVideoController.getInstance().repreparing)
            return;

//        if(!playWhenReady && getStateString(state).contains("R")) {
//            Bitmap bitmap = textureView.getBitmap();
//            if(bitmap!=null) {
//                blurredBitmap = BlurBuilder.blur(context, bitmap);
//                ImageView mImg = (ImageView) root.findViewById(R.id.playerThumbnail);
//                mImg.setImageBitmap(blurredBitmap);
//            }
//
//            TextView mText = (TextView)root.findViewById(R.id.playerStateText);
//
//            if(!endState) {
//                AppLibrary.log_d(TAG,"Show play assets");
//                RecordedVideoController.getInstance().removeJumpingBeans();
//                mText.setText("PAUSED");
//                mText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//                root.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
//                if(root.findViewById(R.id.replayButton).getVisibility() == View.VISIBLE)
//                    root.findViewById(R.id.replayButton).setVisibility(View.GONE);
//            } else {
//                AppLibrary.log_d(TAG,"Video has been ended");
//                RecordedVideoController.getInstance().removeJumpingBeans();
//                ((TextView) root.findViewById(R.id.playerStateText)).setText("ENDED");
//                ((TextView)root.findViewById(R.id.playerStateText)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//                endState = false;
//                showReplayAssets();
//            }
//            root.findViewById(R.id.playerThumbnail).setVisibility(View.VISIBLE);
//            root.findViewById(R.id.playerState).setVisibility(View.VISIBLE);
//        }else if(readyCheck && getStateString(state).contains("B")) {
//            Bitmap bitmap = textureView.getBitmap();
//            if(bitmap!=null){
//                blurredBitmap = BlurBuilder.blur(context, bitmap);
//                ImageView mImg = (ImageView) root.findViewById(R.id.playerThumbnail);
//                mImg.setImageBitmap(blurredBitmap);
//            }
//
//            TextView mText = (TextView)root.findViewById(R.id.playerStateText);
//            mText.setText("BUFFERING...");
//            mText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.buffering_48, 0, 0, 0);
//            RecordedVideoController.getInstance().appendJumpingBeans();
//            root.findViewById(R.id.playerThumbnail).setVisibility(View.VISIBLE);
//            root.findViewById(R.id.playerState).setVisibility(View.VISIBLE);
//        } else if(getStateString(state).contains("E")) {
//            endState = true;
//        } else if(readyCheck) {
//            root.findViewById(R.id.playerThumbnail).setVisibility(View.GONE);
//            root.findViewById(R.id.playerState).setVisibility(View.GONE);
//            RecordedVideoController.getInstance().removeJumpingBeans();
//            root.findViewById(R.id.playButton).setVisibility(View.GONE);
//            root.findViewById(R.id.replayButton).setVisibility(View.GONE);
//        }
    }

//    public void updateEndedState(){
//        if (((YoutubePlayerActivity)context).isComingFromBroadcast())
//            RecordedVideoController.getInstance().repreparing = true;
//
//        Bitmap bitmap = textureView.getBitmap();
//        if(bitmap!=null){
//            blurredBitmap = BlurBuilder.blur(context, bitmap);
//            ImageView mImg = (ImageView) root.findViewById(R.id.playerThumbnail);
//            mImg.setImageBitmap(blurredBitmap);
//        }
//
//        RecordedVideoController.getInstance().removeJumpingBeans();
//        TextView mText = (TextView)root.findViewById(R.id.playerStateText);
//        mText.setText("ENDED");
//        mText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//        root.findViewById(R.id.playerThumbnail).setVisibility(View.VISIBLE);
//        root.findViewById(R.id.playerState).setVisibility(View.VISIBLE);
//    }

//    public void showReplayAssets() {
//        if (((YoutubePlayerActivity)context).isComingFromBroadcast()){
//            root.findViewById(R.id.replayButton).setVisibility(View.VISIBLE);
//            root.findViewById(R.id.replayButton).setClickable(true);
//        }else {
//            if (RecordedVideoController.getInstance().contentType == 2) {
//                if (YoutubePlayerActivity.video_time != 0) {
//                    root.findViewById(R.id.replayButton).setVisibility(View.VISIBLE);
//                    root.findViewById(R.id.replayButton).setClickable(true);
//                }
//                ((YoutubePlayerActivity) context).updateLiveAsset();
//            } else if ((RecordedVideoController.getInstance().contentType == 3)) {
//                root.findViewById(R.id.replayButton).setVisibility(View.VISIBLE);
//                root.findViewById(R.id.replayButton).setClickable(true);
//            }
//        }
//    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "playerFailed [" + getSessionTimeString() + "]", e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        Log.d(TAG, "videoSizeChanged [" + width + ", " + height + ", " + pixelWidthHeightRatio + "]");
    }

    // ExoPlayer.InfoListener

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        Log.d(TAG, "bandwidth [" + getSessionTimeString() + ", " + bytes + ", "
                + getTimeString(elapsedMs) + ", " + bitrateEstimate + "]");
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        Log.d(TAG, "droppedFrames [" + getSessionTimeString() + ", " + count + "]");
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
                              int mediaStartTimeMs, int mediaEndTimeMs) {
        loadStartTimeMs[sourceId] = SystemClock.elapsedRealtime();
        if (VerboseLogUtil.isTagEnabled(TAG)) {
            Log.v(TAG, "loadStart [" + getSessionTimeString() + ", " + sourceId + ", " + type
                    + ", " + mediaStartTimeMs + ", " + mediaEndTimeMs + "]");
        }
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format,
                                int mediaStartTimeMs, int mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (VerboseLogUtil.isTagEnabled(TAG)) {
            long downloadTime = SystemClock.elapsedRealtime() - loadStartTimeMs[sourceId];
            Log.v(TAG, "loadEnd [" + getSessionTimeString() + ", " + sourceId + ", " + downloadTime
                    + "]");
        }
    }

    @Override
    public void onVideoFormatEnabled(Format format, int trigger, int mediaTimeMs) {
        Log.d(TAG, "videoFormat [" + getSessionTimeString() + ", " + format.id + ", "
                + Integer.toString(trigger) + "]");
    }

    @Override
    public void onAudioFormatEnabled(Format format, int trigger, int mediaTimeMs) {
        Log.d(TAG, "audioFormat [" + getSessionTimeString() + ", " + format.id + ", "
                + Integer.toString(trigger) + "]");
    }

    // ExoPlayer.InternalErrorListener

    @Override
    public void onLoadError(int sourceId, IOException e) {
        printInternalError("loadError", e);
    }

    @Override
    public void onRendererInitializationError(Exception e) {
        printInternalError("rendererInitError", e);
    }

    @Override
    public void onDrmSessionManagerError(Exception e) {
        printInternalError("drmSessionManagerError", e);
    }

    @Override
    public void onDecoderInitializationError(DecoderInitializationException e) {
        printInternalError("decoderInitializationError", e);
        RecordedVideoController.getInstance().releasePlayer(true);
        RecordedVideoController.getInstance().onResume();
    }

    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        printInternalError("audioTrackInitializationError", e);
    }

    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        printInternalError("audioTrackWriteError", e);
    }

    @Override
    public void onCryptoError(CryptoException e) {
        printInternalError("cryptoError", e);
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                                     long initializationDurationMs) {
        Log.d(TAG, "decoderInitialized [" + getSessionTimeString() + ", " + decoderName + "]");
    }

    @Override
    public void onSeekRangeChanged(TimeRange seekRange) {
        seekRangeValuesUs = seekRange.getCurrentBoundsUs(seekRangeValuesUs);
        Log.d(TAG, "seekRange [ " + seekRange.type + ", " + seekRangeValuesUs[0] + ", "
                + seekRangeValuesUs[1] + "]");
    }

    private void printInternalError(String type, Exception e) {
        Log.e(TAG, "internalError [" + getSessionTimeString() + ", " + type + "]", e);
    }

    private String getStateString(int state) {
        switch (state) {
            case com.google.android.exoplayer.ExoPlayer.STATE_BUFFERING:
                return "B";
            case com.google.android.exoplayer.ExoPlayer.STATE_ENDED:
                return "E";
            case com.google.android.exoplayer.ExoPlayer.STATE_IDLE:
                return "I";
            case com.google.android.exoplayer.ExoPlayer.STATE_PREPARING:
                return "P";
            case com.google.android.exoplayer.ExoPlayer.STATE_READY:
                return "R";
            default:
                return "?";
        }
    }

    private String getSessionTimeString() {
        return getTimeString(SystemClock.elapsedRealtime() - sessionStartTimeMs);
    }

    private String getTimeString(long timeMs) {
        return TIME_FORMAT.format((timeMs) / 1000f);
    }

}