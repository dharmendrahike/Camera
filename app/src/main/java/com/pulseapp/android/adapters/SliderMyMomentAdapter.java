package com.pulseapp.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.modelView.MediaModelView;
import com.pulseapp.android.models.ViewerDetails;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 27/4/16.
 */
public class SliderMyMomentAdapter extends RecyclerView.Adapter<SliderMyMomentAdapter.ViewHolder> implements FireBaseKEYIDS {

    private Context context;
    private LinkedHashMap<String, MediaModelView> mediaList;
    private ViewControlsCallback viewControlsCallback;
    final int pixelsInDp;
    final int pixels32InDp;
    private String momentId;

    public SliderMyMomentAdapter(Context context, LinkedHashMap<String, MediaModelView> mediaArrayList) {
        this.context = context;
        this.mediaList = mediaArrayList;
        viewControlsCallback = (ViewControlsCallback) context;
        pixelsInDp = AppLibrary.convertDpToPixels(context, 1);
        pixels32InDp = AppLibrary.convertDpToPixels(context,32);
    }

    public void setMomentId(String momentId){
        this.momentId = momentId;
    }

    private MediaModelView getMessageDetailsIndex(int index) {
        String[] keySet = mediaList.keySet().toArray(new String[mediaList.size()]);
        return mediaList.get(keySet[index]);
    }

    public void setMediaList(LinkedHashMap<String, MediaModelView> mediaList) {
        this.mediaList = mediaList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_new_my_moment_slider, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onViewRecycled(final ViewHolder holder) {
//        if (holder.imageTask != null) {
//            if (holder.imageTask.getStatus() == AsyncTask.Status.RUNNING || holder.imageTask.getStatus() == AsyncTask.Status.PENDING)
//                ((AsyncTask) holder.imageTask).cancel(true);
//            else
//                holder.imageTask.cleanup();
//
//            holder.imageTask = null;
//        } else {
//            Picasso.with(context).cancelRequest(holder.mediaIv);
//        }
//
//        holder.mediaIv.setImageResource(0);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final MediaModelView mediaModelView = getMessageDetailsIndex(position);
        if (mediaModelView.status == MEDIA_UPLOADING_COMPLETE || mediaModelView.status == MEDIA_UPLOADING_FAILED || mediaModelView.status == MEDIA_UPLOADING_STARTED || mediaModelView.status == DOWNLOADED_MY_MOMENT_MEDIA) {
            if (AppLibrary.getMediaType(mediaModelView.url) == AppLibrary.MEDIA_TYPE_VIDEO) {
                Glide.with(context).load(new File(mediaModelView.url)).centerCrop().bitmapTransform(new jp.wasabeef.glide.transformations.CropCircleTransformation(context)).error(R.drawable.moment_circle_background).into(holder.mediaIv);
            } else {
                Picasso.with(context).load(new File(mediaModelView.url)).transform(new RoundedTransformation()).resize(pixels32InDp,pixels32InDp).centerCrop().error(R.drawable.moment_circle_background).into(holder.mediaIv);
            }
        } else {
            Picasso.with(context).load(R.drawable.moment_circle_background).transform(new RoundedTransformation()).resize(pixels32InDp,pixels32InDp).centerCrop().error(R.drawable.moment_circle_background).into(holder.mediaIv);
        }
        if (mediaModelView.status == MEDIA_UPLOADING_STARTED || mediaModelView.status == UPLOADING_NOT_READY_VIDEO) {
            holder.updatedAtTv.setText(MEDIA_UPLOADING_TEXT);
        } else if (mediaModelView.status == MEDIA_UPLOADING_FAILED) {
            holder.updatedAtTv.setText(MEDIA_UPLOADING_FAILED_TEXT);
        } else if (mediaModelView.status == MEDIA_UPLOADING_COMPLETE) {
            if (position == 0) {
                viewControlsCallback.updateLatestMediaTime(AppLibrary.timeAccCurrentTime(mediaModelView.createdAt),momentId);
            }
            if (mediaModelView.mediaState == 0 || mediaModelView.mediaState == MEDIA_ACTIVE)
                holder.updatedAtTv.setText(AppLibrary.timeAccCurrentTime(mediaModelView.createdAt));
            else
                holder.updatedAtTv.setText("Pending Approval");
        } else if (mediaModelView.status == DOWNLOADING_MY_MOMENT_MEDIA) {
            //TODO update image with a placeholder
            holder.updatedAtTv.setText(MEDIA_DOWNLOADING_TEXT);
        } else if (mediaModelView.status == DOWNLOAD_MY_MOMENT_MEDIA_FAILED) {
            holder.updatedAtTv.setText(MEDIA_DOWNLOADING_FAILED_TEXT);
        } else if (mediaModelView.status == DOWNLOADED_MY_MOMENT_MEDIA) {
            if (position == 0) {
                viewControlsCallback.updateLatestMediaTime(AppLibrary.timeAccCurrentTime(mediaModelView.createdAt),momentId);
            }
            if (mediaModelView.mediaState == 0 || mediaModelView.mediaState == MEDIA_ACTIVE)
                holder.updatedAtTv.setText(AppLibrary.timeAccCurrentTime(mediaModelView.createdAt));
            else
                holder.updatedAtTv.setText("Pending Approval");
        }

        RelativeLayout.LayoutParams newParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        newParams.addRule(RelativeLayout.END_OF, holder.mediaIv.getId());

        if (mediaModelView.mediaText != null) {
            newParams.setMargins(0, pixelsInDp * 32, 0, 0);
            holder.mediaNameTv.setText(mediaModelView.mediaText);
            holder.mediaNameTv.setSingleLine(true);
            holder.mediaNameTv.setEllipsize(TextUtils.TruncateAt.END);
            holder.mediaNameTv.setVisibility(View.VISIBLE);
        } else {
            holder.mediaNameTv.setVisibility(View.GONE);
            newParams.addRule(RelativeLayout.CENTER_VERTICAL);
        }
        holder.updatedAtTv.setLayoutParams(newParams);


        if (mediaModelView.viewerDetails == null  || mediaModelView.viewerDetails.size() == 0) {
            if (mediaModelView.webViews > 0) {
                holder.viewTv.setText(String.valueOf(mediaModelView.webViews));
                holder.viewTv.setVisibility(View.VISIBLE);
                holder.viewsIv.setVisibility(View.VISIBLE);
            } else {
                holder.viewTv.setVisibility(View.GONE);
                holder.viewsIv.setVisibility(View.GONE);
            }
            holder.screenShotsTv.setVisibility(View.GONE);
            holder.screenShotsIv.setVisibility(View.GONE);
        } else {
            int count = mediaModelView.viewerDetails.size() + mediaModelView.webViews;
            holder.viewTv.setText(String.valueOf(count));
            holder.viewTv.setVisibility(View.VISIBLE);
            holder.viewsIv.setVisibility(View.VISIBLE);

            int screenShot = 0;
            for (Map.Entry<String,ViewerDetails> viewerEntry : mediaModelView.viewerDetails.entrySet()){
                if (viewerEntry.getValue().screenShotted)
                    screenShot++;
            }
            if (screenShot == 0) {
                holder.screenShotsTv.setVisibility(View.GONE);
                holder.screenShotsIv.setVisibility(View.GONE);
            } else {
                holder.screenShotsTv.setText(String.valueOf(screenShot));
                holder.screenShotsTv.setVisibility(View.VISIBLE);
                holder.screenShotsIv.setVisibility(View.VISIBLE);
            }
        }

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaModelView.status == MEDIA_UPLOADING_FAILED) {
                    viewControlsCallback.onUploadRetryClicked(mediaModelView.mediaId);
                } else if (mediaModelView.status == DOWNLOAD_MY_MOMENT_MEDIA_FAILED) {
                    FireBaseHelper.getInstance(context).onRetryMyMomentMediaDownload(momentId,mediaModelView.mediaId);
                } else if (mediaModelView.status == DOWNLOADING_MY_MOMENT_MEDIA) {
                    // do nothing
                } else if (mediaModelView.status != UPLOADING_NOT_READY_VIDEO) {
                    // launch view media fragment
                    ((CameraActivity) context).loadViewMyMediaFragment(mediaModelView.mediaId,momentId);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList == null ? 0 : mediaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        View rootView;
        TextView mediaNameTv, updatedAtTv, screenShotsTv, viewTv;
        ImageView mediaIv, screenShotsIv, viewsIv;
//        FetchImageTask imageTask;

        public ViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            mediaNameTv = (TextView) itemView.findViewById(R.id.headerTV);
            updatedAtTv = (TextView) itemView.findViewById(R.id.subHeaderTV);
            screenShotsTv = (TextView) itemView.findViewById(R.id.screenShotTV);
            viewTv = (TextView) itemView.findViewById(R.id.viewsTV);
            mediaIv = (ImageView) itemView.findViewById(R.id.card_mainIV);
            mediaIv.setBackgroundResource(R.drawable.moment_circle_background);
            screenShotsIv = (ImageView) itemView.findViewById(R.id.screenShotIV);
            viewsIv = (ImageView) itemView.findViewById(R.id.viewsIV);

        }
    }

