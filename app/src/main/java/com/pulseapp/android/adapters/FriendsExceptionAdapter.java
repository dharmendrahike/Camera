package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.CircleImageView;
import com.pulseapp.android.models.SocialModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by indianrenters on 8/2/16.
 */
public class FriendsExceptionAdapter extends RecyclerView.Adapter<FriendsExceptionAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private Context mContext;
    private HashMap<String, SocialModel.Friends> friendsHashMap;
    private HashMap<String, SocialModel.Friends> rejectedFriends;
    private ArrayList<String> allKeys;

    public FriendsExceptionAdapter(Context context, HashMap<String, SocialModel.Friends> friendsHashMap) {
        this.friendsHashMap = friendsHashMap;
        mContext = context;
        rejectedFriends = new HashMap<>();
        inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.single_friends_exception_list_item, parent, false);
        ViewHolder myViewHolder = new ViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItem(position) != null) {
            final int i = position;
            if (getItem(position).name != null && !getItem(position).name.isEmpty())
                holder.name.setText(getItem(position).name);
            if (getItem(position).imageUrl != null && !getItem(position).imageUrl.isEmpty())
                Picasso.with(mContext).load(getItem(position).imageUrl).fit().centerCrop().into(holder.profilePic);

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        rejectedFriends.put(allKeys.get(i),getItem(i));
                    }
                }
            });

        }
    }

    public SocialModel.Friends getItem(int position) {
        if (friendsHashMap != null && !friendsHashMap.isEmpty()) {
            allKeys = new ArrayList<>(friendsHashMap.keySet());
            if (allKeys.size() >= position) return friendsHashMap.get(allKeys.get(position));
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return friendsHashMap.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public CircleImageView profilePic;
        public TextView name;
        public AppCompatCheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            profilePic = (CircleImageView) itemView.findViewById(R.id.iv_profile_pic);
            name = (TextView) itemView.findViewById(R.id.tv_name);
            checkBox = (AppCompatCheckBox) itemView.findViewById(R.id.checkbox);

        }
    }
}
