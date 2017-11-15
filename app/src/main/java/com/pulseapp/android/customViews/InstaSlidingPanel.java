package com.pulseapp.android.customViews;

/**
 * Created by deepankur on 6/8/16.
 */

import android.content.Context;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class InstaSlidingPanel extends SlidingPaneLayout {

    private boolean enabled = true;

    public InstaSlidingPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public InstaSlidingPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InstaSlidingPanel(Context context) {
        super(context);
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        if(isOpen() || enabled){
            return super.onInterceptTouchEvent(arg0);
        }
        else
            return false;
    }
    public void setSlidingEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
