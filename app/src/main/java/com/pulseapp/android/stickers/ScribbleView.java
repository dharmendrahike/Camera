package com.pulseapp.android.stickers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by deepankur on 17/2/16.
 */
public class ScribbleView extends View implements ScaleGestureDetector.OnScaleGestureListener {
//    private Bitmap mBitmap;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    ArrayList<PaintData> pathPaintArrayList;
    ArrayList<float[]> mSprayPoints;
    private boolean mDrawingAllowed = true;
    private boolean neonModeActivated = false;

    public void setDrawingEnabled(boolean enabled) {
        mDrawingAllowed = enabled;
    }

    public ScribbleView(Context context) {
        super(context);
        init();
    }

    private void init() {

        mPath = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.parseColor("#f40032"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaintArrayList = new ArrayList<>();
        mSprayPoints = new ArrayList<>();
        mToolType = ToolType.NORMAL;
        mPaint.setPathEffect(null);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setDither(true);
        mBitmapPaint.setColor(Color.parseColor("#a6ffffff"));
        mBitmapPaint.setStyle(Paint.Style.STROKE);
        mBitmapPaint.setStrokeJoin(Paint.Join.ROUND);
        mBitmapPaint.setStrokeCap(Paint.Cap.ROUND);
        mBitmapPaint.setStrokeWidth(23);

        if (neonModeActivated) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            mPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));
            mPaint.setStrokeWidth(50);
//          mPaint.setShadowLayer(25,0,0,Color.RED);
//          mPaint.setPathEffect(getTrianglePathEffect(sprayStokeWidth));
        } else {
//            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mPaint.setStrokeWidth(16);
            mPaint.setMaskFilter(null);
        }
    }

    public int getPathListSize(){
        return pathPaintArrayList.size();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public ScribbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScribbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    float rotation = 0;

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawARGB(0, 0, 0, 0);
        for (int i = 0; i < pathPaintArrayList.size(); i++) {
//            canvas.rotate(30);
            canvas.drawPath(pathPaintArrayList.get(i).path, pathPaintArrayList.get(i).paint);

            if (neonModeActivated)
                canvas.drawPath(pathPaintArrayList.get(i).path, mBitmapPaint);
        }

        if (mPath != null && !mPath.isEmpty()) {
            canvas.drawPath(mPath, mPaint);

            if (neonModeActivated)
                canvas.drawPath(mPath, mBitmapPaint);
        }
        //mPath.transform(matrix);
//        canvas.restore();
    }

    public void toggleNeonMode (boolean enable) {
        neonModeActivated = enable;

        if (neonModeActivated) {
//          setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            mPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL));
            mPaint.setStrokeWidth(50);
        } else {
//          setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mPaint.setStrokeWidth(16);
            mPaint.setMaskFilter(null);
        }
    }


/*    @Override
    protected  void onDraw(Canvas canvas) {
        Paint strokePaint = new Paint();
        strokePaint.setARGB(255, 0, 0, 0);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setTextSize(40);
        strokePaint.setTypeface(Typeface.SERIF);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(20);

        Paint textPaint = new Paint();
        textPaint.setARGB(255, 255, 255, 255);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(40);
        textPaint.setTypeface(Typeface.SERIF);

        String text = "Bakchod Insaan";
        canvas.drawText(text, 300, 300, strokePaint);
        canvas.drawText(text, 300, 300, textPaint);
    }*/

    final static int dotsToDrawAtATime = 20;
    final static double brushRadius = 30.0; //Not being used now
    // This is however large they set the brush size, could be (1),
    // could be whatever the max size of your brush is, e.g., (50), but set it based on what they choose

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 1;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
            float[] f = new float[2];
            f[0] = x;
            f[1] = y;
            mSprayPoints.add(f);
        }
    }

    private void touchUp() {
        //    mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        PaintData pathPaintData = new PaintData();
        pathPaintData.path = new Path(mPath);
        pathPaintData.paint = new Paint(mPaint);
        pathPaintData.toolType = mToolType;
        pathPaintArrayList.add(pathPaintData);
//        completePath.reset();
//        if (!completePath.isEmpty()) throw new IllegalStateException("complete path not cleared");
        for (int i = 0; i < pathPaintArrayList.size(); i++) {
            Path path = pathPaintArrayList.get(i).path;
            RectF bounds = new RectF();
            //path.computeBounds(bounds, true);
            //Matrix matrix =  new Matrix();
//            matrix.postScale(1.3f, 1.0f);
            int myAngle = 20;
            // matrix.postRotate(myAngle, bounds.centerX(), bounds.centerY());
            //path.transform(matrix);
//            completePath.addPath(path);
        }
        invalidate();
        Log.d(TAG, "touchUp " + pathPaintArrayList.size() + pathPaintArrayList);
    }

    String TAG = "ScribbleView ";

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (!mDrawingAllowed) return false;
//        if (event.getPointerCount() == 2) {
//            performScale(event);
//            //return VideoEditorFragment.isScribblingMode;
//            return true;
//        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN");
                touch_start(x, y);
                invalidate();
                if (mScribbleListener != null)
                    mScribbleListener.onScribbleStart();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "ACTION_MOVE");
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP");
                touchUp();
                invalidate();
                if (mScribbleListener != null)
                    mScribbleListener.onScribbleEnd();
                break;
        }
