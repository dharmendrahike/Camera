package com.pulseapp.android.data;

import android.os.Handler;
import android.os.HandlerThread;

import com.pulseapp.android.models.StickerCategoryModel;
import com.pulseapp.android.models.StickerModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 9/27/16.
 */
public class StickerSearcher {

    private HandlerThread searchThread;
    private Handler searchHandler;
    private String TAG = getClass().getSimpleName();
    private LinkedHashMap<String, LinkedHashMap<String, StickerModel>> allSticker;
    private LinkedHashMap<String, StickerCategoryModel> categoryModelMap;

    public StickerSearcher() {
        init();
    }

    void init() {
        if (this.searchThread == null) {
            this.searchThread = new HandlerThread("StickerSearchThread");
            searchThread.start();
        }

        if (this.searchHandler == null)
            searchHandler = new Handler(searchThread.getLooper());
    }


    public void setCategoryStickersMap(LinkedHashMap<String, LinkedHashMap<String, StickerModel>> allStickers, LinkedHashMap<String, StickerCategoryModel> categoryModelMap) {
        searchHandler.removeCallbacksAndMessages(null);//stopping earlier processing

        if (this.allSticker != null) {
            this.allSticker.clear();
        } else this.allSticker = new LinkedHashMap<>();

        this.allSticker.putAll(allStickers);
        this.categoryModelMap = categoryModelMap;
    }

    private ArrayList<Object> filteredList;

    public void submitQuery(String query) {
        searchHandler.removeCallbacksAndMessages(null);
        this.query = query;
        searchHandler.post(r);

    }

    Runnable r = new Runnable() {
        @Override
        public void run() {
            startSearching();
        }
    };

    private String query;

    @SuppressWarnings({"UnusedLabel", "UnnecessaryLabelOnContinueStatement"})
    private void startSearching() {
        if (filteredList == null)
            filteredList = new ArrayList<>();
        else
            filteredList.clear();

        level_1:
        //iterate over all the categories
        for (Map.Entry<String, LinkedHashMap<String, StickerModel>> entry : allSticker.entrySet()) {
            if (entry.getValue() == null) continue level_1;
            boolean insertedHeader = false;
            level_2:
            //iterate over all stickers in a category
            for (Map.Entry<String, StickerModel> innerEntry : entry.getValue().entrySet()) {
                final HashMap<String, Integer> keywords = innerEntry.getValue().keywords;

                //ignoring unloaded/non-downloaded stickers
                if (!innerEntry.getValue().stickerPresentInAssets && innerEntry.getValue().localUri == null)
                    continue level_2;

                if (keywords == null) continue level_2;
                level_3:
                //iterate through all the given search tags
                for (Map.Entry<String, Integer> keywordEntry : keywords.entrySet()) {
                    if (keywordEntry.getKey().toLowerCase().contains(query) || query.contains(keywordEntry.getKey().toLowerCase())) {
                        if (!insertedHeader) {
                            filteredList.add(categoryModelMap.get(entry.getKey()).title);
                            insertedHeader = true;
                        }
                        filteredList.add(innerEntry.getValue());
                        continue level_2;
                    }
                }
            }
        }

        if (filteredList.size() == 0){
            filteredList.add("No Stickers Found");
            filteredList.add("");
        }

        if (filteredStickersListener != null)
            filteredStickersListener.onListFiltered(filteredList);
    }

    FilteredStickersListener filteredStickersListener;

    public void setFilteredStickersListener(FilteredStickersListener filteredStickersListener) {
        this.filteredStickersListener = filteredStickersListener;
    }

    public interface FilteredStickersListener {
        /**
         * @param list will have the object of two instance types
         *             String will represent just the header
         *             other wise the sticker itself
         */
        void onListFiltered(ArrayList<Object> list);
    }
}
