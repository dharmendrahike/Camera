package com.pulseapp.android.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pulseapp.android.R;
import com.pulseapp.android.ui.GLTextureView;

/**
 * Created by bajaj on 15/1/16.
 */
public class VideoThumbnailGLTVAdapter extends RecyclerView.Adapter<VideoThumbnailGLTVAdapter.ViewHolder> {

    //        private ArrayList<Bitmap> items;
    private int itemLayout;

    public VideoThumbnailGLTVAdapter(/*ArrayList<Bitmap> items, */int itemLayout) {
/*            this.items = items;*/
        this.itemLayout = itemLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
//            Bitmap item = items.get(position);
/*            holder.text.setText(item.getText());*/
//            holder.image.setImageBitmap(item);
/*            Picasso.with(holder.image.getContext()).cancelRequest(holder.image);
            Picasso.with(holder.image.getContext()).load(item.getImage()).into(holder.image);*/
        holder.itemView.setTag(position); //TODO Tag can be the timestamp of the video frame being displayed.
//        holder.image.requestRender();
    }

    @Override
    public int getItemCount() {
//            return items.size();
        return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public GLTextureView image;
//            public TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            image = (GLTextureView) itemView.findViewById(R.id.video_thumbnail_view);
//              text = (TextView) itemView.findViewById(R.id.text);
        }
    }
}