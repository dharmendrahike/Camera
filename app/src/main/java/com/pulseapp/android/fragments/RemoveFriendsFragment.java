//package com.pulseapp.android.fragments;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//import com.pulseapp.android.R;
//import com.pulseapp.android.adapters.RemoveFriendListAdapter;
//import com.pulseapp.android.modelView.ListRoomView;
//import com.pulseapp.android.models.SettingsModel;
//import com.pulseapp.android.util.AppLibrary;
//import com.pulseapp.android.util.RoundedTransformation;
//import com.squareup.picasso.Picasso;
//
//import java.util.HashMap;
//import java.util.List;
//
///**
// * Created by user on 5/5/2016.
// */
//public class RemoveFriendsFragment extends Fragment implements RemoveFriendListAdapter.OnListModified,View.OnClickListener{
//
//    private static final String TAG = "RemoveFriendsFragment";
//
//    private List<ListRoomView> roomViewList;
//    private LoadDataCallBack loadDataCallBack;
//    private ViewControlsCallback viewControlsCallback;
//    private LinearLayout userListLayout;
//    private FrameLayout createListLayout;
//    private HashMap<String,SettingsModel.MemberDetails> selectedRoomForMyMomentShare;
//    private RecyclerView removeFriendsRecyclerView;
//
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        loadDataCallBack = (LoadDataCallBack)getParentFragment();
//    }
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        selectedRoomForMyMomentShare = new HashMap<>();
//        viewControlsCallback = (ViewControlsCallback)getParentFragment();
//        roomViewList = loadDataCallBack.getLoadedFriendList();
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.remove_friend_list_fragment,container,false);
//        initializeViewObject(rootView);
//        return rootView;
//    }
//
//    private void initializeViewObject(View rootView) {
//        createListLayout = (FrameLayout) rootView.findViewById(R.id.createListLayout);
//        createListLayout.setOnClickListener(this);
//        userListLayout = (LinearLayout)rootView.findViewById(R.id.selectedListContainer);
//        SettingsModel settingsModel = viewControlsCallback.onRemoveFriendsFragmentCreated();
//        removeFriendsRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
//        removeFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        removeFriendsRecyclerView.setAdapter(new RemoveFriendListAdapter(this,getActivity(),roomViewList));
//        updateExceptFriendList(settingsModel);
//    }
//
//    private void updateExceptFriendList(SettingsModel settingsModel) {
//        HashMap<String,SettingsModel.MemberDetails> ignoredList = null;
//        if (settingsModel != null)
//            ignoredList = settingsModel.ignoredList;
//        if (ignoredList != null) {
//            for (int i = 0; i < roomViewList.size(); i++) {
//                if (ignoredList.containsKey(roomViewList.get(i).userId)) {
//                    roomViewList.get(i).setItemChecked(true);
//                }
//            }
//            if (removeFriendsRecyclerView != null && removeFriendsRecyclerView.getAdapter() != null) {
//                removeFriendsRecyclerView.getAdapter().notifyDataSetChanged();
//            }
//        }
//    }
//
//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//    }
//
//    @Override
//    public void onItemClicked(ListRoomView friendItem, boolean isChecked) {
//        if (!selectedRoomForMyMomentShare.containsKey(friendItem.userId)) {
//            SettingsModel.MemberDetails details = new SettingsModel.MemberDetails(friendItem.roomId,friendItem.name);
//            selectedRoomForMyMomentShare.put(friendItem.userId, details);
//            updateUserListLayout(friendItem.imageUrl, friendItem.userId);
//        } else {
//            selectedRoomForMyMomentShare.remove(friendItem.userId);
//            removeUserImageFromList(friendItem.userId);
//        }
//        if (selectedRoomForMyMomentShare.size() > 0){
//            if (createListLayout.getVisibility() == View.GONE){
//                createListLayout.setVisibility(View.VISIBLE);
//            }
//        } else if (selectedRoomForMyMomentShare.size() == 0){
//            if (createListLayout.getVisibility() == View.VISIBLE)
//                createListLayout.setVisibility(View.GONE);
//        }
//    }
//
//    private void removeUserImageFromList(String tag){
//        for (int i = 0;i < userListLayout.getChildCount() ; i++){
//            if (userListLayout.getChildAt(i).getTag().equals(tag)){
//                userListLayout.removeViewAt(i);
//            }
//        }
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()){
//            case R.id.createListLayout:
//                viewControlsCallback.onCreateShareListClicked(selectedRoomForMyMomentShare);
//                break;
//        }
//    }
//
//    public interface LoadDataCallBack{
//        List<ListRoomView> getLoadedFriendList();
//    }
//
//    public interface ViewControlsCallback{
//        void onCreateShareListClicked(HashMap<String,SettingsModel.MemberDetails> selectedList);
//        SettingsModel onRemoveFriendsFragmentCreated();
//    }
//
//    private void updateUserListLayout(String imageUrl,String id){
//        int imageDimension = AppLibrary.convertDpToPixels(getActivity(),50);
//        int imageMargin = AppLibrary.convertDpToPixels(getActivity(),3);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
//        params.setMargins(0,0,imageMargin,0);
//        ImageView imageView = new ImageView(getActivity());
//        imageView.setTag(id);
//        imageView.setLayoutParams(params);
//        Picasso.with(getActivity()).load(imageUrl)
//                .resize(imageDimension,imageDimension).centerCrop().transform(new RoundedTransformation()).into(imageView);
//        userListLayout.addView(imageView);
//    }
//}