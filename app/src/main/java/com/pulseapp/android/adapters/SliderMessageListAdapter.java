package com.pulseapp.android.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.pulseapp.android.R;
import com.pulseapp.android.customTextViews.MontserratRegularTextView;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.LinkedHashMap;

/**
 * Created by deepankur on 11/4/16.
 */
public class SliderMessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    private final Context mContext;
    private RecyclerViewClickInterface mClickInterface;
    private LinkedHashMap<String, SliderMessageModel> messageList;
    private final String TAG = getClass().getSimpleName();
    private ViewControlsCallback viewControlsCallback;

    public SliderMessageListAdapter(Context context, LinkedHashMap<String, SliderMessageModel> messageList, RecyclerViewClickInterface clickInterface) {
        this.mClickInterface = clickInterface;
        this.messageList = messageList;
        this.mContext = context;
        viewControlsCallback = (ViewControlsCallback) context;
    }

    private SliderMessageModel getMessageDetailsIndex(int index) {
        String[] keySet = messageList.keySet().toArray(new String[messageList.size()]);
        return messageList.get(keySet[index]);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_HOLDER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_slider_message, parent, false);
            return new ViewHolder(v);
        } else if (viewType == NO_MESSAGE_HOLDER) {
            LinearLayout ll = new LinearLayout(mContext);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ImageView iv = new ImageView(mContext);
            iv.setImageResource(R.drawable.messages_no_results_svg);
            MontserratRegularTextView tv = new MontserratRegularTextView(mContext);
            tv.setTextColor(Color.BLACK);
            tv.setBackgroundColor(mContext.getResources().getColor(R.color.white));
            tv.setAlpha(0.87f);
            tv.setText("No results found");
            int padding12 = AppLibrary.convertDpToPixels(mContext, 12);
            int padding16 = AppLibrary.convertDpToPixels(mContext, 16);
            tv.setPadding(padding16, padding16, padding16, padding12);
            ll.addView(iv);
            ll.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ((LinearLayout.LayoutParams) iv.getLayoutParams()).topMargin = padding16 * 2;
            ll.setGravity(Gravity.CENTER_HORIZONTAL);
            return new NoResultViewHolder(ll);
        } else if (viewType == PADDING_HOLDER) {
            View v = new View(mContext);
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLibrary.convertDpToPixels(mContext, 56)));
            return new BottomPaddingHolder(v);
        }
        throw new RuntimeException(" undefined view type");
    }

    public void setMessagesList(LinkedHashMap<String, SliderMessageModel> messageList) {
        this.messageList = messageList;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewholder, int position) {
        SliderMessageListAdapter.ViewHolder holder = null;
        if (viewholder instanceof ViewHolder) {
            holder = (ViewHolder) viewholder;
            View divider = ((LinearLayout) holder.rootView).getChildAt(((LinearLayout) holder.rootView).getChildCount() - 1);//hiding last divider
            divider.setVisibility(position == messageList.size() - 1 ? View.GONE : View.VISIBLE);
        } else return;

        if ((position > this.messageList.size() - 1))
            return;

        final SliderMessageModel messageModel = getMessageDetailsIndex(position);
        holder.messageStatusIv.setImageResource(0);
        holder.nameTv.setText(messageModel.displayName);
        if (messageModel.imageUrl != null && !messageModel.imageUrl.isEmpty()) {
            Picasso.with(mContext).load(messageModel.imageUrl).transform(new RoundedTransformation()).placeholder(R.drawable.error_profile_picture).
                    into(holder.profileImageIv);
        } else {
            Transformation t = new LetterTileRoundedTransformation(mContext, messageModel.displayName);
            Picasso.with(mContext).load(R.drawable.transparent_image).
                    transform(t).into(holder.profileImageIv);
        }

        holder.timeTv.setText(AppLibrary.timeAccCurrentTime(messageModel.updatedAt));
        if (AppLibrary.checkStringObject(messageModel.status) != null && messageModel.messageType > 4) {
            holder.statusTv.setTextColor(Color.BLACK);
        }
        holder.statusTv.setText(messageModel.status);
        messageModel.displayStatus(holder.messageStatusIv, holder.statusTv);
        holder.statusTv.setText(messageModel.status);
        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageModel.status.equals(SENDING_FAILED_TEXT)) {
                    viewControlsCallback.onResumeUpload(messageModel.roomId);
                } else {
                    if (mClickInterface != null)
                        mClickInterface.onItemClick(0, messageModel);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (messageList == null || messageList.size() == 0) ? 1 : messageList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (this.messageList == null || this.messageList.size() == 0)
            return NO_MESSAGE_HOLDER;
//        return super.getItemViewType(position);
        if (this.messageList.size() > 1 && position == messageList.size()) {
            return PADDING_HOLDER;
        }
        return ITEM_HOLDER;
    }

    final int ITEM_HOLDER = 1, NO_MESSAGE_HOLDER = 2, PADDING_HOLDER = 3;

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageIv;
        TextView nameTv, timeTv, statusTv;
        ImageView messageStatusIv;
        View rootView;

        public ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            profileImageIv = (ImageView) itemView.findViewById(R.id.cardIV);
            nameTv = (TextView) itemView.findViewById(R.id.cardNameTV);
            timeTv = (TextView) itemView.findViewById(R.id.timeTV);
            statusTv = (TextView) itemView.findViewById(R.id.messageStatusTV);
            messageStatusIv = (ImageView) itemView.findViewById(R.id.messageStatusIV);
        }
    }

    private class NoResultViewHolder extends RecyclerView.ViewHolder {
        public NoResultViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * padding to escape floating action button
     */
    private class BottomPaddingHolder extends RecyclerView.ViewHolder {

        public BottomPaddingHolder(View itemView) {
            super(itemView);
        }
    }

    public interface ViewControlsCallback {
        void onResumeUpload(String roomId);
    }
}
