package com.pulseapp.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.SquareImageView;
import com.pulseapp.android.fragments.ParentEmojiPagerFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by deepankur on 9/27/16.
 */
public class StickerSearchRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected Context context;
    private static VideoEditorFragment videoEditorFragment;
    private ArrayList<Object> filteredList;
    private String TAG = this.getClass().getSimpleName();
    protected int width;
    ParentEmojiPagerFragment fragment;


    public StickerSearchRecyclerAdapter(Context context, ArrayList<Object> filteredList, VideoEditorFragment videoEditorFragment, ParentEmojiPagerFragment fragment) {
        this.context = context;
        this.filteredList = filteredList;
        this.videoEditorFragment = videoEditorFragment;
        this.width = AppLibrary.getDeviceWidth((Activity) context) / 3;
        this.fragment = fragment;
    }

    public void refreshData(ArrayList<Object> filteredList) {
        this.filteredList = filteredList;
        notifyDataSetChanged();
    }

    final int HEADER = 2, STICKER = 6, PADDING = 8;

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return PADDING;
        if (filteredList.get(position - 1) instanceof String)
            return HEADER;
        return STICKER;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == STICKER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.emojicell, parent, false);
            return new ViewHolder(v);
        } else if (viewType == PADDING) {
            View v = new View(parent.getContext());
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLibrary.convertDpToPixels(parent.getContext(), 108)));
            return new PaddingViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);
            v.setPadding(AppLibrary.convertDpToPixels(context, 16), 0, 0, 0);
            ((TextView) v.findViewById(android.R.id.text1)).setTextColor(Color.WHITE);
            return new HeadingViewHolder(v);
        }
    }


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HeadingViewHolder) {
            ((HeadingViewHolder) viewHolder).heading.setText(((String) filteredList.get(position - 1)));
            return;
        } else if (viewHolder instanceof PaddingViewHolder) {
            return;
        }
        ViewHolder holder = (ViewHolder) viewHolder;

        final StickerModel stickerModel = (StickerModel) filteredList.get(position - 1);
        holder.squareImageView.setTag(stickerModel);
        if (stickerModel.stickerPresentInAssets) {
            if (stickerModel.localUri != null)
                Picasso.with(context).load(stickerModel.localUri).resize(width, width).config(Bitmap.Config.RGB_565).centerInside().into(holder.squareImageView);
        } else {
            if (stickerModel.localUri != null)
                Picasso.with(context).load(new File(stickerModel.localUri)).resize(width, width).config(Bitmap.Config.RGB_565).centerInside().into(holder.squareImageView);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList == null ? 0 : filteredList.size() + 1;
    }


    public class PaddingViewHolder extends ViewHolder {

        public PaddingViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class HeadingViewHolder extends ViewHolder {

        TextView heading;

        public HeadingViewHolder(View itemView) {
            super(itemView);
            heading = (TextView) itemView.findViewById(android.R.id.text1);
            heading.setAllCaps(true);
            heading.setTypeface(FontPicker.getInstance(context).getMontserratRegular());
//            heading.setAlpha(0.53f);
            int dp = AppLibrary.convertDpToPixels(context, 1);
            heading.setPadding(16 * dp, 32 * dp, 0, 16 * dp);
            heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            heading.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment != null)
                        fragment.hideKeyboard();
                }
            });
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        SquareImageView squareImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            squareImageView = (SquareImageView) itemView.findViewById(R.id.emoji);
            if (squareImageView == null)
                return;
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
                    int i = filteredList.indexOf(tags[0]);
                    notifyItemChanged(i + 1);
                    // we need to call notify item changed on the clicked item as we setting the new tag
                    //on the clicked view. Now if it is clicked more than once the wrong tag will be set
                    //if correct tag is not set again using onBind View
                }
            });
        }
    }

}



