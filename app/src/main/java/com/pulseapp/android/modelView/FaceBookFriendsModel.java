package com.pulseapp.android.modelView;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by deepankur on 6/23/16.
 */
public class FaceBookFriendsModel {
    public String userId;
    public String imageUrl;
    public String handle;
    public String name;
    public boolean isChecked;
    public int type;
    public int total_count;//server reference

    public FaceBookFriendsModel() {
    }

    public FaceBookFriendsModel(String userId, String name, String imageUrl, String handle, boolean isChecked) {
        this.userId = userId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.handle = handle;
        this.isChecked = isChecked;
    }

    //for search
    public FaceBookFriendsModel(String userId, String name, String imageUrl, String handle, int type) {
        this.userId = userId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.handle = handle;
        this.type = type;
    }
}