//        return VideoEditorFragment.isScribblingMode;
        return true;
    }

    float scaleFactor = 1, angularRotation = 0;

    public Bitmap getBitmap() {
        //this.measure(100, 100);
        //this.layout(0, 0, 100, 100);
        this.setDrawingCacheEnabled(true);
        this.buildDrawingCache();
        Bitmap bmp = Bitmap.createBitmap(this.getDrawingCache());
        this.setDrawingCacheEnabled(false);
        return bmp;
    }

    private void performScale(MotionEvent event) {

    }

    public void clear() {
        //mBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
        System.gc();
    }

    /**
     * @return >0 if undo operation could be performed
     * 0 otherwise
     */
    public int undo() {
        mPath.reset();
        Path path = new Path();
        if (pathPaintArrayList.size() == 0) return 0;
        pathPaintArrayList.remove(pathPaintArrayList.size() - 1);
        for (int i = 0; i < pathPaintArrayList.size(); i++) {
            path.addPath(pathPaintArrayList.get(i).path);
        }
//        completePath = path;
        invalidate();
        if (mScribbleListener != null) {
            mScribbleListener.onUndoScribble();
        }
        return pathPaintArrayList.size();
    }

    public int undoCompleteCanvas(){
        pathPaintArrayList.clear();
        mPath.reset();
        invalidate();
        return 0;
    }

    int initialColor = Color.parseColor("#000000");
    Context context = null;

    public void setPaintColor(int color) {
        mPaint.setColor(color);
    }

    enum ToolType {NORMAL, SPRAY_CAN, DOTTED, OTHER}

    ToolType mToolType = ToolType.NORMAL;

    private PathEffect getTrianglePathEffect(int strokeWidth) {
        return new PathDashPathEffect(
                getTriangle(strokeWidth),
                strokeWidth,
                0.0f,
                PathDashPathEffect.Style.ROTATE);
    }

    private Path getTriangle(float size) {
        Random random = new Random();
        Path path = new Path();
        RectF r = new RectF();
        r.bottom = 20;
        r.top = 10;
        r.left = 10;
        r.right = 20;
        //  path.addArc(r, 0, 360);
        for (int i = 0; i < dotsToDrawAtATime; i++) {
            float x = (float) (size + random.nextGaussian() * brushRadius);
            float y = x;
            Log.d(TAG, "random " + x);
            //path.moveTo(x, x);
            //path.lineTo(400,500);
            //path.lineTo((float) (x + 0.1 * x), (float) (y + 0.1 * y));
        }
        float half = size / 2;
        path.moveTo(-half, -half);
        path.lineTo(half, -half);
        path.lineTo(0, half);
        path.close();
        return path;
    }

    ToolType dialogToolType;
    int sprayStokeWidth = 20;

    private void rotate() {
        Canvas canvas = new Canvas();
        canvas.rotate(0);
    }


    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    ScribbleListener mScribbleListener;

    public interface ScribbleListener {
        void onScribbleStart();

        void onScribbleEnd();

        void onTap(MotionEvent event);

        void onUndoScribble();
    }

    public void setScribbleListener(ScribbleListener scribbleListener) {
        this.mScribbleListener = scribbleListener;
    }



}