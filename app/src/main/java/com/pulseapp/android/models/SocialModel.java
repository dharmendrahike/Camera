package com.pulseapp.android.models;

import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Karthik on 3/19/16.
 */
public class SocialModel {


    public HashMap<String, RequestReceived> requestReceived;
    public Map<String, Integer> requestSent;
    public Map<String, Integer> requestIgnored;
    public HashMap<String, Friends> friends;
    public HashMap<String,Groups> groups;
    public long createdAt;
    public HashMap<String,FaceBookFriendsModel> facebookFriends;
    public HashMap<String,PendingGroupRequest> pendingGroupRequest;
    public HashMap<String,Integer> groupRequestIgnored;
    public HashMap<String,String> relations;

    public SocialModel() {
    }

    public static class PendingGroupRequest{
        public String name;
        public String imageUrl;
        public String senderName;
        public String roomId;
        public PendingGroupRequest(){}

        public PendingGroupRequest(String name,String imageUrl,String senderName){
            this.name = name;
            this.imageUrl = imageUrl;
            this.senderName = senderName;
        }
    }

    public static class Groups{
        public String name;
        public String imageUrl;
        public String handle;
        public String admin;
        public String roomId;

        public Groups(){}

        public Groups(String name, String imageUrl, String handle, String admin,String roomId){
            this.name = name;
            this.imageUrl = imageUrl;
            this.handle = handle;
            this.admin = admin;
            this.roomId = roomId;
        }
    }

    public static class Friends {
        public String name;
        public String nickname;
        public String handle;
        public String imageUrl;
        public String momentId;
        public long updatedAt;
        public String roomId; //FirebaseId
        public String friendId;

        public Friends() {
        }

        public Friends(String name, String handle, String imageUrl, String momentId, String room, long updatedAt) {
            this.name = name;
            this.handle = handle;
            this.imageUrl = imageUrl;
            this.momentId = momentId;
            this.roomId = room;
            this.updatedAt = updatedAt;
        }

        //accepFriendRequest
        public Friends(String name, String nickname, String handle, String imageUrl, String momentId, long updatedAt, String roomId) {
            this.name = name;
            this.nickname = nickname;
            this.handle = handle;
            this.imageUrl = imageUrl;
            this.momentId = momentId;
            this.updatedAt = updatedAt;
            this.roomId = roomId;
        }

/*        userId: {
            "name": String,
                    "nickname": String,
                    "handle": String,
                    "image": String,
                    "momentId": String,
                    "updatedAt": String,
                    "roomId": firebaseId
        }*/

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    public static class Favourites {
        String name;
        String handle;
        String image;
        String momentId;
        long updatedAt;

        public Favourites() {
        }

        public Favourites(String name, String handle, String imageUrl, String momentId, long updatedAt) {
            this.name = name;
            this.handle = handle;
            this.image = imageUrl;
            this.momentId = momentId;
            this.updatedAt = updatedAt;
        }
    }

    public static class RequestReceived {

        public RequestReceived() {
        }

        public String name;
        public String handle;
        public String imageUrl;
        public String momentId;
        public long updatedAt;
        public String UID;

        public RequestReceived(String name, String handle, String imageUrl, String momentId, long updatedAt) {
            this.name = name;
            this.handle = handle;
            this.imageUrl = imageUrl;
            this.momentId = momentId;
            this.updatedAt = updatedAt;
        }
    }
}
