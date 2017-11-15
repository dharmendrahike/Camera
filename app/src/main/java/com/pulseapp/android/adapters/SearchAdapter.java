package com.pulseapp.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.data.MemoryCachedUsersData;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.AroundYouFragment;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.DashBoardFragment;
import com.pulseapp.android.fragments.SearchFragment;
import com.pulseapp.android.modelView.SearchResultModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by deepankur on 7/11/16.
 */
public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    private final int searchTag;
    private Context context;
    private LinkedList<SearchResultModel> linkedSearchModel;
    private final int ITEM_VIEW_TYPE = 11, HEADER_VIEW_TYPE = 22;
    private FireBaseHelper fireBaseHelper;
    private String myUserId;
    private static String TAG = SearchAdapter.class.getSimpleName();
    public static DashBoardFragment dashboardFragment;

    public SearchAdapter(int searchTag, Context context, ArrayList<SearchResultModel> searchResultModels) {
        this.searchTag = searchTag;
        this.context = context;
        this.fireBaseHelper = FireBaseHelper.getInstance(context);
        this.myUserId = fireBaseHelper.getMyUserId();
        if (searchResultModels != null)
            linkedSearchModel = new LinkedList<>(searchResultModels);
        else
            linkedSearchModel = new LinkedList<>();
        if (searchResultModels != null && searchResultModels.size() > 0 && this.searchTag != SearchFragment.SEARCH_MY_FRIEND_STREAM) {
            linkedSearchModel.add(0, null);
        }
    }

    public void refreshList(ArrayList<SearchResultModel> searchResultModels) {
        linkedSearchModel = new LinkedList<>(searchResultModels);
        if (searchResultModels.size() > 0 && this.searchTag != SearchFragment.SEARCH_MY_FRIEND_STREAM)
            linkedSearchModel.add(0, null);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return linkedSearchModel.get(position) == null ? HEADER_VIEW_TYPE : ITEM_VIEW_TYPE;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == ITEM_VIEW_TYPE)
            return new VHItem(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_search_result, parent, false));

        if (viewType == HEADER_VIEW_TYPE) {
            MontserratRegularTextView tv = new MontserratRegularTextView(context);
            tv.setTextColor(Color.BLACK);
            tv.setAlpha(0.71f);
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setBackgroundColor(context.getResources().getColor(R.color.pulse_gray));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            switch (searchTag) {

                case SearchFragment.SEARCH_MY_FRIEND_STREAM:
                    tv.setText("SEARCH_MY_FRIEND_STREAM");
                    break;
                case SearchFragment.SEARCH_AROUND_YOU:
                    tv.setText("AROUND YOU");
                    break;
                case SearchFragment.SEARCH_ADD_BY_USERNAME:
                    tv.setText("ADD BY USERNAME");
                    break;
                case SearchFragment.SEARCH_FRIEND_REQUESTS:
                    tv.setText("FRIEND REQUESTS");
                    break;
                case SearchFragment.SEARCH_ALL_FRIENDS:
                    tv.setText("FRIENDS");
                    break;
                case SearchFragment.SEARCH_SUGGESTED_FRIENDS:
                    tv.setText("SUGGESTED FRIENDS");
                    break;
            }
            int padding12 = AppLibrary.convertDpToPixels(context, 12);
            int padding16 = AppLibrary.convertDpToPixels(context, 16);
            tv.setPadding(padding16, padding16, padding16, padding12);
            return new VHHeader(tv);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType + "  wtf ");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof VHHeader)
            return;

        VHItem holder = (VHItem) viewHolder;
        final SearchResultModel searchResultModel = linkedSearchModel.get(position);
        holder.rootView.setTag(searchResultModel);

        holder.rootView.setTag(searchResultModel);
        holder.nameTv.setText(searchResultModel.name);
        if (searchResultModel.roomType == FRIEND_ROOM) {
            Picasso.with(context).load(searchResultModel.imageUrl).transform(new RoundedTransformation()).into(holder.profileIv);
        } else if (searchResultModel.roomType == GROUP_ROOM)
            Picasso.with(context).load(R.drawable.transparent_image).
                    transform(new LetterTileRoundedTransformation(context, searchResultModel.name)).into(holder.profileIv);


        if (searchTag == SearchFragment.SEARCH_AROUND_YOU || searchTag == SearchFragment.SEARCH_ADD_BY_USERNAME || searchTag == SearchFragment.SEARCH_MY_FRIEND_STREAM || this.searchTag == SearchFragment.SEARCH_SUGGESTED_FRIENDS)
            Picasso.with(context).load(searchResultModel.imageUrl).transform(new RoundedTransformation()).into(holder.profileIv);

        if (this.searchTag == SearchFragment.SEARCH_ADD_BY_USERNAME || this.searchTag == SearchFragment.SEARCH_SUGGESTED_FRIENDS) {
            holder.handleTv.setText("@" + searchResultModel.handle);
        } else if (searchTag == SearchFragment.SEARCH_MY_FRIEND_STREAM) {
            holder.handleTv.setText(searchResultModel.momentUpdatedAt);
        } else if (this.searchTag == SearchFragment.SEARCH_FRIEND_REQUESTS) {
            if (searchResultModel.isRequestSent) {
                holder.acceptRejectLayout.setVisibility(View.GONE);
                holder.handleTv.setText("Friend request sent");
                holder.handleTv.setVisibility(View.VISIBLE);
            } else {
                holder.acceptRejectLayout.setVisibility(View.VISIBLE);
                holder.handleTv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return linkedSearchModel.size();
    }

    class VHHeader extends RecyclerView.ViewHolder {

        public VHHeader(View itemView) {
            super(itemView);
        }
    }

    class VHItem extends RecyclerView.ViewHolder {
        ImageView profileIv;
        TextView nameTv, handleTv;
        TextView ignoreTv, acceptTv;
        ImageView addFriendIv, requestSent;
        RelativeLayout rootView;
        LinearLayout acceptRejectLayout;

        public VHItem(View itemView) {

            super(itemView);
            rootView = (RelativeLayout) itemView;
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final SearchResultModel searchResultModel = (SearchResultModel) rootView.getTag();
                    if (searchTag == SearchFragment.SEARCH_AROUND_YOU) {
                        ((CameraActivity) context).getDashboardFragment().exitSearchMode();
                        (((AroundYouFragment) ((FragmentActivity) context).getSupportFragmentManager().
                                findFragmentByTag(AroundYouFragment.class.getSimpleName()))).
                                performSingleTapOnNearbyMoment(searchResultModel.searchId);
                    } else if (searchTag == SearchFragment.SEARCH_ALL_FRIENDS) {
                        if (searchResultModel.roomType == FRIEND_ROOM)
                            BaseFragment.getBaseFragmentInstance().showGenericProfilePopup(context, searchResultModel.name, searchResultModel.imageUrl, searchResultModel.handle);
                    } else if (searchTag == SearchFragment.SEARCH_MY_FRIEND_STREAM) {
                        dashboardFragment.onMomentClickedFromSearch(searchResultModel.searchId);
                    } else if (searchTag == SearchFragment.SEARCH_SUGGESTED_FRIENDS) {
                        BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(context, searchResultModel.searchId, searchResultModel.name, searchResultModel.imageUrl, searchResultModel.handle);
                    } else if (searchTag == SearchFragment.SEARCH_ADD_BY_USERNAME) {
                        BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(context, searchResultModel.searchId, searchResultModel.name, searchResultModel.imageUrl, searchResultModel.handle);
                    } else if (searchTag == SearchFragment.SEARCH_FRIEND_REQUESTS) {
                        BaseFragment.getBaseFragmentInstance().showIncomingProfilePopup(context, searchResultModel.searchId, searchResultModel.name, searchResultModel.imageUrl, searchResultModel.handle);
                    }
                }
            });

            acceptRejectLayout = (LinearLayout) itemView.findViewById(R.id.acceptRejectLL);
            profileIv = (ImageView) itemView.findViewById(R.id.cardIV);
            addFriendIv = (ImageView) itemView.findViewById(R.id.addFriendIV);
            requestSent = (ImageView) itemView.findViewById(R.id.requestSentIV);

            nameTv = (TextView) itemView.findViewById(R.id.cardNameTV);
            handleTv = (TextView) itemView.findViewById(R.id.cardHandleTV);

            ignoreTv = (TextView) itemView.findViewById(R.id.ignore_requestBTN);
            acceptTv = (TextView) itemView.findViewById(R.id.confirm_requestBTN);
            for (int i = 0; i < rootView.getChildCount(); i++) {
                rootView.getChildAt(i).setVisibility(View.GONE);
            }
            switch (searchTag) {
                case SearchFragment.SEARCH_MY_FRIEND_STREAM:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
                    handleTv.setVisibility(View.VISIBLE);
                    break;
                case SearchFragment.SEARCH_AROUND_YOU:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) nameTv.getLayoutParams();
                    params.addRule(RelativeLayout.CENTER_VERTICAL);
                    nameTv.setLayoutParams(params);
                    break;
                case SearchFragment.SEARCH_ADD_BY_USERNAME:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
                    handleTv.setVisibility(View.VISIBLE);
                    addFriendIv.setVisibility(View.VISIBLE);
                    break;
                case SearchFragment.SEARCH_FRIEND_REQUESTS:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
