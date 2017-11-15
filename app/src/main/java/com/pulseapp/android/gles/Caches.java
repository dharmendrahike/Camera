package com.pulseapp.android.gles;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Caches {

    private static final boolean VERBOSE = false;

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int BYTES_PER_SHORT = Short.SIZE / 8;

    private static final int VERTICES = 0;
    private static final int INDICES = 1;
    private static final int TEXTURES = 2;
    private static final int COLORS = 3;
    private static final int ELEMENTS_TYPE_COUNT = 4;

    private int[] bufferObjectArray = new int[ELEMENTS_TYPE_COUNT];

//    private FloatBuffer mCurrentPositionPointer;
//    public ShortBuffer mCurrentIndicesPointer;
//    private FloatBuffer mCurrentTexCoordsPointer;

    private int MAX_MESH_COUNT = 5;

    float mWallDist = 5.0f;

    private int[] mTextures;

    // Assuming that there's going to be only one Drawable based on SurfaceTexture
    // i.e.only 1 from camera/video, rest will be image/gif based stickers or custom renderables.
    public SurfaceTexture mSurfaceTexture;
    public volatile float[] mSurfaceTextureTransform;
    public volatile long mSurfaceTextureTimestamp;

    protected String sPreviewTerminationLock = "plock";
    protected String sEncoderTerminationLock = "elock";

    static GlUtil mGlUtil = null;
    static GlUtil mGlErrorUtil = null;

    static {
        mGlUtil = GlUtil.getInstance();
        mGlErrorUtil = GlErrorUtil.getInstance();
    }

    Lock mLock;

    public void setGlErrorFlag(boolean glErrorFlag) {
        if (!glErrorFlag) {
            mGlUtil = GlUtil.getInstance();
        } else {
            mGlErrorUtil = GlErrorUtil.getInstance();
            mGlUtil = mGlErrorUtil;
        }
    }

    private static final String TAG = "OpenGLRenderer::Caches";

    public static final byte[] indexData = {
            (byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0
    };

/*    enum FlushMode {
        kFlushMode_Layers,
        kFlushMode_Moderate,
        kFlushMode_Full
    };*/

/*    static final int gMeshStride = sizeof(TextureVertex);
    static final int gVertexStride = sizeof(Vertex);
    static final int gAlphaVertexStride = sizeof(AlphaVertex);*/

/*    static final int gMeshTextureOffset = 2 * Float.SIZE;

    static final int gVertexAlphaOffset = 2 * Float.SIZE;
    static final int gVertexAAWidthOffset = 2 * Float.SIZE;
    static final int gVertexAALengthOffset = 3 * Float.SIZE;*/

    public static final String[] FilterModeUniform = {
            "none"
            , "richMode"
            , "bwMode"
            ,"nightMode"
            , "vignetteMode"
            , "mirrorMode"
            , "bulgeMode"
            , "AmaroMode"
            , "XProMode"
            , "sketchMode"
            , "toonMode"

    };

    static final int gMeshCount = 4;

    private FloatBuffer vertexDataBuffer;
    public ByteBuffer indexDataBuffer;
    private FloatBuffer textureDataBuffer;
    private FloatBuffer colorDataBuffer;

    public FloatBuffer blurKernel;
    boolean blend;

    public int lastSrcMode;
    public int lastDstMode;
    public Program currentProgram;
    public boolean scissorEnabled;

//sahil
/*    ProgramCache mProgramCache;
    TextureCache mTextureCache;*/

    ProgramCache mProgramCache;
    TextureCache mTextureCache;

    public int mCurrentVertexHandle;
    public int mCurrentTextureHandle;
    public int mCurrentIndicesHandle;
    public int mCurrentColorsHandle;

//TODO
//Sahil
//    private final int mCurrentPositionPointer;

//    private int mCurrentPositionStride;

    //TODO
//Sahil
//    private final int mCurrentTexCoordsPointer;
//    private int mCurrentTexCoordsStride;

    boolean mVertexArrayEnabled;
    boolean mTexCoordsArrayEnabled;

    int mScissorX;
    int mScissorY;
    int mScissorWidth;
    int mScissorHeight;

    boolean mInitialized;

    float minX = 0, maxX = 0, minY = 0, maxY = 0;

    Caches() {
        mInitialized = false;
//sahil
/*        mProgramCache = new ProgramCache();
        mTextureCache = new TextureCache();*/
        init();

//TODO
//Sahil
//        initTempProperties();
    }

    public boolean init() {
        if (mInitialized) return false;

        Log.d(TAG, "Caches::init");

        mLock = new ReentrantLock();

        mProgramCache = new ProgramCache();
        mTextureCache = new TextureCache();

        GLES20.glGenBuffers(ELEMENTS_TYPE_COUNT, bufferObjectArray, 0);

        float[] vertexData = {
                -1.0f, -1.0f, -mWallDist,     // 0 bottom left
                1.0f, -1.0f, -mWallDist,      // 1 bottom right
                1.0f, 1.0f, -mWallDist,       // 2 top right
                -1.0f, 1.0f, -mWallDist,      // 3 top left
        };

        vertexDataBuffer = ByteBuffer
                .allocateDirect(vertexData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataBuffer.put(vertexData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[VERTICES]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * BYTES_PER_FLOAT, vertexDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        vertexData = null;

        indexDataBuffer = ByteBuffer
                .allocateDirect(indexData.length).order(ByteOrder.nativeOrder());
        indexDataBuffer.put(indexData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferObjectArray[INDICES]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexData.length, indexDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        float textureData[] = new float[]{
//Starting from bottom left (for SurfaceTexture or self created textures etc)
                0.0f, 0.0f,     // 0 bottom left
                1.0f, 0.0f,      // 3 bottom right
                1.0f, 1.0f,     // 2 top right
                0.0f, 1.0f     // 1 top left
                ,
//In case when Surface Texture is flipped about Y Axis (Front camera case)
                1.0f, 0.0f,     // 0 bottom right
                0.0f, 0.0f,      // 3 bottom left
                0.0f, 1.0f,     // 2 top left
                1.0f, 1.0f     // 1 top right
                ,
                0.0f, 1.0f,     // 1 bottom right
                1.0f, 1.0f,     // 2 top right
                1.0f, 0.0f,      // 3 top left
                0.0f, 0.0f     // 0 bottom left
        };

        textureDataBuffer = ByteBuffer
                .allocateDirect(textureData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureDataBuffer.put(textureData).position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * BYTES_PER_FLOAT, textureDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        float colorData[] = new float[] {
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.3f, 0.7f, 0.0f
        };

        colorDataBuffer = ByteBuffer
                .allocateDirect(colorData.length * 2 /* Assuming, we'd need a max of 2 such buffers  */ * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorDataBuffer.put(colorData).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorData.length * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        textureData = null;

        mCurrentIndicesHandle = 0;

        mTexCoordsArrayEnabled = false;
        mVertexArrayEnabled = false;

        // Emulate scissorEnabled, so that disableScissor disables scissor test after checking our flag :D
        scissorEnabled = true;
        disableScissor();
        resetScissor();

/*        mRegionMesh = null;
        mMeshIndices = 0;*/

        blend = false;
        lastSrcMode = GLES20.GL_ZERO;
        lastDstMode = GLES20.GL_ZERO;
        currentProgram = null;

        mInitialized = true;

//s TEMP
//sahil.bajaj@instalively.com
        mCurrentVertexHandle = bufferObjectArray[VERTICES];
        mCurrentIndicesHandle = bufferObjectArray[INDICES];
        mCurrentTextureHandle = bufferObjectArray[TEXTURES];
        mCurrentColorsHandle = bufferObjectArray[COLORS];
//s TEMP END

/*        float [] blurKernelData = new float[] {
                0.000007f, 0.000023f, 0.000191f, 0.000388f, 0.000191f, 0.000023f, 0.000007f,
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f,
                0.000192f, 0.006556f, 0.054721f, 0.110982f, 0.054722f, 0.006560f, 0.000191f,
                0.000388f, 0.013304f, 0.110981f, 0.225083f, 0.110982f, 0.013304f, 0.000388f,
                0.000191f, 0.006560f, 0.054721f, 0.110982f, 0.054722f, 0.006560f, 0.000191f,
                0.000023f, 0.000786f, 0.006556f, 0.013304f, 0.006560f, 0.000786f, 0.000023f,
                0.000007f, 0.000023f, 0.000191f, 0.000388f, 0.000191f, 0.000023f, 0.000007f,
                //Dummy values to fill 64 values (min supported texture size)
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f,
                0.000023f, 0.000786f, 0.006560f, 0.013304f, 0.006556f, 0.000786f, 0.000023f, 0.000023f
        };*/
        float [] blurKernelData = new float[]{
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.02f, 0.04f, 0.08f, 0.04f, 0.02f,
                0.04f, 0.08f, 0.16f, 0.08f, 0.04f,
                0.02f, 0.04f, 0.08f, 0.04f, 0.02f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,

                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,

                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f, 0.01f,
                0.01f, 0.02f, 0.04f, 0.02f
        };
        blurKernel = ByteBuffer.allocateDirect(blurKernelData.length*BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
        blurKernel.position(0);
        blurKernel.put(blurKernelData).position(0);
        return true;
    }

    public void terminate() {
        mLock.lock();

        if (!mInitialized) {
            mLock.unlock();
            return;
        }

        mCurrentVertexHandle = 0;
        mCurrentIndicesHandle = 0;
        mCurrentTextureHandle = 0;
        mCurrentColorsHandle = 0;

        //        delete[] mRegionMesh;
        //        mRegionMesh = null;

        GLES20.glDeleteBuffers(ELEMENTS_TYPE_COUNT, bufferObjectArray, 0);

        mProgramCache.clear();
        mTextureCache.clear();

        currentProgram = null;

//        clearGarbage();

        mInitialized = false;

        mProgramCache = null;
        mTextureCache = null;
        mLock.unlock();
    }

    public Lock getLock() {
        return mLock;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void clearGarbage() {
        mTextureCache.clearGarbage();
    }

    public void flush() {
        Log.d(TAG, "Flushing caches ");

        mTextureCache.clear();
//TODO
//Sahil
//      gradientCache.clear();
//      fall through

        clearGarbage();
//        GLES20.glFinish();
    }

///////////////////////////////////////////////////////////////////////////////
// VBO
///////////////////////////////////////////////////////////////////////////////

    public boolean bindMeshBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[VERTICES]);
        mGlUtil.checkGlError("glBindBuffer vertices");
        mCurrentVertexHandle = bufferObjectArray[VERTICES];
        return true;
//        return bindMeshBuffer(bufferObjectArray[VERTICES]);
    }

    public boolean unbindMeshBuffer() {
        if (mCurrentVertexHandle > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            mCurrentVertexHandle = 0;
            return true;
        }
        return false;
    }

    public boolean bindTextureBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        mGlUtil.checkGlError("glBindBuffer textures");
        mCurrentTextureHandle = bufferObjectArray[TEXTURES];
        return true;
//        return bindTextureBuffer(bufferObjectArray[TEXTURES]);
    }

    public boolean bindTextureBuffer(final int buffer) {
//s        if (mCurrentTextureHandle != buffer) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
        mCurrentTextureHandle = buffer;
        return true;
//s        }
//s        return false;
    }

    public boolean unbindTextureBuffer() {
        if (mCurrentTextureHandle > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            mCurrentTextureHandle = 0;
            return true;
        }
        return false;
    }

    public boolean bindIndicesBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferObjectArray[INDICES]);
        mGlUtil.checkGlError("glBindBuffer indices");
        mCurrentIndicesHandle = bufferObjectArray[INDICES];
        return true;
//        return bindIndicesBuffer(bufferObjectArray[INDICES]);
    }

    public boolean bindIndicesBuffer(final int buffer) {
//s        if (mCurrentIndicesHandle != buffer) {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer);
        mCurrentIndicesHandle = buffer;
        return true;
//s        }
//s        return false;
    }

    public boolean unbindIndicesBuffer() {
        if (mCurrentIndicesHandle > 0) {
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
            mCurrentIndicesHandle = 0;
            return true;
        }
        return false;
    }

    public boolean bindColorsBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
        mGlUtil.checkGlError("glBindBuffer colors");
        mCurrentColorsHandle = bufferObjectArray[COLORS];
        return true;
    }

    public boolean bindColorsBuffer(final int buffer) {
//s        if (mCurrentIndicesHandle != buffer) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
        mCurrentColorsHandle = buffer;
        return true;
//s        }
//s        return false;
    }

    public boolean unbindColorsBuffer() {
        if (mCurrentColorsHandle > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            mCurrentColorsHandle = 0;
            return true;
        }
        return false;
    }

///////////////////////////////////////////////////////////////////////////////
// PBO
///////////////////////////////////////////////////////////////////////////////

/*    public boolean bindPixelBuffer(final int buffer) {
        if (mCurrentPixelBuffer != buffer) {
//TODO
//sahil.bajaj@instalively.com
//Not needed yet
//            GLES20.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, buffer);
            mCurrentPixelBuffer = buffer;
            return true;
        }
        return false;
    }*/

    public boolean unbindPixelBuffer() {
//TODO
//sahil.bajaj@instalively.com
//Not needed now
/*        if (mCurrentPixelBuffer) {
            GLES20.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
            mCurrentPixelBuffer = 0;
            return true;
        }*/
        return false;
    }

/*    public void resetVertexPointers() {
        mCurrentPositionPointer = null;
        mCurrentTexCoordsPointer = null;
    }

    public void resetTexCoordsVertexPointer() {
        mCurrentTexCoordsPointer = null;
    }*/

/*    public void enableVertexArray() {
        if (!mVertexArrayEnabled) {
//            GLES20.glEnableVertexAttribArray(Program.SHADERBINDINGS_BINDINGTEXCOORDS);
            GLES20.glEnableVertexAttribArray(mCurrentVertexHandle);
//TODO
// Sahil
//           mCurrentTexCoordsPointer = this;
            mVertexArrayEnabled = true;
        }
    }

    public void disableVertexArray() {
        if (mVertexArrayEnabled) {
            GLES20.glDisableVertexAttribArray(mCurrentVertexHandle);
            mVertexArrayEnabled = false;
        }
    }*/

/*    public void enableTexCoordsVertexArray() {
        if (!mTexCoordsArrayEnabled) {
//            GLES20.glEnableVertexAttribArray(Program.SHADERBINDINGS_BINDINGTEXCOORDS);
            GLES20.glEnableVertexAttribArray(mCurrentTextureHandle);
//TODO
// Sahil
//           mCurrentTexCoordsPointer = this;
            mTexCoordsArrayEnabled = true;
        }
    }

    public void disableTexCoordsVertexArray() {
        if (mTexCoordsArrayEnabled) {
            GLES20.glDisableVertexAttribArray(mCurrentTextureHandle);
            mTexCoordsArrayEnabled = false;
        }
    }*/

    public void setTextureCoordinates(float minX, float maxX, float minY, float maxY) {
        setTextureCoordinates(minX, maxX, minY, maxY, 0);
    }

    public void setTextureCoordinates(float minX, float maxX, float minY, float maxY, int position) {
        int initialIndex = position * 8;
        int index = initialIndex;
        textureDataBuffer.put(index, minX);  index++;    textureDataBuffer.put(index, minY);    index++;
        textureDataBuffer.put(index, maxX);  index++;    textureDataBuffer.put(index, minY);    index++;
        textureDataBuffer.put(index, maxX);  index++;  textureDataBuffer.put(index, maxY);      index++;
        textureDataBuffer.put(index, minX);  index++;  textureDataBuffer.put(index, maxY);      index++;

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[TEXTURES]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 8 * BYTES_PER_FLOAT, textureDataBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void setColors(float [] colors) {
        setColors(colors, 0);
    }

    public void setColors(float [] colors, int position) {
//        Log.d(TAG, "setColors called for background");
        int initialIndex = position * 16;
        // Again position will be 0 for us, for now atleast.
        int index = initialIndex;

        colorDataBuffer.position(index);
        colorDataBuffer.put(colors);
        colorDataBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 16 * BYTES_PER_FLOAT, colorDataBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 16 * 2 * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void pushColors(float [] colors, int position, int size) {
        int initialIndex = position * 16;
        // Again position will be 0 for us, for now atleast.
        int index = initialIndex;

        float [] temp = new float[size];
        colorDataBuffer.get(temp, index, size);

        colorDataBuffer.position((initialIndex + 1) * 16);
        colorDataBuffer.put(temp);

        colorDataBuffer.position(index);
        colorDataBuffer.put(colors);

        colorDataBuffer.position(0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjectArray[COLORS]);
//        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, initialIndex, 16 * BYTES_PER_FLOAT, colorDataBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 16 * BYTES_PER_FLOAT, colorDataBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public boolean setScissor(int x, int y, int width, int height) {
//        if (scissorEnabled & (x != mScissorX || y != mScissorY ||
//                width != mScissorWidth || height != mScissorHeight)) {

            if (x < 0) {
                width += x;
                x = 0;
            }
            if (y < 0) {
                height += y;
                y = 0;
            }
            if (width < 0) {
                width = 0;
            }
            if (height < 0) {
                height = 0;
            }

//            Log.d(TAG, "Enabling Scissor : " + "x " + x + " y " + y + " width " + width + " height " + height);
            GLES20.glScissor(x, y, width, height);

            mScissorX = x;
            mScissorY = y;
            mScissorWidth = width;
            mScissorHeight = height;

            return true;
/*        }
        return false;*/
    }

    public boolean enableScissor() {
/*        if (!scissorEnabled) {*/
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            scissorEnabled = true;
            resetScissor();
            return true;
/*        }
        return false;*/
    }

    public boolean disableScissor() {
/*        if (scissorEnabled) {*/
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
//            scissorEnabled = false;
            return true;
/*        }
        return false;*/
    }

    void setScissorEnabled(boolean enabled) {
//        if (scissorEnabled != enabled) {
            if (enabled) GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            else GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
//            scissorEnabled = enabled;
            resetScissor();
//        }
    }

    public void resetScissor() {
        mScissorX = mScissorY = mScissorWidth = mScissorHeight = 0;
    }


    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        mGlUtil.checkGlError("before glCreateShader" + shaderType);
        if (VERBOSE) Log.d(TAG, "Shader source: \n" + source);
        int shader = GLES20.glCreateShader(shaderType);
//        checkGlError("glCreateShader type=" + shaderType);
        if (shader == 0) {
            Log.e(TAG, "Error while creating shader");
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        } else {
            Log.d(TAG, "Shader compilation successful");
        }
        return shader;
    }

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        Log.d(TAG, "both shaders created successfully");
        int program = GLES20.glCreateProgram();
        mGlUtil.checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        mGlUtil.checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        mGlUtil.checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        if (program != 0) {
            Log.d(TAG, "Program created successfully");
        }
        return program;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void bindTexture(int texture) {
        mTextureCache.bindTexture(texture);
    }

    public void bindTexture(int texture, int textureUnit) {
        mTextureCache.bindTexture(texture, textureUnit);
    }

    public void bindTexture(int target, int texture, int textureUnit) {
        mTextureCache.bindTexture(target, texture, textureUnit);
    }

//TODO minor
//sahil.bajaj@instalively.com
//Might need to implement later, in case need arises
//For advanced 3d effects such as Lighting or Shadows for example, overlays can cast shadow upon background video
/*    void initTempProperties() {
        propertyLightDiameter = -1.0f;
        propertyLightPosY = -1.0f;
        propertyLightPosZ = -1.0f;
        propertyAmbientRatio = -1.0f;
        propertyAmbientShadowStrength = -1;
        propertySpotShadowStrength = -1;
    }

    void setTempProperty(final char* name, final char* value) {
        Log.d(TAG, "setting property %s to %s", name, value);
        if (!strcmp(name, "ambientRatio")) {
            propertyAmbientRatio = fmin(fmax(atof(value), 0.0), 10.0);
            Log.d(TAG, "ambientRatio = %.2f", propertyAmbientRatio);
            return;
        } else if (!strcmp(name, "lightDiameter")) {
            propertyLightDiameter = fmin(fmax(atof(value), 0.0), 3000.0);
            Log.d(TAG, "lightDiameter = %.2f", propertyLightDiameter);
            return;
        } else if (!strcmp(name, "lightPosY")) {
            propertyLightPosY = fmin(fmax(atof(value), 0.0), 3000.0);
            Log.d(TAG, "lightPos Y = %.2f", propertyLightPosY);
            return;
        } else if (!strcmp(name, "lightPosZ")) {
            propertyLightPosZ = fmin(fmax(atof(value), 0.0), 3000.0);
            Log.d(TAG, "lightPos Z = %.2f", propertyLightPosZ);
            return;
        } else if (!strcmp(name, "ambientShadowStrength")) {
            propertyAmbientShadowStrength = atoi(value);
            Log.d(TAG, "ambient shadow strength = 0x%x out of 0xff", propertyAmbientShadowStrength);
            return;
        } else if (!strcmp(name, "spotShadowStrength")) {
            propertySpotShadowStrength = atoi(value);
            Log.d(TAG, "spot shadow strength = 0x%x out of 0xff", propertySpotShadowStrength);
            return;
        }
        Log.d(TAG, "    failed");
    }
*/
}
