package com.pulseapp.android.broadcast;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.Connectivity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by admin on 1/28/2015.
 */
public class BroadCastSettings {

    public final int videoBitRate;
    private String defaultVideoSize;
    private int videoFrameRate;
    private String videoSize;
    private int videoWidth;
    private int videoHeight;
    private float videoAspect;

    private String defaultEncoderSize;
    private int defaultEncoderWidth;
    private int defaultEncoderHeight;

    public final int audioSamplingRate;
    public final int audioBitRate;

    private static int NO_OF_CAMERAS = 0;
    private int cameraId = 0;

    public static BroadCastSettings LOW_QUALITY;
    public static BroadCastSettings MEDIUM_QUALITY;
    public static BroadCastSettings HIGH_QUALITY;
    public static BroadCastSettings HD_QUALITY_RECORDING;
    public static BroadCastSettings HD_QUALITY_SHARING;
    private static final boolean mPortrait = true;

    public static double aspectRatio = mPortrait ? 1.7777778d: 1.0/1.777778d;

    private BroadCastSettings (int videoBitRate, int encoderWidth, int encoderHeight, int videoFrameRate, int audioBitRate, int audioSamplingRate) {
        this.videoBitRate   = videoBitRate;
        this.videoFrameRate = videoFrameRate;
        if(mPortrait) {
            int width = encoderWidth;
            encoderWidth = encoderHeight;
            encoderHeight = width;
        }
        this.defaultEncoderSize = encoderWidth + "x" + encoderHeight;
        this.defaultEncoderWidth = encoderWidth;
        this.defaultEncoderHeight = encoderHeight;

        this.videoWidth = 1920;
        this.videoHeight = 1440; // Set it to 960 just in case 4:3 is chosen, which would correspond to 1200x900 or 1280x960
        this.defaultVideoSize = videoWidth + "x" + videoHeight;
        this.videoAspect = mPortrait ? 1.777778f : 1.0f/1.777778f;

        this.audioBitRate   = audioBitRate;
        this.audioSamplingRate = audioSamplingRate;
    }

    public int deviceCameraCount() {
        return NO_OF_CAMERAS;
    }

    public static ArrayList<String> getUploadSettings(float maxUpload) {
        ArrayList<String> settings   =   new ArrayList<>();
        maxUpload   *= 0.8*1024;
        if (maxUpload >= LOW_QUALITY.videoBitRate + LOW_QUALITY.audioBitRate) {
            settings.add("low");
        }
        if (maxUpload >= MEDIUM_QUALITY.videoBitRate + MEDIUM_QUALITY.audioBitRate) {
            settings.add("medium");
        }
        if (maxUpload >= HIGH_QUALITY.videoBitRate + HIGH_QUALITY.audioBitRate) {
            settings.add("high");
        }
        if (maxUpload >= HD_QUALITY_SHARING.videoBitRate + HD_QUALITY_SHARING.audioBitRate) {
            settings.add("hd");
        }
        return settings;
    }

    public static ArrayList<String> getSettings () {
        ArrayList<String> settings   =   new ArrayList<>();
        settings.add("low");
        settings.add("medium");
        settings.add("high");
        settings.add("hd");
        return settings;
    }

    public static BroadCastSettings getInstance (String quality){
        if (quality.equals("low"))
            return LOW_QUALITY;
        if (quality.equals("high"))
            return HIGH_QUALITY;
        if (quality.equals("hd"))
            return HD_QUALITY_RECORDING;
        if (quality.equals("hd_sharing"))
            return HD_QUALITY_SHARING;
        return MEDIUM_QUALITY;
    }

    public static String formatFromQuality (String quality) {
        if (quality.equalsIgnoreCase("low"))
            return "240p";
        else if (quality.equalsIgnoreCase("high"))
            return "480p";
        else if (quality.equalsIgnoreCase("hd"))
            return "720p";
        return "360p";
    }

    public static String qualityFromFormat (String format) {
        if (format.equalsIgnoreCase("240p"))
            return "low";
        else if (format.equalsIgnoreCase("480p"))
            return "high";
        else if (format.equalsIgnoreCase("720p"))
            return "hd";
        return "medium";
    }

    public static void initialize(Context context) {
        LOW_QUALITY         =   new BroadCastSettings(320, 426, 240, 25, 64, 44100);
        HIGH_QUALITY        =   new BroadCastSettings(768, 854, 480, 25, 128, 44100);
        HD_QUALITY_RECORDING  =   new BroadCastSettings(3072, 1280, 720, 25, 128, 44100);
        HD_QUALITY_SHARING  =  new BroadCastSettings(1792, 1280, 720, 25, 128, 44100);     //1536
        NO_OF_CAMERAS       =   Camera.getNumberOfCameras();

        if(Connectivity.isConnectedWifi(context)){
            MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
        }
        else if(Connectivity.isConnectedMobile(context)){
            if(Connectivity.getNetworkClass(context).equalsIgnoreCase("3G")){
                MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
            }else if(Connectivity.getNetworkClass(context).equalsIgnoreCase("2G")){
                MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
            }else if(Connectivity.getNetworkClass(context).equalsIgnoreCase("4G")){
                MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
            }
            else{
                MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
            }
        }
        else{
            MEDIUM_QUALITY      =   new BroadCastSettings(300, 640, 360, 25, 64, 44100);
        }

    }

