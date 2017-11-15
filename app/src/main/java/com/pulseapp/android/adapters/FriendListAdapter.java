package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by abc on 4/15/2016.
 */
public class FriendListAdapter extends RecyclerView.Adapter {

    // View Type for Separators
    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    // View Type for Regular rows
    private static final int ITEM_VIEW_TYPE_REGULAR = 1;

    private List<ListRoomView> friendList;
    private Context mContext;
    private ViewControlsCallback viewControlsCallback;
    private String TAG = this.getClass().getSimpleName();
    public BaseFragment baseFragment;

    public FriendListAdapter(Fragment fragment, Context context, List<ListRoomView> list) {
        mContext = context;
        friendList = list;
        viewControlsCallback = (ViewControlsCallback) fragment;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = null;
        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_header, parent, false);
            return new FriendHeaderViewHolder(item);
        } else {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_friend_list_item, parent, false);
            return new FriendListViewHolder(item);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        int itemViewType = holder.getItemViewType();
        if (itemViewType == ITEM_VIEW_TYPE_HEADER) {
            // do nothing
//            ((FriendHeaderViewHolder)holder).headerText.setText(friendList.get(position).name.toUpperCase());
            ((FriendHeaderViewHolder) holder).headerText.setText(Character.toString((char) friendList.get(position + 1).normalizedFirstChar));//getting next list items name

        } else {

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (friendList.get(position).type == FireBaseKEYIDS.FRIEND_ROOM)
                        baseFragment.showGenericProfilePopup(mContext, friendList.get(position).name, friendList.get(position).imageUrl, friendList.get(position).handle);
                    return false;
                }
            });
            ((FriendListViewHolder) holder).profileName.setText(friendList.get(position).name);
            if (friendList.get(position).type == 1)
                Picasso.with(mContext).load(friendList.get(position).imageUrl).fit().centerCrop().transform(new RoundedTransformation()).placeholder(R.drawable.error_profile_picture).into(((FriendListViewHolder) holder).profileImage);
            else
                Picasso.with(mContext).load(R.drawable.transparent_image).fit().centerCrop().transform(new LetterTileRoundedTransformation(mContext, friendList.get(position).name)).placeholder(R.drawable.error_profile_picture).into(((FriendListViewHolder) holder).profileImage);
            ((FriendListViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((FriendListViewHolder) holder).checkBox.isChecked())
                        ((FriendListViewHolder) holder).checkBox.setChecked(false);
                    else
                        ((FriendListViewHolder) holder).checkBox.setChecked(true);
                    viewControlsCallback.onFriendItemClicked(friendList.get(position), ((FriendListViewHolder) holder).checkBox.isChecked());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    @Override
    public int getItemViewType(int position) {
        boolean isSection = /*friendList.get(position).hasHeader;*/friendList.get(position) == null;
        if (isSection) {
            return ITEM_VIEW_TYPE_HEADER;
        } else {
            return ITEM_VIEW_TYPE_REGULAR;
        }
    }

    public class FriendListViewHolder extends RecyclerView.ViewHolder {

        private ImageView profileImage;
        private TextView profileName;
        private CheckBox checkBox;
        private View itemView;

        public FriendListViewHolder(View itemView) {
            super(itemView);
            profileImage = (ImageView) itemView.findViewById(R.id.profileImage);
            profileName = (TextView) itemView.findViewById(R.id.profileName);
            checkBox = (CheckBox) itemView.findViewById(R.id.itemChecked);
            this.itemView = itemView;
        }
    }

    public class FriendHeaderViewHolder extends RecyclerView.ViewHolder {

        private TextView headerText;

        public FriendHeaderViewHolder(View itemView) {
            super(itemView);
            headerText = (TextView) itemView.findViewById(R.id.headerText);
        }
    }

    public interface ViewControlsCallback {
        void onFriendItemClicked(ListRoomView friendItem, boolean isChecked);
    }
}
