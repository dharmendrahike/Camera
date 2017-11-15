package com.pulseapp.android.customTextViews;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.pulseapp.android.util.FontPicker;

/**
 * Created by deepankur on 6/27/16.
 */
public class MontserratBoldTextView extends TextView {
    private AttributeSet attrs;
    private int defStyle;


    public MontserratBoldTextView(Context context) {
        super(context);
        init(context);
    }

    public MontserratBoldTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MontserratBoldTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    Typeface font;
    FontPicker fontPicker;

    private void init(Context context) {
        fontPicker = FontPicker.getInstance(context);
        font = fontPicker.getMontserratBold();
//        this.setTextColor(Color.BLACK);

        this.setTypeface(font);
    }

    @Override
    public void setTypeface(Typeface tf, int style) {
        tf = font;
        super.setTypeface(tf, style);
    }

    @Override
    public void setTypeface(Typeface tf) {
        tf = font;
        super.setTypeface(tf);
    }
}