//                    acceptTv.setVisibility(View.VISIBLE);
//                    ignoreTv.setVisibility(View.VISIBLE);
                    acceptRejectLayout.setVisibility(View.VISIBLE);
//                    requestSent.setVisibility(View.VISIBLE);
                    handleTv.setVisibility(View.VISIBLE);
                    break;
                case SearchFragment.SEARCH_ALL_FRIENDS:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) nameTv.getLayoutParams();
                    params2.addRule(RelativeLayout.CENTER_VERTICAL);
                    nameTv.setLayoutParams(params2);

                    break;
                case SearchFragment.SEARCH_SUGGESTED_FRIENDS:
                    profileIv.setVisibility(View.VISIBLE);
                    nameTv.setVisibility(View.VISIBLE);
                    handleTv.setVisibility(View.VISIBLE);
                    addFriendIv.setVisibility(View.VISIBLE);
                    break;
            }
            if (searchTag == SearchFragment.SEARCH_ADD_BY_USERNAME || searchTag == SearchFragment.SEARCH_SUGGESTED_FRIENDS) {
                addFriendIv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchResultModel searchResultModel = (SearchResultModel) rootView.getTag();
                        sendFriendRequest(searchResultModel);
                    }
                });
            }

            if (searchTag == SearchFragment.SEARCH_FRIEND_REQUESTS) {
                ignoreTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchResultModel searchResultModel = (SearchResultModel) rootView.getTag();
                        Log.d(TAG, "Ignoring request of " + searchResultModel.name + " id " + searchResultModel.searchId);

                        ignoreRequest(searchResultModel);
                        final int i = linkedSearchModel.indexOf(searchResultModel);
                        linkedSearchModel.remove(searchResultModel);
                        notifyItemRemoved(i);

                    }
                });

                acceptTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchResultModel searchResultModel = (SearchResultModel) rootView.getTag();
                        Log.d(TAG, "Ignoring request of " + searchResultModel.name + " id " + searchResultModel.searchId);
                        acceptRequest(searchResultModel);
                        final int i = linkedSearchModel.indexOf(searchResultModel);
                        linkedSearchModel.remove(searchResultModel);
                        notifyItemRemoved(i);
                    }
                });
            }
        }
    }

    private void acceptRequest(SearchResultModel searchResultModel) {
        if (searchResultModel.roomType == FRIEND_ROOM) {
            updateDataToCache(searchResultModel);
//            fireBaseHelper.sendFriendRequest(myUserId, searchResultModel.searchId);
            fireBaseHelper.acceptFriendRequest(myUserId, searchResultModel.searchId);
        } else if (searchResultModel.roomType == GROUP_ROOM) {
            fireBaseHelper.acceptGroupRequest(myUserId, searchResultModel.searchId, fireBaseHelper.getSocialModel().pendingGroupRequest.get(searchResultModel.searchId));
        }
    }

    private void ignoreRequest(SearchResultModel searchResultModel) {
        if (searchResultModel.roomType == FRIEND_ROOM) {
            fireBaseHelper.ignoreFriendRequest(myUserId, searchResultModel.searchId);
        } else if (searchResultModel.roomType == GROUP_ROOM) {
            fireBaseHelper.ignoreGroupRequest(myUserId, searchResultModel.searchId);
        }
    }

    void updateDataToCache(SearchResultModel searchResultModel) {
        MemoryCachedUsersData data = MemoryCachedUsersData.getInstance(context);
        data.putUserData(searchResultModel);
    }

    void sendFriendRequest(SearchResultModel searchResultModel){
        Log.d(TAG, "Sending friend request to " + searchResultModel.searchId);

        Toast.makeText(context, "Friend request sent to " + searchResultModel.name, Toast.LENGTH_SHORT).show();
        updateDataToCache(searchResultModel);
        fireBaseHelper.sendFriendRequest(myUserId, searchResultModel.searchId);
        SearchFragment.updateIgnoreUserSearchList(searchResultModel.searchId);

        final int i = linkedSearchModel.indexOf(searchResultModel);
        linkedSearchModel.remove(searchResultModel);
        notifyItemRemoved(i);
    }
}
