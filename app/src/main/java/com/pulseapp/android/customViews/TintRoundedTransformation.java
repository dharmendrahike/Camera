package com.pulseapp.android.customViews;

/**
 * Created by Morph on 7/9/2015.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;
import com.squareup.picasso.Transformation;

public class TintRoundedTransformation implements Transformation {
    final String TAG = this.getClass().getSimpleName();
    private static final int NUM_OF_TILE_COLORS = 8;
    private Context context;
    private String name;
    private boolean defaultColor;
    private String key;
    private boolean hasTint = true;

    public TintRoundedTransformation(Context context, String name, boolean defaultColor, String momentId) {
        Log.d(TAG, " constructor      " + System.currentTimeMillis());
        this.context = context;
        this.name = name;
        this.key = momentId;
        this.defaultColor = defaultColor;
        final Resources res = context.getResources();
        mColors = res.obtainTypedArray(R.array.letter_tile_colors);
    }

    @SuppressWarnings("ResourceAsColor")
    @Override
    public Bitmap transform(Bitmap source) {
        Log.d(TAG, " transform        " + System.currentTimeMillis());
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
        BitmapShader shader = new BitmapShader(squaredBitmap,
                BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        Paint tintPaint = new Paint();
        tintPaint.setColor(defaultColor?Color.parseColor("#ffffff"):pickColor(name));
        tintPaint.setAlpha(184);
        tintPaint.setAntiAlias(true);

        canvas.drawCircle(r, r, r - 0, tintPaint);

        if (squaredBitmap!=null)
            squaredBitmap.recycle();

        Log.d(TAG, " transform done " + System.currentTimeMillis());
        return bitmap;
    }

    @Override
    public String key() {
        return key + defaultColor;
    }

    private int pickColor(String key) {
        // String.hashCode() is not supposed to change across java versions, so
        // this should guarantee the same key always maps to the same color
        final int color = Math.abs(key.hashCode()) % NUM_OF_TILE_COLORS;
        try {
            return mColors.getColor(color, Color.BLACK);
        } catch (Exception e) {
            AppLibrary.log_d("Letter Tile", "Exception in mColours");
            return 0;
        }
//        } finally {
//            mColors.recycle();
//        }
    }

    private final TypedArray mColors;

}