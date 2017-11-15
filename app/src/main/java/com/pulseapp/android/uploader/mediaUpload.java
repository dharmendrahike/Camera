package com.pulseapp.android.uploader;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.apihandling.RequestManager;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.MediaModel;
import com.pulseapp.android.util.AppLibrary;

import org.json.JSONObject;

import java.io.File;
import java.util.Random;

//import org.apache.http.NameValuePair;
//import org.apache.http.message.BasicNameValuePair;

/**
 * Created by Karthik on 1/23/16.
 */
public class mediaUpload implements FireBaseKEYIDS{

    private NotificationManager mNotificationManager;
    private Context context;
    private static final String TAG = "mediaUpload";
    private SharedPreferences prefs;
    private String picUrl;
    private float percentage;
    private UploadNotificationState uploadNotificationState;
    private String videoName;
    private int contentIntentRequestCode;
    private int notificationId;
    private TransferObserver upload;
    private String uniqueUploadName;
    private int transferId;
    private NotificationCompat.Builder mBuilder;
//    private ParseObject parseObject;
    private String streamId;
    private String pendingMediaUploadId;
    private FireBaseHelper fireBaseHelper;
    private MediaModel mediaObject;
    private UploadStatusCallbacks statusCallbacks;
    private static final int NUMBER_OF_RETRIES = 1;
    private int retry;

