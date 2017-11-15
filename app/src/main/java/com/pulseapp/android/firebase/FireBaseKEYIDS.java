package com.pulseapp.android.firebase;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by deepankur on 28/3/16.
 */
public interface FireBaseKEYIDS {

    String fireBaseURL = AppLibrary.PRODUCTION_MODE ? "https://pulse-production-93968.firebaseio.com/" : "https://glaring-heat-48.firebaseio.com/pulse-staging";
    String ANCHOR_USERS = "users";
    String ANCHOR_SOCIALS = "socials";
    String ANCHOR_ROOMS = "rooms";
    String ANCHOR_MOMENTS = "moments";
    String ANCHOR_MEDIA = "medias";
    String ANCHOR_SETTINGS = "settings";
    String ANCHOR_LOCAL_DATA = "localData";
    String ANCHOR_REQUEST = "requests";
    String ANCHOR_TEMPLATES = "templates";
    String ANCHOR_STICKER_CATEGORY = "stickerCategory";
    String ANCHOR_STICKERS = "stickers";
    String ANCHOR_JOBS = "jobs";
    String ANCHOR_APP_SETTINGS = "appSettings"; //This node is supposed to sync with firebase remote config
    String ANCHOR_QUERIED_INST = "queriedInstitution";
    String ANCHOR_QUERIED_STICKER = "queriedSticker";
    String ANCHOR_INSTITUTIONS = "institutions";
    String INSTITUTE_CHANGE_REQUEST = "institutes";
    String MEDIA_POSTED = "mediaPosted";
    String MEDIA_ADDED = "mediaAdded";
    String LATEST_ANDROID_APPLICATION_VERSION = "LatestAndroidAppVersionCode";

    String REQUEST_RECEIVED = "requestReceived";
    String REQUEST_IGNORED = "requestIgnored";
    String REQUEST_SENT = "requestSent";
    String ROOMS = "rooms";
    String CONTRIBUTED_MOMENTS = "moments";
    String GROUPS = "groups";
    String NOTIFICATION = "notification";
    String CUSTOM_FRIEND_LIST = "customFriendList";
    String IGNORED_LIST = "ignoredList";
    String BLOCKED_FRIEND = "blocked";
    String MESSAGES = "messages";
    String MEMBERS = "members";
    String MESSAGE_COUNT = "msgCount";
    String MOMENT_COUNT = "momentCount";
    String UNSEEN_MESSAGE_ID = "unSeenMessageId";
    String MESSAGE_ID = "messageId";
    String VIEW_DETAILS = "viewerDetails";
    String VIEWERS = "viewers";
    String VIEWED = "viewed";
    String MEDIA_UPLOADING = "mediaUploadingStatus";
    String UPDATED_AT = "updatedAt";
    String DETAIL = "detail";
    String ADMIN = "admin";
    String ROOM_TYPE = "type";
    String MESSAGE_TYPE = "type";
    String FOLLOWED_ROOM = "followedRoom";
    String FLAGS = "flags";
    String DELETION_CHECKED = "deletionChecked";
    String ADDED_TO = "addedTo";
    String MESSAGE_ROOM = "messageRooms";
    String MOMENT_ROOM = "momentRooms";
    String ROOM_ID = "roomId";
    String MEMBER_ID = "memberId";
    String MOMENT_ID = "momentId";
    String PENDING_MEDIA = "pendingMedias";
    String DEFAULT_PROFILE_PIC = "http://www.lcfc.com/images/common/bg_player_profile_default_big.png";
    String FACEBOOK_FRIENDS = "facebookFriends";
    String MOMENTS = "moments";
    String MEDIA = "medias";
    String CONTRIBUTED_MEDIA = "contributedMedias";
    String CREATED_AT = "createdAt";
    String UPLOADED_AT = "uploadedAt";
    String ADDED_AT = "addedAt";
    String MEDIA_DOWNLOAD = "mediaDownload";
    String AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE = "mediaDownload";//to be replaced by above after testing properly

