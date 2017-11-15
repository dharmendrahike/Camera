package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.pulseapp.android.R;

/**
 * Created by abc on 2/20/2016.
 */
public class ColoredTimelineView extends ImageView {

    private float mPercentageFinished = 0;
    Paint paint, backgroundPaint;

    public ColoredTimelineView(Context context) {
        super(context);
        init();
    }

    public ColoredTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColoredTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
//        mColorThemeList = new ArrayList<>();
/*        mColorThemeList.add(new ColorTheme(ColorTheme.DRAW_MODE_COLOR, Color.RED, null, null, 0.0f, 0.25f));
        mColorThemeList.add(new ColorTheme(ColorTheme.DRAW_MODE_COLOR, Color.GREEN, null, null, 0.25f, 0.5f));
        mColorThemeList.add(new ColorTheme(ColorTheme.DRAW_MODE_COLOR, Color.CYAN, null, null, 0.5f, 0.75f));*/
        paint = new Paint();
        paint.setColor(Color.parseColor("#b39900ff")); //70% opacity

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#80ffffff")); //50% opacity
    }

    public void resetTimeline() {
//        mColorThemeList.clear();
    }

    public void update(float percentageFinished) {
        resetTimeline();
        mPercentageFinished = percentageFinished;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // We're using SRC_IN Mode to draw over the background
        // that has already been drawn on the view (as specified in XML)
        // But we still needed to use LAYER_TYPE_SOFTWARE,
        // because the activity's window (in case of hw accel) already has some content on it.
        // So we are left with two options, either to draw on a bitmap or choose layer type as software.
        // We chose the cooler option, as always (the latter.
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

/*        for(ColorTheme colorTheme : mColorThemeList) {
            paint.setColor(colorTheme.mColor);
            canvas.drawRect(colorTheme.mStart*getWidth(), 0, colorTheme.mEnd*getWidth(), getHeight(), paint);
        }*/
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        canvas.drawRect(0, 0, mPercentageFinished * getWidth(), getHeight(), paint);
    }

//    private static class ColorTheme {
//
//        public static final int DRAW_MODE_COLOR = 0;
//        public static final int DRAW_MODE_DRAWABLE = 1;
//        public static final int DRAW_MODE_BITMAP = 2;
//
//        int mDrawMode;
//
//        int mColor;
//        Drawable mDrawable;
//        Bitmap mBitmap;
//
//        float mEnd; // as percentage/fraction of total length ( 0 to 1)
//        float mStart; // as percentage/fraction of total length ( 0 to 1)
//
//        public ColorTheme(int drawMode, int color, Drawable drawable, Bitmap bitmap, float start, float end) {
//            mDrawMode = drawMode;
//            mColor = color;
//            mDrawable = drawable;
//            mBitmap = bitmap;
//            mStart = start;
//            mEnd = end;
//        }
//    }
}
