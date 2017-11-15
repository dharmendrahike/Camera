package com.pulseapp.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Created by deepankur on 6/27/16.
 */
public class OverRideBackEventEditText extends EditText {
    private String TAG = OverRideBackEventEditText.class.getSimpleName();

    public OverRideBackEventEditText(Context context) {
        super(context);
    }

    public OverRideBackEventEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverRideBackEventEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
       if (onKeyPreImeListener!=null)
           onKeyPreImeListener.onKeyPreImePressed(keyCode,event);
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        Log.d(TAG, "onKeyMultiple");
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp");
        return super.onKeyUp(keyCode, event);
    }

    OnKeyPreImeListener onKeyPreImeListener;

    public void setOnKeyPreImeListener(OnKeyPreImeListener onKeyPreImeListener) {
        this.onKeyPreImeListener = onKeyPreImeListener;
    }

    public interface OnKeyPreImeListener{
        void onKeyPreImePressed(int keyCode, KeyEvent event);
    }
}
