package com.pulseapp.android.downloader;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;

import com.pulseapp.android.ExoPlayer.MediaPlayer;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.ViewPublicMomentFragment;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.AppLibrary;

public class PlaylistController implements ViewPublicMomentFragment.ViewControlsCallback, FireBaseKEYIDS {

    private CountDownTimer timer;
    private android.os.Handler autoPlayHandler = new Handler();
    private ViewPublicMomentFragment viewPublicMomentFragment;
    private final String TAG = "PlaylistController";
    static Context sContext;
    private long timerTime, totalTimePerMedia, streamTime;
    private long totalElapsedTimertime = 0;
    public boolean ignoreTimer;

    public void setViewPublicMomentFragment(ViewPublicMomentFragment viewPublicMomentFragment) {
        this.timerTime = totalTimePerMedia = streamTime = totalElapsedTimertime = 0;//resetting all the variables
        this.viewPublicMomentFragment = viewPublicMomentFragment;
        this.viewPublicMomentFragment.viewControlsCallback = this;
    }

    public void setIgnoreTimer(boolean ignoreTimer) {
        this.ignoreTimer = ignoreTimer;
    }


    /**
     * @param momentModel     for which the particular media was downloaded
     * @param downloadedMedia the media which was downloaded recently;
     *                        always call this function whenever any media is downloaded so that  playlist can
     *                        resume if it was waiting for this media itself
     */
    public void notifyMediaDownloaded(MomentModel momentModel, MomentModel.Media downloadedMedia) {
        if (viewPublicMomentFragment != null && !viewPublicMomentFragment.isViewDestroyed && viewPublicMomentFragment.waitingForMedia != null)
            viewPublicMomentFragment.notifyMediaLoaded(downloadedMedia);
    }

    private Runnable autoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPublicMomentFragment != null) {
                viewPublicMomentFragment.playNextMedia();
            }
        }
    };

    private PlaylistController() {
    }

    public static PlaylistController getInstance(Context context) {
        sContext = context;
        if (playlistController == null) playlistController = new PlaylistController();
        return playlistController;
    }

    private static PlaylistController playlistController;

    @Override
    public void setGestureDetector(GestureDetector gestureDetector) {
    }

    @Override
    public void startAutoPlay(long time) {
        if (ignoreTimer) return;

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
                float angle = ((float) timerTime / totalTimePerMedia) * 360;
                float overallAngle = ((float) (totalElapsedTimertime - timerTime) / streamTime) * 360;

//                viewPublicMomentFragment.viewpageTimer.setAngle(angle);
//                viewPublicMomentFragment.viewpageTimer.setEraserSize((float) (overallAngle) / 360);
//                viewPublicMomentFragment.viewpageTimer.invalidate();
                viewPublicMomentFragment.pieView.setPercentage((angle / 360 * 100));
                viewPublicMomentFragment.mediaTimelineView.update((overallAngle) / 360);

                viewPublicMomentFragment.mediaTimelineView.invalidate();
                viewPublicMomentFragment.pieView.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }


    @Override
    public int getMomentStatus() {
        return 0;
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
        if (ignoreTimer) return;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        autoPlayHandler.removeCallbacksAndMessages(null);
        timer = new CountDownTimer(timerTime, 10) {

            public void onTick(long millisUntilFinished) {
                timerTime = millisUntilFinished;
                float angle = ((float) timerTime / totalTimePerMedia) * 360;
                float overallAngle = ((float) (totalElapsedTimertime - timerTime) / streamTime) * 360;

//                viewPublicMomentFragment.viewpageTimer.setAngle(angle);
//                viewPublicMomentFragment.viewpageTimer.setEraserSize((float) (overallAngle) / 360);
//                viewPublicMomentFragment.viewpageTimer.invalidate();
                viewPublicMomentFragment.pieView.setPercentage((angle / 360 * 100));
                viewPublicMomentFragment.mediaTimelineView.update((overallAngle) / 360);
                viewPublicMomentFragment.mediaTimelineView.invalidate();
                viewPublicMomentFragment.pieView.invalidate();
            }

            public void onFinish() {
                autoPlayHandler.post(autoPlayRunnable);
            }
        }.start();
    }

    @Override
    public void onCloseMomentsFragment(Activity activity, MomentModel model, boolean completePlaylistWatched,boolean jumpToCamera) {
        if (viewPublicMomentFragment.isVideoPlayed())
            MediaPlayer.getInstance().onDestroy();
        Log.d(TAG, "onCloseMomentsFragment");
        if (completePlaylistWatched)
//            DynamicDownloader.getInstance(sContext).onPublicMomentDownloadListener.onMomentStateChangedForViews(model.momentId, SEEN_MOMENT);//// TODO: 8/29/16 for both friends also
            DynamicDownloader.getInstance(sContext).notifyCompleteMomentWatched(model.momentId);


        if (viewPublicMomentFragment.momentType!=null && viewPublicMomentFragment.momentType==DynamicDownloader.MomentType.FRIEND_MOMENT) { //Analytics
            if (completePlaylistWatched)
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FRIEND_STREAM_COMPLETE,AnalyticsEvents.STREAM_ID,model.momentId);
            else
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FRIEND_STREAM_MINIMIZE,AnalyticsEvents.STREAM_ID,model.momentId);
        }
        else if (viewPublicMomentFragment.momentType!=null && viewPublicMomentFragment.momentType==DynamicDownloader.MomentType.FOLLOWER_MOMENT) { //Analytics
            if (completePlaylistWatched)
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FOLLOWER_STREAM_COMPLETE,AnalyticsEvents.STREAM_ID,model.momentId);
            else
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FOLLOWER_STREAM_MINIMIZE,AnalyticsEvents.STREAM_ID,model.momentId);
        }
        else {
            if (completePlaylistWatched)
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.PUBLIC_STREAM_COMPLETE,AnalyticsEvents.STREAM_ID,model.momentId);
            else
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.PUBLIC_STREAM_MINIMIZE,AnalyticsEvents.STREAM_ID,model.momentId);
        }

        if (activity != null)
            ((CameraActivity) activity).onCloseViewPublicMomentsFragment(jumpToCamera);
    }


    @Override
    public void setTotalTime(long totalTime) {
        streamTime = totalTime;
        Log.d(TAG, "total time " + streamTime);
    }

}
