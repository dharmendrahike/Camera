package com.pulseapp.android.customViews;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.pulseapp.android.adapters.LetterTileProvider;
import com.squareup.picasso.Transformation;

public class LetterTileRoundedTransformation implements Transformation {

    private boolean letterTileNeeded = true;
    private String name;
    private String momentId;
    private Context context;
    private boolean hasTint;
    private String key;

    public LetterTileRoundedTransformation(Context context, String momentId){
        this.momentId = momentId;
        this.context = context;
        this.key = momentId;
    }

    public LetterTileRoundedTransformation(Context context, String name, String momentId) {
        this.name = name;
        this.context = context;
        this.key = name + hasTint + momentId;
    }

    public LetterTileRoundedTransformation(Context context, String name, boolean hasTint,String momentId) {
        this.name = name;
        this.context = context;
        this.hasTint = hasTint;
        this.key = name + hasTint + momentId;
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

        Bitmap.Config config = source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_4444;
        Bitmap bitmap = Bitmap.createBitmap(size, size, config);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap,
                BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        if (letterTileNeeded) {
            if (name == null) throw new NullPointerException("name must not be null");

            LetterTileProvider tileProvider = new LetterTileProvider(context);
            Bitmap tileBitmap = tileProvider.getLetterTile(name, name, size, size);
            Rect dstRectForRender = new Rect(0, 0, size, size);
            canvas.drawBitmap(tileBitmap, new Rect(0, 0, size, size), dstRectForRender, null);
        }

        if (hasTint) {
            Paint tintPaint = new Paint();
            tintPaint.setColor(Color.parseColor("#ffffff"));
            tintPaint.setAlpha(120);
            canvas.drawCircle(r, r, r - 0, tintPaint);
        }

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return key;
    }
}