package com.pulseapp.android.downloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.ChildEmojiPagerFragment;
import com.pulseapp.android.fragments.ParentEmojiPagerFragment;
import com.pulseapp.android.models.StickerCategoryModel;
import com.pulseapp.android.models.StickerLocalStatusModel;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.models.StickerSerializedData;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by deepankur on 6/2/16.
 */
public class ChatCameraStickerDownloader implements FireBaseKEYIDS {


    /**
     * We need two data structures as they are in a way independent of each other
     * For display we use  #categoryChatCameraStickerMap_To_Download  which essentially was written and stored
     * in the previous run .
     * <p>
     * {@link # categoryChatCameraSticker_To_Download} and {@link #categoryModelMap_To_Download}:-
     * are the ones on which the whole processing is executed carried,
     * <p>
     * {@link #categoryChatCameraStickerMap_To_Disk} and {@link #categoryModelMap_To_Disk}
     * on the other hand are the data structure that will be written to the disk overriding the previous file
     * and will be used in the next run
     * run as in app being killed
     * <p>
     * However------- For the first run {@link #getObject(Context)} will fail as  disk has no content
     * in this case {@link #initLocalDownloadData()} will be called after all the fireBase operations
     * and hence
     * {@link # categoryChatCameraSticker_To_Download} and {@link #categoryModelMap_To_Download}
     * will be assigned the pointers of
     * {@link #categoryChatCameraStickerMap_To_Disk} and {@link #categoryModelMap_To_Disk}
     * by the method {@link #initLocalDownloadData()} as they were null before assignment
     * <p>
     * <p>
     */

    private HashMap<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> categoryChatCameraStickerMap_To_Disk = new HashMap<>();
    private HashMap<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> categoryChatCameraStickerMap_To_Download = null;


    private LinkedHashMap<String, StickerCategoryModel> categoryModelMap_To_Disk = new LinkedHashMap<>();
    private LinkedHashMap<String, StickerCategoryModel> categoryModelMap_To_Download = null;

    private static ChatCameraStickerDownloader chatCameraStickerDownloader;
    private Context context;
    private String stickerStoragePath;
    private FireBaseHelper fireBaseHelper;
    private String TAG = getClass().getSimpleName();
    //    private TransferObserver transferObserver;
    private String myUserId;
    private HandlerThread cameraStickerThread;
    private Handler cameraStickerHandler;
    private boolean debug = true;
    /**
     * if we load the data structure from disk we will ignore {@link #loadedStickersCountMap}
     * and hence categories having no downloaded stickers will be shown.
     * This is bound to happen only on 2nd run and greater at that time, it  is highly likely that
     * most of the stickers are downloaded and hence the check can be skipped.
     * Currently for the sake of simplicity
     * This variable will be true even if the data was loaded from asset.
     */
    private boolean loadedFromDisk;
    private HashSet<String> stickersPresentInAssetsSet;


    public static ChatCameraStickerDownloader getChatCameraStickerDownloader(Context context) {
        if (chatCameraStickerDownloader == null)
            chatCameraStickerDownloader = new ChatCameraStickerDownloader(context);
        return chatCameraStickerDownloader;
    }

