package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

/**
 * Created by Karthik on 10/17/15.
 */
public class BlurBuilder {
    private static final float BITMAP_SCALE = 0.5f;
    private static final float BLUR_RADIUS = 16.0f;

    static RenderScript sRS;
    static ScriptIntrinsicBlur sIntrinsic;

    public static Bitmap blur(Context context, Bitmap image) {
        return blur(context, image, BITMAP_SCALE, BLUR_RADIUS);
    }

    public static Bitmap blur(Context context, Bitmap image, float scale, float blurRadius) {

        if(sRS == null) {
            sRS = RenderScript.create(context.getApplicationContext());
            sIntrinsic = ScriptIntrinsicBlur.create(sRS, Element.U8_4(sRS));
        }
        int width = Math.round(image.getWidth() * scale);
        int height = Math.round(image.getHeight() * scale);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        Allocation tmpIn = Allocation.createFromBitmap(sRS, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(sRS, outputBitmap);
        sIntrinsic.setRadius(blurRadius);
        sIntrinsic.setInput(tmpIn);
        sIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        inputBitmap.recycle();
        return outputBitmap;
    }
}
