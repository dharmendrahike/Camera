package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Karthik on 27/07/16.
 */
public class AnimatedCircleView extends View {

    private static final int START_ANGLE_POINT = -90;

    private final Paint paint;
    private final RectF rect;

    private float angle;

    public AnimatedCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final int strokeWidth = 5;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        //Circle color
        paint.setColor(Color.WHITE);

        //size 200x200 example
        rect = new RectF(strokeWidth, strokeWidth, AppLibrary.convertDpToPixel(80,context) + strokeWidth, AppLibrary.convertDpToPixel(80,context) + strokeWidth);

        //Initial Angle (optional, it can be zero)
        angle = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(rect, START_ANGLE_POINT, angle, false, paint);
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }
}