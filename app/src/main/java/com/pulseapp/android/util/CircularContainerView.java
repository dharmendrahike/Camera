package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.RelativeLayout;

/**
 * Created by abc on 3/21/2016.
 */
public class CircularContainerView extends RelativeLayout {

    private Paint greyPaint,whitePaint;
    private float RADIUS = 0;
    private int BORDER_RADIUS = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(RADIUS, RADIUS, RADIUS, whitePaint);
        canvas.drawCircle(RADIUS, RADIUS, RADIUS - BORDER_RADIUS, greyPaint);
    }

    public CircularContainerView(Context context, int dimension, int border) {
        super(context);
        init(dimension, border);
    }

    private void init(int dimension, int border){
        setWillNotDraw(false);
        greyPaint = new Paint();
        greyPaint.setColor(Color.GRAY);
        greyPaint.setStyle(Paint.Style.FILL);
        greyPaint.setAntiAlias(true);
        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setAntiAlias(true);

        this.RADIUS = dimension/2f;
        this.BORDER_RADIUS = border;
    }

}
