package com.pulseapp.android.stickers;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.pulseapp.android.R;
import com.pulseapp.android.downloader.TemplateDownloader;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.models.LocationTemplateModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.squareup.picasso.Picasso;

import java.io.File;

/**
 * Created by deepankur on 07-12-2015.
 */
public class ChildStickerFragment extends BaseFragment {

    private int bundleNumber;
    private int mCurrentPage;
    private View rootView;
    private static final String TAG = "ChildStickerFragment";

    private static int templateCount = ParentStickerFragment.PAGE_COUNT;
    private static int sDeviceWidth, sDeviceHeight;
    private LocationTemplateModel templateModel;
    private ProgressBar progressBar;
    boolean isThisClearScreen;
    private GestureDetector mGestureListener;

    void initFragments(Activity context, Resources resources) {
        int[] metrics = AppLibrary.getDeviceParams(context);
        sDeviceWidth = metrics[0];
        sDeviceHeight = metrics[1];
    }


    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState!=null) return;

        initFragments(getActivity(), getResources());
        Bundle data = getArguments();
        bundleNumber = data.getInt("bg", 0);
        mCurrentPage = bundleNumber < templateCount ? bundleNumber : bundleNumber % templateCount;
        if (mCurrentPage == ParentStickerFragment.PAGE_COUNT - 1)
            isThisClearScreen = true;
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_child_sticker, container, false);

        if (savedInstanceState!=null) return rootView;

        this.templateModel = TemplateDownloader.getInstance(context).getLocationModelByIndex(mCurrentPage);
        this.progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        this.progressBar.getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);


        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState!=null) return;
        //  if (mGestureListener == null)
        mGestureListener = new GestureDetector(context, new CustomGestureListener());
        this.rootView.findViewById(R.id.stickersLayoutRL).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureListener.onTouchEvent(event);
                //Log.d(TAG, " on touch Rl");
                return true;
            }
        });

        if (isThisClearScreen)
            progressBar.setVisibility(View.GONE);
        else
            addStickerToFragment(this.rootView);
    }


    void refreshData() {
        if (isThisClearScreen)
            progressBar.setVisibility(View.GONE);
        else if (this.rootView != null)
            addStickerToFragment(this.rootView);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    private void addStickerToFragment(View rootView) {
        RelativeLayout relativeLayout = (RelativeLayout) rootView.findViewById(R.id.stickersLayoutRL);
        relativeLayout.removeAllViews();

//        int orientation = getResources().getConfiguration().orientation;
        int width = sDeviceWidth;
        int height = sDeviceHeight;
        if (templateModel == null) {
            Log.d(TAG, " empty template returning");
            return;
        }
        int size = templateModel.stickers.size();

        for (int i = 0; i < size; i++) {
            LocationTemplateModel.LocationSticker asset = templateModel.getStickerByIndex(i);
            if (asset.localUri == null) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, " no image URI for a sticker returning ");
                return;
            }
        }
        for (int i = 0; i < size; i++) {
            LocationTemplateModel.LocationSticker asset = templateModel.getStickerByIndex(i);
            if (asset.localUri == null) {
                Log.d(TAG, " no Uri ");
            }
            final ImageView imageView = new ImageView(context);
            // imageView.setImageBitmap(asset.mBitmap);
            imageView.setRotation(asset.degree);
            imageView.setTag(asset.mStickerId);
            if (asset.width >= 0) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            imageView.setAdjustViewBounds(true);
         /*   imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    //imageView's touch listener consumed here
                    return true;
                }
            });*/
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            lp.width = (int) (asset.width * width);
            if (asset.height >= 0) lp.height = (int) (asset.height * height /** 0.5625*/);

            if (asset.width >= 0) lp.width = (int) (asset.width * width);
//            if(asset.mHeight >= 0) lp.height = (int) (asset.mHeight * height);

            lp.setMargins((int) (asset.marginLeft * width), (int) (asset.marginTop * height), 0, 0);
            relativeLayout.addView(imageView, lp);
            if (asset.width != 0 || asset.height != 0)
                Picasso.with(context).load(new File(asset.localUri)).resize((int) (asset.width * width), (int) (asset.height * height)).
                        into(imageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "onSuccess");
                                progressBar.setVisibility(View.GONE);
                                imageView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onError() {
                                Log.d(TAG, "onError");
                                progressBar.setVisibility(View.VISIBLE);
                                imageView.setVisibility(View.GONE);
                            }
                        });
        }
    }

    public int getProgressBarVisibility() {
        if (this.progressBar == null)
            return -1;
        return progressBar.getVisibility();
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();
            Log.d(TAG, "onSingleTapConfirmed at " + x + " " + y);
            if (progressBar.getVisibility() == View.VISIBLE) {
                Log.d(TAG, " exit CameraMode + view Pager visibility gone " + progressBar.getVisibility());
                ((VideoEditorFragment) getParentFragment().getParentFragment()).exitLocationModeOnSingleTap(true);
            } else {
                Log.d(TAG, " just exit CameraMode " + progressBar.getVisibility());
                ((VideoEditorFragment) getParentFragment().getParentFragment()).exitLocationModeOnSingleTap(false);
            }
            return super.onSingleTapConfirmed(e);
        }
    }
}

