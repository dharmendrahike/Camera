package com.pulseapp.android.apihandling;

import android.os.Bundle;

//import com.instalively.android.data.Category;
//import com.instalively.android.data.ChannelDetails;
//import com.instalively.android.data.ChatDetails;
//import com.instalively.android.data.EventDetails;
//import com.instalively.android.data.Session;
import com.pulseapp.android.util.AppLibrary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by admin on 1/22/2015.
 */
public class Parser {

//    private static Category parseCategory(JSONObject categoryJSON) throws JSONException {
//        Category category = new Category();
//        category.setId(AppLibrary.checkStringObject(categoryJSON.optString("_id")));
//        category.setName(AppLibrary.checkStringObject(categoryJSON.optString("name")));
//        return category;
//    }
//
//    private static ChannelDetails parseChannel(JSONObject channelJSON) throws JSONException {
//        ChannelDetails channel = new ChannelDetails();
//        channel.setId(AppLibrary.checkStringObject(channelJSON.optString("_id")));
//        channel.setName(AppLibrary.checkStringObject(channelJSON.optString("name")));
//        return channel;
//    }
//
//    private static Session parseSession(JSONObject sessionJSON) throws JSONException {
//        Session session = new Session();
//        session.setId(AppLibrary.checkStringObject(sessionJSON.optString("_id")));
//        session.setDate(AppLibrary.checkStringObject(sessionJSON.optString("date")));
//        session.setFromTime(AppLibrary.checkStringObject(sessionJSON.optString("fromTime")));
//        session.setStream(AppLibrary.checkStringObject(sessionJSON.optString("stream")));
//        session.setToTime(AppLibrary.checkStringObject(sessionJSON.optString("toTime")));
//        session.setStatus(sessionJSON.optInt("status"));
//        session.setLiveOn(AppLibrary.checkStringObject(sessionJSON.optString("live_on")));
//        session.setFormat(AppLibrary.checkStringObject(sessionJSON.optString("format")));
//        session.setShortURL(AppLibrary.checkStringObject(sessionJSON.optString("short_id")));
//        return session;
//    }
//
//    public static EventDetails parseEvent(JSONObject eventJSON) throws JSONException {
//
//        EventDetails event = new EventDetails();
////        Log.d("Parser"," ----------event id : "+eventJSON.getString("_id"));
//        if (eventJSON.optString("_id", null) == null)
//            return event;
//
//        event.setId(AppLibrary.checkStringObject(eventJSON.optString("_id")));
//        event.setLocation(AppLibrary.checkStringObject(eventJSON.optString("location")));
//        event.setDescription(AppLibrary.checkStringObject(eventJSON.optString("description")));
//        event.setEventPageUrl(AppLibrary.checkStringObject(eventJSON.optString("eventPageUrl")));
//        event.setName(AppLibrary.checkStringObject(eventJSON.optString("name")));
//        event.setUserId(AppLibrary.checkStringObject(eventJSON.optString("user")));
//        event.setIsDeleted(eventJSON.optBoolean("isDeleted"));
//        event.setEventStatus(eventJSON.optInt("event_status"));
//        event.setNextSession(eventJSON.optInt("nextSession"));
//        event.setCreatedAt(AppLibrary.checkStringObject(eventJSON.optString("created_at")));
//        event.setImageUrl(AppLibrary.checkStringObject(eventJSON.optString("imageUrl")));
//        event.setThumbUrl(AppLibrary.checkStringObject(eventJSON.optString("thumbUrl")));
//        event.setTeaserUrl(AppLibrary.checkStringObject(eventJSON.optString("teaser")));
//
//        if(eventJSON.optJSONObject("user") != null){
//            event.setFullName(AppLibrary.checkStringObject(eventJSON.getJSONObject("user").optString("fullName")));
//            event.setProfilePicUrl(AppLibrary.checkStringObject(eventJSON.getJSONObject("user").optString("pictureUrl")));
//        }
//        if (eventJSON.optJSONObject("metrics") != null)
//            event.setLikesCount(eventJSON.getJSONObject("metrics").optInt("like"));
//
//        if (eventJSON.optJSONObject("view") != null)
//            event.setViewsCount(eventJSON.getJSONObject("view").optInt("_total"));
//
//        if (eventJSON.optJSONObject("coordinate") != null) {
//            event.setLatitude(eventJSON.getJSONObject("coordinate").optDouble("latitude", 0));
//            event.setLongitude(eventJSON.getJSONObject("coordinate").optDouble("longitude", 0));
//        }
//
//        if (eventJSON.optJSONObject("category") != null) {
//            event.setCategory(parseCategory(eventJSON.getJSONObject("category")));
//        }
//
//        if (eventJSON.optJSONObject("channel") != null) {
//            event.setChannel(parseChannel(eventJSON.getJSONObject("channel")));
//        }
//
//        event.setSource(eventJSON.optInt("source"));
//
//        JSONArray arrayObject = eventJSON.optJSONArray("subCategory");
//        ArrayList<Category> categories = new ArrayList<>();
//        if (arrayObject != null) {
//            for (int i = 0; i < arrayObject.length(); i++) {
//                if(arrayObject.optJSONObject(i) != null) {
//                    JSONObject subObject = arrayObject.getJSONObject(i);
//                    categories.add(parseCategory(subObject));
//                }
//            }
//        }
//        event.setSubCategory(categories);
//
//        arrayObject = eventJSON.optJSONArray("sessions");
//        ArrayList<Session> sessions = new ArrayList<>();
//        if (arrayObject != null) {
//            for (int i = 0; i < arrayObject.length(); i++) {
//                JSONObject subObject = arrayObject.getJSONObject(i);
//                sessions.add(parseSession(subObject));
//            }
//        }
//        event.setSessions(sessions);
//
//        HashMap<String, String> keys = new HashMap<>(10);
//        arrayObject = eventJSON.optJSONArray("stream_keys");
//        if (arrayObject != null) {
//            for (int i = 0; i < arrayObject.length(); i++) {
//                JSONObject subObject = arrayObject.optJSONObject(i);
//                if (subObject != null)
//                    keys.put(subObject.optString("format"), subObject.optString("key"));
//            }
//        }
//        event.setStreamKeys(keys);
//        return event;
//    }
//
//    public static ArrayList<EventDetails> parseEvents(JSONArray jsonArray) throws JSONException {
//        ArrayList<EventDetails> events = new ArrayList<>();
//        if (jsonArray != null)
//            for (int i = 0; i < jsonArray.length(); i++) {
//                events.add(parseEvent(jsonArray.getJSONObject(i)));
//            }
//        return events;
//    }

