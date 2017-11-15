package com.pulseapp.android.modelView;

/**
 * Created by user on 7/9/2016.
 */
public class CustomMomentModel {

    public String momentId;
    public boolean autoModerate;
    public String thumbnailUrl;
    public String name;
    public String latitude;
    public String longitude;
    public String radius;
    public String address;
    public String city;
    public String country;
    public long totalViews;
    public boolean isThisMyInstitution;//flag for automatically checking it in share mediafragment
    public boolean isChecked;//flag for maintainig the checked/unchecked state of the holder

    public CustomMomentModel() {
    }


    public CustomMomentModel(String momentId, boolean autoModerate, String thumbnailUrl, String name) {
        this.momentId = momentId;
        this.autoModerate = autoModerate;
        this.thumbnailUrl = thumbnailUrl;
        this.name = name;
    }
}
