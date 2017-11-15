package com.pulseapp.android.gles;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.ETC1Util;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.LruCache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

public class TextureCache {

    String mLock = "TextureCache";
    String TAG = "TextureCache";

    private final boolean VERBOSE = false;

//    HashMap<Object, Texture> mCache;
    LruTextureCache mCache;
//    ArrayList<Integer> mGarbage;

    Texture mExternalTexture = null;
    private HashMap<Integer, Texture> mFBOTextures;
//    private Texture []mFBOTexture = null;

    int mSize;
    private boolean mDebugEnabled = true;

    private long mNonBitmapIdStartIndex = 1 << 32;

    private int[] mBoundTextures;
    private int MAX_TEXTURE_UNITS_COUNT = 15;

    int mTextureUnit;
    private GlUtil mGlUtil;

    public TextureCache() {
        if (mCache == null)
/*            mCache = new LruTextureCache(11 * 1024 * 1024); // 11 MB*/
            mCache = new LruTextureCache(25); // 25 textures
/*        if (mGarbage == null)
            mGarbage = new ArrayList<Integer>();*/
        mFBOTextures = new HashMap<>();
        mBoundTextures = new int[MAX_TEXTURE_UNITS_COUNT];

        resetBoundTextures();
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        mTextureUnit = 0;
        mGlUtil = GlUtil.getInstance();

    }

    // sending true returns the ExternalTexture from Cache. false returns a new texture.
    Texture get(boolean isExternalTexture) {
        Texture texture = null;
        if(isExternalTexture) {
            texture = mExternalTexture;
        } else {
            if(mExternalTexture != null) {
                deleteTexture(mExternalTexture.id);
                mExternalTexture = null;
            }
            texture = new Texture();
            generateTexture(texture);
            mExternalTexture = texture;
        }
        return texture;
    }

/*    Texture get(SurfaceTexture surfaceTexture) {
        if(surfaceTexture == null) return null;
        Texture texture = getCachedTexture(surfaceTexture);
        if(texture == null) {
            texture = new Texture();
            generateTexture(texture);
        }
        return texture;
    }*/

    Texture get(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Texture texture = getCachedTexture(bitmap);

        if (texture == null) {

            final int size = bitmap.getByteCount();
            texture = new Texture();
            texture.bitmapSize = size;
            generateTexture(bitmap, texture, false);
            texture.cleanup = true;
        }

        return texture;
    }

    Texture get(Buffer buffer) {
        if(buffer == null) {
            return null;
        }
        Texture texture = getCachedTexture(buffer);
        return texture;
    }

    Texture get(InputStream inputStream) {
        if(inputStream == null) {
            return null;
        }

        Texture texture = getCachedTexture(inputStream);
        if (texture == null) {
            /*
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int i;
            try {
                while ((i = inputStream.read()) != -1) {
                    bos.write(i);
                }
            }
            catch(Exception E){

            }
            byte[] byteArray = bos.toByteArray();
            final int size = byteArray.length*1000;
            */
            texture = new Texture();
           // texture.bitmapSize = size;
            generateTexture(inputStream, texture);
            texture.cleanup = true;
        }
        return texture;
    }

    Texture get(int textureId) {
        if (mFBOTextures.get(textureId) != null) return mFBOTextures.get(textureId);
//        if (!isExternalTexture) return null;
        Texture texture = new Texture();
        texture.isFBOTexture = true;
        generateTexture(texture, textureId);
        mFBOTextures.put(textureId, texture);
        return texture;
    }

    Texture get(OpenGLRenderer.SBitmap sBitmap) {
        Texture texture = getCachedTexture(sBitmap);

        if (texture == null) {

            final int size = sBitmap.mWidth * sBitmap.mHeight * 4;
            texture = new Texture();
            texture.bitmapSize = size;
            generateTexture(sBitmap, texture, false);
            texture.cleanup = true;
        }

        return texture;
    }

    Texture getCachedTexture(SurfaceTexture surfaceTexture) {
        Texture texture = mCache.get(surfaceTexture.hashCode());
        if (texture == null) {
            texture = new Texture();
        }
        return texture;
    }

    Texture getCachedTexture(Buffer buffer) {
        Texture texture = mCache.get(buffer);
        if(texture == null) {
            texture = new Texture();
            int size = 4 * buffer.capacity();
            texture.bitmapSize = size;
            generateTexture(buffer, texture);
            mSize += size;

            if(VERBOSE) Log.d(TAG, "TextureCache::get: create texture for buffer" + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created, size = " + size);
            }
            mCache.put(buffer, texture);
        }
        return texture;
    }

