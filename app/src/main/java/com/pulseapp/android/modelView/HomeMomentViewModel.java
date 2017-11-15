package com.pulseapp.android.modelView;

import android.util.Log;

import com.pulseapp.android.models.MomentModel;

/**
 * Created by deepankur on 6/5/16.
 */
public class HomeMomentViewModel {// internal reference

    public enum ClickType {SINGLE_TAP, DOUBLE_TAP,AUTO_DOWNLOAD,LONG_PRESS}

    //    public boolean watchedMomentInCurrentSession;
    public ClickType clickType;//internal reference
    public String roomId;
    public String name;
    public String imageUrl;
    public String momentId;
    public long updatedAt;
    public String updatedAtText;//formatted in "hours and minutes"
    public long timeLeftToExpire;
    public boolean checkedOut;//already checked out that moment
    public int roomType;//friend or group
    public int momentStatus;//download status
    public float angle;//representing the time left in expiry angle is 0 is about to expire 360 if its a fresh moment
    private String TAG = this.getClass().getSimpleName();
    public MomentModel.Media latestMedia; //to update views
    public String handle;
    public String thumbnailUrl;
    public String lastMediaId;
    public String userId;
    public String description;
    public boolean showDisplayPicture = false;
    public int fixedtimer;
    public boolean isAnArticle;
    public String palleteColor;
    public boolean contributableNoLocation;
    public int momentType;


    /**
     * absolute coordinates to where the animation should happen
     */
    public static int viewPositionX, viewPositionY;//let it be static as only one moment will animate at a time


    public HomeMomentViewModel() {
    }

    public HomeMomentViewModel(String roomId, String name, String imageUrl, String updatedAtText, long timeLeftToExpire, String momentId, boolean checkedOut, int roomType, int downloadStatus,String thumbnailUrl) {
        this.roomId = roomId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.timeLeftToExpire = timeLeftToExpire;
        this.updatedAtText = updatedAtText;
        this.checkedOut = checkedOut;
        this.roomType = roomType;
        this.momentStatus = downloadStatus;
        this.momentId = momentId;
        this.thumbnailUrl = thumbnailUrl;
//        initRandomAngle();
        initExpireAngle();
    }

/*
    private void initRandomAngle() {
        Random r = new Random();
        int Low = 0;
        int High = 360;
        this.angle = r.nextInt(High - Low) + Low;
    }
*/

    public class Media {
        public String url;
        public long status;
    }

    private void initExpireAngle() {
        this.angle = ((this.timeLeftToExpire + 0.0f) / (24 * 60 * 60 * 1000)) * 360;
    }

    public void refreshExpiredAngle(){
        this.timeLeftToExpire  = updatedAt + 24 * 60 * 60 * 1000 - System.currentTimeMillis();
        this.initExpireAngle();
    }
}
