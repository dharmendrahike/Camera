//package com.pulseapp.android.fragments;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v7.widget.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import com.pulseapp.android.R;
//import com.pulseapp.android.adapters.CustomFriendListAdapter;
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
// * Created by user on 5/9/2016.
// */
//public class CustomFriendListFragment extends Fragment implements CustomFriendListAdapter.OnListModified,View.OnClickListener{
//
//    private static final String TAG = "CustomFriendListFragment";
//    private List<ListRoomView> roomViewList;
//    private LoadDataCallBack loadDataCallBack;
//    private LinearLayout userListLayout;
//    private FrameLayout createListLayout;
//    private ViewControlsCallback viewControlsCallback;
//    private HashMap<String,SettingsModel.MemberDetails> selectedRoomForMyMomentShare;
//    private EditText nameEditText;
//    private String customFriendListId = null;
//    private RecyclerView customFriendRecyclerView;
//    private TextView errorTextView;
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Bundle bundle = getArguments();
//        if (bundle.getString("activeListId") != null) {
//            customFriendListId = bundle.getString("activeListId");
//        }
//        selectedRoomForMyMomentShare = new HashMap<>();
//        viewControlsCallback = (ViewControlsCallback)getParentFragment();
//        roomViewList = loadDataCallBack.getLoadedFriendList();
//    }
//
//    private void updateCurrentList(SettingsModel settingsModel) {
//        selectedRoomForMyMomentShare.clear();
//        if (settingsModel != null) {
//            HashMap<String, SettingsModel.CustomFriendListDetails> customListDetails = settingsModel.customFriendList;
//            if (customListDetails != null) {
//                selectedRoomForMyMomentShare = customListDetails.get(customFriendListId).members;
//                for (int i = 0; i < roomViewList.size(); i++) {
//                    if (selectedRoomForMyMomentShare.containsKey(roomViewList.get(i).userId)) {
//                        roomViewList.get(i).setItemChecked(true);
//                        if (customFriendRecyclerView != null && customFriendRecyclerView.getAdapter() != null) {
//                            customFriendRecyclerView.getAdapter().notifyDataSetChanged();
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.custom_friend_list_fragment,container,false);
//        initializeViewObjects(rootView);
//        return rootView;
//    }
//
//    private void initializeViewObjects(View rootView) {
//        errorTextView = (TextView) rootView.findViewById(R.id.errorTextView);
//        SettingsModel settingsModel = viewControlsCallback.onCustomFragmentCreated();
//        createListLayout = (FrameLayout) rootView.findViewById(R.id.createListLayout);
//        createListLayout.setOnClickListener(this);
//        customFriendRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
//        customFriendRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        customFriendRecyclerView.setAdapter(new CustomFriendListAdapter(this,getActivity(),roomViewList));
//        userListLayout = (LinearLayout)rootView.findViewById(R.id.userListLayout);
//        nameEditText = (EditText)rootView.findViewById(R.id.listName);
//        nameEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                if (s.toString().isEmpty() && errorTextView.getVisibility() == View.GONE)
//                    errorTextView.setVisibility(View.VISIBLE);
//                else
//                    errorTextView.setVisibility(View.GONE);
//            }
//        });
//        updateCurrentList(settingsModel);
//    }
//
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        loadDataCallBack = (LoadDataCallBack)getParentFragment();
//    }
//
//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//    }
//
//    @Override
//    public void onItemClicked(ListRoomView friendItem, boolean isChecked) {
//        if (selectedRoomForMyMomentShare.containsKey(friendItem.userId)){
//            selectedRoomForMyMomentShare.remove(friendItem.userId);
//            removeUserImageFromList(friendItem.userId);
//        } else {
//            SettingsModel.MemberDetails details = new SettingsModel.MemberDetails(friendItem.roomId,friendItem.name);
//            updateUserListLayout(friendItem.imageUrl, friendItem.userId);
//            selectedRoomForMyMomentShare.put(friendItem.userId,details);
//        }
//
//        if (selectedRoomForMyMomentShare.size() > 0){
//            if (createListLayout.getVisibility() == View.GONE){
//                createListLayout.setVisibility(View.VISIBLE);
//            }
//        } else if (selectedRoomForMyMomentShare.size() == 0){
//            if (createListLayout.getVisibility() == View.VISIBLE){
//                createListLayout.setVisibility(View.GONE);
//            }
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
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()){
//            case R.id.createListLayout:
//                if (nameEditText.getText().toString().isEmpty()){
//                    errorTextView.setText(getString(R.string.empty_group_name_error));
//                    errorTextView.setVisibility(View.VISIBLE);
//                    return;
//                }
//                viewControlsCallback.onCreateCustomListClicked(selectedRoomForMyMomentShare,AppLibrary.checkStringObject(nameEditText.getText().toString()));
//                break;
//        }
//    }
//
//    public interface LoadDataCallBack{
//        List<ListRoomView> getLoadedFriendList();
//    }
//
//    public interface ViewControlsCallback{
//        void onCreateCustomListClicked(HashMap<String, SettingsModel.MemberDetails> selectedList, String name);
//        SettingsModel onCustomFragmentCreated();
//    }
//}