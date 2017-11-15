package com.pulseapp.android.util;

/**
 * Created by Morph on 7/9/2015.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.squareup.picasso.Transformation;

public class RoundedTransformation implements Transformation {

    boolean hasTint = false;
    boolean needBorder = false;
    int borderRadius = 30;
    int timerPadding = 0;
    String key;
    float angle = 0;
    private final static float STROKE_WIDTH = 5;

    public RoundedTransformation() {
        key = "circle" + hasTint;
    }

    public RoundedTransformation(boolean hasTint) {
        this.hasTint = hasTint;
        key = "circle" + hasTint;
    }

    public RoundedTransformation(int borderRadius) {
        this.needBorder = true;
        this.borderRadius = borderRadius;
        key = "circle" + hasTint + borderRadius;
    }

    public RoundedTransformation(String momentId){
        key = momentId;
    }

    public RoundedTransformation(String momentId,float angle,int timerPadding){
        key = momentId + angle + timerPadding;
        this.angle = angle;
        this.timerPadding = timerPadding;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap.Config config = Bitmap.Config.ARGB_4444;
        Bitmap bitmap = Bitmap.createBitmap(size, size, config);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r - timerPadding, paint);

        if (angle != 0){
            RectF rectF = new RectF(STROKE_WIDTH, STROKE_WIDTH, size - STROKE_WIDTH, size - STROKE_WIDTH);
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setAntiAlias(true);
            p.setStrokeWidth(STROKE_WIDTH);
            p.setColor(Color.argb(25,0,0,0));
            canvas.drawArc(rectF, -90, angle, false, p);
        }

        if (hasTint) {
            Paint tintPaint = new Paint();
            tintPaint.setColor(Color.parseColor("#ffffff"));
            tintPaint.setAntiAlias(true);
            tintPaint.setAlpha(120);
            canvas.drawCircle(r, r, r - 0, tintPaint);
        }

        if (needBorder) {
            RectF rectF = new RectF(size - borderRadius, size - borderRadius, size - borderRadius, size - borderRadius);
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setAntiAlias(true);
            p.setStrokeWidth(borderRadius);
            p.setColor(Color.WHITE);
            canvas.drawArc(rectF, 0, 360, false, p);
        }

        if (squaredBitmap != null)
            squaredBitmap.recycle();

        return bitmap;
    }

    @Override
    public String key() {
        return key;
    }
}