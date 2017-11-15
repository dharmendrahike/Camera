/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pulseapp.android.gles;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import com.pulseapp.android.broadcast.FilterManager;
import com.pulseapp.android.gles.OpenGLRenderer.SBitmap;
import com.pulseapp.android.gles.ProgramCache.ProgramDescription;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d {
//    private static final String TAG = "Drawable2d";

    private ProgramDescription mDescription;
    // The information the renderer needs to know to choose the program to use to render this drawable.

    public static final int CONVOLUTION_TYPE_NONE = 0;
    public static final int CONVOLUTION_TYPE_BLUR = 1;
    public static final int CONVOLUTION_TYPE_SHARPEN= 2;
    public static final int CONVOLUTION_TYPE_BLUR_HORIZONTAL= 3;
    public static final int CONVOLUTION_TYPE_BLUR_VERTICAL= 4;
    public static final int CONVOLUTION_TYPE_FILTER_TEXTURE=5;

    public static final int BLEND_MODE_NONE = 0;
    public static final int BLEND_MODE_ADD = 1;
    public static final int BLEND_MODE_DISSOLVE = 2;

    public static final int SCALE_TYPE_INSIDE = 1;
    public static final int SCALE_TYPE_CROP = 2;
    public static final int SCALE_TYPE_FIT = 3;
    public static final int SCALE_TYPE_SQUARE = 4;

    public static final int SCALE_TYPE_FIT_WIDTH = 5;
    public static final int SCALE_TYPE_FIT_HEIGHT = 6;

    private boolean mIsMultiPass;
    private boolean mIsFBOBased;

    public boolean mUsesExternalTexture = false;

    // To be used only when drawable is using external texture
    private int mIncomingWidth;
    private int mIncomingHeight;

    private Bitmap mBitmap;
    private SBitmap mSBitmap;

    private Bitmap mNextBitmap;

    private boolean mIsColorBased;
    private boolean mIsBitmapBased;
    private boolean mIsMultiBitmapBased;
    private boolean mIsFilterTextureBased;
    //If it is bitmap based, then texture coordinates would be different from when it is texture/Surface Texture based.
    private boolean mIsSBitmapBased;

    private float[] mTranslateM = new float[]{0.0f, 0.0f, 0.0f};
    private float[] mScaleM = new float[]{1.0f, 1.0f, 1.0f};
    private float[] mZoomM = new float[]{1.0f, 1.0f, 1.0f};

    private int mBlendMode = 0;
    private float mAlpha = 0.0f;
    private int mColorIndex = 0;

    private float[] mRotateM = new float[]{0.0f, 0.0f, 0.0f};

    private float[] mFinalScaleM = new float[]{1.0f, 1.0f, 1.0f};

    private float[] mDefaultTranslateM = new float[]{0.0f, 0.0f, 0.0f};

    private boolean mIsFlipped = false;

    private int mColorFilterMode = FilterManager.FILTER_NATURAL;
    private int mConvolutionType = 0;
    private RectF mRectF;
    private Paint mPaint;
    private int textureIndex;

    private int mScaleType;

    public void setIsFlipped(boolean isFlipped) {
        mIsFlipped = isFlipped;
    }

    public boolean getIsFlipped() {
        return mIsFlipped;
    }

    public float[] getTranslate() {
        return mTranslateM;
    }

    public float[] getRawScale() {
        return mScaleM;
    }

    public float[] getScale() {
        float[] finalScale = new float[3];
        finalScale[0] = mFinalScaleM[0] * mZoomM[0];
        finalScale[1] = mFinalScaleM[1] * mZoomM[1];
        finalScale[2] = mFinalScaleM[2] * mZoomM[2];
        return finalScale;
    }

    public int getScaleType() {
        return mScaleType;
    }

    public void setScaleType(int scaleType) {
        mScaleType = scaleType;
    }

    public float[] getRotate() {
        return mRotateM;
    }

    public float[] getZoom() {
        return mZoomM;
    }

    public RectF getBounds() { return mRectF; }

    public float getAlpha() {
        return mAlpha;
    }

    public void setDescription(ProgramDescription description) {
        mDescription = description;
    }

    public ProgramDescription getDescription() {
        return mDescription;
    }

    public void setTranslate(float[] translate) {
        mTranslateM = translate.clone();
        mDefaultTranslateM = translate.clone();
        computeBounds();
    }

    public void setTranslate(float translateX, float translateY, float translateZ) {
        mTranslateM[0] = translateX;
        mTranslateM[1] = translateY;
        mTranslateM[2] = translateZ;
        computeBounds();
    }

    public void translate(float translateX, float translateY) {
        Log.d("Drawable2d", "Translating by " + translateX + " " + translateY);
        mTranslateM[0] += translateX;
        mTranslateM[1] += translateY;
        computeBounds();
    }

    public void translate(float translateX, float translateY, float translateZ) {
        mTranslateM[0] += translateX;
        mTranslateM[1] += translateY;
        mTranslateM[2] += translateZ;
        computeBounds();
    }

    public void multiplyTranslate(float translateX, float translateY, float translateZ) {
        mTranslateM[0] = mDefaultTranslateM[0] * translateX;
        mTranslateM[1] = mDefaultTranslateM[1] * translateY;
        mTranslateM[2] = mDefaultTranslateM[2] * translateZ;
        computeBounds();
    }

    public void setScale(float[] scale) {
        mScaleM = scale.clone();

        mFinalScaleM = mScaleM.clone();
        computeBounds();
    }

    public void setScale(float scaleX, float scaleY) {
        setScale(scaleX, scaleY, 1.0f);
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        mScaleM[0] = scaleX;
        mScaleM[1] = scaleY;
        mScaleM[2] = scaleZ;

        mFinalScaleM = mScaleM.clone();
        computeBounds();
    }

    public void resetScale() {
        mFinalScaleM = mScaleM.clone();
        computeBounds();
    }

    public void setRotate(float rotateX, float rotateY, float rotateZ) {
        mRotateM[0] = rotateX;
        mRotateM[1] = rotateY;
        mRotateM[2] = rotateZ;
    }

    public void rotate(float rotateX, float rotateY, float rotateZ) {
        mRotateM[0] += rotateX;
        mRotateM[1] += rotateY;
        mRotateM[2] += rotateZ;
    }

    public void setZoom(float scaleX, float scaleY, float scaleZ) {
        mZoomM[0] = scaleX;
        mZoomM[1] = scaleY;
        mZoomM[2] = scaleZ;
        computeBounds();
    }

    // When scalex, scaley and scalez are passed separately, then they simply are multiplied with final scale's current values
    // This behavior is different from when they are passed in a single array (Check below)
    // sahil.bajaj@instalively.com
    public void multiplyScale(float scaleX, float scaleY, float scaleZ) {
        mFinalScaleM[0] *= scaleX;
        mFinalScaleM[1] *= scaleY;
        mFinalScaleM[2] *= scaleZ;
        computeBounds();
    }

    // When scalex, scaley and scalez are passed, packed as single array, then they are multiplied with scale values to obtain finalScale values
    // This behavior is different from when they are passed as separate elements (Check above)
    // sahil.bajaj@instalively.com
    public void multiplyScale(float[] scale) {
        mFinalScaleM[0] = mScaleM[0] * scale[0];
        mFinalScaleM[1] = mScaleM[1] * scale[1];
        mFinalScaleM[2] = mScaleM[2] * scale[2];
        computeBounds();
    }

    public int getConvolutionType() {
        return mConvolutionType;
    }

    void setConvolutionType(int convolutionType) {
        mConvolutionType = convolutionType;
    }

    int getColorFilterMode() {
        return mColorFilterMode;
    }

    void setFilterMode(int filterMode) {
//      mColorFilterMode |= filterMode;
//Right now, the filters are exclusive. No or/and masking right now.
        mColorFilterMode = filterMode;
        setupDrawWithFilter(mColorFilterMode);
        setupDrawWithHSV(mColorFilterMode);
        setupDrawWithFilterHeader(mColorFilterMode);
    }

    int getBlendMode() {
        return mBlendMode;
    }

    void setBlendMode(int blendMode) {
        mBlendMode = blendMode;
    }

    void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    void setColorIndex(int index) {
        mColorIndex = index;
    }

    int getColorIndex() {
        return mColorIndex;
    }

    void computeBounds() {
        float x1 = mTranslateM[0] - mFinalScaleM[0]/2;
        float y1 = mTranslateM[1] - mFinalScaleM[1]/2;
        float x2 = x1 + mFinalScaleM[0];
        float y2 = y1 + mFinalScaleM[1];
        mRectF.set(x1, y1, x2, y2);
//        Log.d("Drawable2d", "drawable bounds : " + x1 + " " + x2 + " " + y1 + " " + y2);
    }

    void printBounds() {
        Log.d("Drawable2d", "drawable bounds : " + mRectF.toString());
    }

    @Override
    public String toString() {
        String result = "Drawable props";
        result += mIsBitmapBased ? "Bitmap based" : "";
        result += mIsColorBased ? "Color based" : "";
        result += mIsFBOBased ? "FBO based" : "";
        result += mUsesExternalTexture ? "External texture based" : "";
        result += " ScaleType: " + mScaleType;
        result += "scale: " + mScaleM[0] + "x" + mScaleM[1] + "x" + mScaleM[2];
        result += "final scale: " + mFinalScaleM[0] + "x" + mFinalScaleM[1] + "x" + mFinalScaleM[2];
        return result;
    }

    void clearFilterMode(int filterMode) {
//        mColorFilterMode &= ~filterMode;
//Right now, the filters are exclusive. No or/and masking right now.
//So no use of this function at all right now.
    }

/*    public void setBWMode(boolean bwMode) {
        mIsGrayMode = bwMode;
    }

    public void setLivelyMode(boolean isLivelyMode) {
        mIsLivelyMode = isLivelyMode;
    }

    public void setRichMode(boolean isRichMode) {
        mIsRichMode = isRichMode;
    }*/

    public int getIncomingWidth() {
        return mIncomingWidth;
    }

    public void setIncomingWidth(int incomingWidth) {
        mIncomingWidth = incomingWidth;
    }

    public void setIncomingHeight(int incomingHeight) {
        mIncomingHeight = incomingHeight;
    }

    public void setIncomingSize(int incomingWidth, int incomingHeight) {
        mIncomingWidth = incomingWidth;
        mIncomingHeight = incomingHeight;
    }

    public int getIncomingHeight() {
        return mIncomingHeight;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Bitmap getBitmap2() {
        return mNextBitmap;
    }

    public SBitmap getSBitmap() {
        return mSBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
        mNextBitmap = null;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setPaint(float [] colors) {
        Paint paint = new Paint();
        if(colors.length == 1 || colors.length == 4) {
            paint.mColor = colors;
        } else {
            Log.d("Drawable", "Paint colors for drawable not set properly");
        }
        mPaint = paint;
    }

    /**
     * Returns the evicted bitmap (if any), else returns null.
     * @param bitmap
     * @return
     */
    public Bitmap addBitmap(Bitmap bitmap) {
        Bitmap evicted = null;
        if(mBitmap == null) {
            mBitmap = bitmap;
            setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
        } else if(mNextBitmap == null) {
            mNextBitmap = bitmap;
            setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
        } else {
            evicted = mBitmap;
            mBitmap = mNextBitmap;
            mNextBitmap = bitmap;
        }
        return evicted;
    }

    public void setBitmaps(Bitmap prevBitmap, Bitmap currBitmap) {
        mBitmap = prevBitmap;
        mNextBitmap = currBitmap;
    }

    public void setSBitmap(SBitmap sBitmap) {
        mSBitmap = sBitmap;
    }


    public boolean getIsColorBased() {
        return mIsColorBased;
    }

    public boolean getIsBitmapBased() {
        return mIsBitmapBased;
    }

    public void setIsFilterTextureBased(boolean isFilterTextureBased){
        this.mIsFilterTextureBased=isFilterTextureBased;
    }

    public boolean getIsFilterTextureBased(){
        return mIsFilterTextureBased;
    }

    public boolean getIsMultiBitmapBased() {
        return mIsMultiBitmapBased;
    }

    public boolean getIsSBitmapBased() {
        return mIsSBitmapBased;
    }

    public void setIsColorBased(boolean isColorBased) {
        mIsColorBased = isColorBased;
    }

    public void setIsBitmapBased(boolean isBitmapBased) {
        mIsBitmapBased = isBitmapBased;
        if(isBitmapBased)
            mIsMultiBitmapBased = false;
    }

    public void setIsMultiBitmapBased(boolean isMultiBitmapBased) {
        mIsMultiBitmapBased = isMultiBitmapBased;
        if(isMultiBitmapBased)
            mIsBitmapBased = false;
    }

    public void setIsSBitmapBased(boolean isSBitmapBased) {
        mIsSBitmapBased = isSBitmapBased;
    }

    public boolean getIsFBOBased() {
        return mIsFBOBased;
    }

    public void setIsFBOBased(boolean isFBOBased) {
        mIsFBOBased = isFBOBased;
        if(mIsFBOBased) {
            mIsBitmapBased = false;
            mIsMultiBitmapBased = false;
            mUsesExternalTexture = false;
        }
    }

/*    public boolean isMultiPass() {
        return mIsMultiPass;
    }

    public void setIsMultiPass(boolean isMultiPass) {
        mIsMultiPass = isMultiPass;
    }*/

    public Drawable2d() {
        mDescription = new ProgramDescription();
        mIsColorBased = false;
        mIsBitmapBased = false;
        mIsSBitmapBased = false;
        mIsMultiBitmapBased = false;
        mUsesExternalTexture = false;
        mIsFilterTextureBased=false;
        mIsFBOBased = false;
        mRectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);

        mIncomingWidth = 0;
        mIncomingHeight = 0;

        mColorFilterMode = FilterManager.FILTER_NATURAL;
        mConvolutionType = CONVOLUTION_TYPE_NONE;
        mAlpha = 0.0f;
        mColorIndex = 0;
        mBlendMode = BLEND_MODE_NONE;
    }

    void setupDrawWithColor() {
        mDescription.hasColor = true;
    }

    void setupDrawWithSTexture() {
        mDescription.hasTexture = true;
        mDescription.hasSBitmapTexture = true;

        mDescription.hasExternalTexture = false;
    }

    void setupDrawWithTexture() {
        mDescription.hasTexture = true;
        mDescription.isFBOBased = false;
        mDescription.hasExternalTexture = false;
        mDescription.hasAlpha8Texture = false;
        mDescription.hasSBitmapTexture = false;
    }

    void setupDrawWithMultiTexture() {
//        Log.d(TAG, "Drawable setup to use multi textures");
        mDescription.hasTexture2 = true;
    }

    void setupDrawWithTexture(boolean isAlpha8) {
        mDescription.hasTexture = true;
        mDescription.hasAlpha8Texture = isAlpha8;
    }

    void setupDrawWithFilter(int filterMode) {
        mDescription.hasColorFilter = (filterMode > 0);
        mDescription.hasFilterTexture=(((filterMode > 6) &&(filterMode < 12))||(filterMode==14)||(filterMode==15)||(filterMode==16) || (filterMode==17));
        mDescription.colorFilterMode = (filterMode << 12) & 0x000FF000;
    }

    void setupDrawWithHSV(int filterMode) {
        mDescription.needsHSV = ( (filterMode == FilterManager.FILTER_NIGHT) || (filterMode == FilterManager.FILTER_TOON));
    }

    void setupDrawWithFilterHeader(int filterMode){
        mDescription.needsFilterHeader = ((filterMode == FilterManager.FILTER_AMARO)||(filterMode == FilterManager.FILTER_XPRO)||(filterMode == FilterManager.FILTER_INKWELL)||(filterMode == FilterManager.FILTER_Moon));
    }

    void setupDrawWithKernel() {
        if(mConvolutionType > 0) {
            mDescription.isKernelBased = true;
        } else {
            mDescription.isKernelBased = false;
        }
        mDescription.blurMode = (mConvolutionType << 20) & 0x00F00000;
    }

    void setupDrawWithFBOTexture() {
        mDescription.hasTexture = true;
        mDescription.isFBOBased = true;
        mDescription.hasExternalTexture = false;
        mDescription.hasTexture2 = false;
        mDescription.hasAlpha8Texture = false;
        mDescription.hasSBitmapTexture = false;
    }

    void setupDrawWithExternalTexture() {
        mDescription.hasExternalTexture = true;
        mDescription.hasTexture2 = false;
        mDescription.isFBOBased = false;
    }
    void setupDrawWithFilterTexture(){
        mDescription.hasFilterTexture=true;
    }

    void setupDrawTextureTransform() {
        mDescription.hasTextureTransform = true;
    }

    public void setupDraw() {
        mDescription.reset();
       // if(mIsFilterTextureBased){
            //setupDrawWithFilterTexture();
       // }
        if(mIsColorBased) {
            setupDrawWithColor();
        }
        if(mIsFBOBased) {
            setupDrawWithFBOTexture();
        } else if (mUsesExternalTexture) {
            setupDrawWithExternalTexture();
            // Can't understand this.
            // Somehow preview frame has rgba format but encoder frame has bgra format :o
        } else if (mIsSBitmapBased) {
            setupDrawWithSTexture();
        } else if (mIsBitmapBased || mIsMultiBitmapBased) {
            setupDrawWithTexture();
            if(mNextBitmap != null) {
                setupDrawWithMultiTexture();
            }
        }
//        setupDrawColor(alpha, alpha, alpha, alpha);

        setupDrawWithFilter(getColorFilterMode());
        setupDrawWithKernel();
        if (mUsesExternalTexture) {
            setupDrawTextureTransform();
        }
    }

    public Drawable2d(Drawable2d drawable) {
        cloneProperties(drawable);
    }

    public void cloneProperties(Drawable2d src) {
        mScaleM = src.mScaleM.clone();
        mTranslateM = src.mTranslateM.clone();
		mRotateM = src.mRotateM.clone();
        mFinalScaleM = src.mFinalScaleM.clone();

        mZoomM = src.mZoomM.clone();

        mDefaultTranslateM = src.mDefaultTranslateM.clone();

        mColorFilterMode = src.mColorFilterMode;

        mConvolutionType = src.mConvolutionType;

        mScaleType = src.mScaleType;
        mAlpha = src.getAlpha();
        mColorIndex = src.getColorIndex();
        mBlendMode = src.getBlendMode();

        mIsColorBased = src.getIsColorBased();
        mIsBitmapBased = src.getIsBitmapBased();
        mIsMultiBitmapBased = src.getIsMultiBitmapBased();
        mIsFlipped = src.getIsFlipped();
        mUsesExternalTexture = src.mUsesExternalTexture;
        mIsFBOBased = src.mIsFBOBased;

        mIsSBitmapBased = src.getIsSBitmapBased();
        mSBitmap = src.getSBitmap();

        mBitmap = src.mBitmap;
        mNextBitmap = src.mNextBitmap;

        mDescription = src.getDescription();
        mRectF = new RectF(src.getBounds());

        mIncomingWidth = src.getIncomingWidth();
        mIncomingHeight = src.getIncomingHeight();
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    public void setTextureIndex(int textureIndex) {
        this.textureIndex = textureIndex;
    }
}
