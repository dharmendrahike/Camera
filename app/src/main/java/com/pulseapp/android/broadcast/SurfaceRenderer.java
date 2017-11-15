package com.pulseapp.android.broadcast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.org.rajawali3d.Object3D;
import android.org.rajawali3d.animation.Animation;
import android.org.rajawali3d.animation.Animation3D;
import android.org.rajawali3d.animation.EllipticalOrbitAnimation3D;
import android.org.rajawali3d.animation.RotateOnAxisAnimation;
import android.org.rajawali3d.animation.TranslateAnimation3D;
import android.org.rajawali3d.cameras.Camera;
import android.org.rajawali3d.lights.ALight;
import android.org.rajawali3d.lights.DirectionalLight;
import android.org.rajawali3d.lights.SpotLight;
import android.org.rajawali3d.loader.LoaderOBJ;
import android.org.rajawali3d.materials.Material;
import android.org.rajawali3d.materials.methods.DiffuseMethod;
import android.org.rajawali3d.materials.methods.SpecularMethod;
import android.org.rajawali3d.math.Matrix4;
import android.org.rajawali3d.math.vector.Vector3;
import android.org.rajawali3d.primitives.NPrism;
import android.org.rajawali3d.primitives.Torus;
import android.org.rajawali3d.renderer.RajawaliRenderer;
import android.org.rajawali3d.util.RajLog;
import android.os.Build;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.pulseapp.android.R;
import com.pulseapp.android.gles.Caches;
import com.pulseapp.android.gles.Drawable2d;
import com.pulseapp.android.gles.OpenGLRenderer;
import com.pulseapp.android.stickers.ThemeModel;
import com.pulseapp.android.util.AppLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.pulseapp.android.broadcast.MoviePlayer.RENDER_TARGET_DISPLAY;
import static com.pulseapp.android.broadcast.MoviePlayer.RENDER_TARGET_VIDEO;

/**
 * Created by bajaj on 14/1/16.
 */
