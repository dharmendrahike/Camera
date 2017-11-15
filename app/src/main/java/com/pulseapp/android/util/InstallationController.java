package com.pulseapp.android.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Created by abc on 11/26/2015.
 */
public class InstallationController {
    private static String sID = null;
    private static final String INSTALLATION = "INSTALIVELY_INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

//    public static boolean hasDeviceId(Context context){
//        File installation = new File(context.getFilesDir(), INSTALLATION);
//        boolean has_device_id;
//        try {
//            if (!installation.exists()) {
//                has_device_id = false;
//            } else {
//                String id = readInstallationFile(installation);
//                if (id != null && id.equals("")){
//                    has_device_id = false;
//                } else if (id != null && !id.equals("")){
//                    has_device_id = true;
//                }else {
//                    has_device_id = false;
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return has_device_id;
//    }
}