    //  BroadCast screen
    public static String parseStreamStatus(JSONObject jsonObject) throws JSONException {
        JSONArray array = jsonObject.getJSONArray("items");
        jsonObject = array.getJSONObject(0);
        jsonObject = jsonObject.getJSONObject("status");
        return jsonObject.getString("streamStatus");
    }

    public static String parseBroadcastStatus(JSONObject jsonObject) throws JSONException {
        JSONArray array = jsonObject.getJSONArray("items");
        jsonObject = array.getJSONObject(0);
        jsonObject = jsonObject.getJSONObject("status");
        return jsonObject.getString("lifeCycleStatus");
    }

    public static String parseBroadcastTransitionStatus(JSONObject jsonObject) throws JSONException {
        jsonObject = jsonObject.getJSONObject("status");
        return jsonObject.getString("lifeCycleStatus");
    }

    public static Bundle userSignupResponse(JSONObject jsonObject) throws JSONException {
        Bundle args = new Bundle();
        jsonObject = jsonObject.getJSONObject("user");
        args.putString(AppLibrary.USER_LOGIN, jsonObject.getString("_id"));
        args.putString(AppLibrary.USER_LOGIN_EMAIL, jsonObject.getString("email"));
        args.putString(AppLibrary.MOBILE_CHANNEL, jsonObject.getString("mobile_channel"));
        args.putString("flags",jsonObject.getString("flags"));
        if (jsonObject.optJSONObject("youtube") != null)
            args.putBoolean("enabled_live_stream", jsonObject.getJSONObject("youtube").getBoolean("liveStreamEnabled"));
        return args;
    }

//    public static ArrayList<ChatDetails> parseChats(JSONArray jsonArray) throws JSONException {
//        ArrayList<ChatDetails> chats = new ArrayList<>();
//        isFirstTime=true;
//        if (jsonArray != null)
//            for (int i = 0; i < jsonArray.length(); i++) {
//                chats.add(parseChat(jsonArray.getJSONObject(i)));
//            }
//        return chats;
//    }
//
//    public static boolean isFirstTime=true;
//    private static ChatDetails parseChat(JSONObject jsonObject) throws JSONException {
//        ChatDetails chat = new ChatDetails();
//        if (jsonObject.has("userId") && jsonObject.getString("userId") != null)
//            chat.setUserId(jsonObject.getString("userId"));
//        if(jsonObject.has("streamId") && jsonObject.getString("streamId") !=  null)
//            chat.setStreamId(jsonObject.getString("streamId"));
//        if(jsonObject.has(AppLibrary.MESSAGE_SENDER) && jsonObject.getString(AppLibrary.MESSAGE_SENDER) !=  null)
//            chat.setUsername(jsonObject.getString(AppLibrary.MESSAGE_SENDER));
//        if(jsonObject.has(AppLibrary.MESSAGE_SENDER_IMAGE) && jsonObject.getString(AppLibrary.MESSAGE_SENDER_IMAGE) !=  null)
//            chat.setProfileImage(jsonObject.getString(AppLibrary.MESSAGE_SENDER_IMAGE));
//        if(jsonObject.has(AppLibrary.MESSAGE_KEY) && jsonObject.getString(AppLibrary.MESSAGE_KEY) !=  null)
//            chat.setMessage(jsonObject.getString(AppLibrary.MESSAGE_KEY));
//        if(jsonObject.has(AppLibrary.PRESENTER))
//            chat.setPresenter(jsonObject.getBoolean(AppLibrary.PRESENTER));
//        if(jsonObject.has("created_at") && jsonObject.getString("created_at") != null)
//            chat.setCreated_time(jsonObject.getString("created_at"));
//        if(jsonObject.has("feedType") && jsonObject.getString("feedType") != null)
//            chat.setFeed_type(jsonObject.getString("feedType"));
//        if(jsonObject.has("state") && jsonObject.getString("state") != null)
//            chat.setState(jsonObject.getString("state"));
//        if(chat.getState().equals("complete")){
//            if(isFirstTime) {
////                Log.d("Parser","first time : "+isFirstTime);
//                chat.setShowBroadcastOver(true);
//                isFirstTime=false;
//            }
//            else {
////                Log.d("Parser","else, first time : "+isFirstTime);
//                chat.setShowBroadcastOver(false);
//            }
//        }
//        if (jsonObject.optBoolean("isBlocked")){
//            chat.setUserBlocked(true);
//        } else {
//            chat.setUserBlocked(false);
//        }
//        return chat;
//    }
}