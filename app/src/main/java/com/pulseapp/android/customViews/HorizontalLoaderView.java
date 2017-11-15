package com.pulseapp.android.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.pulseapp.android.R;

/**
 * Created by deepankur on 10/4/16.
 */

public class HorizontalLoaderView extends View {
    public HorizontalLoaderView(Context context) {
        super(context);
        init(context);
    }

    public HorizontalLoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalLoaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private Context context;
    private Paint paint;
    Paint bgpaint;
    //    RectF rect;
    float percentage = 0;

    private void init(Context context) {
        this.context = context;
        paint = new Paint();
        paint.setColor(Color.parseColor("#800000FF"));
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        bgpaint = new Paint();
        bgpaint.setColor(context.getResources().getColor(R.color.pulse_gray));
        bgpaint.setAntiAlias(true);
        bgpaint.setStyle(Paint.Style.STROKE);

    }

    Handler h = new Handler();
    private String TAG = getClass().getSimpleName();
    Runnable r = new Runnable() {
        @Override
        public void run() {

            currentHead += PROGRESS_DELTA;
            if (currentHead > width) {
                currentHead = 0;
            }

            if (getVisibility() == VISIBLE)
                invalidate();
        }
    };


    /**
     * will tell which side the line being drawn is going
     * ie if starting from zero it will go right until reached right most
     * point
     */

    int width;
    //    float endPointToDraw;
    int currentHead;//representing the head of snake
    private final int PROGRESS_DELTA = 10;
    private static final int percentageLengthOfLoader = 50;//the snake is this % of the view

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = getWidth();
        int height = getHeight();

        paint.setStrokeWidth(height);
        bgpaint.setStrokeWidth(height);

        int lengthOfLoader = percentageLengthOfLoader * width/100 ;

        if (currentHead >= lengthOfLoader) {
            canvas.drawLine(0, height / 2, currentHead - lengthOfLoader, height / 2, bgpaint);
            canvas.drawLine(currentHead - lengthOfLoader, height / 2, currentHead, height / 2, paint);
            canvas.drawLine(currentHead, height / 2, width, height / 2, bgpaint);
        } else if (currentHead < lengthOfLoader) {
            canvas.drawLine(0, height / 2, currentHead, height / 2, paint);
            int remainingDelta = lengthOfLoader - currentHead;
            canvas.drawLine(width - remainingDelta, height / 2, width, height / 2, paint);
        }
        if (getVisibility() == VISIBLE)
            h.postDelayed(r, 10);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE)
            invalidate();
    }
}
