package com.pulseapp.android.stickers;

import android.graphics.Bitmap;

public class ThemeAsset {
    public String mStickerId;
    public float mWidth;
    public float mHeight;
    public float mMarginLeft;
    public float mMarginTop;
    public Bitmap mBitmap;
    public String mUrl;

    public enum StickerPosition {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    StickerPosition mPosition;

    public ThemeAsset() {

    }
//    public ThemeAsset() {
//        mHeight = -1.0f; // Necessary to set. So that height gets calculated using bitmap's aspect ratio.
//        // Setting both width and height to non-negative values will force the parameters for the view.
//    } float width, float height, float marginLeft, float marginTop


    /**
     * @param stickerId uid of sticker maintained with the server
     * @param url       url of the image
     * @param bitmap    bitmap of the image to be drawn
     * @param dimens    0 width , 1 height,2 marginLeft , 3 marginTop
     */
    public ThemeAsset(String stickerId, String url, StickerPosition position, Bitmap bitmap, float[] dimens) {


        mStickerId = stickerId;
        mUrl = url;
        mBitmap = bitmap;
        mPosition = position;
        mWidth = dimens[0];
        mHeight = dimens[1];
        mMarginLeft = dimens[2];
        mMarginTop = dimens[3];
    }
}
