package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by user on 8/25/2016.
 */
public class ViewerDetails {

    public String name;
    public boolean screenShotted;
    public long viewedAt;

    public ViewerDetails(){}

    public ViewerDetails(String name,boolean screenShotted,long viewedAt){
        this.name = name;
        this.screenShotted = screenShotted;
        this.viewedAt = viewedAt;
    }
}
