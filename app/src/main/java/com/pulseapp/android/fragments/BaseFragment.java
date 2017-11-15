package com.pulseapp.android.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.CustomMomentModel;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.InternalFileProvider;
import com.pulseapp.android.util.RoundedTransformation;
import com.pulseapp.android.util.WriteImageCallback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by deepankur on 04-12-2015.
 * <p/>
 * <p/>
 * Fragment used for all common functions of children
 */

public abstract class BaseFragment extends Fragment implements FireBaseKEYIDS {

    public final static String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    protected final String FB_MESSENGER_PACKAGE_NAME = "com.facebook.orca";

    static BaseFragment baseFragment;
    private boolean registerForEvents = false;
    boolean isInternetPresent = false;
    private static String TAG = "BaseFragment";
    protected static String myUserId;
    protected static FireBaseHelper mFireBaseHelper;
    protected Context context;
    protected static FontPicker fontPicker;
    private CountDownTimer toastCountDown;
    public FacebookController facebookController;

    public SharedPreferences getPreferences() {
        return preferences;
    }

    private SharedPreferences preferences;
    protected final boolean TESTING = !AppLibrary.PRODUCTION_MODE;

    public abstract void onEvent(BroadCastSignals.BaseSignal eventSignal);

    protected void registerForInAppSignals(boolean flag) {
        this.registerForEvents = flag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            facebookController = (FacebookController) context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            return;

        if (baseFragment == null || baseFragment.getContext() == null)
            baseFragment = this;
        context = getActivity();
        if (this.registerForEvents) {
            MasterClass.getEventBus().register(this);
        }
        preferences = getActivity().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        mFireBaseHelper = FireBaseHelper.getInstance(getActivity());
        myUserId = preferences.getString(AppLibrary.USER_LOGIN, "");
        if (fontPicker == null)
            fontPicker = FontPicker.getInstance(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.registerForEvents) {
            MasterClass.getEventBus().unregister(this);
        }
    }

