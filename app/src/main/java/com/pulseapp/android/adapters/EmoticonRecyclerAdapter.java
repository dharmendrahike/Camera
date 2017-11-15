package com.pulseapp.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.SquareImageView;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.ParentEmojiPagerFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.util.AppLibrary;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * Created by deepankur on 6/3/16.
 */
public class EmoticonRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    protected Context context;
    private static VideoEditorFragment videoEditorFragment;
    private LinkedHashMap<String, StickerModel> stickersMap;
    private String TAG = this.getClass().getSimpleName();
    protected int width;
    private ParentEmojiPagerFragment fragment;

    public EmoticonRecyclerAdapter(Context context, LinkedHashMap<String, StickerModel> stickersMap, VideoEditorFragment videoEditorFragment, ParentEmojiPagerFragment fragment) {
        this.context = context;
        this.stickersMap = stickersMap;
        this.videoEditorFragment = videoEditorFragment;
        this.width = AppLibrary.getDeviceWidth((Activity) context) / 3;
        this.fragment = fragment;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.emojicell, parent, false);
            return new ViewHolder(v);
        } else {
            View v = new View(parent.getContext());
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLibrary.convertDpToPixels(parent.getContext(), 108)));
            return new PaddingHolder(v);
        }
    }

    final int PADDING = 112, ITEM = 123;

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? PADDING : ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PaddingHolder)
            return;
        ViewHolder viewHolder = (ViewHolder) holder;
        final StickerModel stickerModel = getStickerByindex(position - 1);
        viewHolder.squareImageView.setTag(stickerModel);
        if (stickerModel.stickerPresentInAssets) {
            Log.d(TAG, "onBindViewHolder: stickerPresentInAssets ");
            if (stickerModel.localUri != null)
                Picasso.with(context).load(stickerModel.localUri).resize(width, width).config(Bitmap.Config.RGB_565).centerInside().into(((ViewHolder) holder).squareImageView);
        } else {
            if (stickerModel.localUri != null)
                Picasso.with(context).load(new File(stickerModel.localUri)).resize(width, width).config(Bitmap.Config.RGB_565).centerInside().into(((ViewHolder) holder).squareImageView);
        }
    }

    @Override
    public int getItemCount() {
        return stickersMap == null ? 0 : stickersMap.size() + 1;
    }

    @Override
    public void onViewRecycled(final RecyclerView.ViewHolder holder) {

        if (holder instanceof ViewHolder) {
            Picasso.with(context).cancelRequest(((ViewHolder) holder).squareImageView);
            ((ViewHolder) holder).squareImageView.setImageResource(0);
        }
    }

    private StickerModel getStickerByindex(int index) {
        String[] stickerKeys = stickersMap.keySet().toArray(new String[stickersMap.size()]);
        return stickersMap.get(stickerKeys[index]);

    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        SquareImageView squareImageView;
        public ViewHolder(View itemView) {
            super(itemView);
            squareImageView = (SquareImageView) itemView.findViewById(R.id.emoji);
            squareImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (fragment != null)
                        fragment.hideKeyboard();
                    String TAG = "onClick";
                    int initIVHeight = view.getMeasuredHeight();
                    int initIVWidth = view.getMeasuredWidth();
                    int top = view.getTop();
                    int left = view.getLeft();
                    int[] dimens = {initIVWidth, initIVHeight, top, left};
                    Log.d(TAG, "VTO " + initIVWidth + "<-width height->" + initIVHeight);
                    /**
                     * tag will be an array of objects of size 2 the first one will represent sticker model and the second
                     * fall back positions in case the width and height are 0 form the firebase
                     */
                    Object[] tags = new Object[2];
                    tags[0] = view.getTag();
                    tags[1] = dimens;
                    view.setTag(tags);
                    int[] posXY = new int[2];
                    view.getLocationOnScreen(posXY);
                    int x = posXY[0];
                    int y = posXY[1];
                    Log.d(TAG, "xycor" + " x :" + x + " y :" + y);
                    videoEditorFragment.addEmoticons(view);
                }
            });
        }
    }

    private class PaddingHolder extends RecyclerView.ViewHolder {

        PaddingHolder(View itemView) {
            super(itemView);
        }
    }
}
