package com.pulseapp.android.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.pulseapp.android.customTextViews.MontserratRegularTextView;

/**
 * Created by user on 7/22/2016.
 */
public class CircularTextView extends MontserratRegularTextView {

    private float strokeWidth;
    int strokeColor,solidColor;
    Paint strokePaint, circlePaint;

    public CircularTextView(Context context) {
        super(context);
    }

    public CircularTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {

        if (circlePaint==null) {
            circlePaint = new Paint();
            circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        }
        circlePaint.setColor(solidColor);

        if (strokePaint==null) {
            strokePaint = new Paint();
            strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        }
        strokePaint.setColor(strokeColor);

        int  h = this.getHeight();
        int  w = this.getWidth();

        int diameter = ((h > w) ? h : w);
        int radius = diameter/2;

        this.setHeight(diameter);
        this.setWidth(diameter);

        if (strokeColor!=0)
            canvas.drawCircle(diameter / 2 , diameter / 2, radius, strokePaint);

        canvas.drawCircle(diameter / 2, diameter / 2, radius-strokeWidth, circlePaint);

        super.draw(canvas);
    }

    public void setStrokeWidth(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        strokeWidth = dp*scale;
    }

    public void setStrokeColor(String color) {
        strokeColor = Color.parseColor(color);
    }

    public void setSolidColor(String color) {
        solidColor = Color.parseColor(color);
    }
}
