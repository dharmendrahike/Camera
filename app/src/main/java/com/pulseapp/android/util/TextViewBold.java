package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by admin on 1/23/2015.
 */
public class TextViewBold extends TextView {
    public TextViewBold(Context context) {
        super(context);
        customize(context);
    }

    public TextViewBold(Context context, AttributeSet attr) {
        super(context,attr);
        customize(context);
    }

    public TextViewBold(Context context, AttributeSet attr, int i) {
        super(context,attr,i);
        customize(context);
    }
    
    private void customize(Context context){
        setTypeface(AppLibrary.getTypeface(context, AppLibrary.Bold));
        setPaintFlags(getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON);

        //setPaintFlags(getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        //setTypeface(getTypeface().DEFAULT);
        //setTypeface(getTypeface(), Typeface.NORMAL);
    }

}
