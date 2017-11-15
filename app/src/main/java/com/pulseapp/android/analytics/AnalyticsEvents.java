package com.pulseapp.android.analytics;

/**
 * Created by Karthik on 26/09/16.
 */

public interface AnalyticsEvents {

    //App Login/
    String APP_LOGIN_STARTED = "appLoginStarted";
    String APP_LOGIN_CLICK = "appLoginClick";
    String APP_LOGIN_FACEBOOK_RESPONSE = "appLoginFacebookResponse";
    String APP_LOGIN_API_RESPONSE = "appLoginApiResponse";
//    String APP_LOGIN_FIREBASE_AUTH = "appLoginFirebaseAuth";
    String APP_LOGIN_SUCCESSFUL = "appLoginSuccessful";
    String APP_ONBOARDING_STARTING = "appOnboardingStarting";
    String APP_ONBOARDING_USERNAME_DONE = "appOnboardingUsernameDone";
    String APP_ONBOARDING_FRIEND_SELECTION_DONE = "appOnboardingFriendSelectionDone";
    String APP_ONBOARDING_SUCCESSFUL = "appOnboardingSuccessful";
    String INVITE_FRIENDS_ONBOARDING = "inviteFriendsOnboarding";

    //App opens
    String APP_OPEN = "appOpen";
    String APP_OPEN_VIA_NOTIFICATION = "appOpenViaNotification";
    String NOTIFICATION_RECEIVED = "notificationReceived";
    String APP_EXIT = "appExit";

    //Creation flow
    String CLICK_PICTURE = "clickPicture";
    String RECORD_VIDEO = "recordVideo";
    String SELECT_GALLERY_IMAGE = "selectGalleryImage";
    String OPEN_CANVAS = "openCanvas";
    String WRITE_ON_CANVAS = "writeOnCanvas";
    String DOUBLE_SHOT = "doubleShot";

    //Filters
    String APPLY_FILTER = "applyFilter";
    String SEARCH_STICKER = "searchSticker";
    String SELECT_STICKER = "selectSticker";
    String SEARCH_LOCATION_STICKER = "searchLocationSticker";
    String SELECT_LOCATION_STICKER = "selectLocationSticker";
    String ADD_DEFAULT_CAPTION = "addDefaultCaption";
    String TOGGLE_TEXT_CAPTION = "toggleTextCaption";
    String CANCEL_MEDIA = "cancelMedia";
    String SKETCH = "sketch";

    //Posting
    String QUICK_POST = "quickPost"; //One-click post to MyStream //Add custom argument of momentId
    String QUICK_WHATSAPP = "quickWhatsapp";
    String SAVE_MEDIA = "saveMedia";
    String SEND_MEDIA = "sendMedia";
    String POST_MEDIA = "postMedia";
    String POST_TO_MY_STREAM = "postToMyStream"; //Add custom argument of momentId
    String POST_TO_MY_STREAM_PRIVACY = "postToMyStreamPrivacy"; //Add custom argument of momentId
    String POST_TO_COLLEGE_STREAM = "postToCollegeStream"; //Add custom argument of college momentId
    String POST_TO_PUBLIC_STREAM = "postToPublicStream"; //Add custom argument of number of public streams + Anonymous/non-anonymous
    String POST_TO_PUBLIC_PRIVACY = "postToPublicPrivacy";
    String POST_TO_FACEBOOK = "postToFacebook";


    //Upload
    String UPLOADING_STARTED = "uploadingStarted";
    String UPLOADING_COMPLETE = "uploadingComplete";
    String UPLOADING_FAILED = "uploadingFailed";
    String RETRY_UPLOAD = "retryUpload";

    //My Stream
    String WATCH_MY_STREAM = "watchMyStream"; //Argument is MomentId
    String WATCH_MY_MEDIA = "watchMyMedia"; //Argument is MediaId
    String WATCH_MY_CONTRIBUTED_STREAM = "watchMyContributedStream"; //Argument is MomentId
    String WATCH_MY_CONTRIBUTED_MEDIA = "watchMyContributedMedia"; //Argument is MediaId
    String DELETE_MY_MEDIA = "deleteMyMedia"; //Argument is MediaId
    String SAVE_MY_MEDIA = "saveMyMedia"; //Argument is MediaId

    //Watch Public Stream
    String PUBLIC_STREAM_CLICK = "publicStreamClick";
    String PUBLIC_STREAM_OPEN = "publicStreamOpen";
    String PUBLIC_STREAM_MINIMIZE = "publicStreamMinimize";
    String NEXT_PUBLIC_MEDIA = "nextPublicMedia"; //Every tap counts as a call to next
    String PUBLIC_STREAM_COMPLETE = "publicStreamComplete"; //Finished watching the whole stream
    String PUBLIC_STREAM_CTA = "publicStreamCta"; //Call to action on a public stream

    //Follow
    String FOLLOW_PUBLIC_USER = "followPublicUser";
    String UNFOLLOW_PUBLIC_USER = "unfollowPublicUser";

    //Watch Follower Stream
    String FOLLOWER_STREAM_CLICK = "followerStreamClick";
    String FOLLOWER_STREAM_OPEN = "followerStreamOpen";
    String FOLLOWER_STREAM_MINIMIZE = "followerStreamMinimize";
    String NEXT_FOLLOWER_MEDIA = "nextFollowerMedia"; //Every tap counts as a call to next
    String FOLLOWER_STREAM_COMPLETE = "followerStreamComplete"; //Finished watching the whole stream

    //Provide location access for Around You
    String ENABLE_LOCATION_STREAMS = "enableLocationStreams";

    //Select Institution
    String SELECT_INSTITUTION = "selectInstitution";
    String INSTITUTION_NOT_FOUND = "institutionNotFound";

    //Watch Friend Stream
    String FRIEND_STREAM_CLICK = "friendStreamClick";
    String FRIEND_STREAM_OPEN = "friendStreamOpen";
    String FRIEND_STREAM_MINIMIZE = "friendStreamMinimize";
    String NEXT_FRIEND_MEDIA = "nextFriendMedia"; //Every tap counts as a call to next
    String FRIEND_STREAM_COMPLETE = "friendStreamComplete"; //Finished watching the whole stream

    //Share
    String SHARE_PUBLIC_STREAM = "sharePublicStream";
    String SHARE_MY_STREAM = "shareMyStream";
    String SHARE_PROFILE = "shareProfile";
    String SHARE_MY_USERNAME = "shareMyUsername";

    //Search
    String GLOBAL_SEARCH = "globalSearch";
    String USER_SEARCH = "userSearch";

    //Friends
    String ADD_FRIEND = "addFriend";
    String ACCEPT_FRIEND = "acceptFriend";
    String IGNORE_FRIEND = "ignoreFriend";
    String INVITE_FRIENDS = "inviteFriends";


    //Arguments
    String USER_ID = "userId";
    String USER_HANDLE = "userHandle";
    String STREAM_ID = "streamId";
    String STREAM_NAME = "streamName";
    String STREAM_HANDLE = "streamHandle";
    String STREAM_TYPE = "streamType";
    String STREAM_SUBTYPE = "streamSubType"; //LocationBased, Non-LocationBased
    String NUMBER = "number";
    String POSTING_TYPE = "postingType"; //Anonymous or non-anonymous

    String STICKER_ID = "stickerId";
    String STICKER_URL = "stickerUrl";
    String NOTIFICATION_TYPE = "notificationType";
    String NOTIFICATION_TITLE = "notificationTitle";
    String INSTITUTION_NAME = "institutionName";
    String FILTER_NAME = "filterName";
}
