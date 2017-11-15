package com.pulseapp.android.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.pulseapp.android.modelView.HomeMomentViewModel;

/**
 * Created by Karthik on 19/07/16.
 */
public class SmartViewPage extends FrameLayout {

    private String TAG = "SmartViewPage";
    int lastDownY = 0;
    int benchmarkY, benchmarkX = 0;
    ViewPageCloseCallback mListener;
    boolean mIsMinimizing = false;
    int layoutWidth;
    int layoutHeight;
    float percentage;
    private boolean cancelAnimation = false;
    private float minimizeToX, minimizeToY;

    /**
     * supply absolute coordinates itself
     * <p/>
     * currently will work only for nearby & public
     * use sepate constructor for my streams
     */
    public void setRawEndPoints(Activity context) {
        int rawX = HomeMomentViewModel.viewPositionX;
        int rayY = HomeMomentViewModel.viewPositionY;
        final int[] deviceParams = AppLibrary.getDeviceParams(context);
        int centerX = deviceParams[0] / 2;
        int centerY = deviceParams[1] / 2;
        this.minimizeToX = rawX - centerX;
        this.minimizeToY = rayY - centerY;
    }


    public void setCancelAnimation(boolean cancelAnimation) {
        this.cancelAnimation = cancelAnimation;
    }

    public SmartViewPage(Context context) {
        super(context);
    }

    public SmartViewPage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(ViewPageCloseCallback listener) {
        this.mListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent arg0) {
        if (cancelAnimation) return super.dispatchTouchEvent(arg0);

        try {
//            AppLibrary.log_d(TAG, "Received touch event: " + arg0.toString());
            final int action = MotionEventCompat.getActionMasked(arg0);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    lastDownY = (int) arg0.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:

                    if (mIsMinimizing) {
                        if ((arg0.getRawY() - benchmarkY) > 0) {
                            percentage = 1 - ((2 * (arg0.getRawY() - benchmarkY)) / layoutHeight);

                            if (percentage <= 1 && percentage >= 0.2) {
//                                AppLibrary.log_d(TAG, "Continue minimizing with percentage " + percentage);
                                this.setScaleX(percentage);
                                this.setScaleY(percentage);
                            }
                        }

                        this.setTranslationX((arg0.getRawX() - benchmarkX));
                        this.setTranslationY((arg0.getRawY() - benchmarkY));
                        this.requestLayout();
                        return true;
                    }

                    int yDiff = (int) arg0.getRawY() - lastDownY;

//                    AppLibrary.log_d(TAG, "yDiff is " + yDiff);

                    if (yDiff > 100) { // Start minimizing!
                        mIsMinimizing = true;
                        benchmarkX = (int) arg0.getRawX();
                        benchmarkY = (int) arg0.getRawY();
                        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                        layoutWidth = this.getMeasuredWidth();
                        layoutHeight = this.getMeasuredHeight();
                        AppLibrary.log_d(TAG, "Start minimizing at layout height: " + layoutHeight);
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (mIsMinimizing) {
                        AppLibrary.log_d(TAG, "Cancelling minimizing at percentage " + percentage);
                        this.setLayerType(View.LAYER_TYPE_NONE, null);

                        if (percentage < 0.7f) {
                            Runnable minimize = new Runnable() {
                                @Override
                                public void run() {
                                    mListener.onMinimize();
                                }
                            };
                            this.animate().withLayer().setDuration(300).scaleX(0).scaleY(0).translationX(minimizeToX).translationY(minimizeToY).withEndAction(minimize).start();
                        } else {
                            this.animate().withLayer().setDuration(100).scaleX(1).scaleY(1).translationX(0).translationY(0).start();
                        }
                    }

                    AppLibrary.log_d(TAG, "Reset everything");
                    mIsMinimizing = false;
                    benchmarkY = 0;
                    benchmarkX = 0;
                    percentage = 1;
                    break;
            }

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }

        return super.dispatchTouchEvent(arg0);
    }
}


