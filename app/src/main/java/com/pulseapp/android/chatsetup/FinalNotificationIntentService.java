package com.pulseapp.android.chatsetup;

/**
 * Created by Ashish on 16-04-2015.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.TypedValue;

import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class FinalNotificationIntentService extends IntentService {

    private NotificationManager mNotificationManager;
    private SharedPreferences prefs;
    Bundle extras;

    private JSONObject getPushData(Intent intent) {
        try {
            return new JSONObject(intent.getStringExtra("com.parse.Data"));
        } catch (JSONException e) {
            AppLibrary.log_d(TAG, "Unexpected JSONException when receiving push data:" + e);
            return null;
        }
    }

    public FinalNotificationIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "FinalNotificationIntentService";

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY;
//    }

    @Override
    protected void onHandleIntent(Intent intent) {

        prefs = getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        JSONObject pushData = getPushData(intent);
        extras = intent.getExtras();

        if (!extras.isEmpty()) {

                if (pushData != null && prefs.getString(AppLibrary.USER_LOGIN, null) != null) {

                        if (AppLibrary.checkStringObject(pushData.optString(AppLibrary.NOTIFICATION_TYPE)) != null && pushData.optString("senderId") != null) {

                            if (pushData.optString(AppLibrary.NOTIFICATION_TYPE).equalsIgnoreCase("publicEvent")) {
                                if (AppLibrary.checkStringObject(pushData.optString("stream")) != null) {
                                    String lastNotificationId = prefs.getString("LastNotificationId", null); // Null for first time user
                                    if (lastNotificationId != null &&
                                            ((lastNotificationId.equalsIgnoreCase(pushData.optString("stream"))) && ((int) (System.currentTimeMillis() / 1000) - prefs.getInt("LastNotificationTime", 0)) < 15 * 60)) {
                                        prefs.edit().putString("LastNotificationId", lastNotificationId).commit();
                                        //                            prefs.edit().putInt("LastNotificationTime", (int)(System.currentTimeMillis()/1000)).commit();
                                        AppLibrary.log_d(TAG, "Received similar notification again, skipping");
                                    } else {
                                        prefs.edit().putString("LastNotificationId", pushData.optString("stream")).commit();
                                        prefs.edit().putInt("LastNotificationTime", (int) (System.currentTimeMillis() / 1000)).commit();
                                        AppLibrary.log_d(TAG, "New notification, preparing to send");
                                        sendNotification(intent, this, pushData.optString("body"), pushData.optString("title"), pushData.optString("pictureUrl"), pushData.optString("stream"), pushData.optString("thumbnail"));
                                    }
                                } else {
                                    sendNotification(intent, this, pushData.optString("body"), pushData.optString("title"), pushData.optString("pictureUrl"),null, pushData.optString("thumbnail"));
                                }
                            }
                    }
                }
            }

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }


    private void sendNotification(Intent MainIntent, Context context,String msg,String title,String picUrl,String stream,String thumbnail) {
        AppLibrary.log_i(TAG, "Preparing to send notification...: Message -" + msg + " Title- " + title);
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                64,
                context.getResources().getDisplayMetrics()
        );
        Bitmap picture = null;
        Bitmap BigPicture = null;

        Random random = new Random();
        int contentIntentRequestCode = random.nextInt();
//        int deleteIntentRequestCode = random.nextInt();

        try {
            if (android.os.Build.VERSION.SDK_INT > 20) {
                if (AppLibrary.checkStringObject(picUrl) != null) {
                    if(picUrl.contains("https://"))
                        picUrl = picUrl.replace("https://","http://"); //Avoid https SSL handshake error in Notifications
                    picture = Picasso.with(context).load(picUrl).resize(px, px).transform(new RoundedTransformation()).get();
                }else {
                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).transform(new RoundedTransformation()).get();
                }
            }else {
                if (AppLibrary.checkStringObject(picUrl) != null) {
                    if(picUrl.contains("https://"))
                        picUrl = picUrl.replace("https://","http://"); //Avoid https SSL handshake error in Notifications
                    picture = Picasso.with(context).load(picUrl).resize(px, px).get();

                }else {
                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).get();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Intent intent = new Intent(); //Required for Analytics
        intent.setPackage("com.instalively.android");
        intent.putExtras(MainIntent.getExtras());
        if (stream != null) {
//            intent.setClass(context, YoutubePlayerActivity.class);
//        intent.setClass(context, NotificationReceiver.class);
//        Intent intent = new Intent(context,YoutubePlayerActivity.class);
            intent.putExtra("Notification", true);
            intent.putExtra(AppLibrary.EVENT_SID, stream);
        } else {
            intent.setClass(context, CameraActivity.class);
        }

        if (thumbnail != null && !thumbnail.equals("")) {
            intent.putExtra(AppLibrary.EVENT_THUMBNAIL, thumbnail);

            if (thumbnail.contains("https://"))
                thumbnail = thumbnail.replace("https://","http://"); //Avoid https SSL handshake error in Notifications
            else if (!thumbnail.contains("http://")) //Server sends us without s3 url prefix
                thumbnail = "http://s3-ap-southeast-1.amazonaws.com/instalively.images/" + thumbnail;

            try {
                BigPicture = Picasso.with(getApplicationContext()).load(thumbnail).resize(512, 256).centerCrop().get();
            } catch (Exception e) {
                e.printStackTrace();
                BigPicture = null;
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context,contentIntentRequestCode,
                intent,PendingIntent.FLAG_ONE_SHOT);

//        PendingIntent pDeleteIntent = PendingIntent.getBroadcast(context, deleteIntentRequestCode,
//                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        Notification notification;

        if(BigPicture==null) {
//            notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                    .setContentTitle(title)
//                    .setAutoCancel(true)
//                    .setTicker(title)
//                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                    .setLargeIcon(picture)
//                    .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                    .setContentText(msg).setContentIntent(contentIntent).build();
        } else {
//            notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                    .setContentTitle(title)
//                    .setAutoCancel(true)
//                    .setTicker(title)
//                    .setContentText(msg)
//                    .setLargeIcon(picture)
//                    .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(BigPicture).setBigContentTitle(title).setSummaryText(msg))
//                    .setColor(this.getResources().getColor(R.color.notification_icon_background))
//                    .setContentText(msg).setContentIntent(contentIntent).build();
        }

//        notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.tiny_bell_sms);
//        mNotificationManager.notify((int)System.currentTimeMillis(),notification);
    }
}
