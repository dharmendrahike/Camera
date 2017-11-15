package com.pulseapp.android.customViews;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by deepankur on 9/17/16.
 */
public class PieView extends View {
    Context context;

    public PieView(Context context) {
        super(context);
        init(context);
    }

    public PieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PieView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        paint = new Paint();
        paint.setColor(Color.parseColor("#80FFFFFF"));
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        bgpaint = new Paint();
        bgpaint.setColor(Color.parseColor("#80000000"));
        bgpaint.setAntiAlias(true);
        bgpaint.setStyle(Paint.Style.FILL);
        rect = new RectF();
    }

    Paint paint;
    Paint bgpaint;
    RectF rect;
    float percentage = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //draw background circle anyway
        int left = 0;
        int width = getWidth();
        int top = 0;

        rect.set(left, top, left + width, top + width);
        canvas.drawArc(rect, 0, (360 ), true, bgpaint);
//        canvas.drawArc(rect, -90, 360, true, bgpaint);
        if (percentage != 0) {
            rect.set(left + getPadding(), top + getPadding(), left + width - getPadding(), top + width - getPadding());
            canvas.drawArc(rect,-90, - (360 * percentage), true, paint);
        }
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage / 100;
        invalidate();
    }

    public float getPadding(){
        return 4*getOneDp();
    }
    public float getOneDp() {
        return convertDpToPixel(1, context);
    }

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

}