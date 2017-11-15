package com.pulseapp.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.pulseapp.android.MasterClass;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.adapters.EmoticonRecyclerAdapter;
import com.pulseapp.android.models.StickerCategoryModel;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 21/3/16.
 */
public class ChildEmojiPagerFragment extends BaseFragment {

    private final String TAG = getClass().getSimpleName();
    private RecyclerView recyclerView;
    private LinkedHashMap<String, StickerModel> stickersMap;
    private StickerCategoryModel categoryModelTag;
    private boolean isUnlockScreen = false;


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private Boolean institutionNeeded_Boolean = null;

    private boolean isInstitutionNeeded() {
        if (institutionNeeded_Boolean != null) return institutionNeeded_Boolean;
        SharedPreferences prefs;
        prefs = MasterClass.getGlobalContext().getSharedPreferences(AppLibrary.APP_SETTINGS, 0);
        institutionNeeded_Boolean = prefs.getBoolean(AppLibrary.INSTITUTION_NEEDED, false);
        return institutionNeeded_Boolean;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LinearLayout ll = new LinearLayout(getActivity());
        if (savedInstanceState != null) return ll;

        isUnlockScreen = (isInstitutionNeeded()) && (categoryModelTag.categoryId.equals(mFireBaseHelper.getMyUserId()) && mFireBaseHelper.getMyUserModel().getMyInstitutionId() == null);
        if (!isUnlockScreen) {//normal flow

            recyclerView = new RecyclerView(context);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);

            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position == 0)
                        return 3;
                    else
                        return 1;
                }
            });

            recyclerView.setLayoutManager(gridLayoutManager);
            ll.addView(recyclerView);
            ((LinearLayout.LayoutParams) recyclerView.getLayoutParams()).width = LinearLayout.LayoutParams.MATCH_PARENT;
            ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((VideoEditorFragment) getParentFragment().getParentFragment()).hideEmoticonViewPager();
                }
            });
            return ll;
        } else {
            View rootView = inflater.inflate(R.layout.fragment_unlock_stickers, container, false);
            initUnlockView(rootView);
            return rootView;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) return;

        if (!this.isUnlockScreen)
            loadEmojiz();
    }


    public void setCategoryStickersMap(LinkedHashMap<String, StickerModel> stickersMap) {
        LinkedHashMap<String, StickerModel> tempMap = new LinkedHashMap<>();
        for (Map.Entry<String, StickerModel> entry : stickersMap.entrySet()) {
            if (!entry.getValue().stickerPresentInAssets && (entry.getValue().localUri == null || (entry.getValue().localUri != null && !(new File(entry.getValue().localUri)).exists()))) {
                Log.i(TAG, " skipping non-downloaded sticker " + entry.getKey());
            } else tempMap.put(entry.getKey(), entry.getValue());
        }
        this.stickersMap = tempMap;
    }

    public StickerCategoryModel getCategoryModelTag() {
        return categoryModelTag;
    }

    public void setCategoryModelTag(StickerCategoryModel categoryModelTag) {
        this.categoryModelTag = categoryModelTag;
    }

    private void loadEmojiz() {
        EmoticonRecyclerAdapter adapter = new EmoticonRecyclerAdapter(context, stickersMap, (VideoEditorFragment) getParentFragment().getParentFragment(), (ParentEmojiPagerFragment) getParentFragment());
        recyclerView.setAdapter(adapter);
//        recyclerView.setPadding(0, AppLibrary.convertDpToPixels(context, 00), 0, 0);

    }

    private void initUnlockView(View rootView) {
        rootView.findViewById(R.id.unlockView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((VideoEditorFragment) getParentFragment().getParentFragment()).hideEmoticonViewPager();
                    }
                }, 700);
                ((CameraActivity) getActivity()).openInstitutionEditFragment(true);
            }
        });
    }
}
