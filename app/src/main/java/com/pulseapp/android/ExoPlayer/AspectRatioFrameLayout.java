package com.pulseapp.android.ExoPlayer;

/**
 * Created by abc on 10/13/2015.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.pulseapp.android.util.RecordedVideoController;

public class AspectRatioFrameLayout extends FrameLayout{
    /**
     * The {@link FrameLayout} will not resize itself if the fractional difference between its natural
     * aspect ratio and the requested aspect ratio falls below this threshold.
     * <p>
     * This tolerance allows the view to occupy the whole of the screen when the requested aspect
     * ratio is very close, but not exactly equal to, the aspect ratio of the screen. This may reduce
     * the number of view layers that need to be composited by the underlying system, which can help
     * to reduce power consumption.
     */
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;

    private float videoAspectRatio;

    private Context mContext;
    private int deviceWidth;
    private int deviceHeight;

    public AspectRatioFrameLayout(Context context) {
        super(context);
        mContext = context;
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Set the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    public void setDeviceParameters(int width,int height){
        deviceWidth = width;
        deviceHeight = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio == 0) {
            // Aspect ratio not set.
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance.
            return;
        }

//        boolean isVideoPortrait = ((YoutubePlayerActivity)mContext).isVideoPortait();
//        boolean isDisplayPortrait = ((YoutubePlayerActivity)mContext).isDisplayPortrait();
//        boolean isFromBroadcast = ((YoutubePlayerActivity)mContext).isComingFromBroadcast();
//        boolean isSquareVideo =  ((YoutubePlayerActivity)mContext).isVideoSquare();
        boolean isVideoPortrait = true;
        boolean isDisplayPortrait = true;
        boolean isFromBroadcast = false;
        boolean isSquareVideo = false;
        if(isFromBroadcast){
            if ((!isVideoPortrait ^ isDisplayPortrait) && RecordedVideoController.getInstance().isFullScreen) {
                if (aspectDeformation < 0) {
                    if (isSquareVideo) {
                        height = (int) (deviceWidth);
                        width = (int) (height * videoAspectRatio);
                    } else {
                        height = (int) (width / videoAspectRatio);
                    }
                } else {
                    if (isSquareVideo) {
                        width = (int) (deviceHeight);
                        height = (int) (width / videoAspectRatio);
                    } else {
                        width = (int) (height * videoAspectRatio);
                    }
                }
            } else {
                if (aspectDeformation > 0) {
                    if (isSquareVideo) {
                        height = (int) (deviceWidth);
                        width = (int) (height * videoAspectRatio);
                    } else {
                        height = (int) (width / videoAspectRatio);
                    }
                } else {
                    if (isSquareVideo) {
                        width = (int) (deviceHeight);
                        height = (int) (width / videoAspectRatio);
                    } else {
                        width = (int) (height * videoAspectRatio);
                    }
                }
            }
        } else {
            if ((!isVideoPortrait ^ isDisplayPortrait)) {
                if (aspectDeformation < 0) {
                    if (isSquareVideo) {
                        height = (int) (deviceWidth);
                        width = (int) (height * videoAspectRatio);
                    } else {
                        height = (int) (width / videoAspectRatio);
                    }
                } else {
                    if (isSquareVideo) {
                        width = (int) (deviceHeight);
                        height = (int) (width / videoAspectRatio);
                    } else {
                        width = (int) (height * videoAspectRatio);
                    }
                }
            } else {
                if (aspectDeformation > 0) {
                    if (isSquareVideo) {
                        height = (int) (deviceWidth);
                        width = (int) (height * videoAspectRatio);
                    } else {
                        height = (int) (width / videoAspectRatio);
                    }
                } else {
                    if (isSquareVideo) {
                        width = (int) (deviceHeight);
                        height = (int) (width / videoAspectRatio);
                    } else {
                        width = (int) (height * videoAspectRatio);
                    }
                }
            }
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
