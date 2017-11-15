package com.pulseapp.android.util;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.pulseapp.android.activities.CameraActivity;

/**
 * Created by abc on 10/27/2015.
 */
public class CustomViewPager extends ViewPager {

    public boolean canSwipe = true;
    private String TAG = "CustomViewPager";
    private boolean editingDone = true;
    private CameraActivity cameraActivity;

    public void setCameraActivity(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Call this method in your motion events when you want to disable or enable
    // It should work as desired.
    public void setCanSwipe(boolean canSwipe) {
        this.canSwipe = canSwipe;
    }

    public void setEditingDone(boolean editingDone) {
        this.editingDone = editingDone;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        try {
            if (cameraActivity.isPublicStreamContributionMode())
                return  false;

            if (this.canSwipe) {
                return (this.editingDone) && super.onInterceptTouchEvent(arg0); //Default intercept all touch events
            } else {
                return false; //Let it go down to the child views before calling ontouch of viewpager
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        if (cameraActivity.isPublicStreamContributionMode())
            return  true;

        try {
            return super.onTouchEvent(arg0);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