    public static void setViewAndChildrenVisibility(View view, boolean enabled) {
        if (enabled)
            view.setVisibility(View.VISIBLE);
        else view.setVisibility(View.GONE);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenVisibility(child, enabled);
            }
        }
    }


    public static void enableTheseViews(View[] views) {

    }

    public static void toggleVisibility(View view, Boolean isVisible) {
        if (isVisible)
            view.setVisibility(View.VISIBLE);
        else if (!isVisible) view.setVisibility(View.GONE);

    }

    public static void showShortToastMessage(String object) {
        Toast toast = Toast.makeText(MasterClass.getGlobalContext(), object, Toast.LENGTH_SHORT);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if (v != null) v.setGravity(Gravity.CENTER);
        toast.show();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isInternetAvailable(boolean showNoInternetToast) {
        try {
            isInternetPresent = isNetworkAvailable(getActivity());
            if (!isInternetPresent) {
                if (showNoInternetToast)
                    Toast.makeText(getActivity(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isInternetPresent;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

//    public void showShortToastMessage(String message) {
//        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
//    }

    public static String getActionName(final int action) {

        switch (action) {
            case 0:
                return "ACTION_DOWN";
            case 1:
                return "ACTION_UP";
            case 2:
                return "ACTION_MOVE";
            case 3:
                return "ACTION_CANCEL";
            case 4:
                return "ACTION_OUTSIDE";
            case 5:
                return "ACTION_POINTER_DOWN";
            case 6:
                return "ACTION_POINTER_UP";
            case 7:
                return "ACTION_HOVER_MOVE";
            default:
                Log.w(TAG, "Warning:  Define this action above ");
                return null;
        }

    }

    /**
     * @param view the View Under Inspection
     * @param rx   x Coordinate for eg. event.getX()
     * @param ry   y Coordinate for eg. event.getY()
     * @return whether the point lies inside the bounding box of the view
     */
    public boolean isViewContains(View view, int rx, int ry) {
        int[] l = new int[2];
        view.getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = view.getWidth();
        int h = view.getHeight();

        if (rx < x || rx > x + w || ry < y || ry > y + h) {
            return false;
        }
        return true;
    }

    /**
     * Compute the dot product AB . AC
     */
    private double DotProduct(double[] pointA, double[] pointB, double[] pointC) {
        double[] AB = new double[2];
        double[] BC = new double[2];
        AB[0] = pointB[0] - pointA[0];
        AB[1] = pointB[1] - pointA[1];
        BC[0] = pointC[0] - pointB[0];
        BC[1] = pointC[1] - pointB[1];
        double dot = AB[0] * BC[0] + AB[1] * BC[1];

        return dot;
    }

    /**
     * Compute the cross product AB x AC
     */
    private double CrossProduct(double[] pointA, double[] pointB, double[] pointC) {
        double[] AB = new double[2];
        double[] AC = new double[2];
        AB[0] = pointB[0] - pointA[0];
        AB[1] = pointB[1] - pointA[1];
        AC[0] = pointC[0] - pointA[0];
        AC[1] = pointC[1] - pointA[1];
        double cross = AB[0] * AC[1] - AB[1] * AC[0];

        return cross;
    }

    /**
     * Compute the distance from A to B
     */
    double Distance(double[] pointA, double[] pointB) {
        double d1 = pointA[0] - pointB[0];
        double d2 = pointA[1] - pointB[1];

        return Math.sqrt(d1 * d1 + d2 * d2);
    }

    /**
     * Compute the distance from AB to C
     * if isSegment is true, AB is a segment, not a line.
     */
    public double LineToPointDistance2D(double[] pointA, double[] pointB, double[] pointC,
                                        boolean isSegment) {
        double dist = CrossProduct(pointA, pointB, pointC) / Distance(pointA, pointB);
        if (isSegment) {
            double dot1 = DotProduct(pointA, pointB, pointC);
            if (dot1 > 0)
                return Distance(pointB, pointC);

            double dot2 = DotProduct(pointB, pointA, pointC);
            if (dot2 > 0)
                return Distance(pointA, pointC);
        }
        return Math.abs(dist);
    }

    public Bitmap scaleBitmap(Bitmap bitmap, float outputResolutionWidth) {
        float scaleX = 1;
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        Log.d(TAG, " bitmap before scaling height " + height + " width " + width);
        if (width > outputResolutionWidth) {
            scaleX = outputResolutionWidth / width;
            Bitmap b;
            try {
                b = bitmapResizer(bitmap, (int) (width * scaleX), (int) (height * scaleX));
                Log.d(TAG, " scaled bitmap height " + b.getHeight() + " width " + b.getWidth());
                if (bitmap != null)
                    bitmap.recycle();
            } catch (Exception e) {
                b = bitmap;
            }
//          Bitmap b = Bitmap.createScaledBitmap(bitmap, (int) (width * scaleX), (int) (height * scaleX), true);
            return b;
        } else {
            return bitmap;
        }
    }

    public Bitmap bitmapResizer(Bitmap bitmap, int newWidth, int newHeight) throws Exception {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    File editedFile = null;
//    public void saveBitmap(Bitmap bitmap,String suffix,Context mContext) {
//        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
//        Time today = new Time(Time.getCurrentTimezone());
//        today.setToNow();
//        String name = date + "_" + today.hour + today.minute + today.second +suffix +".jpg";
//        String mediaStoragePath = AppLibrary.getFilesDirectory(mContext);
//        File mediaDir = new File(mediaStoragePath);
//        if (!mediaDir.exists())
//            mediaDir.mkdirs();
//        editedFile = new File(mediaDir, name);
//        writeFile(bitmap, editedFile,true);
//    }

    public void setImageFilePath(boolean justSave, Context mContext, int MEDIA_TYPE, String suffix) {
        if (MEDIA_TYPE == AppLibrary.MEDIA_TYPE_IMAGE) {
            if (!justSave) {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                Time today = new Time(Time.getCurrentTimezone());
                today.setToNow();
                String name = date + "_" + today.hour + today.minute + today.second + suffix + ".webp";

                String mediaStoragePath = AppLibrary.getCreatedMediaDirectory(MasterClass.getGlobalContext()); //Uses local files directory

                File mediaDir = new File(mediaStoragePath);
                if (!mediaDir.exists())
                    mediaDir.mkdirs();
                editedFile = new File(mediaDir, name);
            } else {

                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                Time today = new Time(Time.getCurrentTimezone());
                today.setToNow();
                String name = date + "_" + today.hour + today.minute + today.second + suffix + ".jpg";

                String mediaStoragePath = AppLibrary.setupOutputDirectoryForRecordedFile(); //Uses external SD card

                File mediaDir = new File(mediaStoragePath);
                if (!mediaDir.exists())
                    mediaDir.mkdirs();
                editedFile = new File(mediaDir, name);
            }
        }
    }

    public void writeFile(Bitmap bmp, File f, boolean justSave, boolean gotoShareScreen, WriteImageCallback writeImageCallback) {
        new WriteImageTask(bmp, f, justSave, gotoShareScreen, writeImageCallback).execute();
    }

    public void writeFile(Bitmap bmp, File f, boolean justSave, boolean gotoShareScreen, WriteImageCallback writeImageCallback,boolean showToastAfterSaving) {
        new WriteImageTask(bmp, f, justSave, gotoShareScreen, writeImageCallback,showToastAfterSaving).execute();
    }

    public Bitmap addWaterMarkToBitmap(Bitmap bitmap, Bitmap waterMark) {

        Bitmap result = Bitmap.createBitmap(bitmap);
        Canvas canvas = new Canvas(result);
        try {
            Bitmap waterMarkScaled=bitmapResizer(waterMark,(int)((canvas.getWidth()*12.23)/100.0),(int)((canvas.getHeight()*9.7)/100.0));
            canvas.drawBitmap(waterMarkScaled, canvas.getWidth()-16-waterMarkScaled.getWidth(), canvas.getHeight()-12-waterMarkScaled.getHeight(), null);   //drawing watermark bitmap at the end
        } catch (Exception e) {
            e.printStackTrace(); //Couldn't draw overlay
        }

        if (bitmap!=null)
            bitmap.recycle();

        return result;
    }


    private class WriteImageTask extends AsyncTask<Void, Void, Object> {

        File f;
        Bitmap bitmap;
        boolean justSave;
        boolean gotoShareScreen;
        WriteImageCallback writeImageCallback;
        final float MAX_WIDTH_RESOLUTION = 720f;
        private  boolean showToastAfterSaving=true;

        public WriteImageTask(Bitmap bmp, File f, boolean justSave, boolean gotoShareScreen, WriteImageCallback writeImageCallback, boolean showToastAfterSaving) {
            this.bitmap = bmp;
            this.f = f;
            this.justSave = justSave;
            this.gotoShareScreen = gotoShareScreen;
            this.writeImageCallback = writeImageCallback;
            this.showToastAfterSaving = showToastAfterSaving;
        }

        public WriteImageTask(Bitmap bmp, File f, boolean justSave, boolean gotoShareScreen, WriteImageCallback writeImageCallback) {
            this.bitmap = bmp;
            this.f = f;
            this.justSave = justSave;
            this.gotoShareScreen = gotoShareScreen;
            this.writeImageCallback = writeImageCallback;
        }

        @Override
        protected Object doInBackground(Void... params) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(f);
                if (justSave)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                else {
                    bitmap = scaleBitmap(bitmap, MAX_WIDTH_RESOLUTION);
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                    bitmap.recycle();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            bitmap.recycle();
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (this.justSave) {
                if (this.showToastAfterSaving)
                    showShortToastMessage("Saved to Gallery");
                addRecordingToMediaLibrary(true);
            }
            if (!this.gotoShareScreen) {
                if (writeImageCallback != null)
                    writeImageCallback.onWriteImageComplete();
            }
        }
    }

    void addRecordingToMediaLibrary(boolean image) {
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        if (image) {
            values.put(MediaStore.Images.Media.TITLE, editedFile.getName());
            values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current / 1000));
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATA, editedFile.getAbsolutePath());
        } else {
            values.put(MediaStore.Video.Media.TITLE, editedFile.getName());
            values.put(MediaStore.Video.Media.DATE_ADDED, (int) (current / 1000));
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, editedFile.getAbsolutePath());
        }

        if (getActivity() != null) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            Uri base;
            if (image)
                base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            else
                base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            try {
                Uri newUri = contentResolver.insert(base, values);
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static int getDIP(Context context, int value) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
    }

    public long utcToEpoch(String utcTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINA);
        Date date = null;
        try {
            date = sdf.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long mills = 0;//date.getTime();
        TimeZone t = TimeZone.getDefault();
        Log.d(TAG, " time zone" + t);
        Log.d(TAG, " returning " + mills + " for " + utcTime);
        return System.currentTimeMillis();
    }

//    protected String getUtcFromEpoch(long unixMilliSeconds, boolean accountServerOffset) {
//        if (accountServerOffset)
//            unixMilliSeconds -= (mFireBaseHelper.getServerOffsetTime());
//        Date date = new Date(unixMilliSeconds);
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // the format of your date
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//        return sdf.format(date);
//    }

    public void showRevealAnimation(final boolean reverse, final View revealView, final CanvasFragment.AnimationListeners animationListeners) {

        int cx = revealView.getRight() - AppLibrary.convertDpToPixels(context, 40);
        int cy = revealView.getBottom() - AppLibrary.convertDpToPixels(context, 40);

        int radius = Math.max(revealView.getWidth(), revealView.getHeight());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SupportAnimator animator, animator_reverse;
            animator = ViewAnimationUtils.createCircularReveal(revealView, cx, cy, 0, radius);
            if (reverse)
                animator = animator.reverse();
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(400);
            animator.addListener(new SupportAnimator.AnimatorListener() {
                @Override
                public void onAnimationStart() {
                    if (!reverse)
                        revealView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd() {
                    if (reverse)
                        revealView.setVisibility(View.GONE);
                    else
                        animationListeners.onAnimationEnd();
                }

                @Override
                public void onAnimationCancel() {

                }

                @Override
                public void onAnimationRepeat() {

                }
            });

            animator.start();
        } else {
            Animator animator;
            if (reverse) {
                animator = android.view.ViewAnimationUtils.createCircularReveal(revealView, cx, cy, radius, 0);
            } else {
                animator = android.view.ViewAnimationUtils.createCircularReveal(revealView, cx, cy, 0, radius);
            }
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (!reverse)
                        revealView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (reverse) {
                        revealView.setVisibility(View.GONE);
                    } else {
                        animationListeners.onAnimationEnd();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setDuration(400);
            animator.start();
        }
    }

    protected void toggleSoftKeyboard(Context context, EditText editText, boolean showKeyBoard) {

        InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (showKeyBoard) {
            mgr.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
            return;
        }

        // check if no view has focus:
        View v = ((Activity) context).getCurrentFocus();
        if (v == null)
            return;

        mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    public float pixelsToDp(float px) {
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    protected void traverse(@Nullable String TAG, File dir) {
        if (TAG == null) TAG = this.TAG;
        Log.d(TAG, " transversing");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    Log.d(TAG, " directory: " + file);
                    traverse(TAG, file);
                } else {
                    Log.d(TAG, " file: " + file);
                }
            }
        }
    }

    public static String buildStackTraceString(final StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        if (elements != null && elements.length > 0) {
            for (StackTraceElement element : elements) {
                sb.append(element.toString());
            }
        }
        return sb.toString();
    }

    public enum PopupType {GENERIC_PROFILE_POPUP, INCOMING_PROFILE_POPUP, STREAMS_POPUP, LOGOUT_POPUP, SHARE_USER_PROFILE, FOLLOW_POPUP}


    public void showGenericProfilePopup(Context context, String name, String imageUrl, String handle) {
        showPopup(PopupType.GENERIC_PROFILE_POPUP, context, null, name, imageUrl, handle, null, false, false, 0, false);
    }

    public void showIncomingProfilePopup(Context context, String userId, String name, String imageUrl, String handle) {
        showPopup(PopupType.INCOMING_PROFILE_POPUP, context, userId, name, imageUrl, handle, null, false, false, 0, false);
    }

    public void showStreamPopup(Context context, String momentId, String name, String imageUrl, String handle, String description, boolean isAnArticle) {
        showPopup(PopupType.STREAMS_POPUP, context, momentId, name, imageUrl, handle, description, false, false, 0, isAnArticle);
    }

    public void showFollowPopup(long createdAt, Context context, String userId, String name, String imageUrl, String handle, boolean following, boolean notificationEnabled) {
        showPopup(PopupType.FOLLOW_POPUP, context, userId, name, imageUrl, handle, null, following, notificationEnabled, createdAt, false);
    }

    private static StreamsPopupListener streamsPopupListener;
    private static FollowPopupListener followPopupListener;

    public void setStreamsPopupListener(StreamsPopupListener streamsPopupListener) {
        BaseFragment.streamsPopupListener = streamsPopupListener;
    }

    public void setFollowPopupListener(FollowPopupListener followPopupListener) {
        BaseFragment.followPopupListener = followPopupListener;
    }

    public void showLogoutPopup(Context context, String name) {
        showPopup(PopupType.LOGOUT_POPUP, context, null, name, null, null, null, false, false, 0, false);
    }

    public interface FacebookController {
        void doFacebookLogin(List<String> permissions, CameraActivity.FacebookLoginCallback facebookLoginCallback);
    }

    public interface StreamsPopupListener {
        void onWatchNowClicked(HomeMomentViewModel streamId);
    }

    public interface FollowPopupListener {
        void onButtonClicked(View view, String userId, Dialog popup);

        void onPopupDismiss();
    }

    public void showFacebookSharePopup(final CameraActivity.SharePopupCallbacks sharePopupCallbacks) {
        final Dialog popup = new Dialog(context, android.R.style.Theme_Translucent);
        popup.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        popup.requestWindowFeature(Window.FEATURE_NO_TITLE);
        popup.setContentView(R.layout.facebook_share_popup);
        popup.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        LinearLayout facebookLayout = (LinearLayout) popup.findViewById(R.id.facebookShare);
        LinearLayout intentShareLayout = (LinearLayout) popup.findViewById(R.id.intentShare);
        LinearLayout watsAppShareLayout = (LinearLayout) popup.findViewById(R.id.watsappShare);

        facebookLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharePopupCallbacks != null)
                    sharePopupCallbacks.onShareFacebookClicked();
                dismissPopup(popup);
            }
        });

        intentShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePopupCallbacks.onShareIntentClicked();
                dismissPopup(popup);
            }
        });

        watsAppShareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePopupCallbacks.onWatsAppShareClicked();
                dismissPopup(popup);
            }
        });

        {//animation
            popup.findViewById(R.id.tintFrame).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissPopup(popup);
                    if (sharePopupCallbacks != null)
                        sharePopupCallbacks.onPopupDismiss();
                }
            });

            popup.findViewById(R.id.popupView).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) { //By default, all events are intercepted by this listener first
                return true;
                }
            });

            popup.findViewById(R.id.popupView).setTranslationY(1000);
            popup.findViewById(R.id.popupView).animate().translationY(0).setDuration(300).start();
            ObjectAnimator.ofFloat(popup.findViewById(R.id.tintFrame), "alpha", 0f, 1f).setDuration(300).start();
            popup.show();
        }
    }

    /**
     * @param popupType
     * @param context
     * @param id
     * @param name
     * @param imageUrl
     * @param handle
     * @param subDescription
     * @param following
     * @param notificationEnabled
     * @param createdAt
     * @param isAnArticle         --> used only when called from {@link #showStreamPopup(Context, String, String, String, String, String, boolean)},
     *                            kept as false otherwise
     */
    @SuppressWarnings("JavaDoc")
    private void showPopup(final PopupType popupType, final Context context, final String id, final String name, final String imageUrl, final String handle
            , String subDescription, boolean following, boolean notificationEnabled, final long createdAt, final boolean isAnArticle) {
        final Dialog popup = new Dialog(context, android.R.style.Theme_Translucent);
        popup.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        popup.requestWindowFeature(Window.FEATURE_NO_TITLE);
        popup.setContentView(R.layout.dialog_share_popup);
        popup.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        ImageView profileIv = (ImageView) popup.findViewById(R.id.profileIV);
        Picasso.with(context).load(imageUrl).transform(new RoundedTransformation()).into(profileIv);

        TextView popupName, popupSubDescription;
        View share;
        TextView actionBtn;
        LinearLayout followLayout;

        popupName = (TextView) popup.findViewById(R.id.nameTV);
        popupSubDescription = (TextView) popup.findViewById(R.id.handleTV);
        actionBtn = (TextView) popup.findViewById(R.id.watchNow);
        share = popup.findViewById(R.id.shareImageView);
        followLayout = (LinearLayout) popup.findViewById(R.id.followLayout);
        popupName.setText(name);
        final String popupText = popupName.getText().toString();
        popupSubDescription.setText("@" + handle);

        switch (popupType) {
            case GENERIC_PROFILE_POPUP:
                actionBtn.setVisibility(View.GONE);
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchShareIntent(handle, true, popupText, null);
                        dismissPopup(popup);
                    }
                });
                break;
            case STREAMS_POPUP:
                if (subDescription != null) {
                    popupSubDescription.setVisibility(View.VISIBLE);
                    popupSubDescription.setText(subDescription);
                } else
                    popupSubDescription.setVisibility(View.GONE);
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchShareIntent(handle, false, popupText, null);
                        dismissPopup(popup);
                    }
                });
                actionBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (streamsPopupListener != null) {
                            HomeMomentViewModel momentViewModel = new HomeMomentViewModel();
                            momentViewModel.momentId = id;
                            momentViewModel.imageUrl = imageUrl;
                            momentViewModel.name = name;
                            momentViewModel.handle = handle;
                            momentViewModel.isAnArticle = isAnArticle;
                            streamsPopupListener.onWatchNowClicked(momentViewModel);
                        }
                        dismissPopup(popup);
                    }
                });

                break;
            case INCOMING_PROFILE_POPUP:
                initAddBtnText(actionBtn, id);
