package com.pulseapp.android.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.models.SettingsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.EditTextBackEvent;
import com.pulseapp.android.util.PrivacyPopup;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by deepankur on 7/7/16.
 */
public class FriendSelectionFragment extends BaseFragment {
    private int actionType;
    private final String TAG = getClass().getSimpleName();
    private ArrayList<ListRoomView> roomList;
    private FriendSelectionAdapter adapter;
    private RecyclerView recyclerView;
    private boolean listAltered;
    private EditTextBackEvent groupNameEt;
    private TextView incompleteNameTv;
    private boolean isSearchViewOpen;
    private TextView exceptTV;
//    private TextView headerTV;
    private TextInputLayout wrapper;
    private TextView actionBarHeader;
    private TextView createListTV;
    private LinearLayout textInputLayoutWrapper;
    private InputMethodManager imm;
    private ArrayList<String> existingListNames;
    private static final String FROM_CREATE_BTN = "createBtn";
    private static final String FROM_ACTIONBAR_BACK = "actionBarBack";
    public static final String FROM_IME_BACK = "imeBack";

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        Bundle data = getArguments();
        actionType = data.getInt("action");
    }

    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_friend_selection, container, false);
        Log.d(TAG, "onCreateView");
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        initRecyclerView(rootView);
        initSearchView(rootView);
        initActionBar(rootView.findViewById(R.id.action_bar));
        existingListNames = getAllExistingNames();
        userListLayout = (LinearLayout) rootView.findViewById(R.id.userListLayout);
        if (roomList != null)
            for (ListRoomView roomView : roomList) {
                if (roomView.isChecked) {
                    addToUserListLayout(roomView.imageUrl, roomView.userId);
                }
            }

        imm = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        createListTV = (TextView) rootView.findViewById(R.id.createListTV);
        textInputLayoutWrapper = (LinearLayout) rootView.findViewById(R.id.createListLL);
        groupNameEt = (EditTextBackEvent) rootView.findViewById(R.id.groupName);
        groupNameEt.setTypeface(fontPicker.getMontserratRegular());
        groupNameEt.setOnEditTextImeBackListener(new EditTextBackEvent.EditTextImeBackListener() {
            @Override
            public void onImeBack(EditTextBackEvent ctrl, String text) {
                groupNameEt.clearFocus();
                groupNameEt.setCursorVisible(false);
                groupNameEt.setEnableImeDone(true);
            }
        });

        groupNameEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                groupNameEt.setCursorVisible(hasFocus);
                groupNameEt.setSelection(groupNameEt.getText().length());
//                if (hasFocus)
//                    imm.showSoftInput(groupNameEt, InputMethodManager.SHOW_FORCED);
//                else
//                    imm.hideSoftInputFromWindow(groupNameEt.getWindowToken(), 0);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
            }
        });
        wrapper = (TextInputLayout) rootView.findViewById(R.id.et_wrapper);
        wrapper.setTypeface(fontPicker.getMontserratRegular());
        exceptTV = (TextView) rootView.findViewById(R.id.exceptTV);
