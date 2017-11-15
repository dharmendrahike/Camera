package com.pulseapp.android.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.pulseapp.android.BuildConfig;
import com.pulseapp.android.R;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.fragments.ViewPublicMomentFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

/**
 * Created by deepankur on 11/1/16.
 * <p>
 * Share application specific files with other application using content provider
 */
public class InternalFileProvider {
    private static InternalFileProvider ourInstance = new InternalFileProvider();

    public static InternalFileProvider getInstance() {
        return ourInstance;
    }

    private InternalFileProvider() {
    }


    public void shareMedia(Activity activity, BaseFragment baseFragment, String handle, String name, String packageName, int mediaType, String localPath) {

        if (baseFragment instanceof ViewPublicMomentFragment && mediaType == AppLibrary.MEDIA_TYPE_VIDEO) {
            shareTheStream(activity, handle, name, packageName, mediaType, localPath);
            return;
        }

        File file = addToCache(activity, new File(localPath), mediaType);

        if (file == null) {
            Toast.makeText(activity, "Sorry! Something went wrong", Toast.LENGTH_SHORT).show();
            return;
        }

        // let the FileProvider generate an URI for this private file
        final Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".FileProvider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mediaType == AppLibrary.MEDIA_TYPE_IMAGE ? "image/*" : "video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (mediaType==AppLibrary.MEDIA_TYPE_VIDEO && packageName!=null && packageName.contains(AppLibrary.WHATSAPP_PACKAGE_NAME)) {
            //Do nothing as whatsapp doesn't handle extra text with direct video sharing currently
        }
        else {
            String shareBody;
            if (baseFragment instanceof ViewPublicMomentFragment)
                shareBody = "Check out '" + name + "' on Pulse -\nhttps://mypulse.tv/stream/" + handle;
            else
                shareBody = "Love it? Try out Pulse on \nhttps://www.mypulse.tv/app";

            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        }

        if (packageName != null) {
            if (AppLibrary.isPackageInstalled(packageName, activity)) {
                intent.setPackage(packageName);
                activity.startActivity(intent);
            } else {
                //let it be shared generally (choose activity and proceed)
                Toast.makeText(activity, "Whatsapp not installed on your device", Toast.LENGTH_SHORT).show();
                activity.startActivity(Intent.createChooser(intent, activity.getResources().getString(R.string.share_using)));
            }
        } else {
            activity.startActivity(Intent.createChooser(intent, activity.getResources().getString(R.string.share_using)));
        }
    }

    private void shareTheStream(Activity activity, String handle, String name, String packageName, int mediaType, String localPath) {

        String shareBody = "Check out '" + name + "' on Pulse -\nhttps://mypulse.tv/stream/" + handle;
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out " + name + " on Pulse");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);

        if (packageName != null) {
            if (AppLibrary.isPackageInstalled(packageName, activity)) {
                sharingIntent.setPackage(packageName);
                activity.startActivity(sharingIntent);
            } else {
                //let it be shared generally (choose activity and proceed)
                Toast.makeText(activity, "Whatsapp not installed on your device", Toast.LENGTH_SHORT).show();
                activity.startActivity(Intent.createChooser(sharingIntent, activity.getResources().getString(R.string.share_using)));
            }
        } else {
            activity.startActivity(Intent.createChooser(sharingIntent, activity.getResources().getString(R.string.share_using)));
        }
    }


    /**
     * @param activity
     * @param sourceFile the file form which to read
     * @param mediaType  whether {@link AppLibrary#MEDIA_TYPE_IMAGE} or {@link AppLibrary#MEDIA_TYPE_VIDEO}
     * @return the file written to cache
     * the source file {documents/share.png}
     * be will overwritten in some point in future
     */
    private File addToCache(Activity activity, File sourceFile, int mediaType) {
        File destinationFile;
        if (mediaType == AppLibrary.MEDIA_TYPE_IMAGE)
            destinationFile = new File(activity.getCacheDir(), "documents/share.jpg");
        else
            destinationFile = new File(activity.getCacheDir(), "documents/share.mp4");

        createParentDirectories(destinationFile);
        try {
            copyFile(sourceFile, destinationFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return destinationFile;
    }


    private static void copyFile(File sourceFile, File destFile)
            throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    private static void createParentDirectories(final File inFile) {
        if (inFile != null) {
            final File parentDir = inFile.getParentFile();

            if ((parentDir != null) && !parentDir.exists()) {
                parentDir.mkdirs();
            }
        }
    }
}
