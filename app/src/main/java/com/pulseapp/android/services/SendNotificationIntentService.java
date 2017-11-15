package com.pulseapp.android.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.Splash;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 6/30/2016.
 */
public class SendNotificationIntentService extends IntentService implements FireBaseKEYIDS{

    private static final String TAG = "SendNotificationIntentService";

    public SendNotificationIntentService(){
        super(SendNotificationIntentService.class.getName());
    }

    private SharedPreferences prefs;
    private String userId;

    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        Bundle bundle = intent.getExtras();
        prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        userId = prefs.getString(AppLibrary.USER_LOGIN, "");
        final String title,body, senderName,groupName, roomId,senderId, from = null;
        String bigImage = null, url = null;
        int type = 0;
        boolean sendNotification;


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        Notification notification;
        Bitmap picture = null, bigPicture = null;
        SliderMessageModel model;

        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                64,
                context.getResources().getDisplayMetrics()
        );

        title = bundle.getString("title");
        body = bundle.getString("body");
        senderName = bundle.getString("senderName");
        type = bundle.getInt("type");
        String image = bundle.getString("image"); //imageUrl
        senderId = bundle.getString("senderId");

        //Optional fields
        roomId = bundle.getString("roomId");
        groupName = bundle.getString("groupName");
        final String mediaId = bundle.getString("mediaId");
        final String mediaUrl = bundle.getString("mediaUrl");
        sendNotification = bundle.getBoolean("sendNotification");
        bigImage = bundle.getString("bigImage"); //Big image mainly for marketing notifications
        url = bundle.getString("url");

        try {   //Downloading the picture for notification first
            if (android.os.Build.VERSION.SDK_INT > 20) {
                if (AppLibrary.checkStringObject(image) != null) {
                    if (image.contains("https://"))
                        image = image.replace("https://", "http://"); //Avoid https SSL handshake error in Notifications
                    picture = Picasso.with(context).load(image).resize(px, px).transform(new RoundedTransformation()).centerCrop().get();
                } else {
                    picture = BitmapFactory.decodeResource(getResources(), R.drawable.pulse_icon);
                }
            } else {
                if (AppLibrary.checkStringObject(image) != null) {
                    if (image.contains("https://"))
                        image = image.replace("https://", "http://"); //Avoid https SSL handshake error in Notifications
                    picture = Picasso.with(context).load(image).resize(px, px).centerCrop().get();

                } else {
                    picture = BitmapFactory.decodeResource(getResources(), R.drawable.pulse_icon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            picture = BitmapFactory.decodeResource(getResources(), R.drawable.pulse_icon);
        }


        if (AppLibrary.checkStringObject(bigImage) != null) { //Downloading the big picture for notification in case
            try {
                if (bigImage.contains("https://"))
                    bigImage = bigImage.replace("https://", "http://"); //Avoid https SSL handshake error in Notifications
                bigPicture = Picasso.with(context).load(bigImage).resize(512, 256).centerCrop().get();
            } catch (Exception e) {
                e.printStackTrace();
                bigPicture = null;
            }
        }

        int notificationId = (int)getNotificationId(bundle);

        //Start showing notifications parallely while updating firebase if any
        switch (type) {
            case 1: //Friend request received
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,REQUEST_RECEIVED}).keepSynced(true);
                Intent intent1 = new Intent(this, Splash.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent1.putExtra("action","friendRequestReceived");
                PendingIntent pendingIntent1 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent1, PendingIntent.FLAG_ONE_SHOT);

                notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setTicker(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setLargeIcon(picture)
                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
                        .setContentText(body).setContentIntent(pendingIntent1).build();

                notification.defaults = Notification.DEFAULT_ALL;
//                notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.arpeggio);
                notificationManager.notify(notificationId, notification);
                break;

            case 2: //Friends request accepted
                if (AppLibrary.checkStringObject(groupName) == null)
                    model = new SliderMessageModel(senderName, image, roomId, senderId, FRIEND_ROOM, 000, 0, null);
                else
                    model = new SliderMessageModel(groupName, image, roomId, senderId,GROUP_ROOM, 000, 0, null);
                Intent intent2 = new Intent(this, Splash.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent2.putExtra("action","friendRequestAccepted");
                intent2.putExtra("model",model);

                PendingIntent pendingIntent2 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent2, PendingIntent.FLAG_ONE_SHOT);

                notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setTicker(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setLargeIcon(picture)
                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
                        .setContentText(body).setContentIntent(pendingIntent2).build();

                notification.defaults = Notification.DEFAULT_ALL;
//                notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.arpeggio);
                notificationManager.notify(notificationId, notification);
                break;

            case 3: //Group request received
                FireBaseHelper.getInstance(context).getFireBaseReference(ANCHOR_SOCIALS,new String[]{userId,PENDING_GROUP_REQUEST}).keepSynced(true);
                Intent intent3 = new Intent(this, Splash.class);
                intent3.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent3.putExtra("action","groupRequestReceived");
                PendingIntent pendingIntent3 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent3, PendingIntent.FLAG_ONE_SHOT);

                notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setTicker(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setLargeIcon(picture)
                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
                        .setContentText(body).setContentIntent(pendingIntent3).build();

                notification.defaults = Notification.DEFAULT_ALL;
//                notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.arpeggio);
                notificationManager.notify(notificationId, notification);

                break;

            case 4: //New message
                FireBaseHelper.getInstance(this).getNewFireBase("rooms", new String[]{roomId}).keepSynced(true); //Sync the room instantly using this internet connection
                updateUnreadMessageCount(roomId);
                if(mediaId != null && !mediaId.isEmpty()) { //Media message
                    //Start downloading the media
                    final DatabaseReference firebase = FireBaseHelper.getInstance(this).getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{userId,MEDIA_DOWNLOAD});
                    firebase.child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) { //Media already exists
                                AppLibrary.log_d(TAG,"MEDIA ALREADY EXISTS");
                                int status = dataSnapshot.child(LOCAL_MEDIA_STATUS).getValue(Integer.class);
                                if (status != MEDIA_DOWNLOAD_COMPLETE) { //Update downloading status and start download
                                    AppLibrary.log_d(TAG,"MEDIA EXISTS BUT NOT DOWNLOADED");
                                    dataSnapshot.child(LOCAL_MEDIA_STATUS).getRef().setValue(MEDIA_DOWNLOADING);
                                    dataSnapshot.child(ROOM_ID).getRef().setValue(roomId);
                                    downloadFile(mediaUrl, mediaId, firebase);
                                } else {
                                    //already downloaded, relax!
                                }
                            } else {
                                AppLibrary.log_d(TAG,"MEDIA DOES NOT EXIST IN LOCAL DATA");
                                //Create new data entry and start download
                                Map<String, Object> hashMap = new HashMap<String, Object>();
                                Map<String,Integer> roomMap = new HashMap<String, Integer>();
                                roomMap.put(roomId,1);
                                hashMap.put(ROOMS, roomMap);
                                hashMap.put(LOCAL_MEDIA_STATUS,MEDIA_DOWNLOADING);
//                                hashMap.put(URL, mediaUrl);
                                dataSnapshot.getRef().setValue(hashMap);

                                downloadFile(mediaUrl, mediaId, firebase);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });

                    //Show notification
                    Intent intent4 = new Intent(this, CameraActivity.class);
                    intent4.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    if (AppLibrary.checkStringObject(groupName) == null)
                        model = new SliderMessageModel(senderName, image, roomId, senderId, FRIEND_ROOM, 000, 0, null);
                    else
                        model = new SliderMessageModel(groupName, image, roomId, senderId,GROUP_ROOM, 000, 0, null);

                    intent4.putExtra("action","mediaMessage");
                    intent4.putExtra("model",model);
                    PendingIntent pendingIntent4 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent4, PendingIntent.FLAG_ONE_SHOT);

                    notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                            .setContentTitle(title)
                            .setAutoCancel(true)
                            .setTicker(title)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setLargeIcon(picture)
                            .setColor(context.getResources().getColor(R.color.notification_icon_background))
                            .setContentText(body).setContentIntent(pendingIntent4).build();

                    notification.defaults = Notification.DEFAULT_ALL;
//                    notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.arpeggio);
                    notificationManager.notify(notificationId, notification);

                } else { //Normal chat message

                    Intent intent4 = new Intent(this, CameraActivity.class);
                    intent4.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    if (AppLibrary.checkStringObject(groupName) == null)
                        model = new SliderMessageModel(senderName, image, roomId, senderId, FRIEND_ROOM, 000, 0, null);
                    else
                        model = new SliderMessageModel(groupName, image, roomId, senderId,GROUP_ROOM, 000, 0, null);

                    intent4.putExtra("action","chatMessage");
                    intent4.putExtra("model",model);
                    PendingIntent pendingIntent4 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent4, PendingIntent.FLAG_ONE_SHOT);

                    notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                            .setContentTitle(title)
                            .setAutoCancel(true)
                            .setTicker(title)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setLargeIcon(picture)
                            .setColor(getApplicationContext().getResources().getColor(R.color.notification_icon_background))
                            .setContentText(body).setContentIntent(pendingIntent4).build();

                    notification.defaults = Notification.DEFAULT_ALL;
