package com.pulseapp.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpStatus;
//import org.apache.http.HttpVersion;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.params.CoreProtocolPNames;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

//public static count = 0;
public class ImageStorage {
    // download function
    public void download(String url, ImageView imageView) {
        if (cancelPotentialDownload(url, imageView)) {
            // TODO:Caching code right here
            String filename = String.valueOf(url.hashCode());
            File f = new File(getCacheDirectory(imageView.getContext()), filename);

            // Is the bitmap in our cache?
            Bitmap bitmap = decodeFile(f);

            // No? download it
            if (bitmap == null) {
                System.out.println("downloading images ......");
                BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
//	    		imageView.setImageResource(R.drawable.profile_dummy_48);
                //imageView.setImageBitmap(GlobalVars.addWhiteBorderToBitmap(drawableToBitmap(downloadedDrawable)));
                task.execute(url);
            } else {
                // Yes? set the image
                //imageView.setImageBitmap(bitmap);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, options);

            // The new size we want to scale to
            final int REQ_SIZE = 70;

            // Raw height and width of image
            final int width = options.outWidth;
            final int height = options.outHeight;
            int scale = 1; // Find the correct scale value. It should be the power of 2.
            while (width / scale / 2 >= REQ_SIZE && height / scale / 2 >= REQ_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (Exception e) { /*e.printStackTrace();*/ }
        return null;
    }

    //cancel a download (internal only)
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url)))
                bitmapDownloaderTask.cancel(true);
            else {
                // The same URL is already downloaded
                return false;
            }
        }
        return true;
    }

    //gets an existing download if one exists for the imageview
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    //our caching functions
    // Find the dir to save cached images
    private static File getCacheDirectory(Context context) {
        String sdState = android.os.Environment.getExternalStorageState();
        File cacheDir;

        if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File sdDir = android.os.Environment.getExternalStorageDirectory();
            cacheDir = new File(sdDir, "data/imagedownloaded");
            Log.w("ImageDownloader","setting image view");
        } else
            cacheDir = context.getCacheDir();

        if (!cacheDir.exists())
            cacheDir.mkdirs();

        return cacheDir;
    }

    private void writeFile(Bitmap bmp, File f) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
        } catch (Exception e) { e.printStackTrace(); } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ex) {
            }
        }
    }
    ///////////////////////

    //download AsyncTask
    public class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private String url;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Actual download method, run in the task thread
        protected Bitmap doInBackground(String... params) {
            url = (String) params[0];
            Bitmap bitmap = null;
            return bitmap;
//            return downloadBitmap(params[0]);
        }

        // Once the image is downloaded, associate it to the imageView
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled())
                bitmap = null;

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
//                Log.w("ImageDownloader","setting image view");
//                    Log.w("ImageDownloader","setting image view");
                // Change bitmap only if this process is still associated with it
//                if (this == bitmapDownloaderTask) {
                    imageView.setImageBitmap(bitmap);

                    //cache the image
                    String filename = String.valueOf(url.hashCode());
                    File f = new File(getCacheDirectory(imageView.getContext()), filename);
                    writeFile(bitmap, f);
//                }
            }
        }
    }

    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.BLACK);
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    //the actual download code
//    static Bitmap downloadBitmap(String url) {
//        HttpParams params = new BasicHttpParams();
//        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
//        HttpClient client = new DefaultHttpClient(params);
//        final HttpGet getRequest = new HttpGet(url);
//
//        try {
//            HttpResponse response = client.execute(getRequest);
//            final int statusCode = response.getStatusLine().getStatusCode();
//            if (statusCode != HttpStatus.SC_OK) {
//                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
//                return null;
//            }
//
//            final HttpEntity entity = response.getEntity();
//            if (entity != null) {
//                InputStream inputStream = null;
//                try {
//
//                    inputStream = entity.getContent();
//                    Log.w("ImageDownloader", "Getting bitmap");
//                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                    return bitmap;
//                } finally {
//                    if (inputStream != null)
//                        inputStream.close();
//
//                    entity.consumeContent();
//                }
//            }
//        } catch (Exception e) {
//            // Could provide a more explicit error message for IOException or IllegalStateException
//            getRequest.abort();
//            Log.w("ImageDownloader", "Error while retrieving bitmap from " + url + e.toString());
//        } finally {
//            if (client != null) {
//                //client.close();
//            }
//        }
//        return null;
//    }
}