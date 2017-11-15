package com.pulseapp.android.chatsetup;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.pulseapp.android.util.AppLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

/**
 * Created by Karthik on 17/12/15.
 */
public class NotificationReceiver extends BroadcastReceiver {
    public static final String TAG = "NotificationReceiver";
    private NotificationManager mNotificationManager;
    private SharedPreferences prefs;
    Bundle extras;
    @Override
    public void onReceive(Context context, Intent intent) {

    }

    protected void onPushReceive(Context context, Intent intent) {
        AppLibrary.log_i(TAG, "Parse Push Received");

        JSONObject pushData = getPushData(intent);
        if(!pushData.optString(AppLibrary.NOTIFICATION_TYPE).isEmpty()) {
            AppLibrary.log_i(TAG, "Parse Push from Server");
            ComponentName comp = new ComponentName(context.getPackageName(),
                    FinalNotificationIntentService.class.getName());
            startWakefulService(context, (intent.setComponent(comp)));
        }
        else{
            AppLibrary.log_i(TAG, "Default Parse Push Received");
//            super.onPushReceive(context, intent);
        }

//        Iterator<String> iter = pushData.keys();
//        super.onPushReceive(context, intent);
    }

//    @Override
    protected void onPushOpen(Context context, Intent intent) {
        AppLibrary.log_i(TAG, "Notification opened Analytics Call");
//        ParseAnalytics.trackAppOpenedInBackground(intent);
//        super.onPushOpen(context, intent);
    }

//    @Override
//    protected Notification getNotification(Context context, Intent intent){
//        return super.getNotification(context, intent);
//    }

//    private void sendNotification(Context context,String msg,String title,String picUrl,String stream,int notification_id,String thumbnail) {
//        AppLibrary.log_i(TAG, "Preparing to send notification...: Message -" + msg + " Title- " + title + " Stream- " + stream + " Notification Id -" + notification_id);
//        mNotificationManager = (NotificationManager) context
//                .getSystemService(Context.NOTIFICATION_SERVICE);
//        int px = (int) TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                64,
//                context.getResources().getDisplayMetrics()
//        );
//        Bitmap picture = null;
//        try {
//            if (android.os.Build.VERSION.SDK_INT > 20) {
//                if (AppLibrary.checkStringObject(picUrl) != null) {
//                    if(picUrl.contains("https://"))
//                        picUrl = picUrl.replace("https://","http://"); //Avoid https SSL handshake error in Notifications
//                    picture = Picasso.with(context).load(picUrl).resize(px, px).transform(new RoundedTransformation()).get();
//                }else {
//                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).transform(new RoundedTransformation()).get();
//                }
//            }else {
//                if (AppLibrary.checkStringObject(picUrl) != null) {
//                    if(picUrl.contains("https://"))
//                        picUrl = picUrl.replace("https://","http://"); //Avoid https SSL handshake error in Notifications
//                    picture = Picasso.with(context).load(picUrl).resize(px, px).get();
//
//                }else {
//                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).get();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        Intent intent = new Intent(context,YoutubePlayerActivity.class);
//        intent.putExtra("Notification",true);
//        intent.putExtra(AppLibrary.EVENT_SID, stream);
//        if (thumbnail != null && !thumbnail.equals(""))
//            intent.putExtra(AppLibrary.EVENT_THUMBNAIL,thumbnail);
//        intent.setData(Uri.parse(Integer.toString(notification_id)));
//        PendingIntent contentIntent = PendingIntent.getActivity(context,(int) System.currentTimeMillis(),
//                intent,PendingIntent.FLAG_ONE_SHOT);
//
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
//                context);
//        Notification notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                .setContentTitle(title)
//                .setAutoCancel(true)
//                .setTicker(title)
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                .setLargeIcon(picture)
//                .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                .setContentText(msg).setContentIntent(contentIntent).build();
//
//        notification.sound = Uri.parse("android.resource://"+context.getPackageName()+ "/" + R.raw.tiny_bell_sms);
//
//        mNotificationManager.notify(notification_id,notification);
//    }


    private JSONObject getPushData(Intent intent) {
        try {
            return new JSONObject(intent.getStringExtra("com.parse.Data"));
        } catch (JSONException e) {
            AppLibrary.log_d(TAG, "Unexpected JSONException when receiving push data:" + e);
            return null;
        }
    }
}
