package com.pulseapp.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

//import com.pkmmte.view.CircularImageView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Morph on 6/18/2015.
 */
public class ContactListAdapter extends BaseAdapter implements ListAdapter{

    private JSONArray values;
    private Context context;
//    private ImageStorage isObj = new ImageStorage();

    private class ViewHolder{
        TextView contactNameTextView;
//        CircularImageView contactImage;
    }

    public ContactListAdapter(Context context,JSONArray val){
        this.values = val;
        this.context = context;
    }

    @Override
    public int getCount() {
        if (values == null){
            return 0;
        }else {
            return values.length();
        }
    }

    @Override
    public JSONObject getItem(int position) {
        if (values == null){
            return null;
        }else {
            return values.optJSONObject(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        LayoutInflater mInflater = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
//            convertView = mInflater.inflate(R.layout.social_contact_item, null);
            holder = new ViewHolder();
//            holder.contactImage = (CircularImageView) convertView.findViewById(R.id.contact_photo);
//            holder.contactNameTextView = (TextView) convertView.findViewById(R.id.contactName);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.contactNameTextView.setText("");
//        isObj.download("",holder.contactImage);
//        Picasso.with(context).load("")
//                .transform(new RoundedTransformation()).fit().into(holder.contactImage);

        return convertView;
    }
}