    @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
    private ChatCameraStickerDownloader(Context context) {
        this.context = context;
        this.fireBaseHelper = FireBaseHelper.getInstance(context);
        this.myUserId = fireBaseHelper.getMyUserId();
        this.downloadIdStickerMap = new HashMap<>();
        this.loadedStickersCountMap = new HashMap<>();
        cameraStickerThread = new HandlerThread("cameraStickerThread");
        cameraStickerThread.start();
        cameraStickerHandler = new Handler(cameraStickerThread.getLooper());
        getAllImageInAssetFolder();
        validateFileSystem();
        long t1 = System.currentTimeMillis();
        loadedFromDisk = getObject(MasterClass.getGlobalContext());

        final boolean exportSerializedFileSdCard = false; //Set to True if new serialized file has to be regenerated

        //can only flush if the file was actually read via loadedFromDisk
        if (exportSerializedFileSdCard && loadedFromDisk &&
                (BuildConfig.BUILD_TYPE.equals("debug") || BuildConfig.BUILD_TYPE.equals("productiondebug"))) {

            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/dir1");
                dir.mkdirs();
                exportFile(new File(context.getFilesDir().getAbsolutePath() + File.separator + getSerializationStorageFile()), dir);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Log.d(TAG, "ChatCameraStickerDownloader: getObject " + loadedFromDisk + " takes " + (System.currentTimeMillis() - t1));

        long serialVersionUID = ObjectStreamClass.lookup(StickerModel.class).getSerialVersionUID();
        Log.d(TAG, "readFromResources: serialverUid " + serialVersionUID);
        serialVersionUID = ObjectStreamClass.lookup(StickerCategoryModel.class).getSerialVersionUID();
        Log.d(TAG, "readFromResources: serialverUid " + serialVersionUID);

        boolean loadedFromAsset = false;
        if (!loadedFromDisk) {
            loadedFromAsset = readFromResources();

            Log.d(TAG, "ChatCameraStickerDownloader: readFromResources " + loadedFromDisk);
        }

        if (loadedFromDisk)
            Log.d(TAG, "ChatCameraStickerDownloader: successfully read from disk/asset");

        if (loadedFromDisk || loadedFromAsset) {
            if (!loadedFromAsset) {
                //Last saved file from disk - Firebase callback has been executed atleast once
                //initLocalDownloadData();
                for (Map.Entry<String, StickerCategoryModel> entry : categoryModelMap_To_Download.entrySet())
                    initializeLoadedStickerMapValues(entry.getValue().categoryId);
            } else {
                //Loaded from asset - Firebase callback has not even been executed once
                loadedFromDisk = true;
                skipLocalDownloadData();
                downloadTheCategoriesSticker();
                downloadAllTheStickers();
                prioritizeTheStickersPreSentInAssets();
                for (Map.Entry<String, StickerCategoryModel> entry : categoryModelMap_To_Download.entrySet())
                    initializeLoadedStickerMapValues(entry.getValue().categoryId);
                startExploitingQue();
            }

        }

        fetchCategoriesFromFireBase();
    }

    private void prioritizeTheStickersPreSentInAssets() {
        Queue<PendingItems> tempQueue = new LinkedList<>(this.downloadQueue);
        this.downloadQueue.clear();
        HashSet<String> stickerIdsPresentInAssets = new HashSet<>();
        for (String fileName : stickersPresentInAssetsSet) {
            if (fileName.indexOf(".") > 0) {
                String id = fileName.substring(0, fileName.lastIndexOf("."));
                stickerIdsPresentInAssets.add(id);
            }
        }

        for (Iterator<PendingItems> iterator = tempQueue.iterator(); iterator.hasNext(); ) {
            PendingItems item = iterator.next();

            if (stickerIdsPresentInAssets.contains(item.mediaId)) {
                this.downloadQueue.add(item);
                // Remove the current element from the iterator and the list.
                iterator.remove();
            }
        }


        this.downloadQueue.addAll(tempQueue);
        tempQueue.clear();

    }

    private void validateFileSystem() {
        stickerStoragePath = this.getStickersFilesDirectory(context);
        File mediaDir = new File(stickerStoragePath);
        if (!mediaDir.exists())
            mediaDir.mkdirs();
    }

    private String getStickersFilesDirectory(Context mContext) {
        return mContext.getFilesDir().getAbsolutePath() + File.separator + CHAT_CAMERA_STICKERS_DIR_NAME + File.separator;
    }


    private void fetchCategoriesFromFireBase() {
        fireBaseHelper.getNewFireBase(ANCHOR_STICKER_CATEGORY, null).keepSynced(true);
        fireBaseHelper.getNewFireBase(ANCHOR_STICKER_CATEGORY, null).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                cameraStickerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                            Log.e(TAG, "fetchCategoriesFromFireBase failed; dataSnap null");
                            return;
                        }

                        int count = 0;
                        for (DataSnapshot categorySnap : dataSnapshot.getChildren()) {
                            StickerCategoryModel categoryModel = categorySnap.getValue(StickerCategoryModel.class);
                            if (!categoryModel.isActive) {
                                if (debug)
                                    Log.d(TAG, " skipping inactive category " + categoryModel.title);
                                continue;
                            }
                            categoryModel.categoryId = categorySnap.getKey();
                            categoryModelMap_To_Disk.put(categorySnap.getKey(), categoryModel);
//                            loadedStickersCountMap.put(categoryModel.categoryId, 0);
                            initializeLoadedStickerMapValues(categoryModel.categoryId);
                            ++count;
//                            if (count == 3) break;
                        }

                        fetchAllStickersToShow();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void fetchAllStickersToShow() {
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{CAMERA_STICKERS}).keepSynced(true);
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{CAMERA_STICKERS}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot cameraStickersSnapshot) {
                cameraStickerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (cameraStickersSnapshot == null || cameraStickersSnapshot.getValue() == null) {
                            if (debug) Log.d(TAG, "fetchAllStickersToShow. failed returning");
                            return;
                        }
                        checkSettingNodeForIcon(cameraStickersSnapshot);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    private void checkSettingNodeForIcon(final DataSnapshot cameraStickersSnapshot) {
        final LinkedHashMap<String, LinkedHashMap<String, StickerModel>> categoryStickerMap = new LinkedHashMap<>();

        //Settings anchor already in sync on Splash
        fireBaseHelper.getNewFireBase(ANCHOR_APP_SETTINGS, null).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot settingSnapshot) {

                categoryStickerMap.put(fireBaseHelper.getMyUserId(), new LinkedHashMap<String, StickerModel>());
                loadUserInstitutionSticker(categoryStickerMap, cameraStickersSnapshot, settingSnapshot);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void loadUserInstitutionSticker(final LinkedHashMap<String, LinkedHashMap<String, StickerModel>> categoryStickerMap, final DataSnapshot cameraStickersSnapshot, final DataSnapshot settingSnapshot) {

        /**
         * this field must be added to the data structure {@link #categoryModelMap_To_Disk} ALWAYS, so that is it read by {@link #categoryModelMap_To_Download}
         * every time and also, If we have no stickers to download this will be ignored by the function {@link #registerEmoticonFragment(ParentEmojiPagerFragment)}
         *
         * StickerCategoryModel with id with as {@link #fireBaseHelper#g
         */
        final StickerCategoryModel categoryModel = new StickerCategoryModel(fireBaseHelper.getMyUserId(), true, "My Stickers");

        if (settingSnapshot.hasChild(USER_STICKER_ICON)) {
            categoryModel.imageUrl = settingSnapshot.child(USER_STICKER_ICON).getValue(String.class);
        }
        categoryModelMap_To_Disk.put(fireBaseHelper.getMyUserId(), categoryModel);
        initializeLoadedStickerMapValues(categoryModel.categoryId);
//        categoryChatCameraStickerMap_To_Disk.put(cameraStickersSnapshot.getKey(), new LinkedHashMap<String, LinkedHashMap<String, StickerModel>>());

        if (fireBaseHelper.getMyUserModel() != null && fireBaseHelper.getMyUserModel().miscellaneous != null && fireBaseHelper.getMyUserModel().miscellaneous.institutionData != null && fireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId != null) {
            final String momentId = fireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId;
//            if (momentId != null)
            fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{INSTITUTION, momentId}).keepSynced(true);
            fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{INSTITUTION, momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot institutionSnapShot) {

                    cameraStickerHandler.post(new Runnable() {
                        @Override
                        public void run() {


                            if (institutionSnapShot.hasChildren()) {
                                LinkedHashMap<String, StickerModel> stickerMap = null;
                                for (DataSnapshot stickerSnap : institutionSnapShot.getChildren()) {
                                    if (stickerMap == null)
                                        stickerMap = new LinkedHashMap<>();
                                    StickerModel stickerModel = stickerSnap.getValue(StickerModel.class);
                                    stickerModel.stickerId = stickerSnap.getKey();
                                    stickerMap.put(stickerSnap.getKey(), stickerModel);
                                    categoryStickerMap.put(fireBaseHelper.getMyUserId(), stickerMap);
                                }
                            }
                            institutionStickersAddedToDataStructure = true;
                            loadUserSpecificStickers(categoryStickerMap, cameraStickersSnapshot, settingSnapshot, categoryModel);
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {//ignoring the institution thing
            loadUserSpecificStickers(categoryStickerMap, cameraStickersSnapshot, settingSnapshot, categoryModel);
        }
//        if (fireBaseHelper.getMyUserModel() != null && fireBaseHelper.getMyUserModel().miscellaneous != null && fireBaseHelper.getMyUserModel().miscellaneous.institutionData != null) {
//            final String momentId = fireBaseHelper.getMyUserModel().miscellaneous.institutionData.momentId;
//            if (momentId != null && dataSnapshot.child(INSTITUTION).child(momentId).hasChildren()) {
//                StickerCategoryModel categoryModel = new StickerCategoryModel(fireBaseHelper.getMyUserId(),
//                        true, "My Stickers");
//                if (settingSnapshot.hasChild(USER_STICKER_ICON)) {
//                    categoryModel.imageUrl = settingSnapshot.child(USER_STICKER_ICON).getValue(String.class);
//                }
//                categoryModelMap_To_Disk.put(fireBaseHelper.getMyUserId(), categoryModel);
//                                        loadedStickersCountMap.put(categoryModel.categoryId, 0);
//                initializeLoadedStickerMapValues(categoryModel.categoryId);
//                LinkedHashMap<String, StickerModel> stickerMap = null;
//                for (DataSnapshot stickerSnap : dataSnapshot.child(INSTITUTION).child(momentId).getChildren()) {
//                    if (stickerMap == null)
//                        stickerMap = new LinkedHashMap<>();
//                    StickerModel stickerModel = stickerSnap.getValue(StickerModel.class);
//                    stickerModel.stickerId = stickerSnap.getKey();
//                    stickerMap.put(stickerSnap.getKey(), stickerModel);
//                    categoryStickerMap.put(fireBaseHelper.getMyUserId(), stickerMap);
//                }
//            }
//        }
    }

    private void loadUserSpecificStickers(final LinkedHashMap<String, LinkedHashMap<String, StickerModel>> categoryStickerMap, final DataSnapshot cameraStickersSnapshot, final DataSnapshot settingSnapshot, final StickerCategoryModel categoryModel) {
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{USER, fireBaseHelper.getMyUserId()}).keepSynced(true);
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{USER, fireBaseHelper.getMyUserId()}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userSpecificStickerDataSnapshot) {

                cameraStickerHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (userSpecificStickerDataSnapshot.hasChildren()) {
                            if (categoryModelMap_To_Disk.get(fireBaseHelper.getMyUserId()) == null) {


//                                        loadedStickersCountMap.put(categoryModel.categoryId, 0);
                                initializeLoadedStickerMapValues(categoryModel.categoryId);
                            }
                            LinkedHashMap<String, StickerModel> stickerMap = categoryStickerMap.get(fireBaseHelper.getMyUserId());
                            for (DataSnapshot stickerSnap : userSpecificStickerDataSnapshot.getChildren()) {
                                if (stickerMap == null)
                                    stickerMap = new LinkedHashMap<>();
                                StickerModel stickerModel = stickerSnap.getValue(StickerModel.class);
                                stickerModel.stickerId = stickerSnap.getKey();
                                stickerMap.put(stickerSnap.getKey(), stickerModel);
                            }
                            if (stickerMap != null && stickerMap.size() > 0)
                                categoryStickerMap.put(fireBaseHelper.getMyUserId(), stickerMap);

                        }


                        for (DataSnapshot categorySnap : cameraStickersSnapshot.getChildren()) {
                            if (categoryModelMap_To_Disk.get(categorySnap.getKey()) == null) {
                                if (debug)
                                    Log.d(TAG, " skipping inactive category: " + categorySnap.getKey());
                                continue;
                            }
                            LinkedHashMap<String, StickerModel> stickerMap = new LinkedHashMap<>();
                            for (DataSnapshot stickerSnap : categorySnap.getChildren()) {
                                StickerModel stickerModel = stickerSnap.getValue(StickerModel.class);
                                stickerModel.stickerId = stickerSnap.getKey();
                                stickerMap.put(stickerSnap.getKey(), stickerModel);
                            }
                            categoryStickerMap.put(categorySnap.getKey(), stickerMap);
                        }


                        categoryChatCameraStickerMap_To_Disk.put(cameraStickersSnapshot.getKey(), categoryStickerMap);
                        long t1 = System.currentTimeMillis();
                        saveObject(MasterClass.getGlobalContext());
                        if (debug)
                            Log.d(TAG, "putting in categoryChatCameraStickerMap " + cameraStickersSnapshot.getKey() + " time: " + (System.currentTimeMillis() - t1));

                        initLocalDownloadData();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private LinkedHashMap<String, StickerLocalStatusModel> localStatusModelLinkedHashMap = new LinkedHashMap<>();// can be converted into hashMap

    /**
     * This will behave as the point at which all the downloading starts
     */
    private boolean mProcessingStarted;

    private void initLocalDownloadData() {
        if (mProcessingStarted) {
            Log.e(TAG, "initLocalDownloadData: only one call to this function allowed");
            return;
        }

        if (categoryChatCameraStickerMap_To_Download == null)
            categoryChatCameraStickerMap_To_Download = categoryChatCameraStickerMap_To_Disk;

        if (categoryModelMap_To_Download == null)
            categoryModelMap_To_Download = categoryModelMap_To_Disk;

        mProcessingStarted = true;
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{fireBaseHelper.getMyUserId(), STICKER_DOWNLOAD}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                cameraStickerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dataSnapshot == null || dataSnapshot.getValue() == null) {
                            if (debug)
                                Log.d(TAG, "nothing found in local data; downloading all stickers ");
                            downloadTheCategoriesSticker();
                            downloadAllTheStickers();
                            startExploitingQue();
                            return;
                        }

                        for (DataSnapshot stickerDownloadSnap : dataSnapshot.getChildren()) {
                        //    String key = stickerDownloadSnap.getKey();
                     //       StickerLocalStatusModel statusModel = stickerDownloadSnap.getValue(StickerLocalStatusModel.class);
                       //     statusModel.stickerId = key;
                          //  localStatusModelLinkedHashMap.put(key, statusModel);
                        }
                        downloadTheCategoriesSticker();
                        filterCameraStickersToDownload();
                        startExploitingQue();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }


    private void skipLocalDownloadData() {
        if (mProcessingStarted) {
            Log.e(TAG, "initLocalDownloadData: only one call to this function allowed");
            return;
        }

        if (categoryChatCameraStickerMap_To_Download == null)
            categoryChatCameraStickerMap_To_Download = categoryChatCameraStickerMap_To_Disk;

        if (categoryModelMap_To_Download == null)
            categoryModelMap_To_Download = categoryModelMap_To_Disk;

        mProcessingStarted = true;

    }


    private void downloadTheCategoriesSticker() {

        for (Map.Entry<String, StickerCategoryModel> categoryModelEntry : categoryModelMap_To_Download.entrySet()) {
            String categoryId = categoryModelEntry.getKey();
            if (debug) Log.d(TAG, "downloadTheCategoriesSticker " + categoryId);
            final StickerCategoryModel categoryModel = categoryModelEntry.getValue();

            if (categoryModel.categoryId == null)
                categoryModel.categoryId = categoryId;

            boolean inAssetsSet = checkIfStickersPresentInAssetsSet(categoryModel);
            Log.d(TAG, "downloadTheCategoriesSticker: id:-- " + categoryId + " inAsset " + inAssetsSet);
            if (inAssetsSet) {//loading from asset
                Log.d(TAG, "downloadTheCategoriesSticker: loaded " + categoryId + " from assets");
            } else {//not loading from asset
                if (localStatusModelLinkedHashMap.get(categoryId) == null) {

                    StickerLocalStatusModel localStatusModel = new StickerLocalStatusModel(categoryModel.categoryId, categoryModel.imageUrl, null, MEDIA_DOWNLOAD_NOT_STARTED);
                    fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, categoryModel.categoryId}).setValue(localStatusModel);
                    addToDownloadQue(categoryModel.categoryId, categoryModel.imageUrl);


                } else {
                    final StickerLocalStatusModel statusModel = localStatusModelLinkedHashMap.get(categoryId);
                    if (statusModel.downloadStatus == STICKER_DOWNLOADED) {
                        if (!new File(statusModel.localUri).exists()) {
                            Log.e(TAG, " category sticker of " + categoryModelEntry.getValue().title + " exists with Downloaded status but " +
                                    "not found locally; starting download again ");
                            statusModel.downloadStatus = STICKER_NOT_DOWNLOADED;
                            fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, categoryId}).setValue(statusModel);
                            addToDownloadQue(statusModel.stickerId, statusModel.pictureUrl);
                        } else {
                            if (debug)
                                Log.d(TAG, " category image alreadyDownloaded ; everything OK ");
                            categoryModel.localUri = statusModel.localUri;
                        }
                    } else {//downloadStatus != STICKER_DOWNLOADED
                        addToDownloadQue(categoryModel.categoryId, categoryModel.imageUrl);
                    }
                }


            }
        }
    }

    private void downloadAllTheStickers() {
        for (Map.Entry<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> categoryChatCameraStickerMapEntry : categoryChatCameraStickerMap_To_Download.entrySet()) {
            final LinkedHashMap<String, LinkedHashMap<String, StickerModel>> value = categoryChatCameraStickerMapEntry.getValue();
            for (Map.Entry<String, LinkedHashMap<String, StickerModel>> categoryStickerEntry : value.entrySet()) {
                final LinkedHashMap<String, StickerModel> value1 = categoryStickerEntry.getValue();
                for (Map.Entry<String, StickerModel> stickerModelEntry : value1.entrySet()) {
                    StickerModel stickerModel = stickerModelEntry.getValue();
                    StickerLocalStatusModel model = new StickerLocalStatusModel(stickerModel.stickerId, stickerModel.pictureUrl, null, STICKER_NOT_DOWNLOADED);
                    fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, stickerModel.stickerId}).setValue(model);
                    addToDownloadQue(stickerModelEntry.getKey(), stickerModel.pictureUrl);
                }
            }
        }
    }


    //currently for camera stickers only
    private void filterCameraStickersToDownload() {
        final LinkedHashMap<String, LinkedHashMap<String, StickerModel>> cameraCategoryStickerMap = categoryChatCameraStickerMap_To_Download.get(CAMERA_STICKERS);
        if (cameraCategoryStickerMap == null) {
            Log.e(TAG, "filterCameraStickersToDownload failed, cameraCategoryStickerMap is null");
            return;//todo for conversational also
        }

        for (Map.Entry<String, LinkedHashMap<String, StickerModel>> cameraCategoryStickerMapEntry : cameraCategoryStickerMap.entrySet()) {
            for (Map.Entry<String, StickerModel> stickerModelEntry : cameraCategoryStickerMapEntry.getValue().entrySet()) {
                String stickerId = stickerModelEntry.getKey();
                StickerModel stickerModel = stickerModelEntry.getValue();

                final StickerLocalStatusModel statusModel = localStatusModelLinkedHashMap.get(stickerId);
                if (statusModel == null) {//sticker is not registered with local data itself(not downloaded)
                    addNewStickerToLocalDataAndStartDownload(stickerModel);
                } else {
                    if (statusModel.downloadStatus == STICKER_DOWNLOADED) {
                        if (!new File(statusModel.localUri).exists()) {
                            Log.e(TAG, "  sticker with Id: " + stickerId + " exists with Downloaded status but " +
                                    "not found locally; starting download again ");
                            addNewStickerToLocalDataAndStartDownload(stickerModel);
                        } else {
                            //everything OK; file exists
                            stickerModel.localUri = statusModel.localUri;
                            updateCounterFromStickerId(stickerId);
                        }
                    } else {//downloadStatus != STICKER_DOWNLOADED
                        addToDownloadQue(stickerModel.stickerId, stickerModel.pictureUrl);
                    }
                }
            }
        }
    }

    private void addNewStickerToLocalDataAndStartDownload(StickerModel stickerModel) {

        StickerLocalStatusModel statusModel = new StickerLocalStatusModel(stickerModel.stickerId, stickerModel.pictureUrl, null, STICKER_NOT_DOWNLOADED);
        fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, stickerModel.stickerId}).setValue(statusModel);
        addToDownloadQue(statusModel.stickerId, statusModel.pictureUrl);
    }

    /**
     * this will ensure that processing stops when three stickers fail to download
     * in a row despite the retry attempts; implying that internet connectivity isn't there
     * Otherwise downloader will iterate through all the items, all in despise
     */
    private final int NUMBER_OF_PARALLEL_DOWNLOADS = 5;
    private boolean firstTimeDownload = true;

    private void startExploitingQue() {
        if (firstTimeDownload) {
            firstTimeDownload = false;
            int index = NUMBER_OF_PARALLEL_DOWNLOADS;
            while (index > 0) {
                --index;
                Log.i(TAG, " recursive call ");
                startExploitingQue();
            }
        }

        if (downloadQueue.size() > 0) {
            PendingItems pendingItem = downloadQueue.poll();
            startDownload(pendingItem.mediaId, pendingItem.pictureUrl);
        }
    }