    public interface ViewControlsCallback {
        void onUploadRetryClicked(String mediaId);

        void updateLatestMediaTime(String time,String momentId);
    }

//    private class FetchImageTask extends AsyncTask<String, Void, RoundedBitmapDrawable> {
//
//        String imageUrl;
//        ImageView imageView;
//        Bitmap bitmap;
//
//        public FetchImageTask(String url, ImageView imageView) {
//            this.imageUrl = url;
//            this.imageView = imageView;
//        }
//
//        @SuppressLint("LongLogTag")
//        @Override
//        protected RoundedBitmapDrawable doInBackground(String... params) {
//            bitmap = ThumbnailUtils.createVideoThumbnail(new File(imageUrl).getAbsolutePath(), MediaStore.Images.Thumbnails.MICRO_KIND);
//            if (bitmap != null) {
//                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
//                drawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
//                return drawable;
//            } else {
//                return null;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(RoundedBitmapDrawable drawable) {
//            super.onPostExecute(drawable);
//            if (drawable != null)
//                imageView.setImageDrawable(drawable);
//            else if (!isCancelled())
//                imageView.setImageResource(R.drawable.moment_circle_background);
//        }
//
//        @Override
//        protected void onCancelled() {
//            if (bitmap != null) {
//                bitmap.recycle();
//                bitmap = null;
//            }
//        }
//
//        private void cleanup() {
//            if (bitmap != null) {
//                bitmap.recycle();
//                bitmap = null;
//            }
//        }
//    }

    public boolean fileExists(String value) {
        return new File(value).exists();
    }
}