    public mediaUpload(Context context, String uniqueUploadName, String pendingMediaId, MediaModel mediaModel) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        fireBaseHelper = FireBaseHelper.getInstance(context);
        prefs = this.context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        picUrl = prefs.getString(AppLibrary.USER_PROFILE_PIC_URL, null);
        pendingMediaUploadId = pendingMediaId;
        mediaObject = mediaModel;
        uploadNotificationState = UploadNotificationState.NOT_STARTED;
        this.retry = 0;
        this.uniqueUploadName = uniqueUploadName; //Unique identifier to identify each and every upload
        statusCallbacks = (UploadStatusCallbacks)context;
    }

    public enum UploadNotificationState {
        /**
         * The upload process has not started yet.
         */
        NOT_STARTED,

        /**
         * Set before the initiation request is sent.
         */
        INITIATION_STARTED,

        /**
         * Set after a media file chunk is uploaded.
         */
        MEDIA_IN_PROGRESS,

        /**
         * Set after the complete media file is successfully uploaded.
         */
        MEDIA_COMPLETE,

        /**
         * Interrupted by app crash
         */
        INTERRUPTED,

        /**
         * Paused due to network conditions
         */
        PAUSED,

        /**
         * To check progress of upload from server to s3
         */
        CHECK_PROGRESS
    }

    public void startUpload(final File file, final String videoName) {
        Random random = new Random();
        final int contentIntentRequestCode = random.nextInt();
        final int notificationId = (int) System.currentTimeMillis();
        this.percentage = 0f;
        this.videoName = videoName;
        this.contentIntentRequestCode = contentIntentRequestCode;
        this.notificationId = notificationId;
        String bucketName;
        int p = mediaObject.url.lastIndexOf(".");
        final String extension = mediaObject.url.substring(p+1);
        if( p == -1 || !extension.matches("\\w+") ){
            /* file has no extension */
            return;
        }
        if (AppLibrary.getMediaType(mediaObject.url) == AppLibrary.MEDIA_TYPE_IMAGE) {
            if(AppLibrary.PRODUCTION_MODE)
                bucketName = "images/";
            else
                bucketName = "test/images/";
            this.upload = MasterClass.getTransferUtility().upload(AppLibrary.MediaHostBucket, bucketName + pendingMediaUploadId + "." + extension, file); //Starting upload
        } else {
            if (AppLibrary.PRODUCTION_MODE)
                bucketName = "videos/";
            else
                bucketName = "test/videos/";
            this.upload = MasterClass.getTransferUtility().upload(AppLibrary.MediaHostBucket, bucketName + pendingMediaUploadId + "." +extension, file);
        }
        this.transferId = upload.getId();
        this.uploadNotificationState = UploadNotificationState.INITIATION_STARTED; //ToDo: Add Recorded Stream ID API
        statusCallbacks.updateUploadingStatus(mediaObject,pendingMediaUploadId,MEDIA_UPLOADING_STARTED);
        upload.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                AppLibrary.log_e(TAG, "On State changed of media" + pendingMediaUploadId + " " + transferState + "\t :: " + transferState.toString());

                if (transferState == TransferState.IN_PROGRESS) {
                    //do nothing

                } else if (transferState == TransferState.COMPLETED) {
                    if (percentage > 0) {
                        uploadNotificationState = UploadNotificationState.CHECK_PROGRESS;
                        String finalUrl;
                        if (mediaObject.type == 1) {
                            if (AppLibrary.PRODUCTION_MODE)
                                finalUrl = AppLibrary.MediaHostUrl + "images/" + pendingMediaUploadId + "." + extension;
                            else
                                finalUrl = AppLibrary.MediaHostUrl + "test/images/" + pendingMediaUploadId + "." + extension;
                        }
                        else {
                            if (AppLibrary.PRODUCTION_MODE)
                                finalUrl = AppLibrary.MediaHostUrl + "videos/" + pendingMediaUploadId + "." + extension;
                            else
                                finalUrl = AppLibrary.MediaHostUrl + "test/videos/" + pendingMediaUploadId + "." + extension;
                        }
//                    fireBaseHelper.updateLocalPathForUploadedMedia(mediaObject.url,pendingMediaUploadId);
                        statusCallbacks.updateUploadingStatus(mediaObject, pendingMediaUploadId, MEDIA_UPLOADING_COMPLETE);
                        fireBaseHelper.updateOnCompletedUpload(context, pendingMediaUploadId, mediaObject, finalUrl);
                    }  else {
                        //Faulty transfer state callback - retry upload
                        if (retry < NUMBER_OF_RETRIES) {
                            retry++;
                            AppLibrary.log_e(TAG, "Retrying once");
                            MasterClass.getTransferUtility().cancel(transferId);
                            startUpload(file,videoName);
                        } else {
                            statusCallbacks.updateUploadingStatus(mediaObject, pendingMediaUploadId, MEDIA_UPLOADING_FAILED);
                            fireBaseHelper.updateOnFailedUpload(context, pendingMediaUploadId, mediaObject);
                            MasterClass.getTransferUtility().cancel(transferId);
                            AppLibrary.log_e(TAG, "Some S3Client error while uploading video");
                            uploadNotificationState = UploadNotificationState.INTERRUPTED;
                        }
                    }
                } else {
                    if (retry < NUMBER_OF_RETRIES) {
                        retry++;
                        AppLibrary.log_e(TAG, "Retrying once");
                        MasterClass.getTransferUtility().cancel(transferId);
                        startUpload(file,videoName);
                    } else {
                        statusCallbacks.updateUploadingStatus(mediaObject, pendingMediaUploadId, MEDIA_UPLOADING_FAILED);
                        fireBaseHelper.updateOnFailedUpload(context, pendingMediaUploadId, mediaObject);
                        MasterClass.getTransferUtility().cancel(transferId);
                        AppLibrary.log_e(TAG, "Some S3Client error while uploading video");
                        uploadNotificationState = UploadNotificationState.INTERRUPTED;
                    }
                } /*else if (transferState == TransferState.FAILED){
                    statusCallbacks.updateUploadingStatus(mediaObject,pendingMediaUploadId,MEDIA_UPLOADING_FAILED);
                    fireBaseHelper.updateOnFailedUpload(context,pendingMediaUploadId,mediaObject);
                    uploadNotificationState = UploadNotificationState.INTERRUPTED;
                } else if ((transferState != TransferState.CANCELED) && (transferState != TransferState.PENDING_CANCEL)) {
                    uploadNotificationState = UploadNotificationState.PAUSED;
                }*/
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                AppLibrary.log_e(TAG, "On Progress changed: " + bytesCurrent + "out of " + bytesTotal);
                percentage = 100.0f * bytesCurrent / bytesTotal;
                uploadNotificationState = UploadNotificationState.MEDIA_IN_PROGRESS;
            }

            @Override
            public void onError(int i, Exception e) {

                if (retry < NUMBER_OF_RETRIES) {
                    AppLibrary.log_e(TAG, "Retrying once");
                    retry++;
                    MasterClass.getTransferUtility().cancel(transferId);
                    startUpload(file,videoName);
                } else {
                    statusCallbacks.updateUploadingStatus(mediaObject, pendingMediaUploadId, MEDIA_UPLOADING_FAILED);
                    fireBaseHelper.updateOnFailedUpload(context, pendingMediaUploadId, mediaObject);
                    MasterClass.getTransferUtility().cancel(transferId);
                    AppLibrary.log_e(TAG, "Some S3Client error while uploading video: " + e.getMessage());
                    uploadNotificationState = UploadNotificationState.INTERRUPTED;
                }
            }
        });
    }

    public static void resumeUpload(int transferId) {
        AppLibrary.log_e(TAG, "Trying to resume upload");
        MasterClass.getTransferUtility().resume(transferId);
    }