public class SurfaceRenderer extends RajawaliRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
        /*, TextureMovieEncoder.Callback, VideoEncoderCore.Callback */
        , DummySurfaceRenderer.RenderCallback {

    private static final String TAG = "SurfaceRenderer";
    static final Object mRenderFence = new Object();
    private String mDestFilePath;

    private OpenGLRenderer mGLRenderer;
    private SurfaceTexture mSurfaceTexture;
    private GLSurfaceView mGLSurfaceView;

    private volatile boolean mRecordingEnabled;
    private int mRecordingStatus;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private boolean VERBOSE = false;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private float[] mSTMatrix = new float[16];
    private long mTimeStamp;
    private float scale;
    private float mZoomLevel;
    private Context mContext;
    public Boolean updateSurface;

    private boolean mVideoDecode = false;

    int mVideoWidth, mVideoHeight;
    private int mVideoFPS = 30;
    private int mVideoBitrate;

    private MoviePlayer.PlayerFeedback mPlayerCallbackListener;

    private TextureMovieEncoder mVideoEncoder;

    private long initialTime;
    private Bitmap videoBackground;
    private boolean captureEnabled;
    private CameraCapture cameraCapturer;
    private DummySurfaceRenderer mDummyRenderer;
    private int captureFilter;
    private boolean isFlipped;
    private boolean cameraChanged;
    private long mFrameTimestampNs;

    private int mRenderTarget;

    private boolean mUseOwnContext = false; // For testing
    private boolean mSurfaceCreated;
    private EGLContext mEGLContext;        //DummyGLRenderer (Ubiquitous)
    private EGLContext mSharedEGLContext; //GLSurfaceView
    private int mCurrentFilter;
    private int mNewFilter;

    private Vector3 defaultPos;
    private Vector3 pos = new Vector3();

    public DirectionalLight light;
    public SpotLight spotLight;
    public Object3D sphere;
    public Context context;
    public Camera camera;

    private Animation3D mCameraAnim, mLightAnim;
    private Object3D mObjectGroup;

    TranslateAnimation3D translateAnimation;
    private boolean maxReached;
    private double x;
    private boolean mSurfaceSetupDone;
    private volatile boolean mSurfaceTextureReleased;
    private volatile boolean mRenderPause;
    private boolean mRendererPrepared;

    private StreamingActionController mController;

    volatile boolean mImageCaptureRequest;
    private boolean mIsPlayImageStream;
    private int mFirstFrame = 0;
    private Bitmap mImageBackground;
    private ImageView mOverlayView;
    private boolean mPlaybackStopped;
    private Caches mCache;
    private int[] mBitmapResources;
    private HashMap<Integer,Bitmap> mFilterBitmapHashMap=new HashMap<>();
    public  int mFilterBitmapCount;
    private InputStream inputStream;
    private int mNewFilterOrder;
    public  static final int filterOrder[]={0,7,15,9,16,12,13,14};
    public  static final int filterOrderVideo[]={0,7,15,9,16,12,14,18,19};
    private int mNewCurrentFilter;

    public SurfaceRenderer(Context context, TextureMovieEncoder videoEncoder,int bitRate,int fps) {
        super(context);
        mContext = context.getApplicationContext();
        mVideoEncoder = videoEncoder;
        mVideoEncoder.setRenderController(this);
        mVideoBitrate = bitRate;
        mVideoFPS = fps;

        init();
    }

    private void init() {
        init(false);
    }

    private void init(boolean registerForResources) {

            RajLog.i("Rajawali | Anchor Steam | v1.0 ");
        //RajLog.i("THIS IS A DEV BRANCH CONTAINING SIGNIFICANT CHANGES. PLEASE REFER TO CHANGELOG.md FOR MORE INFORMATION.");
        Log.d(TAG, "mRecordingEnabled set to false");
        mRecordingEnabled = false;
        mFrameTimestampNs = -1;
        mSurfaceCreated = false;
        mSurfaceSetupDone = false;
        mSurfaceTextureReleased = false;
        mRenderPause = false;
        mIsPlayImageStream = false;
        mFirstFrame = 0;
        mCurrentFilter = 0;
        mNewFilterOrder=0;
        mNewCurrentFilter=0;
        mBitmapResources=new int[]{ R.drawable.map_nashville,      //NashVille BitmapResources
                                    R.drawable.filter2,            //Xpro BitmapResources
                                    R.drawable.overlay_map,              //overlay
                                    R.drawable.map_amaro,                //amaro
                                    R.drawable.map_hudson,              //hudson
                                    R.drawable.map_valencia,               //ValenciaMap BitmapResources
                                    R.drawable.map_valencia_gradient,     //ValenciaGradientMap BitmapResources
                                    R.drawable.map_inkwell,        //inkwell BitmapResources
                                    R.drawable.edge_burn,
                                    R.drawable.map_hefe,
                                    R.drawable.map_hefe_gradient,
                                    R.drawable.soft_light_map,
                                    R.raw.metal,
                                    R.drawable.map_brannan,
                                    R.drawable.map_brannan_blowout,
                                    R.drawable.map_brannan_contrast,
                                    R.drawable.map_brannan_luma,
                                    R.drawable.map_brannan_screen,
                                    R.drawable.curves1,
                                    R.drawable.curves2,
                                    };
        setFilterBitmapCount(mBitmapResources.length);
    }

    public void setFilterBitmapCount(int filterBitmapCount){
        mFilterBitmapCount=filterBitmapCount;
    }

    public int getFilterBitmapCount(){
        return mFilterBitmapCount;
    }

    public void setRenderTargetType(int renderTargetType) {
        mRenderTarget = renderTargetType;
        mUseOwnContext = ((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) ? false : true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated...");
        destroyRenderer();
        if(((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) || mPlaybackStopped) {
            createFilterBitmap();
            prepareRenderer();
//        onRenderSurfaceCreated(config, gl, 0, 0);
            setFrameRate(20);
        }


        mRenderPause = false;
    }

    void prepareRenderer() {
        Log.d(TAG, "prepareRenderer");
        setPlaybackStopped(false);
        if(!mUseOwnContext) {
            mSharedEGLContext = EGL14.eglGetCurrentContext();

            mSurfaceCreated = true;
        }
        mRenderPause = false;
        if((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) {
//            OpenGLRenderer.destroyCache();
            mGLRenderer = new OpenGLRenderer(OpenGLRenderer.Fuzzy.PREVIEW, this);
        }
        if(mGLRenderer!=null) {
            mVideoEncoder.setFilterBitmap(mFilterBitmapHashMap,inputStream);
            Log.i(TAG,"in preapre render when gl render is not null");
        }


        OpenGLRenderer.removeStaticDrawables();
        handleSetFilter(mCurrentFilter, true);


        mRendererPrepared = true;
    }

    public void destroyVideoSurface() {
        SurfaceTexture prevSurfaceTexture = OpenGLRenderer.getSurfaceTexture(this);
        if(prevSurfaceTexture != null) {
            Log.d(TAG, "Releasing previous SurfaceTexture");
            prevSurfaceTexture.release();
        }
    }

    //Dharmendra.verma@instalively.com
    //create the bitmap from the pkm data file
    public Bitmap createBitmapFormRawData(int resourceId,Context context){
        int w = 1024;
        int h = 1024;
        int nP = w * h; //no. of pixels

        //load the binary data
        byte[] byteArray = new byte[nP];
        try {
            InputStream fis = context.getResources().openRawResource(resourceId);
            fis.read(byteArray);
            fis.close();
        } catch(IOException e) {
            // Ignore.
        }

        System.out.println(byteArray.length);

        //convert byte to int to work with createBitmap (is there a better way to do this?)
        int[] intArray = new int[nP];
        for (int j=0; j < nP; j++)
        {
            intArray[j] = byteArray[j];
        }

        //create bitmap from intArray and send to texture
        Bitmap bitmap= Bitmap.createBitmap(intArray, w, h, Bitmap.Config.ARGB_8888);
        System.out.println(bitmap.getWidth() + "" +bitmap.getHeight());
        return bitmap;
    }

    //Dharmendra.verma@instalively.com
    //to createBitmap at the oncreatesurface state
    public void createFilterBitmap(){
        Bitmap mFilterBitmap;
        for(int i=0;i<mFilterBitmapCount;i++){
            if(i==12){
                inputStream = mContext.getResources().openRawResource(mBitmapResources[i]);
            } else {
                mFilterBitmap = BitmapFactory.decodeResource(mContext.getResources(), mBitmapResources[i]);
                mFilterBitmapHashMap.put(i,mFilterBitmap);
            }

        }
    }

    public void createVideoSurface() {
        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        // call createSurfaceTexture statically here,because if we're not rendering to display, mGLRenderer might be null.
        mSurfaceTexture = OpenGLRenderer.createSurfaceTexture(this);
        mCache = OpenGLRenderer.getCacheInstance(this);
        Log.d(TAG, "Creating a new External SurfaceTexture for video");
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurfaceTextureReleased = false;
    }

    @Override
    protected void initScene() {
//        mTextureManager.reset();
        LoaderOBJ objParser = new LoaderOBJ(this, R.raw.bottle_obj);
//        LoaderFBX objParser = new LoaderFBX(this, R.raw.ty);
/*        try {
            objParser.parse();
            mObjectGroup = objParser.getParsedObject();
            mObjectGroup.isContainer(false);
            Log.d(TAG, "VECTOR: " + mObjectGroup.getScale().toString());
            Log.d(TAG, "position: " + mObjectGroup.getPosition().toString());
            mObjectGroup.setPosition(0.0, 0.0, -5.0);
            mObjectGroup.setScale(0.12);
            {
                Material material = new Material();
                material.setSpecularMethod(new SpecularMethod.Phong());
                material.setDiffuseMethod(new DiffuseMethod.Lambert());
//            material.addTexture(new Texture("earthColors", R.drawable.broadcaste_screen_filter_none));
                material.setColor(Color.argb(200, 250, 100, 100));
                material.setColorInfluence(4.7f);
                material.enableLighting(true);
                mObjectGroup.setMaterial(material);
            }
            mObjectGroup.setRotation(1.0f, 0.0f, 0.0f, 90);
            getCurrentScene().addChild(mObjectGroup);

*//*            getCurrentScene().addChild(mObjectGroup.getChildAt(0));
            getCurrentScene().addChild(mObjectGroup.getChildAt(1));*//*
            int childCount = mObjectGroup.getNumChildren();
            for(int i = 0; i < childCount; i++) {
                Log.d(TAG, "OBJGroup Child: " + mObjectGroup.getChildAt(i).getName());
*//*                if(i == 1) {
                    mObjectGroup.getChildAt(i).rotate(1, 0, 0, 90);
                    mObjectGroup.getChildAt(i).setPosition(0.0, 10, 0);
                    mObjectGroup.getChildAt(i).setScale(0.5);
                    Log.d(TAG, "child geometry: " + mObjectGroup.getChildAt(i).getGeometry().toString());
                    Log.d(TAG, "child vol: " +mObjectGroup.getChildAt(i).getTransformedBoundingVolume());
                    Log.d(TAG, "child pos: " + mObjectGroup.getChildAt(i).getPosition());
                    Log.d(TAG, "child scale: " + mObjectGroup.getChildAt(i).getScale());
                }*//*
//                mObjectGroup.getChildAt(i).setPosition(1.5*i, 1.7*i, 0);
            }*/

            translateAnimation = new TranslateAnimation3D(new Vector3(0.0, 0, 0.0), new Vector3(1.0, 0, 0.0));
            translateAnimation.setRepeatMode(Animation.RepeatMode.REVERSE_INFINITE);
            translateAnimation.setDurationMilliseconds(2500);
            translateAnimation.setTransformable3D(mObjectGroup);

            mCameraAnim = new RotateOnAxisAnimation(Vector3.Axis.Z, 360);
            mCameraAnim.setDurationMilliseconds(8000);
            mCameraAnim.setRepeatMode(Animation.RepeatMode.INFINITE);
//            mCameraAnim.setTransformable3D(mObjectGroup);
        /*} catch (ParsingException e) {
            e.printStackTrace();
        }*//* catch (ATexture.TextureException e) {
            e.printStackTrace();
        }*/

        light = new DirectionalLight(1.0f, 0.4f, -0.5f); // set the direction
        light.setColor(1.0f, 0.4f, 0.3f);
        light.setPower(8.0f);
        spotLight = new SpotLight(-1f, 0.2f, -1.0f);
        spotLight.setColor(Color.WHITE);
        spotLight.setFalloff(0.7f);
        spotLight.setPower(1.6f);

        mLightAnim = new EllipticalOrbitAnimation3D(new Vector3(0,0,-2),
                new Vector3(7, 10, 4), Vector3.getAxisVector(Vector3.Axis.X), 0,
                360, EllipticalOrbitAnimation3D.OrbitDirection.CLOCKWISE);
        mLightAnim.setRepeatMode(Animation.RepeatMode.INFINITE);
        mLightAnim.setDurationMilliseconds(3500);
        mLightAnim.setTransformable3D(light);

/*        try {*/
            Material material = new Material();
            material.setSpecularMethod(new SpecularMethod.Phong());
            material.setDiffuseMethod(new DiffuseMethod.Lambert());
//            material.addTexture(new Texture("earthColors", R.drawable.broadcaste_screen_filter_none));
            material.setColor(Color.argb(200, 250, 100, 100));
            material.setColorInfluence(0.7f);
            material.enableLighting(true);
//            sphere = new Cylinder(10, 5f, 24, 24);

//            sphere = new Sphere(0.5f, 100, 100);
//            sphere = new Cube(1.0f);
        sphere = new NPrism(12, 0.1, 0.5f, 0.96, 0.8);
        defaultPos = new Vector3(0.25f, 0.3f, 0f);
            sphere.setPosition(0.25f, 0.3f, 2f);
            sphere.setScale(1);
//        sphere.rotate(1, 0, 0, 90);
            sphere.setMaterial(material);
            Torus torus = new Torus(0.4f, 0.2f, 80, 40);
            Material material2 = new Material();
//            material2.addTexture(new Texture("kuchbhi", R.drawable.slide_4));
            material2.setColorInfluence(0.7f);
            material2.setColor(Color.argb(255, 100, 255, 0));
            torus.setMaterial(material2);
            torus.setPosition(0.1f, 0.2f, 0.2f);
            getCurrentScene().addLight(light);
            getCurrentScene().addLight(spotLight);
            final List<ALight> lights = new ArrayList<>();
            lights.add(light);
            getCurrentScene().addChild(sphere);
           // getCurrentScene().addChild(torus);
        /*} catch (ATexture.TextureException e){
            e.printStackTrace();
        }*/
        getCurrentScene().registerAnimation(translateAnimation);
        getCurrentScene().registerAnimation(mCameraAnim);
        getCurrentScene().registerAnimation(mLightAnim);

        //translateAnimation.play();
//        mCameraAnim.play();
        mLightAnim.play();
        Log.d(TAG, "3D Render Scene created");
        configureCamera();
    }

    private void configureCamera() {
        Camera camera = getCurrentCamera();
        setCameraPosition(camera);
        setCameraLookAt(camera);
        setCameraProjection(camera);
    }

    private void setCameraProjection(Camera camera) {
        camera.setProjectionMatrix(new Matrix4(mGLRenderer.getProjectionMatrix()));
    }

    private void setCameraLookAt(Camera camera) {
        camera.setLookAt(new Vector3(doubleArray(mGLRenderer.getCameraLookAt())));
    }

    private void setCameraPosition(Camera camera) {
        camera.setPosition(new Vector3(doubleArray(mGLRenderer.getCameraPosition())));
    }

    // Called from a thread bound to the shared EGLContext. So safe to call any GLES/EGL APIs
    private void startVideoRendering() {
        destroyVideoSurface();
        createVideoSurface();

        // To Do
        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        boolean recordingEnabled = mVideoEncoder.isRecording();
//        Log.d(TAG, "mRecordingEnabled queried from Video Encoder: " + (mRecordingEnabled ? "true" : "false"));
        if (recordingEnabled && mRecordingStatus == RECORDING_ON) {
            Log.d(TAG, "Recording Resumed in Surface Created");
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            Log.d(TAG, "Recording Starting from scratch in Surface Created");
            mRecordingStatus = RECORDING_OFF;
        }

        if (mGLRenderer != null) {
            Log.d(TAG, "Add external source drawable as video");
            mGLRenderer.addExternalSourceDrawable(false); //false for the video, true for the camera.
            setFilter(mCurrentFilter, true);
        }

        mPlayerCallbackListener.playbackStarted(mSurfaceTexture);
    }

    protected void prepareForVideoRendering() {
        Log.d(TAG, "Preparing Dummy Renderer to handle textures, once done, encoding will be initiated");
        Log.d(TAG, "mUseOwnContext :: " + (mUseOwnContext ? "true" : "false") + " surfaceCreated: " + (mSurfaceCreated ? "true" : "false"));
        // If Surface hasn't been created yet, just set this flag to true, so that on
        // Surface creation, the video encoding starts by checking this flag.
        mVideoDecode = true;
        if(!mUseOwnContext && !mSurfaceCreated)  return; //Do not start encoding, until the Surface has been created.

        if (mDummyRenderer != null) {
            mDummyRenderer.release();
            mDummyRenderer.prepareCameraCapture(mUseOwnContext ? null : mSharedEGLContext);
        }
//        try {
//                throw new Exception();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void requestRender() {
        mDummyRenderer.frameAvailable();
        //        mGLSurfaceView.requestRender();
/*
        mVideoEncoder.frameAvailable();
*/
    }

    /**
     * Request Renderer to update frame (draw a new one)
     * @param updateFrame Pass true to go via DummyRenderer
     * (which will do updateTexImage and signal us back again)
     * Passing false will simple render the drawable tree
     */
    public void requestRender(boolean updateFrame) {
//        Log.d(TAG, "requestRender :: mRenderPause is " + (mRenderPause ? "true" : "false"));
        if(mRenderPause) return;
        if(updateFrame) {
            mDummyRenderer.frameAvailable();
        } else {
            if((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) {
// Render to display
                mGLSurfaceView.requestRender();
            }
            if((mRenderTarget & RENDER_TARGET_VIDEO) > 0) {
// Render to video
                // Tell the video encoder thread that a new frame is available.
                // This will be ignored if we're not actually recording.
                mVideoEncoder.frameAvailable(/*mSTMatrix, mTimeStamp*/);
            }
// Render to off screen
/*        if (mCaptureEnabled) {
            mCameraCapturer.frameAvailable(mSTMatrix, mTimeStamp);
        }*/
        }
    }

    public void requestRender(boolean updateFrame, long timestampNs) {
        if(updateFrame) {
            mDummyRenderer.frameAvailable(timestampNs);
        } else {
            //We're forcing the timestamp.
            mCache.mSurfaceTextureTimestamp = timestampNs;
// Render to display
            if((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) {
                mGLSurfaceView.requestRender();
            }
// Render to video
            // Tell the video encoder thread that a new frame is available.
            // This will be ignored if we're not actually recording.
            mVideoEncoder.frameAvailable(/*mSTMatrix, mTimeStamp*/);

// Render to off screen
/*        if (mCaptureEnabled) {
            mCameraCapturer.frameAvailable(mSTMatrix, mTimeStamp);
        }*/
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        if(VERBOSE) {
            Log.d(TAG, "Sending frame to dummy renderer for texImage update with timestamp: " + mFrameTimestampNs);
        }
        if (!MoviePlayer.CODEC_RENDERER_COMPAT_MODE && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
            requestRender(true);
        } else {
            requestRender(true, mFrameTimestampNs);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);

        if(((mRenderTarget & RENDER_TARGET_DISPLAY) > 0) || mPlaybackStopped) {
            mSurfaceSetupDone = true;
        } else {
            return;
        }
        // Incase setCameraPreviewSize is called before this (i.e. onSurfaceChanged), so we wouldn't have passed Camera Preview size to the GLRenderer.
        // Now is the time for that.
//        setCameraPreviewSize(mIncomingWidth, mIncomingHeight);
//        mZoomLevel = 1.0f;
        handleSetRenderParams(width, height);
        //Call this after GL setup (camera) has been done by Renderer (via handleSetRenderParams)
//        onRenderSurfaceSizeChanged(gl, width, height);

        mController.rendererReady();
        if (!mIsPlayImageStream) {
            handleAddDrawable(0.0f, 0.0f, 1.0f, 1.0f, mImageBackground);
            requestRender(false);
        } else {
                requestPrepareVideoRendering();
            }
        }

    public void requestPrepareVideoRendering() {
        mVideoDecode = true;
        if(mSurfaceSetupDone && mVideoDecode) {
            prepareForVideoRendering();
        }
    }

    public void onSurfaceDestroyed() {
        //Passing SurfaceTexture as null, as it is anyway not being used
        onRenderSurfaceDestroyed(null);
        //TODO replace releaseRenderer with handleReleaseRenderer
        //TODO which directly executes in the current thread which is most definitely the GLThread.
        releaseRenderer();
    }

/*    private void setRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;

        if (mGLRenderer != null) {
            //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
            mGLRenderer.setRenderParams(mPreviewWidth, mPreviewHeight);
        }
*//*        //Video encoder setRenderParams should be triggered after PReview GL Renderer's,
        // because that's what sets the parameters required by the Encoder's renderer.
        mVideoEncoder.setPreviewRenderParams(width, height);

        if (mCameraCapturer != null) {
            mCameraCapturer.setPreviewRenderParams(width, height);
        }*//*
    }*/


    @Override
    public void onDrawFrame(GL10 gl) {
/*        if(mVideoDecode) {
            updateTextureImage();
        }*/


        //Check again whether we're really supposed to render (from the uncontrolled calls to onDrawFrame)
        if((mRenderTarget & RENDER_TARGET_DISPLAY) > 0){
            if (VERBOSE) Log.d(TAG, "onDrawFrame ");

            synchronized (SurfaceRenderer.mRenderFence) {
                if(mGLRenderer == null) return;
                mGLRenderer.setFilterBitmapRenderer(mFilterBitmapHashMap,inputStream);
                mGLRenderer.drawFrame();
                GLES20.glFinish();
//		        onRenderFrame(gl);
            }
//	        changeScene();
        }

        if (mOverlayView != null && mFirstFrame++==1) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mGLSurfaceView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            mOverlayView.setImageDrawable(null);
                            if(mOverlayView != null) {
                                mOverlayView.setVisibility(View.GONE);
                                mOverlayView = null;
                            }
                        }
                    }, 100);
                }
            });
            mFirstFrame = 0;
        }

        if (mImageCaptureRequest) {
            Bitmap bitmap = captureBitmap();
            mController.onPictureTakenFinished(bitmap);
            mImageCaptureRequest = false;
        }
    }


    private void changeScene() {
    //    spotLight.setX((1-x)/2);
    //    spotLight.setLookAt(x/2, 0, 0);
        //sphere.setX(x);
//        light.rotate(0.0f, 1.0f, 0.0f, 1f);
//        light.rotate(0.0f, 0.0f, 1.0f, 1f);

/*        light.setX(x);
        light.setLookAt((1-x)/2.0, 0.3f, -1.2f);*/
        if(maxReached) {
            x -= 0.01;
            if(x <= -0.0f) {
//                            minReached = true;
                maxReached = false;
            }
        } else {
            x += 0.01;
            if(x >= 1.0f) {
                maxReached = true;
//                            minReached = false;
            }
        }

        try {
            double width = pos.z; //0.5f; //screen %age width
            float projectionDist = mGLRenderer.getCameraPosition()[2];

            pos.x = 0.5f;
            pos.y = 0.5f;
            pos.z = 0.7f;
            Vector3 position = screenToWorld((float) pos.x * mPreviewWidth, (float) pos.y * mPreviewHeight, mPreviewWidth, mPreviewHeight, projectionDist, (float)width);

//            sphere.setScreenCoordinates((float)pos.x * mPreviewWidth, (float) pos.y * mPreviewHeight, mPreviewWidth, mPreviewHeight, projectionDist);
//            Vector3 position = sphere.getPosition().clone();

            sphere.setDoubleSided(true);
/*            sphere.rotate(1, 0, 0, 1.5);
            sphere.rotate(0, 1, 0, 0.3);
            sphere.rotate(0, 0, 1, 0.3);*/

            Log.d(TAG, "width: " + (float) width + " final position: " + (float) position.x + " " + (float) position.y + " " + (float) position.z);
            sphere.setPosition(position);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static double[] doubleArray(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    public static float[] floatArray(double[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float) input[i];
        }
        return output;
    }

    private Vector3 screenToWorld(float x, float y, int viewportWidth, int viewportHeight, float projectionDepth) {
        return screenToWorld(x, y, viewportWidth, viewportHeight, projectionDepth, 1.0f);
    }

    private Vector3 screenToWorld(float x, float y, int viewportWidth, int viewportHeight, float projectionDepth, float width)
    {
        float[] r1 = new float[16];
        int[] viewport = new int[] { 0, 0, viewportWidth, viewportHeight};

        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];

        Matrix.setIdentityM(modelMatrix, 0);

        Matrix.setIdentityM(modelViewMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, mGLRenderer.getViewMatrix(), 0, modelMatrix, 0);

        GLU.gluUnProject(x, viewportHeight - y, 0.0f, modelViewMatrix, 0, mGLRenderer.getProjectionMatrix(), 0, viewport, 0, r1, 0);
        // Since model matrix is identity, ModelView should be same as View matrix.
//        GLU.gluUnProject(x, viewportHeight - y, 0.0f, doubleArray(mGLRenderer.getViewMatrix()), 0, doubleArray(mGLRenderer.getProjectionMatrix()), 0, viewport, 0, r1, 0);
//        GLU.gluUnProject(x, viewportHeight - y, 0.0f, modelMatrix, 0, doubleArray(mGLRenderer.getProjectionMatrix()), 0, viewport, 0, r1, 0);

        //take the normalized vector from the resultant projection and the camera, and then project by the desired distance from the camera.
        Vector3 result = new Vector3(r1[0], r1[1], r1[2]);
//        result.z = -result.z;
        Log.d(TAG, "result: " + result.toString());
        Vector3 cameraPos = getCurrentCamera().getPosition().clone();
        result.subtract(cameraPos);
        Log.d(TAG, "result: camera position: " + cameraPos.toString());
        Log.d(TAG, "result: offset from camera: " + result.toString());
        result.normalize();
        result.multiply(projectionDepth / width);
//        result.z /= width;
        result.add(cameraPos);
        Log.d(TAG, "final result: " + result.toString());
        return result;
    }

    /**
     * If we choose not to use DummySurfaceRenderer (which updates Tex Image in a separate thread
     * from all rendering threads). This is where we update tex image.
      */
    private void updateTextureImage() {
        synchronized(SurfaceRenderer.mRenderFence) {
            synchronized (TextureMovieEncoder.mRenderFence) {
                mSurfaceTexture.updateTexImage(); // update surfacetexture if available
            }
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mTimeStamp = mSurfaceTexture.getTimestamp();

        mCache.mSurfaceTextureTransform = mSTMatrix;
//        mGLRenderer.setExternalTextureTransformMatrix(mSTMatrix);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCache.mSurfaceTextureTimestamp = mTimeStamp;
        }
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p/>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;

        if (mVideoWidth < 0 || mVideoHeight < 0) {
            Log.w(TAG, "Incoming width/height < 0. So returning");
            return;
        }
        Log.d(TAG, "setCameraPreviewSize");
        // Ideally This needs to be called after onSurfaceCreated.
        // In case this gets triggered before onSurfaceCreated (which is where we actually create GLRenderer and prepare all Buffers (Caches.java)
        // We simply save the camera preview parameters  (mostly width and height), which we
        // use it later in onSurfaceChanged where we would have already created GLRenderer who's
        // the one using these parameters
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetCameraPreviewSize(mVideoWidth, mVideoHeight);
            }
        });
        mVideoEncoder.setCameraParams(mVideoWidth, mVideoHeight);
    }

    private void handleSetCameraPreviewSize(int mVideoWidth, int mVideoHeight) {
        if (mGLRenderer != null) {
            Log.d(TAG, "handleSetCameraPreviewSize :: " + mVideoWidth + "x" + mVideoHeight);
            mGLRenderer.setCameraParams(mVideoWidth, mVideoHeight);
        }
    }

    public void setRenderParams() {
        setRenderParams(mPreviewWidth, mPreviewHeight);
    }

    public void setRenderParams(int width, int height) {
        mPreviewWidth = width;
        mPreviewHeight = height;

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                handleSetRenderParams(mPreviewWidth, mPreviewHeight);
            }
        });
    }

    private void handleSetRenderParams(int width, int height) {
        Log.d(TAG, "handleSetRenderParams :: " + width + "x" + height);
        mPreviewWidth = width;
        mPreviewHeight = height;

        if (mGLRenderer != null) {
            //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
            mGLRenderer.setRenderParams(mPreviewWidth, mPreviewHeight);
        }

        //Video encoder setRenderParams should be triggered after PReview GL Renderer's,
        // because that's what sets the parameters required by the Encoder's renderer.
        mVideoEncoder.setPreviewRenderParams(width, height);

/*        if (mCameraCapturer != null) {
            mCameraCapturer.setPreviewRenderParams(width, height);
        }*/
    }

    public void setController(StreamingActionController controller) {
        mController = controller;
    }

    public void setContext(Context context) {
        mContext = context;
//        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.slide_4);
////        OpenGLRenderer.addBitmap(bitmap, true);
//        OpenGLRenderer.addBitmapDrawable(true, bitmap);
    }

    public void setPlayerCallbackListener(MoviePlayer.PlayerFeedback playerCallbackListener) {
        mPlayerCallbackListener = playerCallbackListener;
    }

    public void setTargetView(GLSurfaceView targetView) {
        mGLSurfaceView = targetView;
    }

    public void setZoom(float zoom) {
        float scale = zoom * mZoomLevel;

        mZoomLevel = Math.max(1.0f, Math.min(scale, 3.0f));
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final String text) {
        Bitmap bitmap = createTextBitmap(text, 50);
        Log.d(TAG, "Created Text bitmap of size : " + bitmap.getWidth() + " x " + bitmap.getHeight());
        addDrawable(x, y, scaleX, scaleY, bitmap);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final Bitmap bitmap) {
        addDrawable(x, y, scaleX, scaleY, bitmap, false);
    }

    public void handleAddDrawable(final float x, final float y, final float scaleX, final float scaleY, final Bitmap bitmap) { //To be called from GL Thread only
        if (bitmap == null) {
            Log.e(TAG, "Bitmap to be added is null");
            return;
        }

        if (mGLRenderer != null) {
            Log.d(TAG, "finally addding new drawable 2");
            mGLRenderer.addAnimatableBitmapDrawable(x, y, scaleX, scaleY, bitmap, true);
        }
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final Bitmap bitmap, boolean onlyEncoder) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap to be added is null");
            return;
        }
        if(!onlyEncoder) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "finally addding new drawable 1");
                    if (mGLRenderer != null) {
                        Log.d(TAG, "finally addding new drawable 2");
                        mGLRenderer.addAnimatableBitmapDrawable(x, y, scaleX, scaleY, bitmap, true);
                    }
                }
            });
        }
        Drawable2d drawable = OpenGLRenderer.createBitmapDrawable(bitmap);
        drawable.setScaleType(Drawable2d.SCALE_TYPE_INSIDE);
        drawable.setTranslate(new float[]{x, y, 0.0f});
