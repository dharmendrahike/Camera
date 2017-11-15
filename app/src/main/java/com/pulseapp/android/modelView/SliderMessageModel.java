package com.pulseapp.android.modelView;

import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.util.AppLibrary;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by deepankur on 8/5/16.
 */
public class SliderMessageModel implements FireBaseKEYIDS, Serializable {
    public String roomId;
    public int roomType;//friend; group
    public String displayName;
    public String imageUrl;//shown at top of message window
    public long updatedAt;
    public String status;
    public int messageType;
    public String friendId;
    public HashMap<String, RoomsModel.Members> members;
    private int normalizedFirstChar;//for internal reference; this will make organising a list based on alphabets faster

    public int getNormalizedFirstChar() {
        return normalizedFirstChar;
    }

    public void setFirstChar(String displayName) {
        this.normalizedFirstChar = AppLibrary.getNormalisedFirstChar(displayName);
    }

    @Override
    public String toString() {
        return (super.toString() + "\n roomId " + roomId + " roomType " + roomType + " displayName " + displayName
                + " updatedAtText " + updatedAt + " status " + status + " messageType " + messageType +
                " friendId " + friendId);
    }

    public SliderMessageModel() {
    }

    public SliderMessageModel(String displayName, String imageUrl, String roomId, String friendId, int roomType, int messageType, long updatedAt, HashMap<String, RoomsModel.Members> members) {
        this.displayName = displayName;
        this.imageUrl = imageUrl;
        this.roomId = roomId;
        this.roomType = roomType;
//        this.status = status;
        this.updatedAt = updatedAt;
        this.messageType = messageType;
        this.friendId = friendId;
        this.members = members;
        this.status = setStatus(this.messageType);
    }

    public String setStatus(final int messageType) {
        this.messageType = messageType;
        switch (messageType) {
            case SENDING_FAILED:
                return SENDING_FAILED_TEXT;
            case SENDING_MEDIA:
                return "Sending...";
            case NO_STATUS:
                return "";
            case SENT_MEDIA:
                return "Sent snap";
            case SENT_CHAT:
                return "Sent chat";
            case SEEN_MEDIA:
                return "Seen snap";
            case SEEN_CHAT:
                return "Seen chat";
            case SCREEN_SHOTTED_CHAT:
                return "Screenshotted chat";
            case SCREEN_SHOTTED_MEDIA:
                return "Screenshotted snap";
            case NEW_MEDIA:
                return "New snap";
            case NEW_TEXT:
                return "New message";
            case MEDIA_WITH_TEXT:
                return "New messages";
            case GROUP_CREATED:
                return "Group created";
            case MEMBER_JOIN:
                return "People joined";
            case YOU_JOIN:
                return "Joined";
            case ADMIN_REMOVED_MEMBER:
                return "Removed";
            case USER_LEFT_GROUP:
                return "Someone left";
            default:
                return null;
        }
    }

    public int roomOpened() {
        switch (this.messageType) {
            case NEW_MEDIA:
                this.messageType = SEEN_MEDIA;
                break;
            case NEW_TEXT:
                this.messageType = SEEN_CHAT;
                break;
            case MEDIA_WITH_TEXT:
                this.messageType = SEEN_MEDIA;
                break;
            default:
                break;
        }
        return messageType;
    }

    public void displayStatus(final ImageView imageView, TextView statusTv) {
        if (this.messageType > 4) {
            statusTv.setTypeface(Typeface.DEFAULT_BOLD);
        } else statusTv.setTypeface(Typeface.DEFAULT);
    }

    private static int getImageResources(int messageType) {
//        return R.drawable.add_friends_svg;
        return 0;
    }
}