//    private void sendNotification(int contentIntentRequestCode, int notificationId) {
//        AppLibrary.log_i(TAG, "Preparing to send notification");
//        int px = (int) TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                64,
//                context.getResources().getDisplayMetrics()
//        );
//
//        Bitmap picture = null;
//        String title;
//        String msg;
//        int progress;
//
//        try {
//            if (android.os.Build.VERSION.SDK_INT > 20) {
//                if (AppLibrary.checkStringObject(picUrl) != null) {
//                    if (picUrl.contains("https://"))
//                        picUrl = picUrl.replace("https://", "http://"); //Avoid https SSL handshake error in Notifications
//                    picture = Picasso.with(context).load(picUrl).resize(px, px).transform(new RoundedTransformation()).get();
//                } else {
//                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).transform(new RoundedTransformation()).get();
//                }
//            } else {
//                if (AppLibrary.checkStringObject(picUrl) != null) {
//                    if (picUrl.contains("https://"))
//                        picUrl = picUrl.replace("https://", "http://"); //Avoid https SSL handshake error in Notifications
//                    picture = Picasso.with(context).load(picUrl).resize(px, px).get();
//                } else {
//                    picture = Picasso.with(context).load("http://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg?sz=50").resize(px, px).get();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        Intent intent = new Intent(this.context, YoutubePlayerActivity.class);
//        intent.setPackage("com.instalively.android");
//        intent.setClass(context, YoutubePlayerActivity.class);
//        intent.putExtra("Notification", true);
//        intent.putExtra(AppLibrary.EVENT_SID, ""); //ToDo - Put Recorded Video SID
//
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//        Intent broadcast = new Intent();
//        broadcast.setPackage("com.instalively.android");
//        broadcast.setClass(this.context, ScreenshotReceiver.class);
//        broadcast.putExtra("TransferId", this.transferId);
//        broadcast.putExtra("NotificationId", notificationId);
//
//        PendingIntent contentIntent = PendingIntent.getActivity(context, contentIntentRequestCode,
//                intent, PendingIntent.FLAG_CANCEL_CURRENT);
//        PendingIntent interruptedBroadcastIntent;
//
//        if (mBuilder == null)
//            mBuilder = new NotificationCompat.Builder(context);
//
//        Notification notification = null;
//
//        switch (uploadNotificationState) {
//            case INITIATION_STARTED:
//                title = "Upload starting";
//                msg = String.format("Video '%s' will be uploaded soon. Please don't kill the app.", this.videoName);
//                interruptedBroadcastIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode, broadcast, PendingIntent.FLAG_CANCEL_CURRENT);
//
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(false)
//                        .setTicker(title)
//                        .setOngoing(true)
//                        .setProgress(0, 0, true)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentIntent(interruptedBroadcastIntent)
//                        .setContentText(msg).build();
//                break;
//
//            case NOT_STARTED:
//                //Do nothing
//                break;
//
//            case MEDIA_IN_PROGRESS:
//                progress = Math.round(percentage);
//                title = "Uploading video: " + progress + "%";
//                msg = String.format("Upload of '%s' in progress. Please don't kill the app.", this.videoName);
//                interruptedBroadcastIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode, broadcast, PendingIntent.FLAG_CANCEL_CURRENT);
//
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(false)
//                        .setTicker(title)
//                        .setOngoing(true)
//                        .setProgress(100, progress, false)
//                        .setOnlyAlertOnce(true)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentIntent(interruptedBroadcastIntent)
//                        .setContentText(msg).build();
//                break;
//
//            case MEDIA_COMPLETE:
//                title = "Video Shared";
//                msg = String.format("Video '%s' has been shared successfully.", this.videoName);
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(true)
//                        .setTicker(title)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setOngoing(false)
//                        .setVibrate(new long[]{1000, 1000}) //delay + vibrate + delay +... and so on
//                        .setProgress(0, 0, false)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentText(msg).setContentIntent(contentIntent).build();
////                notification.sound = Uri.parse("android.resource://"+ context.getPackageName()+ "/" + R.raw.tiny_bell_sms);
//                break;
//
//            case INTERRUPTED:
//                title = "Upload Interrupted";
//                msg = String.format("Upload of '%s' has been interrupted. Tap to Retry.", this.videoName);
////                broadcast.putExtra("Pending",true);
//                interruptedBroadcastIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode, broadcast, PendingIntent.FLAG_CANCEL_CURRENT);
//
//                //Create a new intent
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(false)
//                        .setTicker(title)
//                        .setProgress(0, 0, false)
//                        .setOngoing(true)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentText(msg).setContentIntent(interruptedBroadcastIntent).build();
////                notification.sound = Uri.parse("android.resource://"+ context.getPackageName()+ "/" + R.raw.tiny_bell_sms);
//                break;
//
//            case PAUSED:
//                title = "Upload Paused";
//                msg = String.format("Waiting for Internet connectivity to continue upload of '%s'. Tap to Retry.", this.videoName);
////                broadcast.putExtra("Pending",true);
//                interruptedBroadcastIntent = PendingIntent.getBroadcast(context, contentIntentRequestCode, broadcast, PendingIntent.FLAG_CANCEL_CURRENT);
//
//                //Create a new intent
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(false)
//                        .setTicker(title)
//                        .setProgress(0, 0, false)
//                        .setOngoing(true)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentText(msg).setContentIntent(interruptedBroadcastIntent).build();
//                break;
//
//            case CHECK_PROGRESS:
//                title = "Processing Video...";
//                msg = String.format("Video '%s' is processing...", this.videoName);
//                notification = mBuilder.setSmallIcon(R.drawable.public_event_notification)
//                        .setContentTitle(title)
//                        .setAutoCancel(false)
//                        .setTicker(title)
//                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
//                        .setLargeIcon(picture)
//                        .setOngoing(false)
//                        .setProgress(0, 0, false)
//                        .setColor(context.getResources().getColor(R.color.notification_icon_background))
//                        .setContentText(msg).build();
//                break;
//        }
//
//        mNotificationManager.notify(notificationId, notification);
//    }

