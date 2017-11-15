package com.pulseapp.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.squareup.picasso.Transformation;

/**
 * Created by abc on 10/12/2015.
 */
public class ColoredRoundedTransformation implements Transformation {

    private String color;
    private int BORDER_RADIUS = 2;
    private boolean hasTint = false;
    // radius is corner radii in dpf
    // margin is the board in dp
    public ColoredRoundedTransformation(String color) {
        this.color = color;
    }

    public ColoredRoundedTransformation(String color,int border,boolean hasTint) {
        this.color = color;
        this.BORDER_RADIUS = border;
        this.hasTint = hasTint;
    }

    public ColoredRoundedTransformation(String color,int border) {
        this.color = color;
        this.BORDER_RADIUS = border;
    }


    @Override
    public Bitmap transform(final Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;

        // Prepare the background
        Paint paintBg = new Paint();
        if (color.equals("red")){
            paintBg.setColor(Color.parseColor("#d42e32"));
        }else if (color.equals("green")){
            paintBg.setColor(Color.parseColor("#3a8f3e"));
        }else if (color.equals("blue")){
            paintBg.setColor(Color.parseColor("#0387d1"));
        } else if (color.equals("teal")) {
            paintBg.setColor(Color.parseColor("#00897b"));
        } else {
            paintBg.setColor(Color.parseColor("#ffffff"));
        }

        paintBg.setAntiAlias(true);
        // Draw the background circle
        canvas.drawCircle(r, r, r, paintBg);

        // Draw the image smaller than the background so a little border will be checkedOut
        canvas.drawCircle(r, r, r - BORDER_RADIUS, paint);

        if (hasTint){
            Paint tintPaint = new Paint();
            tintPaint.setColor(Color.parseColor("#a22d2d2d"));
            tintPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
            canvas.drawCircle(r, r, r - BORDER_RADIUS, tintPaint);
        }

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return color;
    }

}
