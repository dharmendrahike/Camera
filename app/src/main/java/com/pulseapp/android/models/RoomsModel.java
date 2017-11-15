package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by Karthik on 3/20/16.
 */
public class RoomsModel {
    public int type; // 1-friend, 2-group
    public HashMap<String, Members> members;
    public String roomId;
    public long updatedAt;//field for internal sorting
    public HashMap<String, Messages> messages;
    public Detail detail;
    public HashMap<String,PendingMemberDetails> pendingMembers;

    public RoomsModel() {
    }

    public RoomsModel(int type, HashMap<String, Members> members, HashMap<String, Messages> message, HashMap<String,PendingMemberDetails> pendingMembers, Detail detail) {
        this.type = type;
        this.members = members;
        this.messages = message;
        this.detail = detail;
        this.pendingMembers = pendingMembers;
    }

    public void startDownload(String userId){

    }

    public void openRoom(String userId){
        // if type == 0 or 2 then Fetch all downloaded media and show

        // else then fetch all messages
    }

    public void configureDetails(String userId,String roomId){
        if(this.detail != null){
            this.detail.roomId = roomId;
            return;
        }
        if (members != null) {
            for (String member : members.keySet()) {
                if (!member.equals(userId)) {
                    Members currentMember = members.get(member);
                    this.detail = new Detail(null, currentMember.name, currentMember.imageUrl, roomId, currentMember.handle, currentMember.momentId, member, this.type, 0);
                }
            }
        }
    }

    public void setUpdatedAt(long updatedAt){
        this.updatedAt = updatedAt;
    }

    public static class Detail {
        public String adminId;
        public String name;
        public String imageUrl;
        public String handle;
        public String momentId;
        public String roomId;
        public long createdAt;
        public int roomType;
        public String userId;

        public Detail() {
        }

        public Detail(String adminId, String name, String imageUrl,String roomId, String handle,String momentId,String userId,int roomType,long createdAt) {
            this.adminId = adminId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.roomId = roomId;
            this.handle = handle;
            this.momentId = momentId;
            this.createdAt = createdAt;
            this.roomType = roomType;
            this.userId = userId;
        }

        public Detail(String adminId, String name, String imageUrl,String handle,long createdAt) {
            this.adminId = adminId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.handle = handle;
            this.createdAt = createdAt;
        }
    }

    public static class Members {
        public String name;
        public String imageUrl;
        public String handle;
        public String roomId;
        public String momentId; //In case of friend
        public int msgCount; //Unread chat messages
        public int momentCount; //Unseen moments
        public int type;
        public String memberId;

        public Members() {
        }

        public Members(String name, String imageUrl, String handle, String roomId, String momentId, int msgCount, int momentCount, int type) {
            this.name = name;
            this.imageUrl = imageUrl;
            this.handle = handle;
            this.roomId = roomId;
            this.momentId = momentId;
            this.msgCount = msgCount;
            this.momentCount = momentCount;
            this.type = type;
        }

        public Members(String name,String imageUrl,int type){
            this.name = name;
            this.imageUrl = imageUrl;
            this.type = type;
        }

        public Members(String memberId, String name,String imageUrl,int type){
            this.memberId = memberId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.type = type;
        }

    }

    public static class PendingMemberDetails{
        public String name;
        public String imageUrl;
        public String handle;
        public String momentId;
        public String pendingMemberId;

        public PendingMemberDetails(){}

        public PendingMemberDetails(String name, String imageUrl, String handle, String momentId, String pendingMemberId){
            this.name = name;
            this.imageUrl = imageUrl;
            this.handle = handle;
            this.momentId = momentId;
            this.pendingMemberId = pendingMemberId;
        }
    }

    public static class Messages {

        public String messageId;//internal reference
        public String memberId;
        public String mediaId;
        public int type;
        public String text;
        public long createdAt;
        public long expiryTime;
        public HashMap<String, Integer> status;
        public int expiryType;
        public HashMap<String,Integer> viewers;
        public int mediaUploadingStatus;
        public boolean isVideo;
        public boolean delivered;
        public String uri;
        public Integer mediaStatus;
        public Integer position;

        //        public Boolean thisMsgMarkedToDelete; //internal reference for views
        public Messages() {
        }

        public Messages(String memberId, String mediaId, int type, String text, long createdAt,HashMap<String,Integer> status,int expiryType, HashMap<String,Integer> viewers) {
            this.memberId = memberId;
            this.mediaId = mediaId;
            this.type = type;
            this.text = text;
            this.createdAt = createdAt;
            this.status = status;
            this.expiryType = expiryType;
            this.viewers = viewers;
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append( this.getClass().getName() );
        result.append(super.toString());
        result.append( " Object {" );
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = this.getClass().getDeclaredFields();

        //print field names paired with their values
        for ( Field field : fields  ) {
            result.append("  ");
            try {
                result.append( field.getName() );
                result.append(": ");
                //requires access to private field:
                result.append( field.get(this) );
            } catch ( IllegalAccessException ex ) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");
        return result.toString();
    }


}