//    private void FacebookUploadVideoRequest(final String bucketName, final String bucketPath) {
//        if (parseObject!=null) {
//            String name = parseObject.getString("Name");
//            String videoLength = parseObject.getString("Videolength");
//            String publish = parseObject.getString("Publish");
//            String pageId = null;
//            String tags = null;
//            if (parseObject.has("pageId"))
//                pageId = parseObject.getString("pageId");
//            if (parseObject.has("tags"))
//                tags = parseObject.getString("tags");
//
//            List<NameValuePair> params = new ArrayList<>();
//            params.add(new BasicNameValuePair("name", name));
//            params.add(new BasicNameValuePair("videoLength", videoLength));
//            if (Boolean.parseBoolean(publish))
//                params.add(new BasicNameValuePair("publish", "true"));
//            else
//                params.add(new BasicNameValuePair("publish", "false"));
//            params.add(new BasicNameValuePair("bucket", bucketName));
//            params.add(new BasicNameValuePair("path", bucketPath));
//            if (pageId != null)
//                params.add(new BasicNameValuePair("pageId", pageId));
//            if (tags != null)
//                params.add(new BasicNameValuePair("tags", tags));
//
//            AppLibrary.log_d(TAG, "Upload video request starting");
//            RequestManager.makePostRequest(context, RequestManager.FINISH_RECORDED_EVENT_REQUEST, RequestManager.FINISH_RECORDED_EVENT_RESPONSE
//                    , null, params, finishUploadVideoCallback);
//        } else {
//            AppLibrary.log_e(TAG, "Parse object is null in FacebookUploadVideoRequest");
//        }
//    }

