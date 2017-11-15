package com.pulseapp.android.stickers;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.analytics.AnalyticsEvents;
import com.pulseapp.android.analytics.AnalyticsManager;
import com.pulseapp.android.explosion.ExplosionField;
import com.pulseapp.android.fragments.BaseFragment;
import com.pulseapp.android.fragments.VideoEditorFragment;
import com.pulseapp.android.models.StickerModel;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.NoMenuEditText;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;


/**
 * Created by deepankur on 15/2/16.
 */

public class EmoticonFragment extends BaseFragment implements ScaleGestureDetector.OnScaleGestureListener {
    String TAG = "EmoticonFragment";

    private final boolean VERBOSE = false;
    View ViewToDrag, imageViewToScale;
    float XOffset = 0;
    float YOffset = 0;
    RelativeLayout containerRL;
    boolean inScale;
    float P1finalX, P1finalY;
    float[] xyCoordinates = new float[2];
    float P2finalX, P2finalY;
    Float startingAngle = null;
    float normalizedAngle;
    float currentAngularDisplacement;
    boolean shouldSwapPoints = false;
    float recordedImageViewRotation;
    MotionEvent motionEvent;
    ImageView deleteImageView;
    boolean isDeleteIntended;
    int deleteColor = Color.parseColor("#ff0000");
    int normalColor = Color.parseColor("#00000000");
    boolean mViewLockedToDrag;
    int moveCount = 0; //To track tiny movements
    public boolean textInFocus = false;
    MotionEvent mDragMotionEvent;
    private ScaleGestureDetector gestureScale;
    private GestureDetector tapGestureDetector;
    private GestureDetector swipeGestureDetector;
    private boolean DragOnText = false;
    private float scaleFactor = 1;
    private float absoluteX, absoluteY;
    private EditText editText;
    private InputMethodManager imm;
    private boolean defaultTextState = true;
    LayoutParams[] paramsArray = new LayoutParams[2];
    private View rootView;
    private int statusBarOffset;
    private Handler keyboardHandler = new Handler();
    private SwipeListener mSwipeEventListener;
    private Toast customToastTut;

    public boolean setTouchEvent(final MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP) { //Unlock drag and possibly delete item

            if (isDeleteIntended && ViewToDrag != null && !(ViewToDrag instanceof EditText)) {
                isDeleteIntended = false;
                if (ViewToDrag instanceof ImageView)
                    performExplodeAnimation();
            }
            mViewLockedToDrag = false;
            deleteImageView.setVisibility(View.GONE);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) { //Reset everything before we start deciding anything

            mViewLockedToDrag = false;
            moveCount = 0;

            if (inScale)
                inScale = false;

            if (startingAngle != null)
                startingAngle = null;

        /*    if(recordedImageViewRotation!=-3000)
                recordedImageViewRotation = -3000;*/
        }

        if (event.getPointerCount() == 2 && !textInFocus) { //Scaling or rotating a particular item
            motionEvent = event;

            ViewToDrag = null;
            deleteImageView.setVisibility(View.GONE);

            if (imageViewToScale == null) {
                if (findNearestTarget()) {//found something ? reset the 2 fields once n only once;( calculateRotation(); takes care of the rest)
                    shouldSwapPoints = (event.getX(0) > event.getX(1));
                    recordedImageViewRotation = -imageViewToScale.getRotation();
                }
            }

            if (imageViewToScale != null) { //Found an imageview to scale

             /*   if(recordedImageViewRotation==-3000)
                    recordedImageViewRotation = -imageViewToScale.getRotation();*/

                calculateRotation(motionEvent);
                imageViewToScale.setRotation(-(currentAngularDisplacement + recordedImageViewRotation));

                gestureScale.onTouchEvent(event);
                return true;
            }
        }

        if (mViewLockedToDrag && ViewToDrag != null && !textInFocus && event.getAction() == MotionEvent.ACTION_MOVE) {  //Dragging a locked item
            mDragMotionEvent = event;
            startTranslation();
            if (ViewToDrag instanceof ImageView)
                checkRecycleBinDistance();
            else {
                moveCount++;
                if (moveCount > 2) //To handle tiny movements
                    DragOnText = true;
            }

            return true;
        }

