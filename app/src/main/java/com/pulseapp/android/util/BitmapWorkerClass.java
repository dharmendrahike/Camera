package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.os.AsyncTask;

import com.pulseapp.android.activities.CameraActivity;

/**
 * Created by Karthik on 12/24/15.
 */
public class BitmapWorkerClass extends AsyncTask<Integer, Void, Bitmap> {
    private Bitmap bitmap;
    private Bitmap returnBitmap;
    private int data = 0;
    private Context context;
    private String userid;
    private boolean Triggerlike;

    public BitmapWorkerClass(Bitmap bitmap, Context context, String userid, boolean Triggerlike) {
        this.bitmap = bitmap;
        this.context = context;
        this.userid = userid;
        this.Triggerlike = Triggerlike;
    }

    @Override
    protected Bitmap doInBackground(Integer... params) {
//        data = params[0];
        int wh = 108;

        returnBitmap = Bitmap.createBitmap(this.context.getResources().getDisplayMetrics(), wh, wh, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(returnBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

//        Bitmap bitmap2 = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.ic_heart_white_18dp);
        canvas.drawARGB(0, 0, 0, 0);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(wh / 2, wh / 2, (wh / 2 - 4), paint);
        Xfermode mode = paint.getXfermode();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, wh, wh), paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setARGB(255, 255, 0, 0);
        canvas.drawCircle(wh / 2, wh / 2, (wh / 2 - 3), paint);

        paint.setXfermode(mode);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle((int) (0.85 * wh), (int) (0.85 * wh), (int) (0.15 * wh), paint);

        paint.setColor(Color.WHITE);

//        canvas.drawBitmap(bitmap2, null, new Rect((int) (0.6 * wh), (int) (0.6 * wh), (int) (1.1 * wh), (int) (1.1 * wh)), paint);

        return returnBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if(this.context!=null)
            ((CameraActivity)context).putNewProfileImage(bitmap,userid, Triggerlike);
    }
}