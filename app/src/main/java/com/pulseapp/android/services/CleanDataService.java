package com.pulseapp.android.services;

import android.app.IntentService;
import android.content.Intent;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.LocalDataHelper;
import com.pulseapp.android.models.MediaDownloadDetails;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by user on 9/8/2016.
 */
public class CleanDataService extends IntentService implements FireBaseKEYIDS {

    private static final String TAG = "CleanDataService";
    private LinkedHashMap<String, MediaDownloadDetails> downloadMap;
    private LocalDataHelper localDataHelper;
    private int expiryTime;

    public CleanDataService() {
        super(CleanDataService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getApplicationContext());
        expiryTime = fireBaseHelper.getMediaExpiryTime();
        localDataHelper = LocalDataHelper.getInstance(FireBaseHelper.getInstance(this), LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD);

        if (LocalDataHelper.downloadStatusHashMap != null && LocalDataHelper.downloadStatusHashMap.size() > 0) {
            synchronized (LocalDataHelper.downloadStatusHashMap) {
                downloadMap = new LinkedHashMap<>(LocalDataHelper.downloadStatusHashMap);
            }

            if (downloadMap != null && downloadMap.size() > 0) {
                FireBaseHelper firebase = FireBaseHelper.getInstance(getApplicationContext());
                for (Map.Entry<String, MediaDownloadDetails> downloadDetails : downloadMap.entrySet()) {
                    if (downloadDetails != null && downloadDetails.getValue() != null) {
                        if (downloadDetails.getValue().getCreatedAt() <= 0 || isExpiredMedia(downloadDetails.getValue().getCreatedAt())) {
                            cleanMedia(downloadDetails, firebase);
                        }
                    }
                }
            }
        }
        cleanContributedMedias();
        cleanAppCache();

        this.stopSelf();
    }

    private void cleanMedia(Map.Entry<String, MediaDownloadDetails> downloadDetails, FireBaseHelper firebase) {
        boolean isMediaPresent = false;
        final DynamicDownloader downloader = DynamicDownloader.getInstance(getApplicationContext());
        if (downloadDetails.getValue().getMoments() != null && downloadDetails.getValue().getMoments().size() > 0) {
            for (Map.Entry<String, Integer> moment : downloadDetails.getValue().getMoments().entrySet()) {
                if (moment.getValue() == 1 && firebase.getMyUserModel() != null && firebase.getMyUserModel().momentId != null && firebase.getMyUserModel().momentId.equals(moment.getKey())) {
//                    removeMediaFromMoments(downloadDetails.getKey(), moment.getKey());
                    MomentModel momentModel = downloader.getNearByMoment(firebase.getMyUserModel().momentId);
                    if (momentModel != null && momentModel.medias != null && momentModel.medias.containsKey(downloadDetails.getKey()))
                        isMediaPresent = true;
                } else {
                    MomentModel momentModel = downloader.getNearByMoment(moment.getKey());
                    if (momentModel != null && momentModel.medias != null && momentModel.medias.containsKey(downloadDetails.getKey()))
                        isMediaPresent = true;
                    else
                        removeContributedMediaReferenceFromLocalData(downloadDetails.getKey(), moment.getKey());
                }
            }
            if (!isMediaPresent) {
                removeMediaFromCache(downloadDetails.getKey(), downloadDetails.getValue().getUrl());
            }
        } else {
            removeMediaFromCache(downloadDetails.getKey(), downloadDetails.getValue().getUrl());
        }
    }

    private void cleanContributedMedias() {
        final DataSnapshot contributedDetailsSnapshot = localDataHelper.getContributedDetailsSnapshot();
        if (contributedDetailsSnapshot != null && contributedDetailsSnapshot.getValue() != null) {
            for (DataSnapshot snapshot : contributedDetailsSnapshot.getChildren()) {
                if (snapshot != null && snapshot.getValue() != null && !snapshot.hasChild(MEDIA)) {
                    snapshot.getRef().removeValue();
                }
            }
        }
    }

