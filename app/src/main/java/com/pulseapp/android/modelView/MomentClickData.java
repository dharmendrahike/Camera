package com.pulseapp.android.modelView;

/**
 * Created by indianrenters on 5/17/16.
 */
public class MomentClickData {

    public final int SINGLE_TAP = 111, DOUBLE_TAP = 222;
    int clickType;
    String momentId;
    int downloadStatus;

    public MomentClickData(int clickType, String momentId, int downloadStatus) {
        this.clickType = clickType;
        this.momentId = momentId;
        this.downloadStatus = downloadStatus;
    }


}
