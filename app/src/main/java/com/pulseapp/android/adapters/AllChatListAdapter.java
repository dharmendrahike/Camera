package com.pulseapp.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.modelView.SliderMessageModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;

/**
 * Created by deepankur on 30/4/16.
 */
public class AllChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    private Context context;
    private ArrayList<SliderMessageModel> allChatsModels;// here each null item will represent headerType;ie switch of alphabet
    private static final int TYPE_HEADER = 11111;
    private static final int TYPE_ITEM = 55555;
    private static final int TYPE_NEW_GROUP = 6666;

    private RecyclerViewClickInterface recyclerViewClickInterface;
    public FontPicker fontPicker;

    public AllChatListAdapter(Context context, ArrayList<SliderMessageModel> allChatsModels, RecyclerViewClickInterface recyclerViewClickInterface) {
        this.context = context;
        this.allChatsModels = allChatsModels;
        this.recyclerViewClickInterface = recyclerViewClickInterface;
        fontPicker = FontPicker.getInstance(context);
    }

    public ArrayList<SliderMessageModel> getAllChatsModels() {
        return allChatsModels;
    }

    public void setAllChatsModels(ArrayList<SliderMessageModel> allChatsModels) {
        this.allChatsModels = allChatsModels;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == TYPE_ITEM)
            return new VHItem(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_all_chat_list, parent, false));

        if (viewType == TYPE_HEADER)
            return new VHHeader(new TextView(context));

        if (viewType == TYPE_NEW_GROUP)
            return new VHNewGroup(LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.item_chat_list_header, parent, false));

        throw new RuntimeException("there is no type that matches the type " + viewType + "  wtf ");
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VHItem) {
            ((VHItem) holder).name.setText(allChatsModels.get(position).displayName);
            int roomType = allChatsModels.get(position).roomType;
            ((VHItem) holder).rootView.setTag(allChatsModels.get(position));
            if (roomType == FRIEND_ROOM) {
                Picasso.with(context).load(allChatsModels.get(position).imageUrl).
                        transform(new RoundedTransformation()).into(((VHItem) holder).imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        Picasso.with(context).load(R.drawable.error_profile_picture).
                                transform(new RoundedTransformation()).into(((VHItem) holder).imageView);
                    }
                });
            } else {
                Transformation t = new LetterTileRoundedTransformation(context, allChatsModels.get(position).displayName);
                Picasso.with(context).load(R.drawable.transparent_image).
                        transform(t).into(((VHItem) holder).imageView);
            }
            ((VHItem) holder).rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recyclerViewClickInterface.onItemClick(0, allChatsModels.get(holder.getAdapterPosition()));
                }
            });
        }
        if (holder instanceof VHHeader) {
            char displayAlphabet = allChatsModels.get(position + 1).displayName.charAt(0);
            ((VHHeader) holder).alphabet.setText(String.valueOf(Character.toUpperCase(displayAlphabet)));
        }
        if (holder instanceof VHNewGroup) {

        }
    }


    @Override
    public int getItemCount() {
        return (allChatsModels == null ? 0 : allChatsModels.size());
    }

    /**
     * @param position the position from 0 to array list size
     * @return header type if the array List item is alphabet
     * new group if index is 0
     * normal otherwise
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_NEW_GROUP;
        return (allChatsModels.get(position) == null ? TYPE_HEADER : TYPE_ITEM);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }


    class VHItem extends RecyclerView.ViewHolder {
        TextView name;
        ImageView imageView;
        View rootView;

        public VHItem(View itemView) {
            super(itemView);
            rootView = itemView;
            name = (TextView) itemView.findViewById(R.id.cardTV);
            imageView = (ImageView) itemView.findViewById(R.id.cardIV);

        }
    }

    class VHHeader extends RecyclerView.ViewHolder {
        TextView alphabet;
        FontPicker fontPicker = FontPicker.getInstance(context);

        public VHHeader(View itemView) {
            super(itemView);
            alphabet = (TextView) itemView;
            alphabet.setTypeface(fontPicker.getMontserratRegular());
            alphabet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            alphabet.setMinHeight(AppLibrary.convertDpToPixels(context, 22));
            alphabet.setGravity(Gravity.CENTER_VERTICAL);
            alphabet.setPadding(AppLibrary.convertDpToPixels(context, 16), 0, 0, 0);
            alphabet.setAlpha(0.54f);
        }
    }

    class VHNewGroup extends RecyclerView.ViewHolder {

        TextView alphabetTv;
        View rootView;
        ImageView imageView;

        public VHNewGroup(View itemView) {
            super(itemView);
            rootView = itemView;
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recyclerViewClickInterface != null)
                        recyclerViewClickInterface.onItemClick(0, null);
                }
            });
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final SliderMessageModel tag = (SliderMessageModel) rootView.getTag();
                    if (tag.roomType == FRIEND_ROOM)
                        BaseFragment.getBaseFragmentInstance().showGenericProfilePopup(context, tag.displayName, tag.imageUrl, "");//todo don't have handle here
                    return false;
                }
            });

            alphabetTv = (TextView) itemView.findViewById(R.id.alphabetTv);
            imageView = (ImageView) itemView.findViewById(R.id.groupIV);
            alphabetTv.setTypeface(fontPicker.getMontserratRegular());
        }
    }
}
