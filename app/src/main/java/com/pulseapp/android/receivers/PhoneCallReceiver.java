package com.pulseapp.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.telephony.TelephonyManager;

import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.io.IOException;

/**
 * Created by Morph on 7/20/2015.
 */
public class PhoneCallReceiver extends BroadcastReceiver {

    private static MediaRecorder mRecorder;
    private File outputFile;

    @Override
    public void onReceive(Context context, Intent intent) {
        AppLibrary.log_d("PhoneCallRecorder", "Received an intent");
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                TelephonyManager.EXTRA_STATE_RINGING)) {
            // Ringing state
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            // This code will execute when the call is answered
            System.out.println("CALL ACCEPTED");
            intializeRecorder();
        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                TelephonyManager.EXTRA_STATE_IDLE)){
            if (this.mRecorder != null){
				AppLibrary.log_d("PhoneCallRecorder", "CAll Disconnected");
                stopRecording();
            } else {
				AppLibrary.log_d("PhoneCallRecorder", "Recorder is null");
            }
        }
    }

    private void intializeRecorder(){
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/InstaLively/");
        dir.mkdirs();
        outputFile = new File(dir,"recording.3gp");
        this.mRecorder = new MediaRecorder();
        this.mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mediaRecorder, int i, int i2) {
                System.out.println(String.format("Audio error %d/%d", new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}));
            }
        });
        this.mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK);
        this.mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mRecorder.setAudioChannels(2);
//        mRecorder.setAudioSamplingRate(44100);
        this.mRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/InstaLively/recording.3gp");
        this.mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recordAudioCall();
    }

    private void recordAudioCall(){
        try {
            this.mRecorder.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecorder.start();
            }
        }, 256);
        AppLibrary.log_d("PhoneCallRecorder", "Recording started!");
    }


    private void stopRecording(){
        try {
            this.mRecorder.stop();
            this.mRecorder.release();
            this.mRecorder = null;
			AppLibrary.log_d("PhoneCallRecorder", "Recording stopped!");
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

}