    String URL = "url";
    String LOCAL_MEDIA_STATUS = "status";
    String MOMENTS_AROUND_YOU_LOCAL_REF = "momentsAroundYou";
    String EXPIRY_TYPE = "expiryType";
    String MEDIA_ID = "mediaId";
    String DEFAULT_MEDIA = "jkhsdguiw";
    String USERID = "userId";
    String USER = "user";
    String PENDING_GROUP_REQUEST = "pendingGroupRequest";
    String GROUP_REQUEST_IGNORED = "groupRequestIgnored";
    String EXPIRY_TIME = "expiryTime";
    String TEMPLATES = "templates";
    String STICKERS = "stickers";
    String PENDING_MEMBERS = "pendingMembers";
    String TYPE = "type";
    String SCREENSHOTS = "screenShots";
    String SCREENSHOTTED = "screenShotted";
    String SCREENSHOT_TEXT = "screenshot";
    String TEMPLATE_DOWNLOAD = "templateDownload";
    String STICKER_DOWNLOAD = "stickerDownload";
    String DOWNLOAD_STATUS = "status";
    String LOCAL_URI = "localUri";
    String CAMERA_STICKERS = "camera";
    String CLEAR_SCREEN = "clearScreen";
    String EMOTICON_ASSET_DIR_NAME = "emoticons";
    String CHAT_CAMERA_STICKERS_DIR_NAME = "chatCameraStickers";
    String IS_VIDEO = "isVideo";
    String IMAGE_URL = "imageUrl";
    String HANDLE = "handle";
    String NAME = "name";
    String AUTO_MODERATE = "autoModerate";
    String MISCELLANEOUS = "miscellaneous";
    String INSTUTUTION_DATA = "institutionData";
    String UNREAD_ROOMS = "unReadRoomsCount";
    String SHOW_REQUEST_NOTIFICATION = "showRequestNotification";
    String USER_BIRTHDAY = "birthday";
    String BIRTHDAY_PARTY_FLAG = "birthdayPartyFlag";
    String PUSH_NOTIFICATION_FLAG = "pushNotification";
    String TRAVEL_MODE = "travelMode";
    String AUTO_SAVE_MODE = "autoSaveMode";
    String MY_CONTRIBUTIONS = "myContributions";
    String CONTRIBUTED_STATE = "state";
    String MOMENT_NAME = "name";
    String MOMENT_IMAGE = "thumbnailUrl";
    String BLOCKED_LIST = "blockedList";
    String SETTINGS_REQUEST = "settings";
    String PRIVACY_LAST_USED_OPTION = "lastUsedPrivacy";
    String REQUEST_USER_ID = "reqUserId";
    String USER_ID = "userId";
    String DETAILS = "details";
    String DESCRIPTION = "description";
    String TIMER = "fixedTimer";
    String PRIVACY = "privacy";
    String DURATION = "duration";
    String MEDIA_DATA = "mediaData";
    String SOURCE = "source";
    String ANDROID = "android";
    String ISARTICLE = "isArticle";
    String ANONYMOUS = "anonymous";
    String FRIEND = "friend";
    String FOLLOW = "follow";
    String FOLLOWED = "followed";
    String SOCIAL_COUNT = "socialCount";
    String UNFOLLOW = "unfollow";
    String FOLLOWED_BY = "followedBy";
    String RELATIONS = "relations";
    String INSTITUTION = "institution";
    String NOTIFICATION_ID = "notificationId";
    String NOTIFICATION_TOKEN = "notificationToken";
    String VIEWED_AT = "viewedAt";
    String MEDIA_VIEW = "mediaViewed";
    String UPDATE_VIEWER = "updateViewer";
    String UPDATE_FOR_SCREENSHOT = "updateScreenShot";
    String PALLET_COLOR = "palleteColor";
    String USER_STICKER_ICON = "userStickerIcon";
    String MEDIA_TIME = "mediaTime";
    String WEB_VIEWS = "webViews";
    String TOKEN = "token";
    String MEDIA_TEXT = "mediaText";
    String MEDIA_TYPE = "mediaType";
    String CONTRIBUTABLE_ANYWHERE = "contributableNoLocation";