    public static void destroy () {
        LOW_QUALITY = null;
        MEDIUM_QUALITY = null;
        HIGH_QUALITY = null;
        HD_QUALITY_RECORDING = null;
        HD_QUALITY_SHARING = null;
    }

    public int getVideoWidth() {
        return this.videoWidth;
    }

    public int getVideoHeight() {
        return this.videoHeight;
    }

    public int getVideoFrameRate () {
        return this.videoFrameRate;
    }

    public double getAspectRatio(){
        return aspectRatio;
    }

    public String getVideoSize() {
        if (videoSize == null || videoSize.isEmpty())
            videoSize = defaultVideoSize;
        return videoSize;
    }

    public String getDefaultEncoderSize() {
        return defaultEncoderSize;
    }

    public void swapCamera (boolean changeCamera) {
        if (changeCamera)
            cameraId = (cameraId + 1 )% NO_OF_CAMERAS;
    }

    public int getCameraId() {
        return cameraId;
    }

    public boolean resetPreviewSizes (Camera.Parameters params) {
        /* Setting Camera Video Size */
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null) {
            sizes = params.getSupportedVideoSizes();
        }
        ListIterator<Camera.Size> it = sizes.listIterator();

        Camera.Size prefSize = params.getPreferredPreviewSizeForVideo();

        String [] sizes1 = defaultVideoSize.split("x");
        int defaultWidth = Integer.parseInt(sizes1[0]);
        int defaultHeight = Integer.parseInt(sizes1[1]);

        if(prefSize != null) {
            Log.d("Instalively", "Preferred preview size returns " + prefSize.width + "x" + prefSize.height);
//            videoAspect = (float) prefSize.width / prefSize.height;
            // No need to change defaultVideoSize here... Because they are just used to set max limit on chosen resolution
            // which doesn't change regardless of what the chosen aspect ratio is.
            if((defaultWidth > prefSize.width) || (defaultHeight > prefSize.height)) {
                defaultWidth = prefSize.width;
                defaultHeight = prefSize.height;
            }
        }

        float MAX_DIFF = 100.0f;
        Camera.Size mSelectedSize = null;   //  sizes.get(0)

        while (it.hasNext()) {
            Camera.Size size = (Camera.Size) it.next();
            int width = Integer.valueOf(size.width);
            int height = Integer.valueOf(size.height);
//            int diff = Math.abs((defaultWidth - width + 1) * (defaultHeight - height + 1));
            float aspect = (float) width / height;
            float diff = Math.abs(aspect - videoAspect);
            if ((diff <= MAX_DIFF) && ((mSelectedSize == null) || (((size.width >= mSelectedSize.width) && (size.height >= mSelectedSize.height)) || (diff < MAX_DIFF))) && (size.height <= defaultHeight) && (size.width <= defaultWidth)) {
                MAX_DIFF = diff;
                mSelectedSize = size;
                Log.d("Instalively", "Accepting config " + size.width + "x" + size.height + " diff: " + diff);
            } else {
                Log.d("Instalively", "Rejecting config " + size.width + "x" + size.height + " diff: " + diff);
            }
        }
        if (mSelectedSize == null || MAX_DIFF > 99.0f) {
            Log.e("InstaLively", "Device does not support Our video streaming setup.");
            return false;
        }

        this.videoSize = mSelectedSize.width + "x" + mSelectedSize.height;
        this.videoWidth = mSelectedSize.width;
        this.videoHeight = mSelectedSize.height;

        AppLibrary.log_i("Video Size in Broadcast Settings:", videoSize);
        aspectRatio = (double) mSelectedSize.width / (mSelectedSize.height);
        AppLibrary.log_i("I/Aspect Ratio in BroadcastSettings is:", "" + aspectRatio);

        /* Setting Video Frame rate */
        List<int[]> fpsRanges = params.getSupportedPreviewFpsRange();
        List<Integer> availFps = new ArrayList<Integer>();
        for (int[] range22 : fpsRanges) {
            for (int i = range22[0] / 1000; i <= range22[1] / 1000; i++) {
                if (!availFps.contains(i)) {
                    availFps.add(i);
                }
            }
        }
        while (!availFps.contains(this.videoFrameRate)) {
            this.videoFrameRate--;
        }
        if (this.videoFrameRate < 10) {
            Log.e("InstaLively", "FPS value coming too low.");
            return false;
        }
        return true;
    }
}
