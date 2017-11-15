package com.pulseapp.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Created by Morph on 7/23/2015.
 */
public class CustomSpinner extends Spinner {

    OnItemSelectedListener listener;

    public CustomSpinner(Context context){
        super(context);
    }

    public CustomSpinner(Context context,int mode){
        super(context,Spinner.MODE_DIALOG);
    }

    public CustomSpinner(Context context,AttributeSet attr){
        super(context,attr);
    }

    public CustomSpinner(Context context,AttributeSet attr,int defStyle){
        super(context,attr,defStyle);
    }

    public CustomSpinner(Context context,AttributeSet attr,int defStyle,int mode) {
        super(context,attr,defStyle,Spinner.MODE_DIALOG);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null)
            listener.onItemSelected(null, null, position, 0);
    }

    public void setOnItemSelectedEvenIfUnchangedListener(
            OnItemSelectedListener listener) {
        this.listener = listener;
    }
}
