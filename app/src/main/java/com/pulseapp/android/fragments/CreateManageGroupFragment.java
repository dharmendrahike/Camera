package com.pulseapp.android.fragments;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.GroupListAdapter;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.ListRoomView;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by abc on 4/15/2016.
 */
public class CreateManageGroupFragment extends BaseFragment implements View.OnClickListener,
        GroupListAdapter.ViewControlsCallback, FireBaseHelper.RoomDataLoadedCallbacks {

    private List<ListRoomView> friendsList;
    private ViewControlsCallback viewControlsCallback;
    private EditText groupNameEditText;
    private int groupAction;
    private HashMap<String, RoomsModel.Members> addedMemberMap;
    private HashMap<String, RoomsModel.Members> existingMemberMap;
    private HashMap<String, RoomsModel.Members> removeExistingMemberMap;
    private View rootView;
    private LinearLayout userListLayout;
    private String roomId;
    private RecyclerView existingMemberRecyclerView;
    private TextView errorTextView;
    private String TAG = getClass().getSimpleName();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) getParentFragment();
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ArrayList<ListRoomView> listRoomView = mFireBaseHelper.getListRoomView();
        friendsList = new ArrayList<>();
        for (ListRoomView room : listRoomView) {
            if (room.type == FRIEND_ROOM)
                friendsList.add(room);
        }
        Collections.sort(friendsList, new Comparator<ListRoomView>() {
            @Override
            public int compare(ListRoomView lhs, ListRoomView rhs) {
                return lhs.name.compareToIgnoreCase(rhs.name);
            }
        });
        groupAction = getArguments().getInt(AppLibrary.GROUP_ACTION);
        if (groupAction == AppLibrary.GROUP_ACTION_MANAGE) {
            // fetch existing members of the group
            mFireBaseHelper.setOnGroupMembersLoadedListener(this);
            mFireBaseHelper.loadExistingGroups(roomId);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.new_group_fragment, container, false);
        initActionBar(rootView.findViewById(R.id.action_bar));
        initSearchView(rootView);
        initializeViewObjects(rootView);
        return rootView;
    }

    private SearchView searchView;
    private boolean isSearchViewOpen;

    void initSearchView(View rootView) {
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
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, " onClose ");
                searchView.onActionViewCollapsed();
                userListLayout.setVisibility(View.VISIBLE);
                isSearchViewOpen = false;
                if (createGroupAdapter != null)
                    createGroupAdapter.refreshFriendList(friendsList);
                return true;
            }
        });
    }

    public boolean closeSearchView() {
        if (isSearchViewOpen) {
            searchView.onActionViewCollapsed();
            userListLayout.setVisibility(View.VISIBLE);
            isSearchViewOpen = false;
            if (createGroupAdapter != null)
                createGroupAdapter.refreshFriendList(friendsList);
            return true;
        }
        return false;
    }

    private void filterList(String query) {
        query = query.toLowerCase();
        List<ListRoomView> filteredList = new ArrayList<>();
        for (ListRoomView listRoomView : friendsList) {
            if (listRoomView.name.toLowerCase().contains(query))
                filteredList.add(listRoomView);
            if (createGroupAdapter != null)
                createGroupAdapter.refreshFriendList(filteredList);

        }
    }

    GroupListAdapter createGroupAdapter;

    private void initializeViewObjects(View rootView) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            int padding = AppLibrary.convertDpToPixels(getActivity(), 8);
