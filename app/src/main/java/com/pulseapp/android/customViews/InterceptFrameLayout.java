package com.pulseapp.android.customViews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.pulseapp.android.fragments.BaseFragment;

/**
 * Created by deepankur on 11/9/16.
 */

public class InterceptFrameLayout extends FrameLayout {

    public InterceptFrameLayout(Context context) {
        super(context);
    }

    public InterceptFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    public interface InterceptTouchListener {

    }

    private InterceptTouchListener interceptTouchListener;
    private boolean shouldIntercept;

    private String TAG = getClass().getSimpleName();

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {

        Log.d(TAG, "onInterceptHoverEvent: " + BaseFragment.getActionName(event.getAction()));
        return super.onInterceptHoverEvent(event);
    }
}
