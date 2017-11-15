package com.pulseapp.android.firebase;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.AccessToken;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Logger;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.apihandling.RequestManager;

import com.pulseapp.android.data.MemoryCachedUsersData;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.downloader.MediaDownloader;
//import com.pulseapp.android.downloader.NearbyMomentDownloader;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.modelView.MediaModelView;
//import com.pulseapp.android.modelView.SearchFriendsModel;
import com.pulseapp.android.modelView.SearchResultModel;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.models.LocalDataHelper;
import com.pulseapp.android.models.MediaDownloadDetails;
import com.pulseapp.android.models.MediaModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.NotificationContent;
import com.pulseapp.android.models.PublicMomentModel;
import com.pulseapp.android.models.RoomDetails;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.models.ViewerDetails;
import com.pulseapp.android.services.CleanDataService;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.PrivacyPopup;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by deepankur on 23/3/16.
 */
public class FireBaseHelper implements FireBaseKEYIDS {

    public static String myUserId = null;//access it only via function getMyuserId()
    private static FireBaseHelper mFireBaseHelper;
    private static LinkedHashMap<String,CustomMomentModel> contributedMoments = new LinkedHashMap<>();
    private static Context mContext;

    private LinkedHashMap<String, RoomDetails> mMomentRoomHashMap;
    private LinkedHashMap<String, RoomsModel> mMessageRoomHashMap;
    private HashMap<String, UserModel.Rooms> mExpiredMomentRooms;
    private LinkedHashMap<String, MomentModel> mMoments;
    private DatabaseReference mFireBase;
    private String TAG = this.getClass().getSimpleName();
    private UserModel mUserModel;
    private SocialModel mSocialModel;
    private HashMap<String, RoomsModel> mFriendRoomHashMap;
    private MediaModelCallback mediaModelCallback;
    private RoomDataLoadedCallbacks roomDataLoadedListener;
    private CustomFriendListCallBack customFriendListCallback;
    private OnMyStreamsLoaded onMyStreamsLoadedCallback;
    private onUploadStatusChanges onUploadStatusChangesCallback;
    private onMyMomentMediaDownloadStatusModified onMyMomentMediaDownloadStatusModified;
    private MomentModel myMoment, instituteMoment;
    private LinkedHashMap<String, MediaModel> pendingMediaInMyMoments = new LinkedHashMap<>();
    private LinkedHashMap<String, MediaModel> pendingMediaInMessageRooms = new LinkedHashMap<>();
    private LinkedHashMap<String, MediaModelView> myMedias = new LinkedHashMap();
    private LinkedHashMap<String, SliderMessageModel> sliderMessageModels = new LinkedHashMap<>();
    private LinkedHashMap<String,LinkedHashMap<String,MediaModelView>> contributedMomentsMediaDetails = new LinkedHashMap<>();
    private LinkedHashMap<String,SocialModel.RequestReceived> socialRequests = new LinkedHashMap<>();
    private HandlerThread firebaseHelperThread;
    private Handler firebaseHelperHandler;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    public ArrayList<UserModel.InstitutionData> getAllInstitutesData() {
        return allInstitutesData;
    }

    private ArrayList<UserModel.InstitutionData> allInstitutesData;

    public void setFireBaseReadyListener(FireBaseReadyListener fireBaseReadyListener) {
        FireBaseHelper.fireBaseReadyListener = fireBaseReadyListener;
        fireBaseReadyListener.onDataLoaded(getSocialModel(), getMyUserModel());
    }

    public Handler getFirebaseHelperHandler(){
        return firebaseHelperHandler;
    }

    private static boolean initializationDone;
    private FireBaseHelper(Context context) {
        this.mContext = context;
        if (mFireBase == null) {

            if (!initializationDone) {
                if (!AppLibrary.PRODUCTION_MODE)
                    FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            }

            mFireBase = FirebaseDatabase.getInstance().getReferenceFromUrl(FireBaseKEYIDS.fireBaseURL);
            initializationDone = true;
        }

        if (mFirebaseRemoteConfig == null) {
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            if (!AppLibrary.PRODUCTION_MODE) {
                FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(BuildConfig.DEBUG)
                        .build();
                mFirebaseRemoteConfig.setConfigSettings(configSettings);
            }
            mFirebaseRemoteConfig.setDefaults(R.xml.app_defaults);
            fetchServerValues();
        }

        if (firebaseHelperThread == null) {
            firebaseHelperThread = new HandlerThread("cameraStickerThread");
            firebaseHelperThread.start();
        }
        if (firebaseHelperHandler == null)
            firebaseHelperHandler = new Handler(firebaseHelperThread.getLooper());

        if (getMyUserId() == null) return;

        if (!AppLibrary.PRODUCTION_MODE)
            mFireBase.child("templates").keepSynced(true);//only for faster debugging  remove this line in production // TODO: 6/21/16

        loadData();

        if (!AppLibrary.MESSAGING_DISABLED)
            cleanMessageData(getMyUserId());
    }

