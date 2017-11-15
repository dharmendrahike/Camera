package com.pulseapp.android.signals;

//import com.instalively.android.broadcast.BroadCastPage;
import android.os.Bundle;

import com.pulseapp.android.broadcast.FilterManager;

/**
 * Created by admin on 1/29/2015.
 */
public class BroadCastSignals {

    public interface BaseSignal {}
    
    public static class CameraPreviewStartedSignal implements BaseSignal {}

    public static class FilterChangeSignal implements BaseSignal {
        private FilterManager mFilter;

        public FilterChangeSignal (FilterManager filter) {
            this.mFilter = filter;
        }

        public FilterManager getFilter() {
            return mFilter;
        }
    }

    public static class ErrorSignal implements BaseSignal {

        public ErrorSignal() {}

    }

    public static class FocusSignal implements BaseSignal {
        public int x, y;

        public FocusSignal(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }

    public static class DoubleShotSignal implements BaseSignal {
        private boolean enable;

        public DoubleShotSignal(boolean enable) {
            this.enable = enable;
        }

        public boolean getEnable() {
            return enable;
        }
    }

    public static class RestartBroadCastSignal  implements BaseSignal {

        private String quality;
        private boolean changeCamera = false;
        private String streamKey;
        private String streamFormat;

        public RestartBroadCastSignal(String key, boolean changeCamera) {
            this.quality = key;
            this.changeCamera = changeCamera;
        }
        
        public void setOtherParams(String key, String format) {
            this.streamFormat = format;
            this.streamKey = key;
        }

        public String getStreamFormat() {
            return streamFormat;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public boolean isChangeCamera(){
            return changeCamera;
        }

        public String getQuality(){
            return quality;
        }
    }

    public static class SessionAccessSignal implements BaseSignal {

        private int sessionId;

        public SessionAccessSignal(int sessionId){
            this.sessionId = sessionId;
        }

        public int getSessionId() {
            return sessionId;
        }
    }
    
    public static class SpeedTestResults implements BaseSignal {
        
        private String format;
        
        public SpeedTestResults(String format){
            this.format = format;
        }
        
        public String getFormat(){
            return this.format;
        }
    }
    
//    public static class NewBroadCastStatus implements BaseSignal {
//
//        BroadCastPage.BroadCastStatus mStatus;
//
//        public NewBroadCastStatus (BroadCastPage.BroadCastStatus mStatus) {
//            this.mStatus = mStatus;
//        }
//
//        public BroadCastPage.BroadCastStatus getStatus() {
//            return mStatus;
//        }
//    }

//    public static class NewBroadCastStatus1 implements BaseSignal {
//
//        CameraActivity.BroadCastStatus mStatus;
//
//        public NewBroadCastStatus1 (CameraActivity.BroadCastStatus mStatus) {
//            this.mStatus = mStatus;
//        }
//
//        public CameraActivity.BroadCastStatus getStatus() {
//            return mStatus;
//        }
//    }

    public static class FetchFriendsStatus implements BaseSignal{
        boolean fetchFriendsComplete;

        public FetchFriendsStatus(boolean isComplete){
            this.fetchFriendsComplete = isComplete;
        }
    }

    public static class DownloadChatMediaStatus implements BaseSignal{
        public String mediaId;
        public String localUrl;

        public DownloadChatMediaStatus(String mediaId,String localUrl){
            this.mediaId = mediaId;
            this.localUrl = localUrl;
        }
    }
}
