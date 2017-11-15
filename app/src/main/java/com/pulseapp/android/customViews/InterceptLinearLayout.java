package com.pulseapp.android.customViews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by deepankur on 6/29/16.
 */
public class InterceptLinearLayout extends LinearLayout {
    public InterceptLinearLayout(Context context) {
        super(context);
    }

    public InterceptLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }
    public interface InterceptTouchListener{
        boolean onInterceptTouchEvent(MotionEvent ev);
    }
}