    Texture getCachedTexture(InputStream inputStream) {
        Texture texture = mCache.get(inputStream);
        if(texture == null) {
            texture = new Texture();
            /*
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int i;
            try {
                while ((i = inputStream.read()) != -1) {
                    bos.write(i);
                }
            }
            catch(Exception E){

            }
            byte[] byteArray = bos.toByteArray();
            int size =byteArray.length*1000;
            texture.bitmapSize = size;
            */
            generateTexture(inputStream, texture);
            /*
            mSize += size;

            if(VERBOSE) Log.d(TAG, "TextureCache::get: create texture for input Stream" + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created from input Stream, size = " + size);
            }
            */
            mCache.put(inputStream, texture);
        }
        return texture;
    }

    Texture getCachedTexture(Bitmap bitmap) {
        Texture texture = mCache.get(bitmap);
        if (texture == null) {
            texture = new Texture();
            final int size = bitmap.getByteCount();
            texture.bitmapSize = size;
            generateTexture(bitmap, texture, false);

            mSize += size;
            if(VERBOSE) Log.d(TAG, "TextureCache::get: create texture " + bitmap + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created, size = " + size);
            }
            mCache.put(bitmap, texture);
        } else if (!texture.isInUse && bitmap.getGenerationId() != texture.generation) {
            // Texture was in the cache but is dirty, re-upload
            // TODO: Re-adjust the cache size if the bitmap's dimensions hab changed
            generateTexture(bitmap, texture, true);
        }
        return texture;
    }

    Texture getCachedTexture(OpenGLRenderer.SBitmap sBitmap) {
        Texture texture = mCache.get(sBitmap);
        if (texture == null) {
            texture = new Texture();
            final int size = sBitmap.mWidth * sBitmap.mHeight * 4;
            texture.bitmapSize = size;
            generateTexture(sBitmap, texture, false);

            // Unfortunately, we cannot account for this texture's size in the cache's size
//            mSize += size;
            Log.d(TAG, "TextureCache::get: create texture " + sBitmap + " size " + size + " mSize " + mSize);
            if (mDebugEnabled) {
                Log.d(TAG, "Texture created, size = " + size);
            }
            mCache.put(sBitmap, texture);
        }
        return texture;
    }

    /**
     * Just sets up an already created texture for which id is known
     */
    void generateTexture(Texture texture, int textureId) {
        texture.id = textureId;

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        Caches.mGlUtil.checkGlError("glBindTexture " + textureId);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        Caches.mGlUtil.checkGlError("glTexParameter");
    }