//                share.setVisibility(View.GONE);
                actionBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final SocialModel socialModel = mFireBaseHelper.getSocialModel();
                        if (socialModel == null || socialModel.requestReceived == null) {
                            mFireBaseHelper.sendFriendRequest(myUserId, id);
                            showShortToastMessage("Friend request sent");
                        }
                        if (socialModel != null && socialModel.requestReceived != null) {
                            if (socialModel.requestReceived.containsKey(id)) {//this is a recieved request
                                mFireBaseHelper.acceptFriendRequest(myUserId, id);
                            } else {
                                mFireBaseHelper.sendFriendRequest(myUserId, id);
                                showShortToastMessage("Friend request sent");
                            }
                        }
                        dismissPopup(popup);
                    }
                });
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchShareIntent(handle, true, popupText, null);
                        dismissPopup(popup);
                    }
                });

                break;
            case SHARE_USER_PROFILE:
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
                break;

            case LOGOUT_POPUP:
//                profileIv.setVisibility(View.GONE);
                profileIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.pulse_icon));
                share.setVisibility(View.GONE);
                popupSubDescription.setVisibility(View.GONE);
                actionBtn.setText("LOGOUT");
                actionBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissPopup(popup);
                        Utils.logoutUser(getActivity());
                    }
                });
                break;
            case FOLLOW_POPUP:
                followLayout.setVisibility(View.VISIBLE);
                actionBtn.setVisibility(View.GONE);
                ((ImageView) share).setImageResource(R.drawable.share_gray_svg);
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchShareIntent(handle, true, popupText, null);
                        dismissPopup(popup);
                        followPopupListener.onButtonClicked(v, id, popup);
                    }
                });
                if (following) {
                    ((ImageView) popup.findViewById(R.id.followIcon)).setImageResource(R.drawable.follow_check);
                    ((TextView) popup.findViewById(R.id.followText)).setText("FOLLOWING");
                    ((TextView) popup.findViewById(R.id.followText)).setTextColor(getResources().getColor(R.color.white));
//                    popup.findViewById(R.id.notificationLayout).setVisibility(View.VISIBLE);
//                    popup.findViewById(R.id.notificationIcon).setTag(notificationEnabled);
//                    if (notificationEnabled) {
//                        ((ImageView)popup.findViewById(R.id.notificationIcon)).setImageResource(R.drawable.notifications_on);
//                    } else {
//                        ((ImageView)popup.findViewById(R.id.notificationIcon)).setImageResource(R.drawable.notifications_off);
//                    }
//                    popup.findViewById(R.id.notificationLayout).setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            boolean notificationEnabled = (boolean) v.getTag();
//                            if (notificationEnabled) {
//                                ((ImageView)popup.findViewById(R.id.notificationIcon)).setImageResource(R.drawable.notifications_off);
//                            } else {
//                                ((ImageView)popup.findViewById(R.id.notificationIcon)).setImageResource(R.drawable.notifications_on);
//                            }
//                        }
//                    });
                    (popup.findViewById(R.id.followlv)).setBackgroundResource(R.drawable.cornered_pulse_theme_background);
                } else {
                    ((ImageView) popup.findViewById(R.id.followIcon)).setImageResource(R.drawable.follow_add);
                    ((TextView) popup.findViewById(R.id.followText)).setText("FOLLOW");
                    ((TextView) popup.findViewById(R.id.followText)).setTextColor(getResources().getColor(R.color.pulse_theme_color));
//                    popup.findViewById(R.id.notificationLayout).setVisibility(View.GONE);
                    (popup.findViewById(R.id.followlv)).setBackgroundResource(R.drawable.follow_user_popup_background);
                }
                popup.findViewById(R.id.followlv).setTag(following);
                popup.findViewById(R.id.followlv).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean following = (boolean) v.getTag();
                        if (following) {
                            mFireBaseHelper.unFollowUser(id);
                            followPopupListener.onButtonClicked(v, id, popup);
                            ((ImageView) popup.findViewById(R.id.followIcon)).setImageResource(R.drawable.follow_add);
                            ((TextView) popup.findViewById(R.id.followText)).setText("FOLLOW");
                            ((TextView) popup.findViewById(R.id.followText)).setTextColor(getResources().getColor(R.color.pulse_theme_color));
                            popup.findViewById(R.id.followlv).setTag(false);
                            popup.findViewById(R.id.notificationLayout).setVisibility(View.GONE);
                            (popup.findViewById(R.id.followlv)).setBackgroundResource(R.drawable.follow_user_popup_background);
                        } else {
                            mFireBaseHelper.followUser(id, createdAt);
                            followPopupListener.onButtonClicked(v, id, popup);
                            ((ImageView) popup.findViewById(R.id.followIcon)).setImageResource(R.drawable.follow_check);
                            ((TextView) popup.findViewById(R.id.followText)).setText("FOLLOWING");
                            ((TextView) popup.findViewById(R.id.followText)).setTextColor(getResources().getColor(R.color.white));
                            popup.findViewById(R.id.followlv).setTag(true);
                            popup.findViewById(R.id.notificationLayout).setVisibility(View.GONE);
                            (popup.findViewById(R.id.followlv)).setBackgroundResource(R.drawable.cornered_pulse_theme_background);
                        }
                    }
                });
        }

        {//animation
            popup.findViewById(R.id.tintFrame).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissPopup(popup);
                    if (followPopupListener != null)
                        followPopupListener.onPopupDismiss();
                }
            });

            popup.findViewById(R.id.popupView).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) { //By default, all events are intercepted by this listener first
                    return true;
                }
            });

            popup.findViewById(R.id.popupView).setTranslationY(1000);
            popup.findViewById(R.id.popupView).animate().translationY(0).setDuration(300).start();
            ObjectAnimator.ofFloat(popup.findViewById(R.id.tintFrame), "alpha", 0f, 1f).setDuration(300).start();
            popup.show();
        }
    }

    public void dismissPopup(final Dialog popup) {
        if (context != null && context instanceof CameraActivity)
            ((CameraActivity) context).toggleFullScreen(false);
        ObjectAnimator.ofFloat(popup.findViewById(R.id.tintFrame), "alpha", 1f, 0f).setDuration(300).start();
        popup.findViewById(R.id.popupView).animate().translationY(1000).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                popup.dismiss();
            }
        }).start();
    }

    public Dialog showCustomDialog(boolean showHeader, Context context, String header, String bodyText, String positiveBtnText, String negativeBtnText, View.OnClickListener listener) {
        final Dialog customDialog = new Dialog(context, android.R.style.Theme_Translucent);
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setContentView(R.layout.dialog_header_two_buttons);
        customDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        customDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        if (!showHeader)
            customDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); //To maintain full-screen in the discard popup

        TextView tvHeader, tvBody, tvPositive, tvNegative;

        tvHeader = (TextView) customDialog.findViewById(R.id.tv_header);
        tvBody = (TextView) customDialog.findViewById(R.id.tv_dialog_body);
        tvNegative = (TextView) customDialog.findViewById(R.id.btn_negative);
        tvPositive = (TextView) customDialog.findViewById(R.id.btn_positive);

        if (showHeader)
            tvHeader.setText(header);
        else
            customDialog.findViewById(R.id.ll_header_container).setVisibility(View.GONE);

        tvBody.setText(bodyText);
        tvNegative.setText(negativeBtnText);
        tvPositive.setText(positiveBtnText);

        tvNegative.setOnClickListener(listener);
        tvPositive.setOnClickListener(listener);

        customDialog.findViewById(R.id.tintFrame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissPopup(customDialog);
            }
        });

        customDialog.findViewById(R.id.popupView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { //By default, all events are intercepted by this listener first
                return true;
            }
        });

        customDialog.findViewById(R.id.popupView).setTranslationY(1000);
        customDialog.findViewById(R.id.popupView).animate().translationY(0).setDuration(300).start();
        ObjectAnimator.ofFloat(customDialog.findViewById(R.id.tintFrame), "alpha", 0f, 1f).setDuration(300).start();
        customDialog.show();

        return customDialog;

    }

    static void initAddBtnText(TextView textView, String userId) {
        final SocialModel socialModel = mFireBaseHelper.getSocialModel();
        if (socialModel == null) return;
        if (socialModel.friends != null) {
            final Set<String> friendIds = socialModel.friends.keySet();
            if (friendIds.contains(userId)) {
                textView.setVisibility(View.GONE);
                return;
            }
        }
        if (socialModel.requestSent != null) {
            if (socialModel.requestSent.keySet().contains(userId)) {
                textView.setVisibility(View.VISIBLE);
                textView.setText("FRIEND REQUEST SENT");
                return;
            }
        }
        textView.setText("ADD FRIEND");

    }

    protected void launchMediaShareIntent(String handle, @Nullable String name, String packageName, int mediaType, String localPath) {
        InternalFileProvider.getInstance().shareMedia((Activity) context, this, handle, name, packageName, mediaType, localPath);

        if (baseFragment instanceof ViewPublicMomentFragment)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SHARE_PUBLIC_STREAM, AnalyticsEvents.STREAM_HANDLE, handle);
        else if (baseFragment instanceof ViewMyMediaFragment)
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SHARE_MY_STREAM);

        //Whatsapp Share is handled in video editor fragment directly
    }

    protected void launchShareIntent(String handle, boolean isAUserProfile, @Nullable String name, String packageName) {
        if (!isAUserProfile) {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SHARE_PUBLIC_STREAM, AnalyticsEvents.STREAM_HANDLE, handle);

            String shareBody = "Check out '" + name + "' on Pulse -\nhttps://mypulse.tv/stream/" + handle;
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            if (packageName != null)
                sharingIntent.setPackage(packageName);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out '" + name + "' on Pulse");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
        } else {
            AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SHARE_PROFILE, AnalyticsEvents.USER_HANDLE, handle);

            String shareBody = "Add " + name + " on Pulse -\nhttps://mypulse.tv/add/" + handle;
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            if (packageName != null)
                sharingIntent.setPackage(packageName);
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Add " + name + " on Pulse");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
        }
    }

    public Toast showCustomToast(Context context, int layoutId, int gravity, int xOffset, int yOffset, int duration) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final Toast customToast = new Toast(context);
        View layout = layoutInflater.inflate(layoutId, null);
        customToast.setGravity(gravity, Utils.dp2Px(xOffset), Utils.dp2Px(yOffset));
        customToast.setDuration(Toast.LENGTH_SHORT);
        customToast.setView(layout);

        toastCountDown = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                customToast.show();
            }

            public void onFinish() {
                customToast.cancel();
            }
        };

        customToast.show();
        toastCountDown.start();

        return customToast;
    }

    public Toast showCustomToast(Context context, int layoutId, int gravity, int xOffset, int yOffset, int duration, String text) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final Toast customToast = new Toast(context);
        View layout = layoutInflater.inflate(layoutId, null);
        ((TextView) layout.findViewById(R.id.text)).setText(text);
        customToast.setGravity(gravity, Utils.dp2Px(xOffset), Utils.dp2Px(yOffset));
        customToast.setDuration(Toast.LENGTH_SHORT);
        customToast.setView(layout);

        toastCountDown = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                customToast.show();
            }

            public void onFinish() {
                customToast.cancel();
            }
        };

        customToast.show();
        toastCountDown.start();

        return customToast;
    }

    public void cancelCustomToast(Toast toast) {
        if (toastCountDown != null) {
            toastCountDown.cancel();
        }
        if (toast != null) {
            toast.cancel();
        }
    }

    /**
     * @return the latest base Fragment instance having non null context
     * Use this only when quering from non subclasses ,  otherwise call super method on the instance itself
     */
    public static BaseFragment getBaseFragmentInstance() {
        return baseFragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the method name for a depth in call stack. <br />
     * Utility function
     *
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    public static String getMethodName(final int depth) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        //System. out.println(ste[ste.length-depth].getClassName()+"#"+ste[ste.length-depth].getMethodName());
        // return ste[ste.length - depth].getMethodName();  //Wrong, fails for depth = 0
        return ste[ste.length - 1 - depth].getMethodName(); //Thank you Tom Tresansky
    }

    /**
     * @return the method name (String)
     * Utility function for logging
     */
    protected String getCurrentMethodName() {
        final int depth = 0;
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[ste.length - 1 - depth].getMethodName();
    }

    protected MomentModel generateMomentModelFromHomeMomentModelView(HomeMomentViewModel homeMomentViewModel) {
        MomentModel momentModel = new MomentModel();
        momentModel.momentId = homeMomentViewModel.momentId;
        momentModel.name = homeMomentViewModel.name;
        momentModel.handle = homeMomentViewModel.handle;
        momentModel.fixedTimer = homeMomentViewModel.fixedtimer;

        if (homeMomentViewModel.imageUrl != null)
            momentModel.imageUrl = homeMomentViewModel.imageUrl;

        momentModel.contributableNoLocation = homeMomentViewModel.contributableNoLocation;
        return momentModel;
    }

    protected int convertDpToPixels(int dp) {
        return AppLibrary.convertDpToPixels(context, dp);
    }


    protected interface OnPublicContributionListener {
        void onContributeToMoreStreams();

        void onContributeNow();
    }

    protected void initPublicContributionView(final View rootView, CustomMomentModel publicMomentModel, final OnPublicContributionListener onPublicContributionListener) {

//        rootView.findViewById(R.id.send).setVisibility(View.GONE);
//        rootView.findViewById(R.id.addToMyStreamIV).setVisibility(View.GONE);

        rootView.findViewById(R.id.send).getLayoutParams().width = 0;
        rootView.findViewById(R.id.addToMyStreamIV).getLayoutParams().width = 0;


        rootView.findViewById(R.id.contributionIncludedView).setVisibility(View.VISIBLE);

        //contributionIncludedView
        ((FrameLayout.LayoutParams) rootView.findViewById(R.id.save_soundLL).getLayoutParams()).bottomMargin = AppLibrary.convertDpToPixels(context, 48);
        ((TextView) rootView.findViewById(R.id.streamNameTv)).setText(publicMomentModel.name);


        rootView.findViewById(R.id.contributionIncludedView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onPublicContributionListener != null)
                    onPublicContributionListener.onContributeToMoreStreams();

            }
        });


        rootView.findViewById(R.id.publicStreamContributeIV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onPublicContributionListener != null)
                    onPublicContributionListener.onContributeNow();
            }
        });

    }

    public static boolean isThisMyInstitutionId(String momentId) {
        String myInstitutionId = null;

        if (mFireBaseHelper != null && mFireBaseHelper.getMyUserModel() != null)
            myInstitutionId = mFireBaseHelper.getMyUserModel().getMyInstitutionId();

        return !(momentId == null || myInstitutionId == null) && momentId.equals(myInstitutionId);
    }
}
