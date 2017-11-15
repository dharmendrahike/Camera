package com.pulseapp.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pulseapp.android.R;
import com.pulseapp.android.adapters.SearchAdapter;
import com.pulseapp.android.modelView.SearchResultModel;
import com.pulseapp.android.signals.BroadCastSignals;

import java.util.ArrayList;

/**
 * Created by deepankur on 7/11/16.
 */
public class ChildSearchFragment extends BaseFragment {

    private ArrayList<SearchResultModel> searchResultModels;
    private View rootView;
    private RecyclerView recyclerView;
    private SearchAdapter searchAdapter;

    private int searchTag;

    public void setSearchTag(int searchTag) {
        this.searchTag = searchTag;
    }
    public int getSearchTag() {
        return searchTag;
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {
    }



    public void setSearchResultModels(ArrayList<SearchResultModel> searchResultModels) {
        this.searchResultModels = searchResultModels;
        if (searchAdapter != null)
            searchAdapter.refreshList(searchResultModels);
    }

    public ArrayList<SearchResultModel> getSearchResultModels() {
        return searchResultModels;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_child_search, container, false);
        initRecyclerView(rootView);
        return rootView;
    }

    private void initRecyclerView(View rootView) {
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        searchAdapter = new SearchAdapter(searchTag, context, searchResultModels);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(searchAdapter);
    }
}