//        drawable.setScale(scaleX, (scaleY * bitmap.getHeight()) / bitmap.getWidth(), 1.0f);
        OpenGLRenderer.addStaticBitmapDrawable(0.0f, 0.0f, 9.0f*9/16/16, 1.0f, bitmap, OpenGLRenderer.DRAWABLES_OVERLAY);
        mVideoEncoder.addDrawable(OpenGLRenderer.DRAWABLES_OVERLAY, drawable);
        Log.d(TAG, "Adding overlay drawable at " + x + "x" + y);
    }

    public void addDrawable(final float x, final float y, final int resId) {
        Log.d(TAG, "adding new drawable");

        final Bitmap bitmap = (OpenGLRenderer.getBitmap(resId) == null) ? BitmapFactory.decodeResource(mContext.getResources(), resId) : OpenGLRenderer.getBitmap(resId);
        if (OpenGLRenderer.getBitmap(resId) == null) {
            OpenGLRenderer.addBitmap(resId, bitmap);
        }
        final float scaleX = 0.3f, scaleY = 0.3f;
        final float X = 2 * (x / mPreviewWidth) - 1.0f;
        final float Y = 1.0f - 2.0f * (y / mPreviewHeight);

        addDrawable(X, Y, scaleX, scaleY, bitmap);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final int resId) {
        addDrawable(x, y, scaleX, scaleY, resId, false, false);
    }

    public void addDrawable(final float x, final float y, final float scaleX, final float scaleY, final int resId, boolean isNormalized, boolean onlyEncoder) {
        Log.d(TAG, "adding new drawable");
//        Log.d(TAG, "x: " + x + " y: " + y + " scaleX: " + scaleX + " scaleY: " + scaleY);
        final Bitmap bitmap = (OpenGLRenderer.getBitmap(resId) == null) ? BitmapFactory.decodeResource(mContext.getResources(), resId) : OpenGLRenderer.getBitmap(resId);
        if(OpenGLRenderer.getBitmap(resId) == null) {
            OpenGLRenderer.addBitmap(resId, bitmap);
        }
        final float X, Y, ScaleX, ScaleY;
        if(!isNormalized) {
            X = 2 * (x / mPreviewWidth) - 1.0f;
            Y = 1.0f - 2.0f * (y / mPreviewHeight);
            ScaleX = scaleX / mPreviewWidth;
            ScaleY = ScaleX;
        } else {
            X = 2 * x - 1.0f;
            Y = 1.0f - 2.0f * y;
            ScaleX = scaleX;
            ScaleY = ScaleX;
        }
//        Log.d(TAG, "Final x: " + X + " y: " + Y + " scaleX: " + ScaleX + " scaleY: " + ScaleY);
        addDrawable(X, Y, ScaleX, ScaleY, bitmap, onlyEncoder);
    }

    public void addDrawable(String text) {
        Bitmap bitmap = createTextBitmap(text, 50);
        Log.d(TAG, "Created Text bitmap of size : " + bitmap.getWidth() + " x " + bitmap.getHeight());
        addDrawable(0.5f, 0.5f, 1.0f, 1.0f, bitmap);
    }
    private Bitmap createTextBitmap(String text, int textSize) {
        // Get text dimensions
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG
                | Paint.LINEAR_TEXT_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        // Create bitmap and canvas to draw to
        Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        // Draw background
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG
                | Paint.LINEAR_TEXT_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(50, 20, 20, 0));
        c.drawPaint(paint);

        // Draw text
        c.drawText(text, 0, text.length(), bounds.width()/2, bounds.height(), textPaint);

        return b;
    }

    Drawable2d mPickedDrawable = null;

    public void moveDrawable(float x, float y) {
        final float X = 2 * (x / mPreviewWidth)/* - 1.0f*/;
        final float Y = /*1.0f*/ - 2.0f * (y / mPreviewHeight);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    if (mPickedDrawable != null) {
                        mPickedDrawable.translate(X, Y);
                    }
                }
            }
        });
    }

    public void pickDrawable(float x, float y) {
        final float X = 2 * (x / mPreviewWidth) - 1.0f;
        final float Y = 1.0f - 2.0f * (y / mPreviewHeight);
        Log.d(TAG, "Query drawable at " + X + " " + Y);
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    Drawable2d drawable = mGLRenderer.pickDrawable(new PointF(X, Y));
                    if (drawable == null) {
                        Log.w(TAG, "No drawable returned");
                        return;
                    }
                    mPickedDrawable = drawable;
                }
            }
        });
    }

    public void handleNotifyPausing() {
        synchronized (MoviePlayer.mDecoderLock) {
            synchronized (SurfaceRenderer.mRenderFence) {
                Log.d(TAG, "handlenotifyPausing");
                releaseRenderer();
                mSurfaceTextureReleased = true;
                if (mSurfaceTexture != null) {
//                            mSurfaceTexture.release();
                    mSurfaceTexture = null;
                }
            }
        }
    }

    public void notifyPausing() {
        mRenderPause = true;
        Log.d(TAG, "notifyPausing");
//        Log.d(TAG, "mRenderPause made true");
        //mSurfaceTextureReleased = false;
/*        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {*/
            handleNotifyPausing();
            synchronized (MoviePlayer.mDecoderLock) {
//            MoviePlayer.mDecoderLock.notifyAll();
            }
/*            }
        });*/
    }

    public void pause() {
        mDummyRenderer.notifyPausing();
    }

