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
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by user on 5/9/2016.
 */
public class RemoveFriendListAdapter extends HeaderRecyclerViewAdapterV2 {
    // View Type for Separators
    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    // View Type for Regular rows
    private static final int ITEM_VIEW_TYPE_REGULAR = 1;
    private static final int ITEM_VIEW_TYPE_COUNT = 2;
    private List<ListRoomView> friendList;
    private Context mContext;
    private OnListModified viewControlsCallback;

    public RemoveFriendListAdapter(Fragment fragment, Context context, List<ListRoomView> list){
        mContext = context;
        friendList = list;
        viewControlsCallback = (OnListModified)fragment;
    }

    public class RemoveFriendListViewHolder extends RecyclerView.ViewHolder{

        private ImageView profileImage;
        private TextView profileName;
        private CheckBox checkBox;
        private View itemView;

        public RemoveFriendListViewHolder(View itemView) {
            super(itemView);
            profileImage = (ImageView) itemView.findViewById(R.id.profileImage);
            profileName = (TextView) itemView.findViewById(R.id.profileName);
            checkBox = (CheckBox) itemView.findViewById(R.id.itemChecked);
            this.itemView = itemView;
        }
    }

    public class FriendHeaderViewHolder extends RecyclerView.ViewHolder{

        private TextView headerText;

        public FriendHeaderViewHolder(View itemView) {
            super(itemView);
            headerText = (TextView) itemView.findViewById(R.id.headerText);
        }
    }

    @Override
    public boolean useHeader() {
        return false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindHeaderView(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public boolean useFooter() {
        return false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateFooterViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindFooterView(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public RecyclerView.ViewHolder onCreateBasicItemViewHolder(ViewGroup parent, int viewType) {
        View item = null;
        if (viewType == ITEM_VIEW_TYPE_HEADER) {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_header,parent,false);
            return new FriendHeaderViewHolder(item);
        } else {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_friend_list_item, parent, false);
            return new RemoveFriendListViewHolder(item);
        }
    }

    @Override
    public void onBindBasicItemView(final RecyclerView.ViewHolder holder, final int position) {
        int itemViewType = holder.getItemViewType();
        if (itemViewType == ITEM_VIEW_TYPE_HEADER) {
            // do nothing
        } else {
            if (friendList.get(position).type == 2) {
                ((RemoveFriendListViewHolder) holder).itemView.setVisibility(View.GONE);
            } else {
                if (friendList.get(position).isChecked){
                    ((RemoveFriendListViewHolder) holder).checkBox.setChecked(true);
                } else {
                    ((RemoveFriendListViewHolder) holder).checkBox.setChecked(false);
                }
                ((RemoveFriendListViewHolder) holder).profileName.setText(friendList.get(position).name);
                Picasso.with(mContext).load(friendList.get(position).imageUrl).fit().transform(new RoundedTransformation()).into(((RemoveFriendListViewHolder) holder).profileImage);
                ((RemoveFriendListViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (((RemoveFriendListViewHolder) holder).checkBox.isChecked()) {
                            ((RemoveFriendListViewHolder) holder).checkBox.setChecked(false);
                            friendList.get(position).setItemChecked(false);
                        } else {
                            ((RemoveFriendListViewHolder) holder).checkBox.setChecked(true);
                            friendList.get(position).setItemChecked(true);
                        }
                        viewControlsCallback.onItemClicked(friendList.get(position), ((RemoveFriendListViewHolder) holder).checkBox.isChecked());
                    }
                });
            }
        }
    }

    @Override
    public int getBasicItemCount() {
        return friendList.size();
    }

    @Override
    public int getBasicItemType(int position) {
        boolean isSection = false;
        if (isSection) {
            return ITEM_VIEW_TYPE_HEADER;
        } else {
            return ITEM_VIEW_TYPE_REGULAR;
        }
    }

    public interface OnListModified{
        void onItemClicked(ListRoomView friendItem,boolean isChecked);
    }
}
