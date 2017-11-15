package com.pulseapp.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.HomeMomentAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.models.SocialModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by deepankur on 22/4/16.
 */
public class MomentListFragment extends BaseFragment {
    private RecyclerView recyclerView;
    private String TAG = this.getClass().getSimpleName();
    private int VIEW_TYPE;
    private HomeMomentAdapter momentAdapter;
    private TextView headerTv;
    private ArrayList<HomeMomentViewModel> momentViewModelArrayList;
    private View noMomentsLayout;
    private ViewControlsCallback viewControlsCallback;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        VIEW_TYPE = data.getInt(MOMENT_VIEW_TYPE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.moment_recycler_view, container, false);
//        initRecyclerView(rootView);
        int dps_16 = AppLibrary.convertDpToPixels(getActivity(), 16);
        noMomentsLayout = rootView.findViewById(R.id.no_moments_layout);
        View includedNoMomentView = rootView.findViewById(R.id.include_no_moment_layout);
        includedNoMomentView.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraActivity cameraActivity = (CameraActivity) getActivity();
                if (cameraActivity != null)
                    cameraActivity.requestAddFriendFragmentOpen();
            }
        });
        String s = null;
        if (VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER) {
            recyclerView.setPadding(dps_16, dps_16 / 2, 0, dps_16 / 2);
            s = "SEEN";
        }
        if (VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER) {
            s = "RECENT UPDATES";
            recyclerView.setPadding(dps_16, 0, 0, dps_16 / 2);
        }
        if (VIEW_TYPE == FAVOURITE_MOMENT_RECYCLER)
            s = "favourites Moment";
        headerTv.setText(s);
        refreshLayout();
        return rootView;
    }

    private AddFriendListener addFriendListener;

    public void updateSeenRoomModelArrayList(String momentId) {
        for (int i = 0; i < momentViewModelArrayList.size(); i++) {
            if (momentViewModelArrayList.get(i).momentId.equals(momentId)) {
                momentViewModelArrayList.get(i).momentStatus = READY_AND_SEEN_MOMENT;
                break;
            }
        }
        momentAdapter.setMomentModelArrayList(momentViewModelArrayList);
//        momentAdapter.notifyDataSetChanged();
        refreshLayout();
    }

    public interface AddFriendListener {
        void requestOpenAddFriendFragment();
    }

    public void setAddFriendListener(AddFriendListener addFriendListener) {
        this.addFriendListener = addFriendListener;
    }

//    private void initRecyclerView(View rootView) {
//        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
//        recyclerView.setNestedScrollingEnabled(false);
//        headerTv = (TextView) rootView.findViewById(R.id.recyclerHeader);
//        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER ? 4 : 3));
//        momentAdapter = new HomeMomentAdapter(myUserId, getActivity(), momentViewModelArrayList, VIEW_TYPE, new RecyclerViewClickInterface() {
//            @Override
//            public void onItemClick(int position, Object data) {
//                HomeMomentViewModel model = (HomeMomentViewModel) data;
//                String momentId = model.momentId;
//                if (model.clickType == HomeMomentViewModel.ClickType.SINGLE_TAP) {
//                    Log.d(TAG, momentId + " singleTapped  status: " + model.momentStatus);
//                    if (model.momentStatus == UNSEEN_MOMENT)
//                        mFireBaseHelper.downloadMoment(momentId);
//                    if (model.momentStatus == READY_TO_VIEW_MOMENT || model.momentStatus == SEEN_MOMENT || model.momentStatus == READY_AND_SEEN_MOMENT)
//                        loadViewMomentFragment(momentId, model.momentStatus);
//                }
//                if (model.clickType == HomeMomentViewModel.ClickType.DOUBLE_TAP) {
//                    ((CameraActivity) getActivity()).loadChatFragmentOnDoubleTap(model.roomId);
//                    Log.d(TAG, " got double click on " + momentId);
//                }
//                if (model.momentStatus == UNSEEN_MOMENT && model.clickType == HomeMomentViewModel.ClickType.AUTO_DOWNLOAD) {
//                    mFireBaseHelper.downloadMoment(momentId);
//                }
//            }
//        });
//        recyclerView.setAdapter(momentAdapter);
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    public void setRoomsModelArrayList(ArrayList<HomeMomentViewModel> momentViewModelArrayList) {
        this.momentViewModelArrayList = momentViewModelArrayList;
        if (momentAdapter != null) {
            momentAdapter.setMomentModelArrayList(momentViewModelArrayList);
//            momentAdapter.notifyDataSetChanged();
        }
        refreshLayout();
        if (recyclerView != null && recyclerView.getLayoutManager() != null)
            recyclerView.getLayoutManager().requestLayout();
    }

    public ArrayList<HomeMomentViewModel> getMomentViewModelArrayList() {
        return momentViewModelArrayList;
    }

    /**
     * we will toggle view only in case of unseen moments
     */
    public void refreshLayout() {
        boolean noMoments = momentViewModelArrayList == null || momentViewModelArrayList.size() == 0;
        if (VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER) {
            if (noMomentsLayout != null)
                noMomentsLayout.setVisibility(View.GONE);
            headerTv.setVisibility(noMoments ? View.GONE : View.VISIBLE);
            return;
        }
        if (noMomentsLayout == null || recyclerView == null || headerTv == null)
            return;
        noMomentsLayout.setVisibility(noMoments ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(noMoments ? View.GONE : View.VISIBLE);
        headerTv.setVisibility(noMoments ? View.GONE : View.VISIBLE);
    }

    public void loadViewMomentFragment(String momentId, int momentStatus) {
        viewControlsCallback.onLoadViewMomentFragment(momentId, momentStatus);
    }

    public interface ViewControlsCallback {
        void onLoadViewMomentFragment(String momentId, int momentStatus);
    }
}