//    Queue<String> downloadQueue = new LinkedList<String>();
//    HashMap<String, String> pendingDownloadQueueMap = new HashMap<>();

    private Queue<PendingItems> downloadQueue = new LinkedList<>();

    private class PendingItems {
        String mediaId;
        String pictureUrl;

        public PendingItems(String mediaId, String pictureUrl) {
            this.mediaId = mediaId;
            this.pictureUrl = pictureUrl;
        }
    }

    private void addToDownloadQue(final String mediaId, String pictureUrl) {
        Log.d(TAG, "addToDownloadQue: " + mediaId);
        downloadQueue.add(new PendingItems(mediaId, pictureUrl));
//        pendingDownloadQueueMap.put(mediaId, pictureUrl);
    }

    /**
     * Mapping downloader's download id with particular sticker
     * It must only be written onto by function startDownload.
     */
    private HashMap<String, StickerLocalStatusModel> downloadIdStickerMap;

    private void startDownload(final String mediaId, String pictureUrl) {
        if (mediaId == null || pictureUrl == null) {
            Log.e(TAG, " wtf cannot start download without these; returning !");
            return;
        }
        int p = pictureUrl.lastIndexOf(".");
        String extension = pictureUrl.substring(p + 1);
        if (p == -1 || !extension.matches("\\w+")) {
            /* file has no extension */
            Log.e(TAG, " startDownload no extension found returning");
            return;
        }
        final String currentMediaStoragePath = stickerStoragePath + mediaId + "." + extension;
        if (debug) Log.d(TAG, " stickerStoragePath " + stickerStoragePath);
        File file = new File(currentMediaStoragePath);
        if (debug) Log.d(TAG, " pictureUrl " + pictureUrl);

//        String key;
//        try {
//            key = pictureUrl.split(AppLibrary.MediaHostBucket + "/")[1];
//            this.transferObserver = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file);
//        } catch (Exception e) { //Temporary to support shifting to new bucket
//            key = pictureUrl.split("pulse.resources/")[1];
//            this.transferObserver = MasterClass.getTransferUtility_US().download("pulse.resources", key, file);
//        }
//
//        if (debug) Log.d(TAG, " key " + key);
//        this.transferObserver = MasterClass.getTransferUtility().download(AppLibrary.MediaHostBucket, key, file);

//        if (debug) Log.d(TAG, " downloadId " + transferObserver.getId());
//        StickerLocalStatusModel model = new StickerLocalStatusModel(mediaId, pictureUrl, currentMediaStoragePath, STICKER_NOT_DOWNLOADED);
//        downloadIdStickerMap.put(transferObserver.getId(), model);
//        this.transferObserver.setTransferListener(new TransferListener() {
//            @Override
//            public void onStateChanged(final int i, final TransferState transferState) {
////                cameraStickerHandler.post(new Runnable() {
////                    @Override
////                    public void run() {
////                        if (debug)
////                            Log.d(TAG, "onStateChanged -" + transferState.toString() + " for mediaId" + i);
////                        if (transferState.toString().equals("IN_PROGRESS")) {
////                            if (debug) Log.d(TAG, "MEDIA DOWNLOAD IN_PROGRESS for " + i);
////                            updateStickerDownloadProgress(i, STICKER_DOWNLOAD_IN_PROGRESS);
////                        } else if (transferState.toString().equals("COMPLETED")) {
////                            if (debug) Log.d(TAG, "MEDIA DOWNLOAD COMPLETED for mediaId" + i);
////                            updateStickerDownloadProgress(i, STICKER_DOWNLOADED);
////                            startExploitingQue();
////                        }
////                    }
////                });
//            }
//
//            @Override
//            public void onProgressChanged(int i, long l, long l1) {
//                if (l == l1)
//                    if (debug) Log.d(TAG, " onProgressChanged i: " + i + " l " + l + " l1 " + l1);
//            }
//
//            @Override
//            public void onError(final int i, final Exception e) {
////                cameraStickerHandler.post(new Runnable() {
////                    @Override
////                    public void run() {
////                        e.printStackTrace();
////                        if (debug) Log.d(TAG, "onError " + e);
////                        if (debug) Log.d(TAG, " downloading failed permanently for mediaId " + i);
////                        updateStickerDownloadProgress(i, STICKER_ERROR_DOWNLOADING);
////                    }
////                });
//            }
//        });


        final String stickerId = mediaId;
        StickerLocalStatusModel model = new StickerLocalStatusModel(mediaId, pictureUrl, currentMediaStoragePath, STICKER_NOT_DOWNLOADED);
        final boolean loadFromAsset = checkIfStickersPresentInAssetsSet(model);
        if (loadFromAsset) {
            Log.d(TAG, "startDownload: skipping, will load from asset " + model.stickerId);
        } else {
            final long id = getThinDownloader().downloadFile(MasterClass.getGlobalContext(), currentMediaStoragePath, pictureUrl, (new ThinDownloader.ThinTransferObserver() {
                @Override
                public void onDownloadComplete() {
                    cameraStickerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: downloadComplete for... " + stickerId);
                            updateStickerDownloadProgress(stickerId, STICKER_DOWNLOADED, false);
                            startExploitingQue();
                        }
                    });


                }

                @Override
                public void onDownloadFailed() {
                    cameraStickerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (debug)
                                Log.d(TAG, "onDownloadFailed: " + mediaId);
                            updateStickerDownloadProgress(stickerId, STICKER_ERROR_DOWNLOADING, false);
                            startExploitingQue();
                        }
                    });
                }
            }));
        }
        downloadIdStickerMap.put(stickerId, model);

        if (loadFromAsset)
            startExploitingQue();
        updateStickerDownloadProgress(stickerId, STICKER_DOWNLOAD_IN_PROGRESS, loadFromAsset);
    }


    private void updateStickerDownloadProgress(String stickerIdInMap, int status, boolean loadFromAsset) {
        StickerLocalStatusModel model = downloadIdStickerMap.get(stickerIdInMap);
        model.downloadStatus = status;
        String stickerId = model.stickerId;

        if (loadFromAsset)
            status = STICKER_DOWNLOADED; //If loaded from assets, no point of writing to localData
        else
            fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, stickerId}).setValue(model);

        if (status == STICKER_ERROR_DOWNLOADING) {
//            if (retryAttemptAllowed(model.stickerId)) {
//                this.startDownload(model.stickerId, model.pictureUrl);
//                Log.w(TAG, "download failed; recursive call to startDownload");
//            } else {
            Log.e(TAG, "download failing continuously; giving up on sticker with Id " + model.stickerId);
            if (NUMBER_OF_PARALLEL_DOWNLOADS == 1)//if download fails permanently move on to next item
                startExploitingQue();
//            }
        }
        if (status == STICKER_DOWNLOADED) {
            updateUriInModel(model, loadFromAsset);
            checkIfRunTimeStickerDownloaded(model);
            updateCounterFromStickerId(model.stickerId);
        }
    }

