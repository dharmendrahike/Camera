package com.pulseapp.android.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;

/**
 * Created by deepankur on 5/23/16.
 */
public class ColorPickerView extends LinearLayout {
    private int[] colors;
    private Context context;
    private HashMap<Integer, Integer> viewId_ColorMap;
    private Paint p;
    private int width, height;
    private String TAG = this.getClass().getSimpleName();
    private float drawableStrokeWidth;

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initView();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    public ColorPickerView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, width, height, p);
    }

    private void loadResources() {
        width = AppLibrary.convertDpToPixels(context, 30);
        height = AppLibrary.convertDpToPixels(context, 40);
        p = new Paint();
        colors = new int[]{android.R.color.black, android.R.color.holo_red_dark,
                android.R.color.holo_blue_bright, android.R.color.white,
                android.R.color.holo_purple, android.R.color.holo_green_dark};
        drawableStrokeWidth = context.getResources().getDimension(R.dimen.color_picker__bg_drawable_stroke);
        Log.d(TAG, " load resources stroke width " + drawableStrokeWidth);

    }

    private void initView() {
        loadResources();
        for (int i = 0; i < colors.length; i++)
            colors[i] = getResources().getColor(colors[i]);
        viewId_ColorMap = new HashMap<>();

        this.setOrientation(LinearLayout.VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));


        for (int i = 0; i < colors.length; i++) {
            View childView = new View(context);
            childView.setBackground(getDrawableShape(i));
            ((GradientDrawable) childView.getBackground()).setColor(colors[i]);
            this.addView(childView);
            childView.getLayoutParams().height = height;
            childView.getLayoutParams().width = width;
            Log.d(TAG, " added view with id " + childView.getId());
            viewId_ColorMap.put(childView.getId(), colors[i]);
            if (i == colors.length - 1)
                childView.setScaleY(-1f);

            if (i > 0 && i < colors.length - 1) {
                childView.setPadding(0, (int) drawableStrokeWidth, 0, (int) drawableStrokeWidth);
            }
            childView.setId(i);
            childView.setOnClickListener(colorPickerClickListener);
        }
        addUndoView();
        this.invalidate();
    }

    private Drawable getDrawableShape(int index) {
        if (index == 0 || index == colors.length - 1)
            return context.getResources().getDrawable(R.drawable.color_picker_bg_rounded_corners);
        return context.getResources().getDrawable(R.drawable.color_picker_bg_normal_corners);
    }

    private void addUndoView() {
        View undoView = new View(context);
        this.addView(undoView);
        Log.d(TAG, " addUndoView " + undoView.getId());
        undoView.getLayoutParams().height = height;
        undoView.getLayoutParams().width = width;
        undoView.setBackgroundResource(R.drawable.undo);
        undoView.setId(colors.length);
        undoView.setOnClickListener(colorPickerClickListener);
    }

    private View.OnClickListener colorPickerClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, " clicked " + v.getId());

        }
    };

    public void hidePicker() {
        this.setVisibility(GONE);
    }

    public void showColorPicker() {
        this.setVisibility(VISIBLE);
    }

    private ColorPickerListener colorPickerListener;

    public void setColorPickerListener(ColorPickerListener colorPickerListener) {
        this.colorPickerListener = colorPickerListener;
    }

    public interface ColorPickerListener {
        void onColorSelected(int colorId);

        //return here the pending size of arrayList of paints
        int onUndoCicked();
    }
}