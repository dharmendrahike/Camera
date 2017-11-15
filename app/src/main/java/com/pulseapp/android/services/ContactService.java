package com.pulseapp.android.services;

import android.app.IntentService;
import android.content.Intent;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by Morph on 6/30/2015.
 */
public class ContactService extends IntentService{

   private static final String TAG = "ContactService";


   public ContactService(){
       super(ContactService.class.getName());
   }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppLibrary.log_d(TAG,"Contact Service started");
//        try {
//            ContactProvider.getInstance().updateContactsRequest();
//        }catch (JSONException e){
//            e.printStackTrace();
//        }

        AppLibrary.log_d(TAG,"Contact Service stopped");
        this.stopSelf();
    }
}