//    private HashMap<String, Integer> retryDownloadMap = new HashMap<>();

//    private boolean retryAttemptAllowed(String stickerId) {
//        if (retryDownloadMap.get(stickerId) == null) {
//            retryDownloadMap.put(stickerId, 1);
//            return true;
//        }
//        int earlierRetryAttempts = retryDownloadMap.get(stickerId);
//        int MAXIMUM_RETRY_ATTEMPTS_FOR_EACH_IMAGE = 3;
//        if (earlierRetryAttempts >= MAXIMUM_RETRY_ATTEMPTS_FOR_EACH_IMAGE)
//            return false;
//        retryDownloadMap.put(stickerId, ++earlierRetryAttempts);
//        return true;
//    }

    /**
     * To know if the image is of category or the sticker
     *
     * @param stickerId
     * @return
     */
    private boolean isThisCategoryId(String stickerId) {
        return categoryModelMap_To_Download.get(stickerId) != null;
    }

    private void updateUriInModel(StickerLocalStatusModel model, boolean loadFromAsset) {
        String uri = model.localUri;
        String stickerId = model.stickerId;

        if (isThisCategoryId(stickerId)) {
            categoryModelMap_To_Download.get(stickerId).localUri = uri;
            //It war a category image that was loaded; done updating uri returning
            return;
        }

        for (Map.Entry<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> categoryChatCameraStickerMapEntry : categoryChatCameraStickerMap_To_Download.entrySet()) {
            if (categoryChatCameraStickerMapEntry.getValue() == null) {
                Log.e(TAG, "categoryChatCameraStickerMapEntry.getValue() null");
                continue;
            }
            for (Map.Entry<String, LinkedHashMap<String, StickerModel>> categoryStickerEntry : categoryChatCameraStickerMapEntry.getValue().entrySet()) {
                if (categoryStickerEntry.getValue() == null) {
                    Log.e(TAG, "categoryStickerEntry.getValue() null");
                    continue;
                }
                for (Map.Entry<String, StickerModel> stickerModelEntry : categoryStickerEntry.getValue().entrySet()) {
                    if (stickerId.equals(stickerModelEntry.getKey())) {
                        stickerModelEntry.getValue().localUri = uri;
                        stickerModelEntry.getValue().stickerPresentInAssets = loadFromAsset;
                        //breaking innermost loop coz we are sure that one category cannot have multiple
                        //stickers with same stickerId
                        break;
                        //todo return here if we are sure that there will be only one sticker id in the whole camera stickers
                        //ie no same stickers in two different categories
                    }
                }
            }
        }
        if (debug)
            Log.d(TAG, "stickerId " + model.stickerId + " not found in linked map; delete it from Local fireBase ?");
    }

    private ArrayList<ChildEmojiPagerFragment> allFragmentArrayList;
    HashMap<StickerCategoryModel, ChildEmojiPagerFragment> cachedMaps = new HashMap<>();

    /**
     * @param parentEmojiPagerFragment
     * @return the number of active Fragments in viewPager
     * -1 on dataSnapshot error
     */
    public int registerEmoticonFragment(ParentEmojiPagerFragment parentEmojiPagerFragment) {

        if (categoryChatCameraStickerMap_To_Download == null) {
            Log.e(TAG, " wtf categoryChatCameraStickerMap itself is null");
            return -1;
        }
        LinkedHashMap<String, LinkedHashMap<String, StickerModel>> cameraStickers = categoryChatCameraStickerMap_To_Download.get(CAMERA_STICKERS);
        if (cameraStickers == null) {
            Log.e(TAG, "registerEmoticonFragment: cameraStickers null");
            return -1;
        }
        if (allFragmentArrayList == null)
            allFragmentArrayList = new ArrayList<>();
        else allFragmentArrayList.clear();

        if (!institutionStickersAddedToDataStructure)
            tryAddingInstitutionStickersAtRunTime(cameraStickers);

        outer:
        for (Map.Entry<String, LinkedHashMap<String, StickerModel>> categoryEntry : cameraStickers.entrySet()) {
            int stickerPresent = 0;
            String categoryId = "";
            final String categoryEntryKey = categoryEntry.getKey();
            for (Map.Entry<String, Integer> modelIntegerEntry : loadedStickersCountMap.entrySet()) {
                if (modelIntegerEntry.getKey().equals(categoryEntryKey)) {
                    stickerPresent = modelIntegerEntry.getValue();
                    categoryId = modelIntegerEntry.getKey();
//                    Log.d(TAG, "registerEmoticonFragment: " + " count " + stickerPresent + " " + modelIntegerEntry.getKey());
                    /**
                     * if it is not my stickers skip it if the no stickers downloaded.
                     */
                    if (!categoryId.equals(fireBaseHelper.getMyUserId()) && stickerPresent < MINIMUM_STICKERS_TO_BE_PRESENT) {
                        continue outer;
                    }
                    if (stickerPresent < MINIMUM_STICKERS_TO_BE_PRESENT && !isInstitutionNeeded() && !categoryId.equals(fireBaseHelper.getMyUserId()))
                        continue outer;//fixme culprit code ; possibly slowing down everything
                    break;
                }
            }


            ChildEmojiPagerFragment childEmojiPagerFragment = cachedMaps.get(categoryModelMap_To_Download.get(categoryEntry.getKey()));

            if (childEmojiPagerFragment == null)
                childEmojiPagerFragment = new ChildEmojiPagerFragment();
            childEmojiPagerFragment.setCategoryModelTag(categoryModelMap_To_Download.get(categoryEntry.getKey()));
            childEmojiPagerFragment.setCategoryStickersMap(categoryEntry.getValue());

            cachedMaps.put(categoryModelMap_To_Download.get(categoryEntry.getKey()), childEmojiPagerFragment);

            if (isInstitutionNeeded()) {
                if (!categoryId.equals(fireBaseHelper.getMyUserId())) {
                    allFragmentArrayList.add(childEmojiPagerFragment);
                } else if (categoryId.equals(fireBaseHelper.getMyUserId()) && !isInstitutionProvided()) {
                    allFragmentArrayList.add(childEmojiPagerFragment);
                } else if (categoryId.equals(fireBaseHelper.getMyUserId()) && isInstitutionProvided() && stickerPresent + runTimeInstitutionStickerDownloadCount >= MINIMUM_STICKERS_TO_BE_PRESENT) {//here sticker present + runTimeInstitutionStickerDownloadCount will represent userspecific  and institution stickers
                    //note that alone stickers present may represent both type of the stickers
                    allFragmentArrayList.add(childEmojiPagerFragment);
                }
            } else {//institution isn't required
                if (!categoryId.equals(fireBaseHelper.getMyUserId()))
                    allFragmentArrayList.add(childEmojiPagerFragment);
//                else if (stickerPresent >=MINIMUM_STICKERS_TO_BE_PRESENT)
//                    allFragmentArrayList.add(childEmojiPagerFragment);
            }
        }
        if (debug)
            Log.d(TAG, "registerEmoticonFragment: added " + allFragmentArrayList.size() + " fragments for stickers ");
        parentEmojiPagerFragment.notifyFragmentsAdded(allFragmentArrayList);
        parentEmojiPagerFragment.setAllStickers(cameraStickers, categoryModelMap_To_Download);
        return allFragmentArrayList.size();
    }


    private Boolean institutionNeeded_Boolean = null;

    private boolean isInstitutionNeeded() {
        if (institutionNeeded_Boolean != null) return institutionNeeded_Boolean;
        SharedPreferences prefs;
        prefs = MasterClass.getGlobalContext().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        institutionNeeded_Boolean = prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED, false);
        return institutionNeeded_Boolean;
    }

    private final int MINIMUM_STICKERS_TO_BE_PRESENT = 1;

    private HashMap<String, Integer> loadedStickersCountMap;//ready to View

    private void initializeLoadedStickerMapValues(String categoryId) {
        Integer integer = loadedStickersCountMap.get(categoryId);
        if (integer == null)
            loadedStickersCountMap.put(categoryId, 0);
    }


    private void updateCounterFromStickerId(String stickerId) {
        if (categoryChatCameraStickerMap_To_Download.get(CAMERA_STICKERS) == null) {
            Log.e(TAG, " wtf? updateCounterFromStickerId failed; cameraStickers in categoryChatCameraStickerMap are null returning");
            return;
        }
        for (Map.Entry<String, LinkedHashMap<String, StickerModel>> mapEntry : categoryChatCameraStickerMap_To_Download.get(CAMERA_STICKERS).entrySet()) {
            final String categoryId = mapEntry.getKey();
            final LinkedHashMap<String, StickerModel> value = mapEntry.getValue();
            if (value.get(stickerId) != null) {
                updateCounterFromCategoryId(categoryId);
            }
        }
    }

    private void updateCounterFromCategoryId(String categoryId) {
        Integer count = loadedStickersCountMap.get(categoryId);
        loadedStickersCountMap.put(categoryModelMap_To_Download.get(categoryId).categoryId, ++count);
    }


    private String getSerializationStorageFile() {
        return "createResumeForm.ser";
    }

    private boolean mSaveObjectCalled;

    private boolean saveObject(Context context) {
        mSaveObjectCalled = true;
        StickerSerializedData stickerSerializedData = new StickerSerializedData();
        stickerSerializedData.categoryChatCameraStickerMap = new LinkedHashMap<>(categoryChatCameraStickerMap_To_Disk);
        stickerSerializedData.categoryModelMap = new LinkedHashMap<>(categoryModelMap_To_Disk);

//        stickerSerializedData.categoryChatCameraStickerMap.get("camera").remove(fireBaseHelper.getMyUserId());
        if (stickersLoadedAtRunTime != null && stickersLoadedAtRunTime.size() > 0) {//saving the runtime stickers also
            LinkedHashMap<String, StickerModel> stringStickerModelLinkedHashMap = stickerSerializedData.categoryChatCameraStickerMap.get(CAMERA_STICKERS).get(fireBaseHelper.getMyUserId());
            stringStickerModelLinkedHashMap.putAll(stickersLoadedAtRunTime);
        }

        for (Map.Entry<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> entry : stickerSerializedData.categoryChatCameraStickerMap.entrySet()) {
            LinkedHashMap<String, LinkedHashMap<String, StickerModel>> value = entry.getValue();
            for (Map.Entry<String, LinkedHashMap<String, StickerModel>> entry1 : value.entrySet()) {
                LinkedHashMap<String, StickerModel> value1 = entry1.getValue();
                for (Map.Entry<String, StickerModel> entry2 : value1.entrySet()) {
                    entry2.getValue().status = 0;
                }
            }
        }

        try {
            FileOutputStream fos = context.openFileOutput(getSerializationStorageFile(), Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(stickerSerializedData);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        stickerSerializedData.categoryChatCameraStickerMap.clear();
        stickerSerializedData.categoryModelMap.clear();
        return true;
    }


    private boolean getObject(Context context) {

        try {
            FileInputStream fis = context.openFileInput(getSerializationStorageFile());
            ObjectInputStream is = new ObjectInputStream(fis);
            Object readObject = is.readObject();
            is.close();

            if (readObject != null && readObject instanceof StickerSerializedData) {
                StickerSerializedData readObject1 = (StickerSerializedData) readObject;
                if (readObject1.categoryChatCameraStickerMap == null || readObject1.categoryModelMap == null)
                    return false;
                categoryChatCameraStickerMap_To_Download = readObject1.categoryChatCameraStickerMap;
                categoryModelMap_To_Download = readObject1.categoryModelMap;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private ThinDownloader thinDownloader;

    private ThinDownloader getThinDownloader() {
        if (thinDownloader == null)
            thinDownloader = new ThinDownloader(MasterClass.getGlobalContext());
        return thinDownloader;
    }

    private boolean isInstitutionProvided() {
        return fireBaseHelper.getMyUserModel().getMyInstitutionId() != null;
    }

    /**
     * boolean to check whether the institution stickers are added to the data structure,
     * if true the processing in {@link #registerEmoticonFragment(ParentEmojiPagerFragment)}
     * won't happen more than once.
     * The boolean will also be true if there is no data corresponding to the particular momentid of the
     * institution.
     */
    private boolean institutionStickersAddedToDataStructure;

    private void tryAddingInstitutionStickersAtRunTime(LinkedHashMap<String, LinkedHashMap<String, StickerModel>> cameraStickers) {
        if (stickersLoadedAtRunTime != null && stickersLoadedAtRunTime.size() > 0) {
            LinkedHashMap<String, StickerModel> stringStickerModelLinkedHashMap = cameraStickers.get(fireBaseHelper.getMyUserId());
            for (Map.Entry<String, StickerModel> dynamicStickersEntry : stickersLoadedAtRunTime.entrySet()) {
                if (stringStickerModelLinkedHashMap.get(dynamicStickersEntry.getKey()) == null)
                    stringStickerModelLinkedHashMap.put(dynamicStickersEntry.getKey(), dynamicStickersEntry.getValue());
            }
            institutionStickersAddedToDataStructure = true;
        }
    }

    private LinkedHashMap<String, StickerModel> stickersLoadedAtRunTime;


    /**
     * The function lazy loads the object {@link #stickersLoadedAtRunTime} and hence ensuring that the function is called only once,
     * as calling the function multiple time would be a waste of query
     *
     * @param momentId of the institute which user has selected
     */
    public void onInstitutionSelectedAtRuntime(String momentId) {
        Log.d(TAG, "onInstitutionSelected: at run time " + momentId);
        if (stickersLoadedAtRunTime != null) return;
        stickersLoadedAtRunTime = new LinkedHashMap<>();
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{INSTITUTION, momentId}).keepSynced(true);
        fireBaseHelper.getNewFireBase(ANCHOR_STICKERS, new String[]{INSTITUTION, momentId}).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot institutionSnapShot) {

                cameraStickerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (institutionSnapShot.hasChildren()) {
                            for (DataSnapshot stickerSnap : institutionSnapShot.getChildren()) {
                                StickerModel stickerModel = stickerSnap.getValue(StickerModel.class);
                                stickerModel.stickerId = stickerSnap.getKey();
                                stickersLoadedAtRunTime.put(stickerSnap.getKey(), stickerModel);
                            }
                            if (mSaveObjectCalled) {
                                Log.d(TAG, "saveObjectCalled already ");
                                /**
                                 *the object is serialized and saved to disk at this point
                                 *of time , hence we have missed our chance to put the institution stickers
                                 *loaded dynamically to put in the D.S. :-
                                 * {@link categoryChatCameraStickerMap_To_Disk}
                                 * so that it could be read quickly from disk itself on next startup
                                 * Nevertheless, we will call the same function here
                                 * so that the missed operation can now be performed
                                 */
                                saveObject(MasterClass.getGlobalContext());
                            } else {
                                /**
                                 * save object is not called yet, so whenever it gets called it will 'absorb'
                                 * the institution stickers and hence they will be ready for the next run also
                                 */
                            }
                            processStickersAddedAtRunTime();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void processStickersAddedAtRunTime() {

        Queue<PendingItems> temp = new LinkedList<>(this.downloadQueue);
        this.downloadQueue.clear();

        for (Map.Entry<String, StickerModel> entry : stickersLoadedAtRunTime.entrySet()) {
            StickerModel stickerModel = entry.getValue();
            StickerLocalStatusModel statusModel = new StickerLocalStatusModel(stickerModel.stickerId, stickerModel.pictureUrl, null, STICKER_NOT_DOWNLOADED);
            fireBaseHelper.getNewFireBase(ANCHOR_LOCAL_DATA, new String[]{myUserId, STICKER_DOWNLOAD, stickerModel.stickerId}).setValue(statusModel);
            this.downloadQueue.add(new PendingItems(stickerModel.stickerId, stickerModel.pictureUrl));
        }
        this.downloadQueue.addAll(temp);
        temp.clear();
        if (downloadQueue.size() > 0) {
            PendingItems pendingItem = downloadQueue.poll();
            startDownload(pendingItem.mediaId, pendingItem.pictureUrl);
            Log.d(TAG, "processStickersAddedAtRunTime: " + pendingItem.mediaId);
        }
    }

    /**
     * Since the sticker loaded at run time is independent of the rest of the data structure.
     * in the manner that those data-structures ie. {@link #loadedStickersCountMap}
     * might/might-not have been instantiated by then, hence we need to ensure that the
     * {@link #stickersLoadedAtRunTime} make use of different integer for maintainig the download count
     * ie. {@link #runTimeInstitutionStickerDownloadCount}
     */
    private int runTimeInstitutionStickerDownloadCount = 0;

    private void checkIfRunTimeStickerDownloaded(StickerLocalStatusModel model) {
        if (stickersLoadedAtRunTime != null) {
            for (Map.Entry<String, StickerModel> entry : stickersLoadedAtRunTime.entrySet()) {
                if (model.stickerId.equals(entry.getKey())) {
                    ++runTimeInstitutionStickerDownloadCount;
                    entry.getValue().localUri = model.localUri;
                    break;
                }
            }
        }
    }

    private boolean readFromResources() {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.production_sticker_file);
            ObjectInputStream ois = new ObjectInputStream(is);
            StickerSerializedData readObject1 = (StickerSerializedData) ois.readObject();

            if (readObject1.categoryChatCameraStickerMap == null || readObject1.categoryModelMap == null)
                return false;
            boolean b = copyFromResourcesToInternalDirectory(readObject1);
            Log.d(TAG, "readFromResources: copyFromResourcesToInternalDirectory " + b);
            categoryChatCameraStickerMap_To_Download = readObject1.categoryChatCameraStickerMap;
            categoryModelMap_To_Download = readObject1.categoryModelMap;
            return true;
        } catch (Exception ex) {
            Log.d("Serialization Error >> ", ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * @param readObject1 immediately write the object read from assets to internal directory
     *                    so that {@link #getObject(Context)} returns true and the flow is normal
     *                    ie. local data is listened to before downloading the new stickers
     */
    private boolean copyFromResourcesToInternalDirectory(StickerSerializedData readObject1) {
        try {
            FileOutputStream fos = context.openFileOutput(getSerializationStorageFile(), Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(readObject1);
            oos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private void getAllImageInAssetFolder() {
        AssetManager assetManager = context.getAssets();
        String[] files = new String[0];
        try {
            files = assetManager.list(EMOTICON_ASSET_DIR_NAME);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, " unable to read local files form asset folders");
        }
        stickersPresentInAssetsSet = new HashSet<>(Arrays.asList(files));
    }

    private boolean checkIfStickersPresentInAssetsSet(Object model) {
        String stickerId;
        if (model instanceof StickerCategoryModel)
            stickerId = ((StickerCategoryModel) model).categoryId;
        else if (model instanceof StickerModel)
            stickerId = ((StickerModel) model).stickerId;
        else if (model instanceof StickerLocalStatusModel) {
            stickerId = ((StickerLocalStatusModel) model).stickerId;
        } else {
            throw new RuntimeException("only StickerCategoryModel & StickerModel objects allowed");
        }
        for (String extension : IMAGE_FILE_EXTENSION) {
            if (stickersPresentInAssetsSet.contains(stickerId + extension)) {
                if (model instanceof StickerCategoryModel) {
                    ((StickerCategoryModel) model).imagePresentInAssets = true;
                    ((StickerCategoryModel) model).localUri = "file:///android_asset/emoticons/" + stickerId + extension;
//                    updateCounterFromCategoryId(((StickerCategoryModel) model).categoryId);
                    if (debug)
                        Log.d(TAG, " found category image in asset " + ((StickerCategoryModel) model).title);
                } else if (model instanceof StickerModel) {
                    ((StickerModel) model).stickerPresentInAssets = true;
                    ((StickerModel) model).localUri = "file:///android_asset/emoticons/" + stickerId + extension;
                    updateCounterFromStickerId(((StickerModel) model).stickerId);
                    if (debug)
                        Log.d(TAG, " found normal image in asset " + ((StickerModel) model).stickerId);
                } else {
                    ((StickerLocalStatusModel) model).localUri = "file:///android_asset/emoticons/" + stickerId + extension;
                }
                return true;
            }
        }
        return false;
    }

    private File exportFile(File src, File dst) throws IOException {
        Log.d(TAG, "exportFile: src " + src + " \n dest " + dst);
        //if folder does not exist
        if (!dst.exists()) {
            if (!dst.mkdir()) {
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File expFile = new File(dst.getPath() + File.separator + "data_" + timeStamp + ".ser");
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(expFile).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }

        return expFile;
    }

}
