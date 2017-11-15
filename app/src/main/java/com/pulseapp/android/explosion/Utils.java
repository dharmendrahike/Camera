/*
 * Copyright (C) 2015 tyrantgit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pulseapp.android.explosion;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.pulseapp.android.activities.FacebookLogin;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;

public class Utils {

    private Utils() {
    }

    private static final float DENSITY = Resources.getSystem().getDisplayMetrics().density;
    private static final Canvas sCanvas = new Canvas();

    public static int dp2Px(int dp) {
        return Math.round(dp * DENSITY);
    }

    public static Bitmap createBitmapFromView(View view) {
        if (view instanceof ImageView) {
            Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable != null && drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
        }
        view.clearFocus();
        Bitmap bitmap = createBitmapSafely(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888, 1);
        if (bitmap != null) {
            synchronized (sCanvas) {
                Canvas canvas = sCanvas;
                canvas.setBitmap(bitmap);
                view.draw(canvas);
                canvas.setBitmap(null);
            }
        }
        return bitmap;
    }

    public static Bitmap createBitmapSafely(int width, int height, Bitmap.Config config, int retryCount) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (retryCount > 0) {
                System.gc();
                return createBitmapSafely(width, height, config, retryCount - 1);
            }
            return null;
        }
    }

    public static void logoutUser(Activity context) {
        Toast.makeText(context, "Logged out. Please reLogin.", Toast.LENGTH_SHORT).show();
        FacebookLogin.isActivityLaunched = false;
        deleteCache(context);
        context.getSharedPreferences(AppLibrary.APP_SETTINGS, 0).edit().clear().commit();
        context.startActivity(new Intent(context, FacebookLogin.class));
        context.finish();
        FireBaseHelper.getInstance(context).nullfySingleTon();
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            Log.e(Utils.class.getSimpleName(), "deleteCache");
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public static String getCustomMarquee(String name, int limit){
        if(name.length()>limit)
            name = name.substring(0,limit)+"...";
        return name;
    }
}
