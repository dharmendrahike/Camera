package com.pulseapp.android.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.LinkedHashMap;

/**
 * Created by deepankur on 5/26/16.
 */
public class GroupRequestFragment extends BaseFragment {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private String TAG = this.getClass().getSimpleName();
    private View rootView;
    private LinkedHashMap<String, SocialModel.PendingGroupRequest> pendingGroupRequests;
    private DatabaseReference groupRequestFireBase;
    private GroupRequestAdapter groupRequestAdapter;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingGroupRequests = new LinkedHashMap<>();

        groupRequestFireBase = mFireBaseHelper.getNewFireBase(ANCHOR_SOCIALS, new String[]{myUserId, PENDING_GROUP_REQUEST});
        groupRequestFireBase.addChildEventListener(groupRequestListener);
    }

    private ChildEventListener groupRequestListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            SocialModel.PendingGroupRequest request = dataSnapshot.getValue(SocialModel.PendingGroupRequest.class);
            request.roomId = dataSnapshot.getKey();
            pendingGroupRequests.put(dataSnapshot.getKey(), request);
            reValidateFragmentVisibility();
            groupRequestAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            pendingGroupRequests.remove(dataSnapshot.getKey());
            reValidateFragmentVisibility();
            groupRequestAdapter.notifyDataSetChanged();

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflateRootView();
        reValidateFragmentVisibility();
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        this.rootView = rootView;
        return rootView;
    }

    private void reValidateFragmentVisibility() {
        if (rootView != null)
            this.rootView.setVisibility(pendingGroupRequests == null ? View.GONE : (pendingGroupRequests.size() == 0 ? View.GONE : View.VISIBLE));
    }

    private View inflateRootView() {
        recyclerView = new RecyclerView(context);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        LinearLayout rootView = new LinearLayout(context);
        rootView.setOrientation(LinearLayout.VERTICAL);

        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setBackgroundColor(Color.WHITE);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        MontserratRegularTextView textView = new MontserratRegularTextView(context);
        textView.setTextColor(Color.BLACK);
        textView.setAlpha(0.71f);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        float dpToPixels = AppLibrary.convertDpToPixels(context, 1);
        textView.setPadding((int) dpToPixels * 16, (int) dpToPixels * 16, 0, (int) dpToPixels * 12);
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText("GROUP REQUESTS");
        initRecyclerView();
        rootView.addView(textView);
        rootView.addView(recyclerView);
        this.rootView = rootView;
        return rootView;
    }


    private void initRecyclerView() {
        groupRequestAdapter = new GroupRequestAdapter(context, pendingGroupRequests, new RecyclerViewClickInterface() {
            @Override
            public void onItemClick(int extras, Object data) {
                reValidateFragmentVisibility();
            }
        });
        recyclerView.setAdapter(groupRequestAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyViewCalled");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroyCalled");
        groupRequestFireBase.removeEventListener(groupRequestListener);
    }
}

class GroupRequestAdapter extends RecyclerView.Adapter<GroupRequestAdapter.ViewHolder> {
    private Context context;
    private String TAG = this.getClass().getSimpleName();
    public FireBaseHelper fireBaseHelper;
    private RecyclerViewClickInterface clickInterface;
    private LinkedHashMap<String, SocialModel.PendingGroupRequest> groupRequestsMap;

    public GroupRequestAdapter(Context context, LinkedHashMap<String, SocialModel.PendingGroupRequest> groupRequestsMap, RecyclerViewClickInterface clickInterface) {
        this.context = context;
        this.groupRequestsMap = groupRequestsMap;
        this.fireBaseHelper = FireBaseHelper.getInstance(context);
        this.clickInterface = clickInterface;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_incoming_request, parent, false);
        return new ViewHolder(v);
    }

    private SocialModel.PendingGroupRequest getRequestByIndex(int index) {
        String[] keys = groupRequestsMap.keySet().toArray(new String[groupRequestsMap.size()]);
        return groupRequestsMap.get(keys[index]);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SocialModel.PendingGroupRequest request = getRequestByIndex(position);
        holder.groupNameTv.setText(request.name);
        if (request.imageUrl != null && !request.imageUrl.isEmpty())
            Picasso.with(context).load(request.imageUrl).transform(new RoundedTransformation()).
                    into(holder.groupIconIv);
        else {
            Transformation t = new LetterTileRoundedTransformation(context, request.name);
            Picasso.with(context).load(R.drawable.transparent_image).
                    transform(t).into(holder.groupIconIv);
        }
        holder.confirmBtn.setTag(request);
        holder.ignoreBtn.setTag(request);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount");
        return groupRequestsMap == null ? 0 : groupRequestsMap.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View rootView;
        TextView groupNameTv;
        ImageView groupIconIv;
        TextView confirmBtn, ignoreBtn;
        int diameter = context.getResources().getDimensionPixelOffset(R.dimen.profile_pic_diameter);

        public ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            groupNameTv = (TextView) itemView.findViewById(R.id.card_displayNameTV);
            groupIconIv = (ImageView) itemView.findViewById(R.id.card_displayIV);
/*            groupIconIv.getLayoutParams().height = diameter;
            groupIconIv.getLayoutParams().width = diameter;
            groupIconIv.setBackgroundColor(Color.parseColor("#ff0000"));*/
            confirmBtn = (TextView) itemView.findViewById(R.id.confirm_requestBTN);
            ignoreBtn = (TextView) itemView.findViewById(R.id.ignore_requestBTN);
            ignoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //ignore friend request
                    SocialModel.PendingGroupRequest request = (SocialModel.PendingGroupRequest) confirmBtn.getTag();
                    Log.d(TAG, " onIgnore group: " + request.name);
                    notifyItemRemoved(getRequestIndexByKey(request.roomId));
                    groupRequestsMap.remove(request.roomId);
                    clickInterface.onItemClick(0, request.roomId);
                    fireBaseHelper.ignoreGroupRequest(fireBaseHelper.getMyUserId(), request.roomId);
                }
            });

            confirmBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //accept group request
                    SocialModel.PendingGroupRequest request = (SocialModel.PendingGroupRequest) confirmBtn.getTag();
                    notifyItemRemoved(getRequestIndexByKey(request.roomId));
                    groupRequestsMap.remove(request.roomId);
                    clickInterface.onItemClick(0, request.roomId);
                    Log.d(TAG, " onConfirm group: " + request.name);
                    fireBaseHelper.acceptGroupRequest(fireBaseHelper.getMyUserId(), request.roomId, request);
                }
            });
        }
    }

    public int getRequestIndexByKey(String roomId) {
        String[] keySet = groupRequestsMap.keySet().toArray(new String[groupRequestsMap.size()]);
        for (int i = 0; i < keySet.length; i++) {
            if (roomId.equals(keySet[i])) {
                Log.d(TAG, " index is " + i);
                return i;
            }
        }
        throw new RuntimeException("no room found");
    }
}