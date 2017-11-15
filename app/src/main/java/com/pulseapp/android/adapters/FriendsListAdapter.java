package com.pulseapp.android.adapters;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.FriendFollowerFragment;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by indianrenters on 8/3/16.
 */
public class FriendsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements BaseFragment.FollowPopupListener, FireBaseKEYIDS{

    private Context context;
    private ArrayList<String> allKeys;
    private LayoutInflater inflater;
    private HashMap<String, SocialModel.Friends> friendsHashMap;
    private int TYPE_HEADER = 123;
    private int TYPE_ITEM = 272;
    private String currentPage;
    private FriendFollowerFragment friendFollowerFragment;

    public FriendsListAdapter(FriendFollowerFragment friendFollowerFragment, String currentPage, Context context, HashMap<String, SocialModel.Friends> friendsListHashMap, List<String> normalisedKeys) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        friendsHashMap = friendsListHashMap;
        allKeys = (ArrayList<String>) normalisedKeys;
        this.currentPage = currentPage;
        BaseFragment.getBaseFragmentInstance().setFollowPopupListener(this);
        this.friendFollowerFragment = friendFollowerFragment;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View itemView = inflater.inflate(R.layout.card_all_chat_list, parent, false);
            return new VHItem(itemView);
        } else if (viewType == TYPE_HEADER) {
            return new VHHeader(new TextView(context));
        }
        throw new RuntimeException("There is no type match for viewType " + viewType);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VHItem && getItem(position) != null) {
            ((VHItem) holder).name.setText(getItem(position).name);
            Picasso.with(context).load(getItem(position).imageUrl).fit().centerCrop().transform(new RoundedTransformation()).into(((VHItem) holder).imageView);
//            ((VHItem) holder).rootView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(
//                            context,
//                            allKeys.get(holder.getAdapterPosition()),
//                            getItem(holder.getAdapterPosition()).name,
//                            getItem(holder.getAdapterPosition()).imageUrl,
//                            getItem(holder.getAdapterPosition()).handle);
//                    return true;
//                }
//            });

            ((VHItem) holder).rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPage.equals(FRIEND)) {
                        BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(
                                context,
                                allKeys.get(holder.getAdapterPosition()),
                                getItem(holder.getAdapterPosition()).name,
                                getItem(holder.getAdapterPosition()).imageUrl,
                                getItem(holder.getAdapterPosition()).handle);
                    } else {
                        BaseFragment.getBaseFragmentInstance().showFollowPopup(0, context, allKeys.get(holder.getAdapterPosition()),  getItem(holder.getAdapterPosition()).name,
                                getItem(holder.getAdapterPosition()).imageUrl,
                                getItem(holder.getAdapterPosition()).handle, true ,false);
                    }
                }
            });
        } else if (holder instanceof VHHeader && allKeys.get(position).equals("header") && getItem(position + 1) != null) {
            if (getItem(position + 1).name != null) {
                char displayAlphabet = getItem(position + 1).name.charAt(0);
                ((VHHeader) holder).alphabet.setText(String.valueOf(Character.toUpperCase(displayAlphabet)));
            }
        }
    }

    @Override
    public int getItemCount() {
        return allKeys.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (allKeys.get(position).equals("header") ? TYPE_HEADER : TYPE_ITEM);
    }

    private SocialModel.Friends getItem(int position) {
        if (allKeys != null && position < allKeys.size() && friendsHashMap != null && !friendsHashMap.isEmpty()) {
            return friendsHashMap.get(allKeys.get(position));
        }

        return null;
    }

    @Override
    public void onButtonClicked(View view, String userId, Dialog popup) {
        boolean following = (boolean)view.getTag();
        if (following) {
            friendsHashMap.remove(userId);
            if (friendsHashMap.size() > 0)
                allKeys = (ArrayList<String>)friendFollowerFragment.getNormalisedKeys(friendsHashMap);
            else
                allKeys = new ArrayList<>();
            notifyDataSetChanged();
            BaseFragment.getBaseFragmentInstance().dismissPopup(popup);
            friendFollowerFragment.updateSocialCount();
        }
        ((CameraActivity)context).updateFollowersList(!following);
    }

    @Override
    public void onPopupDismiss() {

    }

    class VHItem extends RecyclerView.ViewHolder {
        TextView name;
        ImageView imageView;
        View rootView;

        public VHItem(View itemView) {
            super(itemView);
            rootView = itemView;
            name = (TextView) itemView.findViewById(R.id.cardTV);
            imageView = (ImageView) itemView.findViewById(R.id.cardIV);
        }
    }

    class VHHeader extends RecyclerView.ViewHolder {
        TextView alphabet;
        FontPicker fontPicker = FontPicker.getInstance(context);

        public VHHeader(View itemView) {
            super(itemView);
            alphabet = (TextView) itemView;
            alphabet.setTypeface(fontPicker.getMontserratRegular());
            alphabet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            alphabet.setMinHeight(AppLibrary.convertDpToPixels(context, 22));
            alphabet.setGravity(Gravity.CENTER_VERTICAL);
            alphabet.setPadding(AppLibrary.convertDpToPixels(context, 16), 0, 0, 0);
            alphabet.setAlpha(0.54f);
        }
    }
}