    String PRIVACY_ALL_FRIENDS_AND_FOLLOWERS = "allFriendsAndFollowers";
    String PRIVACY_ALL_FRIENDS = "allFriends";
    String PRIVACY_FRIENDS_EXCEPT = "friendsExcept";

    int UPDATE_NAME = 0;
    int UPDATE_IMAGE = 1;

    String[] IMAGE_FILE_EXTENSION = {".webp", ".png", ".jpg", ".jpeg"};
    String[] VIDEO_FILE_EXTENSION = {".mp4"};
    int STICKER_ERROR_DOWNLOADING = -1;
    int STICKER_NOT_DOWNLOADED = 11;
    int STICKER_DOWNLOAD_IN_PROGRESS = 22;
    int STICKER_DOWNLOADED = 33;
    int DEFAULT_INTEGER_VALUE = 0;

    //loginSteps
    int ON_BOARDING_NOT_STARTED = 0;
    int USER_NAME_SELECTION_DONE = 1;
    int FRIENDS_INVITING_DONE = 2;
    int INSTITUTION_PROVING_DONE = 3;//on boarding done


    //for debugging only
    String ON_CHILD_ADDED = " onChildAdded ";
    String ON_CHILD_REMOVED = " onChildRemoved ";
    String ON_CHILD_MOVED = " onChildMoved ";
    String ON_CHILD_CHANGED = " onChildChanged ";
    String ON_CANCELLED = " onCancelled ";

    // Media Views
    String MEDIA_VIEWS = "totalViews";


    String FRIENDS = "friends";
    int FRIEND_ROOM = 1;
    int GROUP_ROOM = 2;
    int FAVORITE_ROOM = 3;

    // expiry Type
    int QUICK_PEEK = 1;
    int VIEW_ONCE = 2;
    int VIEW_FOR_A_DAY = 3;

    String MOMENT_VIEW_TYPE = "momentViewType";
    int UNSEEN_FRIEND_MOMENT_RECYCLER = 88;
    int UNSEEN_FOLLOWER_MOMENT_RECYCLER = 90;
    int SEEN_FRIEND_MOMENT_RECYCLER = 99;
    int SEEN_FOLLOWER_MOMENT_RECYCLER = 100;
    int FAVOURITE_MOMENT_RECYCLER = 104;
    int AROUND_YOU_MOMENT_RECYCLER = 324;
    int ARTICLE_RECYCLER = 777;

    // MOMENT TYPE
    int MY_MOMENT = 1;
    int CUSTOM_MOMENT = 2;

    int MEDIA_TYPE_IMAGE = 1;
    int MEDIA_TYPE_VIDEO = 2;

    int ERROR_DOWNLOADING_MEDIA = -1;
    int MEDIA_DOWNLOAD_NOT_STARTED = 0;
    int MEDIA_DOWNLOADING = 1;
    int MEDIA_DOWNLOAD_COMPLETE = 2;
    int MEDIA_VIEWED = 3;

    int MEDIA_UPLOADING_FAILED = 1;
    int MEDIA_UPLOADING_STARTED = 2;
    int MEDIA_UPLOADING_COMPLETE = 3;
    int UPLOADING_NOT_READY_VIDEO = 4;

    // Moment Seen status
    int UNSEEN_MOMENT = 0;
    int DOWNLOADING_MOMENT = 1;
    int READY_TO_VIEW_MOMENT = 2;
    int READY_AND_SEEN_MOMENT = 3;
    int SEEN_MOMENT = 4;/// in HomeMomentViewModel it will represent the moments seen in current session and not the overall status
    //after killing the application
    int SEEN_BUT_DOWNLOADING = 5;

    int DOWNLOAD_MY_MOMENT_MEDIA_FAILED = 10;
    int DOWNLOADING_MY_MOMENT_MEDIA = 11;
    int DOWNLOADED_MY_MOMENT_MEDIA = 12;

    String MEDIA_UPLOADING_FAILED_TEXT = "Uploading Failed, Tap to retry";
    String MEDIA_UPLOADING_TEXT = "Uploading...";
    String MEDIA_DOWNLOADING_FAILED_TEXT = "Downloading Failed, Tap to retry";
    String MEDIA_DOWNLOADING_TEXT = "Downloading...";

