package com.pulseapp.android.modelView;

import com.pulseapp.android.firebase.FireBaseKEYIDS;

/**
 * Created by deepankur on 7/11/16.
 */
public class SearchResultModel implements FireBaseKEYIDS {
    public String searchId;//user id in case of friend search, roomId in group search momentid other with
    public String imageUrl;
    public String handle;
    public String name;
    public String roomId;
    public final int roomType;//friendRoom or group Room

    public String momentUpdatedAt;//in case of  moments

    //friends
    public SearchResultModel(String searchId, String imageUrl, String handle, String name/*, int type)*/, Object additionalInfo) {
        this.searchId = searchId;
        this.imageUrl = imageUrl;
        this.handle = handle;
        this.name = name;
        if (additionalInfo instanceof Integer)
            this.roomType = (int) additionalInfo;
        else if (additionalInfo instanceof String) {
            String s = (String) additionalInfo;
            if (s.equals(REQUEST_RECEIVED))
                roomType = FRIEND_ROOM;
            else if (s.equals(PENDING_GROUP_REQUEST))
                roomType = GROUP_ROOM;
            else roomType = -1;
        } else roomType = -1;//not defining

    }

    public boolean isRequestSent;

    public void setRequestSentTrue() {
        this.isRequestSent = true;
    }
}