//        headerTV = (TextView) rootView.findViewById(R.id.headerTV);
//        headerTV.setVisibility(View.VISIBLE);
//        headerTV.setText("Add Friends");
        groupNameEt.addTextChangedListener(textWatcher);
        createListTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (groupNameEt.getText().toString().isEmpty()) {

                    wrapper.setError("Please provide valid name");
                }
                if(checkIfNameAvailable(groupNameEt.getText().toString().trim()))
                    wrapper.setError("The name is already being used");
                else addNewCustomListToFireBase(FROM_CREATE_BTN);
            }
        });
        switch (this.actionType) {
            case PrivacyPopup.CREATE_NEW_LIST_ROW:
                incompleteNameTv = (TextView) rootView.findViewById(R.id.errorMessage);
                exceptTV.setVisibility(View.GONE);
//                headerTV.setAllCaps(true);
                updateHeader("Custom Friend List");
                groupNameEt.requestFocus();
                textInputLayoutWrapper.setVisibility(View.VISIBLE);
                break;
            case PrivacyPopup.FRIENDS_EXCEPT_ROW:
                createListTV.setVisibility(View.GONE);
                textInputLayoutWrapper.setVisibility(View.GONE);
                exceptTV.setVisibility(View.VISIBLE);
//                headerTV.setVisibility(View.GONE);
//                wrapper.setVisibility(View.GONE);
                break;
            case PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW:
                exceptTV.setVisibility(View.GONE);
                if (groupName != null) {
                    groupNameEt.setText(groupName);
                }
//                wrapper.setVisibility(View.GONE);
                break;

        }

        return rootView;
    }

    private ArrayList<String> getAllExistingNames() {
        ArrayList<String> al = new ArrayList<>();
        HashMap<String, SettingsModel.CustomFriendListDetails> hashmap = mFireBaseHelper.getSettingsModel().customFriendList;
        if(hashmap!=null){
            for (String s : hashmap.keySet()) {
                al.add(hashmap.get(s).name);
            }
        }
        return al;
    }

    private boolean checkIfNameAvailable(String s){
        return existingListNames.contains(s);
    }

    @Override
    public void onPause() {
        groupNameEt.clearFocus();
        super.onPause();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (wrapper != null) {
                wrapper.setError("");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            createListTV.setVisibility((s.toString().trim().length() > 0 && isUserSelected() && listAltered) ? View.VISIBLE : View.GONE);
            if(actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW){
//                createListTV.setText("Edit List");
                createListTV.setVisibility(View.GONE);
            }
        }
    };

    public void addNewCustomListToFireBase(String calledFrom) {
        if (this.actionType == PrivacyPopup.CREATE_NEW_LIST_ROW && (calledFrom.equals(FROM_ACTIONBAR_BACK) || calledFrom.equals(FROM_IME_BACK))) {
            listAltered = false;
            roomList.clear();
            if (!(!isUserSelected() && groupNameEt!=null && groupNameEt.getText().toString().trim().length() == 0))
                showShortToastMessage("List Discarded");
            getActivity().getSupportFragmentManager().popBackStack();
        } else {
            // code moved to onDestroy();
            if (groupNameEt.getText().toString().trim().isEmpty() && actionType != PrivacyPopup.FRIENDS_EXCEPT_ROW && listAltered) {
                showShortToastMessage("Group Name cannot be blank");
            } else {
                if (groupName != null && !groupName.equals(groupNameEt.getText().toString().trim()) && groupNameEt.getText().toString().trim().length() > 0) {
                    groupName = groupNameEt.getText().toString().trim();
                    listAltered = true;
                }
            }
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        ((ImageView) actionBar.findViewById(R.id.action_bar_IV_4)).setImageResource(R.drawable.camera_svg);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);

        ImageView backIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        backIv.setImageResource(R.drawable.back_svg);
        backIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewCustomListToFireBase(FROM_ACTIONBAR_BACK);
            }
        });
        actionBarHeader = (TextView) actionBar.findViewById(R.id.titleTV);
        actionBarHeader.setText("SHARE");
        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();
    }

    private void updateHeader(String s) {
        if (s != null && actionBarHeader != null) {
            actionBarHeader.setText(s);
        }
    }

    private void initRecyclerView(final View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new FriendSelectionAdapter(actionType, context, roomList, new RecyclerViewClickInterface() {
            @Override
            public void onItemClick(int extras, Object data) {
                listAltered = true;
                ListRoomView roomView = (ListRoomView) data;
                if (roomView.isChecked)
                    addToUserListLayout(roomView.imageUrl, roomView.userId);
                else removeUserImageFromList(roomView.userId);

                if (actionType == PrivacyPopup.CREATE_NEW_LIST_ROW || actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW) {
                    boolean checked = false;
                    for (ListRoomView listRoomView : roomList) {
                        if (listRoomView.isChecked) {
                            checked = true;
                            break;
                        }
                    }
                    createListTV.setVisibility((checked && groupNameEt.getText().toString().trim().length() > 0) ? View.VISIBLE : View.GONE);
                    if(actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW){
//                        createListTV.setText("Edit List");
                        createListTV.setVisibility(View.GONE);
                    }
                }

            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.getItemAnimator().setChangeDuration(0);
//        recyclerView.setNestedScrollingEnabled(true);

    }

    SearchView searchView;

    void initSearchView(final View rootView) {
        searchView = (SearchView) rootView.findViewById(R.id.searchView);
        EditText searchEditText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchEditText.setTypeface(fontPicker.getMontserratRegular());
        searchEditText.setAlpha(0.87f);
        searchEditText.setTextColor(Color.BLACK);
        searchEditText.setBackgroundColor(Color.TRANSPARENT);
        searchEditText.setHintTextColor(Color.GRAY);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, " onQueryTextSubmit " + query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, " onQueryTextChange " + newText);
//                searchView.clearFocus();
                filterList(newText);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, " search bar Opened ");
                userListLayout.setVisibility(View.GONE);
                isSearchViewOpen = true;
                if(groupNameEt.hasFocus())
                    groupNameEt.clearFocus();
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, " onClose ");
                searchView.onActionViewCollapsed();
                userListLayout.setVisibility(View.VISIBLE);
                isSearchViewOpen = false;
                if (adapter != null)
                    adapter.refreshFriendList(roomList);
                return true;
            }
        });
    }

    public boolean closeSearchView() {
        if (isSearchViewOpen) {
            searchView.onActionViewCollapsed();
            userListLayout.setVisibility(View.VISIBLE);
            isSearchViewOpen = false;
            if (adapter != null)
                adapter.refreshFriendList(roomList);
            return true;
        }
        return false;
    }

    private void filterList(String query) {
        query = query.toLowerCase();
        ArrayList<ListRoomView> filteredList = new ArrayList<>();
        for (ListRoomView listRoomView : roomList) {
            if (listRoomView.name.toLowerCase().contains(query))
                filteredList.add(listRoomView);
            if (adapter != null)
                adapter.refreshFriendList(filteredList);
        }
    }

    public void setCreateNewFriendList(Context context, FireBaseHelper fireBaseHelper) {
        ArrayList<ListRoomView> allRooms ;
        if (fireBaseHelper.getListRoomView() != null)
            allRooms = new ArrayList<>(fireBaseHelper.getListRoomView());
        else
            allRooms = new ArrayList<>();
        roomList = new ArrayList<>();
        this.context = context;
        for (ListRoomView roomView : allRooms)
            if (roomView != null && roomView.type == FRIEND_ROOM)
                roomList.add(roomView);
    }


    public void setIgnoredFriendList(Context context, FireBaseHelper fireBaseHelper, SettingsModel settingsModel) {
        if (fireBaseHelper.getListRoomView() == null)
            return;
        ArrayList<ListRoomView> allRooms = new ArrayList<>(fireBaseHelper.getListRoomView());
        roomList = new ArrayList<>();
        this.context = context;

        boolean searchForPreviousIgnoredFriend = false;
        if (settingsModel != null && settingsModel.ignoredList != null)
            searchForPreviousIgnoredFriend = true;
        for (ListRoomView roomView : allRooms) {
            if (roomView == null || roomView.type == GROUP_ROOM)
                continue;

            if (searchForPreviousIgnoredFriend) {
                final SettingsModel.MemberDetails memberDetails = settingsModel.ignoredList.get(roomView.userId);
                if (memberDetails != null) {
                    roomView.isChecked = true;
                }
            }

            roomList.add(roomView);
            Collections.sort(roomList, new Comparator<ListRoomView>() {
                @Override
                public int compare(ListRoomView lhs, ListRoomView rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
        }
    }

    String groupName;
    String customListId;
    Context context;

    public void editCustomList(Context context, FireBaseHelper fireBaseHelper, SettingsModel settingsModel, String listId) {
        customListId = listId;
        this.context = context;
        ArrayList<ListRoomView> allRooms = new ArrayList<>(fireBaseHelper.getListRoomView());
        roomList = new ArrayList<>();
        groupName = settingsModel.customFriendList.get(listId).name;
        for (ListRoomView roomView : allRooms) {
            if (roomView == null || roomView.type == GROUP_ROOM)
                continue;


            final HashMap<String, SettingsModel.MemberDetails> memberDetails = settingsModel.customFriendList.get(customListId).members;
            if (memberDetails != null) {

                for (String keys : memberDetails.keySet()) {
                    if (keys.equals(roomView.userId)) {
                        roomView.isChecked = true;
//                        addToUserListLayout(roomView.imageUrl, roomView.userId);
                    }
                }
            }


            roomList.add(roomView);
            Collections.sort(roomList, new Comparator<ListRoomView>() {
                @Override
                public int compare(ListRoomView lhs, ListRoomView rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
        }
    }

    LinearLayout userListLayout;

    private void addToUserListLayout(String imageUrl, String id) {
        int imageDimension = AppLibrary.convertDpToPixels(context, 32);
        int imageMarginRight = AppLibrary.convertDpToPixels(context, 4);
        int imageMarginBottom = AppLibrary.convertDpToPixels(context, 12);
        int imageMarginTop = AppLibrary.convertDpToPixels(context, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, imageMarginTop, imageMarginRight, imageMarginBottom);
        ImageView imageView = new ImageView(context);
        imageView.setTag(id);
        imageView.setLayoutParams(params);
        Picasso.with(context).load(imageUrl)
                .resize(imageDimension, imageDimension).centerCrop().transform(new RoundedTransformation()).into(imageView);
        userListLayout.addView(imageView);
    }

    private boolean isUserSelected() {
        if (userListLayout.getChildCount() > 0)
            return true;
        return false;
    }

    private void removeUserImageFromList(String tag) {
        for (int i = 0; i < userListLayout.getChildCount(); i++) {
            if (userListLayout.getChildAt(i).getTag().equals(tag)) {
                userListLayout.removeViewAt(i);
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        ArrayList<ListRoomView> tempList = new ArrayList<>(roomList);
        if (groupName != null && groupNameEt != null &&
                !groupName.equals(groupNameEt.getText().toString().trim()) &&
                groupNameEt.getText().toString().trim().length() > 0) {
            groupName = groupNameEt.getText().toString().trim();
            listAltered = true;
        }
        if (listAltered) {
            if (this.actionType == PrivacyPopup.FRIENDS_EXCEPT_ROW) {
//                mFireBaseHelper.getNewFireBase(ANCHOR_SETTINGS, new String[]{myUserId, IGNORED_LIST}).setValue(null);
                HashMap<String, SettingsModel.MemberDetails> ignoredHashMap = new HashMap<>();
                if(mFireBaseHelper.getSettingsModel().lastUsedPrivacy.equals(PrivacyPolicy.PRIVACY_FRIENDS_EXCEPT))
                    mFireBaseHelper.sendLastUsedPrivacyOptionToFireBase(PrivacyPolicy.PRIVACY_ALL_FRIENDS_AND_FOLLOWERS);
                for (ListRoomView roomView : tempList) {
                    if (!roomView.isChecked) continue;
                    SettingsModel.MemberDetails memberDetails = new SettingsModel.MemberDetails(roomView.roomId, roomView.name);
                    ignoredHashMap.put(roomView.userId, memberDetails);
                }
                mFireBaseHelper.getNewFireBase(ANCHOR_SETTINGS, new String[]{myUserId, IGNORED_LIST}).setValue(ignoredHashMap);
            } else if (this.actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW || this.actionType == PrivacyPopup.CREATE_NEW_LIST_ROW) {
                HashMap<String, SettingsModel.MemberDetails> memberDetailsHashMap = new HashMap<>();
                for (ListRoomView roomView : tempList) {
                    if (!roomView.isChecked) continue;
                    SettingsModel.MemberDetails memberDetails = new SettingsModel.MemberDetails(roomView.roomId, roomView.name);
                    memberDetailsHashMap.put(roomView.userId, memberDetails);
                }
                SettingsModel.CustomFriendListDetails details = new SettingsModel.CustomFriendListDetails(groupName, memberDetailsHashMap);
                if (memberDetailsHashMap.size() > 0) {
                    if (this.actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW) {
//                        mFireBaseHelper.getNewFireBase(ANCHOR_SETTINGS, new String[]{myUserId, CUSTOM_FRIEND_LIST, customListId}).setValue(null);
                        mFireBaseHelper.getNewFireBase(ANCHOR_SETTINGS, new String[]{myUserId, CUSTOM_FRIEND_LIST, customListId}).setValue(details);
                        showShortToastMessage("List Updated");
                    } else {
                        if (groupNameEt != null && groupNameEt.getText() != null) {
                            if (!groupNameEt.getText().toString().isEmpty()) {
                                details.name = groupNameEt.getText().toString();
                                mFireBaseHelper.getNewFireBase(ANCHOR_SETTINGS, new String[]{myUserId, CUSTOM_FRIEND_LIST}).push().setValue(details);
                                showShortToastMessage("List Created");
                            }
                        }
                    }
                } else showShortToastMessage("List cannot have 0 friends");
            }
        } else{
            if (this.actionType == PrivacyPopup.FRIENDS_EXCEPT_ROW && mFireBaseHelper.getSettingsModel().ignoredList == null)
                friendSelectionCallback.onExit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    FriendSelectionCallback friendSelectionCallback;

    public void setFriendSelectionCallback(FriendSelectionCallback friendSelectionCallback) {
        this.friendSelectionCallback = friendSelectionCallback;
    }

    public interface FriendSelectionCallback {
        void onExit();
    }

    class FriendSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Context context;
        private String TAG = this.getClass().getSimpleName();
        private FireBaseHelper fireBaseHelper;
        private RecyclerViewClickInterface clickInterface;
        private final int VIEW_TYPE_HEADER = 11, VIEW_TYPE_ITEM = 222;
        private ArrayList<ListRoomView> roomViewArrayList;
        private int checkedResourceId, unCheckedResourceId;

        public FriendSelectionAdapter(int listType, Context context, ArrayList<ListRoomView> roomViewArrayList, RecyclerViewClickInterface clickInterface) {
            this.context = context;
            this.roomViewArrayList = roomViewArrayList;
            this.fireBaseHelper = FireBaseHelper.getInstance(context);
            this.clickInterface = clickInterface;
            if (actionType == PrivacyPopup.CREATE_NEW_LIST_ROW || actionType == PrivacyPopup.EXISTING_CUSTOM_LIST_SELECTED_ROW) {
                this.checkedResourceId = R.drawable.check_on_violet_svg;
                this.unCheckedResourceId = R.drawable.check_off_box_svg;
            } else if (actionType == PrivacyPopup.FRIENDS_EXCEPT_ROW) {
                this.checkedResourceId = R.drawable.check_on_red_svg;
                this.unCheckedResourceId = R.drawable.check_off_round_svg;
            }
        }

        public void refreshFriendList(ArrayList<ListRoomView> roomViewArrayList) {
            this.roomViewArrayList = roomViewArrayList;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_friend_selection, parent, false);
                return new ItemVH(v);
            } else {
                MontserratRegularTextView textView = new MontserratRegularTextView(context);
                return new ItemVH(textView);
            }
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).alphabetTv.setText(String.valueOf(Character.toUpperCase(roomViewArrayList.get(position + 1).name.charAt(0))));
            }
            if (holder instanceof ItemVH) {
                final ListRoomView roomView = roomViewArrayList.get(position);
                ((ItemVH) holder).checkIv.setImageResource(roomView.isChecked ? checkedResourceId : unCheckedResourceId);
                ((ItemVH) holder).rootView.setTag(roomView);
                ((ItemVH) holder).cardNameTv.setText(roomView.name);
                Picasso.with(context).load(roomView.imageUrl).transform(new RoundedTransformation()).into(((ItemVH) holder).profileIv);
            }
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "getItemCount");
            return roomViewArrayList == null ? 0 : roomViewArrayList.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (roomViewArrayList == null)
                return VIEW_TYPE_HEADER;
            else
                return VIEW_TYPE_ITEM;
        }

        class ItemVH extends RecyclerView.ViewHolder {
            View rootView;
            ImageView profileIv, checkIv;
            TextView cardNameTv;

            public ItemVH(View itemView) {
                super(itemView);
                rootView = itemView;
                profileIv = (ImageView) itemView.findViewById(R.id.cardIV);
                checkIv = (ImageView) itemView.findViewById(R.id.checkedIV);
                cardNameTv = (TextView) itemView.findViewById(R.id.cardTV);
                rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(groupNameEt.hasFocus())
                            groupNameEt.clearFocus();
                        ListRoomView roomView = (ListRoomView) rootView.getTag();
                        int index = roomViewArrayList.indexOf(roomView);
                        roomView.isChecked = !roomView.isChecked;
                        notifyItemChanged(index);

                        if (clickInterface != null)
                            clickInterface.onItemClick(0, rootView.getTag());
                    }
                });
            }
        }

        class HeaderVH extends RecyclerView.ViewHolder {
            TextView alphabetTv;

            public HeaderVH(View itemView) {
                super(itemView);
                alphabetTv = (TextView) itemView;
            }
        }
    }
}
