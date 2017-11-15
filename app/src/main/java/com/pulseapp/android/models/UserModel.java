package com.pulseapp.android.models;

import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.SelectInstitutionFragment;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Karthik on 3/19/16.
 */

public class UserModel implements FireBaseKEYIDS {
    public String KEY_UID;

    public String name;
    public String gender;
    public String handle;
    public String facebookId;
    public String verifiedPhone;
    public String momentId;
    public int favourited;
    public long createdAt;
    public long modifiedAt;
    public String imageUrl;
    public List<Object> childUser;
    public int accountType;
    public boolean verified;
    public String accountCategory;
    public HashMap<String, Rooms> rooms;
    public List<String> blockedList;
    public HashMap<String, Integer> blocked;
    public Miscellaneous miscellaneous;

    public HashMap<String, Rooms> messageRooms;
    public HashMap<String, Rooms> momentRooms;
//    public String onboarding_status;
    public int total_contributions;
    public int appVersion;

    public Object misc;

    public static class Miscellaneous {
        public InstitutionData institutionData;

        public Miscellaneous(InstitutionData institutionData) {
            this.institutionData = institutionData;
        }

        public Miscellaneous() {
        }
    }

    public UserModel() {
    }

//    /**
//     * Only default constructor required.
//     * <p/>
//     * Setters are not needed in this data structure as user rooms need not be creates
//     * on the basis of user object
//     */
//
//    public ArrayList<String> getRoomsList() {
//        ArrayList<String> roomIds = new ArrayList();
//        for (Map.Entry<String, Rooms> hash : rooms.entrySet()) {
//            roomIds.add(hash.getKey());
//        }
//        return roomIds;
//    }

    //For person
    public void createAccount(int type, boolean verified) {
        this.accountType = type;
        this.verified = verified;
        this.accountCategory = null;
    }

    //For page
    public void createAccount(int type, boolean verified, String category) {
        this.accountType = type;
        this.verified = verified;
        this.accountCategory = category;
    }

    public static class Rooms {
        public String roomId;
        public long updatedAt;
        public int type;
        public boolean interacted;
        public boolean deletionChecked;
        public HashMap<String,String> details;

        public Rooms() {
        }

        public Rooms(long updatedAt, int type) {
            this.updatedAt = updatedAt;
            this.type = type;
        }


        /**
         * new Constructor
         */
        public Rooms(int type, long updatedAt, boolean interacted) {
            this.updatedAt = updatedAt;
            this.type = type;
            this.interacted = interacted;
        }

        public Rooms(long updatedAt, boolean interacted, boolean deletionChecked) {
            this.updatedAt = updatedAt;
            this.interacted = interacted;
            this.deletionChecked = deletionChecked;
        }
    }

    public String getMyInstitutionId() {
        try {
//            return (String) ((HashMap) ((HashMap) this.misc).get(INSTUTUTION_DATA)).get("ID");
            return this.miscellaneous.institutionData.momentId;
        } catch (Exception e) {
//            e.printStackTrace();
            return null;
        }
    }

    public static class InstitutionData {

        public String momentId;//internal reference
        public int type;//school/college
        public String name;
        public HashMap<String, Integer> tags;

        public InstitutionData() {

        }

        public InstitutionData(String momentId, int type, String name, HashMap<String, Integer> tags) {
            this.momentId = momentId;
            this.type = type;
            this.name = name;
            this.tags = tags;
        }

        //        public InstitutionData(int ID, String name) {
//            this.ID = ID;
//            this.name = name;
//        }


    }
    public static class QueriedInstitution {
        public String userID;
        public String query;

        public QueriedInstitution() {
        }

        public QueriedInstitution(String userID, String query) {
            this.userID = userID;
            this.query = query;
        }
    }


    public static class AllInstitutesNode {
        public HashMap<String, InstitutionData> institutions;

        public AllInstitutesNode(HashMap<String, InstitutionData> institutions) {
            this.institutions = institutions;
        }

        public AllInstitutesNode() {
        }
    }

    public static class InstitutionChange {
        public String userId;
        public String momentId;//internal reference
        public int type;//school/college
        public String name;


        public InstitutionChange() {
        }

        public InstitutionChange(String userId, String momentId, int type, String name) {
            this.userId = userId;
            this.momentId = momentId;
            this.type = type;
            this.name = name;
        }
    }
}

