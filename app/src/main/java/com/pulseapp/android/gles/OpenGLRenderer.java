package com.pulseapp.android.gles;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

import com.pulseapp.android.broadcast.FilterManager;
import com.pulseapp.android.broadcast.FiltersFragment;
import com.pulseapp.android.gles.ProgramCache.ProgramDescription;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;

/**
 * OpenGL Renderer implementation.
 * Should be accessed from GLRender thread
 */
public class OpenGLRenderer {

    private static final boolean VERBOSE = false;

    private static int mRendererCount;

    //temp
    public WeakReference<Activity> activity;
    private String TAG = "GLRenderer";
    private static final String LOG_TAG = "GLRenderer";

    //    private static float[] mSTMatrix = new float[16];
    private int mTextureId;

    private Lock mLock;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;

    private int mIncomingWidth;
    private int mIncomingHeight;

    private int incomingWidth;
    private int incomingHeight;

    private int mOutgoingWidth;
    private int mOutgoingHeight;

    // In order for encoder to be able to access and use Preview's width and height
    // (As of now) Going to be used to determine whether to choose Vertical Video mode
    private int mPreviewWidth;
    private int mPreviewHeight;

    int bitmapBuffer[], bitmapSource[];
    Bitmap mCaptureBitmap;

    private int mCurrentFilter;
    private int mNewFilter;

    private boolean mFirstFrameAfterResize;
    private boolean mFrameStarted;
    private boolean mTrackDirtyRegions;
    private boolean mSkipOutlineClip;
    private boolean mScissorOptimizationDisabled;

    //Used to determine whether rendering to preview or encoder currently
    private boolean mIsPreview;
    private boolean mIsVideo;
    private boolean mIsOffscreen;
    private Fuzzy mRenderTargetType;

    private FBO []fboTarget;

    private int mDefaultFBOWidth = 72;
    private int mDefaultFBOHeight = 128;

    private int mFBOWidth;
    private int mFBOHeight;

    private boolean mIsStagnantFrame = false;
    private boolean mStagnantSettled = false;
    private int mStagnantCount = 0;


    //OpenGL Camera Parameters
    float mFOV = 50.0f; // degrees
    float zNear = 1.0f;
    float zFar = 100.f;
    //    float mRefDistance = 1.0; //(float) (1.0f/mAspectRatio * Math.tan((double)mFOV/2.0));
    float mAspectRatio = 0.5625f;
    float mCameraAspectRatio = 0.5625f;
    float mDistance = 0.0f;
    float[] eye = {0.0f, 0.0f, 0.0f};
    float[] center = {0.0f, 0.0f, 0.0f};
    float[] up = {0.0f, 1.0f, 0.0f};
//OpenGL Camera Parameters end

    //OpenGL Color Filter parameters begin
    private static final int FILTER_LIVELY_MODE = 0x01;
    private static final int FILTER_GRAY_MODE = 0x02;
    private static final int FILTER_RICH_MODE = 0x04;
    private static final int FILTER_NIGHT_MODE = 0x08;
    private static final int FILTER_LAST_MODE = 0x10;
    private static final int FILTER_COUNT = FiltersFragment.filterCount;

//OpenGL Color Filter parameters end

    private static final int DRAWABLES_UNDERLAY = 0;
    private static final int DRAWABLES_DEFAULT = 1;
    private static final int DRAWABLES_RAW = 2;
    public static final int DRAWABLES_ANIMATABLE = 3;
    public static final int DRAWABLES_OVERLAY = 4;
    private static final int DRAWABLES_UPPER_OVERLAY = 5;

    //HashMap containing Drawables for each layer
//For example, camera preview will form the bottom most layer. Then the Preview. Then the WaterMark.
    TreeMap<Integer, ArrayList<Drawable2d>> mDrawables;

    List<Drawable2d> mDefaultDrawables;
    List<Drawable2d> mRawDrawables;

    static List<Drawable2d> mOverlayDrawables = new ArrayList<Drawable2d>();
    static List<Drawable2d> mUpperOverlayDrawables = new ArrayList<>();

    //    Tree<Drawable2d> mAnimatableDrawables;
    ArrayList<Drawable2d> mAnimatableDrawables;
    ArrayList<RectF> mDrawableLocations;
    static HashMap<Integer, Bitmap> mBitmaps = new HashMap<Integer, Bitmap>();
    static HashMap<String,Bitmap> mUrlBitmap= new HashMap<>();

    LinkedBlockingQueue<Bitmap> mCapturedBitmapsProducer;
    LinkedBlockingQueue<Bitmap> mCapturedBitmapsConsumer;

    LinkedBlockingQueue<IntBuffer> mBackingArrays;
    int MAX_BITMAP_QUEUE_SIZE = 3;
    //Current drawable being rendered in order to query the Render parameters
    private Drawable2d mDrawable;

    private static int BYTES_PER_FLOAT = Float.SIZE / 8;
    private Program currentProgram;
    private int mExternalTextureFilter = FilterManager.FILTER_NATURAL;
    private boolean mDrawableConfigured = false;

    // Disable Vertical Mode by default
    // Will see when to enable this
    private static boolean sVerticalModeEnabled = true;

    private boolean mVerticalVideoMode = false;
    private int mBitmapCount;
    private float mBlendFactor;
    private float mAlphaFactor;
    private boolean maxReached = false;
    private boolean mMaxAlphaReached = false;
    private boolean mFlipAnimation;

    private static final int BLUR_PASS_COUNT = 25;
    private static final boolean USE_FBO = false;
    private boolean mInputSourceCamera = false;
    private boolean mUseFBO;
    public float sScale;
    private Context mContext;
    private InputStream inputStream;

    private SurfaceTexture mSurfaceTexture;
    private HashMap<Integer,Bitmap> mFilterBitmapHashMap=new HashMap<>();

    public static void addBitmap(Bitmap bitmap, boolean isLogo) {
        if (!isLogo) {
            OpenGLRenderer.mBitmaps.put(mBitmaps.size(), bitmap);
        } else if (!OpenGLRenderer.mBitmaps.containsValue(bitmap)) {
            OpenGLRenderer.mBitmaps.put(OpenGLRenderer.mBitmaps.size(), bitmap);
        }
    }

    public static void addBitmap(int key, Bitmap bitmap) {
        OpenGLRenderer.mBitmaps.put(key, bitmap);
    }

    public static Bitmap getBitmap(int key) {
        return OpenGLRenderer.mBitmaps.get(key);
    }

    public static Bitmap getUrlBitmap(String url) {
        return OpenGLRenderer.mUrlBitmap.get(url);
    }

    public static void setUrlBitmap(String url,Bitmap bitmap) {
        OpenGLRenderer.mUrlBitmap.put(url,bitmap);
    }

    public Rect getTransformedFocusRect(int x, int y, int[] focusIntensity) {
        ArrayList<Drawable2d> defaultDrawables = mDrawables.get(DRAWABLES_DEFAULT);
        Drawable2d drawable = null;
        if (defaultDrawables != null) {
            drawable = defaultDrawables.get(0);
        }
        if (drawable == null) return null;

        float[] zoom = drawable.getZoom();
        float[] scale = drawable.getRawScale();

//        return new Rect(300, 300, 900, 900);

        int X = (int) (-1000 + 2000 * ((float) x) / mOutgoingWidth);
        int Y = (int) (-1000 + 2000 * ((float) y) / mOutgoingHeight);
        int width = (int) (300 / zoom[0]);
        int height = (int) (300 / zoom[0]);

        focusIntensity[0] = (int) (focusIntensity[0] + 100 * zoom[0]);
        focusIntensity[0] = Math.max(0, Math.min(1000, focusIntensity[0]));
        return new Rect(Math.max(-1000, X - width / 2), Math.max(-1000, Y - height / 2), Math.min(1000, X + width / 2), Math.min(1000, Y + height / 2));
    }

    public void clearErrors() {
        mCache.mGlUtil.checkGlError("Attempting to clear Errors");
    }

