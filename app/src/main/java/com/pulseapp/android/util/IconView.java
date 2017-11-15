package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by admin on 1/23/2015.
 */
public class IconView extends TextView {
    public IconView(Context context) {
        super(context);
        customize(context);
    }

    public IconView(Context context, AttributeSet attr) {
        super(context,attr);
        customize(context);
    }

    public IconView(Context context, AttributeSet attr, int i) {
        super(context,attr,i);
        customize(context);
    }
    
    private void customize(Context context){
        setTypeface(AppLibrary.getTypeface(context, AppLibrary.Icon));
        setPaintFlags(getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.HINTING_ON);

        //setPaintFlags(getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
        //setTypeface(getTypeface().DEFAULT);
        //setTypeface(getTypeface(), Typeface.NORMAL);
    }

}
