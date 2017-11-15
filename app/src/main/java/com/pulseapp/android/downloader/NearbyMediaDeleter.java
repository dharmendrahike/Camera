/*
package com.pulseapp.android.downloader;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.AroundYouFragment;
import com.pulseapp.android.models.MediaDownloadStatus;
import com.pulseapp.android.models.MomentModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

*/
/**
 * Created by deepankur on 7/22/16.
 *//*

public class NearbyMediaDeleter implements FireBaseKEYIDS {

    FireBaseHelper fireBaseHelper;
    private String myUserId;
    private HashMap<String, MediaDownloadStatus> localMediaStatusMap;
    private String TAG = "NearbyMediaDeleter";
    static boolean mCreated;


    public NearbyMediaDeleter(Context context) {
        if (mCreated) {
            Log.e(TAG, " only one instance of this is needed");
            return;
        }
        mCreated = true;
        fireBaseHelper = FireBaseHelper.getInstance(context);
        myUserId = fireBaseHelper.getMyUserId();
        initLocalDownloadData();

    }

    HashMap<String, MomentModel> momentModelHashMap = new HashMap<>();

    private void initLocalDownloadData() {
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, DynamicDownloader.AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (localMediaStatusMap == null)
                    localMediaStatusMap = new HashMap<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    MediaDownloadStatus downloadStatus = new MediaDownloadStatus(child.getKey(), child.child(URL).getValue(String.class), child.child(LOCAL_URI).getValue(String.class), child.child(DOWNLOAD_STATUS).getValue(Integer.class));
                    downloadStatus.mediaId = dataSnapshot.getKey();
                    localMediaStatusMap.put(downloadStatus.mediaId, downloadStatus);
                }
                fetchMomentDatasToCleanUp();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


        // for keeping data fresh and not iterating through each entry
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, DynamicDownloader.AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE}).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (localMediaStatusMap == null)
                    localMediaStatusMap = new HashMap<>();
                MediaDownloadStatus downloadStatus = new MediaDownloadStatus(dataSnapshot.getKey(), dataSnapshot.child(URL).getValue(String.class), dataSnapshot.child(LOCAL_URI).getValue(String.class), dataSnapshot.child(DOWNLOAD_STATUS).getValue(Integer.class));
                downloadStatus.mediaId = dataSnapshot.getKey();
                localMediaStatusMap.put(downloadStatus.mediaId, downloadStatus);


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                MediaDownloadStatus downloadStatus = new MediaDownloadStatus(dataSnapshot.getKey(), dataSnapshot.child(URL).getValue(String.class), dataSnapshot.child(LOCAL_URI).getValue(String.class), dataSnapshot.child(DOWNLOAD_STATUS).getValue(Integer.class));
                downloadStatus.mediaId = dataSnapshot.getKey();
                localMediaStatusMap.put(downloadStatus.mediaId, downloadStatus);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    LinkedHashSet<String> nearbyMomentIdsSet;

    private void fetchMomentDatasToCleanUp() {
        nearbyMomentIdsSet = AroundYouFragment.getHomeMomentView();
        if (nearbyMomentIdsSet != null && nearbyMomentIdsSet.size() > 0)
            for (String momentId : nearbyMomentIdsSet)
                fireBaseHelper.getNewFireBase(ANCHOR_MOMENTS, new String[]{momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        momentModelHashMap.put(dataSnapshot.getKey(), dataSnapshot.getValue(MomentModel.class));
                        checkIfReadyToExecute();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
    }

    private void checkIfReadyToExecute() {
        if (nearbyMomentIdsSet.size() > momentModelHashMap.size()) {
            Log.d(TAG, " ids set > mapsize, not ready ");
            return;
        }
        for (Map.Entry<String, MomentModel> entry : momentModelHashMap.entrySet()) {
            if (entry.getValue() == null) {
                Log.d(TAG, " all moments not loaded yet");
                return;
            }
        }
//        executeDeletor();
        LocalFileDeletionTask task = new LocalFileDeletionTask(localMediaStatusMap, momentModelHashMap);
    }

    final boolean deleteNow = false;

    class LocalFileDeletionTask extends AsyncTask<Object, Void, Boolean> {

        ArrayList<String> mediasToDelete = new ArrayList<>();
        String TAG = getClass().getSimpleName();
        HashMap<String, MediaDownloadStatus> localMediaStatusMap;
        HashMap<String, MomentModel> momentModelHashMap;

        private LocalFileDeletionTask(HashMap<String, MediaDownloadStatus> localMediaStatusMap, HashMap<String, MomentModel> momentModelHashMap) {
            this.localMediaStatusMap = localMediaStatusMap;
            this.momentModelHashMap = momentModelHashMap;
            this.execute(localMediaStatusMap, momentModelHashMap);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, " onPreExecute ");
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Log.d(TAG, " params " + params);
            for (Map.Entry<String, MediaDownloadStatus> entry : localMediaStatusMap.entrySet()) {
                final String mediaId = entry.getKey();
                boolean mediaIsInUse = false;
                for (Map.Entry<String, MomentModel> momentModelEntry : momentModelHashMap.entrySet()) {
                    final HashMap<String, MomentModel.Media> medias = momentModelEntry.getValue().medias;
                    if (medias == null)
                        continue;
                    if (medias.get(mediaId) != null) {
                        mediaIsInUse = true;
                        break;//one moment won't have two medias with same Id
                    }
                }
                if (!mediaIsInUse)
                    mediasToDelete.add(mediaId);
            }

            Log.d(TAG, " will delete " + mediasToDelete.size() + " medias from disk:----- ");
            for (String mediaId : mediasToDelete) {
                Log.d(TAG, " will delete " + mediaId);
            }

            if (deleteRightAway) {
                for (String mediaId : mediasToDelete) {
                    final MediaDownloadStatus mediaDownloadStatus = localMediaStatusMap.get(mediaId);
                    if (mediaDownloadStatus.localUri != null) {
                        if (new File(mediaDownloadStatus.localUri).exists()) {
                            if (new File(mediaDownloadStatus.localUri).delete()) {
                                fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, DynamicDownloader.AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE}).child(mediaId).setValue(null);
                                Log.d(TAG," mediaId: "+mediaId+" deleted from disk..!");
                            } else {
                                Log.e(TAG, " unable to delete file for: " + mediaId);
                            }
                        } else {
                            Log.e(TAG, " file for " + mediaId + " does not exists");
                        }
                    } else {
                        Log.e(TAG, " local uri for: " + mediaId + " is null " + " status: " + mediaDownloadStatus.downloadStatus + " and hence deleting reference from local data");
                        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, DynamicDownloader.AROUND_YOU_LOCAL_DATA_FIREBASE_REFERENCE}).child(mediaId).setValue(null);
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean)
                Log.d(TAG, "deletion accured successfully");
            else Log.d(TAG, "unable to perform deletion ");

        }
    }

    final boolean deleteRightAway = true;//for testing
}
*/