        if (!inScale && !mViewLockedToDrag && !textInFocus) { //text not in focus loop

            ViewToDrag = findViewToDrag(event.getX(0), event.getY(0));
            mViewLockedToDrag = ViewToDrag != null;

            if (ViewToDrag != null) {
                XOffset = event.getX();
                YOffset = event.getY();
            } else if (editText == null) { //Tap on an empty area of the screen when there is no text box
                tapGestureDetector.onTouchEvent(event);
            }

        } else if (editText != null && textInFocus && editText.hasFocus()) { //If text in focus

            Rect outRect = new Rect();
            editText.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) { //Dismiss if clicked out edit text
                textInFocus = false;
                Log.d(TAG, "Moving view back to center with action :" + getActionName(event.getAction()));
                moveViewBackFromCenter(editText);
                return true;
            } else { //Do more finer editing
                event.setLocation(event.getX(), event.getY() - editText.getY()); //Refactor the touch event with respect to its own view before passing touch event
                editText.onTouchEvent(event);
                return true;
            }
        }

        if (ViewToDrag != null && ViewToDrag instanceof EditText && event.getPointerCount() == 1) { //Tap on the edit text

            if (event.getAction() == MotionEvent.ACTION_DOWN)
                DragOnText = false;

            Log.d(TAG, "Drag on Text value :" + DragOnText + " event is :" + getActionName(event.getAction()));

            tapGestureDetector.onTouchEvent(event);
        }

        if (ViewToDrag == null && !textInFocus && imageViewToScale == null) {
            swipeGestureDetector.onTouchEvent(event);
        }

        return true;
    }


    private class SwipeGestureListener implements GestureDetector.OnGestureListener {

        private final int SWIPE_THRESHOLD = 120;
        private final int SWIPE_VELOCITY_THRESHOLD = 240;

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            result = true;
                            onSwipeLeft();
                        }
                    }
                } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                }

            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, " replace try catch " + exception);
            }
            return result;
        }

        public void onSwipeRight() {
            Log.d(TAG, " on SwipeRight");
            // Go to next filter
            mSwipeEventListener.onSwipeRight();
        }

        public void onSwipeLeft() {
            Log.d(TAG, " on SwipeLeft");
            // Go to previous filter
            mSwipeEventListener.onSwipeLeft();
        }

        public void onSwipeTop() {
            Log.d(TAG, " on SwipeTop");
        }

        public void onSwipeBottom() {
            Log.d(TAG, " on SwipeBottom");
        }
    }


    public boolean canHandleTouch() {
        boolean canHandle = true;
        if (imageViewToScale == null && ViewToDrag == null)
            canHandle = false;
        Log.d(TAG, " can Handle ? " + canHandle);
        return (canHandle);
    }

    private void startTranslation() {
        LayoutParams layoutParams = (LayoutParams) ViewToDrag.getLayoutParams();

        Log.d(TAG, " getting location x " + mDragMotionEvent.getX() + " y " + mDragMotionEvent.getY());

        if (ViewToDrag instanceof ImageView) {
            deleteImageView.setVisibility(View.VISIBLE);
            layoutParams.leftMargin += (int) (mDragMotionEvent.getX() - XOffset);
            layoutParams.topMargin += (int) (mDragMotionEvent.getY() - YOffset);
            XOffset = mDragMotionEvent.getX();
            YOffset = mDragMotionEvent.getY();
        } else if (ViewToDrag instanceof EditText && !defaultTextState) {
            layoutParams.leftMargin += (int) (mDragMotionEvent.getX() - XOffset);
            layoutParams.topMargin += (int) (mDragMotionEvent.getY() - YOffset);
            XOffset = mDragMotionEvent.getX();
            YOffset = mDragMotionEvent.getY();
        } else {
            layoutParams.topMargin += (int) (mDragMotionEvent.getY() - YOffset);
            YOffset = mDragMotionEvent.getY();
        }

        Log.d(TAG, " Layout params while dragging. Left: " + layoutParams.leftMargin + " top: " + layoutParams.topMargin);

        ViewToDrag.setLayoutParams(layoutParams);
    }

    private void checkRecycleBinDistance() {
        float realcenterX = deleteImageView.getX() + (deleteImageView.getMeasuredWidth() / 2);
        float realcenterY = deleteImageView.getY() + (deleteImageView.getMeasuredHeight() / 2);
        float realWidth = deleteImageView.getMeasuredWidth() * 1.3f; //To slightly increase the delete area
        float realHeight = deleteImageView.getMeasuredHeight() * 1.3f; //To slightly increase the delete area

        RectF rectF = new RectF(realcenterX - realWidth / 2, realcenterY - realHeight / 2,
                realcenterX + realWidth / 2, realcenterY + realHeight / 2);

        if (rectF.contains(mDragMotionEvent.getRawX(), mDragMotionEvent.getRawY()))
            isDeleteIntended = true;
        else
            isDeleteIntended = false;

        resizeRecycleBin(isDeleteIntended);
    }

    private void resizeRecycleBin(boolean shouldExpandRecycleBin) {
        ViewToDrag.setAlpha(shouldExpandRecycleBin ? 0.4f : 1f);//user won't know that sticker would be deleted in case
        //the its  too big and hence hides the recycle bin itself
        deleteImageView.setScaleX(shouldExpandRecycleBin ? 1.3f : 1);
        deleteImageView.setScaleY(shouldExpandRecycleBin ? 1.3f : 1);
    }

    private void performExplodeAnimation() {
        final View viewToDelete = ViewToDrag;
        ExplosionField mExplosionField;
        mExplosionField = ExplosionField.attach2Window(getActivity());
        mExplosionField.explode(viewToDelete);

        containerRL.removeView(viewToDelete);
    }

    private void performDeleteAnimation() {
        final View viewToDelete = ViewToDrag;
        resizeRecycleBin(false);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(viewToDelete, "scaleX", 0.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(viewToDelete, "scaleY", 0.0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(viewToDelete, "rotation", 360);
        scaleDownX.setDuration(300);
        scaleDownY.setDuration(300);
        rotate.setDuration(300);
        AnimatorSet deleteAnimationSet = new AnimatorSet();
        deleteAnimationSet.play(scaleDownX).with(scaleDownY).with(rotate);
        deleteAnimationSet.start();
        deleteAnimationSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                containerRL.removeView(viewToDelete);
                deleteImageView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    int deviceWidthPixels, deviceHeightPixels;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) return;

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        int params[] = AppLibrary.getDeviceParams((CameraActivity) context);
        deviceWidthPixels = params[0];
        deviceHeightPixels = params[1];
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dragdrop, container, false);

        if (savedInstanceState != null) return rootView;

        deleteImageView = (ImageView) rootView.findViewById(R.id.deleteIV);

        mSwipeEventListener = (SwipeListener) getActivity();

        containerRL = (RelativeLayout) rootView.findViewById(R.id.container);
        containerRL.setClickable(true);

        gestureScale = new ScaleGestureDetector(getActivity(), this);
        tapGestureDetector = new GestureDetector(getContext(), new customGestureListener());
        swipeGestureDetector = new GestureDetector(getContext(), new SwipeGestureListener());

        return rootView;
    }


    public boolean doesHaveAnyStickers() {
        return containerRL.getChildCount() > 0;
    }

    public void addEmoticonFromGrid(View v) {

        ImageView iv = (ImageView) v;
        //  Bitmap bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
        ImageView imageView = new ImageView(getActivity());
        // imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        containerRL.addView(imageView);
        Object[] tags = (Object[]) iv.getTag();
        StickerModel stickerModel = (StickerModel) tags[0];

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.SELECT_STICKER, AnalyticsEvents.STICKER_ID, stickerModel.stickerId, AnalyticsEvents.STICKER_URL, stickerModel.pictureUrl);

        LayoutParams layoutParams = (LayoutParams) (imageView).getLayoutParams(); //Setting the layout params of the imageview

        int topMargin = (int) (stickerModel.marginTop * deviceHeightPixels);
        int leftMargin = (int) (stickerModel.marginLeft * deviceWidthPixels);
        int width = (int) (stickerModel.width * deviceWidthPixels);
        int height = (int) (stickerModel.height * deviceHeightPixels);
        Log.d(TAG, " top " + topMargin + " left " + leftMargin +
                " height " + height + " width " + width);


        Log.d(TAG, " fireBase says height: " + stickerModel.height + " width: " + stickerModel.width + " marginTop: " + stickerModel.marginTop + " marginLeft: " + stickerModel.marginLeft
                + " rotation " + stickerModel.degree);

        imageView.setRotation(stickerModel.degree);

        if (stickerModel.width != 0 && stickerModel.height != 0) {
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.topMargin = topMargin;
            layoutParams.leftMargin = leftMargin;
        } else {
            int[] dimens = (int[]) tags[1];
            layoutParams.width = dimens[0];
            layoutParams.height = dimens[1];
            layoutParams.topMargin = dimens[2];
            layoutParams.leftMargin = dimens[3];
        }

        if (stickerModel.stickerPresentInAssets)
            Picasso.with(context).load(stickerModel.localUri).into(imageView);
        else
            Picasso.with(context).load(new File(stickerModel.localUri)).into(imageView);

        layoutParams.rightMargin = -1000;
        layoutParams.bottomMargin = -1000;

        if (containerRL.getChildCount() > 1) {
            for (int i = 0; i < containerRL.getChildCount(); i++) {
                View view = containerRL.getChildAt(i);
                if (view instanceof EditText) {
                    view.bringToFront();
                    break;
                }
            }
        }

        ViewTagData tagData = new ViewTagData();
        tagData.scaleFactor = 1;
        imageView.setTag(tagData);

        if (!getPreferences().getBoolean(AppLibrary.STICKER_TUT_SHOWN, false)) {
            // tutorial toast for sticker
            customToastTut = showCustomToast(getActivity(), R.layout.tut3, Gravity.CENTER, 0, 0, 7000);
            getPreferences().edit().putBoolean(AppLibrary.STICKER_TUT_SHOWN, true).commit();
        }

