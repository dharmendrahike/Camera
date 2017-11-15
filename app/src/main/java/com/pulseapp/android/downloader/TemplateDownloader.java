package com.pulseapp.android.downloader;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.LocationTemplateModel;
import com.pulseapp.android.stickers.ParentStickerFragment;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 5/31/16.
 */
public class TemplateDownloader implements FireBaseKEYIDS {
    private static TemplateDownloader mTemplateDownloader;
    private String TEMPLATE_STICKERS_DIR_NAME = "templateStickers";
    private Context mContext;
    private String templateStoragePath;
    private String TAG = this.getClass().getSimpleName();
    private TransferObserver transferObserver;
    private FireBaseHelper fireBaseHelper;
    private LinkedHashMap<String, LocationTemplateModel> templateLinkedMap;
    private String myUserId;
    private ParentStickerFragment parentStickerFragment;

    public void setTemplateLinkedMap(LinkedHashMap<String, LocationTemplateModel> templateLinkedMap) {
        this.templateLinkedMap = templateLinkedMap;
    }

    private TemplateDownloader(Context context) {
        mContext = context;
        validateFileSystem();
        fireBaseHelper = FireBaseHelper.getInstance(context);
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{"templateDownload"}).setValue(null);//clearing the old data
        myUserId = fireBaseHelper.getMyUserId();
        Log.d(TAG, "status before transverse");
    }


    private void validateFileSystem() {
        templateStoragePath = this.getTemplateFilesDirectory(mContext);
        File mediaDir = new File(templateStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();
    }

    private String getTemplateFilesDirectory(Context mContext) {
        return mContext.getFilesDir().getAbsolutePath() + File.separator + TEMPLATE_STICKERS_DIR_NAME + File.separator;
    }


    public static TemplateDownloader getInstance(Context context) {
        if (mTemplateDownloader == null)
            mTemplateDownloader = new TemplateDownloader(context);
        return mTemplateDownloader;
    }

    private void startDownload(final String templateId, final LocationTemplateModel.LocationSticker sticker) {
        String pictureUrl = sticker.pictureUrl;

        int p = pictureUrl.lastIndexOf(".");
        String extension = pictureUrl.substring(p + 1);
        if (p == -1 || !extension.matches("\\w+")) {
            /* file has no extension */
            Log.e(TAG, " start download returning");
            return;
        }
        final String currentMediaStoragePath = templateStoragePath + sticker.mStickerId + "." + extension;
        Log.d(TAG, " mediaStoragePath " + templateStoragePath);
        File file = new File(currentMediaStoragePath);
        Log.d(TAG, " pictureUrl " + pictureUrl);

        String key;
        try {
            key = pictureUrl.split(AppLibrary.MediaHostBucket + "/")[1];
            this.transferObserver = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file); //Starting upload
        } catch (Exception e) { //Temporary to support shifting to new bucket
            key = pictureUrl.split("pulse.resources/")[1];
            this.transferObserver = MasterClass.getTransferUtility_US().download("pulse.resources", key, file); //Starting upload
        }

        Log.d(TAG, " downloadId " + transferObserver.getId());

        updateStickerDownloadProgress(templateId, sticker, MEDIA_DOWNLOAD_NOT_STARTED, null);

        this.transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int i, TransferState transferState) {
                Log.d(TAG, "onStateChanged -" + transferState.toString() + " for mediaId" + i);
                if (transferState.toString().equals("IN_PROGRESS")) {
                    Log.d(TAG, "MEDIA DOWNLOAD IN_PROGRESS for " + i);
                    updateStickerDownloadProgress(templateId, sticker, MEDIA_DOWNLOADING, null);
                } else if (transferState.toString().equals("COMPLETED")) {
                    Log.d(TAG, "MEDIA DOWNLOAD COMPLETED for mediaId" + i);
                    updateStickerDownloadProgress(templateId, sticker, MEDIA_DOWNLOAD_COMPLETE, currentMediaStoragePath);
                }
            }

            @Override
            public void onProgressChanged(int i, long l, long l1) {
                Log.d(TAG, " onProgressChanged i: " + i + " l " + l + " l1 " + l1);
            }

            @Override
            public void onError(int i, Exception e) {
                e.printStackTrace();
                Log.d(TAG, "onError " + e);
                Log.d(TAG, " downloading failed permanently for mediaId " + i);
                updateStickerDownloadProgress(templateId, sticker, ERROR_DOWNLOADING_MEDIA, null);
            }
        });
    }

    private void updateStickerDownloadProgress(String templateId, LocationTemplateModel.LocationSticker sticker, int status, @Nullable String storagePath) {
        final DatabaseReference fireBase = fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, TEMPLATE_DOWNLOAD, sticker.mStickerId});
        if (status == MEDIA_DOWNLOAD_COMPLETE) {
            fireBase.child(DOWNLOAD_STATUS).setValue(status);
            fireBase.child(URL).setValue(storagePath);
            sticker.localUri = storagePath;
            notifyStickerLoaded(templateId);
        } else if (status == MEDIA_DOWNLOAD_NOT_STARTED) {
            fireBase.child(DOWNLOAD_STATUS).setValue(status);
        } else if (status == ERROR_DOWNLOADING_MEDIA) {
            final int MAX_RETRY_ATTEMPTS = 3;
            final Integer previousAttempts = downloadRetryMap.get(sticker.mStickerId);
            if (previousAttempts == null) {
                downloadRetryMap.put(sticker.mStickerId, 1);
            } else if (previousAttempts < MAX_RETRY_ATTEMPTS) {
                startDownload(templateId, sticker);
                downloadRetryMap.put(sticker.mStickerId, 1 + previousAttempts);
            }
        }
    }


    private HashMap<String, Integer> downloadRetryMap = new HashMap<>();

    public void notifyTemplateLoadedFromFireBase(final String templateId, LocationTemplateModel templateModel) {
        for (final Map.Entry<String, LocationTemplateModel.LocationSticker> entry : templateModel.stickers.entrySet()) {
            fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, TEMPLATE_DOWNLOAD, entry.getKey()}).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                        //start download
                        startDownload(templateId, entry.getValue());
                        return;
                    }
                    final Integer status = dataSnapshot.child(DOWNLOAD_STATUS).getValue(Integer.class);
                    if (status != null && status == MEDIA_DOWNLOAD_COMPLETE) {
                        if (dataSnapshot.child(URL) != null && dataSnapshot.child(URL).exists() && (new File(dataSnapshot.child(URL).getValue(String.class))).exists()) {
                            //everything ok
                            entry.getValue().localUri = dataSnapshot.child(URL).getValue(String.class);
                            notifyStickerLoaded(templateId);
                        } else {
                            startDownload(templateId, entry.getValue());
                        }
                    } else {
                        startDownload(templateId, entry.getValue());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });
        }
    }

    /**
     * @param templateId for which the sticker was loaded / downloaded
     */
    private void notifyStickerLoaded(String templateId) {//todo optimize bu template id
        if (this.parentStickerFragment != null)
            parentStickerFragment.refreshData();
    }

    public void setFragmentReference(ParentStickerFragment fragmentReference) {
        this.parentStickerFragment = fragmentReference;
    }


    public LocationTemplateModel getLocationModelByIndex(int index) {
        Log.d(TAG, "getLocationModelByIndex " + index);
        String[] keySet = templateLinkedMap.keySet().toArray(new String[templateLinkedMap.size()]);
        return templateLinkedMap.get(keySet[index]);
    }
}