package com.pulseapp.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Created by Morph on 7/23/2015.
 */
public class SettingsSpinner extends Spinner {

    OnItemSelectedListener listener;

    public SettingsSpinner(Context context){
        super(context);
    }

    public SettingsSpinner(Context context,AttributeSet attr){
        super(context,attr);
    }

    public SettingsSpinner(Context context,AttributeSet attr,int defStyle){
        super(context,attr,defStyle);
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
