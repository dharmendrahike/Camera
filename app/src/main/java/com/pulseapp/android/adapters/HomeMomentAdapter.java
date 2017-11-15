package com.pulseapp.android.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.customViews.LetterTileRoundedTransformation;
import com.pulseapp.android.downloader.DynamicDownloader;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.modelView.HomeMomentViewModel;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.FontPicker;
import com.pulseapp.android.util.RoundedTransformation;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.HashSet;

import at.grabner.circleprogress.CircleProgressView;

/**
 * Created by deepankur on 22/4/16.
 */
public class HomeMomentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS, BaseFragment.FollowPopupListener {

    private final String myUserId;
    private Context context;
    private RecyclerViewClickInterface recyclerViewClickInterface;
    private final int MOMENT_VIEW_TYPE;
    private ArrayList<HomeMomentViewModel> momentViewModels;
    private HomeMomentViewModel clickedMoment;
    private GestureDetector gestureDetector;
    private final String TAG = this.getClass().getSimpleName();
    private RecyclerView recyclerView;

    public HomeMomentAdapter(String myUserId, Context context, ArrayList<HomeMomentViewModel> momentViewModels, int momentViewType,
                             RecyclerViewClickInterface recyclerViewClickInterface) {
        this.myUserId = myUserId;
        this.context = context;
        this.momentViewModels = momentViewModels;
        this.MOMENT_VIEW_TYPE = momentViewType;
        this.setMomentModelArrayList(this.momentViewModels);
        this.recyclerViewClickInterface = recyclerViewClickInterface;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
        if (this.MOMENT_VIEW_TYPE == ARTICLE_RECYCLER)
            MAX_HOLDERS = MAX_ITEMS_IN_ARTICLES;
        else if (this.MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER)
            MAX_HOLDERS = MAX_ITEMS_IN_NEARBY;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }


