package com.pulseapp.android.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.explosion.Utils;
import com.pulseapp.android.models.UserModel;
import com.pulseapp.android.util.FontPicker;

import java.util.ArrayList;

/**
 * Created by indianrenters on 8/25/16.
 */
public class InstituteSearchResultsAdapter extends BaseAdapter {

    private ArrayList<UserModel.InstitutionData> names;
    private Context mContext;
    private LayoutInflater inflater;

    public InstituteSearchResultsAdapter(ArrayList<UserModel.InstitutionData> names, Context mContext) {
        this.names = names;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return names.size();
    }

    @Override
    public UserModel.InstitutionData getItem(int position) {
        return names.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView==null){
            convertView = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(getItem(position).name);
        tv.setTypeface(FontPicker.getInstance(mContext).getMontserratRegular());
        tv.setAlpha(0.87f);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        return convertView;
    }
}
