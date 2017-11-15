package com.pulseapp.android.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pulseapp.android.R;
import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by deepankur on 5/23/16.
 */
public class OvalColorPicker extends LinearLayout {
    private int[] colors;
    private Context context;
    private HashMap<Integer, Integer> viewId_ColorMap;
    private Paint p;
    private int width, height;
    private String TAG = this.getClass().getSimpleName();
    int color_element_margin;

    public OvalColorPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initView();
    }

    public OvalColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    public OvalColorPicker(Context context) {
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
        width = AppLibrary.convertDpToPixels(context, 32);
        height = AppLibrary.convertDpToPixels(context, 240);
        boxSize = AppLibrary.convertDpToPixels(context, 20);
        color_element_margin = AppLibrary.convertDpToPixels(context, 2);
        p = new Paint();
        colors = new int[]{
                Color.parseColor("#000000"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#ff6ab0"),
                Color.parseColor("#ff6d69"),
                Color.parseColor("#32b4e6"),
                Color.parseColor("#8d72d6"),
                Color.parseColor("#48c33b"),
                Color.parseColor("#f40032"),
                Color.parseColor("#ffd400"),
        };
    }

    LinearLayout colorsHolder;
    int boxSize;

    @SuppressWarnings("ResourceType")
    private void initView() {
        loadResources();
        colorsHolder = new LinearLayout(context);
        colorsHolder.setOrientation(LinearLayout.VERTICAL);
        colorsHolder.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        colorsHolder.setBackgroundResource(R.drawable.color_picker_base_svg);
        this.addView(colorsHolder);
        this.setGravity(Gravity.CENTER_HORIZONTAL);
        ((LayoutParams) colorsHolder.getLayoutParams()).bottomMargin = AppLibrary.convertDpToPixels(context, 8);

        viewId_ColorMap = new HashMap<>();

        this.setOrientation(LinearLayout.VERTICAL);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        for (int i = 0; i < colors.length; i++) {
            ImageView childView = new ImageView(context);
            childView.setScaleType(ImageView.ScaleType.CENTER);
            colorsHolder.addView(childView);

            if (i == 0) {
                childView.setBackgroundResource(R.drawable.color_1_svg);
            } else if (i == 1) {
                childView.setBackgroundResource(R.drawable.white_color_element);
                int size = AppLibrary.convertDpToPixels(context, 24);
                childView.getLayoutParams().height = size;
                childView.getLayoutParams().width = size;

            } else if (i == colors.length - 1) {
                childView.setBackgroundResource(R.drawable.colors_9_svg);
            } else {
                childView.setBackgroundColor(colors[i]);
                //noinspection SuspiciousNameCombination
                childView.getLayoutParams().height = width;
                childView.getLayoutParams().width = width;
            }


            ((LayoutParams) childView.getLayoutParams()).leftMargin = color_element_margin;
            ((LayoutParams) childView.getLayoutParams()).rightMargin = color_element_margin;

            if (i == 0) {
                ((LayoutParams) childView.getLayoutParams()).topMargin = color_element_margin;
                ((LayoutParams) childView.getLayoutParams()).bottomMargin = color_element_margin;
            } else if (i == 1) {
                ((LayoutParams) childView.getLayoutParams()).topMargin = 0;
                ((LayoutParams) childView.getLayoutParams()).bottomMargin = 0;
                ((LayoutParams) childView.getLayoutParams()).leftMargin = color_element_margin;
                ((LayoutParams) childView.getLayoutParams()).rightMargin = 0;
            } else if (i == 2) {
                ((LayoutParams) childView.getLayoutParams()).topMargin = color_element_margin;
                ((LayoutParams) childView.getLayoutParams()).bottomMargin = color_element_margin / 2;
            } else if (i == colors.length - 1) {
                ((LayoutParams) childView.getLayoutParams()).topMargin = color_element_margin / 2;
                ((LayoutParams) childView.getLayoutParams()).bottomMargin = color_element_margin;
            } else {
                ((LayoutParams) childView.getLayoutParams()).topMargin = color_element_margin / 2;
                ((LayoutParams) childView.getLayoutParams()).bottomMargin = color_element_margin / 2;
            }


            ((LayoutParams) childView.getLayoutParams()).width = boxSize;
            ((LayoutParams) childView.getLayoutParams()).height = boxSize;

            viewId_ColorMap.put(childView.getId(), colors[i]);
            int id = View.generateViewId();
            childView.setId(id);
            Log.d(TAG, " added view with id " + childView.getId());
            childView.setOnClickListener(colorClickClickListener);
            id_colorMap.put(id, colors[i]);
            if (i == colors.length - 2) {//initially drawing tick mark
                childView.callOnClick();
            }
        }
        addUndoView();
        undoView.setVisibility(INVISIBLE);//initially no undo is there
        this.invalidate();
    }

    HashMap<Integer, Integer> id_colorMap = new HashMap<>();
    View undoView;

    private void addUndoView() {
        undoView = new View(context);
        this.addView(undoView);
        Log.d(TAG, " addUndoView " + undoView.getId());
        undoView.getLayoutParams().height = AppLibrary.convertDpToPixels(context, 32);
        undoView.getLayoutParams().width = AppLibrary.convertDpToPixels(context, 32);
        undoView.setBackgroundResource(R.drawable.undo_svg);
        undoView.setId(colors.length);
        undoView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (colorPickerListener != null) {
                    undoView.setVisibility(colorPickerListener.onUndoClicked() > 0 ? View.VISIBLE : View.INVISIBLE);
                }
            }
        });
    }


    private OnClickListener colorClickClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, " clicked " + v.getId());
            int viewId = v.getId();
            refreshTickMark(viewId);
            if (colorPickerListener != null)
                colorPickerListener.onColorSelected(id_colorMap.get(viewId));
        }
    };

    void refreshTickMark(int resourceId) {
        for (Map.Entry<Integer, Integer> entry : id_colorMap.entrySet()) {
            if (entry.getKey() == resourceId)
                if (entry.getValue().equals(colors[1]))//ia a white color
                    ((ImageView) colorsHolder.findViewById(entry.getKey())).setImageResource(R.drawable.check_black_svg);
                else
                    ((ImageView) colorsHolder.findViewById(entry.getKey())).setImageResource(R.drawable.check_white_svg);
            else
                ((ImageView) colorsHolder.findViewById(entry.getKey())).setImageResource(0);
        }
    }

//    public void hidePicker() {
//        this.setVisibility(GONE);
//    }
//
//    public void showColorPicker() {
//        this.setVisibility(VISIBLE);
//    }

    public void showUndo() {
        this.undoView.setVisibility(VISIBLE);
    }

    private ColorPickerListener colorPickerListener;

    public void setColorPickerListener(ColorPickerListener colorPickerListener) {
        this.colorPickerListener = colorPickerListener;
    }

    public interface ColorPickerListener {
        void onColorSelected(int colorInt);


        /**
         * @return the pending size of arrayList of paints
         */
        int onUndoClicked();

    }
}