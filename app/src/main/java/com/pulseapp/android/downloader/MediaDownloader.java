package com.pulseapp.android.downloader;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.MediaModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;

/**
 * Created by user on 5/10/2016.
 */
public class MediaDownloader implements FireBaseKEYIDS {

    private static final String TAG = "MediaDownloader";
    private Context mContext;
    private HandlerThread downloaderThread;
    private Handler downloadHandler;
    private String mediaUrl;
    private TransferObserver download;
    private int transferId;
    private String mediaStoragePath;
    private String mediaId;
    private float percentage;


    public MediaDownloader(Context context) {
        mContext = context;
//        this.downloaderThread = new HandlerThread("downloadMediaThread");
//        this.downloaderThread.start();
//        this.downloadHandler = new Handler(this.downloaderThread.getLooper());
        validateFileSystem();
    }

    private void validateFileSystem() {
        mediaStoragePath = AppLibrary.getFilesDirectory(MasterClass.getGlobalContext());
        File mediaDir = new File(mediaStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();
    }

    // FOR MOMENT MEDIA DOWNLOAD
//    public void startDownload(final MomentModel.Media media) {
//        this.mediaUrl = media.url;
//        mediaId = media.mediaId;
//        this.percentage = 0f;
//        int p = mediaUrl.lastIndexOf(".");
//        String extension = mediaUrl.substring(p + 1);
//        if (p == -1 || !extension.matches("\\w+")) {
//            /* file has no extension */
//            return;
//        }
//        mediaStoragePath = mediaStoragePath + mediaId + "." + extension;
//        File file = new File(mediaStoragePath);
//        String key;
//        try {
//            key = this.mediaUrl.split(AppLibrary.MediaHostBucket + "/")[1];
//            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
//        } catch (Exception e) { //Temporary to support shifting to new bucket
//            /**
//             * also using same at
//             * {@link DynamicDownloader}
//             */
//
//            key = this.mediaUrl.split("pulse.resources/")[1];
//            /**
//             * fixme @ above line
//             * E/UncaughtException: java.lang.ArrayIndexOutOfBoundsException: length=1; index=1
//             at com.pulseapp.android.downloader.MediaDownloader.startDownload(MediaDownloader.java:73)
//             at com.pulseapp.android.firebase.FireBaseHelper.downloadMoment(FireBaseHelper.java:2095)
//             at com.pulseapp.android.fragments.MomentListFragment$2.onItemClick(MomentListFragment.java:127)
//             at com.pulseapp.android.adapters.HomeMomentAdapter$GestureListener.onSingleTapConfirmed(HomeMomentAdapter.java:492)
//             at android.view.GestureDetector$GestureHandler.handleMessage(GestureDetector.java:273)
//             at android.os.Handler.dispatchMessage(Handler.java:102)
//             at android.os.Looper.loop(Looper.java:136)
//             */
//            this.download = MasterClass.getTransferUtility_US().download("pulse.resources", key, file); //Starting upload
//        }
//
////        if (AppLibrary.getMediaType(mediaUrl) == AppLibrary.MEDIA_TYPE_IMAGE) {
////            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
////        } else {
////            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file);
////        }
//        this.transferId = this.download.getId();
//        this.download.setTransferListener(new TransferListener() {
//            @Override
//            public void onStateChanged(int i, TransferState transferState) {
//                AppLibrary.log_d(TAG, "onStateChanged -" + transferState.toString());
//                if (transferState.toString().equals("IN_PROGRESS")) {
//                    AppLibrary.log_d(TAG, "MEDIA DOWNLOAD IN_PROGRESS");
////                    FireBaseHelper.getInstance(mContext).updateDownloadStatus(media.momentId, media.mediaId, MEDIA_DOWNLOADING);
//                } else if (transferState.toString().equals("COMPLETED")) {
//                    AppLibrary.log_d(TAG, "MEDIA DOWNLOAD COMPLETED");
////                    FireBaseHelper.getInstance(mContext).updateLocalPath(media.momentId, mediaId, mediaStoragePath);
//                    if (percentage > 0) {
//                        FireBaseHelper.getInstance(mContext).updateDownloadStatus(media.momentId, media.mediaId, MEDIA_DOWNLOAD_COMPLETE, mediaStoragePath);
//                    } else {
//                        MasterClass.getTransferUtility().cancel(transferId);
//                        FireBaseHelper.getInstance(mContext).onMomentMediaDownloadFailed(media.momentId);
//                    }
//                } else {
//                    MasterClass.getTransferUtility().cancel(transferId);
//                    FireBaseHelper.getInstance(mContext).onMomentMediaDownloadFailed(media.momentId);
//                }
//            }
//
//            @Override
//            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
//                AppLibrary.log_e(TAG, "On Progress changed: " + bytesCurrent + "out of " + bytesTotal);
//                percentage = 100.0f * bytesCurrent / bytesTotal;
//            }
//
//            @Override
//            public void onError(int i, Exception e) {
//                FireBaseHelper.getInstance(mContext).onMomentMediaDownloadFailed(media.momentId);
//                MasterClass.getTransferUtility().cancel(transferId);
//                e.printStackTrace();
//            }
//        });
//    }
//
    // FOR CHAT MEDIA DOWNLOAD
    public void startDownload(final RoomsModel.Messages message, final String mediaId, final MediaModel mediaModel, final FireBaseHelper.DownloadStatusCallbacks statusCallbacks) {
        String mediaUrl = mediaModel.url;
        int p = mediaUrl.lastIndexOf(".");
        this.percentage = 0f;
        String extension = mediaUrl.substring(p + 1);
        if (p == -1 || !extension.matches("\\w+")) {
            /* file has no extension */
            return;
        }
        mediaStoragePath = mediaStoragePath + mediaId + "." + extension;
        File file = new File(mediaStoragePath);
        String key;
        try {
            key = mediaUrl.split(AppLibrary.MediaHostBucket + "/")[1];
            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
        } catch (Exception e) { //Temporary to support shifting to new bucket
            key = mediaUrl.split("pulse.resources/")[1];
            this.download = MasterClass.getTransferUtility_US().download("pulse.resources", key, file); //Starting upload
        }

//        if (AppLibrary.getMediaType(mediaUrl) == AppLibrary.MEDIA_TYPE_IMAGE) {
//            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
//        } else {
//            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file);
//        }
        this.transferId = this.download.getId();
        this.download.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                AppLibrary.log_d(TAG, "onStateChanged -" + transferState.toString());
                if (transferState.toString().equals("IN_PROGRESS")) {
                    AppLibrary.log_d(TAG, "MEDIA DOWNLOAD IN_PROGRESS "+mediaId);
//                    statusCallbacks.updateDownloadingStatus(mediaId,MEDIA_DOWNLOADING);
                } else if (transferState.toString().equals("COMPLETED "+mediaId)) {
                    AppLibrary.log_d(TAG, "MEDIA DOWNLOAD COMPLETED "+mediaId);
                    if (percentage>0) {
                        FireBaseHelper.getInstance(mContext).updateLocalPathForChatMedia(mediaModel, message, mediaStoragePath, MEDIA_DOWNLOAD_COMPLETE);
                        statusCallbacks.updateDownloadingStatus(mediaId, MEDIA_DOWNLOAD_COMPLETE);
                    } else {
                        MasterClass.getTransferUtility().cancel(transferId);
                        statusCallbacks.updateDownloadingStatus(mediaId,ERROR_DOWNLOADING_MEDIA);
                    }
                } else {
                    MasterClass.getTransferUtility().cancel(transferId);
                    statusCallbacks.updateDownloadingStatus(mediaId,ERROR_DOWNLOADING_MEDIA);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                percentage = 100.0f * bytesCurrent / bytesTotal;
            }

            @Override
            public void onError(int i, Exception e) {
//                statusCallbacks.updateDownloadingStatus(mediaId,ERROR_DOWNLOADING_MEDIA);
//                MasterClass.getTransferUtility().cancel(transferId);
                e.printStackTrace();
            }
        });
    }

    // FOR MY MOMENT MEDIA DOWNLOAD
    public void startDownload(final String mediaId, String mediaUrl, final FireBaseHelper.OnMyMomentMediaDownloadCallback statusCallbacks) {
        int p = mediaUrl.lastIndexOf(".");
        String extension = mediaUrl.substring(p + 1);
        this.percentage = 0f;
        if (p == -1 || !extension.matches("\\w+")) {
            /* file has no extension */
            return;
        }
        mediaStoragePath = mediaStoragePath + mediaId + "." + extension;
        File file = new File(mediaStoragePath);
        String key;
        try {
            key = mediaUrl.split(AppLibrary.MediaHostBucket + "/")[1];
            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
        } catch (Exception e) { //Temporary to support shifting to new bucket
            key = mediaUrl.split("pulse.resources/")[1];
            this.download = MasterClass.getTransferUtility_US().download("pulse.resources", key, file); //Starting upload
        }

//        if (AppLibrary.getMediaType(mediaUrl) == AppLibrary.MEDIA_TYPE_IMAGE) {
//            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
//        } else {
//            this.download = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file);
//        }
        this.transferId = this.download.getId();
        this.download.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                AppLibrary.log_d(TAG, "onStateChanged -" + transferState.toString()+", mediaId -"+mediaId);
                if (transferState.toString().equals("IN_PROGRESS")) {
                    AppLibrary.log_d(TAG, "MEDIA DOWNLOAD IN_PROGRESS "+mediaId);
//                    statusCallbacks.onDownloadCallback(mediaId,DOWNLOADING_MY_MOMENT_MEDIA,null);
                } else if (transferState.toString().equals("COMPLETED")) {
                    if (percentage > 0) {
                        AppLibrary.log_d(TAG, "MEDIA DOWNLOAD COMPLETED "+mediaId);
                        statusCallbacks.onDownloadCallback(mediaId, DOWNLOADED_MY_MOMENT_MEDIA, mediaStoragePath);
                    } else {
                        MasterClass.getTransferUtility().cancel(transferId);
                        statusCallbacks.onDownloadCallback(mediaId,DOWNLOAD_MY_MOMENT_MEDIA_FAILED,null);
                    }
                } else {
                    MasterClass.getTransferUtility().cancel(transferId);
                    statusCallbacks.onDownloadCallback(mediaId,DOWNLOAD_MY_MOMENT_MEDIA_FAILED,null);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                percentage = 100.0f * bytesCurrent / bytesTotal;
            }

            @Override
            public void onError(int i, Exception e) {
//                statusCallbacks.onDownloadCallback(mediaId,DOWNLOAD_MY_MOMENT_MEDIA_FAILED,null);
//                MasterClass.getTransferUtility().cancel(transferId);
                e.printStackTrace();
            }
        });
    }

}