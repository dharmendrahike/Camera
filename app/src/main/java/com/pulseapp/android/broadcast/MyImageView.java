package com.pulseapp.android.broadcast;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.pulseapp.android.fragments.CameraFragment;

/**
 * Created by Karthik on 3/12/16.
 */

public class MyImageView extends ImageView {
    public MyImageView(Context context) {
        this(context, null, 0);
        setWillNotDraw(false);
    }

    public MyImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setWillNotDraw(false);
    }

    public MyImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawARGB(0,0,0,0);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(2);
        paint.setTextSize(40.0f);

        //SmileProbability
        paint.setColor(Color.WHITE);
        if(CameraFragment.smileProbability>=0.5f)
            canvas.drawText("Smiling. Yaw is " + CameraFragment.yaw + "degrees, Roll is " + CameraFragment.roll + "degrees", 50, 200, paint);
        else
            canvas.drawText("Yaw is " + CameraFragment.yaw + "degrees, Roll is " + CameraFragment.roll + "degrees", 100, 50, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        //FaceBounds
        paint.setColor(Color.WHITE);
        canvas.drawRect(CameraFragment.faceX, CameraFragment.faceY, CameraFragment.faceX+CameraFragment.faceWidth, CameraFragment.faceY+CameraFragment.faceHeight, paint);

        //LeftEye
        paint.setColor(Color.RED);
        canvas.drawCircle(CameraFragment.leftEyeX, CameraFragment.leftEyeY, 10, paint);

        //RightEye
        paint.setColor(Color.RED);
        canvas.drawCircle(CameraFragment.rightEyeX, CameraFragment.rightEyeY, 10, paint);

        //Nose
        paint.setColor(Color.BLUE);
        canvas.drawCircle(CameraFragment.noseX, CameraFragment.noseY, 10, paint);

        //LeftCheek
        paint.setColor(Color.BLACK);
        canvas.drawCircle(CameraFragment.leftCheekX, CameraFragment.leftCheekY, 10, paint);

        //RightCheek
        paint.setColor(Color.BLACK);
        canvas.drawCircle(CameraFragment.rightCheekX, CameraFragment.rightCheekY, 10, paint);

        //LeftMouth
        paint.setColor(Color.GREEN);
        canvas.drawCircle(CameraFragment.leftMouthX, CameraFragment.leftMouthY, 10, paint);

        //RightMouth
        paint.setColor(Color.GREEN);
        canvas.drawCircle(CameraFragment.rightMouthX, CameraFragment.rightMouthY, 10, paint);

        //BottomMouth
        paint.setColor(Color.GREEN);
        canvas.drawCircle(CameraFragment.bottomMouthX, CameraFragment.bottomMouthY, 10, paint);

        //LeftEar
        paint.setColor(Color.CYAN);
        canvas.drawCircle(CameraFragment.leftEarX, CameraFragment.leftEarY, 10, paint);

        //RightEar
        paint.setColor(Color.CYAN);
        canvas.drawCircle(CameraFragment.rightEarX, CameraFragment.rightEarY, 10, paint);
        }
}
