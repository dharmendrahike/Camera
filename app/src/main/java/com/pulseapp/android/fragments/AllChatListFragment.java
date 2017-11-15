package com.pulseapp.android.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.AllChatListAdapter;
import com.pulseapp.android.adapters.RecyclerViewClickInterface;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by deepankur on 30/4/16.
 */
public class AllChatListFragment extends BaseFragment {
    private RecyclerView recyclerView;
    private AllChatListAdapter allChatListAdapter;
    private String TAG = this.getClass().getSimpleName();
    private SearchView searchView;

    private ArrayList<SliderMessageModel> superSetChatModels;
    private TextView headerTv;
    private boolean isSearchViewOpen;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        superSetChatModels = DummyDataGenerator.getInstance().getAllChats(20);

        mFireBaseHelper.setAllChatListRoomCountChangedListener(new FireBaseHelper.AllChatListRoomCountChangedListener() {
            @Override
            public void onChatRoomsChanged() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshRooms();
                        if (isSearchViewOpen)
                            onBackPressed();
                    }
                });
            }
        });

        refreshRooms();
    }

    private void refreshRooms() {
        superSetChatModels = mFireBaseHelper.getAllMessageRoom();
        if (superSetChatModels != null)
            doSorting((ArrayList<SliderMessageModel>) superSetChatModels.clone());

        if (allChatListAdapter != null) {
            allChatListAdapter.setAllChatsModels(superSetChatModels);
            allChatListAdapter.notifyDataSetChanged();
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_all_chat_list, container, false);
        int statusBarHeight = AppLibrary.getStatusBarHeight(context);
        rootView.findViewById(R.id.status_bar).getLayoutParams().height = statusBarHeight;
        rootView.findViewById(R.id.cha_top_bar_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, " action " + getActionName(event.getAction()) + " touch X " + event.getRawX() + " y " + event.getRawY());
                return true;
            }
        });
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        rootView.findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        if (superSetChatModels == null || superSetChatModels.size() == 0) {
//            rootView.findViewById(R.id.no_moments_layout).setVisibility(View.VISIBLE);//todo empty state
//            View includedView = rootView.findViewById(R.id.include_no_moment_layout);
//            includedView.findViewById(R.id.add_friendBTN).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ((CameraActivity) getActivity()).requestAddFriendFragmentOpen();
//                }
//            });
        }
        searchView = (SearchView) rootView.findViewById(R.id.searchView);
        EditText searchEditText = (EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(Color.WHITE);
        searchEditText.setBackgroundColor(Color.TRANSPARENT);
        searchEditText.setHintTextColor(Color.parseColor("#80FFFFFF"));
        searchEditText.setHint("Search friends & groups");
        searchEditText.setTypeface(fontPicker.getMontserratRegular());
        headerTv = (TextView) rootView.findViewById(R.id.chat_nameTV);
        headerTv.setTextColor(Color.WHITE);

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(searchEditText, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(Color.TRANSPARENT);

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
                headerTv.setVisibility(View.GONE);
                isSearchViewOpen = true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, " onClose ");
                searchView.onActionViewCollapsed();
                headerTv.setVisibility(View.VISIBLE);
                isSearchViewOpen = false;
                return true;
            }
        });
        initRecyclerView(rootView);
        return rootView;
    }


    private void initRecyclerView(View rootView) {
        allChatListAdapter = new AllChatListAdapter(getActivity(), superSetChatModels, new RecyclerViewClickInterface() {
            @Override
            public void onItemClick(int position, Object data) {
                if (data != null)
                    ((CameraActivity) getActivity()).loadChatFragment((SliderMessageModel) data, true);
                else //null in second parameter indicates intention to open new Group fragment
                    ((CameraActivity) getActivity()).loadCreateGroupFragment();
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(allChatListAdapter);
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                int firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();

                Log.d(TAG, " onScroll " + firstVisibleItemPosition + " " + firstCompletelyVisibleItemPosition + " "
                        + lastVisibleItemPosition + " " + lastCompletelyVisibleItemPosition);
            }
        });
    }


    private void doSorting(ArrayList<SliderMessageModel> unsortedList) {
        if (unsortedList == null || unsortedList.size() == 0) return;

        Log.d(TAG, " going to sort @ " + System.currentTimeMillis());
        Collections.sort(unsortedList, new Comparator<SliderMessageModel>() {
            @Override
            public int compare(SliderMessageModel lhs, SliderMessageModel rhs) {
                return lhs.displayName.compareToIgnoreCase(rhs.displayName);
            }
        });

        Log.d(TAG, " sorted List @ " + System.currentTimeMillis());

        superSetChatModels.clear();
        int currentChar = unsortedList.get(0).getNormalizedFirstChar();
        superSetChatModels.add(null);
        superSetChatModels.add(null);//first item 0 indicating the first character of the first element's name in our arrayList,
        for (int i = 0; i < unsortedList.size(); i++) {
            SliderMessageModel chatsModel = unsortedList.get(i);
            if (currentChar != chatsModel.getNormalizedFirstChar() && i < unsortedList.size()) {
                superSetChatModels.add(null);
                currentChar = chatsModel.getNormalizedFirstChar();
            }
            superSetChatModels.add(chatsModel);
        }

        Log.d(TAG, " added Header Data Structure to the list @ " + System.currentTimeMillis());
    }

    private void filterList(String query) {
        if (query.trim().length() == 0 && allChatListAdapter.getAllChatsModels() != null
                && allChatListAdapter.getAllChatsModels() != superSetChatModels) {
            allChatListAdapter.setAllChatsModels(superSetChatModels);
            allChatListAdapter.notifyDataSetChanged();
            return;
        }
        query = query.toLowerCase();
        if (superSetChatModels == null) return;
        ArrayList<SliderMessageModel> filteredList = new ArrayList<>();

        filteredList.clear();
        filteredList.add(null);//for the create group header thing
        int currentChar = -1;
        for (SliderMessageModel model : superSetChatModels) {
            if (model == null) continue;
            if (model.displayName.toLowerCase().contains(query)) {
                if (filteredList.size() == 1) {
                    filteredList.add(null);//we have > 0 people to show and hence can safely add first char
                    currentChar = model.getNormalizedFirstChar();
                }
                if (currentChar != model.getNormalizedFirstChar()) {
                    filteredList.add(null);
                }
                filteredList.add(model);
                currentChar = model.getNormalizedFirstChar();
            }
        }
        allChatListAdapter.setAllChatsModels(filteredList);
        allChatListAdapter.notifyDataSetChanged();
    }

    /**
     * Notify Activity's BackPress here.
     *
     * @return true if it can consume the back event false otherwise
     */
    public boolean onBackPressed() {
        if (isSearchViewOpen) {
            searchView.onActionViewCollapsed();
            headerTv.setVisibility(View.VISIBLE);
            isSearchViewOpen = false;
            return true;
        }
        return false;
    }
}
