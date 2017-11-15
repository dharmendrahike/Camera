package com.pulseapp.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.pulseapp.android.activities.CameraActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by deepankur on 12/21/16.
 */

public class MiuiPermissionHandler {

    public static void openMiuiPermissionActivity(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
        String ROM = getMiUiVersionProperty();
        if (TextUtils.equals(ROM, "V5")) {
            PackageInfo pInfo = null;
            try {
                pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            intent.setClassName("com.android.settings", "com.miui.securitycenter.permission.AppPermissionsEditor");
            intent.putExtra("extra_package_uid", pInfo.applicationInfo.uid);
        } else if (TextUtils.equals(ROM, "V6") || TextUtils.equals(ROM, "V7") || TextUtils.equals(ROM, "V8")) {
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
        } else {
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
        }
        if (isIntentAvailable(context, intent) && (context instanceof Activity)) {
            ((Activity) context).startActivityForResult(intent, 2);
        }
    }

    public static String getMiUiVersionProperty() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("getprop ro.miui.ui.version.name").getInputStream()), 1024);
            String line = reader.readLine();
            reader.close();
            return line;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isMIUIV8() {
        String version = getMiUiVersionProperty();
        return !(version == null || !TextUtils.equals(version, "V8"));
    }

    private static boolean isIntentAvailable(Context context, Intent Intent) {
        boolean z = true;
        if (Intent == null) {
            return false;
        }
        if (context.getPackageManager().queryIntentActivities(Intent, 1).size() <= 0) {
            z = false;
        }
        return z;
    }

}
