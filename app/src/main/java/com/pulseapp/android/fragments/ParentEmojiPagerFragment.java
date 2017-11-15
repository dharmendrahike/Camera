package com.pulseapp.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.pulseapp.android.R;
import com.pulseapp.android.adapters.EmojiPagerAdapter;
import com.pulseapp.android.adapters.StickerSearchRecyclerAdapter;
import com.pulseapp.android.customViews.EmojiPagerTab;
import com.pulseapp.android.data.StickerSearcher;
import com.pulseapp.android.models.StickerCategoryModel;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.OverRideBackEventEditText;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by deepankur on 21/3/16.
 */

public class ParentEmojiPagerFragment extends BaseFragment {
    private ViewPager emojiViewPager;
    private EmojiPagerTab pagerTab;
    private GestureDetector mGestureListener;
    private final String TAG = getClass().getSimpleName();
    public static int PREVIOUS_PAGE_NUMBER;
    private OverRideBackEventEditText searchStickersEt;
    private LinkedHashMap<String, LinkedHashMap<String, StickerModel>> allStickers;
    private RecyclerView filteredRecyclerView;
    private LinkedHashMap<String, StickerCategoryModel> categoryModelMap;
    private ImageView clearTextIv;

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_emoji_pager, container, false);

        if (savedInstanceState != null) return rootView;

        mGestureListener = new GestureDetector(context, new CustomGestureListener());

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureListener.onTouchEvent(event);
                return true;
            }
        });
        emojiViewPager = (ViewPager) rootView.findViewById(R.id.emoji_view_pager);
        pagerTab = (EmojiPagerTab) rootView.findViewById(R.id.pagerTab);
        searchStickersEt = (OverRideBackEventEditText) rootView.findViewById(R.id.stickerSearchET);
        clearTextIv = (ImageView) rootView.findViewById(R.id.clearTextIV);
        clearTextIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchStickersEt.setText("");
            }
        });
        rootView.findViewById(R.id.gradientView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        searchStickersEt.setTypeface(fontPicker.getMontserratRegular());
        searchStickersEt.setOnKeyPreImeListener(new OverRideBackEventEditText.OnKeyPreImeListener() {
            @Override
            public void onKeyPreImePressed(int keyCode, KeyEvent event) {
                Log.d(TAG, "onKeyPreIme: keycode " + keyCode + " event " + event);
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    searchStickersEt.clearFocus();
                }
            }
        });

        try {
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(searchStickersEt, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        searchStickersEt.addTextChangedListener(textWatcher);
        searchStickersEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    searchStickersEt.setHint("");
                else
                    searchStickersEt.setHint("Search stickers");
            }
        });
        filteredRecyclerView = (RecyclerView) rootView.findViewById(R.id.filteredStickersRecycler);
