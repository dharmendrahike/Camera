package com.pulseapp.android.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by deepankur on 8/10/16.
 */
public class StorageHelper {

    private static String TAG = "StorageHelper";
    final static boolean testing = !AppLibrary.PRODUCTION_MODE;

    public static void printStats(Context context) {
        Log.d(TAG, "getAvailableInternalMemorySize " + getAvailableInternalMemorySize());
        Log.d(TAG, "getTotalInternalMemorySize " + getTotalInternalMemorySize());
        Log.d(TAG, "getAvailableExternalMemorySize " + getAvailableExternalMemorySize());
        Log.d(TAG, "getTotalExternalMemorySize " + getTotalExternalMemorySize());
        Log.d(TAG, "ram " + ram(context));
//        if (testing)
//            testTime();

    }

    private static String ERROR;

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return formatSize(availableBlocks * blockSize);
    }

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return formatSize(totalBlocks * blockSize);
    }

    public static String getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return formatSize(availableBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    public static String getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return formatSize(totalBlocks * blockSize);
        } else {
            return ERROR;
        }
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    static long ram(Context context) {
        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return memInfo.totalMem;
    }

    static ArrayList<Test> arrayList = new ArrayList<>();

    static void testTime() {
        Test test = null;
        Log.d(TAG, "t1 " + System.currentTimeMillis());
        for (int i = 0; i < 50000; i++) {
            Test t = new Test("index " + i, i * 10);
            arrayList.add(t);
            if (i == 450) test = t;
        }
        Log.d(TAG, "t2 " + System.currentTimeMillis());
        arrayList.remove(0);
        Log.d(TAG, "index is " + arrayList.indexOf(test));
        arrayList.contains(test);
        arrayList.clear();
        Log.d(TAG, "t3 " + System.currentTimeMillis());
    }

    static class Test {
        String a;
        int b;

        public Test(String a, int b) {
            this.a = a;
            this.b = b;
        }
    }
}