//        addViewTreeObserver(imageView);
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelCustomToast(customToastTut);
    }

    //    public void toggleEmoticonLayer(boolean enableTouchOnMe) {
//        containerRL.setClickable(enableTouchOnMe);
//        containerRL.setOnTouchListener(enableTouchOnMe ? touchDetector : null);
//
//        touchDetectorFrame.setClickable(enableTouchOnMe);
//        touchDetectorFrame.setOnTouchListener(enableTouchOnMe ? touchDetector : null);
//    }

//    private void addViewTreeObserver(final View view) {
//        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
//        if (viewTreeObserver.isAlive()) {
//            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//                @Override
//                public void onGlobalLayout() {
//                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                    ImageViewTagData tagData = new ImageViewTagData();
//                    int initIVHeight = view.getMeasuredHeight();
//                    int initIVWidth = view.getMeasuredWidth();
//                    Log.d(TAG, "VTO " + initIVWidth + "<-width height->" + initIVHeight);
//                    tagData.imageViewId = view.getId();
//                    tagData.mEffectiveWidth = view.getWidth();
//                    tagData.mEffectiveHeight = view.getHeight();
//                    view.setTag(tagData);
//                }
//            });
//        }
//    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        ViewToDrag = null;
        mViewLockedToDrag = false;

        scaleFactor *= detector.getScaleFactor();

        if (detector.getScaleFactor() < 1) //Enchancing scaling sensitivity
            scaleFactor = scaleFactor * 0.99f;
        else
            scaleFactor = scaleFactor * 1.01f;

        if (VERBOSE) {
            Log.d(TAG, " onScaleCalled. scale factor by detector is " + detector.getScaleFactor());
            Log.d(TAG, " scale factor " + scaleFactor);
        }
        scaleFactor = (scaleFactor < 0.3f ? 0.3f : scaleFactor); // prevent our view from becoming too small //
        scaleFactor = (scaleFactor > 15 ? 15 : scaleFactor);//not too large image
        scaleFactor = ((float) ((int) (scaleFactor * 100))) / 100; // Change precision to help with jitter when user just rests their fingers //
        imageViewToScale.setScaleX(scaleFactor);
        imageViewToScale.setScaleY(scaleFactor);

        if (VERBOSE) {
            Log.d(TAG, " absolute X" + absoluteX + " Y " + absoluteY);
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.d(TAG, " scale begin");

        ViewTagData tag = (ViewTagData) imageViewToScale.getTag();
        scaleFactor = tag.scaleFactor;
        inScale = true;
        ((VideoEditorFragment) getParentFragment()).setEmoticonTranslated(true);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(TAG, " scale end");

        if (imageViewToScale == null)
            return;

        ViewTagData tag = (ViewTagData) imageViewToScale.getTag();
        tag.scaleFactor = scaleFactor;
        imageViewToScale.setTag(tag);
        containerRL.invalidate();
        imageViewToScale = null;

        cancelCustomToast(customToastTut);
    }

    /**
     * Finding the image view intersecting the line
     *
     * @return true if any suitable draggable/scalable view found
     * false otherwise
     */
    private boolean findNearestTarget() {
        ArrayList<View> imageViewArrayList = new ArrayList<>();

        for (int i = 0; i < containerRL.getChildCount(); i++) {
            View view = containerRL.getChildAt(i);
            absoluteX = view.getX() + (view.getMeasuredWidth() / 2);
            absoluteY = view.getY() + (view.getMeasuredHeight() / 2);

            float x1, x2, y1, y2;
            x1 = motionEvent.getX(0);
            x2 = motionEvent.getX(1);
            y1 = motionEvent.getY(0);
            y2 = motionEvent.getY(1);

            if (x1 > x2) {
                float temp = x1;
                x1 = x2;
                x2 = temp;

                temp = y1;
                y1 = y2;
                y2 = temp;
            }

            if ((x1 <= absoluteX && absoluteX <= x2) || (y1 <= absoluteY && absoluteY <= y2)) { //Add everything if midpoint lies in between
                if (!(view instanceof EditText && defaultTextState)) {
                    imageViewArrayList.add(view);
                    Log.d(TAG, " qualified ImageView ");
                }
            }
        }

        for (int i = imageViewArrayList.size() - 1; i >= 0; i--) {
            View imageView = imageViewArrayList.get(i);

            float scaleFactor = ((ViewTagData) imageView.getTag()).scaleFactor;

            P1finalX = motionEvent.getX(0);
            P1finalY = motionEvent.getY(0);

            P2finalX = motionEvent.getX(1);
            P2finalY = motionEvent.getY(1);

            float[] pointA = {P1finalX, P1finalY};
            float[] pointB = {P2finalX, P2finalY};

            float realcenterX = imageView.getX() + (imageView.getMeasuredWidth() / 2);
            float realcenterY = imageView.getY() + (imageView.getMeasuredHeight() / 2);
            float realWidth = imageView.getMeasuredWidth() * scaleFactor;
            float realHeight = imageView.getMeasuredHeight() * scaleFactor;

            float minX = realcenterX - realWidth / 2;
            float minY = realcenterY - realHeight / 2;

            float[] rectPointA = new float[2];
            float[] rectPointB = new float[2];
            float[] rectPointC = new float[2];
            float[] rectPointD = new float[2];

            rectPointA[0] = minX;
            rectPointA[1] = minY;

            rectPointB[0] = minX + realWidth;
            rectPointB[1] = minY;

            rectPointC[0] = minX + realWidth;
            rectPointC[1] = minY + realHeight;

            rectPointD[0] = minX;
            rectPointD[1] = minY + realHeight;

            RectF rectF = new RectF(realcenterX - realWidth / 2, realcenterY - realHeight / 2,
                    realcenterX + realWidth / 2, realcenterY + realHeight / 2); //Non-rotated Rect
            Matrix m1 = new Matrix();
            m1.setRotate(-imageView.getRotation(), realcenterX, realcenterY); //to rotate point by -ve rotation
            float[] pointA1;
            float[] pointB1;
            pointA1 = pointA;
            pointB1 = pointB;
            m1.mapPoints(pointA1);
            m1.mapPoints(pointB1);

            // First check if both the points of the line are inside the rect - Best case scenario
            if (rectF.contains(pointA1[0], pointA1[1]) && rectF.contains(pointB1[0], pointB1[1])) {
                imageViewToScale = imageView;
                return true;
            }

            // Else, Rotate all rect points to get rotated rectangle and form rectangle ABCD
            // Check recursively if the line joining Point A & Point B, intersects with any line in the rectangle
            Matrix m = new Matrix();
            m.setRotate(imageView.getRotation(), realcenterX, realcenterY);
            m.mapPoints(rectPointA);
            m.mapPoints(rectPointB);
            m.mapPoints(rectPointC);
            m.mapPoints(rectPointD);

            if (get_line_intersection(pointA, pointB, rectPointA, rectPointB)) {
                imageViewToScale = imageView;
                return true;
            } else if (get_line_intersection(pointA, pointB, rectPointB, rectPointC)) {
                imageViewToScale = imageView;
                return true;
            } else if (get_line_intersection(pointA, pointB, rectPointC, rectPointD)) {
                imageViewToScale = imageView;
                return true;
            } else if (get_line_intersection(pointA, pointB, rectPointD, rectPointA)) {
                imageViewToScale = imageView;
                return true;
            }
        }

        return false;
    }

    private boolean get_line_intersection(float[] p0, float[] p1,
                                          float[] p2, float[] p3) {
        float p0_x = p0[0];
        float p0_y = p0[1];

        float p1_x = p1[0];
        float p1_y = p1[1];

        float p2_x = p2[0];
        float p2_y = p2[1];

        float p3_x = p3[0];
        float p3_y = p3[1];

        float s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        float s, t;
        try {
            s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
            t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

            if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                return true; // Collision detected
            }

            return false; // No collision
        } catch (Exception e) { //To handle any division by zero or any other exception
            return false;
        }
    }


    /**
     * @param event the motionEvent w.r.t. which the rotation is calculated
     */
    private void calculateRotation(MotionEvent event) {
        if (imageViewToScale == null) return;
        String TAG = this.TAG + " rotation";
        P1finalX = event.getX(0);
        P1finalY = event.getY(0);

        P2finalX = event.getX(1);
        P2finalY = event.getY(1);
        if (shouldSwapPoints) swapThePoints();

        xyCoordinates[0] = (P1finalX + P2finalX) / 2;
        xyCoordinates[1] = (P1finalY + P2finalY) / 2;

        if (VERBOSE) {
            Log.d(TAG, " P1 X: " + event.getX(0) + " P1 Y:" + event.getY(0));
            Log.d(TAG, " P2 X: " + event.getX(1) + " P2 Y:" + event.getY(1));
        }
        float angle = (float) Math.toDegrees(Math.atan2(-(P2finalY - P1finalY), (P2finalX - P1finalX)));

        if (VERBOSE) {
            Log.d(TAG, " rawAngle " + angle);
        }
        normalizedAngle = (angle < 0 ? angle += 360 : angle);
        if (startingAngle == null) {
            startingAngle = normalizedAngle;
        }
        currentAngularDisplacement = normalizedAngle - startingAngle;
        Log.d(TAG, "normalizedAngle " + normalizedAngle);
        Log.d(TAG, "displacement " + currentAngularDisplacement);
    }

    private void swapThePoints() {
        float temp;
        temp = P1finalX;
        P1finalX = P2finalX;
        P2finalX = temp;

        temp = P1finalY;
        P1finalY = P2finalY;
        P2finalY = temp;
    }


    private View findViewToDrag(float x, float y) {
        View closestIV = null;
        for (int i = containerRL.getChildCount() - 1; i >= 0; i--) {
            View dragView = containerRL.getChildAt(i);

            float scaleFactor = ((ViewTagData) dragView.getTag()).scaleFactor;
            float realWidth = dragView.getMeasuredWidth() * scaleFactor;
            float realHeight = dragView.getMeasuredHeight() * scaleFactor;
            float realcenterX = dragView.getX() + (dragView.getMeasuredWidth() / 2);
            float realcenterY = dragView.getY() + (dragView.getMeasuredHeight() / 2);

            RectF rectF = new RectF(realcenterX - realWidth / 2, realcenterY - realHeight / 2,
                    realcenterX + realWidth / 2, realcenterY + realHeight / 2);
            Matrix m = new Matrix();
            m.setRotate(-dragView.getRotation(), realcenterX, realcenterY);
            float[] points = new float[2];
            points[0] = x;
            points[1] = y;
            m.mapPoints(points);

            if (rectF.contains(points[0], points[1])) {
                Log.d(TAG, "lock n load");
                closestIV = dragView;
                ((VideoEditorFragment) getParentFragment()).setEmoticonTranslated(true);
                break;
            }
//            if ((x >= (realcenterX - realWidth / 2) && x <= (realcenterX + realWidth / 2)) &&
//                    (y >= (realcenterY - realHeight / 2) && y <= (realcenterY + realHeight / 2))) {
//                Log.d(TAG, "Lock and Load");
//                closestIV = dragView;
//                break;
//            }
        }


        return closestIV;
    }


    private class customGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {

            if (editText == null) {
                addTextBox(event);
            } else { //Its a tap on the edit text
                if (!DragOnText) {
                    if (!textInFocus) {
                        Log.d(TAG, "Detected single tap on the edit text");
                        moveViewToScreenCenter(editText);
                        textInFocus = true;
                    } else { //Not supposed to happen
                        Log.d(TAG, "Tap detected after focus on edit text");
                    }
                } else {
                    Log.d(TAG, "Tap detected with drag on text as true");
                    return true;
                }
            }
            return true;
        }