    /**
     * Generates an External Texture
     */
    void generateTexture(Texture externalTexture) {
        if (externalTexture == null) {
            Log.e(TAG, "Input Texture is null, there is a serious issue");
            return;
        }
        Log.d(TAG, "Upload Input/External Texture");

        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        Caches.mGlUtil.checkGlError("generate External Texture :: glGenTextures");

        int textureId = textureHandles[0];
        externalTexture.id = textureId;
        externalTexture.isExternalTexture = true;

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        Caches.mGlUtil.checkGlError("glBindTexture " + textureId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        Caches.mGlUtil.checkGlError("glTexParameter");
    }

    /**
     * Generates the texture from a bitmap into the specified texture structure.
     *
     * @param regenerate If true, the bitmap data is reuploaded into the texture, but
     *                   no new texture is generated.
     */
    void generateTexture(final Bitmap bitmap, Texture texture, boolean regenerate) {

        if(VERBOSE) Log.d(TAG, "Upload Texture " + "width: " + bitmap.getWidth() + "height: " + bitmap.getHeight());

        // If the texture had mipmap enabled but not anymore,
        // force a glTexImage2D to discard the mipmap levels
        final boolean resize = !regenerate || bitmap.getWidth() != texture.width || bitmap.getHeight() != texture.height;

        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        if (!regenerate) {
            GLES20.glGenTextures(1, textureHandles, 0);
            texture.id = textureHandles[0];
        }

        texture.generation = bitmap.getGenerationId();
        texture.width = bitmap.getWidth();
        texture.height = bitmap.getHeight();

        bindTexture(texture.id);
        int[] pixels = null;

        switch (bitmap.getConfig()) {
            case ALPHA_8:
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
//TODO major
//sahil.bajaj@instalively
//Original usage needs row bytes as pixels instead of bytes. So need to see whether to use getRowBytes or getPixels here
                pixels = new int[bitmap.getWidth() * bitmap.getHeight() / 4];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                uploadToTexture(resize, GLES20.GL_ALPHA, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_BYTE, pixels);
                texture.blend = true;
                break;
            case RGB_565:
/*                pixels = new int[bitmap.getWidth() * bitmap.getHeight() / 2];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());*/
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//                uploadToTexture(resize, GLES20.GL_RGB, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_SHORT_5_6_5, pixels);
                texture.blend = false;
                break;
            case ARGB_8888:
                pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                uploadToTexture(resize, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getRowBytes() / bitmap.getWidth(), texture.width, texture.height, GLES20.GL_UNSIGNED_BYTE, pixels);
//                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//TODO
//sahil.bajaj@instalively.com
//blend might need to be set, not sure at this moment
                texture.blend = false;
                break;
            default:
                Log.w(TAG, "Unsupported bitmap colorType: " + bitmap.getConfig());
                break;
        }

        if (!regenerate) {
            texture.setFilter(GLES20.GL_NEAREST);
            texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        }
    }

    void generateTexture(Bitmap bitmap, Texture texture) {
        generateTexture(bitmap, texture, false);
    }

    void generateTexture(final OpenGLRenderer.SBitmap sBitmap, Texture texture, boolean regenerate) {

        Log.d(TAG, "Upload Texture " + "width: " + sBitmap.getWidth() + "height: " + sBitmap.getHeight());

        // If the texture had mipmap enabled but not anymore,
        // force a glTexImage2D to discard the mipmap levels
        final boolean resize = !regenerate || sBitmap.getWidth() != texture.width || sBitmap.getHeight() != texture.height;

        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        if (!regenerate) {
            GLES20.glGenTextures(1, textureHandles, 0);
            texture.id = textureHandles[0];
        }

        texture.isSBitmapTexture = true;
        texture.generation = sBitmap.hashCode();
        texture.width = sBitmap.getWidth();
        texture.height = sBitmap.getHeight();

        bindTexture(texture.id);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        uploadToTexture(resize, GLES20.GL_RGBA, sBitmap.getWidth(), 4, texture.width, texture.height, GLES20.GL_UNSIGNED_BYTE, sBitmap.getPixelBuf());
//                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//TODO
//sahil.bajaj@instalively.com
//blend might need to be set, not sure at this moment
        texture.blend = false;

        if (!regenerate)

        {
            texture.setFilter(GLES20.GL_NEAREST);
            texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        }
    }

    void generateTexture(Buffer buffer, Texture texture) {
        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        texture.id = textureHandles[0];

        texture.generation = buffer.hashCode();
        texture.width = buffer.capacity();
        texture.height = 1;

        bindTexture(texture.id);
        buffer.position(0);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, buffer.capacity(), 1, 0, GLES20.GL_LUMINANCE, GLES20.GL_FLOAT, buffer);
        mGlUtil.checkGlError("TexImage2d for 1D blur Kernel");

        texture.setFilter(GLES20.GL_NEAREST); // Very important to make it GL_NEAREST.
        // Because we need precise values for blur coefficients.

        texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        activeTexture(0);
        bindTexture(0);
    }


    void generateTexture(InputStream inputStream, Texture texture) {
        int[] textureHandles = new int[1];

        GLES20.glGenTextures(1, textureHandles, 0);
        texture.id = textureHandles[0];


        bindTexture(texture.id);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        int w = 1024;
        int h = 1024;
        int nP = w * h; //no. of pixels

        //load the binary data
        byte[] byteArray = new byte[nP];
        try {
            InputStream fis = inputStream;
            fis.read(byteArray);
            fis.reset();
        } catch(IOException e) {
        }

        System.out.println(byteArray[1]);

        ByteBuffer buffer = ByteBuffer.allocateDirect(nP);
        buffer.put(byteArray);
        buffer.position(0);
        InputStream inputStreamRenderer=new ByteArrayInputStream(byteArray);
        try {
            inputStreamRenderer.reset();
        }catch(IOException e){
        }

        bindTexture(texture.id);

        try{
            // Build Texture from loaded bitmap for the currently-bind texture ID
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0,GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, inputStreamRenderer);
        }

        catch(IOException e){
            System.out.println("DEBUG! IOException"+e.getMessage());

        }

        texture.setFilter(GLES20.GL_NEAREST); // Very important to make it GL_NEAREST.
        // Because we need precise values for blur coefficients.

