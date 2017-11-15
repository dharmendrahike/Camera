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

public class RoundedCornerTransformation implements Transformation {

    String key;
    int radius;
    int margin = 0;


    public RoundedCornerTransformation(String momentId,int radius){
        this.key = momentId;
        this.radius = radius;
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
        canvas.drawRoundRect(new RectF(margin, margin, source.getWidth() - margin, source.getHeight() - margin),
                radius, radius, paint);

        if (squaredBitmap != null)
            squaredBitmap.recycle();

        return bitmap;
    }

    @Override
    public String key() {
        return key;
    }
}