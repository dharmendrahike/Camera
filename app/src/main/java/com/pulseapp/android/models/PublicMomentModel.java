package com.pulseapp.android.models;

/**
 * Created by indianrenters on 9/16/16.
 */
public class PublicMomentModel {

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

    public PublicMomentModel(){}


    public PublicMomentModel(String momentId,boolean autoModerate,String thumbnailUrl,String name){
        this.momentId = momentId;
        this.autoModerate = autoModerate;
        this.thumbnailUrl = thumbnailUrl;
        this.name = name;
    }
}
