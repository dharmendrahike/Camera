package com.pulseapp.android.models;

import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;

/**
 * Created by user on 5/4/2016.
 */
public class SettingsModel {

    public HashMap<String, CustomFriendListDetails> customFriendList;
    public HashMap<String, MemberDetails> ignoredList;
    public HashMap<String, BlockedUserModel> blockedList;
    public String birthday;
    public boolean birthdayPartyFlag;
    public boolean pushNotification;
    public boolean travelMode;
    public boolean autoSaveMode;
    public boolean facebookBirthday;
    public String lastUsedPrivacy;

    public SettingsModel() {
    }

    public SettingsModel(HashMap<String, CustomFriendListDetails> customFriendList, HashMap<String, MemberDetails> ignoredList) {
        this.customFriendList = customFriendList;
        this.ignoredList = ignoredList;
    }

    public static class CustomFriendListDetails {
        public String name;
        public HashMap<String, MemberDetails> members;

        public CustomFriendListDetails() {
        }

        public CustomFriendListDetails(String name, HashMap<String, MemberDetails> members) {
            this.name = name;
            this.members = members;
        }
    }

    public static class MemberDetails {
        public String roomId;
        public String name;

        public MemberDetails() {
        }

        public MemberDetails(String roomId, String name) {
            this.roomId = roomId;
            this.name = name;
        }
    }

    public static class BlockedUserModel {
        public String name, handle, imageUrl;

        public BlockedUserModel(){}

        public BlockedUserModel(String name, String handle, String imageUrl) {
            this.name = name;
            this.handle = handle;
            this.imageUrl = imageUrl;
        }
    }

    public static class UpdateName {
        public int type;
        public String name, userId;

        public UpdateName() {
        }

        public UpdateName(String name, String userId) {
            this.type = FireBaseKEYIDS.UPDATE_NAME;
            this.name = name;
            this.userId = userId;
        }
    }

    public static class UpdateImageUrl {
        public int type;
        public String userId, imageUrl;

        public UpdateImageUrl() {
        }

        public UpdateImageUrl(String imageUrl, String userId) {
            this.type = FireBaseKEYIDS.UPDATE_IMAGE;
            this.userId = userId;
            this.imageUrl = imageUrl;
        }
    }


}