        texture.setWrap(GLES20.GL_CLAMP_TO_EDGE);
        activeTexture(0);
        bindTexture(0);
    }


    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, int[] pixels) {
        IntBuffer data = IntBuffer.wrap(pixels);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, data);
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, Buffer pixelBuf) {
        if(pixelBuf instanceof ByteBuffer) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, ((ByteBuffer)pixelBuf).asIntBuffer());
        } else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf);
        }
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, ByteBuffer pixelBuf) {
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf.asIntBuffer());
    }

    void uploadToTexture(boolean resize, int format, int stride, int bpp, int width, int height, int type, IntBuffer pixelBuf) {
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, type, pixelBuf);
    }

    public void bindTexture(int target, int texture, int textureUnit) {
        activeTexture(textureUnit);
        if (VERBOSE) Log.d(TAG, "Caches :: bindTexture < texture");
//        if (mBoundTextures[mTextureUnit] != texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        mGlUtil.checkGlError("glBindTexture GL_TEXTURE_2D");
        mBoundTextures[mTextureUnit] = texture;
//        }
    }

    public void bindTexture(int texture) {

        if (VERBOSE) Log.d(TAG, "Caches :: bindTexture < texture");
//        if (mBoundTextures[mTextureUnit] != texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        mGlUtil.checkGlError("glBindTexture GL_TEXTURE_2D");
        mBoundTextures[mTextureUnit] = texture;
//        }
    }

    public void bindTexture(int target, int texture) {
        if (VERBOSE) Log.d(TAG, "Caches :: bindTexture < target, texture");
        if (target == GLES20.GL_TEXTURE_2D) {
            bindTexture(texture);
        } else {
            // GLConsumer directly calls glBindTexture() with
            // target=GL_TEXTURE_EXTERNAL_OES, don't cache this target
            // since the cached state could be stale
            GLES20.glBindTexture(target, 0);
            GLES20.glBindTexture(target, texture);
            mGlUtil.checkGlError("glBindTexture GL_TEXTURE_EXTERNAL_OES");
        }
    }

    public void deleteTexture(int texture) {
        unbindTexture(texture);
    }

    void activeTexture(int textureUnit) {
//s        if (mTextureUnit != textureUnit) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
        mGlUtil.checkGlError("Caches :: activeTexture");
//s            mTextureUnit = textureUnit;
//s        }
    }

    public void resetActiveTexture() {
        mTextureUnit = -1;
    }

    public void resetBoundTextures() {
        for (int i = 0; i < MAX_TEXTURE_UNITS_COUNT; i++) {
            mBoundTextures[i] = 0;
        }
    }

    public int unbindTexture(int texture) {
/*        for (int i = 0; i < MAX_TEXTURE_UNITS_COUNT; i++) {
            if (mBoundTextures[i] == texture) {
                GLES20.glDeleteTextures(1, mBoundTextures, i);
                mBoundTextures[i] = 0;
                return i;
            }
        }*/
        int [] tempArr = new int[1];
        tempArr[0] = texture;
        GLES20.glDeleteTextures(1, tempArr, 0);
        return -1;
    }

    /**
     * Removes the texture associated with the specified bitmap. This is meant
     * to be called from threads that are not the EGL context thread.
     */
    void releaseTexture(Bitmap bitmap) {
        if (bitmap == null) return;

/*        synchronized (mLock) {
            mGarbage.add(bitmap.getGenerationId());
        }*/
    }

    /**
     * Process deferred removals.
     */
    void clearGarbage() {
/*        synchronized (mLock) {
            int count = mGarbage.size();
            for (int i = 0; i < count; i++) {
                int bitmapId = mGarbage.get(i);
                mCache.remove(bitmapId);
            }
            mGarbage.clear();
        }*/
    }

    /**
     * Clears the cache. This causes all textures to be deleted.
     */
    void clear() {
        if(mExternalTexture != null) {
            deleteTexture(mExternalTexture.id);
            mExternalTexture = null;
        }
        mCache.evictAll();
/*        Collection<Texture> textureCollection = mCache.values();
        Iterator<Texture> textureIterator = textureCollection.iterator();
        while (textureIterator.hasNext()) {
            Texture texture = textureIterator.next();
            texture.deleteTexture();
        }
        mCache.clear();*/
        Log.d(TAG, "TextureCache:clear(), mSize = " + mSize);
    }

/*    private void deleteTexture(Texture texture) {
        texture.deleteTexture();
    }*/

    private class LruTextureCache extends LruCache<Object, Texture> {
        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public LruTextureCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved (boolean evicted, Object bitmap, Texture texture, Texture newTexture) {
            if(VERBOSE) Log.d(TAG, "Evicting texture " + texture + " sourced from Bitmap " + bitmap);
            deleteTexture(texture.id);
        }

/*        @Override
        protected int sizeOf(Object bitmap, Texture texture) {
            if(bitmap instanceof Bitmap) {
                return ((Bitmap) bitmap).getByteCount();
            } else if(bitmap instanceof OpenGLRenderer.SBitmap) {
                return ((OpenGLRenderer.SBitmap) bitmap).getByteCount();
            } else {
                return texture.bitmapSize*4;
            }
        }*/
    }
}