//                    notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.arpeggio);
                    notificationManager.notify(notificationId, notification);
                }
                break;

            case 6: //Notifications sent from the admin dashboard
                Intent intent6 = new Intent(this, Splash.class);
                intent6.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (url!=null && !url.isEmpty()) {
                    if (url.contains("/add/") || url.contains("/profile/")) {
                        intent6.putExtra("action", "openProfilePopup");
                        intent6.putExtra("url",url);
                    } else if (url.contains("/stream/")) {
                        intent6.putExtra("action", "openStream");
                        intent6.putExtra("url",url);
                    } else if (url.equalsIgnoreCase("openCamera")) {
                        // Do nothing - not putting an extra action will open camera like default
                    }
                } else {
                    intent6.putExtra("action","genericNotification"); //Will open dashboard and do nothing
                }

                PendingIntent pendingIntent6 = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent6, PendingIntent.FLAG_ONE_SHOT);

                if (bigPicture==null) {
                    notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                            .setContentTitle(title)
                            .setAutoCancel(true)
                            .setTicker(title)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setLargeIcon(picture)
                            .setColor(context.getResources().getColor(R.color.notification_icon_background))
                            .setContentText(body).setContentIntent(pendingIntent6).build();
                } else {
                    notification = mBuilder.setSmallIcon(R.drawable.logo_png)
                            .setContentTitle(title)
                            .setAutoCancel(true)
                            .setTicker(title)
                            .setContentText(body)
                            .setLargeIcon(picture)
                            .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setBigContentTitle(title).setSummaryText(body))
                            .setColor(this.getResources().getColor(R.color.notification_icon_background))
                            .setContentText(body).setContentIntent(pendingIntent6).build();
                }

                notification.defaults = Notification.DEFAULT_ALL;
                notificationManager.notify((int)System.currentTimeMillis(), notification);
                break;
        }

    }

    private void updateUnreadMessageCount(String roomId){
        FireBaseHelper.getInstance(this).getNewFireBase(ANCHOR_LOCAL_DATA,new String[]{userId,UNREAD_ROOMS,roomId}).setValue(1);
    }

    private void downloadFile(final String mediaUrl, final String mediaId, final DatabaseReference firebase) {
        AppLibrary.log_d(TAG,"Inside download file, with media Id -"+mediaId);
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock mWakeLock; mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire(); //Acquire wakelock to prevent CPU from dozing off

        ThinDownloadManager downloadManager = new ThinDownloadManager();

        String filename = null;
        if(mediaUrl.endsWith(".jpg"))
            filename = mediaId + ".jpg";
        else if (mediaUrl.endsWith(".webp"))
            filename = mediaId + ".webp";
        else if (mediaUrl.endsWith(".mp4"))
            filename = mediaId + ".mp4";

        if(filename == null) return;

        String mediaStoragePath = AppLibrary.getFilesDirectory(getApplicationContext());
        File mediaDir = new File(mediaStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();

        mediaStoragePath = mediaStoragePath + filename;

        final String finalMediaStoragePath = mediaStoragePath;
        DownloadStatusListenerV1 downloadListener = new DownloadStatusListenerV1() {
            @Override
            public void onDownloadComplete(DownloadRequest downloadRequest) {
                AppLibrary.log_d(TAG,"Download complete for media chat notification");
                firebase.child(mediaId).keepSynced(true);
                firebase.child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            AppLibrary.log_d(TAG,"Updating media status to download complete");
                            dataSnapshot.child(LOCAL_MEDIA_STATUS).getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                            dataSnapshot.child(URL).getRef().setValue(finalMediaStoragePath);
                            MasterClass.getEventBus().post(new BroadCastSignals.DownloadChatMediaStatus(mediaId,finalMediaStoragePath));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
                mWakeLock.release();
            }

            @Override
            public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                AppLibrary.log_d(TAG,"Download failed for media chat notification");
                firebase.child(mediaId).keepSynced(true);
                firebase.child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            AppLibrary.log_d(TAG,"Updating failed status to local data");
                            dataSnapshot.child(LOCAL_MEDIA_STATUS).getRef().setValue(MEDIA_DOWNLOAD_NOT_STARTED);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
                mWakeLock.release();
            }

            @Override
            public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
                AppLibrary.log_d(TAG,"Ongoing download for chat media");
            }

        };

        DownloadRequest req = new DownloadRequest(Uri.parse(mediaUrl))
                .setStatusListener(downloadListener)
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(Uri.parse(mediaStoragePath))
                .setPriority(DownloadRequest.Priority.HIGH);

        long mDownloadedFileID = downloadManager.add(req);
    }

    public int getNotificationId(Bundle bundle){
        String uniqueString = null;
        if (bundle.containsKey("roomId") && AppLibrary.checkStringObject(bundle.getString("roomId")) != null){
            uniqueString = bundle.getString("roomId");
        }
        uniqueString += bundle.getString("senderId");
        uniqueString += bundle.get("type");
        return uniqueString.hashCode();
    }
}
