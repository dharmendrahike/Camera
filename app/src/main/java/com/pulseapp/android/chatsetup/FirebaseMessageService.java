package com.pulseapp.android.chatsetup;

/**
 * Created by Karthik on 5/19/16.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.services.SendNotificationIntentService;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.Foreground;

//import com.firebase.client.DataSnapshot;
//import com.firebase.client.Firebase;
//import com.firebase.client.FirebaseError;
//import com.firebase.client.ValueEventListener;

public class FirebaseMessageService extends FirebaseMessagingService implements FireBaseKEYIDS {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String NOTIFICATION_SERVICE = "SendNotificationService";
    private Context context;
    private SharedPreferences prefs;
    private String userId;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        AppLibrary.log_d(TAG,"onMessageReceived called -"+remoteMessage.getData().toString());
        context = getApplicationContext();
        prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        userId = prefs.getString(AppLibrary.USER_LOGIN, "");

        String roomId,messageId = null;
        boolean sendNotification = false;
        final Bundle bundle = new Bundle();

        if (AppLibrary.checkStringObject(remoteMessage.getData().toString()) == null) return;
        if (userId.isEmpty()) return;

        ArrayMap<String,String> jsonData = (ArrayMap<String, String>) remoteMessage.getData();

        if (!CameraActivity.isAppForeground) {
            sendNotification = true; //default config is null when app is killed
            AppLibrary.log_d(TAG,"App is killed");
        } else {
            sendNotification = true; //Ignoring check for app killed or not
            AppLibrary.log_d(TAG,"App is not killed");
        }

        try {
            //Compulsory fields
            bundle.putString("title", jsonData.get("title"));
            bundle.putString("body",jsonData.get("body"));
            bundle.putString("senderName",jsonData.get("senderName"));
            bundle.putInt("type" ,Integer.parseInt(jsonData.get("type")));
            bundle.putString("image",jsonData.get("image"));
            bundle.putString("senderId",jsonData.get("senderId"));

            //Optional fields
            bundle.putString("roomId",jsonData.get("roomId"));
            bundle.putString("groupName",jsonData.get("groupName"));
            bundle.putString("mediaId",jsonData.get("mediaId"));
            bundle.putString("mediaUrl",jsonData.get("mediaUrl"));
            bundle.putString("messageId",jsonData.get("messageId"));
            bundle.putString("bigImage",jsonData.get("bigImage"));
            bundle.putString("url",jsonData.get("url"));
            bundle.putBoolean("sendNotification",sendNotification);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        roomId = bundle.getString("roomId");
        messageId = bundle.getString("messageId");

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.NOTIFICATION_RECEIVED,AnalyticsEvents.NOTIFICATION_TYPE,Integer.toString(bundle.getInt("type")),AnalyticsEvents.NOTIFICATION_TITLE,bundle.getString("title"));

        if (sendNotification && AppLibrary.checkStringObject(roomId) != null && AppLibrary.checkStringObject(messageId) != null) { //Its a new chat/media message, check first if already seen
            DatabaseReference firebase = FireBaseHelper.getInstance(this).getNewFireBase("rooms", new String[]{roomId, "messages", messageId, "viewers"});
            firebase.keepSynced(true);
            firebase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        if (dataSnapshot.hasChild(userId)) {
                            AppLibrary.log_d(TAG, "Already Seen ignoring notification");
                            return; //Don't do anything if message already seen
                        }
                    }
                    sendNotification(bundle);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    sendNotification(bundle);
                }

            });
        } else if (sendNotification && (bundle.getInt("type") == 1 || bundle.getInt("type") == 3)){
            if (AppLibrary.checkStringObject(bundle.getString("senderId")) != null && bundle.getInt("type") == 1){
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,REQUEST_RECEIVED}).keepSynced(true);
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,FRIENDS}).keepSynced(true);
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,FRIENDS}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(bundle.getString("senderId"))){
                            // already accepted the friend request , dont send notification
                        } else {
                            FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,REQUEST_IGNORED}).keepSynced(true);
                            FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,REQUEST_IGNORED}).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(bundle.getString("senderId"))) {
                                        // already ignored the friend request, dont send notification
                                    } else {
                                        sendNotification(bundle);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    sendNotification(bundle);
                                }

                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        sendNotification(bundle);
                    }

                });
            } else if (AppLibrary.checkStringObject(bundle.getString("roomId")) != null && bundle.getInt("type") == 3){
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,PENDING_GROUP_REQUEST}).keepSynced(true);
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_ROOMS,new String[]{bundle.getString("roomId"),MEMBERS}).keepSynced(true);
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_ROOMS,new String[]{bundle.getString("roomId"),MEMBERS}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(userId)){
                            // already accepted group request, dont send notification
                        } else {
                            FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,GROUP_REQUEST_IGNORED}).keepSynced(true);
                            FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,GROUP_REQUEST_IGNORED}).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(bundle.getString("roomId"))){
                                        // already ignored the group request
                                    } else {
                                        sendNotification(bundle);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    sendNotification(bundle);
                                }

                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        sendNotification(bundle);
                    }

                });
            }
        } else if (sendNotification){
            sendNotification(bundle);
        }
    }

    private void sendNotification(Bundle bundle) {
        Intent intent = new Intent(getApplicationContext(),SendNotificationIntentService.class);
        intent.putExtras(bundle);
        startService(intent);
    }

}