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
public class RecentFriendListAdapter extends HeaderRecyclerViewAdapterV2 {

    // View Type for Separators
    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    // View Type for Regular rows
    private static final int ITEM_VIEW_TYPE_REGULAR = 1;
    private static final int ITEM_VIEW_TYPE_COUNT = 2;
    private Context mContext;
    private List<ListRoomView> roomList;
    private ViewControlsCallback viewControlsCallback;


    public RecentFriendListAdapter(Fragment fragment, Context context, List<ListRoomView> list) {
        mContext = context;
        roomList = list;
        viewControlsCallback = (ViewControlsCallback) fragment;
    }

    public class RecentListViewHolder extends RecyclerView.ViewHolder {

        private ImageView profileImage;
        private TextView profileName;
        private CheckBox checkBox;
        private View itemView;

        public RecentListViewHolder(final View itemView) {
            super(itemView);
            profileImage = (ImageView) itemView.findViewById(R.id.profileImage);
            profileName = (TextView) itemView.findViewById(R.id.profileName);
            checkBox = (CheckBox) itemView.findViewById(R.id.itemChecked);
            this.itemView = itemView;
            this.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final ListRoomView tag = (ListRoomView) itemView.getTag();
                    if (tag.type == FireBaseKEYIDS.FRIEND_ROOM)
                        baseFragment.showGenericProfilePopup(mContext, tag.name, tag.imageUrl, tag.handle);
                    return false;
                }
            });
        }

    }

    public BaseFragment baseFragment;

    public class FooterViewHolder extends RecyclerView.ViewHolder {

        private TextView footerText;

        public FooterViewHolder(View itemView) {
            super(itemView);
            footerText = (TextView) itemView.findViewById(R.id.footerText);
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
        return true;
    }

    @Override
    public RecyclerView.ViewHolder onCreateFooterViewHolder(ViewGroup parent, int viewType) {
        View footerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_friend_list_footer, parent, false);
        return new FooterViewHolder(footerView);
    }

    @Override
    public void onBindFooterView(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public RecyclerView.ViewHolder onCreateBasicItemViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_friend_list_item, parent, false);
        return new RecentListViewHolder(rowItem);
    }

    @Override
    public void onBindBasicItemView(final RecyclerView.ViewHolder holder, final int position) {
        int itemViewType = holder.getItemViewType();
        if (itemViewType == ITEM_VIEW_TYPE_HEADER) {
            // do nothing
        } else {

            holder.itemView.setTag(roomList.get(position));
            ((RecentListViewHolder) holder).profileName.setText(roomList.get(position).name);
            if (roomList.get(position).type == 1)
                Picasso.with(mContext).load(roomList.get(position).imageUrl).fit().centerCrop().transform(new RoundedTransformation()).placeholder(R.drawable.error_profile_picture).into(((RecentListViewHolder) holder).profileImage);
            else
                Picasso.with(mContext).load(R.drawable.transparent_image).fit().centerCrop().transform(new LetterTileRoundedTransformation(mContext, roomList.get(position).name)).placeholder(R.drawable.error_profile_picture).into(((RecentListViewHolder) holder).profileImage);
            ((RecentListViewHolder) holder).itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (((RecentListViewHolder) holder).checkBox.isChecked())
                        ((RecentListViewHolder) holder).checkBox.setChecked(false);
                    else
                        ((RecentListViewHolder) holder).checkBox.setChecked(true);
                    viewControlsCallback.onRecentFriendItemClicked(roomList.get(position), ((RecentListViewHolder) holder).checkBox.isChecked());
                }
            });
        }
    }

    @Override
    public int getBasicItemCount() {
        return roomList.size();
    }

    @Override
    public int getBasicItemType(int position) {
        return ITEM_VIEW_TYPE_REGULAR;
    }

    public interface ViewControlsCallback {
        void onRecentFriendItemClicked(ListRoomView friendItem, boolean isChecked);
    }
}