/*    public void pause() {
        notifyPausing();
        if(MoviePlayer.mDecoderLock == null) return;
        synchronized (MoviePlayer.mDecoderLock) {
            Log.d(TAG, "pause" + "mSurfaceTextureReleased: " + mSurfaceTextureReleased);
*//*            while(!mSurfaceTextureReleased) {
                try {
                    Log.d(TAG, "pause wait" + "mSurfaceTextureReleased: " + mSurfaceTextureReleased);
                    MoviePlayer.mDecoderLock.wait(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*//*
        }
    }*/

    void destroyRenderer() {
        if (mGLRenderer != null) {
            mGLRenderer.destroy();
            mGLRenderer = null;
        }
    }

/*
    @Override
    public void onEncoderCreated() {
        mVideoEncoder.getEncoder().setCallback(this);
    }

    @Override
    public void onVideoNal(byte[] bArr, long j, long j2) {
        //mRecordingMuxer.postFrame();
    }

    @Override
    public void onVideoError(String e) {

    }*/

    public void setDestFilePath(String destFilePath) {
        mDestFilePath = destFilePath;
    }

    public void setInitialTime(long initialTime) {
        this.initialTime = initialTime;
    }

    public void setVideoBackground(Bitmap videoBackground) {
        this.videoBackground = videoBackground;
    }

    public void setVideoBackground(float[] colors) {


    }

    public void setCaptureEnabled(boolean captureEnabled) {
        this.captureEnabled = captureEnabled;
    }

    public Rect getTransformedFocusRect(int x, int y, int[] focusIntensity) {
        return null;
    }

    public void setCameraCapturer(CameraCapture cameraCapturer) {
        this.cameraCapturer = cameraCapturer;
        this.cameraCapturer.setRenderController(this);
    }

    public void setDummyRenderer(DummySurfaceRenderer dummyRenderer) {
        mDummyRenderer = dummyRenderer;
        mDummyRenderer.setRenderController(this);
    }

    private void changeEncoderState() {
//        Log.d(TAG, "changeEncoderState");
        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
//        Log.d(TAG, "changeEncoderState, mRecordingEnabled: " + (mRecordingEnabled ? "true" : "false") + " mRecordingStatus:" + mRecordingStatus +  "renderer instance: " + this);
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // start recording
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            mVideoWidth,
                            mVideoHeight,
                            this.mVideoBitrate,
                            mVideoFPS,
                            mSharedEGLContext,
                            false));

                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoEncoder.updateSharedContext(mSharedEGLContext);

                    removeAnimatableDrawables();
