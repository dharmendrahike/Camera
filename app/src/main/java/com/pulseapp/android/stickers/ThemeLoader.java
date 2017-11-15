package com.pulseapp.android.stickers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by deepankur on 2/2/16.
 */
public class ThemeLoader {

    public static SparseArray<ThemeModel> sThemeModelMap;
    private Context context = null;
    private final static String TAG = "ThemeLoader";
    ArrayList<ThemeModel> themeModelList;

    public ThemeLoader(Context context, ArrayList<ThemeModel> themeModelList) {
        this.context = context;
        sThemeModelMap = new SparseArray<>();
        this.themeModelList = themeModelList;
        syncBitmapFromFireBase();
        for (int i = 0; i < themeModelList.size(); i++)
            sThemeModelMap.put(i, themeModelList.get(i));
    }

    /**
     * Picasso keeps weak reference to the target and hence target's instance is
     * garbage collected.
     */
    private Set<Target> protectedFromGarbageCollectorTargets = new HashSet<>();

    public static void destroy() {
        if (sThemeModelMap != null) {
            for (int i = 0; i < sThemeModelMap.size(); i++) {
                int key = sThemeModelMap.keyAt(i);
                ThemeModel themeModel = sThemeModelMap.get(key);
                for (int j = 0; j < themeModel.getAssets().length; j++) {
                    Bitmap bitmap = themeModel.getAssets()[j].mBitmap;
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
            }
            sThemeModelMap = null;
        }
    }

    private class UrlTarget implements Target {
        String TAG = "ThemeLoader";
        ThemeAsset themeAsset;
        Context context;

        public UrlTarget(ThemeAsset themeAsset, Context context) {
            this.themeAsset = themeAsset;
            this.context = context;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.d(TAG, "onBitmapLoaded");
            this.themeAsset.mBitmap = bitmap;
            writeThisBitmapToInternalStorage(String.valueOf(this.themeAsset.mStickerId), bitmap);
            protectedFromGarbageCollectorTargets.remove(this);
        }

        private void writeThisBitmapToInternalStorage(String stickerId, Bitmap bitmap) {
            File file = new File(context.getFilesDir(), stickerId);
            FileOutputStream fOutputStream;
            try {
                fOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream);
                fOutputStream.flush();
                fOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "exception FNF" + e);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "exception IO" + e);
            }
        }


        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(TAG, "onBitmapFailed");
            protectedFromGarbageCollectorTargets.remove(this);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d(TAG, "onPrepareLoad");
        }
    }

    String IMAGES_HOST_URL = "";

    private Bitmap readFromInternalDirectory(String stickerId) {
        String filePath = context.getFilesDir() + "/" + stickerId;
        File file = new File(filePath);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void syncBitmapFromFireBase() {
        for (ThemeModel themeModel : themeModelList) {
            for (ThemeAsset themeAsset : themeModel.getAssets()) {
                Bitmap bitmap = readFromInternalDirectory(themeAsset.mStickerId);
                if (bitmap == null) {
                    Log.d(TAG, "bitmap not found internally");
                    UrlTarget urlTarget = new UrlTarget(themeAsset, context);
                    protectedFromGarbageCollectorTargets.add(urlTarget);
                    Picasso.with(context).load(IMAGES_HOST_URL + themeAsset.mUrl).into(urlTarget);
                } else themeAsset.mBitmap = bitmap;
            }
        }
    }
}