    private void removeContributedMediaReferenceFromLocalData(final String mediaId, final String momentId) {
        if (mediaId != null && momentId != null) {
            final DataSnapshot contributedDetailsSnapshot = localDataHelper.getContributedDetailsSnapshot();
            if (contributedDetailsSnapshot != null && contributedDetailsSnapshot.getValue() != null) {
                if (contributedDetailsSnapshot.hasChild(momentId) && contributedDetailsSnapshot.child(momentId).hasChild(MEDIA)) {
                    if (contributedDetailsSnapshot.child(momentId).child(MEDIA).hasChild(mediaId)) {
                        if (contributedDetailsSnapshot.child(momentId).child(MEDIA).getChildrenCount() <= 1) {
                            contributedDetailsSnapshot.child(momentId).getRef().removeValue();
                        } else {
                            contributedDetailsSnapshot.child(momentId).child(MEDIA).child(mediaId).getRef().removeValue(new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (contributedDetailsSnapshot.child(momentId).child(MEDIA).getChildrenCount() <= 0) {
                                        contributedDetailsSnapshot.child(momentId).getRef().removeValue();
                                    }
                                }
                            });
                        }
                    } else {
                        if (contributedDetailsSnapshot.child(momentId).child(MEDIA).getChildrenCount() == 0) {
                            contributedDetailsSnapshot.child(momentId).getRef().removeValue();
                        }
                    }
                } else {
                    contributedDetailsSnapshot.child(momentId).getRef().removeValue();
                }
            } else {
                FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getApplicationContext());
                fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), MY_CONTRIBUTIONS, momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                            if (dataSnapshot.hasChild(MEDIA)) {
                                if (dataSnapshot.child(MEDIA).hasChild(mediaId)) {
                                    if (dataSnapshot.child(MEDIA).getChildrenCount() <= 1) {
                                        dataSnapshot.getRef().removeValue();
                                    } else {
                                        dataSnapshot.child(MEDIA).child(mediaId).getRef().removeValue(new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                if (dataSnapshot.child(MEDIA).getChildrenCount() <= 0) {
                                                    dataSnapshot.getRef().removeValue();
                                                }
                                            }
                                        });
                                    }
                                } else {
                                    if (dataSnapshot.child(MEDIA).getChildrenCount() == 0) {
                                        dataSnapshot.getRef().removeValue();
                                    }
                                }
                            } else {
                                dataSnapshot.getRef().removeValue();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });
            }
        }
    }

    private void removeMediaFromMoments(String mediaId, String momentId) {
        if (mediaId != null && momentId != null)
            FireBaseHelper.getInstance(getApplicationContext()).getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId, MEDIA, mediaId}).removeValue();
    }

    private void removeMediaFromCache(String mediaId, String url) {
        if (AppLibrary.checkStringObject(url) != null) {
            File localFile = new File(url);
            if (localFile.exists()) {
                if (localFile.delete()) {
                    removeMediaFromLocalData(mediaId);
                }
            } else {
                removeMediaFromLocalData(mediaId);
            }
        } else {
            removeMediaFromLocalData(mediaId);
        }
    }

    private void removeMediaFromMemory(String mediaId) {
        LocalDataHelper instance = LocalDataHelper.getInstance(FireBaseHelper.getInstance(this), LocalDataHelper.LocalDataChild.MEDIA_DOWNLOAD);
        instance.removeMediaIfExists(mediaId);
    }

    private void removeMediaFromLocalData(final String mediaId) {
        FireBaseHelper fireBaseHelper = FireBaseHelper.getInstance(getApplicationContext());
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), MEDIA_DOWNLOAD, mediaId}).removeValue();
        removeMediaFromMemory(mediaId);
    }

    private boolean isExpiredMedia(long createdAt) {
        long now = System.currentTimeMillis() - ((48 + expiryTime) * 60 * 60 * 1000);
        if (now > createdAt) {
            return true;
        }
        return false;
    }

    private void cleanAppCache() {
        File mediaDir = new File(AppLibrary.getCreatedMediaDirectory(getApplicationContext()));
        if (mediaDir != null && mediaDir.exists()) {
            File[] directoryListing = mediaDir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (child != null && child.exists() && isExpiredMedia(child.lastModified())) {
                        child.delete();
                    }
                }
            }
        }
    }
}