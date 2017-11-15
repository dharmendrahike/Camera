package com.pulseapp.android.modelView;

/**
 * Created by deepankur on 30/4/16.
 */
public class AllChatsModel {
    public String imageUrl;
    public String name;
    public String handle;
    public String roomId;
    public String createdAt;

    public AllChatsModel() {
    }

    public AllChatsModel(String name, String imageUrl, Object otherStuff, int index) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.otherStuff = otherStuff;
        this.index = index;
    }

    Object otherStuff;
    int index;
}