//    private RequestManager.OnRequestFinishCallback finishUploadVideoCallback = new RequestManager.OnRequestFinishCallback() {
//        @Override
//        public void onBindParams(boolean success, Object response) {
//            final JSONObject object = (JSONObject) response;
//            try {
//                if (success && object!=null) {
//                    if (object.getString("error").equalsIgnoreCase("false")) {
//                        streamId = object.getString("value");
//                        parseObject.put("StreamId", streamId);
//                        parseObject.put("Uploaded", "true");
//                        parseObject.pinInBackground();
////                        checkUploadProgress(streamId);
//                        AppLibrary.log_d(TAG, "Upload video request successful. Response:- " + object.getString("value"));
//                    } else {
//                        // handle the error
////                        FacebookUploadVideoRequest("instalively.data", "videos/" + uniqueUploadName + ".mp4");
//                        AppLibrary.log_d(TAG, "Upload video request failed. Response:- " + object.getString("value"));
//                    }
//                } else {
//                    // handle the error
////                    FacebookUploadVideoRequest("instalively.data", "videos/" + uniqueUploadName + ".mp4");
//                }
//            } catch (Exception e) {
//                // handle the error
////                FacebookUploadVideoRequest("instalively.data", "videos/" + uniqueUploadName + ".mp4");
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public boolean isDestroyed() {
//            return false;
//        }
//    };

//    private void checkUploadProgress(String streamId) {
//        List<NameValuePair> pairs = new ArrayList<>();
//        pairs.add(new BasicNameValuePair("streamId", streamId));
//        RequestManager.makeGetRequest(context, RequestManager.CHECK_VIDEO_PROGRESS_REQUEST, RequestManager.CHECK_VIDEO_PROGRESS_RESPONSE
//                , pairs, checkVideoProgressCallback);
//    }

    private RequestManager.OnRequestFinishCallback checkVideoProgressCallback = new RequestManager.OnRequestFinishCallback() {
        @Override
        public void onBindParams(boolean success, Object response) {
            final JSONObject object = (JSONObject) response;
            try {
                if (success && object!=null) {
                    if (object.getString("error").equalsIgnoreCase("false")) {
                        AppLibrary.log_d(TAG, "Video progress request successful. Response:- " + object.toString());

                        if (object.getJSONObject("value").getString("status").equalsIgnoreCase("readyToShare")) {
                            uploadNotificationState = UploadNotificationState.MEDIA_COMPLETE;
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
////                                    sendNotification(contentIntentRequestCode, notificationId);
//                                }
//                            });

//                            parseObject.unpinInBackground(); //Safe to remove the object as all the work regarding it is done

                            //And Finally, safe to quit this notification posting thread and rest in peace :)
//                            if (thread != null && thread.isAlive())
//                                thread.quitSafely();

                        } else {
//                            handler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
////                                    checkUploadProgress(streamId);
//                                }
//                            }, 1000);
                        }
                    } else {
                        // handle the error
                        AppLibrary.log_d(TAG, "Video progress request error. Response:- " + object.toString());
//                        checkUploadProgress(streamId);
                    }
                } else {
//                    checkUploadProgress(streamId);
                }
            } catch (Exception e) {
//                checkUploadProgress(streamId);
                e.printStackTrace();
            }
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }
    };

    public interface UploadStatusCallbacks{
        void updateUploadingStatus(MediaModel mediaModel,String mediaId,int status);
    }

}