package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by root on 27/5/16.
 */
public class NotificationContent {

    public String senderName;
    public String senderId;
    public String to;
    public String messageId;
    public String roomId;
    public String groupName;
    public int type;
    public String image;
    public String mediaId;
    public String mediaUrl;

    public NotificationContent(String senderId,String senderName,String to,String messageId,String roomId,String groupName,String image,int type,String mediaId,String mediaUrl) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.to = to;
        this.messageId = messageId;
        this.roomId = roomId;
        this.groupName = groupName;
        this.image = image;
        this.type = type;
        this.mediaId = mediaId;
        this.mediaUrl = mediaUrl;
    }
}
