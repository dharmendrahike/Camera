package com.pulseapp.android.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.util.AppLibrary;

/**
 * Created by Morph on 6/13/2015.
 */
public class SmsReceiver extends BroadcastReceiver {

    public static final String SMS_BUNDLE = "pdus";
    public static final String TAG = "SmsReceiver";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private Context mcontext;
    private SharedPreferences prefs;
    private long defVal = 0;


    @Override
    public void onReceive(Context context, Intent intent) {
        mcontext = context;
        prefs = context.getSharedPreferences(AppLibrary.APP_SETTINGS,0);
        Bundle intentExtras = intent.getExtras();
        if (intentExtras != null){
            Object[] sms = (Object[])intentExtras.get(SMS_BUNDLE);
            for (int i = 0;i < sms.length;i++){
                SmsMessage message = SmsMessage.createFromPdu((byte[])sms[i]);
                String smsBody = message.getMessageBody();
                String smsAddress = message.getOriginatingAddress();
                AppLibrary.log_d(TAG,"Sms Address" +smsAddress);  //make a check if the message is from google (BW-Google)
                if (smsAddress.contains("BW-Google")) {
                    String verificationCode = smsBody.replaceAll("\\D+", "");
                    AppLibrary.log_d(TAG, "Verification Code : " + verificationCode);

                    //Set verification code wherever

                    if (prefs.getBoolean(AppLibrary.ActivityStopped, false)) {
                        sendNotification(verificationCode);
                    }
                }
            }
        }
    }

    private void sendNotification(String msg) {
        AppLibrary.log_i(TAG, "Preparing to send notification...: " + msg);
        mNotificationManager = (NotificationManager)mcontext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(mcontext, CameraActivity.class);
        intent.putExtra("verificationCode",msg);
        PendingIntent contentIntent = PendingIntent.getActivity(mcontext,0,intent,0);

//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
//                mcontext).setSmallIcon(R.drawable.logoart_inverted_small)
//                .setContentTitle("Your Instalively verification code has arrived. Please click to enable verification.")
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                .setContentText(msg);
//
//        mBuilder.setContentIntent(contentIntent);
//        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        Log.d(TAG, "Notification sent successfully.");
    }


}
