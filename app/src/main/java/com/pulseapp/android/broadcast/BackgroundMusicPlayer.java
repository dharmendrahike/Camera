package com.pulseapp.android.broadcast;

import android.media.MediaPlayer;

import com.pulseapp.android.util.AppLibrary;

import java.io.IOException;

/**
 * Created by ShwStppr on 23-Jul-2015.
 */
public class BackgroundMusicPlayer implements Runnable,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnInfoListener {
    public static final boolean BACKGROUND_MUSIC_PLAYBACK_ENABLED = false;
    private static final String TAG = "BackgroundMusicPlayer";
    private static final boolean VERBOSE = false;
    private Thread mThread;
    private MediaPlayer mMediaPlayer = null;
    private String mMediaFilePath = "";
    private boolean isPaused = false;

    //TODO: Add callbacks

    BackgroundMusicPlayer() {}

    public void setMediaFile(String mediaFile) {
        if(this.mThread!=null || (this.mMediaPlayer!=null && this.mMediaPlayer.isPlaying())) {
            AppLibrary.log_e(TAG, "Background player already running!");
        }
        this.mMediaFilePath = mediaFile;
    }

    public void prepare() {
        if(this.mMediaFilePath.isEmpty()) return;
        try {
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setOnCompletionListener(this);
            this.mMediaPlayer.setOnErrorListener(this);
            this.mMediaPlayer.setOnInfoListener(this);
            this.mMediaPlayer.setDataSource(this.mMediaFilePath);
            this.mMediaPlayer.prepare();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if(this.mMediaFilePath.isEmpty()) {
            AppLibrary.log_e(TAG, "Media file path is empty!");
            return;
        }
        AppLibrary.log_d(TAG, "Start");
        this.mThread = new Thread(this);
        this.mThread.start();
    }

    public void stop() {
        AppLibrary.log_d(TAG, "Stop");
        if (this.mMediaPlayer != null) {
            try {
                this.mMediaPlayer.stop();
                this.mMediaPlayer.release();
            } catch (Exception e) {
            }
            this.mMediaPlayer = null;
            this.isPaused = false;
        }
        if (this.mThread != null) {
            try {
                this.mThread.interrupt();
                this.mThread.join();
            } catch (Exception e3) {
            }
            this.mThread = null;
        }
    }

    public void run() {
        AppLibrary.log_d(TAG, "Thread");
        try {
            if(this.mMediaPlayer==null)
                prepare();
            this.mMediaPlayer.start();
            while(true) {
                if(this.mMediaPlayer==null || !this.mMediaPlayer.isPlaying() &&  !this.isPaused) {
                    if(VERBOSE)
                        AppLibrary.log_d(TAG, "Not playing, time to stop!");
                    return;
                }
//                if(VERBOSE)
//                    AppLibrary.log_d(TAG, "Current position:: "+this.mMediaPlayer.getCurrentPosition()+"/"+this.mMediaPlayer.getDuration());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void pause() {
        if(this.mMediaPlayer != null && this.mMediaPlayer.isPlaying() && !this.isPaused) {
            this.mMediaPlayer.pause();
            this.isPaused = true;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Paused!");
        }
    }

    public void resume() {
        if(this.mMediaPlayer != null && this.isPaused) {
            this.mMediaPlayer.start();
            this.isPaused = false;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Resumed!");
        }
    }

    public void onCompletion(MediaPlayer media) {
        if(VERBOSE)
            AppLibrary.log_d(TAG, "Music playback complete!");
        this.stop();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        AppLibrary.log_e(TAG, "MediaPlayer error! " + what + ", " + extra);
        this.stop();
        return true;
    }

    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if(VERBOSE)
            AppLibrary.log_d(TAG, "MediaPlayer info! " + what + ", " + extra);
        return true;
    }
}
