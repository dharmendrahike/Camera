package com.pulseapp.android.providers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.util.AppLibrary;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Morph on 6/13/2015.
 * Singleton Class to fetch and control users contacts
 */
final public class ContactProvider{

    public static ContactProvider mInstance;
    public static Context context;
    public JSONArray phoneContactList;
    public String contactList;
    public String countryCode = "";
    public static final String Tag = "ContactProvider";
    private TelephonyManager telephonyManager;
    private SharedPreferences preferences;


    private ContactProvider(){}

    public static ContactProvider getInstance(){
        if (mInstance == null){
            mInstance = new ContactProvider();
        }
        return mInstance;
    }

    public static void setContext(Context mcontext){
        context = mcontext;
    }

//    public void updateContactsRequest() throws JSONException{
//        telephonyManager = (TelephonyManager)context.getSystemService(context.TELEPHONY_SERVICE);
//        countryCode = telephonyManager.getSimCountryIso();
//        try {
//            getContactInfo(context);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        List<NameValuePair> pairs = new ArrayList<>();
////        pairs.add(new BasicNameValuePair("phonebook",phoneContactList.toString()));
//        pairs.add(new BasicNameValuePair("contacts",contactList));
//        pairs.add(new BasicNameValuePair("country_code",countryCode));
//        RequestManager.makePostRequest(context,RequestManager.USER_UPDATE_CONTACT_REQUEST,
//                RequestManager.USER_UPDATE_CONTACT_RESPONSE,null,pairs,updateContactCallback);
//    }

    private RequestManager.OnRequestFinishCallback updateContactCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                if (success) {
                    JSONObject object = (JSONObject)response;
                    if (object.getString("error").equalsIgnoreCase("false")){
                        AppLibrary.log_d(Tag,"Update Contact Success- "+object.getString("value"));
                        //set a flag that contact has been updated to the server.
                        preferences = context.getSharedPreferences(AppLibrary.APP_SETTINGS,0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(AppLibrary.CONTACT_UPDATED,true);
                        editor.commit();
                    }else {
                        AppLibrary.log_d(Tag,"Update Contact Error- "+object.getString("value"));
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };



    public void getContactInfo(Context mContext) throws Exception{
        phoneContactList = new JSONArray();
        contactList = "";

        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);
        if (cursor.getCount() > 0){
            while (cursor.moveToNext()){
                boolean isFetched = false;
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0){

                    Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID+"=?",new String[]{id},null);
                    while (phoneCursor.moveToNext()){
                        if (isFetched){
                            break;
                        }
                        String mime = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.MIMETYPE));
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contactList = contactList+phoneNumber+",";
//                        System.out.println("Name: "+name+", Id: "+id+", Number :"+phoneNumber+", MimeType :"+mime);
                        //searching the above contact id in DATA table with query parameter as watsApp mime type and contact id
                        Cursor c = contentResolver.query(ContactsContract.Data.CONTENT_URI,null,ContactsContract.Data.MIMETYPE+"=? and "+ContactsContract.Data.CONTACT_ID+"=?",new String[]{"vnd.android.cursor.item/vnd.com.whatsapp.profile",id},null);
                        String watsAppNumber = "";
                        while (c.moveToNext()){
                            watsAppNumber = c.getString(c.getColumnIndex(ContactsContract.Data.DATA3));
                        }
                        c.close();
                        JSONObject object = new JSONObject();
                        object.put("phoneNumber",phoneNumber);
                        object.put("Name",name);
                        if (phoneNumber.equals(watsAppNumber))
                            object.put("isWatsAppContact",true);
                        else
                            object.put("isWatsAppContact",false);
                        phoneContactList.put(object);
                        isFetched = true;

                    }
                    phoneCursor.close();
                }
            }
            cursor.close();
        }


//        JSONObject response = new JSONObject();
//        response.put("phoneContact",phoneContactList.toString());
//        response.put("Contact",contactList);
//        return response;

    }

    public void setCountryCode(String code){
        countryCode = code;
    }

    public void testContact(Context context){
        Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[] {ContactsContract.Data._ID, ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL},
                ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
                null,
                ContactsContract.Data.DISPLAY_NAME);
        String[] columnNames = c.getColumnNames();
        int displayNameColIndex = c.getColumnIndex("display_name");
        int idColIndex = c.getColumnIndex("_id");
        int col2Index = c.getColumnIndex(columnNames[2]);
        int col3Index = c.getColumnIndex(columnNames[3]);
        int col4Index = c.getColumnIndex(columnNames[4]);
        int count = c.getCount();
        while(c.moveToNext()){
            String displayName = c.getString(displayNameColIndex);
            String phoneNumber = c.getString(col2Index);
//            System.out.println("CONTACT "+displayName+"-"+phoneNumber);
        }
        c.close();
    }

    public void getWatsappContacts(Context mContext){
        final String[] projection={
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                "account_type",
                ContactsContract.Data.DATA3,
        };
        final String selection = ContactsContract.Data.MIMETYPE+" =? and account_type=?";
        final String[] selectionArgs = {
                "vnd.android.cursor.item/vnd.com.whatsapp.profile",
                "com.whatsapp"
        };
        ContentResolver cr = mContext.getContentResolver();
        Cursor c = cr.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        while(c.moveToNext()){
            String id = c.getString(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            String number = c.getString(c.getColumnIndex(ContactsContract.Data.DATA3));
            String name="";
            Cursor mCursor = mContext.getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                    ContactsContract.Contacts._ID+" =?",
                    new String[]{id},
                    null);
            while(mCursor.moveToNext()){
                name = mCursor.getString(mCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
//            System.out.println("NAME "+name+"Number "+number);
            mCursor.close();

        }

        c.close();
    }
}
