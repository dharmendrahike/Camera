package com.pulseapp.android.downloader;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;

/**
 * Created by deepankur on 11/12/16.
 */

public class ThinDownloader implements FireBaseKEYIDS {

    private ThinDownloadManager downloadManager;
    private String TAG = getClass().getSimpleName();

    ThinDownloader(Context context) {
        downloadManager = new ThinDownloadManager();
        this.init(context);

    }

    public ThinDownloadManager getDownloadManager() {
        return downloadManager;
    }

    interface ThinTransferObserver {
        void onDownloadComplete();

        void onDownloadFailed();
    }


    static String getCdnUrl(final String amazonUrl) {
        if (!BuildConfig.IS_PRODUCTION || amazonUrl.contains("/test/images/") || amazonUrl.contains("/test/videos/") || amazonUrl.contains("/test/stickers/") || amazonUrl.contains("/test/thumbnails/"))
            return amazonUrl;

        final String POST_FIX, PREFIX, IMAGES = "/images/", VIDEOS = "/videos/", STICKERS = "/stickers/", THUMBNAILS = "/thumbnails/";

        if (amazonUrl.contains(IMAGES)) {
            PREFIX = "http://cdn.images.instalively.co/";
            POST_FIX = amazonUrl.split(IMAGES)[1];
        } else if (amazonUrl.contains(VIDEOS)) {
            PREFIX = "http://cdn.videos.instalively.co/";
            POST_FIX = amazonUrl.split(VIDEOS)[1];
        } else if (amazonUrl.contains(STICKERS)) {//stickers
            PREFIX = "http://cdn.stickers.instalively.co/";
            POST_FIX = amazonUrl.split(STICKERS)[1];
        } else if (amazonUrl.contains(THUMBNAILS)) {//thumbnails
            PREFIX = "http://cdn.thumbnails.instalively.co/";
            POST_FIX = amazonUrl.split(THUMBNAILS)[1];
        } else {                                    //for everything else return the normal url itself
            PREFIX = amazonUrl;
            POST_FIX = "";
        }
        return PREFIX + POST_FIX;
    }

    /**
     * @return transfer if the the download in progress
     */
    long downloadFile(final Context context, final String filename, final String mediaUrl, final ThinTransferObserver thinTransferObserver) {

        Log.d(TAG, "downloadFile: " + filename);
        File mediaDir = new File(filename);

        if (!mediaDir.exists())
            mediaDir.mkdirs();

        DownloadStatusListenerV1 downloadListener = new DownloadStatusListenerV1() {
            @Override
            public void onDownloadComplete(DownloadRequest downloadRequest) {
                Log.d(TAG, "Download complete for media  " + mediaUrl);
                thinTransferObserver.onDownloadComplete();
            }

            @Override
            public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                Log.d(TAG, "Download failed for media");
                thinTransferObserver.onDownloadFailed();
            }

            @Override
            public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
//                Log.d(TAG, "Ongoing download for chat media");
            }

        };

        DownloadRequest req = new DownloadRequest(Uri.parse(getCdnUrl(mediaUrl)))
                .setStatusListener(downloadListener)
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(Uri.parse(filename))
                .setPriority(DownloadRequest.Priority.HIGH);

        return downloadManager.add(req);
    }


//    public void setThinTransferObserver(ThinTransferObserver thinTransferObserver) {
//        this.thinTransferObserver = thinTransferObserver;
//    }

//    ThinTransferObserver thinTransferObserver;

//    public long downloadFileWithoutListener(final Context context, final String filename, final String mediaUrl) {
//
//        Log.d(TAG, "downloadFile: " + filename);
//        File mediaDir = new File(filename);
//
//        if (!mediaDir.exists())
//            mediaDir.mkdirs();
//
//        DownloadStatusListenerV1 downloadListener = new DownloadStatusListenerV1() {
//            @Override
//            public void onDownloadComplete(DownloadRequest downloadRequest) {
//                Log.d(TAG, "Download complete for media  " + mediaUrl);
//                thinTransferObserver.onDownloadComplete();
//            }
//
//            @Override
//            public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
//                Log.d(TAG, "Download failed for media");
//                thinTransferObserver.onDownloadFailed();
//            }
//
//            @Override
//            public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
////                Log.d(TAG, "Ongoing download for chat media");
//            }
//
//        };
//
//        DownloadRequest req = new DownloadRequest(Uri.parse(getCdnUrl(mediaUrl)))
//                .setStatusListener(downloadListener)
//                .setRetryPolicy(new DefaultRetryPolicy())
//                .setDestinationURI(Uri.parse(filename))
//                .setPriority(DownloadRequest.Priority.HIGH);
//
//        return downloadManager.add(req);
//    }

    private void init(Context context) {
        File f = new File(context.getFilesDir() + File.separator + "downloadedMedia");
        if (!f.exists())
            f.mkdirs();
        else if (!f.isDirectory() && f.canWrite()) {
            f.delete();
            f.mkdirs();
        } else {
            //you can't access there with write permission.
            //Try other way.
        }
    }
}