//        ChatCameraStickerDownloader.getChatCameraStickerDownloader(context).registerEmoticonFragment(this);
        return rootView;
    }

    public void notifyFragmentsAdded(ArrayList<ChildEmojiPagerFragment> fragmentArrayList) {
        StickerCategoryModel[] models = new StickerCategoryModel[fragmentArrayList.size()];

        for (int i = 0; i < fragmentArrayList.size(); i++) {
            models[i] = fragmentArrayList.get(i).getCategoryModelTag();
        }
        emojiViewPager.setAdapter(new EmojiPagerAdapter(getChildFragmentManager(), fragmentArrayList));
        if (fragmentArrayList.size() > 0) {//don't worry about the pager tab with 0 fragments
            pagerTab.setCategoryModelsList(models);
            pagerTab.setViewPager(emojiViewPager);
        }
        emojiViewPager.setCurrentItem(PREVIOUS_PAGE_NUMBER, false);
        emojiViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                PREVIOUS_PAGE_NUMBER = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void setAllStickers(LinkedHashMap<String, LinkedHashMap<String, StickerModel>> allStickers, LinkedHashMap<String, StickerCategoryModel> categoryModelMap) {
        this.allStickers = allStickers;
        this.categoryModelMap = categoryModelMap;
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            ((VideoEditorFragment) getParentFragment()).hideEmoticonViewPager();
            return super.onSingleTapConfirmed(e);
        }
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            notifyQueryChanged(s);
            if (s.toString().trim().length() > 0) {
                emojiViewPager.setVisibility(View.GONE);
                pagerTab.setVisibility(View.GONE);
                filteredRecyclerView.setVisibility(View.VISIBLE);
                searchTheStickers(s.toString().trim().toLowerCase());
                clearTextIv.setVisibility(View.VISIBLE);
            } else {
                emojiViewPager.setVisibility(View.VISIBLE);
                pagerTab.setVisibility(View.VISIBLE);
                filteredRecyclerView.setVisibility(View.GONE);
                clearTextIv.setVisibility(View.GONE);
            }
        }
    };
    StickerSearcher stickerSearcher;
    StickerSearchRecyclerAdapter searchAdapter;
    ArrayList<Object> list;

    void searchTheStickers(String query) {
        final ParentEmojiPagerFragment fragment = this;
        if (stickerSearcher == null) {
            stickerSearcher = new StickerSearcher();
            stickerSearcher.setCategoryStickersMap(allStickers, categoryModelMap);
            stickerSearcher.setFilteredStickersListener(new StickerSearcher.FilteredStickersListener() {
                @Override
                public void onListFiltered(final ArrayList<Object> newList) {
                    if (getActivity() != null)
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (list == null)
                                    list = new ArrayList<>(newList);
                                else list = (ArrayList<Object>) newList.clone();
                                if (searchAdapter == null) {
                                    searchAdapter = new StickerSearchRecyclerAdapter(getActivity(), list, (VideoEditorFragment) getParentFragment(), fragment);
                                    GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
                                    filteredRecyclerView.setAdapter(searchAdapter);
                                    filteredRecyclerView.setLayoutManager(gridLayoutManager);
                                    gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                        @Override
                                        public int getSpanSize(int position) {
                                            if (position == 0)
                                                return 3;
                                            if (list.size() <= position) {
                                                Log.e(TAG, "Illigal range " + position + " size is " + list.size());
                                                return 1;
                                            }
                                            if (list.get(position - 1) instanceof String)
                                                return 3;
                                            return 1;
                                        }
                                    });

                                } else {
                                    searchAdapter.refreshData(list);
                                }
                            }
                        });
                }
            });
        }

        stickerSearcher.submitQuery(query);
    }

    public void hideKeyboard() {
        toggleSoftKeyboard(getActivity(), searchStickersEt, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dumpData();
    }


    public void dumpData() {
        if (allQueries != null)
            processAllQueries();
    }

    /**
     * D.S. recording all the strings after onTExt change of edit text,
     * List is cleared once
     */
    ArrayList<String> allQueries;

    private void notifyQueryChanged(Editable s) {
        if (allQueries == null) allQueries = new ArrayList<>();
        allQueries.add(s.toString());
    }


    enum DataRecordingAlgorithm {SINGLE_MAXIMA, ALL_MAXIMA, ALL_MINIMA,}

    DataRecordingAlgorithm recordingAlgorithm = DataRecordingAlgorithm.ALL_MAXIMA;

    private void processAllQueries() {
        ArrayList<String> filteredList = new ArrayList<>();
        if (recordingAlgorithm == DataRecordingAlgorithm.ALL_MAXIMA) {
            for (int i = 1; i < allQueries.size() - 1; i++) {//for all maxima
                int currentLength = allQueries.get(i).length();
                if (currentLength > allQueries.get(i - 1).length() && currentLength > allQueries.get(i + 1).length()) {
                    filteredList.add(allQueries.get(i));
                }
            }
        } else if (recordingAlgorithm == DataRecordingAlgorithm.SINGLE_MAXIMA) {
            String largestString = "";
            for (int i = 0; i < allQueries.size(); i++) {
                if (allQueries.get(i).length() > largestString.length()) {
                    largestString = allQueries.get(i);
                }
            }
            filteredList.add(largestString);
        } else if (recordingAlgorithm == DataRecordingAlgorithm.ALL_MINIMA) {
            for (int i = 1; i < allQueries.size() - 1; i++) {//for all maxima
                int currentLength = allQueries.get(i).length();
                if (currentLength < allQueries.get(i - 1).length() && currentLength < allQueries.get(i + 1).length()) {
                    filteredList.add(allQueries.get(i));
                }
            }
        }

        if (filteredList.size()>0)
            writeData(filteredList, recordingAlgorithm);

        allQueries.clear();
    }

    DatabaseReference newFireBase;

    private void writeData(ArrayList<String> filteredQuery, DataRecordingAlgorithm recordingAlgorithm) {
        if (newFireBase == null)
            newFireBase = mFireBaseHelper.getNewFireBase(ANCHOR_QUERIED_STICKER, null);
        HashMap<String, Integer> keyValPair = new HashMap<>();
        for (int i = 0; i < filteredQuery.size(); i++) {
            keyValPair.put(filteredQuery.get(i), 1);
        }
        DatabaseReference push = newFireBase.push();
        push.child("query").setValue(keyValPair);
        push.child("recordingAlgorithm").setValue(String.valueOf(recordingAlgorithm));
        push.child("userID").setValue(mFireBaseHelper.getMyUserId());
    }
}