    public void clearCaptureBitmaps() {
        resetAllocatedBitmapCount();
        if(mCapturedBitmapsConsumer == null || mCapturedBitmapsProducer == null) return;
        while(mCapturedBitmapsConsumer.size() != 0) {
            try {
                Bitmap bitmap = mCapturedBitmapsConsumer.take();
                if(bitmap != null) {
                    bitmap.recycle();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while(mCapturedBitmapsProducer.size() != 0) {
            try {
                Bitmap bitmap = mCapturedBitmapsProducer.take();
                if(bitmap != null) {
                    bitmap.recycle();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mCapturedBitmapsConsumer.clear();
        mCapturedBitmapsProducer.clear();
    }

    public LinkedBlockingQueue<Bitmap> [] prepareCaptureBitmaps() {
        mBitmapCount = 0;
        mCapturedBitmapsConsumer = new LinkedBlockingQueue<>(MAX_BITMAP_QUEUE_SIZE+2);
        mCapturedBitmapsProducer = new LinkedBlockingQueue<>(MAX_BITMAP_QUEUE_SIZE+2);
        mBackingArrays = new LinkedBlockingQueue<>(MAX_BITMAP_QUEUE_SIZE);

        LinkedBlockingQueue<Bitmap> [] capturedBitmapsQueues = new LinkedBlockingQueue[2];
        capturedBitmapsQueues[0] = mCapturedBitmapsProducer;
        capturedBitmapsQueues[1] = mCapturedBitmapsConsumer;
        return capturedBitmapsQueues;
    }

    public void updateVideoBackground(Bitmap bitmap) {
        if(bitmap == null) {
            removeDrawables(DRAWABLES_UNDERLAY);
            return;
        }
        addBitmapDrawable(DRAWABLES_UNDERLAY, 0.0f, 0.0f, 1.0f, 1.0f, bitmap, true);
        resetBlendFactor();
    }

    public void updateVideoBackground(float [] colors) {
        if(colors == null) {
            removeDrawables(DRAWABLES_UNDERLAY);
            return;
        }

/*        Log.d(TAG, "Static Colors of size" + colors.length);
        for(int k = 0; k < 4; k++) {
            String cc = "";
            for(int l = 0; l < 4; l++) {
                cc += colors[4*k + l] + " ";
            }
            Log.d(TAG, "" + cc);
        }*/
        addColorDrawable(DRAWABLES_UNDERLAY, 0.0f, 0.0f, 1.0f, 1.0f, colors, true);
        resetBlendFactor();
    }

    public void toggleSwapBlur(Bitmap bitmap) {
        if(bitmap == null) {
            removeDrawables(DRAWABLES_UPPER_OVERLAY);
            return;
        }
        addBitmapDrawable(DRAWABLES_UPPER_OVERLAY, 0.0f, 0.0f, 1.0f, 1.0f, bitmap, true);
    }

    public static void removeStaticDrawables() {
        mOverlayDrawables.clear();
        mUpperOverlayDrawables.clear();
    }

    public static void destroyCache() {
        mRendererCount = 0;
        //terminateCache();
    }

    public void setRenderer(Renderer renderer) {
        mRenderer = renderer;
    }

    enum ModelViewMode {
        /**
         * Used when the model view should simply translate geometry passed to the shader. The resulting
         * matrix will be a simple translation.
         */
        kModelViewMode_Translate,

        /**
         * Used when the model view should translate and scale geometry. The resulting matrix will be a
         * translation + scale. This is frequently used together with VBO 0, the (0,0,1,1) rect.
         */
        kModelViewMode_TranslateAndScale,
    }

    ProgramDescription mDescription;

    private static HashMap<Renderer, Caches> mCaches = new HashMap<>(5);
    private static HashMap<Renderer, float[]> mSTMatrices = new HashMap<>(5);
    public Caches mCache;

    Rect mClipRect;
    int mViewportWidth;
    int mViewportHeight;

    private boolean mDirtyClip;

    // Color description
    boolean mColorSet;
    float mColorA, mColorR, mColorG, mColorB;
    // Indicates that the shader should get a color
    boolean mSetShaderColor;

    // Current texture unit
    int mTextureUnit;

    //    Matrix mModelViewMatrix;
    float[] mModelMatrix;
    float[] mViewMatrix;
    float[] mModelViewMatrix;
    float[] mProjectionMatrix;
    float[] mMVPMatrix;

    public static enum Fuzzy {
        PREVIEW,
        VIDEO,
        OFFSCREEN
    }

    public Caches getCacheInstance() {
        return mCache;
    }

    public static Caches getCacheInstance(Renderer renderer) {
        Caches cache = mCaches.get(renderer);
        if (cache == null) {
            cache = new Caches();
            Log.d(LOG_TAG, "Creating Cache: " + cache + " for renderer: " + renderer);

//            cache.setRenderer(renderer);
            mCaches.put(renderer, cache);
        } else {
            Log.d(LOG_TAG, "Cache: " + cache + " for renderer: " + renderer);
        }
        return cache;
    }

/*    public void terminateCache() {
        if(mCaches != null) {
            for(Renderer renderer : mCaches.keySet()) {
                if(mCache == mCaches.get(renderer)) {
                    mCaches.remove(renderer);
                }
            }
        }
        mCache.terminate();
    }*/

    public void terminateCache() {
        terminateCache(mRenderer);
    }

    public static void terminateCache(Renderer renderer) {
        if(mCaches != null) {
            Caches cache = mCaches.remove(renderer);
            if(cache != null) {
                cache.terminate();
            }
        }
    }

    private void addColorDrawable() {
        Drawable2d drawable = new Drawable2d();
        drawable.setIsColorBased(true);
        drawable.setupDraw();

        float drawableScreenWidth = 0.25f;
        float drawableScreenHeight = drawableScreenWidth * 0.5f;
        drawable.setScale(new float[]{drawableScreenWidth, drawableScreenHeight, 1.0f});
        mDefaultDrawables.add(new Drawable2d(drawable));
    }

    public void setmContext(Context context){
        mContext=context;
    }
    /**
     * Add a drawable based on External texture (to which Camera or Video Player/extractor can feed frames)
     * @param isCamera
     * isCamera to indicate the data source.
     * This is used to determine whether orientation change affects the orientation of the frames,
     * which happens in the case of camera.
     */
    public void addExternalSourceDrawable(boolean isCamera) {
        mInputSourceCamera = isCamera;
        //For rendering external textures
        Drawable2d drawable = new Drawable2d();
        drawable.mUsesExternalTexture = true;
        if(mRenderTargetType == Fuzzy.PREVIEW) {
            drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
        } else if(mRenderTargetType == Fuzzy.VIDEO) {
            drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
        } else {
            drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
        }
        drawable.setIsFlipped(false);
        drawable.setIsFBOBased(false);
        drawable.setIsColorBased(false);
        drawable.setIsBitmapBased(false);
        drawable.setIsMultiBitmapBased(false);
     //   drawable.setConvolutionType(Drawable2d.CONVOLUTION_TYPE_BLUR); // critical sahil.bajaj@instalively.com
        drawable.setupDraw();
        mDefaultDrawables.add(new Drawable2d(drawable));

        if(mDrawables.get(DRAWABLES_DEFAULT) == null) {
            addDrawables(DRAWABLES_DEFAULT, mDefaultDrawables);
        }
    }

    public OpenGLRenderer(Fuzzy type, Renderer renderer) {

        mIsPreview = type == Fuzzy.PREVIEW;
        mIsVideo = type == Fuzzy.VIDEO;
        mIsOffscreen = type == Fuzzy.OFFSCREEN;
        mRenderTargetType = type;

        mRenderer = renderer;
        init();

        mIsStagnantFrame = false;
        mStagnantSettled = false;

        mUseFBO = mIsVideo && USE_FBO;
        if(mUseFBO) {
            fboTarget = new FBO[BLUR_PASS_COUNT];
            for(int i = 0; i < BLUR_PASS_COUNT; i++) {
                fboTarget[i] = new FBO();
            }
        }

        //HashMap containing Drawables for each layer
//For example, camera preview will form the bottom most layer. Then the Preview. Then the WaterMark.
        mDrawables = new TreeMap<Integer, ArrayList<Drawable2d>>();
//    HashMap<Integer, ArrayList<Drawable2d>> mEncoderDrawables = new HashMap<Integer, ArrayList<Drawable2d>>();

        mDefaultDrawables = new ArrayList<Drawable2d>();
        mRawDrawables = new ArrayList<Drawable2d>();

//        addExternalSourceDrawable();

        if(type == Fuzzy.PREVIEW) {
            TAG = "Preview" + "GLRenderer";
//            mAnimatableDrawables = new Tree<>();
            mAnimatableDrawables = new ArrayList<>();
            mDrawableLocations = new ArrayList<>();
        } else if (type == Fuzzy.VIDEO) {
            TAG = "Video" + "GLRenderer";
            mAnimatableDrawables = new ArrayList<>();
            mDrawableLocations = new ArrayList<>();
        } else if(type == Fuzzy.OFFSCREEN) {
            TAG = "OffScreen" + "GLRenderer";
            mBitmapCount = 0;
        } else {
            TAG = "Unknown" + "GLRenderer";
        }
        mCache = getCacheInstance(mRenderer);
        setSurfaceTexture(mCache.getSurfaceTexture());

        if (mDefaultDrawables != null && !mDefaultDrawables.isEmpty()) {
            addDrawables(DRAWABLES_DEFAULT, mDefaultDrawables);
        } else {
            Log.e(TAG, "mDefaultDrawables is null or empty");
        }

        if (mRawDrawables != null && !mRawDrawables.isEmpty()) {
            addDrawables(DRAWABLES_RAW, mRawDrawables);
        } else {
            Log.e(TAG, "mRawDrawables is null or empty");
        }

        if (mIsVideo && mOverlayDrawables != null && !mOverlayDrawables.isEmpty()) {
            Log.d(TAG, "Adding overlay drawables");
            addDrawables(DRAWABLES_OVERLAY, mOverlayDrawables);
        } else {
            Log.w(TAG, "mOverlayDrawables is null or empty");
        }

        if (mUpperOverlayDrawables != null && !mUpperOverlayDrawables.isEmpty()) {
            addDrawables(DRAWABLES_UPPER_OVERLAY, mUpperOverlayDrawables);
        } else {
            Log.w(TAG, "mUpperOverlayDrawables is null or empty");
        }
    }

    Renderer mRenderer;

    void init() {
        mIncomingSizeUpdated = false;

        mCurrentFilter = -1;

        mFrameStarted = false;

        mDescription = new ProgramDescription();

        mRendererCount++;

        mAlphaFactor = 1.0f;

        mModelMatrix = new float[16];
        mViewMatrix = new float[16];
        mModelViewMatrix = new float[16];
        mProjectionMatrix = new float[16];
        mMVPMatrix = new float[16];

        bitmapSource = null;
        bitmapBuffer = null;
        mCaptureBitmap = null;
    }

    public static void addBitmapDrawable(boolean isLogo, Bitmap bitmap) {
        Drawable2d drawable = new Drawable2d();
        drawable.setBitmap(bitmap);
        drawable.setIsBitmapBased(true);
        drawable.setupDraw();
        drawable.setBlendMode(Drawable2d.BLEND_MODE_ADD);
//        drawable.setTranslate(new float[]{-0.775f, 0.475f, 0.0f});
        float drawableScreenWidth = 0.1666f;
        float drawableScreenHeight = drawableScreenWidth * bitmap.getHeight() / bitmap.getWidth();
        drawable.setTranslate(new float[]{-1.0f + drawableScreenWidth * 2 - 0.15f, 1.0f - drawableScreenHeight * 2 - 0.02f, 0.0f});
        drawable.setScale(new float[]{drawableScreenWidth, drawableScreenHeight, 1.0f});

        if (isLogo && !mBitmaps.containsValue(bitmap)) {
            OpenGLRenderer.addBitmap(bitmap, true);
            mOverlayDrawables.add(new Drawable2d(drawable));
        } else if (!mBitmaps.containsValue(bitmap)) {
            OpenGLRenderer.addBitmap(bitmap, false);
            mUpperOverlayDrawables.add(new Drawable2d(drawable));
        }
    }

    public static void addBitmapDrawable(boolean isLogo, Bitmap bitmap, boolean isPreview) {
        Drawable2d drawable = new Drawable2d();
        drawable.setBitmap(bitmap);
        drawable.setIsBitmapBased(true);
        drawable.setTranslate(new float[]{-0.775f, 0.375f, 0.0f});
        drawable.setScale(new float[]{0.25f, 0.25f * bitmap.getHeight() / bitmap.getWidth(), 1.0f});

    }

    public void addBitmapDrawable(float x, float y, float scaleX, float scaleY, Bitmap bitmap) {
        addBitmapDrawable(DRAWABLES_DEFAULT, x, y, scaleX, scaleY, bitmap, false);
    }

    public static void addStaticBitmapDrawable(float x, float y, float scaleX, float scaleY, Bitmap bitmap, int layer) {
        addStaticBitmapDrawable(DRAWABLES_OVERLAY, x, y, scaleX, scaleY, bitmap, false);
    }

    public void addAnimatableBitmapDrawable(float x, float y, float scaleX, float scaleY, Bitmap bitmap) {
        addBitmapDrawable(DRAWABLES_ANIMATABLE, x, y, scaleX, scaleY, bitmap, false);
    }

    public Drawable2d addAnimatableBitmapDrawable(float x, float y, float scaleX, float scaleY, Bitmap bitmap, boolean update) {
        return addBitmapDrawable(DRAWABLES_ANIMATABLE, x, y, scaleX, scaleY, bitmap, update);
    }

    public void addBitmapDrawable(int layer, float x, float y, float scaleX, float scaleY, Bitmap bitmap) {
        addBitmapDrawable(layer, x, y, scaleX, scaleY, bitmap, false);
    }

    // Written assuming there will be max 2 color drawables on top of each other for blending purposes
    public void addColorDrawable(int layer, float x, float y, float scaleX, float scaleY, float [] colors, boolean update) {
        Drawable2d drawable0 = null;
        Drawable2d drawable1 = null;
        Drawable2d drawable2 = null;

        if(update) {
            // Query the last drawable
            drawable0 = peekDrawable(layer, 1);
            drawable1 = peekDrawable(layer, 0);
        }

        if(drawable0 != null) {
            removeDrawable(layer, 1);
        }

        drawable2 = new Drawable2d();
        drawable2.setIsColorBased(true);

        if(drawable1 != null) {
            drawable1.setAlpha(1.0f);
            int index = drawable1.getColorIndex();
            if(colors != null) {
                mCache.setColors(colors, (1 - index));
            }
            drawable2.setColorIndex(1 - index);
/*            if((1-index) == 0) {
                drawable2.setTranslate(new float[]{x, 0.25f + y / mAspectRatio, 0.0f});
            } else {
                drawable2.setTranslate(new float[]{x, -0.25f - y / mAspectRatio, 0.0f});
            }*/
            Log.d(TAG, "Adding new drawable with color index " + (1-index));
        } else {
            if(colors != null) {
                mCache.setColors(colors, 0);
            }
            drawable2.setColorIndex(0);
            drawable2.setAlpha(0.0f);
//            drawable2.setTranslate(new float[]{x, 0.25f + y / mAspectRatio, 0.0f});
        }

        Drawable2d drawable = drawable2;

        drawable.setTranslate(new float[]{x, y / mAspectRatio, 0.0f});
        drawable.setScale(new float[]{scaleX, scaleY / mAspectRatio, 1.0f});
        drawable.setupDraw();
        if(layer == DRAWABLES_ANIMATABLE) {
            addAnimDrawable(drawable);
        } else {
            addDrawable(layer, drawable);
        }
    }

    public static void addStaticBitmapDrawable(int layer, float x, float y, float scaleX, float scaleY, Bitmap bitmap, boolean update) {
        Drawable2d drawable = new Drawable2d();
        drawable.setBitmap(bitmap);
        drawable.setIsBitmapBased(true);
        drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
        drawable.setTranslate(new float[]{x, y, 0.0f});
        //If scale type is one of inside, crop, fit etc... then scale values will be overidden
        drawable.setScale(new float[]{scaleX, (scaleY * bitmap.getHeight()) / bitmap.getWidth(), 1.0f});
        drawable.setupDraw();
        mOverlayDrawables.add(drawable);
    }


    public Drawable2d addBitmapDrawable(int layer, float x, float y, float scaleX, float scaleY, Bitmap bitmap, boolean update) {
        Drawable2d drawable = null;
        if(update) {
            drawable = peekDrawable(layer, 2);
            if(drawable != null) {
                removeDrawable(layer, 2);
            }
            drawable = null;
        }
        if(drawable == null) {
            drawable = new Drawable2d();
            drawable.setIsBitmapBased(true);
        } else {
            drawable.setIsMultiBitmapBased(true);
        }
        drawable.setBitmap(bitmap);
//        Bitmap evictedBitmap = drawable.addBitmap(bitmap);
//        deleteBitTex(evictedBitmap);
//        drawable.setBitmap(bitmap);
        drawable.setTranslate(new float[]{x, y / mAspectRatio, 0.0f});
//        drawable.setTranslate(new float[]{0.0f, 0.0f, 0.0f});

        drawable.setScale(new float[]{scaleX, (scaleY * bitmap.getHeight()) / bitmap.getWidth(), 1.0f});
        drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
        setDrawableRenderParams(drawable);
        //TODO This is a wrong way to set filter... will screw up things when we add stickers etc.
        // Because we're applying filter to just any drawable, which shouldn't be the case.
        drawable.setFilterMode(mExternalTextureFilter);
        drawable.setupDraw();
        if(layer == DRAWABLES_ANIMATABLE) {
            addAnimDrawable(drawable);
        } else {
            addDrawable(layer, drawable, update);
        }
        return drawable;
    }

    private void deleteBitTex(Bitmap evictedBitmap) {
        Texture evictedTexture = getTexture(evictedBitmap);
        if(evictedTexture != null) {
            mCache.mTextureCache.deleteTexture(evictedTexture.id);
        }
        if (evictedBitmap != null) {
            evictedBitmap.recycle();
        }
    }

    public void addDrawable(int layer, Drawable2d drawable) {
        addDrawable(layer, drawable, false);
    }

    public void addDrawable(int layer, Drawable2d drawable, boolean update) {
        HashMap<Integer, ArrayList<Drawable2d>> drawables;

        ArrayList<Drawable2d> drawableList = mDrawables.get(layer);
        if (drawableList == null) {
            drawableList = new ArrayList<Drawable2d>();
            drawableList.add(new Drawable2d(drawable));
            mDrawables.put(layer, drawableList);
        } else {
            if(update) {
                Drawable2d drawable2d = drawableList.get(drawableList.size()-1);
                drawableList.set(drawableList.size() - 1, new Drawable2d(drawable));
            } else {
                drawableList.add(new Drawable2d(drawable));
            }
        }
    }

    public void removeDrawables(int layer) {
        mDrawables.remove(layer);
        if(layer == DRAWABLES_ANIMATABLE) {
            if(mAnimatableDrawables != null) {
                mAnimatableDrawables.clear();
            }
        }
    }

    Drawable2d peekDrawable(int layer) {
        Drawable2d drawable = null;
        ArrayList<Drawable2d> drawableList = mDrawables.get(layer);
        if(drawableList!= null) {
            drawable = drawableList.get(drawableList.size() - 1);
        }
        return drawable;
    }

    // Picks from 'index' position from the end
    Drawable2d peekDrawable(int layer, int index) {
        Drawable2d drawable = null;
        ArrayList<Drawable2d> drawableList = mDrawables.get(layer);
        if(drawableList!= null) {
            if(index < drawableList.size()) {
                drawable = drawableList.get((drawableList.size() - 1) - index);
            }
        }
        return drawable;
    }

    Drawable2d removeDrawable(int layer, int index) {
        Drawable2d drawable = null;
        ArrayList<Drawable2d> drawableList = mDrawables.get(layer);
        if(drawableList!= null) {
            if(index < drawableList.size()) {
                drawable = drawableList.get((drawableList.size() - 1) - index);
                drawableList.remove((drawableList.size() - 1) - index);
            }
        }
        return drawable;
    }

    void addDrawables(int layer, List<Drawable2d> drawables) {

        ArrayList<Drawable2d> drawableList = mDrawables.get(layer);
        if (drawableList == null) {
            drawableList = new ArrayList<>();

            mDrawables.put(layer, drawableList);
        }
        Log.d(TAG, "Going to add drawables to layer: " + layer);
        for (Drawable2d drawable : drawables) {
            Log.d(TAG, "Adding drawable: " + drawable + " to layer: " + layer);
            drawableList.add(new Drawable2d(drawable));
        }
    }

    public void setExternalTextureZoom(float scale) {
        if (scale < 1.0f || scale > 3.0f) {
            return;
        }
        ArrayList<Drawable2d> defaultDrawables = mDrawables.get(DRAWABLES_DEFAULT);
        if (defaultDrawables != null) {
            Iterator<Drawable2d> itr = defaultDrawables.iterator();
            while (itr.hasNext()) {
                Drawable2d drawable = itr.next();
                drawable.setZoom(scale, scale, 1.0f);
//                drawable.multiplyScale(drawable.getZoom());
            }
        }
    }

    public void resetLifecycle() {
        resetDrawableProperties();
    }

    public void setExternalTextureFilter(int filterMask, boolean enable) {
        if(VERBOSE) Log.d(TAG, "Filter being set to " + filterMask);
//        mExternalTextureFilter = enable ? mExternalTextureFilter | filterMask : mExternalTextureFilter & ~filterMask;
//For now the filters are all exclusive, i.e. only 1 filter can be applied a one point of time. So only 1 bit is set at a time.
//Anyway, this needs to be implemented by the drawable class not here. Because we won't know what filter each drawable is using.
        mExternalTextureFilter = filterMask;
        ArrayList<Drawable2d> drawables = mDrawables.get(DRAWABLES_DEFAULT);
        if (drawables != null) {
            Iterator<Drawable2d> itr = drawables.iterator();
            while (itr.hasNext()) {
                Drawable2d drawable = itr.next();
                drawable.setFilterMode(mExternalTextureFilter);
            }
        }
        drawables = mAnimatableDrawables;
        if (drawables != null) {
            Iterator<Drawable2d> itr = drawables.iterator();
            while (itr.hasNext()) {
                Drawable2d drawable = itr.next();
                drawable.setFilterMode(mExternalTextureFilter);
            }
        }
//        mDefaultDrawables.get(0).setFilterMode(mExternalTextureFilter);
    }

    public void setExternalTextureFlip(boolean flip) {
        if(VERBOSE) Log.d(TAG, "Flip being set to " + (flip ? "true" : "false"));

        ArrayList<Drawable2d> defaultDrawables = mDrawables.get(DRAWABLES_DEFAULT);
        if (defaultDrawables != null) {
            Iterator<Drawable2d> itr = defaultDrawables.iterator();
            while (itr.hasNext()) {
                Drawable2d drawable = itr.next();
                drawable.setIsFlipped(flip);
            }
        }
    }

    public Bitmap createBitmapFromGLSurface() {
        return createBitmapFromGLSurface(0, 0, mOutgoingWidth, mOutgoingHeight);
    }

    public Bitmap createBitmapFromGLSurface(int x, int y, int w, int h)
            throws OutOfMemoryError {
        if (bitmapBuffer == null || bitmapSource==null || mCaptureBitmap==null) {
            Log.d(TAG, "Bitmap buffer or Capture bitmap is null");
            allocTempResources();

            if (bitmapBuffer == null || bitmapSource==null || mCaptureBitmap==null) //Even after allocating resources, if its still null, somehow the renderer is destroyed. So return null
                return null;
        }
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        /**
         * fixme    java.lang.NullPointerException
         at java.nio.IntBuffer.wrap(IntBuffer.java:63)
         at com.instalively.android.gles.OpenGLRenderer.createBitmapFromGLSurface(OpenGLRenderer.java:790)
         at com.instalively.android.gles.OpenGLRenderer.createBitmapFromGLSurface(OpenGLRenderer.java:785)
         at com.instalively.android.broadcast.CameraSurfaceRenderer.captureBitmap(StreamingActionControllerKitKat.java:5935)
         at com.instalively.android.broadcast.CameraSurfaceRenderer.onDrawFrame(StreamingActionControllerKitKat.java:5370)
         */
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        mCaptureBitmap.setPixels(bitmapSource, 0, w, 0, 0, w, h);
        return mCaptureBitmap;
    }

    SBitmap captureScreen() {
        return captureScreen(false);
    }

    /**
     * Call at beginning/end of a frame
     */
    SBitmap captureScreen(boolean intBuf) {
        Buffer pixelBuf = null;

        if(intBuf) {
            pixelBuf = IntBuffer.allocate(mOutgoingWidth * mOutgoingHeight);
        } else {
            pixelBuf = ByteBuffer.allocateDirect(mOutgoingWidth * mOutgoingHeight * 4);
            ((ByteBuffer)pixelBuf).order(ByteOrder.LITTLE_ENDIAN);
        }
        pixelBuf.position(0);
        // Try to ensure that rendering has finished.
        // First a glFinish and then a dummy read of 1 pixel just in case, glFinish is fucked by the drivers.
        GLES20.glFinish();
        GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

        GLES20.glReadPixels(0, 0, mOutgoingWidth, mOutgoingHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

        SBitmap sBitmap;
        if(intBuf) {
            sBitmap = new SBitmap((IntBuffer)pixelBuf, mOutgoingWidth, mOutgoingHeight);
        } else {
            sBitmap = new SBitmap((ByteBuffer)pixelBuf, mOutgoingWidth, mOutgoingHeight);
        }
        return sBitmap;
    }

    public void useRawFrame() {
        removeDrawables(DRAWABLES_DEFAULT);
        addDrawables(DRAWABLES_RAW, mRawDrawables);
    }

    public void resetAllocatedBitmapCount() {
        Log.d(TAG, "resetAllocatedBitmapCount");
        mBitmapCount = 0;
    }

    public boolean readDisplay(/*IntBuffer pixelBuf*/) {
        boolean result = true;

        IntBuffer pixelBuf = mBackingArrays.poll();

        if(pixelBuf == null) {
            if (mBackingArrays.size() == 0) {
                if(mBitmapCount < MAX_BITMAP_QUEUE_SIZE) {
                    Log.d(TAG, "Allocating new backing array for captured bitmap");
                    pixelBuf = IntBuffer.allocate(mOutgoingWidth*mOutgoingHeight);
                    boolean added = mBackingArrays.offer(pixelBuf);
                    if(VERBOSE) Log.d(TAG, "New Buffer added to Backing Queue " + added);
                    result = true;
                } else {
                    Log.w(TAG, "Can't allocate more backing arrays.. Nothing can be done here...");
                    result = false;
                }
            } else {
                Log.w(TAG, "Can't allocate more backing arrays. Waiting for others to be freed");
                result = false;
            }
        } else {
            boolean added = mBackingArrays.offer(pixelBuf);
            if(VERBOSE) Log.d(TAG, "Buffer added to Backing Queue " + added);
        }

        if(!result) {
            return false;
        }

        pixelBuf.position(0);
//        pixelBuf.order(ByteOrder.LITTLE_ENDIAN);

        // Try to ensure that rendering has finished.
        // First a glFinish and then a dummy read of 1 pixel just in case, glFinish is fucked by the drivers.
        GLES20.glFinish();
        GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

        GLES20.glReadPixels(0, 0, mOutgoingWidth, mOutgoingHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

/*        int [] result = new int[pixelBuf.limit()];
        pixelBuf.get(result);*/
        int[] pixelArray = pixelBuf.array();

        if (pixelArray == null) {
            Log.e(TAG, "Couldn't read anything from the display for capturing");
        } else {
            if (pixelArray.length < mOutgoingWidth * mOutgoingHeight) {
                Log.e(TAG, "Couldn't read anything from the display for capturing, data read of size : " + pixelArray.length + " words");
                return false;
            }
        }
        Bitmap bitmap = mCapturedBitmapsConsumer.poll();

        if(bitmap != null) {
            if (VERBOSE) Log.d(TAG, "Returning reused Bitmap of size " + mOutgoingWidth + "x" + mOutgoingHeight);
            if((bitmap.getWidth() > mOutgoingWidth) || (bitmap.getHeight() > mOutgoingHeight)) {
                mBitmapCount--;
                if(VERBOSE) Log.d(TAG, "rotated bitmap. So creating a new one");
                result = false;
            } else {
                bitmap.setPixels(pixelBuf.array(), 0, mOutgoingWidth, 0, 0, mOutgoingWidth, mOutgoingHeight);
                result = mCapturedBitmapsProducer.offer(bitmap);
            }
            //mBackingArrays.offer(pixelBuf);
        } else {
            if(mCapturedBitmapsProducer.size() == 0) {
                if(mBitmapCount++ < MAX_BITMAP_QUEUE_SIZE) {
                    Log.d(TAG, "Returning new Bitmap of size " + mOutgoingWidth + "x" + mOutgoingHeight);
                    bitmap = Bitmap.createBitmap(mOutgoingWidth, mOutgoingHeight, Bitmap.Config.ARGB_8888);
                    bitmap.setPixels(pixelBuf.array(), 0, mOutgoingWidth, 0, 0, mOutgoingWidth, mOutgoingHeight);
                    result = mCapturedBitmapsProducer.offer(bitmap);
                    //mBackingArrays.offer(pixelBuf);
                } else {
                    Log.w(TAG, "Couldn't get Bitmap from queue. Enough bitmaps allocated");
                    result = false;
                }
            } else {
                if(VERBOSE) Log.w(TAG, "Couldn't get Bitmap from queue, Queue size: " + mCapturedBitmapsProducer.size());
                mCapturedBitmapsProducer.clear();
                mBitmapCount = 0;
                bitmap = Bitmap.createBitmap(mOutgoingWidth, mOutgoingHeight, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixelBuf.array(), 0, mOutgoingWidth, 0, 0, mOutgoingWidth, mOutgoingHeight);
                result = mCapturedBitmapsProducer.offer(bitmap);
            }
        }
        return result;
    }

    public void unUseRawFrame() {
        removeDrawables(DRAWABLES_RAW);
        addDrawables(DRAWABLES_DEFAULT, mDefaultDrawables);
        setDrawableRenderParams();
    }

/*    public void handleFlipAnimation() {
//        Log.d(TAG, "blend factor is " + mBlendFactor);

        int slot = currentProgram.getUniform("alphaFactor");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: alphaFactor returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: alphaFactor returns nice location");
        }
        GLES20.glUniform1f(slot, mAlphaFactor);
    }*/

    public void fadeInAnimation() {
        Log.d(TAG, "fade animation On");
        unUseRawFrame();
    }

    public void fadeOutAnimation(Bitmap bitmap) {
        if(bitmap == null) {
            Log.d(TAG, "fade animation Off... Bitmap is null. NOP");
            mRawDrawables.clear();
            unUseRawFrame();
            return;
        }
        Log.d(TAG, "fade animation Off");
/*            SBitmap sBitmap = captureScreen(true);
            Bitmap bitmap = Bitmap.createBitmap(mOutgoingWidth, mOutgoingHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(((IntBuffer)sBitmap.getPixelBuf()).array(), 0, mOutgoingWidth, 0, 0, mOutgoingWidth, mOutgoingHeight);

            Bitmap blurBitmap = BlurBuilder.blur(bitmap, 0.25f, 20);
            int [] pixels = new int[blurBitmap.getWidth()*blurBitmap.getHeight()];
            blurBitmap.getPixels(pixels, 0, blurBitmap.getWidth(), 0, 0, blurBitmap.getWidth(), blurBitmap.getHeight());
            IntBuffer intBuffer = IntBuffer.wrap(pixels);
            SBitmap sbit = new SBitmap(intBuffer, blurBitmap.getWidth(), blurBitmap.getHeight());
            Log.d(TAG, "fade animation image blurred " + blurBitmap);*/

        Drawable2d drawable = new Drawable2d();
        drawable.setIsBitmapBased(true);
        drawable.setBitmap(bitmap);
        drawable.setupDraw();
        drawable.setScale(1.0f, 1.0f / mAspectRatio, 1.0f);

        mRawDrawables.clear();
        mRawDrawables.add(drawable);
        useRawFrame();
    }

    public static Drawable2d createBitmapDrawable(Bitmap bitmap) {
        Drawable2d drawable = new Drawable2d();
        drawable.setBitmap(bitmap);
        if(bitmap != null) {
            drawable.setIncomingSize(bitmap.getWidth(), bitmap.getHeight());
        }
        drawable.setIsBitmapBased(true);
        drawable.setupDraw();
        return drawable;
    }

    private Drawable2d createSBitmapDrawable(SBitmap bitmap) {
        Drawable2d drawable = new Drawable2d();
        drawable.setSBitmap(bitmap);
        drawable.setIsSBitmapBased(true);
        return drawable;
    }

    public void addAnimDrawable(Drawable2d drawable) {
        if(mAnimatableDrawables == null) return;
        int index = mAnimatableDrawables.size();
//        mAnimatableDrawables.add(index, drawable);
        mAnimatableDrawables.add(drawable);
        drawable.printBounds();
/*        float [] translation = drawable.getTranslate();
        float [] scale = drawable.getScale();
        float x1 = translation[0] - scale[0]/2;
        float y1 = translation[1] - scale[1]/2;
        float x2 = x1 + scale[0];
        float y2 = y1 + scale[1];*/
        RectF rect = (mDrawableLocations.size() > index) ? mDrawableLocations.get(index) : null;
        if(rect != null) {
            rect.set(drawable.getBounds());
        } else {
            mDrawableLocations.add(index, new RectF(drawable.getBounds()));
        }
    }

    public Drawable2d pickDrawable(PointF point) {
        point.y /= mAspectRatio;
        int size = (mAnimatableDrawables != null) ? mAnimatableDrawables.size() : 0;
        for(int i = 0; i < size; i++) {
            RectF rect = mDrawableLocations.get(i);
            if(rect.contains(point.x, point.y)) {
                return mAnimatableDrawables.get(i);
            }
        }
        return null;
    }

    public static class SBitmap {
        Buffer mPixelBuf;
        int mWidth;
        int mHeight;
        BUF_TYPE mType;

        public static enum BUF_TYPE {
            BYTE,
            INT,
            SHORT,
            FLOAT
        }

        public SBitmap(ByteBuffer pixelBuf, int width, int height) {
            mPixelBuf = pixelBuf;
            mWidth = width;
            mHeight = height;
            mType = BUF_TYPE.BYTE;
        }

        public SBitmap(IntBuffer pixelBuf, int width, int height) {
            mPixelBuf = pixelBuf;
            mWidth = width;
            mHeight = height;
            mType = BUF_TYPE.INT;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getByteCount() {
            return mWidth * mHeight * 4;
        }

        public Buffer getPixelBuf() {
            return mPixelBuf;
        }

        public void recycle() {
            mPixelBuf = null;
        }
/*        public BUF_TYPE getType() {
            return mType;
        }*/
    }


    void onViewportInitialized() {
        GLES20.glEnable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_ALPHA);
//        GLES11Ext.glAlphaFuncxOES(GLES20.GL_GEQUAL, 50);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        if(mIsVideo) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        } else {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }
//        clear(0, 0, mViewportWidth, mViewportHeight, false);

        mCache.mGlUtil.checkGlError("onViewportInitialized");
        mFirstFrameAfterResize = true;

        setDrawableRenderParams();
    }

    public float getVerticalVideoScale() {
        if (mVerticalVideoMode) {
            return 1.0f/mAspectRatio;
        } else {
            return 1.0f;
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setDrawableRenderParams() {
        incomingWidth = mIncomingWidth;
        incomingHeight = mIncomingHeight;
        //Camera image width and height parameters depend on the orientation, In portrait case, height is actually the width and vice-versa
        if(mInputSourceCamera) {
            if (mVerticalVideoMode) {
                if (mPreviewHeight > mPreviewWidth) {
                    incomingHeight = mIncomingWidth;
                    incomingWidth = mIncomingHeight;
                }
            } else {
                if (mOutgoingHeight > mOutgoingWidth) {
                    incomingHeight = mIncomingWidth;
                    incomingWidth = mIncomingHeight;
                }
            }
        }

        allocTempResources();

        if (VERBOSE)
            Log.d(TAG, "DRAWABLE Scaling :: width : " + incomingWidth + " height: " + incomingHeight + " ratio: " + (float) incomingWidth / (float) incomingHeight);
        if (VERBOSE)
            Log.d(TAG, "DRAWABLE Scaling Preview width : " + mPreviewWidth + " height : " + mPreviewHeight);
        if (VERBOSE)
            Log.d(TAG, "DRAWABLE Scaling Output width : " + mOutgoingWidth + " height : " + mOutgoingHeight);

/*        if(incomingHeight != 0) {
            float cameraAspect = ((float) incomingWidth / incomingHeight);
            mCameraAspectRatio = cameraAspect;
        }*/

        if(mIsPreview || mIsVideo) {
            sScale = (mDistance + mCache.mWallDist)/mDistance/2.0f;
        }

        Iterator<ArrayList<Drawable2d>> iterator = mDrawables.values().iterator();
        while ((iterator.hasNext())) {
            ArrayList<Drawable2d> drawableList = iterator.next();
            for (Drawable2d drawable : drawableList) {
                setDrawableRenderParams(drawable);
            }
        }

    }

    public void setDrawableRenderParams(Drawable2d drawable) {
        float aspectRatio = 0.5625f;
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float verticalScaleX = 1.0f; // For the purpose of calculating scissor in vertical mode
        float verticalScaleY = 1.0f;
        if (drawable.mUsesExternalTexture) {
            drawable.setIncomingSize(incomingWidth, incomingHeight);
        }
        aspectRatio = (drawable.getIncomingHeight() != 0) ? ((float) drawable.getIncomingWidth()) / drawable.getIncomingHeight() : 0.5625f;
        if (!mVerticalVideoMode) {
            int originalScaleType = drawable.getScaleType();
            if (mPreviewWidth > mPreviewHeight) {
                if (drawable.getScaleType() == Drawable2d.SCALE_TYPE_INSIDE) {
                    drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
                }
            }
            switch (drawable.getScaleType()) {
                case Drawable2d.SCALE_TYPE_CROP:
                    if (mAspectRatio < aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / aspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 2");
                    }
                    mClipRect = null;
                    break;
                case Drawable2d.SCALE_TYPE_INSIDE:
                    if (mAspectRatio > aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / aspectRatio;
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 2");
                    }
                    verticalScaleX = scaleX;
                    verticalScaleY = scaleY * mAspectRatio;
                    mClipRect = new Rect(mOutgoingWidth / 2 - (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 - (int) (verticalScaleY * mOutgoingHeight) / 2, mOutgoingWidth / 2 + (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 + (int) (verticalScaleY * mOutgoingHeight) / 2);
                    break;
                case Drawable2d.SCALE_TYPE_SQUARE:
                    if (mAspectRatio < aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                                /*if(VERBOSE) */
                        Log.d(TAG, "square normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / aspectRatio;
                                /*if(VERBOSE) */
                        Log.d(TAG, "square normal mode 2");
                    }
                    float scale = Math.min(scaleX, scaleY);
                    verticalScaleX = scale;
                    verticalScaleY = scale * mAspectRatio;
                    mClipRect = new Rect(mOutgoingWidth / 2 - (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 - (int) (verticalScaleY * mOutgoingHeight) / 2, mOutgoingWidth / 2 + (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 + (int) (verticalScaleY * mOutgoingHeight) / 2);
                    break;
                case Drawable2d.SCALE_TYPE_FIT:
                    scaleX = 1.0f;
                    scaleY = 1.0f / mAspectRatio;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_HEIGHT:
                    scaleX = aspectRatio / mAspectRatio;
                    scaleY = 1.0f / mAspectRatio;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_WIDTH:
                    scaleX = 1.0f;
                    scaleY = 1.0f / aspectRatio;
                    break;
                default:
                    Log.w(TAG, "Unknown scale type for drawable: " + drawable);
            }
            if (mPreviewWidth > mPreviewHeight) {
                drawable.setScaleType(originalScaleType);
            }
        } else {
            int originalScaleType = drawable.getScaleType();
            if (mPreviewWidth > mPreviewHeight) {
                if (drawable.getScaleType() == Drawable2d.SCALE_TYPE_INSIDE) {
                    drawable.setScaleType(Drawable2d.SCALE_TYPE_CROP);
                }
            }
            switch (drawable.getScaleType()) {
                case Drawable2d.SCALE_TYPE_CROP:
                    if (mAspectRatio < aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / aspectRatio;
                        if (VERBOSE) Log.d(TAG, "crop normal mode 2");
                    }
                    verticalScaleX = scaleX;
                    verticalScaleY = scaleY * mAspectRatio;
                    mClipRect = new Rect(mOutgoingWidth / 2 - (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 - (int) (verticalScaleY * mOutgoingHeight) / 2, mOutgoingWidth / 2 + (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 + (int) (verticalScaleY * mOutgoingHeight) / 2);
                    break;
                case Drawable2d.SCALE_TYPE_INSIDE:
                    if (mAspectRatio > aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                        verticalScaleX = Math.min(aspectRatio, 0.5625f) / mAspectRatio;
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 1");
                    } else {
                        scaleX = 1.0f;
                        scaleY = 1.0f / aspectRatio;
                        verticalScaleX = Math.min(scaleX, 0.5625f);
                        if (VERBOSE) Log.d(TAG, "scale inside normal mode 2");
                    }
                    verticalScaleY = scaleY * mAspectRatio;
                    mClipRect = new Rect(mOutgoingWidth / 2 - (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 - (int) (verticalScaleY * mOutgoingHeight) / 2, mOutgoingWidth / 2 + (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 + (int) (verticalScaleY * mOutgoingHeight) / 2);
                    break;
                case Drawable2d.SCALE_TYPE_SQUARE:
                    //Vertical mode (only for encoder part)
                    if (mAspectRatio > aspectRatio) {
                        scaleX = aspectRatio / mAspectRatio;
                        scaleY = 1.0f / mAspectRatio;
                        verticalScaleX = Math.min(aspectRatio, 0.5625f) / mAspectRatio;
                        if (VERBOSE) Log.d(TAG, "square vertical normal mode 1");
                    } else {
                        scaleX = aspectRatio;
                        scaleY = 1.0f;
                        verticalScaleX = Math.min(scaleX, 0.5625f);
                        if (VERBOSE) Log.d(TAG, "square vertical normal mode 2");
                    }
                    float scale = Math.min(scaleX, scaleY);
                    if (scale < 1.0f / mAspectRatio) {
                        scale *= mAspectRatio;
                        scaleX *= mAspectRatio;
                        scaleY *= mAspectRatio;
                    }
                    verticalScaleX = scale;
                    verticalScaleY = scale;
                    mClipRect = new Rect(mOutgoingWidth / 2 - (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 - (int) (verticalScaleY * mOutgoingHeight) / 2, mOutgoingWidth / 2 + (int) (verticalScaleX * mOutgoingWidth) / 2, mOutgoingHeight / 2 + (int) (verticalScaleY * mOutgoingHeight) / 2);
                    break;
                case Drawable2d.SCALE_TYPE_FIT:
                    scaleX = 1.0f;
                    scaleY = 1.0f / mAspectRatio;
                    mClipRect = null;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_HEIGHT:
                    scaleX = aspectRatio / mAspectRatio;
                    scaleY = 1.0f / mAspectRatio;
                    mClipRect = null;
                    break;
                case Drawable2d.SCALE_TYPE_FIT_WIDTH:
                    scaleX = 1.0f;
                    scaleY = 1.0f / aspectRatio;
                    mClipRect = null;
                    break;
                default:
                    Log.d(TAG, "vertical video mode... Unknown scale type: " + drawable);
            }
            if (mPreviewWidth > mPreviewHeight) {
                drawable.setScaleType(originalScaleType);
            }
        }
        Log.d(TAG, "DRAWABLE SCALE: " + scaleX + "x" + scaleY);
        Log.d(TAG, "drawable: " + drawable);
        drawable.setScale(scaleX, scaleY, 1.0f);
        if (!drawable.mUsesExternalTexture) {
            drawable.multiplyTranslate(1.0f, 1.0f / mAspectRatio, 1.0f);

            if (VERBOSE) {
                Log.d("SAHIL", "Drawing bitmap drawable");
                Log.d("SAHIL", "scale :: " + drawable.getScale()[0] + " " + drawable.getScale()[1] + " " + drawable.getScale()[2]);
                Log.d("SAHIL", "Translate :: " + drawable.getTranslate()[0] + " " + drawable.getTranslate()[1] + " " + drawable.getTranslate()[2]);
            }
        }
    }

    public void setIsStagnantFrame(boolean isStagnantFrame) {
        if (mIsStagnantFrame != isStagnantFrame) {
            mIsStagnantFrame = isStagnantFrame;
            if (mIsStagnantFrame) {
                mStagnantSettled = false;
                mStagnantCount = 0;
                SBitmap sBitmap = captureScreen(false);

                Drawable2d drawable = new Drawable2d();
                drawable.setIsSBitmapBased(true);
                drawable.setSBitmap(sBitmap);
                drawable.setupDraw();
                drawable.setScale(1.0f, 1.0f / mAspectRatio, 1.0f);

                mRawDrawables.clear();
                mRawDrawables.add(drawable);

                useRawFrame();
            } else {
                unUseRawFrame();
            }
        }
    }

    public void resetCameraParams() {
        mIncomingWidth = -1;
        mIncomingHeight = -1;
    }

    public void resetRenderParams() {
        mOutgoingWidth = -1;
        mOutgoingHeight = -1;
    }

    public void setCameraParams(int width, int height) {
        Log.w(TAG, "Setting camera params to: " + "width: " + width + " height: " + height);
        mIncomingWidth = width;
        mIncomingHeight = height;
    }

    public void setPreviewRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;
        mVerticalVideoMode = mIsVideo && sVerticalModeEnabled && (mPreviewWidth < mPreviewHeight);
    }

    public float[] getCameraLookAt() {
        return center;
    }

    public float[] getCameraPosition() {
        return eye;
    }

    public float[] getViewMatrix() {
        return mViewMatrix;
    }

    public float[] getProjectionMatrix() {
        return mProjectionMatrix;
    }

    public void setRenderParams(int width, int height) {
        mOutgoingWidth = width;
        mOutgoingHeight = height;

        if(mUseFBO) {
            if (mOutgoingWidth < mOutgoingHeight) {
                mFBOHeight = mDefaultFBOHeight;
                mFBOWidth = mDefaultFBOWidth;
            } else {
                mFBOHeight = mDefaultFBOWidth;
                mFBOWidth = mDefaultFBOHeight;
            }
            for (int i = 0; i < BLUR_PASS_COUNT; i++) {
                fboTarget[i].prepareFramebuffer(mFBOWidth, mFBOHeight);
            }
        }

        mAspectRatio = (float) mOutgoingWidth / (float) mOutgoingHeight;

        Log.d(TAG, "setRenderparams width : " + width + "height: " + height + "aspect ratio: " + mAspectRatio);
//        float refDistance = (float) (1.0f / mAspectRatio * Math.tan(((double) mFOV * Math.PI)  / 180.0 / 2.0));
        float refDistance = (float) (1.0f / mAspectRatio / 2.0 / Math.tan(((double) mFOV * Math.PI)  / 360.0));

        mDistance = 1.0f * refDistance;
        if (VERBOSE) Log.d(TAG, "setRenderparams refDistance: " + refDistance);
        eye[2] = mDistance;

        setViewport(mOutgoingWidth, mOutgoingHeight);

        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, eye[0], eye[1], eye[2], center[0], center[1], center[2], up[0], up[1], up[2]);

        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.perspectiveM(mProjectionMatrix, 0, mFOV, mAspectRatio, zNear, zFar);

    }

    public boolean isRenderParamsInitialized() {
        if (mOutgoingWidth <= 0 || mOutgoingHeight <= 0) {
            return false;
        }
        return true;
    }

    void setViewport(int width, int height) {
        Log.d(TAG, "set Viewport " + " width: " + width + " height: " + height);
        mViewportWidth = width;
        mViewportHeight = height;
        GLES20.glViewport(0, 0, mViewportWidth, mViewportHeight);
        mCache.mGlUtil.checkGlError("glViewport");
        onViewportInitialized();
    }

    boolean clear(float left, float top, float right, float bottom, boolean opaque) {
        if (!opaque) {
//s            mCache.enableScissor();
//s            mCache.setScissor((int) left, (int) (mViewportHeight - bottom), (int) (right - left), (int) (bottom - top));
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            return true;
        }

//s        mCache.resetScissor();
        return true;
    }

    /**
     * Transform matrix for normal (Texture2D) textures
     */
    void setTextureTransformMatrix() {
        // TODO
        // sahil.bajaj@instalively.com
        // Doesn't seem likely to be needed anytime in the near future
    }

    public boolean setdraw(Context context,int mFilterTexture,int mCremaTexture,int mCremaGradient){
       // setupDrawWithFilter(getColorFilterMode());
        //mDrawable.setupDrawWithMultiTextureNew();
       // mContext=context;
       // mDrawable.setupDraw();
       // drawMultiTexturewithPngPass(context,mFilterTexture,mCremaTexture,mCremaGradient);
        return true;
    }


    public boolean drawFrame() {

        if(mCaches == null) {
            Log.w(TAG, "mCaches is null");
            return false;
        }
        //Since cache is shared between preview and encoder threads,
        // Use mutex lock to prevent termination from happening during draw
        // Only use synchronisation for Encoder thread. Because anyway, the cache termination happens from GL Render thread.
/*        if (mIsVideo) {*/

        mCache.mLock.lock();
/*        }*/
        if (!mCache.isInitialized()) {
            Log.e(TAG, "OpenGL setup not ready");
/*            if (mIsVideo) {*/
            mCache.mLock.unlock();
/*            }*/
            return false;
        }
        mCache.setGlErrorFlag(true);

            /*
            * Check gl Error only at beginning of drawFrame in release builds
            * If glError is encountered, configure to enable querying of gl errors at every step of the draw
             */
        boolean error = mCache.mGlErrorUtil.checkGlError("start drawFrame");
/*        if (error) {
            mCache.setGlErrorFlag(true);
        }*/

        if (mIsStagnantFrame) {
            if (!mStagnantSettled) {
                ++mStagnantCount;
                if (mStagnantCount >= 5) {
                    mStagnantCount = 0;
//                    mStagnantSettled = true;
                }
            } else {
/*                if (mIsVideo) {*/
                mCache.mLock.unlock();
/*                }*/
//                return true;
            }
        }



        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Iterator<Integer> iterator = mDrawables.navigableKeySet().iterator();

        boolean mAnimDrawn = false;

        boolean fboDrawable = false;
        int convolutionType =  Drawable2d.CONVOLUTION_TYPE_NONE;
        while ((iterator.hasNext())) {
            int key = iterator.next();
            ArrayList<Drawable2d> drawableList = mDrawables.get(key);
            if(drawableList == null) continue;
            for (Drawable2d drawable : drawableList) {
                mDrawable = drawable;
                mDescription = drawable.getDescription();
                mDescription.mOffscreenSurface = mIsOffscreen;

                if(drawable.getConvolutionType() > 0 && mUseFBO) {
                    fboDrawable = true;
                    drawable.setIsFBOBased(false);
                    int j = 0;
                    convolutionType = drawable.getConvolutionType();

                    for(j = 0; j < BLUR_PASS_COUNT; j++) {
                        if(j > 0) {
                            drawable.setConvolutionType((j%2 == 0) ? Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL : Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL);
//                            drawable.setConvolutionType(Drawable2d.CONVOLUTION_TYPE_BLUR);
                            drawable.mUsesExternalTexture = false;
                            drawable.setIncomingSize(fboTarget[j-1].getWidth(), fboTarget[j-1].getHeight());
                            drawable.setIsFBOBased(true);
                            drawable.setTextureIndex(j-1);
                        } else {
                            drawable.setConvolutionType(Drawable2d.CONVOLUTION_TYPE_NONE);
                        }
                        drawable.setupDraw();
                        mDescription = drawable.getDescription();
                        mDescription.mOffscreenSurface = mIsOffscreen;
                        fboTarget[j].startDraw();
                        draw(drawable);
                        fboTarget[j].finishDraw();
                    }
                    if((j > 0) && (j == BLUR_PASS_COUNT)) {
                        drawable.setConvolutionType(Drawable2d.CONVOLUTION_TYPE_NONE);
                        drawable.setIsFBOBased(true);
                        drawable.setTextureIndex(j-1);
                        drawable.mUsesExternalTexture = false;
                        setViewport(mOutgoingWidth, mOutgoingHeight);
                        drawable.setupDraw();
                        mDescription = drawable.getDescription();
                        mDescription.mOffscreenSurface = mIsOffscreen;
                    }
                }
                draw(drawable);
                if(fboDrawable) {
                    GLES20.glFinish();
                    drawable.setConvolutionType(Drawable2d.CONVOLUTION_TYPE_BLUR);
                    drawable.setIncomingSize(incomingWidth, incomingHeight);
                    drawable.mUsesExternalTexture = true;
                    drawable.setIsFBOBased(false);
                    drawable.setConvolutionType(convolutionType);
                    drawable.setupDraw();
                    fboDrawable = false;
                }
            }
            if(key >= DRAWABLES_DEFAULT && !mAnimDrawn && (mAnimatableDrawables != null)) {
                for (Drawable2d drawable : mAnimatableDrawables) {
                    mDrawable = drawable;
                    mDescription = drawable.getDescription();
                    mDescription.mOffscreenSurface = mIsOffscreen;
                    draw(drawable);
                }
                mAnimDrawn = true;
            }
        }
        if(!mAnimDrawn && (mAnimatableDrawables != null)) {
            for (Drawable2d drawable : mAnimatableDrawables) {
                mDrawable = drawable;
                mDescription = drawable.getDescription();
                mDescription.mOffscreenSurface = mIsOffscreen;
               // if(mContext!=null)
                draw(drawable);
            }
            mAnimDrawn = true;
        }

/*        if (mIsVideo) {*/
        mCache.mLock.unlock();
/*        }*/

//        GLES20.glFinish();
        return true;
    }

    private void readPixels(FBO fboTarget) {
        IntBuffer pixelBuf = IntBuffer.allocate(fboTarget.getWidth() * fboTarget.getHeight());
        GLES20.glFinish();
        GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

        GLES20.glReadPixels(0, 0, fboTarget.getWidth(), fboTarget.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);
        pixelBuf.position(0);

        Log.d(TAG, "FBO pixels:");
        for(int i = 10; i < 20; i++) {
            String string = "";
            for(int j = 10; j < 20; j++) {
                string += pixelBuf.get(i*mOutgoingWidth + j);
            }
            Log.d(TAG, string);
        }
    }



    public void setFilterBitmapRenderer(HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream){
        mFilterBitmapHashMap=filterBitmapHashMap;
        this.inputStream=inputStream;
    }


    void draw(Drawable2d drawable) {
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            if (drawable.getIsFBOBased()) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                drawWithFBOTexture(fboTarget[drawable.getTextureIndex()].getTextureId());
            } else {
                if (drawable.mUsesExternalTexture) {
                    drawWithExternalTexture(mFilterBitmapHashMap,inputStream);
                } else if (drawable.getIsSBitmapBased()) {
                    drawSBitmap(drawable.getSBitmap(),mFilterBitmapHashMap,inputStream);
                } else if (drawable.getIsColorBased()) {
                    drawColorRect(drawable.getPaint());
                } else {
                    if (drawable.getIsBitmapBased()) {
                        if (drawable.getBlendMode() == Drawable2d.BLEND_MODE_DISSOLVE) {
                            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        } else {
                            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        }
                        drawBitmap(drawable.getBitmap(), mFilterBitmapHashMap,inputStream);
                    } else if (drawable.getIsMultiBitmapBased()) {
                        drawBitmaps(drawable.getBitmap(), drawable.getBitmap2());
                    }
                }
            }
    }

    void setupDraw() {
        setupDraw(true);
    }

    void setupDraw(boolean clearLayer) {
        // Make sure setScissor & setStencil happen at the beginning of
        // this method
/*        if (mDirtyClip) {
            if (mCache.scissorEnabled) {
            //    setScissorFromClip();
            }
        }*/

//        mDescription.reset();

/*        mSetShaderColor = false;
        mColorSet = false;
        mColorA = mColorR = mColorG = mColorB = 0.0f;
        mTrackDirtyRegions = true;*/

        mTextureUnit = 0;
    }

    private void setScissorFromClip() {
        Rect clip = new Rect(mClipRect);
        if (mCache.setScissor(clip.left, mViewportHeight - clip.bottom,
                clip.width(), clip.height())) {
            mDirtyClip = false;
        }
    }

    void setupDrawDirtyRegionsDisabled() {
        mTrackDirtyRegions = false;
    }

    void setupDrawColorFilterUniforms() {

        int slot = 0;

        mNewFilter = mDrawable.getColorFilterMode();
        if (mCurrentFilter != mNewFilter) {
//            Log.d(TAG, "Filter mode set is " + mNewFilter);
            mCurrentFilter = mNewFilter;
            if (mCurrentFilter > 0) {
                for (int i = 0; i < FILTER_COUNT; i++) {
                    int filterMode = 1 << i;
                    slot = currentProgram.getUniform(Caches.FilterModeUniform[i]);
                    if (slot < 0 && i != FilterManager.FILTER_NATURAL) {
                        //There are no uniforms for natural mode (Why would we need One! :D)
                        Log.e(TAG, "getUniform FilterMode returns invalid location for " + Caches.FilterModeUniform[i]);
                    } else {
                        if (VERBOSE)
                            Log.d(TAG, "getUniform FilterMode returns nice location for " + Caches.FilterModeUniform[i]);
                    }
                    GLES20.glUniform1i(slot, ((mCurrentFilter & filterMode) > 0) ? 1 : 0);
                }
            }
        }
    }

    void setupDrawModelView(float[] translate, float[] scale, float [] rotate) {

        int slot = currentProgram.getUniform("uMVPMatrix");
        if (slot < 0) {
            Log.e(TAG, "getUniform uMVPMatrix returns invalid location");
        }

        Matrix.setIdentityM(mModelMatrix, 0);

        if (translate != null) {
            Matrix.translateM(mModelMatrix, 0, translate[0], translate[1], translate[2]);
        }

/*        if(rotate != null) {
            Matrix.rotateM(mModelMatrix, 0, rotate[0], 1.0f, 0.0f, 0.0f);
            Matrix.rotateM(mModelMatrix, 0, rotate[1], 0.0f, 1.0f, 0.0f);
            Matrix.rotateM(mModelMatrix, 0, rotate[2], 0.0f, 0.0f, 1.0f);
        }*/

        if (scale != null) {
            Matrix.scaleM(mModelMatrix, 0, scale[0], scale[1], scale[2]);
            Matrix.scaleM(mModelMatrix, 0, sScale, sScale, 1.0f);
        }

        Matrix.setIdentityM(mModelViewMatrix, 0);
        Matrix.multiplyMM(mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mModelViewMatrix, 0);

        GLES20.glUniformMatrix4fv(slot, 1, false, mMVPMatrix, 0);
        mCache.mGlUtil.checkGlError("glUniformMatrix4fv");
    }

    void setupDrawModelView(ModelViewMode mode, boolean offset, float left, float top, float right, float bottom, boolean ignoreTransform) {
        Matrix.setIdentityM(mModelViewMatrix, 0);
        Matrix.translateM(mModelViewMatrix, 0, left, top, 0.0f);
        if (mode == ModelViewMode.kModelViewMode_TranslateAndScale) {
            Matrix.scaleM(mModelViewMatrix, 0, right - left, bottom - top, 1.0f);
        }

        boolean dirty = right - left > 0.0f && bottom - top > 0.0f;
        if (ignoreTransform) {
            Matrix.setIdentityM(mModelViewMatrix, 0);
        }

/*        if (dirty && mTrackDirtyRegions) {
//TODO
//Sahil
            if (!ignoreTransform) {
                dirtyLayer(left, top, right, bottom, currentTransform());
            } else {
                dirtyLayer(left, top, right, bottom);
            }
        }*/
    }

    void bindTexture(int texture) {
        mCache.bindTexture(texture);
    }

    void bindTexture(int texture, int textureUnit) {
        mCache.bindTexture(GLES20.GL_TEXTURE_2D, texture, textureUnit);
    }

    void bindExternalTexture(int texture) {
        mCache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
    }

    void setupDrawTexture(int texture) {
        if (texture != 0) {
            bindTexture(texture);
        } else {
            Log.e(TAG, "Attempt to bind null Texture");
        }
        mTextureUnit++;
//s        mCache.enableTexCoordsVertexArray();
    }

    void setupDrawTexture(int texture, int textureUnit) {

        if (texture != 0) {
            bindTexture(texture, textureUnit);
        } else {
            Log.e(TAG, "Attempt to bind null Texture");
        }
        mTextureUnit++;
//s        mCache.enableTexCoordsVertexArray();
    }

    void setupDrawExternalTexture(int texture) {
        bindExternalTexture(texture);
        mTextureUnit++;
//s        mCache.enableTexCoordsVertexArray();
    }

    public void setBlendFactor(float blendFactor) {
//        Log.d(TAG, "blend factor is " + blendFactor);
        mBlendFactor = blendFactor;
        Drawable2d drawable0 = peekDrawable(DRAWABLES_UNDERLAY, 1);
        Drawable2d drawable1 = peekDrawable(DRAWABLES_UNDERLAY, 0);

        if(drawable0 != null) {
            drawable0.setAlpha(1.0f - mBlendFactor);
        }
        if(drawable1 != null) {
            drawable1.setAlpha(mBlendFactor);
        }
    }

    public void resetBlendFactor() {
        maxReached = !maxReached;
/*        if(maxReached)
            mBlendFactor = 1.0f;
        else*/
        mBlendFactor = 0.0f;
    }

    void setupDrawTextureUniforms() {
/*        if(!maxReached) {*/
//            mBlendFactor += 0.02f;
        if(mBlendFactor > 0.97f) {
            mBlendFactor = 1.0f;
        }
/*        } else {
            mBlendFactor -= 0.04f;
            if(mBlendFactor < 0.04) {
                mBlendFactor = 0.0f;
            }
        }*/
//        Log.d(TAG, "blend factor is " + mBlendFactor);

        int slot = currentProgram.getUniform("blendFactor");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: blendFactor returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: blendFactor returns nice location");
        }
        GLES20.glUniform1f(slot, mBlendFactor);
    }

    void setupColorBlends(float blendFactor) {
        int slot = currentProgram.getUniform("blendFactor");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: blendFactor returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: blendFactor returns nice location");
        }
        GLES20.glUniform1f(slot, blendFactor);
    }

    private void setupTexelSizeUniform() {
        int texelSizeUniformLocation = currentProgram.getUniform("texelSize");

        if (texelSizeUniformLocation < 0) {
            Log.e(TAG, "Unable to find uniform texelSize");
        } else {
//                Log.d(TAG, "Found uniform texelSize");
            GLES20.glUniform2fv(texelSizeUniformLocation, 1, new float[]{1.0f / mIncomingWidth, 1.0f / mIncomingHeight}, 0);
        }
    }

    private void setupTexelSizeUniform(float incomingWidth, float incomingHeight) {
        int texelSizeUniformLocation = currentProgram.getUniform("texelSize");

        if (texelSizeUniformLocation < 0) {
            Log.e(TAG, "Unable to find uniform texelSize");
        } else {
//                Log.d(TAG, "Found uniform texelSize");
            GLES20.glUniform2fv(texelSizeUniformLocation, 1, new float[]{1.0f / incomingWidth, 1.0f / incomingHeight}, 0);
        }
    }

    void setupDrawTextureTransformUniforms() {
        int slot = currentProgram.getUniform("mainTextureTransform");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: mainTextureTransform returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: mainTextureTransform returns nice location");
        }

// Uncomment this if the App crashes just below.
        if(mCache.mSurfaceTextureTransform == null) {
            float [] dummy = new float[16];
            Matrix.setIdentityM(dummy, 0);
            mCache.mSurfaceTextureTransform = dummy;
        }

        if(mCache.mSurfaceTextureTransform == null) {
            Log.w(TAG, "SurfaceTexture not configured yet");
            return;
        }
        GLES20.glUniformMatrix4fv(slot, 1, false, mCache.mSurfaceTextureTransform, 0);
        mCache.mGlUtil.checkGlError("glUniformMatrix4fv 1");
    }

    public Texture getTexture(boolean isExternalTexture) {
        return mCache.mTextureCache.get(isExternalTexture);
    }

    public Texture getTexture(Buffer buffer) {
        return mCache.mTextureCache.get(buffer);
    }

    public Texture getTexture(InputStream inputStream){
        return mCache.mTextureCache.get(inputStream);
    }

    Texture getTexture(SBitmap sBitmap) {
        return mCache.mTextureCache.get(sBitmap);
    }

    Texture getTexture(Bitmap bitmap) {
        return mCache.mTextureCache.get(bitmap);
    }

    Texture getTexture(int textureId) {
        return mCache.mTextureCache.get(textureId);
    }

    void setupTexture(Texture texture, int textureUnit) {
//        Log.d(TAG, "Setting up texture for texture unit " + textureUnit);
        mCache.mTextureCache.activeTexture(textureUnit);
        if(texture != null) {
            if (texture.isExternalTexture) {
                setupDrawExternalTexture(texture.id);
            } else {
                setupDrawTexture(texture.id);
            }
        }
        texture.setWrap(GLES20.GL_CLAMP_TO_EDGE, true);

/*        if(!texture.isExternalTexture) {
            texture.mipMap = true;
        } else {
            texture.mipMap = false;
        }*/
        texture.setFilter(GLES20.GL_LINEAR, true);
    }

    boolean drawColorRect(Paint paint) {
        if(VERBOSE) {
            Log.d(TAG, "drawColorRect");
        }
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
        int setup = setupDrawProgram();
        if(setup < 0) return false;

        setupDrawModelView(mDrawable.getTranslate(), mDrawable.getScale(), mDrawable.getRotate());
        //        Log.d(TAG, "Setting up alpha as " + mDrawable.getAlpha());
        setupColorBlends(mDrawable.getAlpha());

        setupDrawMeshIndices();

        mCache.mGlUtil.checkGlError("drawTextureMesh :: Before glDrawElements");
        if (VERBOSE) Log.d(TAG, "going to call glDrawElements for color rect");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Caches.indexData.length, GLES20.GL_UNSIGNED_BYTE, 0);

        mCache.mGlUtil.checkGlError("drawTextureMesh :: glDrawElements");
        //GLES20.glFinish();
        mCache.unbindMeshBuffer();
        mCache.unbindIndicesBuffer();
        mCache.unbindColorsBuffer();

        return true;
    }

    void drawTextureRect(Texture texture,Texture texture1,Texture texture2, Paint paint) {
        setupTexture(texture, 0);
        if(texture1!=null&&texture2!=null&&mDrawable.getColorFilterMode()!=10) {
            setupTexture(texture1, 6);
            setupTexture(texture2, 7);
        }
        else if(texture1!=null && texture2!=null&&mDrawable.getColorFilterMode()==10){
            Log.i(TAG,"in nashville in setuptexture passing bitmap 3 and 4");
            setupTexture(texture1, 8);
            setupTexture(texture2, 9);
        }
       // drawTextureMesh(texture,texture1,texture2, texture.blend, true);
    }

    void drawTextureRect(Texture texture1, Texture texture2, Paint paint) {
        setupTexture(texture1, 0);
        setupTexture(texture2, 1);
        drawTextureMesh(texture1, texture2, texture1.blend, true);
    }

    void drawTextureRect(Texture texture,HashMap<Integer,Texture> textures){
        setupTexture(texture, 0);
        for(int i=0;i<textures.size();i++) {
            if(textures.get(i)!=null) {
                if (mDrawable.getColorFilterMode() == 7){
                    setupTexture(textures.get(0), 1);
                } else if(mDrawable.getColorFilterMode() == 8 ){
                    setupTexture(textures.get(1), 2);
                } else if(mDrawable.getColorFilterMode() == 9){
                    setupTexture(textures.get(2), 3);
                    setupTexture(textures.get(3), 4);
                } else if(mDrawable.getColorFilterMode() == 10){
                    setupTexture(textures.get(2), 3);
                    setupTexture(textures.get(4), 5);
                } else if(mDrawable.getColorFilterMode() == 11){
                    setupTexture(textures.get(5), 6);
                    setupTexture(textures.get(6), 7);
                }  else if(mDrawable.getColorFilterMode() == 14){
                    setupTexture(textures.get(7), 8);
                } else if(mDrawable.getColorFilterMode() == 15){
                    setupTexture(textures.get(8), 9);
                    setupTexture(textures.get(9), 10);
                    setupTexture(textures.get(10), 11);
                    setupTexture(textures.get(11), 12);
                    setupTexture(textures.get(12), 13);
                } else if(mDrawable.getColorFilterMode() == 16){
                    setupTexture(textures.get(13), 14);
                    setupTexture(textures.get(14), 15);
                    setupTexture(textures.get(15), 16);
                    setupTexture(textures.get(16), 17);
                    setupTexture(textures.get(17), 18);
                } else if(mDrawable.getColorFilterMode() == 17){
                    setupTexture(textures.get(18), 19);
                    setupTexture(textures.get(19), 20);
                }
            }
        }
        drawTextureMesh(texture,textures, texture.blend, true);

    }

/*    void setupDrawMesh(final FloatBuffer vertices, final FloatBuffer indices, final FloatBuffer texCoords) {
        if (vertices != null) {
            mCache.bindMeshBuffer(vertices);
        } else {
            mCache.unbindMeshBuffer();
        }

        mCache.bindPositionVertexPointer(vertices);

        if (mCache.currentProgram.texCoords >= 0) {
            mCache.bindTexCoordsVertexPointer(texCoords);
        }

        mCache.unbindIndicesBuffer();
    }*/

/*    void setupDrawMesh(final float[] vertices, final float[] texCoords, final float[] colors) {
        boolean force = mCache.unbindMeshBuffer();
        int stride = sizeof(ColorTextureVertex);

        mCache.bindPositionVertexPointer(force, vertices, stride);
        if (mCache.currentProgram.texCoords >= 0) {
            mCache.bindTexCoordsVertexPointer(force, texCoords, stride);
        }
        int slot = mCache.currentProgram.getAttrib("colors");
        if (slot >= 0) {
            GLES20.glEnableVertexAttribArray(slot);

            //TODO
            //sahil.bajaj@instalively.com
            //Create FloatBuffer instead of float[]
            //Actually not sure yet. So let it be.
            //        GLES20.glVertexAttribPointer(slot, 4, GLES20.GL_FLOAT, false, stride, colors);
        }

        mCache.unbindIndicesBuffer();
    }*/

    ///////////////////////////////////////////////////////////////////////////////
    // Meshes and textures
    ///////////////////////////////////////////////////////////////////////////////

    public void bindPositionVertexPointer() {
        int slot = currentProgram.getAttrib("position");
        if (slot < 0) {
            Log.e(TAG, "slot returns invalid value bindPositionVertexPointer for position");
        }
        GLES20.glEnableVertexAttribArray(slot);
        GLES20.glVertexAttribPointer(slot, 3, GLES20.GL_FLOAT, true, 0, 0);
        mCache.mGlUtil.checkGlError("bindPositionVertexPointer");
//        mCurrentPositionPointer = vertexDataBuffer;
    }

    public void bindTexCoordsVertexPointer(int position) {
        int slot = currentProgram.getAttrib("texCoords");

        GLES20.glVertexAttribPointer(slot, 2, GLES20.GL_FLOAT, false, 0, 8 * BYTES_PER_FLOAT * position);
        //Assuming it's a quad and 2 texture coordinates (x,y) are given for each vertex

        if (slot < 0) {
            Log.d(TAG, "slot returns invalid value bindTexCoordsVertexPointer for texCoords");
        }
        mCache.mGlUtil.checkGlError("bindTexCoordsPointer");
        GLES20.glEnableVertexAttribArray(slot);
//        mCurrentTexCoordsPointer = textureDataBuffer;
    }

    public void bindColorsVertexPointer(int position) {
        int slot = currentProgram.getAttrib("color");

        // position is to be used when we've multiple sets of colors packaged in a single array
        // Right now, it'll be 0, because we've only one set of colors unlike the case with texture coordinates
        GLES20.glVertexAttribPointer(slot, 4, GLES20.GL_FLOAT, false, 0, 16 * BYTES_PER_FLOAT * position);
        //Assuming it's a quad and 2 texture coordinates (x,y) are given for each vertex

        if (slot < 0) {
            Log.d(TAG, "slot returns invalid value bindColorsVertexPointer for color");
        }
        mCache.mGlUtil.checkGlError("bindColorsPointer");
        GLES20.glEnableVertexAttribArray(slot);
//        mCurrentTexCoordsPointer = textureDataBuffer;
    }

    void setupDrawMeshIndices() {
        mCache.bindMeshBuffer();
        bindPositionVertexPointer();

        int position = 0;
        if (mDrawable.getIsBitmapBased() || mDrawable.getIsMultiBitmapBased() || mIsOffscreen) {
            position = 2;
        } else {
            if ((mDrawable.mUsesExternalTexture || mDrawable.getIsFBOBased())) {
                if (mDrawable.getIsFlipped()) {
                    position = 1;
                } else {
                    position = 0;
                }
            } else {
                position = 0;
            }
        }
        if (mDrawable.getIsBitmapBased() || mDrawable.getIsMultiBitmapBased() || mDrawable.getIsSBitmapBased() || mDrawable.mUsesExternalTexture || mDrawable.getIsFBOBased()) {
            mCache.bindTextureBuffer();
            bindTexCoordsVertexPointer(position);
        }

        mCache.bindIndicesBuffer();

        if(mDrawable.getIsColorBased()) {
            mCache.bindColorsBuffer();
            bindColorsVertexPointer(mDrawable.getColorIndex());
        }
    }

/*    void setupDrawMeshIndices(final int vertices, final int texCoords, final int indices) {
        mCache.bindMeshBuffer(vertices);
        mCache.bindIndicesBuffer(indices);
        mCache.bindTextureBuffer(texCoords);

        mCache.bindPositionVertexPointer();
//        if (mCache.currentProgram.texCoords >= 0) {
            mCache.bindTexCoordsVertexPointer();
//        }
    }*/

    void setupDrawIndexedVertices(float[] vertices) {
        boolean force = mCache.unbindMeshBuffer();
        mCache.bindIndicesBuffer();
        bindPositionVertexPointer();
    }


    void drawTextureMesh(Texture texture,HashMap<Integer,Texture> textures, boolean blend, boolean dirty) {
        if(VERBOSE) {
            Log.d(TAG, "drawTextureMesh");
        }

        //Log.i(TAG,"texture "+texture1 +texture2);
//TODO
// sahil.bajaj@instalively.com
// we don't support (glsl based i.e. in shader blending) blending as of now
//      setupDrawBlending(paint, blend, swapSrcDst);

        // Having setup all the parameters which are needed to created a shader
        // Go on to create one
        int setup = setupDrawProgram();
        if(setup < 0) {
            Log.w(TAG, "drawTextureMesh :: setup program failed");
            return;
        }
        int CurrentFilter=mDrawable.getColorFilterMode();
        if (mDrawable.getColorFilterMode() == 12||mDrawable.getColorFilterMode()==13) {
            setupTexelSizeUniform(mDrawable.getIncomingWidth(), mDrawable.getIncomingHeight());
        }

        switch (mDrawable.getConvolutionType()) {
            case Drawable2d.CONVOLUTION_TYPE_NONE:
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR:
                setupTexelSizeUniform(mDrawable.getIncomingWidth(), mDrawable.getIncomingHeight());

                mCache.mTextureCache.activeTexture(1);
                Texture blurKernel = getTexture(mCache.blurKernel);
                int slot = currentProgram.getUniform("blurCoeffSampler");

                if (slot < 0) {
                    Log.e(TAG, "getUniform :: blurCoeffSampler returns invalid location 2");
                } else {
                    if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler returns nice location");
                }

                GLES20.glUniform1i(slot, 1);

                setupDrawTexture(blurKernel.id, 1);
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL:
                setupTexelSizeUniform(mDrawable.getIncomingWidth(), mDrawable.getIncomingHeight() * 1000);
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL:
                setupTexelSizeUniform(mDrawable.getIncomingWidth() * 1000, mDrawable.getIncomingHeight());
                break;
            default:
                Log.w(TAG, "Unsupported convolution type");
        }

        if (!dirty) setupDrawDirtyRegionsDisabled();

//        handleFlipAnimation();

        setupDrawModelView(mDrawable.getTranslate(), mDrawable.getScale(), mDrawable.getRotate());

        int slot = currentProgram.getUniform("baseSampler");

        if (slot < 0) {
            Log.e(TAG, "getUniform :: baseSampler returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler returns nice location");
        }

        GLES20.glUniform1i(slot, 0);

        mCache.mTextureCache.activeTexture(0);

        if(texture != null) {
            if (texture.isExternalTexture) {
                setupDrawExternalTexture(texture.id);
            } else {
                setupDrawTexture(texture.id);
            }

            if (texture.isExternalTexture) {
                setupDrawTextureTransformUniforms();
            }
        }

        drawMultiTextureMesh(textures);

        setupDrawMeshIndices();

//s        setupDrawColorFilterUniforms();
        mCache.mGlUtil.checkGlError("drawTextureMesh :: Before glDrawElements");
        if (VERBOSE) Log.d(TAG, "going to call glDrawElements");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Caches.indexData.length, GLES20.GL_UNSIGNED_BYTE, 0);

        mCache.mGlUtil.checkGlError("drawTextureMesh :: glDrawElements");
        //GLES20.glFinish();
        mCache.unbindMeshBuffer();
        mCache.unbindIndicesBuffer();
//        GLES20.glUseProgram(0);
//s        GLES20.glDisableVertexAttribArray(0);

        //S    mCache.unbindTextureBuffer();
    }


    void drawMultiTextureMesh(HashMap<Integer,Texture> textures){
        int CurrentFilter=mDrawable.getColorFilterMode();
        for(int i=0;i<textures.size();i++) {
            if (textures.get(i) != null) {
                if((CurrentFilter>=7) && (CurrentFilter<=11) ||(CurrentFilter==14) || (CurrentFilter==15) || (CurrentFilter==16) || (CurrentFilter==17)) {
                    int slot = currentProgram.getUniform("baseSampler3");
                    if (slot < 0) {
                        Log.e(TAG, "getUniform ::baseSampler3 returns invalid location" + CurrentFilter);
                    } else {
                        if (VERBOSE)
                            Log.d(TAG, "getUniform :: baseSampler3 returns nice location" + CurrentFilter);
                    }
                    if (CurrentFilter == 7) {
                        GLES20.glUniform1i(slot, 1);
                        mCache.mTextureCache.activeTexture(1);
                       setupDrawTexture(textures.get(0).id);
                    }

                    if (CurrentFilter == 8) {
                        GLES20.glUniform1i(slot, 2);
                        mCache.mTextureCache.activeTexture(2);
                        setupDrawTexture(textures.get(1).id);
                    }

                    if (CurrentFilter==9)  {
                        GLES20.glUniform1i(slot,3);
                        mCache.mTextureCache.activeTexture(3);
                        setupDrawTexture(textures.get(2).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 4);
                        mCache.mTextureCache.activeTexture(4);
                        setupDrawTexture(textures.get(3).id);
                    }

                    if(CurrentFilter == 10){
                        GLES20.glUniform1i(slot, 3);
                        mCache.mTextureCache.activeTexture(3);
                        setupDrawTexture(textures.get(2).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 5);
                        mCache.mTextureCache.activeTexture(5);
                        setupDrawTexture(textures.get(4).id);
                    }

                    if (CurrentFilter == 11) {
                        GLES20.glUniform1i(slot, 6);
                        mCache.mTextureCache.activeTexture(6);
                        setupDrawTexture(textures.get(5).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 7);
                        mCache.mTextureCache.activeTexture(7);
                        setupDrawTexture(textures.get(6).id);
                    }

                    if (CurrentFilter == 14) {
                        GLES20.glUniform1i(slot, 8);
                        mCache.mTextureCache.activeTexture(8);
                        setupDrawTexture(textures.get(7).id);
                    }
                    if (CurrentFilter == 15) {
                        GLES20.glUniform1i(slot, 9);
                        mCache.mTextureCache.activeTexture(9);
                        setupDrawTexture(textures.get(8).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 10);
                        mCache.mTextureCache.activeTexture(10);
                        setupDrawTexture(textures.get(9).id);

                        slot = currentProgram.getUniform("baseSampler5");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler5 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler5 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 11);
                        mCache.mTextureCache.activeTexture(11);
                        setupDrawTexture(textures.get(10).id);

                        slot = currentProgram.getUniform("baseSampler6");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler6 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler6 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 12);
                        mCache.mTextureCache.activeTexture(12);
                        setupDrawTexture(textures.get(11).id);

                        slot = currentProgram.getUniform("baseSampler7");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler7 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler7 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 13);
                        mCache.mTextureCache.activeTexture(13);
                        setupDrawTexture(textures.get(12).id);
                    }


                    if (CurrentFilter == 16) {
                        GLES20.glUniform1i(slot, 14);
                        mCache.mTextureCache.activeTexture(14);
                        setupDrawTexture(textures.get(13).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 15);
                        mCache.mTextureCache.activeTexture(15);
                        setupDrawTexture(textures.get(14).id);

                        slot = currentProgram.getUniform("baseSampler5");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler5 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler5 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 16);
                        mCache.mTextureCache.activeTexture(16);
                        setupDrawTexture(textures.get(15).id);

                        slot = currentProgram.getUniform("baseSampler6");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler6 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler6 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 17);
                        mCache.mTextureCache.activeTexture(17);
                        setupDrawTexture(textures.get(16).id);

                        slot = currentProgram.getUniform("baseSampler7");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler7 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler7 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 18);
                        mCache.mTextureCache.activeTexture(18);
                        setupDrawTexture(textures.get(17).id);
                    }

                    if (CurrentFilter==17)  {
                        GLES20.glUniform1i(slot,19);
                        mCache.mTextureCache.activeTexture(19);
                        setupDrawTexture(textures.get(18).id);

                        slot = currentProgram.getUniform("baseSampler4");
                        if (slot < 0) {
                            Log.e(TAG, "getUniform ::baseSampler4 returns invalid location"+CurrentFilter);
                        } else {
                            if (VERBOSE)
                                Log.d(TAG, "getUniform :: baseSampler4 returns nice location"+CurrentFilter);
                        }
                        GLES20.glUniform1i(slot, 20);
                        mCache.mTextureCache.activeTexture(20);
                        setupDrawTexture(textures.get(19).id);
                    }

                }

            }
        }

    }

    void drawTextureMesh(Texture texture1, Texture texture2, boolean blend, boolean dirty) {

//TODO
// sahil.bajaj@instalively.com
// we don't support (glsl based i.e. in shader blending) blending as of now
//      setupDrawBlending(paint, blend, swapSrcDst);

        // Having setup all the parameters which are needed to created a shader
        // Go on to create one
        int setup = setupDrawProgram();
        if(setup < 0) return;

        if (!dirty) setupDrawDirtyRegionsDisabled();

        setupDrawModelView(mDrawable.getTranslate(), mDrawable.getScale(), mDrawable.getRotate());

        int slot = currentProgram.getUniform("baseSampler");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: baseSampler returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler returns nice location");
        }
        GLES20.glUniform1i(slot, 0);

        mCache.mTextureCache.activeTexture(0);

        setupDrawTexture(texture1.id, 0);

        slot = currentProgram.getUniform("baseSampler2");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: baseSampler2 returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler2 returns nice location");
        }
        GLES20.glUniform1i(slot, 1);

        mCache.mTextureCache.activeTexture(1);

        setupDrawTexture(texture2.id, 1);

        // Restore active texture back to 0
        // sahil.bajaj@instalively.com
        // Don't know why. Lets see if 2 textures at a time start working becoz of this.
        mCache.mTextureCache.activeTexture(0);

        setupDrawTextureUniforms();

        setupDrawMeshIndices();

//s        setupDrawColorFilterUniforms();
        mCache.mGlUtil.checkGlError("drawTextureMesh :: Before glDrawElements");
        if (VERBOSE) Log.d(TAG, "going to call glDrawElements");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Caches.indexData.length, GLES20.GL_UNSIGNED_BYTE, 0);

        mCache.mGlUtil.checkGlError("drawTextureMesh :: glDrawElements");
        //GLES20.glFinish();
        mCache.unbindMeshBuffer();
        mCache.unbindIndicesBuffer();
//        GLES20.glUseProgram(0);
//s        GLES20.glDisableVertexAttribArray(0);

        //S    mCache.unbindTextureBuffer();
    }

    void drawAlpha8TextureMesh(float left, float top, float right, float bottom,
                               int texture, final Paint paint,
                               float[] vertices, float[] texCoords, int drawMode, int elementsCount,
                               boolean ignoreTransform, ModelViewMode modelViewMode, boolean dirty) {

/*        int color = paint != null ? paint.getColor() : 0;
        int alpha;
        SkXfermode::Mode mode;
        getAlphaAndMode(paint, &alpha, &mode);*/

/*        setupDraw();
        setupDrawWithTexture(true);*/
    /*    if (paint != null) {
            setupDrawAlpha8Color(color, alpha);
        }*/
        //    setupDrawColorFilter(getColorFilter(paint));

//TODO
//Sahil
//No need for blending as of now
//        setupDrawBlending(paint, true);
        setupDrawProgram();
        if (!dirty) setupDrawDirtyRegionsDisabled();
//        setupDrawModelView(modelViewMode, false, left, top, right, bottom, ignoreTransform);
        setupDrawTexture(texture);
/*        setupDrawPureColorUniforms();
        setupDrawColorFilterUniforms(getColorFilter(paint));
        setupDrawShaderUniforms(getShader(paint), ignoreTransform);*/
//        setupDrawMesh(vertices, texCoords);

        GLES20.glDrawArrays(drawMode, 0, elementsCount);
    }

    int setupDrawProgram() {
        Program program = mCache.mProgramCache.get(mDescription, mRenderTargetType);
        if(program == null) {
            return -1;
        }
        useProgram(program);

        return 0;
    }

    void drawAlphaBitmap(Texture texture, float left, float top, Paint paint) {
//        int color = paint != null ? paint.getColor() : 0;

        float x = left;
        float y = top;

        texture.setWrap(GLES20.GL_CLAMP_TO_EDGE, true);

/*        bool ignoreTransform = false;
        if (currentTransform().isPureTranslate()) {
            x = (int) floorf(left + currentTransform().getTranslateX() + 0.5f);
            y = (int) floorf(top + currentTransform().getTranslateY() + 0.5f);
            ignoreTransform = true;

            texture.setFilter(GLES20.GL_NEAREST, true);
        } else {
            texture.setFilter(getFilter(paint), true);
        }*/

        // No need to check for a UV mapper on the texture object, only ARGB_8888
        // bitmaps get packed in the atlas
/*        drawAlpha8TextureMesh(x, y, x + texture.width, y + texture.height, texture.id,
                paint, null, gMeshTextureOffset,
                GLES20.GL_TRIANGLE_STRIP, gMeshCount, ignoreTransform);*/
    }

    boolean useProgram(Program program) {
//        if (!program.isInUse()) {
//            if (mCache.currentProgram != null) mCache.currentProgram.remove();
        program.use();
        currentProgram = program;
//            return false;
//        }
        return true;
    }

    /**
     * Returns false and sets scissor enable based upon bounds if drawing won't be clipped out.
     */
/*    boolean quickRejectSetupScissor(float left, float top, float right, float bottom) {

        boolean clipRequired = false;
        boolean roundRectClipRequired = false;
        if (calculateQuickRejectForScissor(left, top, right, bottom,clipRequired, roundRectClipRequired)) {
            return true;
        }

        // not quick rejected, so enable the scissor if clipRequired
        mCache.setScissorEnabled(mScissorOptimizationDisabled || clipRequired);
        mSkipOutlineClip = !roundRectClipRequired;
        return false;
    }*/
/*    if (quickRejectSetupScissor(0, 0, bitmap.getWidth(), bitmap.getHeight())) {
        return false;
    }*/

/*    public SurfaceTexture getSurfaceTexture() {
        Texture texture = getTexture(true);
        Caches.mSurfaceTexture = new SurfaceTexture(texture.id);
        return Caches.mSurfaceTexture;
    }*/

    public static SurfaceTexture createSurfaceTexture(Renderer renderer) {
        Caches cache = mCaches.get(renderer);
        if (cache == null) {
            cache = new Caches();
            mCaches.put(renderer, cache);
        }
        Texture texture = cache.mTextureCache.get(false); //Sending false creates new texture
        SurfaceTexture surfaceTexture = new SurfaceTexture(texture.id);
        cache.setSurfaceTexture(surfaceTexture);
        return surfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        Log.d(TAG, "Setting SurfaceTexture to: " + mSurfaceTexture + " Cache: " + mCache);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public static SurfaceTexture getSurfaceTexture(Renderer renderer) {
        Caches cache = mCaches.get(renderer);
        if(cache == null) return null;
        return cache.getSurfaceTexture();
    }

    private boolean drawWithFBOTexture(int textureId) {
        Log.d(TAG, "Going to render FBO Texture");

//        Texture texture = getTexture(true);
        int setup = setupDrawProgram();
        if(setup < 0) return false;

        Texture texture = getTexture(textureId);

        setupTexture(texture, 0);

        int slot = currentProgram.getUniform("baseSampler");
        if (slot < 0) {
            Log.e(TAG, "getUniform :: baseSampler returns invalid location");
        } else {
            if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler returns nice location");
        }
        GLES20.glUniform1i(slot, 0);

        if (mDrawable.getColorFilterMode() == 12||mDrawable.getColorFilterMode()==13) {
            setupTexelSizeUniform(mIncomingWidth, mIncomingHeight);
            // Passing a very large height so that it becomes equivalent to sampling from only one direction horizontal or vertical
        }

        switch (mDrawable.getConvolutionType()) {
            case Drawable2d.CONVOLUTION_TYPE_NONE:
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR:
                setupTexelSizeUniform(mDrawable.getIncomingWidth(), mDrawable.getIncomingHeight());

                mCache.mTextureCache.activeTexture(1);
                Texture blurKernel = getTexture(mCache.blurKernel);
                slot = currentProgram.getUniform("blurCoeffSampler");

                if (slot < 0) {
                    Log.e(TAG, "getUniform :: blurCoeffSampler returns invalid location");
                } else {
                    if (VERBOSE) Log.d(TAG, "getUniform :: baseSampler returns nice location");
                }

                GLES20.glUniform1i(slot, 1);

                setupDrawTexture(blurKernel.id, 1);
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL:
                setupTexelSizeUniform(mDrawable.getIncomingWidth(), mDrawable.getIncomingHeight() * 1000);
                break;
            case Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL:
                setupTexelSizeUniform(mDrawable.getIncomingWidth() * 1000, mDrawable.getIncomingHeight());
                break;
            default:
                Log.w(TAG, "Unsupported convolution type");
        }

        setupDrawModelView(mDrawable.getTranslate(), mDrawable.getScale(), mDrawable.getRotate());

        setupDrawTexture(textureId, 0);

        setupDrawMeshIndices();

        mCache.mGlUtil.checkGlError("drawTextureMesh :: Before glDrawElements");
        if (VERBOSE) Log.d(TAG, "going to call glDrawElements for fbo rect");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Caches.indexData.length, GLES20.GL_UNSIGNED_BYTE, 0);

        mCache.mGlUtil.checkGlError("drawTextureMesh :: glDrawElements");
        //GLES20.glFinish();
        mCache.unbindMeshBuffer();
        mCache.unbindIndicesBuffer();

        return true;
    }

    boolean drawWithExternalTexture(HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream) {
        if (VERBOSE) {
            Log.d(TAG, "drawWithExternalTexture");
        }

//      Make Texture Unit 0 as active
        mCache.mTextureCache.activeTexture(0);
        Texture texture = getTexture(true);
        if (texture == null) return false;
        texture.width = mIncomingWidth;
        texture.height = mIncomingHeight;

        if (mVerticalVideoMode || (mClipRect != null)) {
/*            mCache.setScissorEnabled(true);
            setScissorFromClip();*/
        }

        drawFilterBitmap(texture,filterBitmapHashMap,inputStream);
        finishDraw();
        return true;
    }


/*        if (mVerticalVideoMode || (mClipRect != null)) {
            //mCache.setScissorEnabled(false);
            mCache.disableScissor();
        }*/





    boolean drawSBitmap(SBitmap bitmap,HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream) {
//      Make Texture Unit 0 as active
        mCache.mTextureCache.activeTexture(0);
        Texture texture = getTexture(bitmap);
        if (texture == null) return false;
        drawFilterBitmap(texture,filterBitmapHashMap,inputStream);
/*        if (bitmap.getConfig() == Bitmap.Config.ALPHA_8) {
//      drawAlphaBitmap(texture, 0, 0, paint);
        } else {*/

/*        }*/
        finishDraw();
        return true;
    }

    boolean drawBitmap(Bitmap bitmap,HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream, Paint paint) {
        if(VERBOSE) {
            Log.d(TAG, "drawBitmap");
        }
//      Make Texture Unit 0 as active
        mCache.mTextureCache.activeTexture(0);
        Texture texture = getTexture(bitmap);
        if (texture == null) {
            Log.w(TAG, "drawBitmap:: texture is null");
            return false;
        }
        drawFilterBitmap(texture,filterBitmapHashMap,inputStream);
             //   }
/*        if (bitmap.getConfig() == Bitmap.Config.ALPHA_8) {
//      drawAlphaBitmap(texture, 0, 0, paint);
        } else {*/
/*        }*/
        finishDraw();
        return true;
    }

    public void drawFilterBitmap(Texture texture,HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream){
        HashMap<Integer, Texture> mFilterTexture = new HashMap<>();
        if(filterBitmapHashMap!=null) {
            int activeTexture =1;  // starting active texture from 1
            for (int i = 0; i < (filterBitmapHashMap.size()+1); i++) { // +1for input Stream texture
                if (i==12){
                    mCache.mTextureCache.activeTexture(activeTexture++);
                    Texture texture2=getTexture(inputStream);
                    mFilterTexture.put(i,texture2);
                }
               else {
                    mCache.mTextureCache.activeTexture(activeTexture++);
                    Texture texture1 = getTexture(filterBitmapHashMap.get(i));
                    mFilterTexture.put(i, texture1);
                }
                    }

        }

        drawTextureRect(texture, mFilterTexture);
    }

    boolean drawBitmaps(Bitmap bitmap1, Bitmap bitmap2) {
        mCache.mTextureCache.activeTexture(0);
        Texture texture1 = getTexture(bitmap1);
        if (texture1 == null) return false;

        mCache.mTextureCache.activeTexture(1);
        Texture texture2 = getTexture(bitmap2);
        drawTextureRect(texture1, texture2, null);
        finishDraw();
        return true;
    }



    private void finishDraw() {
        mCache.unbindMeshBuffer();
        mCache.unbindIndicesBuffer();
        mCache.unbindTextureBuffer();
        GLES20.glFlush();
    }

    boolean drawBitmap(Bitmap bitmap,HashMap<Integer,Bitmap> filterBitmapHashMap,InputStream inputStream) {
        return drawBitmap(bitmap,filterBitmapHashMap,inputStream,null);
    }

    public void allocTempResources() {
        if (mOutgoingHeight <= 0 || mOutgoingWidth <= 0) {
            return;
        }
        if (mIsPreview && (mCaptureBitmap==null || bitmapSource==null || bitmapBuffer == null || bitmapBuffer.length != mOutgoingWidth*mOutgoingHeight)) {
            try {
                bitmapBuffer = new int[mOutgoingWidth * mOutgoingHeight];
                bitmapSource = new int[mOutgoingWidth * mOutgoingHeight];
                if (mCaptureBitmap != null) {
                    mCaptureBitmap.recycle();
                    mCaptureBitmap = null;
                }
                mCaptureBitmap = Bitmap.createBitmap(mOutgoingWidth, mOutgoingHeight, Bitmap.Config.ARGB_8888);
                Log.d(TAG, "mCaptureBitmap created");
            } catch (OutOfMemoryError e) {
                return;
            }
        }
    }

    public void trimMemory() {
        clearCaptureBitmaps();
        if (mCaptureBitmap != null) {
            mCaptureBitmap.recycle();
            mCaptureBitmap = null;
        }
        bitmapSource = null;
        bitmapBuffer = null;
        Log.d(TAG, "mCaptureBitmap destroyed");
    }

    public void destroy() {
        destroy(false);
    }

    public void destroy(boolean destroy) {
        if(mRendererCount == 0) return; // Do not proceed for spurious or redundant destroy calls
        mRendererCount--;
        if(mRendererCount < 0) mRendererCount = 0;

        copyPropertiesToDefault();

        if (mRendererCount == 0 || destroy) {
//            mRendererCount = 0;
            //Terminate cache when no one is using it anymore...
            // which will happen when mRendererCount becomes 0
            terminateCache();
            mDrawables.remove(DRAWABLES_OVERLAY);
            mDrawables.remove(DRAWABLES_UPPER_OVERLAY);
            mCache = null;
            resetCameraParams();
            destroyStatics();
        }
        trimMemory();

        mDescription = null;
        mClipRect = null;

        mModelMatrix = null;
        mViewMatrix = null;
        mModelViewMatrix = null;
        mProjectionMatrix = null;
        mMVPMatrix = null;

        if(mUseFBO && fboTarget != null) {
            for (int i = 0; i < BLUR_PASS_COUNT; i++) {
                FBO fbo = fboTarget[i];
                fbo.release();
            }
        }
        resetRenderParams();

        if(mDrawables != null) mDrawables.clear();

/*        if (mIsPreview) {
            OpenGLRenderer.mPreviewDrawables.clear();
//Terminate Cache only in one of the cases... preferably in case of the GLSurfaceView's method calls
            if (mCache != null) {
                mCache.terminate();
                mCache = null;
            }
            resetRenderParams();
            resetCameraParams();
        } else {
            resetRenderParams();
            OpenGLRenderer.mEncoderDrawables.clear();
        }*/
    }

    private void copyPropertiesToDefault() {
        if(mDrawables == null || mDefaultDrawables == null) return;
        ArrayList<Drawable2d> defaultDrawables = mDrawables.get(DRAWABLES_DEFAULT);
        if (defaultDrawables != null) {
            try {
                Iterator<Drawable2d> itr = defaultDrawables.iterator();
                for (Drawable2d drawable2d : mDefaultDrawables) {
                    if (drawable2d != null) {
                        //drawable2d.setupDrawWithMultiTexture();
                        drawable2d.cloneProperties(itr.next());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void resetDrawableProperties() {
        mExternalTextureFilter = FilterManager.FILTER_NATURAL;
        Iterator<ArrayList<Drawable2d>> iterator = mDrawables.values().iterator();
        while ((iterator.hasNext())) {
            ArrayList<Drawable2d> drawableList = iterator.next();
            for (Drawable2d drawable : drawableList) {
                drawable.setFilterMode(FilterManager.FILTER_NATURAL);
            }
        }
        for (Drawable2d drawable : mDefaultDrawables) {
            drawable.setFilterMode(FilterManager.FILTER_NATURAL);
        }
        for(Drawable2d drawable2d : mDefaultDrawables) {
            drawable2d.setFilterMode(FilterManager.FILTER_NATURAL);
        }
        if(mAnimatableDrawables != null) {
            mAnimatableDrawables.clear();
//            mAnimatableDrawables = null;
        }
    }

    public static void destroyStatics() {
        if(mBitmaps != null) {
            for(Bitmap bitmap : mBitmaps.values()) {
                if(bitmap != null) {
                    Log.d(LOG_TAG, "Recycling bitmaps");
                    bitmap.recycle();
                }
            }
            mBitmaps.clear();
        }
    }
} // class OpenGLRenderer

class Paint {
    public float[] mColor;
    //  Bit mask for different filters that need to be applied
    public int colorFilter;
}
