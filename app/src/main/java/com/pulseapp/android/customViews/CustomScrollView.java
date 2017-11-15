package com.pulseapp.android.customViews;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;

/**
 * Created by indianrenters on 8/12/16.
 */
public class CustomScrollView extends ScrollView {

    private final int defaultHeight = 200;
    private int maxHeight;
    private Context context;


    public CustomScrollView(Context context) {
        super(context);
        this.context = context;

    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(context, attrs);
        }
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            init(context, attrs);
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (!isInEditMode()) {
            init(context, attrs);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(AppLibrary.convertDpToPixels(context, 396), MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView);
            maxHeight = styledAttrs.getDimensionPixelSize(R.styleable.MaxHeightScrollView_maxHeight, AppLibrary.convertDpToPixels(getContext(), 396));

            styledAttrs.recycle();
        }
    }
}