//        @Override
//        public boolean onLongPress(MotionEvent event) {
//            return false;
//        }

//        @Override
//        public boolean onDown(MotionEvent event) {
//            return false;
//        }

    }


    private void addTextBox(MotionEvent event) { //Function called only once

        ((VideoEditorFragment) getParentFragment()).removeTuts();
        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.ADD_DEFAULT_CAPTION);

        defaultTextState = true;
        textInFocus = true;

        editText = new NoMenuEditText(getActivity());
        editText.setGravity(Gravity.CENTER);
        editText.setBackgroundColor(Color.parseColor("#80000000"));
        editText.setTextColor(Color.parseColor("#ffffff"));
        editText.setHint("Add a caption");
        editText.setTextSize(15);
        editText.setSingleLine(false);
        editText.setTypeface(fontPicker.getMontserratRegular());
        editText.setBackgroundResource(R.drawable.edit_text_background);
        editText.setHintTextColor(Color.parseColor("#80ffffff"));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)}); //60 character limit ~ 2 lines
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(true);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);

        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editText, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        paramsArray[0] = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT); //Min and max width cannot be controlled in match_parent layout

        if (event != null)
            paramsArray[0].setMargins(0, (int) event.getY(), 0, -1000);
        else
            paramsArray[0].setMargins(0, (AppLibrary.getDeviceParams(getActivity())[1]) / 2 - editText.getMeasuredHeight() / 2, 0, -1000);

        editText.setLayoutParams(paramsArray[0]);
        editText.setMaxWidth(AppLibrary.getDeviceParams(getActivity())[0]);
