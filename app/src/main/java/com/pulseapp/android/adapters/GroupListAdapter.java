package com.pulseapp.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.fragments.CreateManageGroupFragment;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by abc on 4/18/2016.
 */
public class GroupListAdapter extends HeaderRecyclerViewAdapterV2 {

    // View Type for Separators
    private static final int ITEM_VIEW_TYPE_HEADER = 0;
    // View Type for Regular rows
    private static final int ITEM_VIEW_TYPE_REGULAR = 1;
    private static final int ITEM_VIEW_TYPE_COUNT = 2;
    private List<ListRoomView> friendList;
    private RoomsModel.Members[] existingGroupList;
    private ViewControlsCallback viewControlsCallback;
    private CreateManageGroupFragment fragment;

    public GroupListAdapter(Fragment fragment, List<ListRoomView> list) {
        viewControlsCallback = (ViewControlsCallback) fragment;
        this.fragment = (CreateManageGroupFragment) fragment;
        this.friendList = list;
    }

    public void refreshFriendList(List<ListRoomView> list) {
        this.friendList = list;
        notifyDataSetChanged();
    }

    public GroupListAdapter(Fragment fragment, RoomsModel.Members[] list) {
        viewControlsCallback = (ViewControlsCallback) fragment;
        this.fragment = (CreateManageGroupFragment) fragment;
        existingGroupList = list;
    }


    public class MemberListViewHolder extends RecyclerView.ViewHolder {

        private ImageView profileImage;
        private TextView profileName;
        private CheckBox checkBox;
        private View itemView;

        public MemberListViewHolder(View itemView) {
            super(itemView);
            profileImage = (ImageView) itemView.findViewById(R.id.profileImage);
            profileName = (TextView) itemView.findViewById(R.id.profileName);
            checkBox = (CheckBox) itemView.findViewById(R.id.itemChecked);
            this.itemView = itemView;
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
        item = LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_friend_list_item, parent, false);
        return new MemberListViewHolder(item);
    }

    @Override
    public void onBindBasicItemView(final RecyclerView.ViewHolder holder, final int position) {
        if (friendList.get(position).type == 2) {
            ((MemberListViewHolder) holder).itemView.setVisibility(View.GONE);
            return;
        } else {
            ((MemberListViewHolder) holder).itemView.setVisibility(View.VISIBLE);
        }
        if (friendList != null) {
            ((MemberListViewHolder) holder).profileName.setText(friendList.get(position).name);
            Picasso.with(fragment.getContext()).load(friendList.get(position).imageUrl).fit().transform(new RoundedTransformation()).into(((MemberListViewHolder) holder).profileImage);
        } else if (existingGroupList != null) {
            ((MemberListViewHolder) holder).checkBox.setChecked(true);
            ((MemberListViewHolder) holder).profileName.setText(existingGroupList[position].name);
            Picasso.with(fragment.getContext()).load(existingGroupList[position].imageUrl).fit().transform(new RoundedTransformation()).into(((MemberListViewHolder) holder).profileImage);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MemberListViewHolder) holder).checkBox.isChecked()) {
                    ((MemberListViewHolder) holder).checkBox.setChecked(false);
                } else {
                    ((MemberListViewHolder) holder).checkBox.setChecked(true);
                }
                if (friendList != null)
                    viewControlsCallback.onMemberClicked(friendList.get(position));
                else
                    viewControlsCallback.onExistingGroupMemberClicked(existingGroupList[position]);
            }
        });
    }

    @Override
    public int getBasicItemCount() {
        return friendList == null ? 0 : friendList.size();
    }

    @Override
    public int getBasicItemType(int position) {
        return ITEM_VIEW_TYPE_REGULAR;
    }

    public interface ViewControlsCallback {
        void onMemberClicked(ListRoomView friendObject);

        void onExistingGroupMemberClicked(RoomsModel.Members memberObject);
    }
}
