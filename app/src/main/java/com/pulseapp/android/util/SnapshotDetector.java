package com.pulseapp.android.util;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;

import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.FaceBookFriendsModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by karthik on 7/13/2016.
 */
public class SnapshotDetector extends ContentObserver implements FireBaseKEYIDS{

    private static final String TAG = "SnapshotDetector";
    private static String PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private OnScreenshotTakenListener mListener;
    private int mLastTakenTime = 0;
    private final int CALLBACK_DELAY = 2700; //In milliseconds
    private boolean deleteScreenshot = false;
    private Handler handler;
    private HashMap <Integer,HashMap<String,String>> mediaList = new HashMap<>();

    public SnapshotDetector(Handler handler) {
        super(handler);
        this.handler = handler;
    }

    public void setListener(OnScreenshotTakenListener listener) {
        mListener = listener;
    }

    public void addToMediaList(int time, String mediaId, String momentId) {
        HashMap<String,String> object = new HashMap<>();
        object.put(MEDIA_ID,mediaId);
        object.put(MOMENT_ID,momentId);
        mediaList.put(time,object);
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange,null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        AppLibrary.log_i(TAG, "Self change:" + selfChange + " URI: " + uri.toString());

        if (mLastTakenTime == 0) {
            mLastTakenTime = (int)System.currentTimeMillis();
            AppLibrary.log_i(TAG, "Valid screenshot at " + mLastTakenTime);
        }
        else if (((int)System.currentTimeMillis() - mLastTakenTime) > 2000) {
            mLastTakenTime = (int)System.currentTimeMillis();
            AppLibrary.log_i(TAG, "Valid screenshot at " + mLastTakenTime);
        }
        else {
            AppLibrary.log_i(TAG, "Skipping repeat screenshot calls");
            return;
        }

        HashMap<String,String> mediaDetails = findRightMedia(mLastTakenTime);

        if (mListener != null && mediaDetails != null)
            mListener.onScreenshotTaken(mediaDetails);
    }

    private HashMap<String,String> findRightMedia (int mLastTakenTime) {

        HashMap<String,String> mediaDetails = null;
        int modifiedTime = mLastTakenTime - CALLBACK_DELAY; //Need to validate callbackDelay on different phones
        int difference = 0;

        if (mediaList != null && !mediaList.isEmpty()) {
            for (Map.Entry<Integer, HashMap<String,String>> e : mediaList.entrySet()) {
                if (e.getKey() < modifiedTime) {
                    if (difference == 0 || ((modifiedTime - e.getKey()) < difference)) { //Minimizing the difference
                        difference = modifiedTime - e.getKey();
                        mediaDetails = e.getValue();
                    }
                }
            }

            if (mediaDetails == null) {
                AppLibrary.log_i(TAG, "No matching screenshot media found in mediaList");
            }
        } else {
            mediaDetails = null;
        }

        return mediaDetails;
    }

    public void clearMediaList() {
        mediaList.clear();
    }

    public void start(Context context) {
        handler.removeCallbacksAndMessages(null); //To account for the handler postDelayed in stop()
        context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,true,this);
    }

    public void stop (final Context context) {
        final ContentObserver observer = this;
        handler.postDelayed(new Runnable() { //If the screenshot is in the last media, need to account for delayed callback
            @Override
             public void run() {
                if (context!=null && observer!=null)
                    context.getContentResolver().unregisterContentObserver(observer);
            }
        },5000);
    }

    public void deleteScreenshot(boolean deleteScreenshot) {
        this.deleteScreenshot = deleteScreenshot;
    }


//    @Override
//    public void onEvent(int event, String path) {
//        AppLibrary.log_i(TAG, "Event:" + event + "\t" + path);
//
//        if (mLastTakenPath != null && path.equalsIgnoreCase(mLastTakenPath)) {
//            AppLibrary.log_i(TAG, "This event has been observed before.");
//        }
//        else if (((event & FileObserver.CREATE)!=0) || ((event & FileObserver.CLOSE_WRITE)!=0)) {
//            AppLibrary.log_i(TAG, "Gotcha!");
//            mLastTakenPath = path;
//            File file = null;
//
//            if (AppLibrary.checkStringObject(mLastTakenPath)!=null)
//                 file = new File(PATH + path);
//
//                if (deleteScreenshot) {
//                    if (file != null)
//                        file.delete();
//
//                /*
//                * A null uri is returned to listener once screenshot
//                * has been deleted.
//                * */
//                    if (mListener != null)
//                        mListener.onScreenshotTaken(null);
//
//                } else {
//                    if (mListener != null)
//                        mListener.onScreenshotTaken(Uri.fromFile(file));
//                }
//        }
//    }

}