    public void setMomentModelArrayList(final ArrayList<HomeMomentViewModel> momentViewModels) {
        this.momentViewModels = momentViewModels;

        if (recyclerView != null && recyclerView.isComputingLayout()) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    setMomentModelArrayList(momentViewModels);
                }
            }, 100);
        } else
            notifyDataSetChanged();


        if (AUTO_DOWNLOAD_ENABLED && this.MOMENT_VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER)
            initAutomaticDownloading(momentViewModels);
    }

    private static HashSet<String> momentsTriggeredForAutomaticDownload = new HashSet<>();
    private static final boolean AUTO_DOWNLOAD_ENABLED = false;

    private void initAutomaticDownloading(ArrayList<HomeMomentViewModel> momentViewModels) {
        if (momentsTriggeredForAutomaticDownload.size() >= 3 || momentViewModels == null || momentViewModels.size() == 0)
            return;
        for (HomeMomentViewModel momentModel : momentViewModels) {
            if (momentsTriggeredForAutomaticDownload.size() >= 3) break;
            if (momentModel == null || momentsTriggeredForAutomaticDownload.contains(momentModel.momentId))
                continue;
            momentsTriggeredForAutomaticDownload.add(momentModel.momentId);
            momentModel.clickType = HomeMomentViewModel.ClickType.AUTO_DOWNLOAD;
            if (recyclerViewClickInterface != null)
                recyclerViewClickInterface.onItemClick(0, momentModel);

//            momentModel.momentStatus=DOWNLOADING_MOMENT;
//            notifyItemChanged(momentViewModels.indexOf(momentModel));
        }

    }

    public ArrayList<HomeMomentViewModel> getMomentModelArrayList() {
        return this.momentViewModels;
    }

    private final int ITEM_VIEW_TYPE = 11, PADDING_VIEW_TYPE = 22, EMPTY_MOMENT_VIEW_TYPE = 33;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE) {
            View v;
            if (this.MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER || this.MOMENT_VIEW_TYPE == ARTICLE_RECYCLER) {
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cell_around_you, parent, false);
                return new NearbyHolder(v);
            } else {
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.cell_friends_grid, parent, false);
                return new ViewHolder(v);
            }
        } else if (viewType == PADDING_VIEW_TYPE) {//padding
            View v = new View(context);
            v.setLayoutParams(new ViewGroup.LayoutParams(AppLibrary.convertDpToPixels(context, 5), ViewGroup.LayoutParams.WRAP_CONTENT));
            return new PaddingHolder(v);
        } else if (viewType == EMPTY_MOMENT_VIEW_TYPE) {
            int width = AppLibrary.convertDpToPixels(context, getImageDiameterForMoment(this.MOMENT_VIEW_TYPE));//unseen
            LinearLayout linearLayout = new LinearLayout(context);
            ImageView imageView = new ImageView(context);
            //noinspection SuspiciousNameCombination
            imageView.setLayoutParams(new ViewGroup.LayoutParams(width, width));
            imageView.setBackgroundResource(R.drawable.moment_circle_background);
            linearLayout.addView(imageView);
            linearLayout.setGravity(Gravity.TOP);
            return new PaddingHolder(linearLayout);
        }
        throw new RuntimeException("undefined viewType");
    }

    @Override
    public int getItemViewType(int position) {
        if (this.MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER)
            return ITEM_VIEW_TYPE;
        else if (position > this.momentViewModels.size() - 1)
            return EMPTY_MOMENT_VIEW_TYPE;
        else return ITEM_VIEW_TYPE;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder recyclerViewHolder) {

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder recyclerViewHolder, int position) {

        final HomeMomentViewModel homeMoment;
        homeMoment = momentViewModels.get(position);

        if (recyclerViewHolder instanceof NearbyHolder) {
            handleNearbyRecycler((NearbyHolder) recyclerViewHolder, homeMoment, position);
            ((NearbyHolder) recyclerViewHolder).rootView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    clickedMoment = homeMoment;
                    HomeMomentViewModel.viewPositionX = (int) (event.getRawX());
                    HomeMomentViewModel.viewPositionY = (int) event.getRawY();
                    return true;
                }
            });
        } else if ((recyclerViewHolder instanceof ViewHolder)) {
            ViewHolder holder = (ViewHolder) recyclerViewHolder;

            holder.timeTv.setText(homeMoment.updatedAtText);
            if (MOMENT_VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER) {
                handleUnSeenRecycler(holder, homeMoment);
            } else if (MOMENT_VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER) {
                holder.profilePicIv.setAlpha(1f);
                holder.nameTv.setText(AppLibrary.getFirstName(homeMoment.name));
                if (!homeMoment.showDisplayPicture)
                    return;
                if (AppLibrary.checkStringObject(homeMoment.thumbnailUrl) != null) {
                    Picasso.with(context).load(homeMoment.thumbnailUrl).fit().centerCrop().
                            transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
                } else {
                    Picasso.with(context).load(homeMoment.imageUrl).fit().centerCrop().
                            transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
                }
            }

            holder.holder.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);
                    clickedMoment = homeMoment;
                    HomeMomentViewModel.viewPositionX = (int) (event.getRawX());
                    HomeMomentViewModel.viewPositionY = (int) event.getRawY();
                    return true;
                }
            });
        }
    }

    private void handleNearbyRecycler(final NearbyHolder holder, HomeMomentViewModel homeMoment, int position) {
        int width = refreshWidthOfHolder(holder, position);
        holder.momentNameTv.setText(homeMoment.name);

        if (homeMoment.palleteColor != null)
            holder.momentImageIv.setBackgroundColor(Color.parseColor(homeMoment.palleteColor));
        else
            holder.momentImageIv.setBackgroundColor(Color.parseColor("#860088"));

        holder.momentNameTv.setText(homeMoment.name);

        if (MOMENT_VIEW_TYPE == ARTICLE_RECYCLER) {
            if (homeMoment.imageUrl != null) {
                Picasso.with(context).cancelRequest(holder.momentImageIv);
                int height = AppLibrary.convertDpToPixels(context, 150);
                Picasso.with(context).load(homeMoment.imageUrl).resize(width, height).config(Bitmap.Config.RGB_565).centerCrop().into(holder.momentImageIv);
            }
        } else if (MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER) {
            if (homeMoment.showDisplayPicture) {
                int height = AppLibrary.convertDpToPixels(context, 94);
                Picasso.with(context).cancelRequest(holder.momentImageIv);
                if (homeMoment.thumbnailUrl != null) {
                    Picasso.with(context).load(homeMoment.thumbnailUrl).resize(width, height).config(Bitmap.Config.RGB_565).centerCrop().into(holder.momentImageIv);
                } else if (homeMoment.imageUrl != null) {
                    Picasso.with(context).load(homeMoment.imageUrl).resize(width, height).config(Bitmap.Config.RGB_565).centerCrop().into(holder.momentImageIv);
                }
            }
            String fullString = homeMoment.name;
            if (fullString.contains(" ")) {
                holder.momentNameTv.setMaxLines(8);
            } else holder.momentNameTv.setMaxLines(1);

        }

        Log.d(TAG, "handleNearbyRecycler: status of " + homeMoment.name + " "+DynamicDownloader.getMomentStatusString(homeMoment.momentStatus));
        switch (homeMoment.momentStatus) {
            case UNSEEN_MOMENT:
                holder.toggleSeenTick(false);
                holder.stopLoader();
                break;
            case DOWNLOADING_MOMENT:
            case SEEN_BUT_DOWNLOADING:
                holder.toggleSeenTick(false);
                holder.startLoader();
                break;
//            case READY_TO_VIEW_MOMENT:
//                holder.toggleSeenTick(false);
//                holder.stopLoader();
//                break;
//            case READY_AND_SEEN_MOMENT:
//                break;
            case SEEN_MOMENT:
                holder.toggleSeenTick(true);
                holder.stopLoader();
                break;
        }
    }


    @SuppressWarnings("FieldCanBeLocal")
    private int MAX_HOLDERS = 5;// represents maximum holders in a view at once (will appear as n + 0.5) so that user know its scrollable
    private int deviceWidth;

    private int refreshWidthOfHolder(NearbyHolder nearbyHolder, int position) {
        if (deviceWidth == 0) {
            deviceWidth = AppLibrary.getDeviceWidth((Activity) context);
        }
        int currentStrength = this.momentViewModels.size();
        int margin = (AppLibrary.convertDpToPixels(context, 2)) / 2;

        if (margin == 0)
            margin = 1;
        //handling  for low Definition devices

        if (currentStrength == MAX_HOLDERS)
            nearbyHolder.rootView.getLayoutParams().width = deviceWidth / MAX_HOLDERS;
        else if (currentStrength > MAX_HOLDERS) {
            nearbyHolder.rootView.getLayoutParams().width = (int) (deviceWidth / (MAX_HOLDERS + 0.3));
        } else
            nearbyHolder.rootView.getLayoutParams().width = deviceWidth / currentStrength;


        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) nearbyHolder.rootView.getLayoutParams();
        if (position == 0) {
            if (currentStrength > 1)
                nearbyHolder.rootView.setPadding(0, 0, margin, 0);
            else
                nearbyHolder.rootView.setPadding(0, 0, 0, 0);

        } else if (position == currentStrength - 1) {
            nearbyHolder.rootView.setPadding(margin, 0, 0, 0);
        } else {
            nearbyHolder.rootView.setPadding(margin, 0, margin, 0);
        }

        return layoutParams.width;
    }

    private void handleUnSeenRecycler(ViewHolder holder, HomeMomentViewModel homeMoment) {
        Transformation t;
        holder.nameTv.setText(AppLibrary.getFirstName(homeMoment.name));
        if (getDisplayMomentStatusForUnseenHolder(homeMoment.momentId, homeMoment.momentStatus) == UNSEEN_MOMENT) {
            AppLibrary.log_d(TAG, "In Unseen Moment");
            holder.profilePicIv.setAlpha(1f);
            holder.circularLoader.setAlpha(1f);
            holder.circularLoader.setVisibility(View.VISIBLE);
            drawTimer(holder, homeMoment.angle, true);
            if (!homeMoment.showDisplayPicture)
                return;
            if (AppLibrary.checkStringObject(homeMoment.thumbnailUrl) != null) {
                Picasso.with(context).load(homeMoment.thumbnailUrl).
                        fit().centerCrop().transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            } else if (AppLibrary.checkStringObject(homeMoment.imageUrl) != null) {
                Picasso.with(context).load(homeMoment.imageUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            } else {
                t = new LetterTileRoundedTransformation(context, homeMoment.momentId);
                Picasso.with(context).load(homeMoment.imageUrl).
                        fit().centerCrop().transform(t).into(holder.profilePicIv);
            }
        } else if (getDisplayMomentStatusForUnseenHolder(homeMoment.momentId, homeMoment.momentStatus) == SEEN_MOMENT) {
            AppLibrary.log_d(TAG, "In Seen Moment");
            drawTimer(holder, homeMoment.angle, true);
            holder.circularLoader.setAlpha(0.38f);
            holder.profilePicIv.setAlpha(0.38f);
            if (AppLibrary.checkStringObject(homeMoment.thumbnailUrl) != null) {
                Picasso.with(context).load(homeMoment.thumbnailUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            } else {
                Picasso.with(context).load(homeMoment.imageUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            }
        } else if (getDisplayMomentStatusForUnseenHolder(homeMoment.momentId, homeMoment.momentStatus) == READY_TO_VIEW_MOMENT) {
            AppLibrary.log_d(TAG, "In ReadyToView Moment");
            holder.profilePicIv.setAlpha(1f);
            holder.circularLoader.setAlpha(1f);
            drawTimer(holder, homeMoment.angle, false);
            if (AppLibrary.checkStringObject(homeMoment.thumbnailUrl) != null) {
                Picasso.with(context).load(homeMoment.thumbnailUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            } else {
                Picasso.with(context).load(homeMoment.imageUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            }
        } else if (getDisplayMomentStatusForUnseenHolder(homeMoment.momentId, homeMoment.momentStatus) == DOWNLOADING_MOMENT) {
            AppLibrary.log_d(TAG, "In Downloading Moment");
            holder.profilePicIv.setAlpha(1f);
            holder.circularLoader.setAlpha(1f);
            showLoader(holder);
            if (AppLibrary.checkStringObject(homeMoment.thumbnailUrl) != null) {
                Picasso.with(context).load(homeMoment.thumbnailUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            } else {
                Picasso.with(context).load(homeMoment.imageUrl).fit().centerCrop().
                        transform(new RoundedTransformation(homeMoment.momentId)).into(holder.profilePicIv);
            }
        }
    }

    private DynamicDownloader downloader;

    public DynamicDownloader getDownloader() {
        if (downloader == null) {
            downloader = DynamicDownloader.getInstance(context);
        }
        return downloader;
    }

    private int getDisplayMomentStatusForUnseenHolder(String momentId, int actualMomentState) {
        switch (actualMomentState) {
            case UNSEEN_MOMENT:
                final boolean minimumMediasPresentToOpenMoment = getDownloader().isMinimumMediasPresentToOpenMoment(momentId);
                if (!minimumMediasPresentToOpenMoment)
                    return UNSEEN_MOMENT;
                else return READY_TO_VIEW_MOMENT;

            case DOWNLOADING_MOMENT:
                return DOWNLOADING_MOMENT;
            case SEEN_MOMENT:
                return SEEN_MOMENT;
        }
        return -1;
    }

    private static int getImageDiameterForMoment(int MOMENT_VIEW_TYPE) {
        int diameter;
        switch (MOMENT_VIEW_TYPE) {
            case SEEN_FOLLOWER_MOMENT_RECYCLER:
            case SEEN_FRIEND_MOMENT_RECYCLER:
                diameter = 56;
                break;
            case UNSEEN_FOLLOWER_MOMENT_RECYCLER:
            case UNSEEN_FRIEND_MOMENT_RECYCLER:
                diameter = 88;
                break;
            case FAVOURITE_MOMENT_RECYCLER:
                diameter = 90;
                break;
            case AROUND_YOU_MOMENT_RECYCLER:
                diameter = 56;
                break;
            default:
                diameter = 70;
                break;
        }
        return diameter;
    }

    private void showLoader(ViewHolder holder) {
        int diameter = AppLibrary.convertDpToPixels(context, getImageDiameterForMoment(MOMENT_VIEW_TYPE)) + AppLibrary.convertDpToPixels(context, 10);
        holder.circularLoader.getLayoutParams().height = diameter;
        holder.circularLoader.getLayoutParams().width = diameter;
        holder.circularLoader.setSpinBarColor(Color.parseColor("#9900FF"));
        holder.circularLoader.setSpinSpeed(5);
        holder.circularLoader.setSpinningBarLength(180);
        holder.circularLoader.setShowTextWhileSpinning(false);
        holder.circularLoader.setBarWidth(AppLibrary.convertDpToPixels(context, 1));
        holder.circularLoader.setValue(0);
        holder.circularLoader.setBarColor(context.getResources().getColor(R.color.transparent));
        holder.circularLoader.spin();
        if (holder.circularLoader.getVisibility() == View.GONE)
            holder.circularLoader.setVisibility(View.VISIBLE);
    }

    private void drawTimer(ViewHolder holder, float angle, boolean defaultColor) {
//        if (holder.circularLoader != null && (int) angle != (270 - holder.circularLoader.getStartAngle())) {
        holder.circularLoader.stopSpinning();
        if (defaultColor) {
            holder.circularLoader.setBarColor(Color.argb(97, 0, 0, 0)); //38% opacity
        } else {
            holder.circularLoader.setBarColor(Color.parseColor("#9900FF"));
        }
        int diameter = AppLibrary.convertDpToPixels(context, getImageDiameterForMoment(MOMENT_VIEW_TYPE)) + AppLibrary.convertDpToPixels(context, 10);
        holder.circularLoader.getLayoutParams().height = diameter;
        holder.circularLoader.getLayoutParams().width = diameter;
        holder.circularLoader.setText("");
        holder.circularLoader.setTextSize(0);
        if (angle > 350)
            angle = 350;
        holder.circularLoader.setStartAngle((int) (270 - angle));
        holder.circularLoader.setBarWidth(AppLibrary.convertDpToPixels(context, 1));
        holder.circularLoader.setValue(angle);
        if (holder.circularLoader.getVisibility() == View.GONE)
            holder.circularLoader.setVisibility(View.VISIBLE);
//        }
    }

    @Override
    public int getItemCount() {
        if (this.MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER) {
            return momentViewModels == null || momentViewModels.size() == 0 ? 0 : momentViewModels.size();
        }
        return momentViewModels == null || momentViewModels.size() == 0 ? 0 : momentViewModels.size();
    }

    @Override
    public void onButtonClicked(View view, String userId, Dialog popup) {
        switch (view.getId()) {
            case R.id.shareImageView:
                break;
            case R.id.followlv:
                boolean following = (boolean) view.getTag();
                if (following) {

                } else {

                }
                ((CameraActivity) context).updateFollowersList(!following);
                break;
        }
    }

    @Override
    public void onPopupDismiss() {

    }

    private class PublicMomentHolder extends RecyclerView.ViewHolder {
        public PublicMomentHolder(View itemView) {
            super(itemView);
        }
    }


    private class ArticleMomentHolder extends RecyclerView.ViewHolder {
        public ArticleMomentHolder(View itemView) {
            super(itemView);
        }
    }

    private class NearbyHolder extends RecyclerView.ViewHolder {

        ImageView momentImageIv;
        TextView momentNameTv;
        RelativeLayout rootView;
        ImageView progressBarIv;
        ImageView seenTickIv;

        NearbyHolder(View itemView) {
            super(itemView);
            rootView = (RelativeLayout) itemView;
            momentImageIv = (ImageView) itemView.findViewById(R.id.momentImageIV);
            momentNameTv = (TextView) itemView.findViewById(R.id.momentNameTV);
            momentNameTv.setTypeface(FontPicker.getInstance(context).getMontserratRegular());
            momentNameTv.setShadowLayer(2, 1, 1, Color.parseColor("#000000"));

            progressBarIv = (ImageView) rootView.findViewById(R.id.progressBarLoader);
            progressBarIv.setImageResource(R.drawable.loader);
            progressBarIv.setVisibility(View.GONE);

            seenTickIv = ((ImageView) rootView.findViewById(R.id.seenTickIV));
            seenTickIv.setImageResource(R.drawable.stream_viewed_svg);
            seenTickIv.setVisibility(View.GONE);

            if (MOMENT_VIEW_TYPE == ARTICLE_RECYCLER) {
                momentNameTv.setGravity(Gravity.START);
                momentImageIv.getLayoutParams().height = AppLibrary.convertDpToPixels(context, 150);
                momentNameTv.setBackground(context.getResources().getDrawable(R.drawable.dashboard_name_gradient));
            } else {//around you
                momentNameTv.setBackground(context.getResources().getDrawable(R.drawable.dashboard_name_gradient));
            }

        }

        private RotateAnimation rotate;

        private void initAnimation() {
            rotate = new RotateAnimation(0, 359, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(1000);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.setRepeatCount(Animation.INFINITE);
        }

        private void startLoader() {
            if (rotate == null) {
                initAnimation();
            }
            this.progressBarIv.startAnimation(rotate);
            this.progressBarIv.setVisibility(View.VISIBLE);
        }

        private void stopLoader() {
            this.progressBarIv.setVisibility(View.GONE);
            if (rotate != null) {
                rotate.cancel();
                rotate.reset();
                rotate = null;
            }
        }


        private void toggleSeenTick(boolean isSeen) {
            this.seenTickIv.setVisibility(isSeen ? View.VISIBLE : View.GONE);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePicIv, timerIv;
        TextView nameTv, timeTv;
        ImageView ripple1, ripple2;
        LinearLayout holder;
        CircleProgressView circularLoader;
        FrameLayout imagesContainerFrame;

        public ViewHolder(View itemView) {
            super(itemView);
            holder = (LinearLayout) itemView.findViewById(R.id.holder);
            imagesContainerFrame = (FrameLayout) itemView.findViewById(R.id.imagesContainerFrame);
            profilePicIv = (ImageView) itemView.findViewById(R.id.profileIV);
            timerIv = (ImageView) itemView.findViewById(R.id.timerIV);
            nameTv = (TextView) itemView.findViewById(R.id.nameTV);
            timeTv = (TextView) itemView.findViewById(R.id.updatedAtTV);
            ripple1 = (ImageView) itemView.findViewById(R.id.rippleIv1);
            ripple2 = (ImageView) itemView.findViewById(R.id.rippleIv2);
//            loader = (ProgressBar) itemView.findViewById(R.id.loader);
            circularLoader = (CircleProgressView) itemView.findViewById(R.id.circular_progress_bar);
//            loader.getIndeterminateDrawable().setColorFilter(context.getResources().getColor(R.color.white_transparent), android.graphics.PorterDuff.Mode.SRC_IN);

            int diameter = AppLibrary.convertDpToPixels(context, getImageDiameterForMoment(MOMENT_VIEW_TYPE));


//            diameter = AppLibrary.convertDpToPixels(context, diameter);
            profilePicIv.getLayoutParams().height = diameter;
            profilePicIv.getLayoutParams().width = diameter;

            if (MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER) {
                timeTv.setVisibility(View.GONE);
                timerIv.setVisibility(View.GONE);
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                int padding_3 = AppLibrary.convertDpToPixels(context, 3);
                imagesContainerFrame.setPadding(padding_3, 0, padding_3, 0);
                nameTv.setMaxLines(2);
                imagesContainerFrame.getLayoutParams().width = (int) (diameter * 1.3);
            } else if (MOMENT_VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER) {
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                nameTv.setPadding(0, AppLibrary.convertDpToPixels(context, 4), 0, 0);
//                loader.setVisibility(View.GONE);
                timerIv.setVisibility(View.GONE);
                int padding_12 = AppLibrary.convertDpToPixels(context, 12);
                itemView.setPadding(0, 0, 0, padding_12);
            } else if (MOMENT_VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER) {
                int padding_7 = AppLibrary.convertDpToPixels(context, 7);
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                timerIv.setVisibility(View.GONE);
                imagesContainerFrame.setPadding(padding_7, 0, padding_7, 0);
                itemView.setPadding(0, 0, 0, AppLibrary.convertDpToPixels(context, 12));
            }
        }
    }


    private class PaddingHolder extends RecyclerView.ViewHolder {

        PaddingHolder(View itemView) {
            super(itemView);
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "Single Tap " + "Tapped at: (" + e.getX() + "," + e.getY() + ")");
            clickedMoment.clickType = HomeMomentViewModel.ClickType.SINGLE_TAP;
            clickedMoment.momentType = MOMENT_VIEW_TYPE;
            if (recyclerViewClickInterface != null)
                recyclerViewClickInterface.onItemClick(0, clickedMoment);

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "Double Tap " + "Tapped at: (" + e.getX() + "," + e.getY() + ")");
            clickedMoment.clickType = HomeMomentViewModel.ClickType.DOUBLE_TAP;
            if (recyclerViewClickInterface != null)
                recyclerViewClickInterface.onItemClick(0, clickedMoment);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            clickedMoment.clickType = HomeMomentViewModel.ClickType.LONG_PRESS;
            if (MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == ARTICLE_RECYCLER) {
                if (recyclerViewClickInterface != null)
                    recyclerViewClickInterface.onItemClick(0, clickedMoment);
            } else if (MOMENT_VIEW_TYPE == UNSEEN_FRIEND_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == SEEN_FRIEND_MOMENT_RECYCLER) {
                BaseFragment.getBaseFragmentInstance().showGenericProfilePopup(context, clickedMoment.name, clickedMoment.imageUrl, clickedMoment.handle);
            } else if (MOMENT_VIEW_TYPE == UNSEEN_FOLLOWER_MOMENT_RECYCLER || MOMENT_VIEW_TYPE == SEEN_FOLLOWER_MOMENT_RECYCLER) {
                BaseFragment.getBaseFragmentInstance().setFollowPopupListener(HomeMomentAdapter.this);
                BaseFragment.getBaseFragmentInstance().showFollowPopup(0, context, clickedMoment.userId, clickedMoment.name, clickedMoment.imageUrl,
                        clickedMoment.handle, true, false);
            }
            super.onLongPress(e);
        }
    }

    public void safeNotifyItemInserted(final int index) {
        if (recyclerView == null) {
            super.notifyItemInserted(index);
        } else {
            try {
                if (recyclerView.isComputingLayout())
                    throw new IllegalStateException();
                super.notifyItemInserted(index);
            } catch (Exception e) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        safeNotifyItemInserted(index);
                    }
                }, 100);
            }
        }
    }

    public void safeNotifyItemRemoved(final int index) {
        if (recyclerView == null) {
            super.notifyItemRemoved(index);
        } else {
            try {
                if (recyclerView.isComputingLayout())
                    throw new IllegalStateException();
                super.notifyItemRemoved(index);
            } catch (Exception e) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        safeNotifyItemRemoved(index);
                    }
                }, 100);
            }
        }
    }

    public void safeNotifyDataSetChanged() {
        if (recyclerView == null) {
            super.notifyDataSetChanged();
        } else {
            try {
                if (recyclerView.isComputingLayout())
                    throw new IllegalStateException();
                super.notifyDataSetChanged();
            } catch (Exception e) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        safeNotifyDataSetChanged();
                    }
                }, 100);
            }
        }
    }

    public void safeNotifyItemChanged(final int index) {
        if (recyclerView == null) {
            super.notifyItemChanged(index);
        } else {
            try {
                if (recyclerView.isComputingLayout())
                    throw new IllegalStateException();
                super.notifyItemChanged(index);
            } catch (Exception e) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        safeNotifyItemChanged(index);
                    }
                }, 100);
            }
        }
    }

    private final int MAX_ITEMS_IN_ARTICLES = 4, MAX_ITEMS_IN_NEARBY = 4;

    public boolean canScrollHorizontally() {
        if (this.MOMENT_VIEW_TYPE == ARTICLE_RECYCLER && momentViewModels != null && momentViewModels.size() == MAX_ITEMS_IN_ARTICLES)
            return false;
        else if (this.MOMENT_VIEW_TYPE == AROUND_YOU_MOMENT_RECYCLER && momentViewModels != null && momentViewModels.size() == MAX_ITEMS_IN_NEARBY)
            return false;
        return true;

    }
}