//        editText.setMinWidth(AppLibrary.getDeviceParams(getActivity())[0]);

        ViewTagData tagData = new ViewTagData();
        tagData.scaleFactor = 1;
        tagData.params[0] = paramsArray[0].leftMargin;
        tagData.params[1] = paramsArray[0].topMargin;
        tagData.rotationAngle = 0;
        editText.setTag(tagData);

        if (VERBOSE) {
            Log.d(TAG, "Location on screen while adding text box: " + tagData.params[0] + " " + tagData.params[1]);
        }

        containerRL.addView(editText);

        moveViewToScreenCenter(editText);

        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (VERBOSE) {
                    Log.d(TAG, " Key Event Value: " + actionId + "--->"/* + event.getAction()*/);
                }

                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_PREVIOUS) {

                    minimizeEditor();
                    return true;
                }
                return false;
            }
        });

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before, int count) {
                if (arg0.length() != 0) { //Handles wrap content of edit text
                    editText.setHint("");
                } else {
                    editText.setHint("Add a caption");
                }
            }
        });

    }


    public void minimizeEditor() {
        textInFocus = false;
        moveViewBackFromCenter(editText);
    }


    public void toggleTextBox() {
        if (editText == null) {
            addTextBox(null);
            return;
        }

        Log.d(TAG, "Called Toggle Textbox");

        if (textInFocus) {
            editText.clearFocus();
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    toggleEditTextProperties();
                    moveViewToScreenCenter(editText);
                }
            };
            keyboardHandler.postDelayed(r, 100);
        } else {
            toggleEditTextProperties();
        }
    }

    public void toggleEditTextProperties() {
        defaultTextState = !defaultTextState;

        AnalyticsManager.getInstance().trackEvent(AnalyticsEvents.TOGGLE_TEXT_CAPTION);

        if (paramsArray[1] == null)
            paramsArray[1] = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextState ? 15 : 30);
        editText.setBackgroundResource(defaultTextState ? R.drawable.edit_text_background : 0);
        editText.setTypeface(defaultTextState ? fontPicker.getMontserratRegular() : fontPicker.getMuseo700());
        editText.setTextColor(defaultTextState ? Color.parseColor("#ffffff") : Color.parseColor("#ffffff"));
        editText.setRotation(0);
        editText.setScaleY(1);
        editText.setScaleX(1);
        scaleFactor = 1;
        editText.setGravity(defaultTextState ? Gravity.CENTER : Gravity.CENTER);
        if (defaultTextState)
            editText.setShadowLayer(0, 0, 0, 0);
        else
            editText.setShadowLayer(2, 1, 1, Color.parseColor("#B3000000"));

