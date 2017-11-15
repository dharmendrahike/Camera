package com.pulseapp.android.adapters;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.pulseapp.android.R;

import java.util.ArrayList;

/**
 * Created by bajaj on 14/1/16.
 */
public class VideoThumbnailAdapter extends RecyclerView.Adapter<VideoThumbnailAdapter.ViewHolder> {

        private ArrayList<Bitmap> items;
        private int itemLayout;

        public VideoThumbnailAdapter(ArrayList<Bitmap> items, int itemLayout) {
            this.items = items;
            this.itemLayout = itemLayout;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            Bitmap item = items.get(position);
/*            holder.text.setText(item.getText());*/
            holder.image.setImageBitmap(item);
/*            Picasso.with(holder.image.getContext()).cancelRequest(holder.image);
            Picasso.with(holder.image.getContext()).load(item.getImage()).into(holder.image);*/
            holder.itemView.setTag(item);
        }

        @Override
        public int getItemCount() {
                return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ImageView image;
//            public TextView text;

            public ViewHolder(View itemView) {
                super(itemView);
                image = (ImageView) itemView.findViewById(R.id.image);
//              text = (TextView) itemView.findViewById(R.id.text);
            }
        }
    }
