package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

/**
 * Created by deepankur on 16/4/16.
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private HashMap<String, SocialModel.RequestReceived> mRequestReceivedMap;
    public static Context mContext;
    private FireBaseHelper mFireBaseHelper;
    private RecyclerViewClickInterface mClickInterface;
    private String myUserId;
    private String[] requestKeySet;
    private HashMap<String, SocialModel.RequestReceived> friendRequestReceivedMap;

    public FriendRequestAdapter(HashMap<String, SocialModel.RequestReceived> requestReceivedMap, Context mContext, RecyclerViewClickInterface clickInterface) {
        this.mRequestReceivedMap = requestReceivedMap;
        this.requestKeySet = mRequestReceivedMap.keySet().toArray(new String[mRequestReceivedMap.size()]);
        this.mContext = mContext;
        this.mFireBaseHelper = FireBaseHelper.getInstance(mContext);
        this.myUserId = mFireBaseHelper.getMyUserId();
        this.mClickInterface = clickInterface;
        this.refreshData();
    }

    public void refreshData() {
        this.requestKeySet = mRequestReceivedMap.keySet().toArray(new String[mRequestReceivedMap.size()]);
        notifyDataSetChanged();
    }

    @Override
    public FriendRequestAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_incoming_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        SocialModel.RequestReceived request = mRequestReceivedMap.get(requestKeySet[position]);
        holder.getAdapterPosition();
        holder.rootView.setTag(request);
        holder.nameIv.setText(request.name);
        Picasso.with(mContext).load(request.imageUrl).transform(new RoundedTransformation()).into(holder.profileImageIv);

        holder.acceptRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendRequestTo = ((requestKeySet[holder.getAdapterPosition()]));
                mRequestReceivedMap.remove(requestKeySet[holder.getAdapterPosition()]);
                mFireBaseHelper.getPendingFriendRequests().remove(requestKeySet[holder.getAdapterPosition()]);
                mFireBaseHelper.acceptFriendRequest(myUserId, sendRequestTo);
                if (mClickInterface != null)
                    mClickInterface.onItemClick(mRequestReceivedMap.size(), false);

            }
        });

        holder.ignoreRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendRequestTo = ((requestKeySet[holder.getAdapterPosition()]));
                mRequestReceivedMap.remove(requestKeySet[holder.getAdapterPosition()]);
                mFireBaseHelper.getPendingFriendRequests().remove(requestKeySet[holder.getAdapterPosition()]);
                mFireBaseHelper.ignoreFriendRequest(myUserId, sendRequestTo);
                if (mClickInterface != null)
                    mClickInterface.onItemClick(mRequestReceivedMap.size(), true);

            }
        });
    }

    @Override
    public int getItemCount() {
        return mRequestReceivedMap.size();
    }

    public void setFriendRequestReceivedMap(HashMap<String, SocialModel.RequestReceived> friendRequestReceivedMap) {
        this.friendRequestReceivedMap = friendRequestReceivedMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameIv;
        ImageView profileImageIv;
        View rootView;
        TextView acceptRequest, ignoreRequest;

        public ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SocialModel.RequestReceived request = (SocialModel.RequestReceived) rootView.getTag();
                    BaseFragment.getBaseFragmentInstance().showGenericProfilePopup(mContext, request.name, request.imageUrl, request.handle);
                    return false;
                }
            });
            nameIv = (TextView) itemView.findViewById(R.id.card_displayNameTV);
            profileImageIv = (ImageView) itemView.findViewById(R.id.card_displayIV);
            acceptRequest = (TextView) itemView.findViewById(R.id.confirm_requestBTN);
            ignoreRequest = (TextView) itemView.findViewById(R.id.ignore_requestBTN);

        }
    }
}
