package com.pulseapp.android.modelView;

import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.ViewerDetails;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by user on 6/8/2016.
 */
public class MediaModelView implements Serializable {

    public String url;
    public long createdAt;
    public int type;
    public int totalViews;
    public HashMap<String,ViewerDetails> viewerDetails;
    public int screenShots;
    public String createdAtText;
    public String momentId;
    public String mediaId;
    public int status;
    public String mediaText;
    public int mediaState;
    public long duration;
    public int webViews;

    public MediaModelView(){}

    public MediaModelView(String url,long createdAt,int type,int totalViews,HashMap<String,ViewerDetails> viewerDetails,
                          int screenShots,String createdAtText,String momentId,String mediaId,int status,String mediaText,int webViews){
        this.url = url;
        this.createdAt = createdAt;
        this.type = type;
        this.totalViews = totalViews;
        this.viewerDetails = viewerDetails;
        this.screenShots = screenShots;
        this.createdAtText = createdAtText;
        this.momentId = momentId;
        this.mediaId = mediaId;
        this.status = status;
        this.mediaText = mediaText;
        this.webViews = webViews;
    }
}