//                    addOverlays(mThemeStickers);

                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else

        {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }
    }

        public void addOverlays(ArrayList<ThemeModel> themeModels) {
    }

    private void removeAnimatableDrawables() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    //Let this crash if mGLRenderer is null. There is nothing to live for anyway.
                    mGLRenderer.removeDrawables(OpenGLRenderer.DRAWABLES_ANIMATABLE);
                }
            }
        });
        //Video encoder setRenderParams should be triggered after PReview GL Renderer's,
        // because that's what sets the parameters required by the Encoder's renderer.
        mVideoEncoder.removeDrawables(OpenGLRenderer.DRAWABLES_ANIMATABLE);
    }

    public void setFilter(final int filterMask, final boolean enable) {
        mCurrentFilter = filterMask;
        mVideoEncoder.setFilter(filterMask);

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mGLRenderer != null) {
                    mGLRenderer.setExternalTextureFilter(filterMask, enable);
                }
            }
        });
    }

    public void handleSetFilter(final int filterMask, final boolean enable) { //To be called from GLThread only
        mCurrentFilter = filterMask;
        mVideoEncoder.setFilter(filterMask);

        if (mGLRenderer != null) {
            mGLRenderer.setExternalTextureFilter(filterMask, enable);
        }
    }

    public void setCaptureFilter(int captureFilter) {
        this.captureFilter = captureFilter;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        synchronized (this) {
            if(VERBOSE) {
                Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
            }
            mRecordingEnabled = isRecording;
        }
        // If playback is going on, then the next call to render will take care of changing Encoder state (ending mostly)
        // Otherwise, we take matters in our hands.
        if(mPlaybackStopped) {
            changeEncoderState();
        }
    }

    public void changeBitrate(int i) {

    }

    public void setIsFlipped(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    public void setCameraChanged(boolean cameraChanged) {
        this.cameraChanged = cameraChanged;
    }

    public void flagForReset() {

    }

    public void releaseRenderer() {
        destroyRenderer();
        if(mVideoEncoder != null) {
            mVideoEncoder.destroyRenderer();
        }
    }

    public void destroy() {
        releaseRenderer();
        mController = null;
        mContext = null;
        releaseFilterBitmap();
    }

    public void releaseFilterBitmap(){
        for(int i=0;i<mFilterBitmapHashMap.size();i++){
            if(mFilterBitmapHashMap.get(i)!=null){
                mFilterBitmapHashMap.get(i).recycle();
                mFilterBitmapHashMap.put(i,null);
            }
        }
    }

    public void frameAvailable() {
    }

    public void stopCameraSwapAnimation(Bitmap bitmap) {

    }

    @Override
    public void contextReady(EGLContext eglContext) {
        Log.d(TAG, "Got Context ready callback from dummy renderer" + " Context: " + eglContext);
        mSharedEGLContext = mEGLContext = eglContext;
        if(mIsPlayImageStream) {
            startVideoRendering();
        }
    }

    @Override
    public void textureAvailable() {
        if(VERBOSE) {
            Log.d(TAG, "Got render callback from dummy renderer after updateTexImage");
        }
        changeEncoderState();
        requestRender(false);
    }

    public void setFrameTimestamp(long timeStampNs) {
        mFrameTimestampNs = timeStampNs;
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    float scaleFactor = 1.0f;

    public void setPosX(float v) {
        pos.x = /*defaultPos.x + */scaleFactor * (v/* - 0.5*/);
//        sphere.setPosition(pos);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
    }

    public void setPosY(float v) {
        pos.y = /*defaultPos.y + */scaleFactor * (v/* - 0.5*/);
//        sphere.setPosition(pos);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
    }

    public void setPosZ(float v) {
        pos.z = 2 * /*defaultPos.z  + */(v/* - 0.5*/);
//        Log.d(TAG, "FINAL POSITION: " + pos.toString());
//        sphere.setPosition(pos);
    }

    public int decrementFilter() {

       // while (((mCurrentFilter-1)==3) || ((mCurrentFilter-1)==4) || ((mCurrentFilter-1)==7)) {  //Temporarily skipping Night, Vignette and Sketch
        //    --mCurrentFilter;
        // }
        if (((RecordingActionControllerKitkat)mController).mMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {

            if((mNewFilterOrder-1)==6){
                --mNewFilterOrder;
            }

            if (mNewCurrentFilter== 0) { // Clear screen to SlowMo
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(3);

                setFilter(0, true);
                mNewCurrentFilter = FilterManager.FILTER_COUNT+1;
                mCurrentFilter=mNewCurrentFilter;
                return mNewCurrentFilter;
            }
            else if (mNewCurrentFilter == (FilterManager.FILTER_COUNT+1)){ //SlowMo to FastForward
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(2);

                setFilter(0, true);
                mNewCurrentFilter =FilterManager.FILTER_COUNT;
                mCurrentFilter=mNewCurrentFilter;
                return mNewCurrentFilter;
            }
            else if (mNewCurrentFilter == FilterManager.FILTER_COUNT) { //FastForward to Toon
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(1);

                mNewFilterOrder=filterOrder.length;

                mNewCurrentFilter=filterOrder[--mNewFilterOrder];
                mCurrentFilter=mNewCurrentFilter;
                setFilter(mNewCurrentFilter, true);
                return mNewCurrentFilter;
            }
            mNewCurrentFilter=filterOrder[--mNewFilterOrder];
            mCurrentFilter=mNewCurrentFilter;
        }

        else{
            if(mNewFilterOrder==0){
                mNewFilterOrder=filterOrder.length;
            }
            mNewCurrentFilter=filterOrder[--mNewFilterOrder];
            mCurrentFilter=mNewCurrentFilter;
        }

        setFilter(mNewCurrentFilter, true);
        return mNewCurrentFilter;

        /*
        while ((mCurrentFilter-1) >=1 &&(mCurrentFilter-1)<=6 || ((mCurrentFilter-1)==11) || ((mCurrentFilter-1)==17) || ((mCurrentFilter-1)==15)){
>>>>>>> Stashed changes
             --mCurrentFilter;
        }

        if (((RecordingActionControllerKitkat)mController).mMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {

            if((mCurrentFilter-1)==13){
                --mCurrentFilter;
            }

            if (mCurrentFilter == 0) { // Clear screen to SlowMo
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(3);

                setFilter(0, true);
                mCurrentFilter = FilterManager.FILTER_COUNT+1;

                return mCurrentFilter;
            }
            else if (mCurrentFilter == FilterManager.FILTER_COUNT+1) { //SlowMo to FastForward
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(2);

                setFilter(0, true);
                mCurrentFilter = FilterManager.FILTER_COUNT;

                return mCurrentFilter;
            }
            else if (mCurrentFilter == FilterManager.FILTER_COUNT) { //FastForward to Toon
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(1);

                setFilter(--mCurrentFilter, true);
                return mCurrentFilter;
            }

        } else { // For image, no Fast-forward, slow-mo
            if(mCurrentFilter == 0) {
                mCurrentFilter = FilterManager.FILTER_COUNT;
            }
        }


        setFilter(mNewFilterOrder, true);
        return mNewFilterOrder;
        */
    }

    public int incrementFilter() {

        //while (((mCurrentFilter+1)==3) || ((mCurrentFilter+1)==4) || ((mCurrentFilter+1)==7)) {  //Temporarily skipping Night, Vignette and Sketch
       //     ++mCurrentFilter;
       // }
        if (((RecordingActionControllerKitkat)mController).mMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {

            if((mNewFilterOrder+1)==6){
                ++mNewFilterOrder;
            }

            if (mNewCurrentFilter == (filterOrder[filterOrder.length - 1])) { //Toon to FastForward
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(2);

                setFilter(0, true);
                mNewCurrentFilter = FilterManager.FILTER_COUNT;
                mCurrentFilter=mNewCurrentFilter;
                return mNewCurrentFilter;
            }
            else if (mNewCurrentFilter == FilterManager.FILTER_COUNT) { // FastForward to SlowMo
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(3);

                setFilter(0, true);
                mNewCurrentFilter = (FilterManager.FILTER_COUNT+1);
                mCurrentFilter=mNewCurrentFilter;
                return mNewCurrentFilter;
            }
            else if (mNewCurrentFilter == (FilterManager.FILTER_COUNT+1)) { //SlowMo to clear screen
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(1);

                setFilter(0, true);
                mNewFilterOrder=0;
                mNewCurrentFilter = 0;
                mCurrentFilter=mNewCurrentFilter;
                return mNewCurrentFilter;
            }
            mNewCurrentFilter=filterOrder[++mNewFilterOrder];
            mCurrentFilter=mNewCurrentFilter;
        }
        else{
            if(mNewFilterOrder==(filterOrder.length-1)){
                mNewFilterOrder=-1;
            }
            mNewCurrentFilter=filterOrder[++mNewFilterOrder];
            mCurrentFilter=mNewCurrentFilter;
        }
        setFilter(mNewCurrentFilter, true);
        return mNewCurrentFilter;
        /*
        while ((mCurrentFilter+1) >=1 && (mCurrentFilter+1)<=6 || ((mCurrentFilter+1)==11) || ((mCurrentFilter+1)==17) || ((mCurrentFilter+1)==15)){
>>>>>>> Stashed changes
            ++mCurrentFilter;
        }

        if (((RecordingActionControllerKitkat)mController).mMediaType == AppLibrary.MEDIA_TYPE_VIDEO) {

            if((mCurrentFilter+1)==13){
                ++mCurrentFilter;
            }

            if (mCurrentFilter == FilterManager.FILTER_COUNT - 1) { //Toon to FastForward
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(2);

                setFilter(0, true);
                mCurrentFilter = FilterManager.FILTER_COUNT;

                return mCurrentFilter;
            }
            else if (mCurrentFilter == FilterManager.FILTER_COUNT) { // FastForward to SlowMo
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(3);

                setFilter(0, true);
                mCurrentFilter = FilterManager.FILTER_COUNT+1;

                return mCurrentFilter;
            }
            else if (mCurrentFilter == FilterManager.FILTER_COUNT + 1) { //SlowMo to clear screen
                ((RecordingActionControllerKitkat) mController).toggleVideoPlaybackSpeed(1);

                setFilter(0, true);
                mCurrentFilter = 0;

                return mCurrentFilter;
            }

        } else { // For image, no Fast-forward, slow-mo
            if (mCurrentFilter >= FilterManager.FILTER_COUNT-1) {
                mCurrentFilter = -1;
            }
        }

        setFilter(mNewFilterOrder, true);
        return mNewFilterOrder
        */
    }

    /**
     * Must be called be GLThread.
     * @return
     */
    public Bitmap captureBitmap() {
        return mGLRenderer.createBitmapFromGLSurface();
    }

    public void requestCaptureBitmap() {
        mImageCaptureRequest = true;
        mGLSurfaceView.requestRender();
    }

    public void setIsPlayImageStream(boolean isPlayImageStream) {
        mIsPlayImageStream = isPlayImageStream;
    }

    public void setImageBackground(Bitmap imageBackground) {
        mImageBackground = imageBackground;
/*        if(!mIsPlayImageStream) {
            mGLSurfaceView.setBackground(new BitmapDrawable(mContext.getResources(), mImageBackground));
        }*/
    }

    public void setOverlayView(ImageView imageView) {
        mOverlayView = imageView;
    }

    public void setPlaybackStopped(boolean playbackStopped) {
         mPlaybackStopped = playbackStopped;
    }
}
