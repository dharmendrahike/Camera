package com.pulseapp.android.models;

import android.support.annotation.NonNull;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Karthik on 3/20/16.
 */
public class MomentModel {

    public int source;
    public String name;
    public String description;
    public String userId;
    public String locName;
    public HashMap<String, Media> medias;
    public Timestamp timestamp;
    public Flags flags;
    public int momentStatus;
    public long totalViews;

    public float locLat;
    public float locLong;
    public float locRadius;
    public String momentId;
    public String updatedDate;
    public String roomId;
    public boolean autoModerate;
    public Object contributedMedias;
    public String handle;
    public String thumbnailUrl; //Used for last media item
    public String imageUrl; // Used for thumbnail
    public String lastMediaId;
    public boolean contributableNoLocation;
    public int fixedTimer;

    public MomentModel() {
    }

    public MomentModel(int source) {
        this.source = source;
    }

    public MomentModel(int source, String name, String description, String userId, String locName,
                       LinkedHashMap<String, Media> media, Timestamp timestamp, Flags flags, float locLat, float locLong, float locRadius) {
        this.source = source;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.locName = locName;
        this.medias = media;
        this.timestamp = timestamp;
        this.flags = flags;
        this.locLat = locLat;
        this.locLong = locLong;
        this.locRadius = locRadius;
    }

    public static class Media {
        public String url;
        public long createdAt;
        public int expiryType;
        public int type;
        public int totalViews;
        public HashMap<String, Integer> viewers;
        public HashMap<String, ViewerDetails> viewerDetails;
        public int screenShots;
        public String createdAtText;
        public Privacy privacy;
        public String momentId;
        public String mediaId;
        public int status;
        public String mediaText;
        public String gender;
        public String thumbnail;
        public long duration;
        public long uploadedAt;
        public String userId;
        public HashMap<String, Object> userDetail;
        public boolean anonymous;
        public int webViews;
        public VideoDownScale downscale;
        public Cta cta;

        public Media() {
        }

        public Media(String url, long createdAt, int type, int mediaViews, HashMap<String, ViewerDetails> viewerDetails, int screenShots, String createdAtText, String mediaText) {
            this.url = url;
            this.createdAt = createdAt;
            this.type = type;
            this.totalViews = mediaViews;
            this.viewerDetails = viewerDetails;
            this.screenShots = screenShots;
            this.createdAtText = createdAtText;
            this.mediaText = mediaText;
        }
    }

    public static class Privacy {
        public int type;
        public HashMap<String, String> value;

        public Privacy() {
        }

        public Privacy(int type, HashMap<String, String> value) {
            this.type = type;
            this.value = value;
        }
    }

    public static class VideoDownScale {
        public String p_360;
        public String p_480;
        public String p_720;

        public VideoDownScale() {
        }
    }

    public static class ContributedMedia {

        public int state;
        public String user;
        public String originalMedia;
        public String gender;
        public long createdAt;

        public ContributedMedia() {
        }

        public ContributedMedia(int state, String user, String originalMedia) {
            this.state = state;
            this.originalMedia = originalMedia;
            this.user = user;
        }

        public ContributedMedia(int state, String user) {
            this.state = state;
            this.user = user;
        }
    }

    public static class Timestamp {
        public long startTime;
        public long endTime;
        public long createdAt;
        public long modifiedAt;

        public Timestamp() {
        }

        /**
         * @param startTime  only field needed while creating group
         * @param endTime
         * @param createdAt
         * @param modifiedAt
         */
        public Timestamp(long startTime, long endTime, long createdAt, long modifiedAt) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.createdAt = createdAt;
            this.modifiedAt = modifiedAt;
        }

       /* //For user and groups, no start/end time
        public Timestamp( String createdAt, String modifiedAt) {

            this.createdAt = createdAt;
            this.modifiedAt = modifiedAt;
            this.endTime = null;
            this.startTime = null;
        }*/

        public void setModifiedAt(long modifiedAt) {
            this.modifiedAt = modifiedAt;
        }
    }

    public static class Flags {

        public int type; //0 - Inactive, 1 - Published, 2 - History
        public boolean expired;
        public boolean deleted;
        public boolean allowContribution;
        public boolean isFeatured;

        public Flags() {
        }

        public Flags(int type, boolean expired) {
            this.type = type;
            this.expired = expired;
        }

        public void setExpired() {
            this.expired = true;
        }
    }

    public static class Cta {//call to action
        public String text;
        public MomentModel.Intent androidIntent;

        public Cta() {
        }
    }

    public static class Downscale {
        String P_360;
        String P_480;
        String P_720;

        public Downscale() {

        }
    }

    public static class Intent {
        @NonNull//will be either an android Intent String  or the web ie (http url)
        public String intentAction;
        public String intentType;
        public String intentPackage;
        public String intentData;
        public String intentExtra;

        public Intent() {
        }
    }

}
