package com.pulseapp.android.models;

/**
 * Created by user on 9/8/2016.
 */
public class RoomDetails {
    public String roomId;
    public String memberName;
    public String memberProfilePicture;
    public String memberHandle;
    public String memberMomentId;
    public String memberUserId;
    public long updatedAt;

    public RoomDetails(String roomId,String memberName,String memberProfilePicture,String memberHandle,String memberMomentId,String memberUserId){
        this.roomId = roomId;
        this.memberName = memberName;
        this.memberProfilePicture = memberProfilePicture;
        this.memberHandle = memberHandle;
        this.memberMomentId = memberMomentId;
        this.memberUserId = memberUserId;
    }
}
