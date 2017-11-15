package com.pulseapp.android.models;

import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Karthik on 3/20/16.
 */
public class MediaModel {

    public String name;
    public String description;
    public String userId;
    public String url;
    public int type; //1-image, 2-video
    public int views;
    public List<String> viewerList;
    public boolean deletedOnS3;
    public boolean expired;
    public boolean screenshotEnabled;
    public boolean audioEnabled;
    public long createdAt;
    public long modifiedAt;
    public AddedTo addedTo;
    public int expiryType;
    public String mediaText;
    public Privacy privacy;
    public int totalViews;
    public HashMap<String,Integer> viewers;
    public HashMap<String,ViewerDetails> viewerDetails;
    public String mediaId;
    public String handle;
    public int screenShots;
    public String thumbnail;
    public long duration;
    public HashMap<String,PublicMomentModel> momentDetails;
    public long uploadedAt;
    public HashMap<String,Object> userDetail;
    public boolean anonymous;
    public String source;
    public boolean facebookPost;

    public MediaModel() {
    }

    public MediaModel(String mediaText,String userId, String url, int type, boolean screenshotEnabled, boolean audioEnabled, long createdAt) {
        this.mediaText = mediaText;
        this.userId = userId;
        this.url = url;
        this.type = type;
        this.screenshotEnabled = screenshotEnabled;
        this.audioEnabled = audioEnabled;
        this.createdAt = createdAt;
    }

    public static class AddedTo {

        public HashMap<String,Integer> moments;
        public HashMap<String,Integer> rooms;

        public AddedTo(){
        }

        public AddedTo(HashMap<String,Integer> momentsMap,HashMap<String,Integer> roomMap) {
            this.moments = momentsMap;
            this.rooms = roomMap;
        }

        public void addToCustomMoment(String momentId) {
            customMoment newCustomMoment = new customMoment(momentId);
//            customMoments.add(newCustomMoment);
        }

        public void addToMessageRooms(String roomId) {
            rooms messageRoom = new rooms(roomId);
//            messageRooms.add(messageRoom);
        }

        static class customMoment {
            String momentId;

            public customMoment(String momentId) {
                this.momentId = momentId;
            }
        }

        static class rooms {
            String roomId;

            public rooms(String roomId) {
                this.roomId = roomId;
            }
        }
    }

    public static class Privacy{

        public int type;
        public HashMap<String,String> value;
        public Privacy(){}

        public Privacy(int type,HashMap<String,String> value){
            this.type = type;
            this.value = value;
        }
    }

    public void setExpired() {
        this.expired = true;
    }

//    public void setModifiedAt(String modifiedAt) {
//        this.modifiedAt = modifiedAt;
//    }

    public void setUrl(String mediaUrl){
        url = mediaUrl;
    }

}
