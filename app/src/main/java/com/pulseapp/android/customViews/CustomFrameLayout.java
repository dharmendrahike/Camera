package com.pulseapp.android.customViews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by deepankur on 6/16/16.
 */
public class CustomFrameLayout extends FrameLayout {
    public CustomFrameLayout(Context context) {
        super(context);
    }

    public CustomFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.interceptTouchListener != null)
            return interceptTouchListener.onInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }


    private InterceptTouchListener interceptTouchListener;

    public void setInterceptTouchListener(InterceptTouchListener interceptTouchListener) {
        this.interceptTouchListener = interceptTouchListener;
    }

    public interface InterceptTouchListener {
        boolean onInterceptTouchEvent(MotionEvent event);
    }
}
