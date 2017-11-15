//package com.pulseapp.android.stickers;
//
//import android.content.Context;
//import android.graphics.Color;
//import android.graphics.Rect;
//import android.os.Bundle;
//import android.os.Handler;
//import android.support.annotation.Nullable;
//import android.util.Log;
//import android.util.TypedValue;
//import android.view.Gravity;
//import android.view.KeyEvent;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.ScaleGestureDetector;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewTreeObserver;
//import android.view.inputmethod.EditorInfo;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.EditText;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//import com.pulseapp.android.R;
//import com.pulseapp.android.fragments.BaseFragment;
//import com.pulseapp.android.signals.BroadCastSignals;
//import com.pulseapp.android.util.AppLibrary;
//
//
///**
// * Created by deepankur on 11/3/16.
// */
//
//public class TextFragment extends BaseFragment {
//
//    private Handler handler;
//    private boolean mDefaultState = true;
//    private boolean inScale;
//    private boolean isTextBoxAdded;
//    private EditText editText;
//    private String TAG = "TextFragment";
//    private InputMethodManager imm;
//    private RelativeLayout relativeLayout;
//    private RelativeLayout.LayoutParams[] paramsArray;
//    private ScaleGestureDetector gestureScale;
//    private boolean mDragLockGained, mScaleLockGained;
//    private float scaleFactor = 1;
//
//    @Override
//    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {
//
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        handler = new Handler();
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_edit_text, container, false);
//
//        paramsArray = new RelativeLayout.LayoutParams[2];
//
//        //zoomable , scalable , movable params
//        paramsArray[0] = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        paramsArray[0].topMargin = 450;
//        paramsArray[0].leftMargin = 150;
//        paramsArray[0].rightMargin = -1000;
//        paramsArray[0].bottomMargin = -1000;
//
//        //default ie. black background, just movable along y axis
//        paramsArray[1].leftMargin = 0;
//        paramsArray[1] = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        paramsArray[1].topMargin = 200;
//        relativeLayout = (RelativeLayout) rootView;
//        gestureScale = new ScaleGestureDetector(getActivity(), scaleZoomListener);
//
//        return rootView;
//    }
//
//    boolean isHandlerPosted;
//
//    public boolean canTakeTouch() {
//        return editText != null && (mDragLockGained || mScaleLockGained);
//    }
//
//    public boolean setTouchEvent(final MotionEvent event) {
//        if (editText == null) return false;
//        currentMotionEvent = event;
//        Log.d(TAG, " time outside" + System.currentTimeMillis());
//        if (event.getPointerCount() == 2 && mDragLockGained)
//            endTranslation();
//
//        if (mDragLockGained && event.getPointerCount() == 1)
//            translate(event);
//        if (!isHandlerPosted && (!mDragLockGained && !mScaleLockGained)) {
//            isHandlerPosted = true;
//            Runnable r = new Runnable() {
//                @Override
//                public void run() {
//                    isHandlerPosted = false;
//                    Log.d(TAG, " firing new runnable dragLock:" + mDragLockGained +
//                            " scaleLock " + mScaleLockGained);
//                    if (event.getPointerCount() == 1 && event.getAction() != MotionEvent.ACTION_UP) {
//                        mDragLockGained = meetsMinimumDistanceForDrag(event);
//                        if (mDragLockGained) startTranslation(event);
//                    }
//                    if (event.getPointerCount() == 2) {
//                        mScaleLockGained = meetMinimumDistanceForScale(event);
//
//                    }
//                    Log.d(TAG, " time in run" + System.currentTimeMillis());
//                }
//            };
//            handler.postDelayed(r, 50);
//        }
//        gestureScale.onTouchEvent(event);
//        if (inScale && event.getPointerCount() == 2 && !mDefaultState) {
//            calculateRotation(event);
//        }
//        return false;
//    }
//
//    MotionEvent currentMotionEvent;
//    final float DRAG_HIT_RADIUS = 100;//pixels
//    final float SCALE_HIT_RADIUS = 100;//pixels
//
//    public boolean meetsMinimumDistanceForDrag(MotionEvent event) {
//        if (editText == null) return false;
//        if (mDefaultState) {
//            double distance = Math.abs(event.getY() - editText.getTop());
//            Log.d(TAG, " eligible for drag ? " + distance + " action " + getActionName(event.getAction()));
//            return distance < DRAG_HIT_RADIUS;
//        }
//        double distance = Math.sqrt(Math.pow((event.getX() - recordedCenterXY[0]), 2) +
//                Math.pow((event.getY() - recordedCenterXY[1]), 2));
//        Log.d(TAG, " eligible for drag ? " + distance + " action " + getActionName(event.getAction()));
//        return distance < DRAG_HIT_RADIUS;
//    }
//
//    public boolean meetMinimumDistanceForScale(MotionEvent event) {
//        double[] pointA = {event.getX(0), -event.getY(0)};
//        double[] pointB = {event.getX(1), -event.getY(1)};
//        double[] pointC = {recordedCenterXY[0], -recordedCenterXY[1]};
//        double LineToPointDistance = LineToPointDistance2D(pointA, pointB, pointC, true);
//        Log.d(TAG, " eligible for scale ?" + LineToPointDistance);
//        return LineToPointDistance < SCALE_HIT_RADIUS;
//    }
//
//    public void textEditorClicked() {
//        if (mDragLockGained || mScaleLockGained) {
//            Log.d(TAG, " returning as drag or scale in progress " + mDragLockGained + mScaleLockGained);
//            return;
//        }// we are already busy
//        if (!isTextBoxAdded) {
//            isTextBoxAdded = true;
//            addTextBox();
//        } else toggleTextBox();
//    }
//
//    public String getMediaText(){
//        if (editText != null){
//            return AppLibrary.checkStringObject(editText.getText().toString());
//        } else {
//            return null;
//        }
//    }
//
//    private void addTextBox() {
//        editText = new EditText(getActivity());
//        editText.setSingleLine(true);
//        relativeLayout.addView(editText, paramsArray[1]);
//        // editText.setGravity(Gravity.CENTER);
//        editText.setTextColor(Color.parseColor("#ffffff"));
//        editText.setFocusable(true);
//        editText.requestFocus();
//
//        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
//
//        editText.setBackgroundColor(Color.parseColor("#80000000"));
//        editText.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return false;
//            }
//        });
//
//        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                Log.d(TAG, " Key Event Value: " + actionId + "--->"/* + event.getAction()*/);
//
//                if (actionId == EditorInfo.IME_ACTION_SEND
//                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT
//                        || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
//                    // editText.setFocusable(false);
//                    editText.clearFocus();
//                    editText.setCursorVisible(false);
//                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
//                    editText.setGravity(mDefaultState ? Gravity.CENTER : Gravity.LEFT);
//                    VTO(editText);
//                    return true;
//                }
//                return false;
//            }
//        });
//    }
//
//    private void toggleTextBox() {
//        mDefaultState = !mDefaultState;
//        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mDefaultState ? 16 : 30);
//        editText.setLayoutParams(mDefaultState ? paramsArray[1] : paramsArray[0]);
//        editText.setBackgroundColor(mDefaultState ? Color.parseColor("#80000000") : Color.parseColor("#00000000"));
//        editText.setTextColor(mDefaultState ? Color.parseColor("#ffffff") : Color.parseColor("#ffffff"));
////       editText.setSingleLine(mDefaultState);why do you mess with keyboard dude..?
//        editText.setRotation(0);
//        editText.setScaleY(1);
//        editText.setScaleX(1);
//        scaleFactor = 1;
//        // editText.setGravity(mDefaultState ? Gravity.CENTER : Gravity.LEFT);
//        VTO(editText);
//    }
//
//
//    public void notifyTap(MotionEvent event) {
//        Log.d(TAG, "onSingleTapConfirmed x:" + event.getX() + " y: " + event.getY());
//        //     Log.d(TAG, " contains result " + isViewContains(editText, (int) event.getX(), (int) event.getY()));
//        if (meetsMinimumDistanceForDrag(event))
//            showKeyboard();
//
//    }
//
//    private void showKeyboard() {
//        Log.d(TAG, " keyboard requested ");
//        editText.setFocusable(true);
//        editText.setCursorVisible(true);
//        editText.setGravity(Gravity.NO_GRAVITY);
//        editText.requestFocus();
//        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
//
//    }
//
//
//    public void notifyFingerUp() {
//        Log.d(TAG, " notifyUp");
//        if (mDragLockGained) endTranslation();
//        mScaleLockGained = false;
//        //resetScaleAndRotate();
//    }
//
//    private boolean shouldSwapPoints;
//    private ScaleGestureDetector.OnScaleGestureListener scaleZoomListener = new ScaleGestureDetector.OnScaleGestureListener() {
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            scaleFactor *= detector.getScaleFactor();
//            scaleFactor = (scaleFactor < 1 ? 1 : scaleFactor); // prevent our view from becoming too small //
//            scaleFactor = (scaleFactor > 5 ? 5 : scaleFactor);//not too large image
//            scaleFactor = ((float) ((int) (scaleFactor * 100))) / 100; // Change precision to help with jitter when user just rests their fingers //
//            //Log.d(TAG, " scale " + scaleFactor);
//            Log.d(TAG, " onScale " + editText.getLeft() + " " + editText.getTop());
//            zoomTheView();
//            return true;
//        }
//
//        @Override
//        public boolean onScaleBegin(ScaleGestureDetector detector) {
//            Log.d(TAG, " scale begin");
//            if (currentMotionEvent.getPointerCount() == 2) {
//                shouldSwapPoints = (currentMotionEvent.getX(0) > currentMotionEvent.getX(1));
//            }
//            if (!mScaleLockGained || mDragLockGained || mDefaultState) return false;
//            startingAngle = null;
//            inScale = true;
//            recordedImageViewRotation = -editText.getRotation();
//            return true;
//        }
//
//        @Override
//        public void onScaleEnd(ScaleGestureDetector detector) {
//            Log.d(TAG, " scale ended ");
//            mScaleLockGained = false;
//            inScale = false;
//        }
//    };
//
//    private void resetScaleAndRotate() {
//        editText.setRotation(0);
//        editText.setScaleX(1);
//        editText.setScaleY(1);
//        scaleFactor = 1;
//    }
//
//    private void zoomTheView() {
//        editText.setScaleX(scaleFactor);
//        editText.setScaleY(scaleFactor);
//    }
//
//    float P1finalX, P1finalY;
//    float P2finalX, P2finalY;
//    float[] xyCoordinates = new float[2];
//    float normalizedAngle;
//    Float startingAngle = null;
//    float currentAngularDisplacement;
//    float recordedImageViewRotation;
//
//    private void calculateRotation(MotionEvent event) {
//        String TAG = this.TAG + " rotation";
//        P1finalX = event.getX(0);
//        P1finalY = event.getY(0);
//
//        P2finalX = event.getX(1);
//        P2finalY = event.getY(1);
//        if (shouldSwapPoints) swapThePoints();
//
//        xyCoordinates[0] = (P1finalX + P2finalX) / 2;
//        xyCoordinates[1] = (P1finalY + P2finalY) / 2;
//
//        Log.d(TAG, " P1 X: " + event.getX(0) + " P1 Y:" + event.getY(0));
//        Log.d(TAG, " P2 X: " + event.getX(1) + " P2 Y:" + event.getY(1));
//
//        float angle = (float) Math.toDegrees(Math.atan2(-(P2finalY - P1finalY), (P2finalX - P1finalX)));
//
//        Log.d(TAG, " rawAngle " + angle);
//        normalizedAngle = (angle < 0 ? angle += 360 : angle);
//        if (startingAngle == null) {
//            startingAngle = normalizedAngle;
//        }
//        currentAngularDisplacement = normalizedAngle - startingAngle;
//        Log.d(TAG, "normalizedAngle " + normalizedAngle);
//        Log.d(TAG, "displacement " + currentAngularDisplacement);
//        editText.setRotation(-(currentAngularDisplacement + recordedImageViewRotation));
//
//    }
//
//    private void swapThePoints() {
//        float temp;
//        temp = P1finalX;
//        P1finalX = P2finalX;
//        P2finalX = temp;
//
//        temp = P1finalY;
//        P1finalY = P2finalY;
//        P2finalY = temp;
//    }
//
//    private boolean rectContains(View view, int rx, int ry) {
//        int[] l = new int[2];
//        view.getLocationOnScreen(l);
//        Log.d(TAG, " contains func left: " + l[0] + " top: " + l[1] +
//                " bottom: " + view.getBottom() + " right: " + view.getRight() +
//                " width: " + view.getWidth() + " height: " + view.getHeight());
//        Rect rect = new Rect(l[0], l[1], l[0] + view.getWidth(), l[1] + view.getHeight());
//        return rect.contains(rx, ry);
//    }
//
//
//    RelativeLayout.LayoutParams hitAreaParams;
//
//    private void hitAreaUsingParams() {
//        hitAreaParams = (RelativeLayout.LayoutParams) editText.getLayoutParams();
//        int left = hitAreaParams.leftMargin;
//        int right = hitAreaParams.rightMargin;
//        int top = hitAreaParams.topMargin;
//        int bottom = hitAreaParams.bottomMargin;
//        int height = hitAreaParams.height;
//        int width = hitAreaParams.width;
//        Log.d(TAG, " hitArea Params" +
//                " left " + left + " right " + right +
//                " top " + top + " bottom " + bottom +
//                " height " + height + " width " + width);
//    }
//
//
//    float initX, initY, currentDeltaX, currentDeltaY;
//    float[] recordedCenterXY = new float[2];//0 for X; 1 for Y
//
//    private void VTO(final View view) {
//        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
//                int left = params.leftMargin;
//                int top = params.topMargin;
//                recordedCenterXY[0] = left + view.getWidth() / 2;
//                recordedCenterXY[1] = top + view.getHeight() / 2;
//                Log.d(TAG, " width height " + view.getWidth() + " " + view.getHeight());
//                Log.d(TAG, " VTO x: " + recordedCenterXY[0] + " y: " + recordedCenterXY[1]);
////                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
////                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
////                else
////                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//            }
//        });
//    }
//
//    int[] initParamXY = new int[2];
//
//    private void startTranslation(MotionEvent event) {
//        Log.d(TAG, " dragLock aquired");
//        initX = event.getX();
//        initY = event.getY();
//        translationParams = (RelativeLayout.LayoutParams) editText.getLayoutParams();
//        initParamXY[0] = translationParams.leftMargin;
//        initParamXY[1] = translationParams.topMargin;
//    }
//
//    RelativeLayout.LayoutParams translationParams;
//
//    private void translate(MotionEvent event) {
//        currentDeltaX = event.getX() - initX;
//        currentDeltaY = event.getY() - initY;
//        Log.d(TAG, " translate x: " + currentDeltaX + " y: " + currentDeltaY);
//        if (!mDefaultState)
//            translationParams.leftMargin = (int) (initParamXY[0] + currentDeltaX);
//        translationParams.topMargin = (int) (initParamXY[1] + currentDeltaY);
//        editText.setLayoutParams(translationParams);
//    }
//
//    private void endTranslation() {
//        Log.d(TAG, " dragLockReleased ");
//        mDragLockGained = false;
//        Log.d(TAG, " recording x: " + recordedCenterXY[0] + " y: " + recordedCenterXY[1]);
//    }
//}
