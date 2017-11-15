package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.pulseapp.android.R;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.modelView.FaceBookFriendsModel;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by deepankur on 16/4/16.
 */
public class AddFriendFromFBAdapter extends RecyclerView.Adapter<AddFriendFromFBAdapter.ViewHolder> {

    private ArrayList<FaceBookFriendsModel> userModelArrayList;
    private Context mContext;
    private FireBaseHelper mFireBaseHelper;
    private RecyclerViewClickInterface recyclerViewClickInterface;
    private ArrayList<FaceBookFriendsModel> facebookFriendsList;

    public AddFriendFromFBAdapter(ArrayList<FaceBookFriendsModel> userModelArrayList, Context mContext, RecyclerViewClickInterface recyclerViewClickInterface) {
        this.userModelArrayList = userModelArrayList;
        this.mContext = mContext;
        this.mFireBaseHelper = FireBaseHelper.getInstance(mContext);
        this.recyclerViewClickInterface = recyclerViewClickInterface;
    }

    @Override
    public AddFriendFromFBAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_suggested_friend_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (position >= userModelArrayList.size()){//temporary fix
            notifyDataSetChanged();
            return;
        }

        final FaceBookFriendsModel userModel = userModelArrayList.get(position);
        holder.rootView.setTag(userModel);
        holder.nameIv.setText(userModel.name);
        holder.handleTv.setText("@" + userModel.handle);
        Picasso.with(mContext).load(userModel.imageUrl).transform(new RoundedTransformation()).into(holder.profileImageIv);
        holder.addFriendIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFireBaseHelper.sendFriendRequest(mFireBaseHelper.getMyUserId(), userModel.userId);
                Toast.makeText(mContext, "Sent friend request to " + userModelArrayList.get(holder.getAdapterPosition()).name, Toast.LENGTH_SHORT).show();
                userModelArrayList.remove(userModelArrayList.get(holder.getAdapterPosition()));
                notifyItemRemoved((holder.getAdapterPosition()));
                recyclerViewClickInterface.onItemClick(userModelArrayList.size(),null);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userModelArrayList == null ? 0 : userModelArrayList.size();
    }

    public void setFacebookFriendsList(ArrayList<FaceBookFriendsModel> facebookFriendsList) {
        this.facebookFriendsList = facebookFriendsList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameIv;
        ImageView profileImageIv, addFriendIv;
        TextView handleTv;
        View rootView;

        public ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    FaceBookFriendsModel friendsModel = (FaceBookFriendsModel) rootView.getTag();
                    BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(BaseFragment.getBaseFragmentInstance().getContext(),
                            friendsModel.userId, friendsModel.name, friendsModel.imageUrl, friendsModel.handle);
                    return false;
                }
            });
            nameIv = (TextView) itemView.findViewById(R.id.cardNameTV);
            profileImageIv = (ImageView) itemView.findViewById(R.id.profilePicIV);
            addFriendIv = (ImageView) itemView.findViewById(R.id.friendSelectedIV);
            handleTv = (TextView) itemView.findViewById(R.id.cardHandleTV);
            addFriendIv.setImageResource(R.drawable.add_svg);
        }
    }
}