    //Moment-flags 0 - Normal, 1-group, 2 - Live, 3-featured
    int MOMENT_FLAG_NORMAL = 0;
    int MOMENT_FLAG_GROUP = 1;
    int MOMENT_FLAG_LIVE = 2;
    int MOMENT_FLAG_FEATURED = 3;

    // Member Types
    String SENDING_FAILED_TEXT = "Sending Failed, Tap to retry";
    int SENDING_FAILED = -2;
    int SENDING_MEDIA = -1;
    int NO_STATUS = 0;
    int SENT_MEDIA = 1;
    int SENT_CHAT = 2;

    int SEEN_MEDIA = 3;
    int SEEN_CHAT = 4;


    int SCREEN_SHOTTED_CHAT = 5;
    int SCREEN_SHOTTED_MEDIA = 6;

    int NEW_MEDIA = 7;
    int NEW_TEXT = 8;
    int MEDIA_WITH_TEXT = 9;
    int GROUP_CREATED = 10;
    int MEMBER_JOIN = 11;// (message)
    int YOU_JOIN = 12; //(message)
    int ADMIN_REMOVED_MEMBER = 13;
    int USER_LEFT_GROUP = 14;

    int MEDIA_INACTIVE = 1;
    int MEDIA_ACTIVE = 2;
    int MEDIA_HISTORY = 3;

    int SHARE_PRIVACY_ALL_FRIENDS = 1;
    int SHARE_PRIVACY_EXCEPT_FRIENDS = 2;
    int SHARE_PRIVACY_CUSTOM_FRIEND_LIST = 3;

    int MESSAGE_TYPE_TEXT = 1;
    int MESSAGE_TYPE_MEDIA = 2;
    int MESSAGE_TYPE_MEDIA_WITH_TEXT = 3;
    int SYSTEM_MESSAGE = 4;

    // notification types
    int SEND_FRIEND_REQUEST = 1;
    int ACCEPT_FRIEND_REQUEST = 2;
    int SEND_GROUP_REQUEST = 3;
    int NEW_MESSAGE_REQUEST = 4;

    //message expiry types
    int EXPIRY_TYPE_QUICK_PEEK = 1;
    int EXPIRY_TYPE_VIEW_ONCE = 2;
    int EXPIRY_TYPE_24_HOURS = 3;
    int EXPIRY_TYPE_AUTO = 4;
    int MESSAGE_EXPIRED_BUT_NOT_VIEWED = 5;
    int REMOVED_UPON_ROOM_OPEN = 6; // Can not be opened upon clicking
    int VIEW_ONCE_AND_VIEWED = 7;


    int MOMENT_SOURCE_MY_MOMENT = 1;
    int MOMENT_SOURCE_CUSTOM_MOMENT = 2;
    int INSTITUTION_ACTIVE = 1;

    // For all the request node api's
    String ACCEPT_FRIEND_REQUEST_API = "acceptFriendRequest";
    String SEND_FRIEND_REQUEST_API = "sendFriendRequest";
    String INSTITUTE_CHANGE_REQUEST_API = "instituteChangeRequest";
    String UPDATE_NAME_REQUEST_API = "updateNameRequest";
    String UPDATE_JOB_FOR_PUBLIC_MOMENT_CONTRIBUTION = "updateJobForPublicMomentContribution";
    String UPDATE_MEDIA_COMPLETE = "updateMediaComplete";
    String FOLLOW_USER = "followUser";
    String UNFOLLOW_USER = "unFollowUser";
    String UPDATE_TOKEN = "updateToken";
    String FACEBOOK_NODE = "facebook";
    String UPDATE_SCREENSHOT = "mediaScreenShotted";
    String UPDATE_FACEBOOK_REQUEST = "updateFacebookRequest";
    String CONTRIBUTABLE_MOMENTS = "contributableMoments";
    String THUMBNAIL = "thumbnail";
    String MONGO_ID = "_id";
    String TOTAL_VIEWS = "totalViews";
    String VAT_TEXT = "text";
    String CTA_VALUE = "value";
}