//      editText.setMinimumWidth(defaultTextState ? AppLibrary.getDeviceParams(getActivity())[0]:0);
//      editText.setMaxWidth(defaultTextState ? AppLibrary.getDeviceParams(getActivity())[0]:AppLibrary.getDeviceParams(getActivity())[0]);

        if (!defaultTextState)
            paramsArray[1].setMargins(AppLibrary.getDeviceParams(getActivity())[0] / 2 - editText.getMeasuredWidth() / 2, AppLibrary.getDeviceParams(getActivity())[1] / 2 - (editText.getMeasuredHeight() * 2) / 2 - statusBarOffset, -1000, -1000);
        else
            paramsArray[0].setMargins(0, (AppLibrary.getDeviceParams(getActivity())[1]) / 2 - (editText.getMeasuredHeight()) / 4, 0, -1000);

        editText.setLayoutParams(defaultTextState ? paramsArray[0] : paramsArray[1]);

        Log.d(TAG, "Measured Height while toggling: " + editText.getMeasuredHeight());

        ViewTagData tag = (ViewTagData) editText.getTag();
        LayoutParams params = (LayoutParams) editText.getLayoutParams();
        tag.params[0] = params.leftMargin;
        tag.params[1] = params.topMargin;
        tag.rotationAngle = editText.getRotation();
        tag.scaleFactor = 1;
        editText.setTag(tag);
    }


    private void moveViewToScreenCenter(final View view) {
        statusBarOffset = AppLibrary.getDeviceParams(getActivity())[1] - rootView.getMeasuredHeight();

        ViewTagData tag = (ViewTagData) view.getTag();
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        tag.params[0] = params.leftMargin;
        tag.params[1] = params.topMargin;
        tag.rotationAngle = view.getRotation();
        view.setTag(tag); //Scale is already being set elsewhere

        int xDest = AppLibrary.getDeviceParams(getActivity())[0] / 2;
        xDest -= view.getMeasuredWidth() / 2;
        int yDest = (int) (AppLibrary.getDeviceParams(getActivity())[1] / 2 - (view.getMeasuredHeight() / 2) - statusBarOffset);

        Log.d(TAG, "Value of xDest an yDest are: " + xDest + " " + yDest);
        Log.d(TAG, "Value of measured width and height are: " + view.getMeasuredWidth() + " " + view.getMeasuredHeight());

        int t;
        t = 200;

        Runnable showKeyboard = new Runnable() {
            @Override
            public void run() {
                if (view instanceof EditText) {
                    editText.setCursorVisible(true);
                    editText.setSelection(editText.getText().length());
                    editText.setMaxLines(3);
                    editText.requestFocus();
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        };

        if (defaultTextState) {
            view.animate()
                    .y(yDest - 120)
                    .x(0)
                    .rotation(0)
                    .setDuration(t)
                    .withEndAction(showKeyboard).start();
        } else {
            view.animate()
                    .y(yDest - 150)
                    .x(10)
                    .rotation(0)
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(t)
                    .withEndAction(showKeyboard).start();
        }

        ((VideoEditorFragment) getParentFragment()).checkAndToggleBackButton(true);
    }

    private void moveViewBackFromCenter(View view) {

        if (view == null) return;

        ((VideoEditorFragment) getParentFragment()).checkAndToggleBackButton(false);

        if (view instanceof EditText) {
            editText.clearFocus();
            editText.setCursorVisible(false);
            editText.setMaxLines(5);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

            if (editText.getText().toString().trim().length() == 0 && !textInFocus) {
                view.animate().setDuration(200).alpha(0f);

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        containerRL.removeView(editText);
                        editText = null;
                    }
                };

                keyboardHandler.postDelayed(r, 400);
                return;
            } else {
                editText.setText(editText.getText());
            }
        }

        ViewTagData tag = (ViewTagData) view.getTag();
        float x = tag.params[0];
        float y = tag.params[1];
        float scaleFactor = tag.scaleFactor;
        float rotation = tag.rotationAngle;

        float apparentX = x;
        float apparentY = y;

        Log.d(TAG, "Moving view back to: " + apparentX + " " + apparentY);

        int t;
        t = 200;

        view.animate().x(x).y(y).setDuration(t).rotation(rotation).scaleX(scaleFactor).scaleY(scaleFactor).start();
    }

    public class ReverseInterpolator implements Interpolator { //Not being used
        @Override
        public float getInterpolation(float paramFloat) {
            return Math.abs(paramFloat - 1f);
        }
    }

    public interface SwipeListener {
        void onSwipeLeft();

        void onSwipeRight();
    }

    @Nullable
    public String getMediaText() {
        if (editText == null) return null;
        String s = editText.getText().toString().trim();
        return s.length() == 0 ? null : s;
    }

}
