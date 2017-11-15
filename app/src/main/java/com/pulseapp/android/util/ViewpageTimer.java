package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Karthik on 27/07/16.
 */
public class ViewpageTimer extends View {

    private static final int START_ANGLE_POINT = -90;

    private final Paint paint, eraserPaint, fillPaint;
    private final RectF rect;

    private float angle, radius, variableRadius;
    private int centerX, centerY;

    public ViewpageTimer(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        final int strokeWidth = 5;

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(Color.WHITE);

        fillPaint = new Paint(Paint.DITHER_FLAG);
        fillPaint.setAntiAlias(true);
        fillPaint.setDither(true);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setAlpha(150);

        eraserPaint = new Paint(Paint.DITHER_FLAG);
        eraserPaint.setAntiAlias(true);
        eraserPaint.setDither(true);
        eraserPaint.setColor(Color.TRANSPARENT);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        //size 200x200 example
        rect = new RectF(strokeWidth, strokeWidth, AppLibrary.convertDpToPixel(28,context) + strokeWidth, AppLibrary.convertDpToPixel(28,context) + strokeWidth);
        centerX = (int) (strokeWidth + AppLibrary.convertDpToPixel(28,context)/2);
        centerY = (int) (strokeWidth + AppLibrary.convertDpToPixel(28,context)/2);
        radius = AppLibrary.convertDpToPixel(28,context)/2 - AppLibrary.convertDpToPixels(context,4);
        variableRadius = 0;

        //Initial Angle (optional, it can be zero)
        angle = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(rect, START_ANGLE_POINT, angle, false, paint);

        canvas.drawCircle(centerX,centerY,radius,fillPaint);

        canvas.drawCircle(centerX,centerY,variableRadius,eraserPaint);
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void setEraserSize(float percentage) {
        this.variableRadius = radius*percentage;
    }
}