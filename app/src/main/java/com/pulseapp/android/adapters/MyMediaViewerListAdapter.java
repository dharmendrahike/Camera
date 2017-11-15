package com.pulseapp.android.adapters;

import android.graphics.Color;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.ViewerDetails;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;

import java.util.ArrayList;

/**
 * Created by user on 6/22/2016.
 */
public class MyMediaViewerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    private ArrayList<Object> objectsArrayList;

    public MyMediaViewerListAdapter(ArrayList<Object> list) {
        this.objectsArrayList = list;
    }


    private class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView viewerNameTv;
        ImageView screenShotIv;

        ItemViewHolder(View itemView) {
            super(itemView);
            viewerNameTv = (TextView) ((LinearLayout) (((LinearLayout) itemView).getChildAt(0))).getChildAt(0);
            screenShotIv = (ImageView) (((LinearLayout) ((LinearLayout) itemView).getChildAt(0)).getChildAt(1));
            screenShotIv.setAlpha(0.5f);
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView;
            header.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            header.setTypeface(FontPicker.getInstance(itemView.getContext()).getMontserratSemiBold());
            header.setTextColor(Color.WHITE);
            header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            header.setAllCaps(true);
            header.setPadding(AppLibrary.convertDpToPixels(itemView.getContext(), 16), AppLibrary.convertDpToPixels(itemView.getContext(), 16), 0, 0);
//            header.setGravity(Gravity.CENTER);
        }
    }

    private final int VIEW_TYPE_HEADER = 21323, VIEW_TYPE_NORMAL = 987987;

    @Override
    public int getItemViewType(int position) {
        return objectsArrayList.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_NORMAL;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER)
            return new HeaderViewHolder(new MontserratRegularTextView(parent.getContext()));

        LinearLayout linearLayout = new LinearLayout(parent.getContext());
        LinearLayoutCompat.LayoutParams linearLayoutParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.gravity = Gravity.CENTER_VERTICAL;
        linearLayout.setLayoutParams(linearLayoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        LinearLayout horizontalRow = new LinearLayout(parent.getContext());
        horizontalRow.setLayoutParams(layoutParams);

        MontserratRegularTextView textView = new MontserratRegularTextView(parent.getContext());
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setTextColor(Color.WHITE);
        textView.setMinHeight(AppLibrary.convertDpToPixels(parent.getContext(), 36));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setSingleLine(true);
        textView.setPadding(AppLibrary.convertDpToPixels(parent.getContext(), 16), AppLibrary.convertDpToPixels(parent.getContext(), 8), 0, AppLibrary.convertDpToPixels(parent.getContext(), 8));

        ImageView screenShotImageView = new ImageView(parent.getContext());
        screenShotImageView.setImageResource(R.drawable.screen_shot_svg);
        screenShotImageView.setPadding(0, AppLibrary.convertDpToPixels(parent.getContext(), 8), AppLibrary.convertDpToPixels(parent.getContext(), 16), 0);
        screenShotImageView.setVisibility(View.GONE);

        horizontalRow.addView(textView);
        horizontalRow.addView(screenShotImageView);
        ((LinearLayout.LayoutParams) screenShotImageView.getLayoutParams()).gravity = Gravity.END;
        ((LinearLayout.LayoutParams) screenShotImageView.getLayoutParams()).leftMargin = AppLibrary.convertDpToPixels(parent.getContext(), 8);
        linearLayout.addView(horizontalRow);
        return new ItemViewHolder(linearLayout);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        Object o = objectsArrayList.get(position);
        if (o instanceof String) {
            ((HeaderViewHolder) viewHolder).header.setText(((String) o));
        } else if (o instanceof Integer) {
            ItemViewHolder holder = (ItemViewHolder) viewHolder;
            Integer viewedCount = (Integer) o;
            holder.viewerNameTv.setText(viewedCount + " Web Viewers ");
            holder.viewerNameTv.setAlpha(1f);
            holder.screenShotIv.setVisibility(View.GONE);
        } else if (o instanceof android.util.Pair) {
            ItemViewHolder holder = (ItemViewHolder) viewHolder;
            Pair<String, ViewerDetails> pair = (Pair<String, ViewerDetails>) o;
            holder.viewerNameTv.setText(pair.second.name);

            boolean screenShotted = pair.second.screenShotted;
            if (screenShotted) {
                holder.viewerNameTv.setAlpha(0.5f);
                holder.screenShotIv.setVisibility(View.VISIBLE);
            } else {
                holder.viewerNameTv.setAlpha(1f);
                holder.screenShotIv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return objectsArrayList == null ? 0 : objectsArrayList.size();
    }
}