package com.pulseapp.android.customViews;

import android.content.Context;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.fragments.BaseFragment;

/**
 * Created by deepankur on 7/27/16.
 */
public class LockableScrollView extends NestedScrollView {


    // true if we can scroll (not locked)
    // false if we cannot scroll (locked)
    private boolean mScrollable = true;
    private String TAG = this.getClass().getSimpleName();
    private boolean mTouched;

    public boolean isTouched() {
        return mTouched;
    }

    public void setScrollable(boolean mScrollable) {
        this.mScrollable = mScrollable;
    }

    public LockableScrollView(Context context) {
        super(context);
    }

    public LockableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setScrollingEnabled(boolean enabled) {
        mScrollable = enabled;
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mTouched = true;
//        Log.d(TAG, " motion " + BaseFragment.getActionName(ev.getAction()));
        DynamicDownloader.notifyScrollDetectedInDashboard();

        if (ev.getAction() != mOldActionEvent && motionEventChangeListener != null) {
            mOldActionEvent = ev.getAction();
            motionEventChangeListener.onOnActionChanged(mOldActionEvent);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // if we can scroll pass the event to the superclass
                if (mScrollable) return super.onTouchEvent(ev);
                // only continue to handle the touch event if scrolling enabled
                return mScrollable; // mScrollable is always false at this point
            default:
                return super.onTouchEvent(ev);
        }
    }

    private int mOldActionEvent = -12345;

    private MotionEventChangeListener motionEventChangeListener;

    public void setMotionEventChangeListener(MotionEventChangeListener motionEventChangeListener) {
        this.motionEventChangeListener = motionEventChangeListener;
    }

    public interface MotionEventChangeListener {
        void onOnActionChanged(int newAction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Don't do anything with intercepted touch events if
        // we are not scrollable
        if (!mScrollable) return false;
        else return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
//        Log.d(TAG,"onScrollChanged");
//        DynamicDownloader.notifyScrollDetectedInDashboard();
        super.onScrollChanged(l, t, oldl, oldt);
    }
}