//            ((CardView) rootView.findViewById(R.id.cardView)).setContentPadding(-padding, -padding, -padding, -padding);
//        }
        errorTextView = (TextView) rootView.findViewById(R.id.errorMessage);
        userListLayout = (LinearLayout) rootView.findViewById(R.id.userListLayout);
        existingMemberRecyclerView = (RecyclerView) rootView.findViewById(R.id.existingListRecyclerView);
        existingMemberRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        RecyclerView friendsRecyclerView = (RecyclerView) rootView.findViewById(R.id.friendListRecyclerView);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        addedMemberMap = new HashMap<>();
        if (groupAction == AppLibrary.GROUP_ACTION_CREATE) {
            rootView.findViewById(R.id.existingDescLayout).setVisibility(View.GONE);
            header.setText(getResources().getString(R.string.create_group_text));
            ((TextView) rootView.findViewById(R.id.createGroupTV)).setText(getResources().getString(R.string.create_group_text));
            if (friendsList != null) {
                createGroupAdapter = new GroupListAdapter(this, friendsList);
                friendsRecyclerView.setAdapter(createGroupAdapter);
            }
            existingMemberRecyclerView.setVisibility(View.GONE);
        } else if (groupAction == AppLibrary.GROUP_ACTION_MANAGE) {
            removeExistingMemberMap = new HashMap<String, RoomsModel.Members>();
            header.setText(getResources().getString(R.string.manage_group_text));
            header.setText(getResources().getString(R.string.save_text));
            existingMemberRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            if (friendsList != null)
                friendsRecyclerView.setAdapter(new GroupListAdapter(this, friendsList));
        }
        groupNameEditText = (EditText) rootView.findViewById(R.id.groupName);
        groupNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty())
                    errorTextView.setVisibility(View.GONE);
                else {
                    errorTextView.setText(getString(R.string.empty_group_name_error));
                    errorTextView.setVisibility(View.VISIBLE);
                }

            }
        });
        rootView.findViewById(R.id.createGroupTV).setOnClickListener(this);
    }

    TextView header;

    void initActionBar(View actionBar) {
        actionBar.findViewById(R.id.action_bar_IV_2).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_4).setVisibility(View.GONE);
        actionBar.findViewById(R.id.action_bar_IV_3).setVisibility(View.GONE);

        ImageView backBtnIv = (ImageView) actionBar.findViewById(R.id.action_bar_IV_1);
        (backBtnIv).setImageResource(R.drawable.back_svg);
        backBtnIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //navigate back from here
                getActivity().onBackPressed();
            }
        });
        header = ((TextView) actionBar.findViewById(R.id.titleTV));
        View topView = actionBar.findViewById(R.id.status_bar_background);
        topView.getLayoutParams().height = AppLibrary.getStatusBarHeight(getActivity());
        topView.requestLayout();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.createGroupTV:
                if (groupNameEditText.getText().toString().isEmpty()) {
                    errorTextView.setText(getString(R.string.empty_group_name_error));
                    errorTextView.setVisibility(View.VISIBLE);
                    return;
                }
                updateRoomsForGroupToFireBase();
                v.setVisibility(View.GONE);
                break;
        }
    }

    public void updateRoomsForGroupToFireBase() {
        if (groupAction == AppLibrary.GROUP_ACTION_CREATE) {
            String roomId = mFireBaseHelper.updateOnGroupCreated(removeExistingMemberMap, addedMemberMap, AppLibrary.checkStringObject(groupNameEditText.getText().toString()), null);
            ListRoomView listRoomView = new ListRoomView();
            listRoomView.roomId = roomId;
            listRoomView.type = 2;
            listRoomView.name = groupNameEditText.getText().toString();
            if (viewControlsCallback != null)
                viewControlsCallback.onCreateGroupClicked(listRoomView);
            else {
                if (getActivity().getSupportFragmentManager().findFragmentByTag(CreateManageGroupFragment.class.getSimpleName()) != null) {
                    showShortToastMessage("group will be created soon");
                    boolean popped = getActivity().getSupportFragmentManager().popBackStackImmediate();
                    Log.d(TAG, "onCloseViewChatMediaFragment: popped " + popped);
                }
            }
        }
    }

    @Override
    public void onMemberClicked(ListRoomView friendObject) {
        if (groupAction == AppLibrary.GROUP_ACTION_CREATE) {
            rootView.findViewById(R.id.createGroupTV).setVisibility(View.VISIBLE);
            if (!addedMemberMap.containsKey(friendObject.userId)) {
                RoomsModel.Members members = new RoomsModel.Members(friendObject.userId, friendObject.name, friendObject.imageUrl, GROUP_CREATED);
                addedMemberMap.put(friendObject.userId, members);
                updateUserListLayout(friendObject.imageUrl, friendObject.userId);
            } else {
                removeUserImageFromList(friendObject.userId);
                addedMemberMap.remove(friendObject.userId);
            }
        }
        if (addedMemberMap.isEmpty() && rootView.findViewById(R.id.createGroupTV).getVisibility() == View.VISIBLE)
            rootView.findViewById(R.id.createGroupTV).setVisibility(View.GONE);
    }

    @Override
    public void onExistingGroupMemberClicked(RoomsModel.Members memberObject) {
        if (removeExistingMemberMap.containsKey(memberObject.memberId)) {
            removeExistingMemberMap.remove(memberObject.memberId);
            updateUserListLayout(memberObject.imageUrl, memberObject.memberId);
        } else {
            removeUserImageFromList(memberObject.memberId);
            removeExistingMemberMap.put(memberObject.memberId, memberObject);
        }
        if (removeExistingMemberMap.isEmpty() && rootView.findViewById(R.id.createGroupTV).getVisibility() == View.VISIBLE)
            rootView.findViewById(R.id.createGroupTV).setVisibility(View.GONE);
    }

    private void removeUserImageFromList(String tag) {
        for (int i = 0; i < userListLayout.getChildCount(); i++) {
            if (userListLayout.getChildAt(i).getTag().equals(tag)) {
                userListLayout.removeViewAt(i);
            }
        }
    }

    private void updateUserListLayout(String imageUrl, String id) {
        int imageDimension = AppLibrary.convertDpToPixels(getActivity(), 50);
        int imageMargin = AppLibrary.convertDpToPixels(getActivity(), 3);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, imageMargin, 0);
        ImageView imageView = new ImageView(getActivity());
        imageView.setTag(id);
        imageView.setLayoutParams(params);
        Picasso.with(getActivity()).load(imageUrl)
                .resize(imageDimension, imageDimension).centerCrop().transform(new RoundedTransformation()).into(imageView);
        userListLayout.addView(imageView);
    }

    @Override
    public void onGroupMemberLoaded(HashMap<String, RoomsModel.Members> memberMap) {
        existingMemberMap = memberMap;
        if (existingMemberMap != null) {
            RoomsModel.Members[] memberArray = (RoomsModel.Members[]) memberMap.entrySet().toArray();
            Iterator it = memberMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                updateUserListLayout(((RoomsModel.Members) pair.getValue()).imageUrl, ((RoomsModel.Members) pair.getValue()).memberId);
                it.remove();
            }
            existingMemberRecyclerView.setAdapter(new GroupListAdapter(this, memberArray));
            rootView.findViewById(R.id.existingDesc).setVisibility(View.GONE);
        }
    }

    public interface ViewControlsCallback {
        void onCreateGroupClicked(ListRoomView listRoomView);
    }
}
