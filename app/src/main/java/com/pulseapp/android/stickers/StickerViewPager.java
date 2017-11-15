package com.pulseapp.android.stickers;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by bajaj on 23/12/15.
 */
public class StickerViewPager extends ViewPager {
    private boolean isPagingEnabled = true;
    private String TAG = "StickerViewPager";

    public StickerViewPager(Context context) {
        super(context);
        isPagingEnabled = true;
    }

    public StickerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        isPagingEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        return this.isPagingEnabled && super.onTouchEvent(event);

        try {
            return super.onTouchEvent(event);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, " caught Exception " + e);
            return false;
        }
    }
//
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent event) {
//        return this.isPagingEnabled && super.onInterceptTouchEvent(event);
//    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, " caught Exception " + ex);
            ex.printStackTrace();
        }
        return false;
    }

    public void setPagingEnabled(boolean b) {
        this.isPagingEnabled = b;
    }
}
