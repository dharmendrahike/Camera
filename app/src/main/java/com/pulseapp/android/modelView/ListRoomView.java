package com.pulseapp.android.modelView;

import android.util.Log;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by root on 4/5/16.
 */
public class ListRoomView {
    public String name;
    public String imageUrl;
    public String roomId;
    public String handle;
    public String momentId;
    public int type;
    public String userId;
    public boolean isChecked = false;
    public boolean hasHeader = false;
    public int normalizedFirstChar = -1;// hello and Hello to be grouped under H and not 'h' and 'H'

    public void setFirstChar(String displayName) {
      this.normalizedFirstChar= AppLibrary.getNormalisedFirstChar(displayName);
    }

    public ListRoomView() {
    }

    public ListRoomView(String name, String imageUrl, String roomId, String handle, String momentId, String userId, int type) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.roomId = roomId;
        this.handle = handle;
        this.momentId = momentId;
        this.type = type;
        this.userId = userId;
    }

    public void setItemChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }
}
