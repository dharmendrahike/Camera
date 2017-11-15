package com.pulseapp.android.customViews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.pulseapp.android.R;

/**
 * Created by deepankur on 3/5/16.
 *
 * Helper Class to set an attribute MaxHeight to a linearLayout
 */
public class MaxHeightLinearLayout extends LinearLayout {
    private  int mMaxHeight;

    public void setmMaxHeight(int mMaxHeight) {
        this.mMaxHeight = mMaxHeight;
    }

    public MaxHeightLinearLayout(Context context) {
        super(context);
        mMaxHeight=0;
    }

    public MaxHeightLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MaxHeightLinearLayout);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightLinearLayout__max_height, Integer.MAX_VALUE);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }
}