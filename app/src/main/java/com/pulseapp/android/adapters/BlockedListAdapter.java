package com.pulseapp.android.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.CircleImageView;
import com.pulseapp.android.models.SettingsModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by indianrenters on 8/1/16.
 */
public class BlockedListAdapter extends RecyclerView.Adapter<BlockedListAdapter.ViewHolder> {

    private HashMap<String, SettingsModel.BlockedUserModel> blockedUsers;
    private Context mContext;
    private LayoutInflater inflater;
    private ArrayList<String> allKeys;


    public BlockedListAdapter(Context context, HashMap<String, SettingsModel.BlockedUserModel> blockedUsers) {
        mContext = context;
        this.blockedUsers = blockedUsers;
        allKeys = new ArrayList<>(blockedUsers.keySet());
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.single_blocked_contact_layout, parent, false);
        ViewHolder myViewHolder = new ViewHolder(view);
        return myViewHolder;

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItem(position) != null) {
            final int i = position;
            if (getItem(position).name != null) holder.userName.setText(getItem(position).name);
            if (getItem(position).handle != null)
                holder.userHandle.setText(getItem(position).handle);
            if (getItem(position).imageUrl != null)
                Picasso.with(mContext).load(getItem(position).imageUrl).fit().centerCrop().into(holder.profilePic);
            holder.unBlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    unblockUser(allKeys.get(i));
                }
            });
        }
    }

    private void unblockUser(String s) {
        // TODO unblock this user
        Log.e("unblockUser: ", s);
    }

    @Override
    public int getItemCount() {
        if (blockedUsers != null) {
            return blockedUsers.size();
        }
        return 0;
    }

    public SettingsModel.BlockedUserModel getItem(int position) {
        if (blockedUsers != null && !blockedUsers.isEmpty()) {
            if (allKeys.size() >= position) return blockedUsers.get(allKeys.get(position));
        }
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public CircleImageView profilePic;
        public TextView userName, userHandle;
        public Button unBlock;

        public ViewHolder(View itemView) {
            super(itemView);
            profilePic = (CircleImageView) itemView.findViewById(R.id.iv_profile_pic);
            userName = (TextView) itemView.findViewById(R.id.tv_name);
            userHandle = (TextView) itemView.findViewById(R.id.tv_user_handle);
            unBlock = (Button) itemView.findViewById(R.id.btn_unblock);
            Typeface font = Typeface.createFromAsset(mContext.getAssets(), "fonts/Montserrat-Regular.otf");
            unBlock.setTypeface(font);
        }
    }
}