    private void fetchServerValues() {
        long cacheExpiration = 0; // in seconds.
        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
//        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
//            cacheExpiration = 0;
//        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            AppLibrary.log_d(TAG,"fetch successfull");

                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            AppLibrary.log_d(TAG,"fetch failed");
                        }
                        printValues();
                    }
                });
    }

    private void printValues() {
        AppLibrary.log_d(TAG,"expiry time fetched value is -"+mFirebaseRemoteConfig.getString(EXPIRY_TIME));
    }

    private static FireBaseReadyListener fireBaseReadyListener;

    public LinkedHashMap<String, SliderMessageModel> getMessageList() {
        return sliderMessageModels;
    }

    public void updateSliderStatus(int messageType, String roomId) {
        mFireBase.child(ROOMS).child(roomId).child(MEMBERS).child(getMyUserId()).child(TYPE).setValue(messageType);
    }

    public MomentModel getInstituteMomentModel(final String momentId) {
        if (momentId != null) {
            instituteMoment = null;
            mFireBase.child(ANCHOR_MOMENTS).child(momentId).keepSynced(true);
            mFireBase.child(ANCHOR_MOMENTS).child(momentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    instituteMoment = dataSnapshot.getValue(MomentModel.class);
                    if (instituteMoment != null) {
                        instituteMoment.momentId = momentId;
                    }
                    instituteLoadedCallback.onInstituteLoaded(instituteMoment);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });

            return instituteMoment;
        }
        return null;
    }

    public void followUser(String userId, long createdAt) {
        if (getSocialModel().relations == null)
            getSocialModel().relations = new HashMap<>();
        getSocialModel().relations.put(userId,FOLLOWED);

        HashMap<Object,Object> followObject = new HashMap<>();
        followObject.put(REQUEST_USER_ID,getMyUserId());
        followObject.put(USER_ID,userId);
        if (createdAt > 0)
            followObject.put(MEDIA_TIME,createdAt);
        postFireBaseRequest(FOLLOW_USER,followObject);
    }

    public void unFollowUser(String userId) {
//        getNewFireBase(ANCHOR_SOCIALS, new String[]{userId,FOLLOWED_BY,getMyUserId()}).removeValue();
        getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId(),RELATIONS,userId}).removeValue();
        getNewFireBase(FOLLOWED_ROOM, new String[]{getMyUserId(),userId}).removeValue();

        if (getSocialModel().relations != null && getSocialModel().relations.containsKey(userId))
            getSocialModel().relations.remove(userId);

        HashMap<Object,Object> followObject = new HashMap<>();
        followObject.put(REQUEST_USER_ID,getMyUserId());
        followObject.put(USER_ID,userId);
        postFireBaseRequest(UNFOLLOW_USER,followObject);
    }

    public int getMediaExpiryTime() {
        return Integer.parseInt(mFirebaseRemoteConfig.getString(EXPIRY_TIME));
    }

    public double getMaximumMediaDuration() {
        return System.currentTimeMillis() - getMediaExpiryTime() * 60 * 60 * 1000;
    }

    public interface FireBaseReadyListener {
        void onDataLoaded(SocialModel socialModel, UserModel userModel);
    }

    public static synchronized FireBaseHelper getInstance(Context context) {
        String TAG = "FireBaseHelper";
        if (mFireBaseHelper == null) {
            mFireBaseHelper = new FireBaseHelper(context.getApplicationContext());
            Log.i(TAG, "initialized fireBase ");
            Log.d(TAG, BaseFragment.buildStackTraceString(Thread.currentThread().getStackTrace()));
        }
        return mFireBaseHelper;
    }

    public  synchronized void nullfySingleTon(){
        mFireBaseHelper = null;
    }

    private void loadMySocialModel() {
        DatabaseReference socialFireBase = this.getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId()});
        socialFireBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSocialModel = dataSnapshot.getValue(SocialModel.class);
                        if (mSocialModel != null) {
                            getFriendList();
                            MemoryCachedUsersData.getInstance(mContext).initSentRequestData(mSocialModel.requestSent);
                        } else Log.e(TAG, " mSocialModel null ");
                        if (fireBaseReadyListener != null)
                            fireBaseReadyListener.onDataLoaded(mSocialModel, mUserModel);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void loadPendingFriendRequests(){
        this.getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId(),REQUEST_RECEIVED}).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot.getValue() != null) {
                            SocialModel.RequestReceived requestReceived = dataSnapshot.getValue(SocialModel.RequestReceived.class);
                            socialRequests.put(dataSnapshot.getKey(), requestReceived);
                            if (onMyStreamsLoadedCallback != null)
                                onMyStreamsLoadedCallback.onSocialRequestChanges(socialRequests);
                        }
                    }
                });
            }

            @Override
            public void onChildChanged(final DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot.getValue() != null) {
                            socialRequests.remove(dataSnapshot.getKey());
                            if (onMyStreamsLoadedCallback != null)
                                onMyStreamsLoadedCallback.onSocialRequestChanges(socialRequests);
                        }
                    }
                });
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private SocialModel.Friends[] getFriendList() {
        if (mSocialModel.friends == null) {
            Log.d(TAG, " user " + getMyUserId() + " has no friends");
            return null;
        }
        HashMap<String, SocialModel.Friends> friendsHashMap = mSocialModel.friends;
        String[] friendKeys = friendsHashMap.keySet().toArray(new String[friendsHashMap.size()]);
        SocialModel.Friends[] friendList = new SocialModel.Friends[friendKeys.length];
        for (int i = 0; i < friendKeys.length; i++) {
            friendList[i] = friendsHashMap.get(friendKeys[i]);
            friendList[i].friendId = friendKeys[i];
        }
        return friendList;
    }

    private ArrayList<FaceBookFriendsModel> faceBookFriendsuggestions;

    private void loadFriendSuggestions() {
        final String TAG = this.TAG + "loadFriendSuggestions";
        this.getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId(), FACEBOOK_FRIENDS}).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (faceBookFriendsuggestions == null)
                            faceBookFriendsuggestions = new ArrayList<>();
                        else faceBookFriendsuggestions.clear();

                        for (DataSnapshot friendSnap : dataSnapshot.getChildren()) {
                            FaceBookFriendsModel model = new FaceBookFriendsModel(friendSnap.getKey(), friendSnap.child(NAME).getValue(String.class), friendSnap.child(IMAGE_URL).getValue(String.class), friendSnap.child(HANDLE).getValue(String.class), false);
                            faceBookFriendsuggestions.add(model);
                        }
                        if (onMyStreamsLoadedCallback != null)
                            onMyStreamsLoadedCallback.onFacebookFriendsChanged();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

    }

    public ArrayList<FaceBookFriendsModel> getSuggestedFriends() {
        return faceBookFriendsuggestions;
    }

    public SocialModel getSocialModel() {
        return mSocialModel;
    }

    public UserModel getMyUserModel() {
        return mUserModel;
    }

    public MomentModel getMyInstituteModel() {
        if (instituteMoment == null) {
            loadInstituteMoment();
        }

        return instituteMoment;
    }

    public SettingsModel getSettingsModel() {
        return settingsModel;
    }

    /**
     * @param anchor   the outer  FireBase node ie. socials,moments,users,media
     * @param children subsequent nodes to query upon; supply null If you need the anchor nodes.
     *                 instead of children
     * @return the resolved FireBase
     */

    public DatabaseReference getNewFireBase(String anchor, String[] children) {
        if (mFireBase == null)
            mFireBase = FirebaseDatabase.getInstance().getReferenceFromUrl(FireBaseKEYIDS.fireBaseURL);
        if (anchor == null) throw new NullPointerException("anchor cannot be null");
        DatabaseReference fireBase = this.mFireBase.child(anchor);
        if (children != null && children.length > 0) {
            for (int i = 0; i < children.length; i++) {
                fireBase = fireBase.child(children[i]);
            }
        }
        return fireBase;
    }

    public String getMyUserId() {
        String userId;
        if (myUserId != null) {
            userId = myUserId;
        } else {
            userId = mContext.getSharedPreferences(AppLibrary.APP_SETTINGS, 0).getString(AppLibrary.USER_LOGIN, "");
        }
        return userId;
    }

    public void setMyUserId(String userId) {
        myUserId = userId;
    }


    public long getUpdatedAt() {
        return System.currentTimeMillis();
    }

    private void loadAllFriendRooms() {
        mFriendRoomHashMap = new HashMap<>();
        if (getMyUserModel().messageRooms == null) return;
        final HashMap<String, UserModel.Rooms> messageRoomsHashMap = getMyUserModel().messageRooms;
        String[] roomKeys = messageRoomsHashMap.keySet().toArray(new String[messageRoomsHashMap.size()]);
        for (int i = 0; i < roomKeys.length; i++) {
            DatabaseReference roomFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomKeys[i]});
            roomFireBase.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                    firebaseHelperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mFriendRoomHashMap == null)
                                mFriendRoomHashMap.put(dataSnapshot.getKey(), dataSnapshot.getValue(RoomsModel.class));
                        }
                    });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    private long getPriorityTimeStamp() {
        return System.currentTimeMillis();
    }

    long getCreatedAt() {
        return System.currentTimeMillis();
    }

    public void startCleaningDataService(){
        if (getFirebaseHelperHandler() != null) {
            getFirebaseHelperHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MasterClass.getGlobalContext().startService(new Intent(MasterClass.getGlobalContext(),CleanDataService.class));
                }
            },20000);
        }
    }

    private void loadMyUserModel() {
        DatabaseReference fireBase = this.getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId()});
        fireBase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mUserModel = dataSnapshot.getValue(UserModel.class);
                        if (mUserModel != null) {
                            mUserModel.KEY_UID = dataSnapshot.getKey();
                            if (fireBaseReadyListener != null)
                                fireBaseReadyListener.onDataLoaded(mSocialModel, mUserModel);
                            if (!AppLibrary.MESSAGING_DISABLED && mFriendRoomHashMap == null) //Message rooms
                                loadAllFriendRooms();
                            if (myMoment == null)
                                loadMyMoment();
                            if (instituteMoment == null)
                                loadInstituteMoment();
                        } else Log.e(TAG, "userModel is null ");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    // FireBase Request Api's (All request node write operation are to be done here)
    public void postFireBaseRequest(String action,HashMap<Object,Object> value){
        DatabaseReference ref;
        // add metaData
        value.put(AppLibrary.VERSION,RequestManager.APP_VERSION_CODE);
        value.put(AppLibrary.SOURCE,AppLibrary.ANDROID);
        // Post based on action
        switch (action) {
            case SEND_FRIEND_REQUEST_API:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{FRIENDS}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.ADD_FRIEND,AnalyticsEvents.USER_ID,value.get(AppLibrary.USER_ID).toString());
                break;
            case ACCEPT_FRIEND_REQUEST_API:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{FRIENDS}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.ACCEPT_FRIEND,AnalyticsEvents.USER_ID,value.get(AppLibrary.USER_ID).toString());
                break;
            case INSTITUTE_CHANGE_REQUEST_API:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{INSTITUTE_CHANGE_REQUEST}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SELECT_INSTITUTION,AnalyticsEvents.STREAM_ID,value.get(AppLibrary.MOMENT_ID).toString(),AnalyticsEvents.INSTITUTION_NAME,value.get(AppLibrary.NAME).toString());
                break;
            case UPDATE_NAME_REQUEST_API:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{SETTINGS_REQUEST}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case UPDATE_JOB_FOR_PUBLIC_MOMENT_CONTRIBUTION:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{ANCHOR_JOBS}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case UPDATE_MEDIA_COMPLETE:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{MEDIA_ADDED}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case FOLLOW_USER:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{FOLLOW}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.FOLLOW_PUBLIC_USER,AnalyticsEvents.USER_ID,value.get(AppLibrary.USER_ID).toString());
                break;
            case UNFOLLOW_USER:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{UNFOLLOW}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.UNFOLLOW_PUBLIC_USER,AnalyticsEvents.USER_ID,value.get(AppLibrary.USER_ID).toString());
                break;
            case UPDATE_TOKEN:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{NOTIFICATION_TOKEN}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case UPDATE_VIEWER:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{MEDIA_VIEW}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case UPDATE_FOR_SCREENSHOT:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{UPDATE_SCREENSHOT}).push();
                ref.setValue(value);
                ref.keepSynced(true);
                break;
            case UPDATE_FACEBOOK_REQUEST:
                ref = getNewFireBase(ANCHOR_REQUEST, new String[]{FACEBOOK_NODE}).push();
                ref.setValue(value);
                ref.keepSynced(true);
        }
    }

    public void sendFriendRequest(final String reqUserId, final String userId) {
        HashMap<Object,Object> friendObject = new HashMap<>();
        friendObject.put(REQUEST_USER_ID,reqUserId);
        friendObject.put(USER_ID,userId);
        friendObject.put(TYPE,"sent");
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUserId, FACEBOOK_FRIENDS, userId}).setValue(null);
        getNewFireBase(ANCHOR_SOCIALS, new String[]{userId, FACEBOOK_FRIENDS, reqUserId}).setValue(null);
        postFireBaseRequest(SEND_FRIEND_REQUEST_API,friendObject);
    }

    public SocialModel.RequestReceived[] getFriendRequestList(String reqUser) {
        HashMap<String, SocialModel.RequestReceived> friendsRequestMap = getSocialModel().requestReceived;
        String[] keys = friendsRequestMap.keySet().toArray(new String[friendsRequestMap.size()]);
        SocialModel.RequestReceived[] requests = new SocialModel.RequestReceived[keys.length];
        for (int i = 0; i < keys.length; i++) {
            requests[i] = friendsRequestMap.get(keys[i]);
        }
        return requests;
    }

    public HashMap<String, SocialModel.RequestReceived> getPendingFriendRequests(){
        return socialRequests;
    }

    public void acceptFriendRequest(final String reqUserId, final String userId) {
        HashMap<Object,Object> friendObject = new HashMap<>();
        friendObject.put(REQUEST_USER_ID,reqUserId);
        friendObject.put(USER_ID,userId);
        friendObject.put(TYPE,"accepted");
        getNewFireBase(ANCHOR_SOCIALS,null).child(reqUserId).child(REQUEST_RECEIVED).child(userId).removeValue();
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUserId, FACEBOOK_FRIENDS, userId}).removeValue();
        postFireBaseRequest(ACCEPT_FRIEND_REQUEST_API,friendObject);
    }

    public void ignoreFriendRequest(String reqUserId, String userId) {
        DatabaseReference socialFireBase = this.getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId()});
        socialFireBase.child(REQUEST_RECEIVED).child(userId).removeValue();
        socialFireBase.child(REQUEST_IGNORED).child(userId).setValue(1);
    }

    public void removeOrBlockFriend(@Nullable String roomId, String reqUserId, String userId, boolean blockAlso) {
        //STEP 1
        if (roomId == null)
            roomId = getSocialModel().friends.get(userId).roomId;

        //STEP 2
        DatabaseReference socialAnchorFireBase = this.getNewFireBase(ANCHOR_SOCIALS, null);
        socialAnchorFireBase.child(reqUserId).child(FRIENDS).child(userId).setValue(null);
        socialAnchorFireBase.child(userId).child(FRIENDS).child(reqUserId).setValue(null);
        //     2.3
        DatabaseReference userAnchorFireBase = this.getNewFireBase(ANCHOR_USERS, null);
        userAnchorFireBase.child(reqUserId).child(MESSAGE_ROOM).child(roomId).setValue(null);
        userAnchorFireBase.child(reqUserId).child(MOMENT_ROOM).child(roomId).setValue(null);
        //     2.4
        userAnchorFireBase.child(userId).child(MESSAGE_ROOM).child(roomId).setValue(null);
        userAnchorFireBase.child(userId).child(MOMENT_ROOM).child(roomId).setValue(null);
        //STEP 3
        DatabaseReference roomAnchorFireBase = this.getNewFireBase(ANCHOR_ROOMS, null);
        roomAnchorFireBase.child(roomId).setValue(null);
        //STEP 4

        if (!blockAlso) return;
        userAnchorFireBase.child(reqUserId).child(BLOCKED_FRIEND).child(userId).setValue(1);
    }

    /**
     * 10. Send Message To Friend
     * INPUT: reqUser, user, roomId, messageDetail
     * <p/>
     * STEP 1:
     * PUSH message Details rooms/<roomId>/messages
     * STEP 2:
     * STEP 2.1
     * IF msgCount is 0
     * INCREMENT rooms/<roomId>/members/<user._id>/msgCount
     * STEP 2.2
     * UPDATE rooms/<roomId>/members/<user._id>/type
     * UPDATE rooms/<roomId>/members/<reqUser._id>/type
     * STEP 3:
     * UPDATE users/<user._id>/messageRooms/<roomId>/updatedAt
     * UPDATE users/<user._id>/messageRooms/<roomId>/interacted as true
     * UPDATE users/<user._id>/messageRooms/<roomId>/deletionChecked as false
     * <p/>
     * STEP 4:
     * Set Priority of messageRooms with -ve (timestamp)
     * PATH is  : users/<userId>/messageRooms/<roomId>
     */
    public void sendMessageToFriend(final String reqUserId, final String userId, final String roomId, final RoomsModel.Messages message, final int messageType, String mediaId, String mediaUrl) {
        AppLibrary.log_d(TAG, "Sending Message To Friend-> " + userId);
        DatabaseReference roomFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES});
        DatabaseReference pushedMessageFB = roomFireBase.push();
        pushedMessageFB.setValue(message);
        pushedMessageFB.setPriority(getPriorityTimeStamp());
        this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId, MESSAGE_COUNT}).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue((Long) mutableData.getValue() + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }

        });
        //STEP 2.2
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId, MESSAGE_TYPE}).setValue(messageType);
        if (messageType == NEW_MEDIA) {
            getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, reqUserId, MESSAGE_TYPE}).setValue(SENT_MEDIA);
        } else {
            getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, reqUserId, MESSAGE_TYPE}).setValue(SENT_CHAT);
        }
        UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
        userRoom.type = FRIEND_ROOM;
        DatabaseReference receiverMessageReference = getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM, roomId});
        receiverMessageReference.setValue(userRoom);
        receiverMessageReference.setPriority(-getPriorityTimeStamp());
        DatabaseReference senderMessageReference = getNewFireBase(ANCHOR_USERS, new String[]{reqUserId, MESSAGE_ROOM, roomId});
        senderMessageReference.setValue(userRoom);
        senderMessageReference.setPriority(-getPriorityTimeStamp());
        NotificationContent content = new NotificationContent(reqUserId, getMyUserModel().name, userId, pushedMessageFB.getKey(), roomId, null, getMyUserModel().imageUrl, NEW_MESSAGE_REQUEST, null, null);
        sendNotification(content);
    }

    private void createMediaMessageForFriend(String roomId, RoomsModel.Messages message, String userId) {
        DatabaseReference roomFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES});
        DatabaseReference pushedMessageFB = roomFireBase.push();
        pushedMessageFB.setValue(message);
        pushedMessageFB.setPriority(getPriorityTimeStamp());
        this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId, MESSAGE_COUNT}).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue((Long) mutableData.getValue() + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }

        });
        UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
        userRoom.type = FRIEND_ROOM;
        DatabaseReference senderFirebase = getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId(), MESSAGE_ROOM, roomId});
        senderFirebase.setValue(userRoom);
        senderFirebase.setPriority(-getPriorityTimeStamp());
    }

    private void updateMediaMessageRoomAndSendNotificationForFriend(String reqUserId, String roomId, String userId, int messageType, String messageId, String mediaId, String mediaUrl) {
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId, MESSAGE_TYPE}).setValue(messageType);
        if (messageType == NEW_MEDIA) {
            getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, reqUserId, MESSAGE_TYPE}).setValue(SENT_MEDIA);
        } else {
            getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, reqUserId, MESSAGE_TYPE}).setValue(SENT_CHAT);
        }
        UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
        userRoom.type = FRIEND_ROOM;
        getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM, roomId}).setValue(userRoom);
        getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM, roomId}).setPriority(-getPriorityTimeStamp());
        NotificationContent content = new NotificationContent(reqUserId, getMyUserModel().name, userId, messageId, roomId, null, getMyUserModel().imageUrl, NEW_MESSAGE_REQUEST, mediaId, mediaUrl);
        sendNotification(content);
    }

    private void updateMediaMessageRoomAndSendNotificationForGroup(final String reqUserId, final String roomId, final int messageType, final String messageId, final String mediaId, final String mediaUrl) {
        DatabaseReference firebaseRooms = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId});
        firebaseRooms.keepSynced(true);
        firebaseRooms.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                HashMap<String, RoomsModel.Members> membersHashMap = roomsModel.members;
                String sendTo = null;
                if (membersHashMap != null) {
                    final String[] membersKey = membersHashMap.keySet().toArray(new String[membersHashMap.size()]);
                    for (int i = 0; i < membersKey.length; i++) {
                        if (!membersKey[i].equals(reqUserId)) {
                            sendTo += membersKey[i] + ",";
                            final DatabaseReference roomMemberFireBase = getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS});
                            final int finalI = i;
                            roomMemberFireBase.child(membersKey[i]).child(MESSAGE_COUNT).runTransaction(new Transaction.Handler() {
                                @Override
                                public Transaction.Result doTransaction(MutableData mutableData) {
                                    if (mutableData.getValue() == null) {
                                        mutableData.setValue(1);
                                    } else {
                                        mutableData.setValue((Long) mutableData.getValue() + 1);
                                    }
                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                }

                            });

                            roomMemberFireBase.child(membersKey[finalI]).child(ROOM_TYPE).setValue(messageType);
                            UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
                            userRoom.type = GROUP_ROOM;
                            DatabaseReference receiverFirebase = getNewFireBase(ANCHOR_USERS, new String[]{membersKey[finalI], MESSAGE_ROOM, roomId});
                            receiverFirebase.setValue(userRoom);
                            receiverFirebase.setPriority(-getPriorityTimeStamp());
                        }
                    }
                }
                NotificationContent content = new NotificationContent(reqUserId, getMyUserModel().name, sendTo, messageId, roomId, null, getMyUserModel().imageUrl, NEW_MESSAGE_REQUEST, mediaId, mediaUrl);
                sendNotification(content);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public interface SearchFriendListener {
        void onSearchResultChanged(String query, ArrayList<SearchResultModel> searchResultModels);
    }

    private ArrayList<SearchFriendListener> searchFriendListeners;

    public void addSearchFriendListener(SearchFriendListener searchFriendListener) {
        if (this.searchFriendListeners == null)
            this.searchFriendListeners = new ArrayList<>();
        this.searchFriendListeners.add(searchFriendListener);
    }

    public void removeSearchFriendListener(SearchFriendListener searchFriendListener) {
        this.searchFriendListeners.remove(searchFriendListener);
    }

    @SuppressWarnings("deprecation")
    public void hitSearchApi(String query) {
        if (!BaseFragment.isNetworkAvailable(mContext)) {
            Log.e(TAG, " not hitting api; no internet");
            return;
        }
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("name", query));
        pairs.add(new BasicNameValuePair("userId", this.getMyUserId()));

        RequestManager.makePostRequest(mContext, RequestManager.SEARCH_REQUEST, RequestManager.SEARCH_RESPONSE,
                null, pairs, searchResponseCallback);
    }

    private RequestManager.OnRequestFinishCallback searchResponseCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            try {
                final JSONObject object = (JSONObject) response;
                if (success && object != null) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        Log.d(TAG, "searchResponseCallback Success, response -" + object);
                        JSONArray searchResponseArray = object.getJSONObject("value").getJSONArray("users");

                        ArrayList<SearchResultModel> searchResultModels = new ArrayList<>();
                        for (int i = 0; i < searchResponseArray.length(); i++) {
                            JSONObject searchResponse = (JSONObject) searchResponseArray.get(i);
                            SearchResultModel result = new SearchResultModel(searchResponse.getString("_id"), searchResponse.getString("imageUrl"), searchResponse.getString("handle"), searchResponse.getString("name"), null);
                            searchResultModels.add(result);
                        }

                        if (searchFriendListeners != null) {
                            for (SearchFriendListener searchFriendListener : searchFriendListeners) {
                                searchFriendListener.onSearchResultChanged(object.getJSONObject("value").getString("query"), searchResultModels);
                            }
                        }

                    } else {
                        Log.e(TAG, "searchResponseCallback Error, response -" + object.getString("value"));
                    }
                } else {
                    Log.e(TAG, "searchResponseCallback Error");
                    // request failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "searchResponseCallback JsonException " + e);
            }

        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };

    public void createMediaMessageForGroup(String roomId, RoomsModel.Messages message) {
        final DatabaseReference pushedMessageFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES}).push();
        pushedMessageFireBase.setValue(message);
        pushedMessageFireBase.setPriority(getPriorityTimeStamp());
        UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
        userRoom.type = GROUP_ROOM;
        DatabaseReference senderFirebase = getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId(), MESSAGE_ROOM, roomId});
        senderFirebase.setValue(userRoom);
        senderFirebase.setPriority(-getPriorityTimeStamp());
    }

    public void sendMessageToGroup(final String reqUserId, final String roomId, RoomsModel.Messages messageDetail, final int messageType, final String mediaId, final String mediaUrl) {
        //STEP 1
        final DatabaseReference pushedMessageFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES}).push();
        pushedMessageFireBase.setValue(messageDetail);
        pushedMessageFireBase.setPriority(getPriorityTimeStamp());

        //STEP 2
        DatabaseReference firebaseRooms = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId});
        firebaseRooms.keepSynced(true);
        firebaseRooms.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                HashMap<String, RoomsModel.Members> membersHashMap = roomsModel.members;
                final String[] membersKey = membersHashMap.keySet().toArray(new String[membersHashMap.size()]);
                String sendTo = null;
                for (int i = 0; i < membersKey.length; i++) {
                    if (!membersKey[i].equals(reqUserId)) {
                        sendTo += membersKey[i] + ",";
                        final DatabaseReference roomMemberFireBase = getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS});
                        final int finalI = i;
                        roomMemberFireBase.child(membersKey[i]).child(MESSAGE_COUNT).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                if (mutableData.getValue() == null) {
                                    mutableData.setValue(1);
                                } else {
                                    mutableData.setValue((Long) mutableData.getValue() + 1);
                                }
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                            }

                        });

                        roomMemberFireBase.child(membersKey[finalI]).child(ROOM_TYPE).setValue(messageType);
                        UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
                        userRoom.type = GROUP_ROOM;
                        DatabaseReference receiverFireBase = getNewFireBase(ANCHOR_USERS, new String[]{membersKey[finalI], MESSAGE_ROOM, roomId});
                        receiverFireBase.setValue(userRoom);
                        receiverFireBase.setPriority(-getPriorityTimeStamp());
                    }
                }
                UserModel.Rooms userRoom = new UserModel.Rooms(getUpdatedAt(), true, false);
                userRoom.type = GROUP_ROOM;
                DatabaseReference senderFirebase = getNewFireBase(ANCHOR_USERS, new String[]{reqUserId, MESSAGE_ROOM, roomId});
                senderFirebase.setValue(userRoom);
                senderFirebase.setPriority(-getPriorityTimeStamp());
                NotificationContent content = new NotificationContent(reqUserId, getMyUserModel().name, sendTo, pushedMessageFireBase.getKey(), roomId, null, getMyUserModel().imageUrl, NEW_MESSAGE_REQUEST, mediaId, mediaUrl);
                sendNotification(content);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    /**
     * INPUT reqUser, roomId
     * STEP 1:
     * UPDATE rooms/<roomId>/members/<reqUser._id>/msgCount 0
     * STEP 2: Iterate over each message
     * STEP 2.1
     * -if reqUser != memberId and ( expiryType 6) and reqUser exists in viewers && rooms/<roomId>/type == FRIEND_ROOM
     * Remove message from room
     * -if roomType == GROUP_ROOM and ( expiryType 6)
     * Remove message from room
     * STEP 2.2
     * UPDATE viewers for message of type
     */
    public void roomOpen(final String reqUserId, final String roomId, final int roomType) {
        final DatabaseReference roomFireBase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId});
        roomFireBase.child(MEMBERS).child(reqUserId).child(MESSAGE_COUNT).setValue(0);
        roomFireBase.keepSynced(true);
        roomFireBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                if (roomsModel == null || roomsModel.members == null) {
                    Log.e(TAG, " openRoom failed : values null, returning");
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public DatabaseReference getFireBaseReference(String anchor, String[] children) {
        return this.getNewFireBase(anchor, children);
    }

    public void loadExistingGroups(String groupId) {
        SocialModel.Groups groups = mSocialModel.groups.get(groupId);
        this.getNewFireBase(ANCHOR_ROOMS, new String[]{groups.roomId}).keepSynced(true);
        this.getNewFireBase(ANCHOR_ROOMS, new String[]{groups.roomId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                roomDataLoadedListener.onGroupMemberLoaded(roomsModel.members);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void updateOnScreenShotTaken(@Nullable String userId, @Nullable String roomId, HashMap<String,String> mediaDetails) {
        if (userId != null && roomId != null){
            RoomsModel.Messages message = new RoomsModel.Messages(userId, mediaDetails.get(MEDIA_ID), NEW_TEXT,
                    SCREENSHOT_TEXT, System.currentTimeMillis(), null, EXPIRY_TYPE_24_HOURS, null);
        }
        updateScreenShotValue(mediaDetails);
//        sendMessageToFriend(getMyUserId(), userId, roomId, message, NEW_TEXT, null, null);
    }

    private void updateScreenShotValue(final HashMap<String,String> mediaDetails) {
        final String momentId = mediaDetails.get(MOMENT_ID);
        final String mediaId = mediaDetails.get(MEDIA_ID);
        if (momentId != null && mediaId != null) {
            if (LocalDataHelper.downloadStatusHashMap != null && LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                if (LocalDataHelper.downloadStatusHashMap.get(mediaId).status != MEDIA_VIEWED) {
                    LocalDataHelper.downloadStatusHashMap.get(mediaId).status = MEDIA_VIEWED;
                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE).child(mediaId).child(LOCAL_MEDIA_STATUS).setValue(MEDIA_VIEWED);
                }

                if (LocalDataHelper.downloadStatusHashMap.get(mediaId).screenShotted == null || !LocalDataHelper.downloadStatusHashMap.get(mediaId).screenShotted.containsKey(momentId)) {
                    if (LocalDataHelper.downloadStatusHashMap.get(mediaId).screenShotted == null)
                        LocalDataHelper.downloadStatusHashMap.get(mediaId).screenShotted = new HashMap<>();
                    LocalDataHelper.downloadStatusHashMap.get(mediaId).screenShotted.put(momentId,getCreatedAt());
                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE).child(mediaId).child(SCREENSHOTTED).child(momentId).setValue(getCreatedAt());
                    updateScreenShotDetails(momentId,mediaId);
                }
            }
        }
    }

    private void updateScreenShotDetails(String momentId,String mediaId) {
        HashMap<Object, Object> postObject = new HashMap<>();
        postObject.put(USER_ID, getMyUserId());
        postObject.put(NAME, getMyUserModel().name);
        postObject.put(MOMENT_ID, momentId);
        postObject.put(VIEWED_AT, getCreatedAt());
        postObject.put(MEDIA_ID, mediaId);
        postFireBaseRequest(UPDATE_FOR_SCREENSHOT, postObject);
    }

    public void updateOnFailedUpload(final Context context, String pendingMediaUploadId, final MediaModel mediaObject) {
        if (mediaObject.addedTo.moments != null && mediaObject.addedTo.moments.size() > 0) {
            if (!pendingMediaInMyMoments.containsKey(pendingMediaUploadId)) {
                pendingMediaInMyMoments.put(pendingMediaUploadId, mediaObject);
            }
        }
        HashMap<String, Integer> rooms = mediaObject.addedTo.rooms;
        if (rooms == null || rooms.size() <= 0)
            return;
        pendingMediaInMessageRooms.put(pendingMediaUploadId, mediaObject);
        for (final String room : rooms.keySet()) {
            mFireBase.child(ANCHOR_ROOMS).child(room).child(MESSAGES).keepSynced(true);
            mFireBase.child(ANCHOR_ROOMS).child(room).child(MESSAGES).orderByChild(MEDIA_ID).equalTo(pendingMediaUploadId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        RoomsModel.Messages messages = snapshot.getValue(RoomsModel.Messages.class);
                        if (messages.type == MESSAGE_TYPE_MEDIA) {
                            String messageId = snapshot.getKey();
                            DatabaseReference firebase = getNewFireBase(ANCHOR_ROOMS, new String[]{room, MESSAGES, messageId});
                            firebase.child(MEDIA_UPLOADING).setValue(MEDIA_UPLOADING_FAILED);
                            firebase.setPriority(getPriorityTimeStamp());
                            if (onUploadStatusChangesCallback != null)
                                onUploadStatusChangesCallback.onUploadingStatusModified(messageId, MEDIA_UPLOADING_FAILED);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    public void updateMediaMessageUploadStarting(final String roomId, String pendingMediaUploadId) {
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).keepSynced(true);
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).orderByChild(MEDIA_ID).equalTo(pendingMediaUploadId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    RoomsModel.Messages messages = snapshot.getValue(RoomsModel.Messages.class);
                    if (messages.type == MESSAGE_TYPE_MEDIA) {
                        String messageId = snapshot.getKey();
                        DatabaseReference firebase = getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES, messageId});
                        firebase.child(MEDIA_UPLOADING).setValue(MEDIA_UPLOADING_STARTED);
                        firebase.setPriority(getPriorityTimeStamp());
                        if (onUploadStatusChangesCallback != null)
                            onUploadStatusChangesCallback.onUploadingStatusModified(messageId, MEDIA_UPLOADING_STARTED);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void deleteMyMediaFromMyMoments(String mediaId) {
        if (mMoments != null && mMoments.containsKey(getMyUserModel().momentId) && mMoments.get(getMyUserModel().momentId).medias != null && mMoments.get(getMyUserModel().momentId).medias.containsKey(mediaId))
            mMoments.get(getMyUserModel().momentId).medias.remove(mediaId);
        if (getMyStreams().containsKey(mediaId))
            getMyStreams().remove(mediaId);
        if (pendingMediaInMyMoments.containsKey(mediaId))
            pendingMediaInMyMoments.remove(mediaId);
        removeMediaFromMoments(getMyUserModel().momentId,mediaId);
        if (onMyStreamsLoadedCallback != null)
            onMyStreamsLoadedCallback.onStreamsLoaded(myMedias, pendingMediaInMyMoments, pendingMediaInMessageRooms);
    }

    public interface RoomDataLoadedCallbacks {
        void onGroupMemberLoaded(HashMap<String, RoomsModel.Members> memberMap);
    }

    public void setOnGroupMembersLoadedListener(Fragment fragment) {
        this.roomDataLoadedListener = (RoomDataLoadedCallbacks) fragment;
    }

    public String createCustomFriendList(SettingsModel.CustomFriendListDetails customFriendDetails) {
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), CUSTOM_FRIEND_LIST}).push();
        firebase.setValue(customFriendDetails);
        return firebase.getKey();
    }

    public String updateIgnoredList(HashMap<String, SettingsModel.MemberDetails> ignoredList) {
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), IGNORED_LIST});
        firebase.setValue(ignoredList);
        return firebase.getKey();
    }

    private SettingsModel settingsModel;

    private void getCustomFriendList() {
        this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId()}).addValueEventListener(new ValueEventListener() {//keeping data fresh
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                settingsModel = dataSnapshot.getValue(SettingsModel.class);
                if (customFriendListCallback != null)
                    customFriendListCallback.onCustomFriendListLoaded(settingsModel);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void setCustomFriendListCallback(Fragment fragment) {
        this.customFriendListCallback = (CustomFriendListCallBack) fragment;
        customFriendListCallback.onCustomFriendListLoaded(settingsModel);
    }

    public void removeCustomFriendListCallback(Fragment fragment) {
        this.customFriendListCallback = null;

    }

    public interface CustomFriendListCallBack {
        void onCustomFriendListLoaded(SettingsModel settingsModel);
    }

    public String updateOnGroupCreated(HashMap<String, RoomsModel.Members> removeMembers,
                                       HashMap<String, RoomsModel.Members> addedMembers, String groupName, String roomId) {
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_ROOMS, null).push();
        long createdAt = System.currentTimeMillis();
        HashMap<String, RoomsModel.Members> adminMemberMap = new HashMap<>();
        RoomsModel.Members adminMember = new RoomsModel.Members(getMyUserId(), getMyUserModel().name, getMyUserModel().imageUrl, GROUP_CREATED);
        adminMemberMap.put(getMyUserId(), adminMember);
        RoomsModel.Detail details = new RoomsModel.Detail(getMyUserId(), groupName, null, null, createdAt);
        details.roomType = GROUP_ROOM;
        String[] memberArray = addedMembers.keySet().toArray(new String[addedMembers.size()]);
        HashMap<String, RoomsModel.PendingMemberDetails> pendingMemberMap = new HashMap<>();
        for (int i = 0; i < memberArray.length; i++) {
            if (!getMyUserId().equals(memberArray[i])) {
                RoomsModel.Members members = addedMembers.get(memberArray[i]);
                pendingMemberMap.put(memberArray[i], new RoomsModel.PendingMemberDetails(members.memberId, members.name, members.imageUrl, members.handle, members.momentId));
                // update pending group request
                SocialModel.PendingGroupRequest groupRequest = new SocialModel.PendingGroupRequest(groupName, null, getMyUserModel().name);
                DatabaseReference pendingGroupFireBase = this.getNewFireBase(ANCHOR_SOCIALS, new String[]{memberArray[i], PENDING_GROUP_REQUEST, firebase.getKey()});
                pendingGroupFireBase.setValue(groupRequest);
                pendingGroupFireBase.setPriority(getPriorityTimeStamp());
            }
        }
        RoomsModel roomModel = new RoomsModel(GROUP_ROOM, adminMemberMap, null, pendingMemberMap, details);
        firebase.setValue(roomModel);
        updateUserMessageRoomOnGroupCreated(firebase.getKey(), createdAt, groupName);
        if (removeMembers != null && removeMembers.size() > 0) {
            if (roomId != null)
                removeUsersFromGroup(removeMembers, roomId);
        }

        String sendTo = TextUtils.join(",", addedMembers.keySet().toArray());
        NotificationContent content = new NotificationContent(getMyUserId(), getMyUserModel().name, sendTo, null, firebase.getKey(), groupName, getMyUserModel().imageUrl, SEND_GROUP_REQUEST, null, null);
        sendNotification(content);
        return firebase.getKey();
    }

    private void removeUsersFromGroup(HashMap<String, RoomsModel.Members> removeList, final String roomId) {
        String[] keyArray = removeList.keySet().toArray(new String[removeList.size()]);
        for (int i = 0; i < keyArray.length; i++) {
            this.getNewFireBase(ANCHOR_USERS, new String[]{keyArray[i], MESSAGE_ROOM, roomId}).setValue(null);
            this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, keyArray[i]}).setValue(null);
            this.getNewFireBase(ANCHOR_SOCIALS, new String[]{keyArray[i], GROUPS, roomId}).setValue(null);
            RoomsModel.Messages messages = new RoomsModel.Messages(keyArray[i], null, 4, removeList.get(keyArray[i]).name + " left", System.currentTimeMillis(), null, VIEW_FOR_A_DAY, null);
            this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES}).push().setValue(messages);
        }

        final DatabaseReference firebase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId});
        firebase.keepSynced(true);
        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                HashMap<String, RoomsModel.Members> membersHashMap = roomsModel.members;
                String[] memberArray = membersHashMap.keySet().toArray(new String[membersHashMap.size()]);
                for (int i = 0; i < memberArray.length; i++) {
                    membersHashMap.get(memberArray[i]).type = ADMIN_REMOVED_MEMBER;
                    getNewFireBase(ANCHOR_USERS, new String[]{memberArray[i], MESSAGE_ROOM, roomId}).setPriority(getPriorityTimeStamp());
                }
                firebase.child(MEMBERS).setValue(membersHashMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

    }

    private void leaveGroupByUser(final String userId, final String roomId, final String userName) {
        this.getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM, roomId}).setValue(null);
        this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId}).setValue(null);
        this.getNewFireBase(ANCHOR_SOCIALS, new String[]{userId, GROUPS, roomId}).setValue(null);
        final DatabaseReference firebase = this.getNewFireBase(ANCHOR_ROOMS, new String[]{roomId});
        firebase.keepSynced(true);
        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                if (roomsModel.members.size() == 0) {
                    firebase.setValue(null);
                } else {
                    RoomsModel.Messages messages = new RoomsModel.Messages(userId, null, 4, userName + " left", System.currentTimeMillis(), null, VIEW_FOR_A_DAY, null);
                    getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES}).push().setValue(messages);

                    HashMap<String, RoomsModel.Members> membersHashMap = roomsModel.members;
                    String[] memberArray = membersHashMap.keySet().toArray(new String[membersHashMap.size()]);
                    for (int i = 0; i < memberArray.length; i++) {
                        if (i == 0 && userId.equals(roomsModel.detail.adminId) && !userId.equals(memberArray[i])) {
                            getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, DETAIL, ADMIN}).setValue(memberArray[i]);
                        }
                        membersHashMap.get(memberArray[i]).type = USER_LEFT_GROUP;
                        getNewFireBase(ANCHOR_USERS, new String[]{memberArray[i], MESSAGE_ROOM, roomId}).setPriority(getPriorityTimeStamp());
                    }
                    firebase.child(MEMBERS).setValue(membersHashMap);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void updateUserMessageRoomOnGroupCreated(String roomId, long createdAt, String groupName) {
        UserModel.Rooms rooms = new UserModel.Rooms(GROUP_ROOM, createdAt, false);
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId(), MESSAGE_ROOM, roomId});
        firebase.setValue(rooms);
        firebase.setPriority(getPriorityTimeStamp());
        updateUserSocialDataOnGroupCreation(roomId, groupName);
    }

    private void updateUserSocialDataOnGroupCreation(String roomId, String groupName) {
        SocialModel.Groups groups = new SocialModel.Groups(groupName, null, null, getMyUserId(), roomId);
        this.getNewFireBase(ANCHOR_SOCIALS, new String[]{getMyUserId(), GROUPS, roomId}).setValue(groups);
    }

    public void updateAppStatus() {
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId()});
        firebase.child("modifiedAt").setValue(System.currentTimeMillis());
        firebase.child("versionCode").setValue(BuildConfig.VERSION_CODE);
        firebase.child("versionName").setValue(BuildConfig.VERSION_NAME);
    }

    public void updateDeviceName() {
        DatabaseReference firebase = this.getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId()});
        firebase.child("deviceName").setValue(AppLibrary.getDeviceName());
    }


    public String getPendingMediaKey() {
        DatabaseReference localDataFireBase = getNewFireBase(MEDIA,null);
        final DatabaseReference pushedFireBase = localDataFireBase.push();
        return pushedFireBase.getKey();
    }

    public String updatePendingUploads(final MediaModel mediaModel,String pendingMediaId) {
        DatabaseReference localDataFireBase = getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), PENDING_MEDIA,pendingMediaId});

        String mediaPath = mediaModel.url;
        int p = mediaPath.lastIndexOf(".");
        String extension = mediaPath.substring(p + 1);
        if (p == -1 || !extension.matches("\\w+")) {
            /* file has no extension */
            AppLibrary.log_d(TAG, "MediaPath does not have an extension!");
        }

        File oldFile = new File(mediaPath);
        File newFile = new File(AppLibrary.getFilesDirectory(MasterClass.getGlobalContext()) + pendingMediaId + "." + extension);
        oldFile.renameTo(newFile);
        mediaModel.url = newFile.getAbsolutePath();
        if (mediaModel.addedTo != null && (mediaModel.addedTo.rooms != null || mediaModel.addedTo.moments != null))
            updateDownloadDetailsForMedia(pendingMediaId, mediaModel, MEDIA_DOWNLOAD_COMPLETE);
        if (mediaModel.addedTo != null && mediaModel.addedTo.rooms != null) {
            updateMessageRoomsOnPendingMedia(mediaModel, pendingMediaId);
        }
        localDataFireBase.setValue(mediaModel);
        localDataFireBase.setPriority(mediaModel.createdAt);
        return pendingMediaId;
    }

    public void updateOnCompletedUpload(final Context context, final String pendingMediaKey, final MediaModel mediaModel, final String finalUrl) {
        firebaseHelperHandler.post(new Runnable() {
            @Override
            public void run() {
                clearPendingMediasInList(pendingMediaKey);
                HashMap<String, Integer> momentsMap = mediaModel.addedTo.moments;
                mediaModel.uploadedAt = System.currentTimeMillis();

                if (momentsMap != null) {
                    String[] momentIds = momentsMap.keySet().toArray(new String[momentsMap.size()]);
                    for (int i = 0; i < momentIds.length; i++) {
                        if (momentsMap.get(momentIds[i]) == CUSTOM_MOMENT) {
                            updateCustomMoment(momentIds[i], pendingMediaKey, getFromSharedPreferences(context, AppLibrary.USER_LOGIN), mediaModel, finalUrl);
                        } else if (momentsMap.get(momentIds[i]) == MY_MOMENT) {

                        }
                    }
                }
                if (mediaModel.addedTo != null && mediaModel.addedTo.rooms != null && mediaModel.addedTo.rooms.size() > 0)
                    updateMessageRoomsOnUploadComplete(mediaModel, pendingMediaKey, finalUrl);

                getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), PENDING_MEDIA, pendingMediaKey}).setValue(null);

                if (mediaModel.addedTo != null && mediaModel.addedTo.moments != null && mediaModel.addedTo.moments.size() > 0) {
                    HashMap<Object, Object> postObject = new HashMap<>();
                    postObject.put(AppLibrary.MEDIA_ID, pendingMediaKey);
                    postObject.put(AppLibrary.USER_ID, getMyUserId());
                    HashMap<Object, Object> mediaData = new HashMap<>();
                    mediaData.put(MOMENTS, mediaModel.addedTo.moments);
                    mediaData.put(PRIVACY, mediaModel.privacy);
                    mediaData.put(CREATED_AT, mediaModel.createdAt);
                    mediaData.put(UPLOADED_AT, mediaModel.uploadedAt);
                    mediaData.put(DURATION, mediaModel.duration);
                    mediaData.put(URL, finalUrl);
                    mediaData.put(TYPE, mediaModel.type);
                    mediaData.put(ANONYMOUS, mediaModel.anonymous);
                    postObject.put(MEDIA_DATA, mediaData);
                    postFireBaseRequest(UPDATE_MEDIA_COMPLETE, postObject);
                }
                if (mediaModel.facebookPost) {
                    HashMap<Object, Object> facebookObject = new HashMap<>();
                    facebookObject.put(USER_ID, mFireBaseHelper.getMyUserId());
                    facebookObject.put(MEDIA_ID,pendingMediaKey);
                    facebookObject.put(URL,finalUrl);
                    facebookObject.put(MEDIA_TYPE,mediaModel.type);
                    if (mediaModel.mediaText != null)
                        facebookObject.put(MEDIA_TEXT,mediaModel.mediaText);
                    facebookObject.put(TYPE, 3);
                    facebookObject.put(TOKEN, AccessToken.getCurrentAccessToken().getToken());
                    mFireBaseHelper.postFireBaseRequest(UPDATE_FACEBOOK_REQUEST, facebookObject);
                }
            }
        });
    }

    private void clearPendingMediasInList(String pendingMediaKey) {
        if (pendingMediaInMyMoments != null && pendingMediaInMyMoments.size() > 0) {
            if (pendingMediaInMyMoments.containsKey(pendingMediaKey)) {
                pendingMediaInMyMoments.remove(pendingMediaKey);
                onMyStreamsLoadedCallback.onMediaInMomentUpdated(pendingMediaInMyMoments);
            }
        }
        if (pendingMediaInMessageRooms != null && pendingMediaInMessageRooms.size() > 0) {
            if (pendingMediaInMessageRooms.containsKey(pendingMediaKey)) {
                pendingMediaInMessageRooms.remove(pendingMediaKey);
                onMyStreamsLoadedCallback.onMediaInMessageRoomsUpdated(pendingMediaInMessageRooms);
            }
        }
    }

    private void updateMessageRoomsOnUploadComplete(final MediaModel mediaModel, final String mediaId, final String finalUrl) {
        HashMap<String, Integer> rooms = mediaModel.addedTo.rooms;
        final long createdAt = System.currentTimeMillis();
        for (final String room : rooms.keySet()) {
            this.getNewFireBase(ANCHOR_ROOMS, new String[]{room}).keepSynced(true);
            this.getNewFireBase(ANCHOR_ROOMS, new String[]{room}).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot roomDataSnapShot) {
                    roomDataSnapShot.child(MESSAGES).getRef().orderByChild(MEDIA_ID).equalTo(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                RoomsModel.Messages messages = snapshot.getValue(RoomsModel.Messages.class);
                                if (messages.type == MESSAGE_TYPE_MEDIA) {
                                    String messageId = snapshot.getKey();
                                    DatabaseReference firebase = getNewFireBase(ANCHOR_ROOMS, new String[]{room, MESSAGES, messageId});
                                    messages.mediaUploadingStatus = MEDIA_UPLOADING_COMPLETE;
                                    messages.createdAt = createdAt;
                                    if (AppLibrary.getMediaType(mediaModel.url) == AppLibrary.MEDIA_TYPE_VIDEO) {
                                        messages.isVideo = true;
                                    } else {
                                        messages.isVideo = false;
                                    }
                                    firebase.setValue(messages);
                                    firebase.setPriority(getPriorityTimeStamp());
                                    RoomsModel roomsModel = roomDataSnapShot.getValue(RoomsModel.class);
                                    if (roomsModel.type == FRIEND_ROOM) {
                                        for (String user : roomsModel.members.keySet()) {
                                            if (!user.equals(getMyUserId())) {
                                                updateMediaMessageRoomAndSendNotificationForFriend(getMyUserId(), room, user, NEW_MEDIA, messageId, mediaId, finalUrl);
                                            }
                                        }
                                    } else {
                                        updateMediaMessageRoomAndSendNotificationForGroup(getMyUserId(), room, NEW_MEDIA, messageId, mediaId, finalUrl);
                                    }
                                    if (onUploadStatusChangesCallback != null)
                                        onUploadStatusChangesCallback.onUploadingStatusModified(messageId, MEDIA_UPLOADING_COMPLETE);
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    private void fetchContributedMedias(){
        this.getNewFireBase(ANCHOR_LOCAL_DATA,new String[]{getMyUserId(),MY_CONTRIBUTIONS}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot.getValue() != null){
                            // set the contributed details in localdatahelper
                            LocalDataHelper localDataHelper = LocalDataHelper.getInstance(mFireBaseHelper, LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD);
                            localDataHelper.setContributedDetailsSnapshot(dataSnapshot);
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                                final String momentId = snapshot.getKey();
                                String momentName = (String) snapshot.child(MOMENT_NAME).getValue();
                                String momentImage = (String) snapshot.child(MOMENT_IMAGE).getValue();
                                CustomMomentModel momentModel = new CustomMomentModel();
                                momentModel.momentId = momentId;
                                momentModel.name = momentName;
                                momentModel.thumbnailUrl = momentImage;
                                contributedMoments.put(momentId,momentModel);
                                for (final DataSnapshot media : snapshot.child(MEDIA).getChildren()){
                                    final String mediaId = media.getKey();
                                    getNewFireBase(ANCHOR_MOMENTS,new String[]{momentId,MEDIA,mediaId}).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(final DataSnapshot dataSnapshot) {
                                            firebaseHelperHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    onContributedStateUpdated(dataSnapshot,momentId,mediaId);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }

                                    });
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void onContributedStateUpdated(DataSnapshot dataSnapshot, final String momentId, final String mediaId){
        if (dataSnapshot.getValue() != null) {
            processNonExpiredContributedMedia(momentId,dataSnapshot, MEDIA_ACTIVE);
        } else {
            getNewFireBase(ANCHOR_MEDIA, new String[]{mediaId}).keepSynced(true);
            getNewFireBase(ANCHOR_MEDIA, new String[]{mediaId}).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot mediaSnapshot) {
                    firebaseHelperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!isExpiredMedia(mediaSnapshot)) {
                                final MediaModel mediaModel = mediaSnapshot.getValue(MediaModel.class);
                                final LinkedHashMap<String, MediaModelView> mediaList;
                                if (!contributedMomentsMediaDetails.containsKey(momentId)) {
                                    mediaList = new LinkedHashMap<>();
                                    contributedMomentsMediaDetails.put(momentId, mediaList);
                                } else
                                    mediaList = contributedMomentsMediaDetails.get(momentId);

                                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(final DataSnapshot dataSnapshot) {
                                        firebaseHelperHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!isExpiredMedia(mediaSnapshot)) {
                                                    String localUrl = (String) dataSnapshot.child(URL).getValue();
                                                    MediaModelView mediaModelView = new MediaModelView();
                                                    mediaModelView.url = localUrl;
                                                    mediaModelView.mediaText = mediaModel.mediaText;
                                                    mediaModelView.mediaState = MEDIA_INACTIVE;
                                                    mediaModelView.createdAt = mediaModel.createdAt;
                                                    mediaModelView.duration = mediaModel.duration;
                                                    mediaModelView.mediaId = mediaId;
                                                    if (fileExists(localUrl)) {
                                                        mediaModelView.status = MEDIA_UPLOADING_COMPLETE;
                                                    } else {
                                                        // download valid medias in my moment
                                                        mediaModelView.status = DOWNLOADING_MY_MOMENT_MEDIA;
                                                        mediaModelView.url = mediaModel.url;
                                                        MediaDownloader mediaDownloader = new MediaDownloader(mContext);
                                                        mediaDownloader.startDownload(mediaId, mediaModel.url, new OnMyMomentMediaDownloadCallback() {
                                                            @Override
                                                            public void onDownloadCallback(String mediaId, int status, String mediaPath) {
                                                                if (mediaPath != null) {
                                                                    mediaList.get(mediaId).url = mediaPath;

                                                                    if (onMyMomentMediaDownloadStatusModified != null)
                                                                        onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(momentId, mediaId, status);
                                                                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(SOURCE).setValue(ANDROID);
                                                                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CONTRIBUTED_MOMENTS).setValue(mediaModel.addedTo.moments);
                                                                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CREATED_AT).setValue(mediaModel.createdAt);
                                                                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaPath);
                                                                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                                            if (dataSnapshot.getValue() == null || dataSnapshot.getValue(Integer.class) != MEDIA_VIEWED) {
                                                                                dataSnapshot.getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(DatabaseError databaseError) {

                                                                        }

                                                                    });

                                                                    // update Local data map
                                                                    if (LocalDataHelper.downloadStatusHashMap != null) {
                                                                        if (LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                                                                            MediaDownloadDetails downloadDetails = LocalDataHelper.downloadStatusHashMap.get(mediaId);
                                                                            if (downloadDetails.status < MEDIA_DOWNLOAD_COMPLETE) {
                                                                                downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                            }
                                                                            downloadDetails.url = mediaPath;
                                                                        } else {
                                                                            MediaDownloadDetails downloadDetails = new MediaDownloadDetails();
                                                                            downloadDetails.url = mediaPath;
                                                                            downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                            downloadDetails.createdAt = mediaList.get(mediaId).createdAt;
                                                                            LocalDataHelper.downloadStatusHashMap.put(mediaId, downloadDetails);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }
                                                    mediaList.put(mediaId, mediaModelView);
                                                    if (onMyStreamsLoadedCallback != null)
                                                        onMyStreamsLoadedCallback.onContributedStreamsLoaded(momentId, mediaId, contributedMoments, contributedMomentsMediaDetails);
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }

                                });
                            }

                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    private void processNonExpiredContributedMedia(final String momentId, DataSnapshot dataSnapshot, final int state) {
        final LinkedHashMap<String,MediaModelView> mediaList;
        if (!contributedMomentsMediaDetails.containsKey(momentId)) {
            mediaList = new LinkedHashMap<>();
            contributedMomentsMediaDetails.put(momentId,mediaList);
        } else
            mediaList = contributedMomentsMediaDetails.get(momentId);
        final MomentModel.Media media = dataSnapshot.getValue(MomentModel.Media.class);
        media.mediaId = dataSnapshot.getKey();
        boolean isExpired = isExpiredMedia(dataSnapshot);
        if (!isExpired){
            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(media.mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    firebaseHelperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mFireBase.child(VIEW_DETAILS).child(momentId).child(media.mediaId).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot snapshot) {
                                    firebaseHelperHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            final String localUrl = (String) dataSnapshot.child(URL).getValue();
                                            final MediaModelView mediaModelView = new MediaModelView(localUrl, media.createdAt, media.type, media.totalViews, media.viewerDetails,
                                                    media.screenShots, media.createdAtText, media.momentId, media.mediaId, MEDIA_UPLOADING_COMPLETE, media.mediaText, media.webViews);
                                            HashMap<String, ViewerDetails> newViewerDetails = new HashMap<>();
                                            if (snapshot.getValue() != null) {
                                                GenericTypeIndicator<HashMap<String, ViewerDetails>> t = new GenericTypeIndicator<HashMap<String, ViewerDetails>>() {};
                                                HashMap<String, ViewerDetails> viewers = snapshot.getValue(t);
                                                newViewerDetails.putAll(viewers);
                                            }
                                            if (mediaModelView.viewerDetails != null && mediaModelView.viewerDetails.size() > 0)
                                                newViewerDetails.putAll(mediaModelView.viewerDetails);
                                            mediaModelView.viewerDetails = newViewerDetails;

                                            mediaModelView.mediaState = state;
                                            mediaModelView.duration = media.duration;

                                            if (fileExists(localUrl)) {
                                                mediaModelView.status = MEDIA_UPLOADING_COMPLETE;
                                            } else {
                                                // download valid medias in my moment
                                                mediaModelView.status = DOWNLOADING_MY_MOMENT_MEDIA;
                                                mediaModelView.url = media.url;
                                                MediaDownloader mediaDownloader = new MediaDownloader(mContext);
                                                mediaDownloader.startDownload(media.mediaId, media.url, new OnMyMomentMediaDownloadCallback() {
                                                    @Override
                                                    public void onDownloadCallback(String mediaId, int status, String mediaPath) {
                                                        if (mediaPath != null) {
                                                            mediaList.get(mediaId).url = mediaPath;

                                                            // pass status to my moment slider fragment
                                                            if (onMyMomentMediaDownloadStatusModified != null)
                                                                onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(momentId,mediaId, status);
                                                            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(SOURCE).setValue(ANDROID);
                                                            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CREATED_AT).setValue(media.createdAt);
                                                            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaPath);
                                                            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    if (dataSnapshot.getValue() == null || dataSnapshot.getValue(Integer.class) != MEDIA_VIEWED) {
                                                                        dataSnapshot.getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {

                                                                }

                                                            });

                                                            // update Local data map
                                                            if (LocalDataHelper.downloadStatusHashMap != null) {
                                                                if (LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                                                                    MediaDownloadDetails downloadDetails = LocalDataHelper.downloadStatusHashMap.get(mediaId);
                                                                    if (downloadDetails.status < MEDIA_DOWNLOAD_COMPLETE) {
                                                                        downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                    }
                                                                    downloadDetails.url = mediaPath;
                                                                } else {
                                                                    MediaDownloadDetails downloadDetails = new MediaDownloadDetails();
                                                                    downloadDetails.url = mediaPath;
                                                                    downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                    downloadDetails.createdAt = mediaList.get(mediaId).createdAt;
                                                                    LocalDataHelper.downloadStatusHashMap.put(mediaId,downloadDetails);
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                            }
                                            mediaList.put(media.mediaId, mediaModelView);
                                            if (onMyStreamsLoadedCallback != null)
                                                onMyStreamsLoadedCallback.onContributedStreamsLoaded(momentId,media.mediaId,contributedMoments, contributedMomentsMediaDetails);
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    public interface onUploadStatusChanges {
        void onUploadingStatusModified(String messageId, int status);
    }

    public void setOnUploadStatusChangesCallback(Fragment fragment) {
        onUploadStatusChangesCallback = (onUploadStatusChanges) fragment;
    }

    private void updateMessageRoomsOnPendingMedia(final MediaModel mediaModel, final String mediaId) {
        HashMap<String, Integer> rooms = mediaModel.addedTo.rooms;
        for (final String room : rooms.keySet()) {
            if (rooms.get(room) == FRIEND_ROOM) {
                this.getNewFireBase(ANCHOR_ROOMS, new String[]{room}).keepSynced(true);
                this.getNewFireBase(ANCHOR_ROOMS, new String[]{room}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                        for (String user : roomsModel.members.keySet()) {
                            if (!user.equals(getMyUserId())) {
                                RoomsModel.Messages messages;
                                messages = new RoomsModel.Messages(getMyUserId(), mediaId, MESSAGE_TYPE_MEDIA, null, mediaModel.createdAt, null, mediaModel.expiryType, null);
                                if (messages.viewers == null)
                                    messages.viewers = new HashMap<String, Integer>();
                                messages.viewers.put(getMyUserId(), 1);
                                messages.mediaUploadingStatus = MEDIA_UPLOADING_STARTED;
                                createMediaMessageForFriend(room, messages, user);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
            } else if (rooms.get(room) == GROUP_ROOM) {
                RoomsModel.Messages messages;
                messages = new RoomsModel.Messages(getMyUserId(), mediaId, MESSAGE_TYPE_MEDIA, null, mediaModel.createdAt, null, VIEW_FOR_A_DAY, null);
                if (messages.viewers == null)
                    messages.viewers = new HashMap<String, Integer>();
                messages.viewers.put(getMyUserId(), 1);
                messages.mediaUploadingStatus = MEDIA_UPLOADING_STARTED;
                createMediaMessageForGroup(room, messages);
            }
        }
    }

    private void updateCustomMoment(final String momentId, final String pendingMediaId, String myUserId, final MediaModel mediaModel, final String finalUrl) {
        if (mediaModel.momentDetails != null && mediaModel.momentDetails.get(momentId) != null) {
            DatabaseReference firebase = getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId, MEDIA, pendingMediaId});
            updateContributionToLocalData(mediaModel.momentDetails.get(momentId).name,mediaModel.momentDetails.get(momentId).thumbnailUrl,momentId,pendingMediaId, false);
            firebase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    onContributedStateUpdated(dataSnapshot,momentId,pendingMediaId);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        } else {
            getNewFireBase(ANCHOR_MOMENTS,new String[]{momentId}).keepSynced(true);
            getNewFireBase(ANCHOR_MOMENTS,new String[]{momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    MomentModel momentModel = dataSnapshot.getValue(MomentModel.class);

                    DatabaseReference firebase = getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId, MEDIA, pendingMediaId});
                    updateContributionToLocalData(momentModel.name,momentModel.thumbnailUrl,momentId,pendingMediaId, false);
                    firebase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            onContributedStateUpdated(dataSnapshot,momentId,pendingMediaId);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    private void updateContributionToLocalData(String name,String thumbnailUrl,String momentId,String pendingMediaId,boolean isAutoModerate){
        this.getNewFireBase(ANCHOR_LOCAL_DATA,new String[]{getMyUserId(),MY_CONTRIBUTIONS,momentId,MOMENT_NAME}).setValue(name);
        this.getNewFireBase(ANCHOR_LOCAL_DATA,new String[]{getMyUserId(),MY_CONTRIBUTIONS,momentId,MOMENT_IMAGE}).setValue(thumbnailUrl);
        this.getNewFireBase(ANCHOR_LOCAL_DATA,new String[]{getMyUserId(),MY_CONTRIBUTIONS,momentId,MEDIA,pendingMediaId}).setValue(1);
        CustomMomentModel customMomentModel = new CustomMomentModel();
        customMomentModel.name = name;
        customMomentModel.thumbnailUrl = thumbnailUrl;
        customMomentModel.momentId = momentId;
        customMomentModel.autoModerate = isAutoModerate;
        contributedMoments.put(momentId,customMomentModel);
    }

    private void updateMediaDataToMoments(Context context, MediaModel mediaModel, String mediaId, String finalUrl) {
        String momentId = getMyUserModel().momentId;
        if (AppLibrary.checkStringObject(momentId) != null) {
            MomentModel.Media media = new MomentModel.Media(finalUrl, mediaModel.createdAt, mediaModel.type, 0, null, 0, null, mediaModel.mediaText);
            media.privacy = new MomentModel.Privacy(mediaModel.privacy.type, mediaModel.privacy.value);
            media.duration = mediaModel.duration;
            media.userId = mediaModel.userId;
            media.userDetail = mediaModel.userDetail;
            media.uploadedAt = mediaModel.uploadedAt;
            DatabaseReference firebase = this.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId, ANCHOR_MEDIA, mediaId});
            firebase.setValue(media,media.createdAt);
        }
    }

    public void fetchPendingUploadsForMedia(String mediaId) {
        DatabaseReference pendingDataFireBase = this.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), PENDING_MEDIA, mediaId});
        pendingDataFireBase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MediaModel model = dataSnapshot.getValue(MediaModel.class);
                if (model!=null)
                    mediaModelCallback.onPendingMediaListLoaded(model, dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void fetchPendingUploadsForMyMoments() {
        for (Map.Entry<String, MediaModel> entry : pendingMediaInMyMoments.entrySet()) {
            mediaModelCallback.onPendingMediaListLoaded(entry.getValue(), entry.getKey());
        }
    }

    public void fetchPendingUploadsForRoom(final String roomId) {
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).keepSynced(true);
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).orderByChild(MEDIA_UPLOADING).endAt(MEDIA_UPLOADING_STARTED).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    final RoomsModel.Messages messages = snapshot.getValue(RoomsModel.Messages.class);
                    String mediaId = messages.mediaId;
                    if (mediaId != null) {
                        getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), PENDING_MEDIA, mediaId}).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                MediaModel model = dataSnapshot.getValue(MediaModel.class);
                                if (model != null)
                                    mediaModelCallback.onPendingMediaForMessageLoaded(model, dataSnapshot.getKey(), roomId, messages.messageId);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }

                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void fetchPendingUploadsForAllRoom() {
        for (Map.Entry<String, MediaModel> entry : pendingMediaInMessageRooms.entrySet()) {
            mediaModelCallback.onPendingMediaListLoaded(entry.getValue(), entry.getKey());
        }
    }

    public interface MediaModelCallback {
        void onPendingMediaListLoaded(MediaModel pendingList, String key);

        void onPendingMediaForMessageLoaded(MediaModel pendingList, String key, String roomId, String messageId);
    }

    public void setMediaModelCallback(Context context) {
        mediaModelCallback = (MediaModelCallback) context;
    }

    public Map<String, MomentModel> modelMap;

    private MomentsDataListener momentsDataListener;

    public interface MomentsDataListener {
        void onUnseenDataChanged(ArrayList<HomeMomentViewModel> unseenFriendMomentList);

        void onSeenDataChanged(ArrayList<HomeMomentViewModel> rooms);

        void onFavouritesDataChanged(ArrayList<HomeMomentViewModel> favouritesMomentsList);

        void onUnseenItemStatusChanged(String momentId);

        void onSeenItemStatusChanges(String momentId, int position);
    }

    private String getFromSharedPreferences(Context context, String key) {
        return context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0).getString(key, "");
    }

    public void setMomentsDataListener(MomentsDataListener momentsDataListener) {
        this.momentsDataListener = momentsDataListener;
        momentsDataListener.onSeenDataChanged(getSeenMomentRoomView());
        momentsDataListener.onUnseenDataChanged(getMomentRoomView());
        momentsDataListener.onFavouritesDataChanged(null);
    }


    /**
     * @param momentRoom the particular momentRoom
     * @return true if the user has checkedOut the moment already false otherwise
     */
    private boolean getMomentSeenStatus(RoomsModel momentRoom) {
        final HashMap<String, RoomsModel.Members> members = momentRoom.members;
        for (Map.Entry<String, RoomsModel.Members> membersEntry : members.entrySet()) {
            if (membersEntry.getKey().equals(getMyUserId())) {
                return membersEntry.getValue().momentCount == 0;
            }
        }
        throw new RuntimeException("getMomentSeenStatus: userNot found in members");
    }

    private SliderMessageListListener mSliderMessageListListener;

    public void setSliderMessageListListener(SliderMessageListListener sliderMessageListListener) {
        this.mSliderMessageListListener = sliderMessageListListener;
    }

    public void updateLocalPath(String momentId, String mediaId, String path) {
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(path);
        mMoments.get(momentId).medias.get(mediaId).url = path;
    }

    public void onMomentMediaDownloadFailed(String momentId) {
        mMoments.get(momentId).momentStatus = UNSEEN_MOMENT;
        if (momentsDataListener != null)
            momentsDataListener.onUnseenDataChanged(getMomentRoomView());
        if (momentsDataListener != null)
            momentsDataListener.onSeenDataChanged(getSeenMomentRoomView());
    }

    private void feedMediaToMyStreams(final MomentModel.Media media) {
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(media.mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFireBase.child(VIEW_DETAILS).child(getMyUserModel().momentId).child(media.mediaId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(final DataSnapshot snapshot) {
                                firebaseHelperHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String localUrl = (String) dataSnapshot.child(URL).getValue();
                                        final MediaModelView mediaModelView = new MediaModelView(localUrl, media.createdAt, media.type, media.totalViews, media.viewerDetails,
                                                media.screenShots, media.createdAtText, media.momentId, media.mediaId, MEDIA_UPLOADING_COMPLETE, media.mediaText,media.webViews);
                                        HashMap<String,ViewerDetails> newViewerDetails = new HashMap<>();
                                        if (snapshot.getValue() != null) {
                                            GenericTypeIndicator<HashMap<String,ViewerDetails>> t = new GenericTypeIndicator<HashMap<String, ViewerDetails>>(){};
                                            HashMap<String,ViewerDetails> viewers = snapshot.getValue(t);
                                            newViewerDetails.putAll(viewers);
                                        }
                                        if (mediaModelView.viewerDetails != null && mediaModelView.viewerDetails.size() > 0)
                                            newViewerDetails.putAll(mediaModelView.viewerDetails);
                                        mediaModelView.viewerDetails = newViewerDetails;
                                        mediaModelView.mediaState = 0;
                                        mediaModelView.duration = media.duration;
                                        if (localUrl != null && fileExists(localUrl)) {
                                            mediaModelView.status = MEDIA_UPLOADING_COMPLETE;
                                        } else {
                                            // download valid medias in my moment
                                            mediaModelView.status = DOWNLOADING_MY_MOMENT_MEDIA;
                                            mediaModelView.url = media.url;
                                            MediaDownloader mediaDownloader = new MediaDownloader(mContext);
                                            mediaDownloader.startDownload(media.mediaId, media.url, new OnMyMomentMediaDownloadCallback() {
                                                @Override
                                                public void onDownloadCallback(String mediaId, int status, String mediaPath) {
                                                    if (mediaPath != null) {
                                                        myMedias.get(mediaId).url = mediaPath;

                                                        // pass status to my moment slider fragment
                                                        if (onMyMomentMediaDownloadStatusModified != null)
                                                            onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(getMyUserModel().momentId,mediaId, status);
                                                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(SOURCE).setValue(ANDROID);
                                                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CREATED_AT).setValue(media.createdAt);
                                                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaPath);
                                                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                if (dataSnapshot.getValue() == null || dataSnapshot.getValue(Integer.class) != MEDIA_VIEWED) {
                                                                    dataSnapshot.getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }

                                                        });

                                                        // update Local data map
                                                        if (LocalDataHelper.downloadStatusHashMap != null) {
                                                            if (LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                                                                MediaDownloadDetails downloadDetails = LocalDataHelper.downloadStatusHashMap.get(mediaId);
                                                                if (downloadDetails.status < MEDIA_DOWNLOAD_COMPLETE) {
                                                                    downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                }
                                                                downloadDetails.url = mediaPath;
                                                            } else {
                                                                MediaDownloadDetails downloadDetails = new MediaDownloadDetails();
                                                                downloadDetails.url = mediaPath;
                                                                downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                                                downloadDetails.createdAt = myMedias.get(mediaId).createdAt;
                                                                LocalDataHelper.downloadStatusHashMap.put(mediaId,downloadDetails);
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                        myMedias.put(media.mediaId, mediaModelView);
                                        if (onMyStreamsLoadedCallback != null)
                                            onMyStreamsLoadedCallback.onStreamsLoaded(myMedias, pendingMediaInMyMoments, pendingMediaInMessageRooms);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void fetchMyPendingStreams() {
        // fetch pending streams
        getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), PENDING_MEDIA}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (DataSnapshot pendingMedia : dataSnapshot.getChildren()) {
                            MediaModel mediaModel = pendingMedia.getValue(MediaModel.class);
                            if (!fileExists(mediaModel.url) || isExpiredMedia(pendingMedia)) {

                            } else {
                                mediaModel.mediaId = pendingMedia.getKey();
                                if (mediaModel.addedTo.moments != null && !mediaModel.addedTo.moments.isEmpty()) {
                                    pendingMediaInMyMoments.put(mediaModel.mediaId, mediaModel);
                                    HashMap<String,Integer> momentList = mediaModel.addedTo.moments;
                                    for (Map.Entry<String,Integer> momentEntry : momentList.entrySet()){
                                        if (momentEntry.getKey().equals(getMyUserModel().momentId)){
                                            MediaModelView mediaModelView = new MediaModelView(mediaModel.url, mediaModel.createdAt, mediaModel.type, mediaModel.totalViews,
                                                    mediaModel.viewerDetails, 0, null, mFireBaseHelper.getMyUserModel().momentId, pendingMedia.getKey(), MEDIA_UPLOADING_FAILED, mediaModel.mediaText,0);
                                            mediaModelView.mediaState = 0;
                                            mediaModelView.duration = mediaModel.duration;
                                            myMedias.put(mediaModel.mediaId, mediaModelView);
                                        } else {
                                            MediaModelView mediaModelView = new MediaModelView(mediaModel.url, mediaModel.createdAt, mediaModel.type, mediaModel.totalViews,
                                                    mediaModel.viewerDetails, 0, null, mFireBaseHelper.getMyUserModel().momentId, pendingMedia.getKey(), MEDIA_UPLOADING_FAILED, mediaModel.mediaText,0);
                                            mediaModelView.mediaState = 0;
                                            mediaModelView.duration = mediaModel.duration;
                                            final LinkedHashMap<String, MediaModelView> mediaList;
                                            if (contributedMomentsMediaDetails.containsKey(momentEntry.getKey()) && contributedMomentsMediaDetails.get(momentEntry.getKey()) != null) {
                                                mediaList = contributedMomentsMediaDetails.get(momentEntry.getKey());
                                                if (!mediaList.containsKey(mediaModel.mediaId)){
                                                    mediaList.put(mediaModel.mediaId,mediaModelView);
                                                }
                                            } else {
                                                mediaList = new LinkedHashMap<>();
                                                mediaList.put(mediaModel.mediaId,mediaModelView);
                                                contributedMomentsMediaDetails.put(momentEntry.getKey(), mediaList);
                                            }
                                        }
                                    }
                                }
                                if (mediaModel.addedTo.rooms != null && !mediaModel.addedTo.rooms.isEmpty()) {
                                    pendingMediaInMessageRooms.put(mediaModel.mediaId, mediaModel);
                                }
                            }
                        }

                        if (onMyStreamsLoadedCallback != null)
                            onMyStreamsLoadedCallback.onStreamsLoaded(myMedias, pendingMediaInMyMoments, pendingMediaInMessageRooms);
                        fetchContributedMedias();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void onRetryMyMomentMediaDownload(final String momentId, String mediaId) {
        AppLibrary.log_d(TAG,"Retrying media "+mediaId+" for the moment "+momentId);
        if (myMedias != null && myMedias.containsKey(mediaId)) {
            AppLibrary.log_d(TAG,"Media list contains, Retrying now, MediaUrl is "+myMedias.get(mediaId).url);
            MediaDownloader mediaDownloader = new MediaDownloader(mContext);
            if (onMyMomentMediaDownloadStatusModified != null)
                onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(momentId,mediaId, DOWNLOADING_MY_MOMENT_MEDIA);

            if (!myMedias.get(mediaId).url.contains("https:") && !myMedias.get(mediaId).url.contains("http:")) { //Safety check if file has already been downloaded
                int status = DOWNLOADED_MY_MOMENT_MEDIA;
                String mediaPath = myMedias.get(mediaId).url;
                File file = new File(mediaPath);

                if (file!=null && file.exists()) {} else return;

                // pass status to my moment slider fragment
                if (onMyMomentMediaDownloadStatusModified != null)
                    onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(momentId,mediaId, status);

                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaPath);
                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null || dataSnapshot.getValue(Integer.class) != MEDIA_VIEWED) {
                            dataSnapshot.getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
                return;
            }

            mediaDownloader.startDownload(mediaId, myMedias.get(mediaId).url, new OnMyMomentMediaDownloadCallback() {
                @Override
                public void onDownloadCallback(String mediaId, int status, String mediaPath) {
                    if (mediaPath != null) {
                        myMedias.get(mediaId).url = mediaPath;

                        // pass status to my moment slider fragment
                        if (onMyMomentMediaDownloadStatusModified != null)
                            onMyMomentMediaDownloadStatusModified.onDownloadStatusChanges(momentId,mediaId, status);

                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaPath);
                        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() == null || dataSnapshot.getValue(Integer.class) != MEDIA_VIEWED) {
                                    dataSnapshot.getRef().setValue(MEDIA_DOWNLOAD_COMPLETE);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }

                        });

                        // update Local data map
                        if (LocalDataHelper.downloadStatusHashMap != null) {
                            if (LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                                MediaDownloadDetails downloadDetails = LocalDataHelper.downloadStatusHashMap.get(mediaId);
                                if (downloadDetails.status < MEDIA_DOWNLOAD_COMPLETE) {
                                    downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                }
                                downloadDetails.url = mediaPath;
                            } else {
                                MediaDownloadDetails downloadDetails = new MediaDownloadDetails();
                                downloadDetails.url = mediaPath;
                                downloadDetails.status = MEDIA_DOWNLOAD_COMPLETE;
                                downloadDetails.createdAt = myMedias.get(mediaId).createdAt;
                                LocalDataHelper.downloadStatusHashMap.put(mediaId,downloadDetails);
                            }
                        }
                    }
                }
            });
        }
    }

    public interface onMyMomentMediaDownloadStatusModified {
        void onDownloadStatusChanges(String momentId,String mediaId, int status);
    }

    public void setOnMyMomentMediaDownloadStatusModified(Fragment fragment) {
        onMyMomentMediaDownloadStatusModified = (onMyMomentMediaDownloadStatusModified) fragment;
    }

    public interface OnMyMomentMediaDownloadCallback {
        void onDownloadCallback(String mediaId, int status, String mediaPath);
    }

    public LinkedHashMap<String, MediaModelView> getMyStreams() {
        return myMedias;
    }

    public LinkedHashMap<String,LinkedHashMap<String,MediaModelView>> getContributedStreams(){
        return contributedMomentsMediaDetails;
    }

    public LinkedHashMap<String, MediaModel> getPendingMediaInMyMoment(){
        return pendingMediaInMyMoments;
    }

    public HashMap<String, MediaModelView> getMyDownloadedStreams() {
        LinkedHashMap<String, MediaModelView> downloadedMedias = new LinkedHashMap<>();
        if (myMedias != null && myMedias.size() > 0) {
            for (Map.Entry<String, MediaModelView> entry : myMedias.entrySet()) {
                if (entry.getValue().status != DOWNLOAD_MY_MOMENT_MEDIA_FAILED && entry.getValue().status != DOWNLOADING_MY_MOMENT_MEDIA) {
                    downloadedMedias.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return downloadedMedias;
    }

    public HashMap<String,MediaModelView> getContributableDownloadedStreams(String momentId){
        LinkedHashMap<String,MediaModelView> downloadedMedias = new LinkedHashMap<>();
        if (contributedMomentsMediaDetails != null && contributedMomentsMediaDetails.get(momentId) != null){
            LinkedHashMap<String, MediaModelView> mediaList = contributedMomentsMediaDetails.get(momentId);
            for (Map.Entry<String, MediaModelView> entry : mediaList.entrySet()) {
                if (entry.getValue().status != DOWNLOAD_MY_MOMENT_MEDIA_FAILED && entry.getValue().status != DOWNLOADING_MY_MOMENT_MEDIA) {
                    downloadedMedias.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return downloadedMedias;
    }

    public interface OnMyStreamsLoaded {
        void onStreamsLoaded(LinkedHashMap<String, MediaModelView> list, LinkedHashMap<String, MediaModel> pendingMyMomentMedia, LinkedHashMap<String, MediaModel> pendingMediaInMessageRoom);

        void onMediaInMessageRoomsUpdated(LinkedHashMap<String, MediaModel> pendingMediaInMessageRoom);

        void onMediaInMomentUpdated(LinkedHashMap<String, MediaModel> pendingMyMomentMedia);

        void onSocialRequestChanges(HashMap<String,SocialModel.RequestReceived> requestReceived);

        void onContributedStreamsLoaded(String momentId,String mediaId,LinkedHashMap<String, CustomMomentModel> momentList, LinkedHashMap<String,LinkedHashMap<String,MediaModelView>> contributionList);

        void onFacebookFriendsChanged();
    }

    public void setMyStreamListener(Fragment fragment) {
        onMyStreamsLoadedCallback = (OnMyStreamsLoaded) fragment;
    }

    public LinkedHashMap<String, MomentModel> openSeenMoment(String momentId) {
        LinkedHashMap<String, MomentModel> mLocalMoments = new LinkedHashMap<>();
        if (mMoments != null) {
            for (Map.Entry<String, MomentModel> entry : mMoments.entrySet()) {
                Log.d(TAG, "entry.getKey() = " + entry.getKey());
                if (entry.getKey().equals(momentId)) {
                    if (mMoments.get(entry.getKey()).momentStatus == SEEN_MOMENT || mMoments.get(entry.getKey()).momentStatus == READY_AND_SEEN_MOMENT) {
                        mLocalMoments.put(entry.getKey(), mMoments.get(entry.getKey()));
                        break;
                    }
                }
            }
        }
        return mLocalMoments;
    }

    public void viewCompleteMoment(String momentId, int momentStatus) {
        if (mMoments != null && mMoments.get(momentId) != null) {
            if (momentStatus == READY_TO_VIEW_MOMENT) {
                mMoments.get(momentId).momentStatus = READY_AND_SEEN_MOMENT;
                momentsDataListener.onUnseenItemStatusChanged(momentId);
                AppLibrary.log_d(TAG, "updating status for moment view completed for moment " + momentId);
            }
        }
    }

    public void refreshMomentList() {
        firebaseHelperHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoments == null) {
                    Log.d(TAG, " mMoments null; probably user has no friends ");
                    return;
                }
                for (Map.Entry<String, MomentModel> entry : mMoments.entrySet()) {
                    if (entry.getValue().momentStatus == READY_AND_SEEN_MOMENT) {
                        entry.getValue().momentStatus = SEEN_MOMENT;
                    }
                }
                if (momentsDataListener != null)
                    momentsDataListener.onUnseenDataChanged(getMomentRoomView());
                if (momentsDataListener != null)
                    momentsDataListener.onSeenDataChanged(getSeenMomentRoomView());
            }
        });
    }

    public LinkedHashMap<String, MomentModel> openReadyMoment(String momentId) {
        LinkedHashMap<String, MomentModel> mLocalMoments = new LinkedHashMap<>();
        if (mMoments != null) {
            for (Map.Entry<String, MomentModel> entry : mMoments.entrySet()) {
                Log.d(TAG, "entry.getKey() = " + entry.getKey());
                if (entry.getKey().equals(momentId)) {
                    if (mMoments.get(entry.getKey()).momentStatus == READY_TO_VIEW_MOMENT) {
                        mLocalMoments.put(entry.getKey(), mMoments.get(entry.getKey()));
                        break;
                    }
                }
            }
        }
        return mLocalMoments;
    }

    public LinkedHashMap<String, MomentModel> openMoment(String momentId) {
        LinkedHashMap<String, MomentModel> mLocalMoments = new LinkedHashMap<>();
        boolean found = false;
        int totalSize = mMoments.size();
        int momentIndex = 0;
        int currentIndex = -1;
        Log.d(TAG, "momentId = " + momentId);

        if (mMoments != null) {
            for (Map.Entry<String, MomentModel> entry : mMoments.entrySet()) {
                currentIndex++;
                Log.d(TAG, "entry.getKey() = " + entry.getKey());
                if (entry.getKey().equals(momentId)) {
                    if (mMoments.get(entry.getKey()).momentStatus == READY_TO_VIEW_MOMENT) {
                        mLocalMoments.put(entry.getKey(), mMoments.get(entry.getKey()));
                        found = true;
                        momentIndex = currentIndex;
                        populatePreviousUnseen(mLocalMoments, momentIndex, totalSize);
                    } else {
                        break;
                    }
                } else if (found) {
                    if (mMoments.get(entry.getKey()).momentStatus == READY_TO_VIEW_MOMENT) {
                        mLocalMoments.put(entry.getKey(), mMoments.get(entry.getKey()));
                    } else {
                        break;
                    }
                }
            }
        }
        return mLocalMoments;
    }

    private void populatePreviousUnseen(LinkedHashMap<String, MomentModel> mLocalMoments, int foundIndex, int totalSize) {
        int currentIndex = -1;
        if (foundIndex < totalSize - 1) {
            for (Map.Entry<String, MomentModel> entry : mMoments.entrySet()) {
                currentIndex++;
                if (currentIndex < foundIndex) {
                    if (mMoments.get(entry.getKey()).momentStatus == READY_TO_VIEW_MOMENT) {
                        mLocalMoments.put(entry.getKey(), mMoments.get(entry.getKey()));
                    } else {
                        break;
                    }
                } else
                    break;
            }
        }
    }

    public interface SliderMessageListListener {
        void onSliderListChanged(String roomId, LinkedHashMap<String, SliderMessageModel> sliderMessageModels);

        void onMessageCountUpdate(long unreadRoomsCount);
    }

    private void removeMediaFromMoments(String momentId,String mediaId){
        getNewFireBase(ANCHOR_MOMENTS,new String[]{momentId,MEDIA,mediaId}).removeValue();
    }

    private void checkMediaInMessages(final boolean deleteFromMoment, final String mediaId, DataSnapshot dataSnapshot){
        final boolean[] deleteFromMessages = {true};
        final long roomCount = (long) dataSnapshot.child(ROOMS).getChildrenCount();
        final long[] roomIndex = {0};
        for (DataSnapshot room : dataSnapshot.child(ROOMS).getChildren()) {
            mFireBase.child(ANCHOR_ROOMS).child(room.getKey()).child(MESSAGES).keepSynced(true);
            mFireBase.child(ANCHOR_ROOMS).child(room.getKey()).child(MESSAGES).orderByChild(MEDIA_ID).equalTo(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    firebaseHelperHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (dataSnapshot.getValue() != null) {
                                deleteFromMessages[0] = false;
                            }
                            roomIndex[0]++;
                            if (roomIndex[0] == roomCount){
                                if (deleteFromMessages[0] && deleteFromMoment){
                                    removeMediaFromDownloaded(getMyUserId(),mediaId);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });

        }
    }

    private class MediaTransactionHandler implements Transaction.Handler {
        boolean isFinal;
        DataSnapshot mRoom;
        String userId;
        Callable completeFunc;

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() != null) {
                long momentCount = (long) currentData.getValue();
                if (momentCount > 0) {
                    momentCount--;
                }
                currentData.setValue(momentCount);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            // If I get that it is final then
            /*if(isFinal){
                if(mMomentRoomHashMap == null)
                    mMomentRoomHashMap = new LinkedHashMap<>();
                mMomentRoomHashMap.put(mRoom.getKey(),mRoom.getValue(RoomsModel.class));
                mMomentRoomHashMap.get(mRoom.getKey()).configureDetails(userId);
            }*/

            // Update Count
            if (mMomentRoomHashMap != null) {
                if (mRoom != null) {
                    RoomDetails obj = mMomentRoomHashMap.get(mRoom.getKey());
                    if (obj != null) {
//                        obj.members.get(userId).momentCount--;
                    }
                }
            }
        }

    }

    private class MessageTransactionHandler implements Transaction.Handler {
        boolean mRemove;
        boolean isFinal;
        DataSnapshot mRoom;
        DataSnapshot mMessage;
        String userId;
        int deductions = 0;

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() != null && deductions > 0) {
                long msgCount = (long) currentData.getValue();
                if (msgCount > 0) {
                    msgCount = msgCount - deductions;
                    if (msgCount < 0) {
                        msgCount = 0;
                    }
                }
                currentData.setValue(msgCount);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            if (mRemove) {
                mFireBase.child(ANCHOR_ROOMS).child(mRoom.getKey()).child(MESSAGES).child(mMessage.getKey()).removeValue();
            }
            if (mMessageRoomHashMap != null) {
                RoomsModel obj = mMessageRoomHashMap.get(mRoom.getKey());
                if (obj != null && obj.members != null && obj.members.get(userId) != null) {
                    obj.members.get(userId).msgCount--;
                }
            }
            if (isFinal) {
                // Trigger
                updateSliderMessageView(mRoom.getKey());
                if (mSliderMessageListListener != null)
                    mSliderMessageListListener.onSliderListChanged(mRoom.getKey(), sliderMessageModels);
            }
        }

    }

    public ArrayList<ListRoomView> getListRoomView() {
        ArrayList<ListRoomView> listRoomViews = new ArrayList<>();
        SocialModel.Friends[] friendList = getFriendList();
        if (friendList == null)
            return null;
        for (SocialModel.Friends friend : friendList) {
            ListRoomView listRoomView = new ListRoomView(friend.name, friend.imageUrl, friend.roomId, friend.handle, friend.momentId, friend.friendId, 1);
            listRoomView.setFirstChar(friend.name);
            listRoomViews.add(listRoomView);
        }
        return listRoomViews;
    }

    private ArrayList<HomeMomentViewModel> getMomentRoomView() {
        ArrayList<HomeMomentViewModel> momentRoomViews = new ArrayList<>();
        if (mMomentRoomHashMap == null || mMoments == null)
            return null;
        for (Map.Entry<String, RoomDetails> entry : mMomentRoomHashMap.entrySet()) {
            RoomDetails roomDetail = entry.getValue();
            if (mMoments.get(roomDetail.memberMomentId) == null)
                continue;

            if (mMoments.get(roomDetail.memberMomentId).medias.size() < 1)
                continue;

            if (mMoments.get(roomDetail.memberMomentId).momentStatus < SEEN_MOMENT) {
                long offset = entry.getValue().updatedAt + 24 * 60 * 60 * 1000 - System.currentTimeMillis();
                mMoments.get(roomDetail.memberMomentId).name = roomDetail.memberName;
                mMoments.get(roomDetail.memberMomentId).roomId = roomDetail.roomId;
                mMoments.get(roomDetail.memberMomentId).updatedDate = AppLibrary.timeAccCurrentTime(entry.getValue().updatedAt);

                HomeMomentViewModel momentRoomView = new HomeMomentViewModel(roomDetail.roomId,
                        roomDetail.memberName, roomDetail.memberProfilePicture,
                        AppLibrary.timeAccCurrentTime(entry.getValue().updatedAt), offset,
                        roomDetail.memberMomentId, false, 1, MEDIA_DOWNLOAD_NOT_STARTED,mMoments.get(roomDetail.memberMomentId).thumbnailUrl);
                momentRoomView.lastMediaId = mMoments.get(roomDetail.memberMomentId).lastMediaId;
                momentRoomView.userId = roomDetail.memberUserId;
                momentRoomView.handle = roomDetail.memberHandle;

                momentRoomView.momentStatus = mMoments.get(roomDetail.memberMomentId).momentStatus;
                momentRoomViews.add(momentRoomView);
            }
        }
        return momentRoomViews;
    }

    private ArrayList<HomeMomentViewModel> getSeenMomentRoomView() {
        ArrayList<HomeMomentViewModel> momentRoomViews = new ArrayList<>();
        if (mMomentRoomHashMap == null || mMoments == null || mMoments.size() <= 0)
            return null;
        for (Map.Entry<String, RoomDetails> entry : mMomentRoomHashMap.entrySet()) {
            RoomDetails roomDetail = entry.getValue();
            if (mMoments.get(roomDetail.memberMomentId) != null && mMoments.get(roomDetail.memberMomentId).medias != null && mMoments.get(roomDetail.memberMomentId).medias.size() > 0) {
                if (mMoments.get(roomDetail.memberMomentId).momentStatus >= SEEN_MOMENT) {
                    long offset = entry.getValue().updatedAt + 24 * 60 * 60 * 1000 - System.currentTimeMillis();
                    long offsetHours = (System.currentTimeMillis() - entry.getValue().updatedAt) / (1000 * 60 * 60);
                    long offsetMinutes = (System.currentTimeMillis() - entry.getValue().updatedAt) / (1000 * 60);

                    String timeString = "";
                    if (offsetHours > 0) {
                        timeString = offsetHours + (offsetHours > 1 ? " hrs " : " hr ");
                    } else if (offsetMinutes > 2) {
                        timeString = "" + offsetMinutes + " mins";
                    } else {
                        timeString = "Now";
                    }
                    mMoments.get(roomDetail.memberMomentId).name = roomDetail.memberName;
                    mMoments.get(roomDetail.memberMomentId).roomId = roomDetail.roomId;
                    mMoments.get(roomDetail.memberMomentId).updatedDate = timeString;

                    HomeMomentViewModel momentRoomView = new HomeMomentViewModel(roomDetail.roomId, roomDetail.memberName, roomDetail.memberProfilePicture, timeString, offset, roomDetail.memberMomentId, false, 1 , MEDIA_DOWNLOAD_NOT_STARTED,mMoments.get(roomDetail.memberMomentId).thumbnailUrl);
                    momentRoomView.lastMediaId = mMoments.get(roomDetail.memberMomentId).lastMediaId;
                    momentRoomView.thumbnailUrl = mMoments.get(roomDetail.memberMomentId).thumbnailUrl;
                    momentRoomView.userId = roomDetail.memberUserId;

                    momentRoomView.momentStatus = mMoments.get(roomDetail.memberMomentId).momentStatus;
                    momentRoomView.handle = roomDetail.memberHandle;

                    momentRoomViews.add(momentRoomView);
                }
            }
        }
        return momentRoomViews;
    }

    private LinkedHashMap<String, SliderMessageModel> updateSliderMessageView(String roomId) {
        RoomsModel roomsModel = mMessageRoomHashMap.get(roomId);
        RoomsModel.Detail roomDetail = roomsModel.detail;

        String timeString = AppLibrary.timeAccCurrentTime(roomsModel.updatedAt);

        if (roomsModel.members == null || roomsModel.members.get(getMyUserId()) == null) {
            Log.e(TAG, " user itself not found in messageRoom ; skipping room: " + roomId);
            return null;
        }
        SliderMessageModel sliderMessageModel;
        if (roomsModel.members.get(getMyUserId()) != null) {
            sliderMessageModel = new SliderMessageModel(roomDetail.name, roomDetail.imageUrl, roomDetail.roomId, roomDetail.userId, roomDetail.roomType, roomsModel.members.get(getMyUserId()).type, roomsModel.updatedAt, roomsModel.members);
        } else {
            sliderMessageModel = new SliderMessageModel(roomDetail.name, roomDetail.imageUrl, roomDetail.roomId, roomDetail.userId, roomDetail.roomType, 0, roomsModel.updatedAt, roomsModel.members);
        }
        sliderMessageModels.put(roomDetail.roomId, sliderMessageModel);
        return sliderMessageModels;
    }

    private class MediaViewTransactionHandler implements Transaction.Handler {

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() != null) {
                int mediaViews = currentData.getValue(Integer.class);
                mediaViews++;
                currentData.setValue(mediaViews);
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

        }

    }

    private class ExpiryTypeTransactionHandler implements Transaction.Handler {

        int finalValue = 0;
        String messageId = "";
        String roomId = "";

        @Override
        public Transaction.Result doTransaction(MutableData currentData) {
            if (currentData.getValue() != null) {
                if (currentData.getValue(Long.class) != finalValue) {
                    currentData.setValue(finalValue);
                }
            } else {
                mFireBase.child(ANCHOR_ROOMS).child(roomId).child(messageId).removeValue();
            }
            return Transaction.success(currentData);
        }

        @Override
        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

        }

    }

    public void openMedia(final String momentId, final String mediaId, String roomId, String messageId, final int expiryType, final int roomType) {
        if (momentId != null) {
            mFireBase.child(ANCHOR_MOMENTS).child(momentId).child(MEDIA).child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                        // Update Moment/media Views and viewers
                        if (dataSnapshot.child(VIEW_DETAILS) == null || dataSnapshot.child(VIEW_DETAILS).getValue() == null || !dataSnapshot.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                            ViewerDetails viewerDetails = new ViewerDetails(getMyUserModel().name,false,getCreatedAt());
                            mFireBase.child(ANCHOR_MOMENTS).child(momentId).child(MEDIA).child(mediaId).child(VIEW_DETAILS).child(getMyUserId()).setValue(viewerDetails);
                        }
                        mMoments.get(momentId).medias.get(mediaId).status = MEDIA_VIEWED;
                    } else {

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
        // Update View Status

        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null){
                    dataSnapshot.getRef().child(LOCAL_MEDIA_STATUS).setValue(MEDIA_VIEWED);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
        // Update Media views and viewers
        mFireBase.child(ANCHOR_MEDIA).child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    if (dataSnapshot.child(VIEW_DETAILS).getValue() == null || !dataSnapshot.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                        ViewerDetails viewerDetails = new ViewerDetails(getMyUserModel().name,false,getCreatedAt());
                        mFireBase.child(ANCHOR_MEDIA).child(mediaId).child(VIEW_DETAILS).child(getMyUserId()).setValue(viewerDetails);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

        if (roomId != null && messageId != null && expiryType != -1) {
            // Update room
            mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        RoomsModel.Messages message = dataSnapshot.getValue(RoomsModel.Messages.class);
                        if (message.viewers == null)
                            message.viewers = new HashMap<String, Integer>();


                        if (!message.viewers.containsKey(getMyUserId()))
                            message.viewers.put(getMyUserId(), 1);
                        if (roomType == FRIEND_ROOM) {
                            if (expiryType <= EXPIRY_TYPE_VIEW_ONCE) {
                                message.expiryType = VIEW_ONCE_AND_VIEWED;
                            } else if (expiryType == MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                                message.expiryType = REMOVED_UPON_ROOM_OPEN;
                            }
                        }
                        dataSnapshot.getRef().setValue(message);
                        dataSnapshot.getRef().setPriority(message.createdAt);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    public void openNearbyMedia(final String momentId, final String mediaId, String roomId, String messageId, final int expiryType, final int roomType) {
        if (momentId != null && mediaId != null) {
            final DynamicDownloader downloader = DynamicDownloader.getInstance(mContext);
            MomentModel momentModel = downloader.getNearByMoment(momentId);

            if (momentModel != null && momentModel.medias != null && momentModel.medias.get(mediaId) != null && momentModel.medias.get(mediaId).status != MEDIA_VIEWED) {
                momentModel.medias.get(mediaId).status = MEDIA_VIEWED;
            }

            if (LocalDataHelper.downloadStatusHashMap != null && LocalDataHelper.downloadStatusHashMap.containsKey(mediaId)) {
                if (LocalDataHelper.downloadStatusHashMap.get(mediaId).status != MEDIA_VIEWED) {
                    LocalDataHelper.downloadStatusHashMap.get(mediaId).status = MEDIA_VIEWED;
                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE).child(mediaId).child(LOCAL_MEDIA_STATUS).setValue(MEDIA_VIEWED);
                }
                if (LocalDataHelper.downloadStatusHashMap.get(mediaId).viewed == null || !LocalDataHelper.downloadStatusHashMap.get(mediaId).viewed.containsKey(momentId)) {
                    if (LocalDataHelper.downloadStatusHashMap.get(mediaId).viewed == null)
                        LocalDataHelper.downloadStatusHashMap.get(mediaId).viewed = new HashMap<>();
                    LocalDataHelper.downloadStatusHashMap.get(mediaId).viewed.put(momentId,getCreatedAt());
                    updateViewerDetails(momentId,mediaId);
                    mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE).child(mediaId).child(VIEWED).child(momentId).setValue(getCreatedAt());
                }
            }
        }

        // for message
        if (roomId != null && messageId != null && expiryType != -1) {
            // Update room
            mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).child(messageId).keepSynced(true);
            mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        RoomsModel.Messages message = dataSnapshot.getValue(RoomsModel.Messages.class);
                        if (message.viewers == null)
                            message.viewers = new HashMap<String, Integer>();


                        if (!message.viewers.containsKey(getMyUserId()))
                            message.viewers.put(getMyUserId(), 1);
                        if (roomType == FRIEND_ROOM) {
                            if (expiryType <= EXPIRY_TYPE_VIEW_ONCE) {
                                message.expiryType = VIEW_ONCE_AND_VIEWED;
                            } else if (expiryType == MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                                message.expiryType = REMOVED_UPON_ROOM_OPEN;
                            }
                        }
                        dataSnapshot.getRef().setValue(message);
                        dataSnapshot.getRef().setPriority(message.createdAt);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    private void updateViewerDetails(String momentId,String mediaId) {
        HashMap<Object, Object> postObject = new HashMap<>();
        postObject.put(USER_ID, getMyUserId());
        postObject.put(NAME, getMyUserModel().name);
        postObject.put(MOMENT_ID, momentId);
        postObject.put(VIEWED_AT, getCreatedAt());
        postObject.put(MEDIA_ID, mediaId);
        postFireBaseRequest(UPDATE_VIEWER, postObject);
    }

    public void removeMessage(String roomId, String messageId) {
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).child(messageId).setValue(null);
    }

    private void sendNotification(NotificationContent notificationContent) {
        mFireBase.child(ANCHOR_REQUEST).child(NOTIFICATION).push().setValue(notificationContent);
    }

    private void removeMediaFromDownloaded(String userId, String mediaId) {
        mFireBase.child(ANCHOR_LOCAL_DATA).child(userId).child(MEDIA_DOWNLOAD).child(mediaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                            if (dataSnapshot.hasChild(URL)) {
                                String filepath = (String) dataSnapshot.child(URL).getValue();
                                if (AppLibrary.checkStringObject(filepath) != null) {
                                    File file = new File(filepath);
                                    if (file.exists()) {
                                        file.delete();
                                    }
                                }
                            }
                        }
                        if (dataSnapshot != null) {
                            dataSnapshot.getRef().removeValue();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    void replaceMediaFromFriendRoom(final String mediaId, final String roomId) {
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).orderByChild(MEDIA_ID).equalTo(mediaId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.child(EXPIRY_TYPE).getValue(Integer.class) != MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                    dataSnapshot.getRef().child(MEDIA_ID).setValue(DEFAULT_MEDIA);
                    removeMediaFromDownloaded(getMyUserId(), mediaId);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

    }

    void replaceMediaFromGroupRoom(final String mediaId, final String roomId, final boolean sentToFriend) {
        mFireBase.child(ANCHOR_ROOMS).child(roomId).child(MESSAGES).orderByChild(MEDIA_ID).equalTo(mediaId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                dataSnapshot.getRef().child(MEDIA_ID).setValue(DEFAULT_MEDIA);
                if (!sentToFriend)
                    removeMediaFromDownloaded(getMyUserId(), mediaId);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private AllChatListRoomCountChangedListener allChatListRoomCountChangedListener;

    public void setAllChatListRoomCountChangedListener(AllChatListRoomCountChangedListener allChatListRoomCountChangedListener) {
        this.allChatListRoomCountChangedListener = allChatListRoomCountChangedListener;
    }

    public interface AllChatListRoomCountChangedListener {
        void onChatRoomsChanged();
    }

    private void fetchMessageRooms(final String userId) {
        this.getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM}).addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processNonExpiredMessageRooms(dataSnapshot, userId);
                        if (allChatListRoomCountChangedListener != null)
                            allChatListRoomCountChangedListener.onChatRoomsChanged();
                    }
                });
            }

            @Override
            public void onChildChanged(final DataSnapshot dataSnapshot, String s) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processNonExpiredMessageRooms(dataSnapshot, userId);
                    }
                });
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (allChatListRoomCountChangedListener != null)
                            allChatListRoomCountChangedListener.onChatRoomsChanged();
                    }
                });
            }

            @Override
            public void onChildMoved(final DataSnapshot dataSnapshot, String s) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mMessageRoomHashMap != null && mMessageRoomHashMap.containsKey(dataSnapshot.getKey())) {
                            RoomsModel model = mMessageRoomHashMap.remove(dataSnapshot.getKey());
                            LinkedHashMap<String, RoomsModel> roomsModelLinkedHashMap = new LinkedHashMap<String, RoomsModel>();
                            roomsModelLinkedHashMap.put(dataSnapshot.getKey(), model);
                            roomsModelLinkedHashMap.putAll(mMessageRoomHashMap);
                            mMessageRoomHashMap = roomsModelLinkedHashMap;
                        }
                        if (sliderMessageModels != null && sliderMessageModels.containsKey(dataSnapshot.getKey())) {
                            SliderMessageModel sliderMessageModel = sliderMessageModels.remove(dataSnapshot.getKey());
                            LinkedHashMap<String, SliderMessageModel> updatedSliderMessageModel = new LinkedHashMap<String, SliderMessageModel>();
                            updatedSliderMessageModel.put(dataSnapshot.getKey(), sliderMessageModel);
                            updatedSliderMessageModel.putAll(sliderMessageModels);
                            sliderMessageModels = updatedSliderMessageModel;
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void fetchUnreadMessageCount() {
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(UNREAD_ROOMS).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mSliderMessageListListener != null)
                            mSliderMessageListListener.onMessageCountUpdate(dataSnapshot.getChildrenCount());
                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void processNonExpiredMessageRooms(DataSnapshot dataSnapshot, String userId) {
        HandleMessageRoom(dataSnapshot, userId);
    }

    private void HandleMessageRoom(final DataSnapshot room, final String userId) {
        final long roomType = (long) (room.child(ROOM_TYPE).getValue());
        final HashMap<String, Integer> transactionMap = new HashMap<>();
        // Fetch all messages which are expired and remove them
        mFireBase.child(ANCHOR_ROOMS).child(room.getKey()).keepSynced(true);
        mFireBase.child(ANCHOR_ROOMS).child(room.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot.getValue() == null)
                            return;
                        if (mMessageRoomHashMap == null)
                            mMessageRoomHashMap = new LinkedHashMap<>();
                        mMessageRoomHashMap.put(dataSnapshot.getKey(), dataSnapshot.getValue(RoomsModel.class));
                        mMessageRoomHashMap.get(dataSnapshot.getKey()).configureDetails(userId, room.getKey());
                        mMessageRoomHashMap.get(dataSnapshot.getKey()).setUpdatedAt((long) room.child(UPDATED_AT).getValue());
                        if (!dataSnapshot.hasChild(MESSAGES)) {
                            updateSliderMessageView(dataSnapshot.getKey());
                            if (mSliderMessageListListener != null) {
                                mSliderMessageListListener.onSliderListChanged(dataSnapshot.getKey(), sliderMessageModels);
                            }
                        } else {
                            long messageCount = dataSnapshot.child(MESSAGES).getChildrenCount();
                            long msgIndex = 0;
                            for (DataSnapshot message : dataSnapshot.child(MESSAGES).getChildren()) {
                                msgIndex++;
                                boolean isExpired = false;
                                try {
                                    isExpired = isExpiredMessage(message);
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                                if (isExpired && dataSnapshot.hasChild(MEMBERS)) {
                                    long memberCount = (long) dataSnapshot.child(MEMBERS).getChildrenCount();
                                    long i = 0;
                                    boolean viewed = true;
                                    MessageTransactionHandler transactionHandler = new MessageTransactionHandler();
                                    transactionHandler.mMessage = message;
                                    transactionHandler.mRoom = dataSnapshot;
                                    for (DataSnapshot member : dataSnapshot.child(MEMBERS).getChildren()) {
                                        i++;
                                        transactionMap.put(member.getKey(), 0);
                                        transactionHandler.isFinal = (i == (memberCount));
                                        transactionHandler.mRemove = (i == (memberCount)) && viewed;
                                        transactionHandler.userId = member.getKey();
                                        if (message.child(VIEW_DETAILS).child(member.getKey()).getValue() == null) {
                                            if (roomType == GROUP_ROOM) {
                                                transactionMap.put(member.getKey(), (transactionMap.get(member.getKey() + 1)));
                                            } else {
                                                Log.d(TAG, " message key " + message.getKey());
                                                if (message.child(EXPIRY_TYPE).getValue(Integer.class) != MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                                                    Log.d(TAG, " inside and setting " + message.getKey());
                                                    ExpiryTypeTransactionHandler expiryTypeTransactionHandler = new ExpiryTypeTransactionHandler();
                                                    expiryTypeTransactionHandler.finalValue = MESSAGE_EXPIRED_BUT_NOT_VIEWED;
                                                    expiryTypeTransactionHandler.roomId = room.getKey();
                                                    expiryTypeTransactionHandler.messageId = message.getKey();
                                                    message.child(EXPIRY_TYPE).getRef().runTransaction(expiryTypeTransactionHandler);
                                                    viewed = false;
                                                    if (!message.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                                                        updateUnreadRooms(room.getKey());
                                                    }
                                                }
                                            }
                                        } else {
                                            transactionMap.put(member.getKey(), (transactionMap.get(member.getKey()) + 1));
                                        }
                                        if (i == memberCount) {
                                            // Trigger
                                            Log.d(TAG, "Trigger for messages ");
                                            updateSliderMessageView(room.getKey());
                                            if (mSliderMessageListListener != null) {
                                                mSliderMessageListListener.onSliderListChanged(room.getKey(), sliderMessageModels);
                                            }
                                            runTransaction(room, transactionMap, message, transactionHandler);
                                        }
                                    }
                                } else {
                                    if (!message.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                                        updateUnreadRooms(room.getKey());
                                    }
                                    if ((msgIndex == messageCount)) {
                                        // Trigger
                                        Log.d(TAG, "Trigger for messages ");
                                        updateSliderMessageView(room.getKey());
                                        if (mSliderMessageListListener != null)
                                            mSliderMessageListListener.onSliderListChanged(room.getKey(), sliderMessageModels);
                                    }
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void updateUnreadRooms(String roomId) {
        getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), UNREAD_ROOMS, roomId}).setValue(1);
    }

    public void runTransaction(DataSnapshot room, HashMap<String, Integer> map, DataSnapshot message, MessageTransactionHandler transactionHandler) {
        int i = 0;
        for (Map.Entry<String, Integer> member : map.entrySet()) {
            i++;
            if (map.get(member.getKey()) != null) {
                transactionHandler.deductions = map.get(member.getKey());
                mFireBase.child(ANCHOR_ROOMS).child(room.getKey()).child(MESSAGES).child(member.getKey()).child(MESSAGE_COUNT).runTransaction(transactionHandler);
            }
        }
    }

    private boolean fileExists(String value) {
        return value != null && !(value.contains("https://") || value.contains("http://")) && new File(value).exists();
    }

    public boolean checkIfMediaExempted(MomentModel.Media media) {
        if (media != null && media.privacy != null) {
            MomentModel.Privacy privacy = media.privacy;
            if (privacy.value != null){
                HashMap<String, String> list = privacy.value;
                if (privacy.type == PrivacyPopup.FRIENDS_EXCEPT_ROW) {
                    if(list.containsKey(getMyUserId()))
                        return true;
                } else if (privacy.type == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW) {
                    if (list.containsKey(getMyUserId()))
                        return false;
                    return true;
                }
            }
        }

        return false;
    }

    private void cleanMessageMediaFromLocalData(String userId) {
        this.getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM}).keepSynced(true);
        this.getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            getNewFireBase(ANCHOR_ROOMS, new String[]{snapshot.getKey()}).keepSynced(true);
                            getNewFireBase(ANCHOR_ROOMS, new String[]{snapshot.getKey()}).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot roomSnapshot) {
                                    firebaseHelperHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            RoomsModel model = roomSnapshot.getValue(RoomsModel.class);
                                            final HashMap<String, RoomsModel.Messages> messagesHashMap = model.messages;
                                            if (messagesHashMap != null && !messagesHashMap.isEmpty()) {
                                                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).orderByChild("rooms/" + roomSnapshot.getKey()).equalTo(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(final DataSnapshot dataSnapshot) {
                                                        firebaseHelperHandler.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                                    String mediaId = snapshot.getKey();
                                                                    boolean isMediaFound = false;
                                                                    for (Map.Entry<String, RoomsModel.Messages> entry : messagesHashMap.entrySet()) {
                                                                        if (entry.getValue().type == MESSAGE_TYPE_MEDIA && entry.getValue().mediaId.equals(mediaId)) {
                                                                            isMediaFound = true;
                                                                            break;
                                                                        }
                                                                    }
                                                                    if (!isMediaFound) {
                                                                        // delete media for local db
//                                                                        cleanMedia(mediaId);
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }

                                                });
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }

                            });
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void cleanLocalData(final String momentId, final HashMap<String, MomentModel.Media> currentMedias) {
        if (currentMedias == null)
            return;
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).orderByChild(CONTRIBUTED_MOMENTS + "/" + momentId).equalTo(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                firebaseHelperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!currentMedias.isEmpty()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                if (!currentMedias.containsKey(snapshot.getKey())) {
//                                    cleanMedia(snapshot.getKey());
                                }
                            }
                        } else {
                            mFireBase.child(MOMENTS).child(momentId).child(MEDIA).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot medias) {
                                    firebaseHelperHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                if (!medias.hasChild(snapshot.getKey())) {
//                                                    cleanMedia(getMyUserId());
                                                }
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }

                            });
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private boolean isExpiredMedia(DataSnapshot media) {
        long now = System.currentTimeMillis() - getMediaExpiryTime() * 60 * 60 * 1000;
        Long dtStart = (Long) media.child(CREATED_AT).getValue();

        if (dtStart == null)
            return true;
        if (now > dtStart) {
            return true;
        }
        return false;
    }

    private boolean isExpiredMessage(DataSnapshot message) {
        long now = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        Long dtStart = (Long) message.child(CREATED_AT).getValue();
        if (now > dtStart) {
            return true;
        }
        return false;
    }

    private void cleanMessageData(final String userId) {
        firebaseHelperHandler.post(new Runnable() {
            @Override
            public void run() {
                fetchUnreadMessageCount();

                fetchMessageRooms(getMyUserId()); //ToDo: Remove privacy code from this and shift elsewhere

                cleanMessageMediaFromLocalData(userId);
            }
        });
    }

    void loadData() {
        LocalDataHelper.getInstance(this, LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD);
        firebaseHelperHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mUserModel == null)
                    loadMyUserModel();
                if (getSocialModel() == null)
                    loadMySocialModel();
                if (faceBookFriendsuggestions == null)
                    loadFriendSuggestions();
                if (socialRequests.size() == 0)
                    loadPendingFriendRequests();
                getCustomFriendList();
            }
        });
    }

    int myMomentStatus = SEEN_MOMENT;

    public void loadMyMoment() {
        firebaseHelperHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mUserModel == null || mUserModel.momentId == null)
                    return;
                mFireBase.child(ANCHOR_MOMENTS).child(mUserModel.momentId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        firebaseHelperHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                myMoment = dataSnapshot.getValue(MomentModel.class);
                                myMoment.momentId = mUserModel.momentId;
                                final LinkedHashMap<String, MomentModel.Media> medias = new LinkedHashMap<>();
                                dataSnapshot.getRef().child(MEDIA).addChildEventListener(new ChildEventListener() {
                                    @Override
                                    public void onChildAdded(final DataSnapshot media, String s) {
                                        firebaseHelperHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MomentModel.Media mediaObj = media.getValue(MomentModel.Media.class);
                                                mediaObj.mediaId = media.getKey();
                                                mediaObj.momentId = mUserModel.momentId;
                                                boolean isExpired = isExpiredMedia(media);
                                                if (isExpired) {
                                                } else {
                                                    medias.put(media.getKey(), mediaObj);
                                                    if (!media.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                                                        myMomentStatus = UNSEEN_MOMENT;
                                                    }
                                                    feedMediaToMyStreams(mediaObj);
                                                }
                                                myMoment.momentStatus = myMomentStatus;
                                                myMoment.medias = medias;
                                            }
                                        });
                                    }

                                    @Override
                                    public void onChildChanged(final DataSnapshot dataSnapshot, String s) {
                                        firebaseHelperHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                MomentModel.Media mediaObj = dataSnapshot.getValue(MomentModel.Media.class);
                                                mediaObj.mediaId = dataSnapshot.getKey();
                                                mediaObj.momentId = mUserModel.momentId;
                                                String mediaId = dataSnapshot.getKey();
                                                if (myMedias.containsKey(mediaId)) {
                                                    medias.put(mediaId, mediaObj);
                                                    if (!dataSnapshot.child(VIEW_DETAILS).hasChild(getMyUserId())) {
                                                        myMomentStatus = UNSEEN_MOMENT;
                                                    }
                                                    feedMediaToMyStreams(mediaObj);
                                                }
                                                myMoment.momentStatus = myMomentStatus;
                                                myMoment.medias = medias;
                                            }
                                        });
                                    }

                                    @Override
                                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                                    }

                                    @Override
                                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }

                                });
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
                fetchMyPendingStreams();
            }
        });
    }

    public ArrayList<SliderMessageModel> getAllMessageRoom() {
        if (mMessageRoomHashMap == null)
            return null;
        ArrayList<SliderMessageModel> arrayList = new ArrayList<>();
        for (Map.Entry<String, RoomsModel> roomEntry : mMessageRoomHashMap.entrySet()) {
            RoomsModel roomsModel = roomEntry.getValue();
            if (roomsModel == null) continue;
            String displayName = null, imageUrl = null;
            String friendId = null;
            if (roomsModel.type == FRIEND_ROOM) {
                HashMap<String, RoomsModel.Members> members = roomsModel.members;
                for (Map.Entry<String, RoomsModel.Members> membersEntry : members.entrySet()) {
                    if (!membersEntry.getKey().equals(getMyUserId())) {
                        displayName = membersEntry.getValue().name;
                        friendId = membersEntry.getKey();
                        imageUrl = membersEntry.getValue() == null ? null : membersEntry.getValue().imageUrl;
                        break;
                    }
                }
            } else if (roomsModel.type == GROUP_ROOM) {
                displayName = roomsModel.detail == null ? null : roomsModel.detail.name;
                imageUrl = roomsModel.detail == null ? null : roomsModel.detail.imageUrl;
            }
            SliderMessageModel model = new SliderMessageModel(displayName, imageUrl, roomEntry.getKey(), friendId, roomsModel.type, 000, 0, roomsModel.members);
            model.setFirstChar(displayName);
            arrayList.add(model);
        }
        return arrayList;
    }

    /**
     * 9. Accept ` Request
     * <p/>
     * STEP 1:
     * UPDATE rooms/<roomId>/members as
     * <reqUser._id> : {
     * name: String,
     * imageUrl: String,
     * handle: String,
     * momentId : String,
     * type: 12 // you joined
     * }
     * <p/>
     * STEP 2
     * REMOVE rooms/<roomId>/pendingMembers/<reqUser._id>
     * REMOVE socials/<reqUser._id>/pendingGroupRequest/<roomId>
     * }
     */
    public void acceptGroupRequest(final String reqUser, final String roomId, final SocialModel.PendingGroupRequest requestReceived) {
        Log.d(TAG, " accepting group request with roomId: " + roomId);

        //STEP 1
        final RoomsModel.Members members = new RoomsModel.Members();
        members.name = getMyUserModel().name;
        members.imageUrl = getMyUserModel().imageUrl;
        members.handle = getMyUserModel().handle;
        members.momentId = getMyUserModel().momentId;
        members.type = YOU_JOIN;
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, reqUser}).setValue(members);

        //STEP 2
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, PENDING_MEMBERS, reqUser}).setValue(null);
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUser, PENDING_GROUP_REQUEST, roomId}).setValue(null);
        /**
         * STEP 3
         STEP 3.1
         PUSH rooms/<roomId>/messages
         {
         type: 4,
         text: <reqUser Name> Joined
         createdAt: Date,
         expiryType: 3
         }

         STEP 3.2 Iterate over each members except reqUser
         STEP 3.2.1
         UPDATE rooms/<roomId>/members/<userId> as {type : 11}
         STEP 3.2.3
         SET Priority of users/<userId>/messageRooms/<roomId> with -ve timestamp

         */
        //STEP3
        //3.1
        RoomsModel.Messages message = new RoomsModel.Messages();
        message.type = SYSTEM_MESSAGE;
        message.createdAt = getCreatedAt();
        message.expiryType = EXPIRY_TYPE_24_HOURS;
        message.text = getMyUserModel().name + " joined";
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MESSAGES}).push().setValue(message);

        //3.2
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId}).keepSynced(true);
        getNewFireBase(ANCHOR_ROOMS, new String[]{roomId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomsModel roomsModel = dataSnapshot.getValue(RoomsModel.class);
                final HashMap<String, RoomsModel.Members> roomMembers = roomsModel.members;
                for (Map.Entry<String, RoomsModel.Members> memberEntry : roomMembers.entrySet()) {
                    if (memberEntry.getKey().equals(reqUser)) continue;
                    String userId = memberEntry.getKey();
                    getNewFireBase(ANCHOR_ROOMS, new String[]{roomId, MEMBERS, userId, TYPE}).setValue(MEMBER_JOIN);
                    getNewFireBase(ANCHOR_USERS, new String[]{userId, MESSAGE_ROOM, roomId}).setPriority(getPriorityTimeStamp());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
        //STEP 4
        //4.1
        UserModel.Rooms room = new UserModel.Rooms();
        room.updatedAt = getUpdatedAt();
        room.type = GROUP_ROOM;
        room.interacted = false;
        getNewFireBase(ANCHOR_USERS, new String[]{reqUser, MESSAGE_ROOM, roomId}).setValue(room);

        //4.2
        getNewFireBase(ANCHOR_USERS, new String[]{reqUser, MESSAGE_ROOM, roomId}).setPriority(getPriorityTimeStamp());

        //4.3
//        public Groups(String name, String imageUrl, String handle, String admin,String roomId){
        SocialModel.Groups groups = new //fixme admin??
                SocialModel.Groups(requestReceived.name, requestReceived.imageUrl, null, null, roomId);
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUser, GROUPS, roomId}).setValue(groups);
    }

    /**
     * Group Request
     * INPUT : reqUser, roomId
     * <p/>
     * STEP 1:
     * Remove socials/<reqUser._id>/pendingGroupRequest/<roomId>
     * <p/>
     * STEP 2:
     * UPDATE socials/<reqUser._id>/groupRequestIgnored with key as <roomId> and value as 1
     */
    public void ignoreGroupRequest(String reqUser, String roomId) {

        Log.d(TAG, " ignoring group request roomId: " + roomId);
        //STEP 1
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUser, PENDING_GROUP_REQUEST, roomId}).setValue(null);

        //STEP 2
        getNewFireBase(ANCHOR_SOCIALS, new String[]{reqUser, GROUP_REQUEST_IGNORED, roomId}).setValue(1);
    }

    public void registerChatMediaForDownload(final RoomsModel.Messages message) {
        getNewFireBase(ANCHOR_MEDIA, new String[]{message.mediaId}).keepSynced(true);
        getNewFireBase(ANCHOR_MEDIA, new String[]{message.mediaId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MediaModel mediaModel = dataSnapshot.getValue(MediaModel.class);
                if (mediaModel == null) {
                    Log.d(TAG, " registerChatMediaForDownload failed returning");
                    return;
                }
                MediaDownloader downloader = new MediaDownloader(mContext);
                downloader.startDownload(message, dataSnapshot.getKey(), mediaModel, new DownloadStatusCallbacks() {
                    @Override
                    public void updateDownloadingStatus(String mediaId, int status) {
                        if (status == ERROR_DOWNLOADING_MEDIA) {
                            message.mediaStatus = status;
                            chatMediaStatusChangeListener.onMediaChanged();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public interface DownloadStatusCallbacks {
        void updateDownloadingStatus(String mediaId, int status);
    }

    public void updateLocalPathForChatMedia(MediaModel mediaModel, RoomsModel.Messages message, String path, int status) {
        message.uri = path;
        message.mediaStatus = MEDIA_DOWNLOAD_COMPLETE;
        chatMediaStatusChangeListener.onMediaChanged();
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(message.mediaId).child(URL).setValue(path);
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(message.mediaId).child(LOCAL_MEDIA_STATUS).setValue(status);
        if (mediaModel != null && mediaModel.addedTo != null && mediaModel.addedTo.rooms != null) {
            mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(message.mediaId).child(ROOMS).setValue(mediaModel.addedTo.rooms);
        }
    }

    public void updateDownloadDetailsForMedia(String mediaId, MediaModel mediaModel, int status) {
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(URL).setValue(mediaModel.url);
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(LOCAL_MEDIA_STATUS).setValue(status);
        mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CREATED_AT).setValue(mediaModel.createdAt);
        if (mediaModel.addedTo != null) {
            if (mediaModel.addedTo.moments != null) {
                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(CONTRIBUTED_MOMENTS).setValue(mediaModel.addedTo.moments);
            }
            if (mediaModel.addedTo.rooms != null) {
                mFireBase.child(ANCHOR_LOCAL_DATA).child(getMyUserId()).child(MEDIA_DOWNLOAD).child(mediaId).child(ROOMS).setValue(mediaModel.addedTo.rooms);
            }
        }

        LocalDataHelper.getInstance(this, LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD).writeNewMediaDownloadDetailsToMemory(mediaId,mediaModel,status);
    }

    public void checkChatMediaDownloadStatus(final RoomsModel.Messages message) {
        final String mediaId = message.mediaId;
        getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{getMyUserId(), MEDIA_DOWNLOAD, mediaId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    int status = dataSnapshot.child(LOCAL_MEDIA_STATUS).getValue(Integer.class);
                    if (status >= MEDIA_DOWNLOAD_COMPLETE) {
                        if (dataSnapshot.child(URL).getValue() != null && fileExists(dataSnapshot.child(URL).getValue().toString())) {
                            message.uri = dataSnapshot.child(URL).getValue().toString();
                            message.mediaStatus = status;
                            chatMediaStatusChangeListener.onMediaChanged();
                        } else {
                            if (message.memberId.equals(getMyUserId()) && message.mediaUploadingStatus != MEDIA_UPLOADING_COMPLETE) {
                                // delete the media message
                                chatMediaStatusChangeListener.deleteMediaMessage(message.messageId);
                            } else {
                                message.mediaStatus = MEDIA_DOWNLOADING;
                                chatMediaStatusChangeListener.onMediaChanged();
                                registerChatMediaForDownload(message);
                            }
                        }
                    } else {
                        // downloading chat media
                        message.mediaStatus = MEDIA_DOWNLOADING;
                        chatMediaStatusChangeListener.onMediaChanged();
                        registerChatMediaForDownload(message);
                    }
                } else {
                    message.mediaStatus = MEDIA_DOWNLOADING;
                    chatMediaStatusChangeListener.onMediaChanged();
                    registerChatMediaForDownload(message);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public void retryDownloadingChatMedia(RoomsModel.Messages messages) {
        if (messages.mediaId != null)
            registerChatMediaForDownload(messages);
    }

    private ChatMediaStatusChangeListener chatMediaStatusChangeListener;

    public void setChatMediaStatusChangeListener(ChatMediaStatusChangeListener chatMediaStatusChangeListener) {
        this.chatMediaStatusChangeListener = chatMediaStatusChangeListener;
    }

    public interface ChatMediaStatusChangeListener {
        void onMediaChanged();

        void deleteMediaMessage(String messageId);
    }

    public void updateNameInFireBase(String newName) {
            this.getMyUserModel().name = newName;
            this.getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId(), NAME}).setValue(newName);
    }

    public void updateBirthdayInFireBase(String birthday, boolean birthdayFlag) {
            this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), USER_BIRTHDAY}).setValue(birthday);
            this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), BIRTHDAY_PARTY_FLAG}).setValue(birthdayFlag);
    }

    public void updatePushNotificationFlagOnFireBase(boolean isChecked) {
        this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), PUSH_NOTIFICATION_FLAG}).setValue(isChecked);
    }

    public void updateTravelModeFlagOnFireBase(boolean isChecked) {
        this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), TRAVEL_MODE}).setValue(isChecked);
    }

    public void updateAutoSaveModeFlagOnFireBase(boolean isChecked) {
        this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), AUTO_SAVE_MODE}).setValue(isChecked);
    }

    public void sendNameUpdateRequestToFireBase(String newName, String userId) {
        HashMap<Object,Object> requestObject = new HashMap<>();
        requestObject.put(AppLibrary.NAME,newName);
        requestObject.put(AppLibrary.USER_ID,userId);
        postFireBaseRequest(UPDATE_NAME_REQUEST_API,requestObject);
    }

    public void sendImageUpdateRequestToFireBase(String imageUrl, String userId){
        mFireBase.child(ANCHOR_REQUEST).child(SETTINGS_REQUEST).push().setValue(new SettingsModel.UpdateImageUrl(imageUrl, userId));
    }

    public void sendLastUsedPrivacyOptionToFireBase(String option) {
        this.getNewFireBase(ANCHOR_SETTINGS, new String[]{getMyUserId(), AppLibrary.PRIVACY_LAST_USED_OPTION}).setValue(option);
    }

    public void loadInstituteMoment() {
        if(getMyUserModel().miscellaneous!=null && getMyUserModel().miscellaneous.institutionData!=null && getMyUserModel().miscellaneous.institutionData.momentId!=null){
            mFireBase.child(ANCHOR_MOMENTS).child(getMyUserModel().miscellaneous.institutionData.momentId).keepSynced(true);
            mFireBase.child(ANCHOR_MOMENTS).child(getMyUserModel().miscellaneous.institutionData.momentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    instituteMoment = dataSnapshot.getValue(MomentModel.class);
                    // add callback if latest institute is required
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    boolean institutionRefreshed;
    public void loadAllInstitutesData() {
       allInstitutesData = new ArrayList<>();
       final DatabaseReference firebase = FireBaseHelper.getInstance(mContext).getNewFireBase("institutions", null);
//        firebase.keepSynced(true);
        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    UserModel.InstitutionData data = dataSnapshot1.getValue(UserModel.InstitutionData.class);
                    allInstitutesData.add(data);
                    if (allInstitutesLoaded != null) {
                        allInstitutesLoaded.onInstituteLoaded();
                    }
                }

                if (!institutionRefreshed) {
                    institutionRefreshed = true;
                    firebase.keepSynced(true);
                    firebase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            allInstitutesData.clear();
                            for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                                UserModel.InstitutionData data = dataSnapshot1.getValue(UserModel.InstitutionData.class);
                                allInstitutesData.add(data);

                                if (allInstitutesLoaded != null) {
                                    allInstitutesLoaded.onInstituteLoaded();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

    }

    public void sendInstituteChangeRequest(UserModel.InstitutionData institute) {
        // update userModel locally and on Firebase
        // add requestNode to update college
        if (getMyUserModel().miscellaneous != null) {
            getMyUserModel().miscellaneous.institutionData = institute;
        }else {
            getMyUserModel().miscellaneous = new UserModel.Miscellaneous(institute);
        }
        getNewFireBase(ANCHOR_USERS, new String[]{getMyUserId(), MISCELLANEOUS, INSTUTUTION_DATA}).setValue(institute);
        HashMap<Object,Object> requestObject = new HashMap<>();
        requestObject.put(AppLibrary.USER_ID,getMyUserId());
        requestObject.put(AppLibrary.MOMENT_ID,institute.momentId);
        requestObject.put(AppLibrary.TYPE,institute.type);
        requestObject.put(AppLibrary.NAME,institute.name);
        postFireBaseRequest(INSTITUTE_CHANGE_REQUEST_API,requestObject);
        loadInstituteMoment();
    }

    InstituteLoadedCallback instituteLoadedCallback;

    public void setInstituteLoadedCallback(InstituteLoadedCallback instituteLoadedCallback){
        this.instituteLoadedCallback = instituteLoadedCallback;
    }

    public interface InstituteLoadedCallback {
        void onInstituteLoaded(MomentModel instituteMoment);
    }

    AllInstitutesLoaded allInstitutesLoaded;

    public void setAllInstitutesLoaded(AllInstitutesLoaded allInstitutesLoaded) {
        this.allInstitutesLoaded = allInstitutesLoaded;
    }

    public interface AllInstitutesLoaded {
        void onInstituteLoaded();
    }

}
