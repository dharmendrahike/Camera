package com.pulseapp.android.broadcast;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Handler;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.widget.TextView;
import android.content.Context;

import com.pulseapp.android.signals.BroadCastSignals;

import java.io.File;
import java.util.concurrent.locks.Lock;

/**
 * Created by admin on 1/15/2015.
 */

public abstract class StreamingActionController implements
        SurfaceHolder.Callback,
        Runnable,
        IVideoCodec.Callback,
        IAudioCodec.Callback,
        RtmpMuxer.Callback {
    protected static final long BYTE_UPDATE_TIMEOUT = 1000;
    protected Handler handler;
    protected SurfaceHolder holder;
    protected long audioBytes;
    protected IAudioCodec audioCodec = null;
    protected Camera camera = null;
    protected long lastByteUpdateTime;
//    protected PreviewLayout layout = null;
    protected Camera.Size previewSize;
    public boolean publishing;
    protected RtmpMuxer rtmp = null;
    protected TextView textMessage;
    protected Thread thread;
    protected long videoBytes;
    protected IVideoCodec mVideoCodec;
    protected BroadCastSettings mSettings;
    protected String rtmpUrl;
    protected int orientationResult = 1000;
    protected int result;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;
    protected String mCameraPreviewSize;
    protected int mCameraPreviewFrameRate;
    protected int mCameraPreviewWidth;
    protected int mCameraPreviewHeight;
    protected double mCameraPreviewAspectRatio;

    protected String SharedPreferencesIdentity = "mFinalSettings_19_05_2016";

    protected static final String SPI_PreviewSize = "PreviewSize";
    protected static final String SPI_PreviewWidth = "PreviewWidth";
    protected static final String SPI_PreviewHeight = "PreviewHeight";
    protected static final String SPI_PreviewAspectRatio = "PreviewAspectRatio";
    protected static final String SPI_PreviewFrameRate = "PreviewFrameRate";
    protected static final String SPI_Focus = "FocusMode";

    Lock mCameraLock;

    SparseArray<CameraProperties> mCameraProperties;

    public abstract void onPictureTakenFinished(Bitmap bitmap);

    public abstract void rendererReady();

    public class CameraProperties {
        String mPreviewSize;
        int mPreviewFrameRate;
        int mPreviewWidth;
        int mPreviewHeight;
        double mPreviewAspectRatio;
        boolean stored;
    }

    protected String smoothFocusMode;
    public File file;
    protected boolean stored = false;

    public abstract void prepare();
    public abstract void stopCamera();
    public abstract void startCamera();
    public abstract void actionPublish();
    public abstract void asyncStopRtmpPublishing();
    public abstract boolean isPublishing();
    public abstract void setStreamKey (String key, String format);
    public abstract void destroy();
    public abstract void restartController (BroadCastSignals.RestartBroadCastSignal rsSignal);
    public abstract FilterManager getFilter ();
    public abstract void setNewFilter (FilterManager mSetFilter);
    public abstract void resumeView() throws Exception;
    public abstract void pauseView();
    public abstract void startRecording();
    public abstract void continueWithoutPopups();
    public abstract boolean isRecorderRunning();
    public abstract Thread isThreadWorking();
    public abstract void destroyThread();
    public abstract String getRecordingDirectory();
    public abstract void setCameraControlCallback(StreamingActionControllerKitKat.CameraControlCallback cameraControlCallback);
    public abstract void setFilterControlCallback(StreamingActionControllerKitKat.FilterControlCallback filterControlCallback);
    public abstract void setDoubleShotCallback(CameraShotCallback cameraShotCallback);
    public abstract void CaptureImage(final String ImgName);
    public abstract int decrementFilter();
    public abstract int incrementFilter();
    public abstract void BitrateIncreaseForUploadMode();
}