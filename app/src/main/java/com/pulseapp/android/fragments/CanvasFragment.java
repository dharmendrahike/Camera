package com.pulseapp.android.fragments;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.customViews.OvalColorPicker;
import com.pulseapp.android.signals.BroadCastSignals;
import com.pulseapp.android.stickers.ScribbleView;
import com.pulseapp.android.util.AppLibrary;
import com.pulseapp.android.util.EditTextBackEvent;
import com.pulseapp.android.util.PrivacyPopup;
import com.pulseapp.android.util.WriteImageCallback;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by abc on 3/9/2016.
 */

public class CanvasFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = "CanvasFragment";
    public EditTextBackEvent editText;//editText extension to detect backKey press;
    private InputMethodManager imm;
    private FrameLayout relativeLayout;
    private View bitmapView, backGroundColorView;
    private ViewControlsCallback viewControlsCallback;
    private View rootView;
    public FrameLayout revealView;
    private boolean defaultTextState;
    private int backgroundCount = 0;
    private static final int PERMISSION_ACCESS_EXTERNAL_STORAGE = 0;
    private boolean gotoShareScreen = false;
    public static boolean getContributableStreams = true;

    public void undoAction() {
        hideKeyboard();
        if (isColorPickerOpen())
            toggleColorPicker();
        editText.clearFocus();
        editText.setCursorVisible(false);
        viewControlsCallback.notifyFocus(false);
    }

    public interface AnimationListeners{
        void onAnimationStart();
        void onAnimationEnd();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        viewControlsCallback = (ViewControlsCallback) getParentFragment();
    }

    @Override
    public void onEvent(BroadCastSignals.BaseSignal eventSignal) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        defaultTextState = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFireBaseHelper == null || editText == null) return;

        editText.clearFocus();

        if (getActivity() != null && ((CameraActivity) getActivity()).isCanvasFragmentActive) //Handle only when canvas fragment is active
            viewControlsCallback.notifyFocus(false);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.canvas_fragment, container, false);
        progressView = rootView.findViewById(R.id.progressFram);
        progressView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;//blocking touch events to pass at bottom layer
            }
        });

        ((ProgressBar) rootView.findViewById(R.id.progressBar))
                .getIndeterminateDrawable()
                .setColorFilter(Color.parseColor("#80FFFFFF"), PorterDuff.Mode.SRC_IN);

        if (savedInstanceState != null) return rootView;

        revealView = (FrameLayout) rootView.findViewById(R.id.revealView);
        revealView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                showRevealAnimation(false, revealView,new AnimationListeners(){
                    @Override
                    public void onAnimationStart() {

                    }

                    @Override
                    public void onAnimationEnd() {
                        editText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                revealView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        bitmapView = rootView.findViewById(R.id.bitmapLayout);
        bitmapView.setDrawingCacheEnabled(true);
        bitmapView.setDrawingCacheQuality(FrameLayout.DRAWING_CACHE_QUALITY_HIGH);
        backGroundColorView = rootView.findViewById(R.id.backgroundColorLayout);
        backGroundColorView.setBackgroundResource(R.drawable.canvas_background_default);
        rootView.findViewById(R.id.addToMoment_IV).setOnClickListener(this);
        rootView.findViewById(R.id.addToStream).setOnClickListener(this);
        rootView.findViewById(R.id.shareToWhatsApp).setOnClickListener(this);
        //disabling sound as we will be performing on click on the download button itself and that button will make a sound
        rootView.findViewById(R.id.shareToWhatsApp).setSoundEffectsEnabled(false);

        initWhatsAppBtn(rootView);

        rootView.findViewById(R.id.closeButton).setOnClickListener(this);
//        undoIv = (ImageView) rootView.findViewById(R.id.undo_IV);
        editText = (EditTextBackEvent) rootView.findViewById(R.id.textEditLayout);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        editText.setTypeface(fontPicker.getMuseo500());
//        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                Rect r = new Rect();
//                //r will be populated with the coordinates of your view that area still visible.
//                rootView.getWindowVisibleDisplayFrame(r);
//
//                int heightDiff = rootView.getRootView().getHeight() - (r.bottom - r.top);
//                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
//                    Log.d(TAG, "Height difference is " + heightDiff);
//                }
//            }
//        });
        editText.setOnEditTextImeBackListener(new EditTextBackEvent.EditTextImeBackListener() {
            @Override
            public void onImeBack(EditTextBackEvent ctrl, String text) {
                editText.clearFocus();
                editText.setCursorVisible(false);
                viewControlsCallback.notifyFocus(false);
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                editText.setCursorVisible(hasFocus);
                editText.setSelection(editText.getText().length());
                if (hasFocus)
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                else
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                viewControlsCallback.notifyFocus(hasFocus);
            }
        });

//        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (
//                        actionId == EditorInfo.IME_ACTION_UNSPECIFIED
//                        || actionId == EditorInfo.IME_ACTION_NEXT
//                        || actionId == EditorInfo.IME_ACTION_PREVIOUS) {
//                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
//                    editText.clearFocus();
//                    editText.setCursorVisible(false);
//                    viewControlsCallback.notifyFocus(false);
//                    return true;
//                }
//                return false;
//            }
//        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    editText.setHint("");
                } else if (scribbleView.getPathListSize() == 0) {
                    editText.setHint("What's on your mind?");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCanvasViews(s.length() + scribbleView.getPathListSize());
                viewControlsCallback.notifyFocus(true);
            }
        });

        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
            f.setAccessible(true);
            f.set(editText, R.drawable.cursor);
        } catch (Exception ignored) {
        }

        final GestureDetector gestureDetector = new GestureDetector(getContext(), new CustomGestureListener());
        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isColorPickerOpen())
                    return false;
                else {
                    event.setLocation(event.getRawX(), event.getRawY());
                    try {
                        scribbleView.dispatchTouchEvent(event); //Pass the event back to scribble view
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        });
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = false;
                if (!isColorPickerOpen())
                    result = gestureDetector.onTouchEvent(event);

                return result;
            }
        });
        frameLayout = (FrameLayout) rootView.findViewById(R.id.touchDetector);
        scribbleView = (ScribbleView) rootView.findViewById(R.id.scribbleView);
        scribbleView.setDrawingEnabled(false);
        scribbleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        scribbleView.setScribbleListener(new ScribbleView.ScribbleListener() {
            @Override
            public void onScribbleStart() {
                ovalColorPicker.showUndo();
                updateCanvasViews(1);
                viewControlsCallback.notifyFocus(true);
            }

            @Override
            public void onScribbleEnd() {

            }

            @Override
            public void onTap(MotionEvent event) {

            }

            @Override
            public void onUndoScribble() {
                if (isCanvasLocked())
                    updateCanvasViews(1);
                else
                    updateCanvasViews(0);

                viewControlsCallback.notifyFocus(true);
            }
        });

        rootView.findViewById(R.id.hideCanvas).setOnClickListener(this);
        initColorEditor(rootView);
        relativeLayout = (FrameLayout) rootView;
        return rootView;
    }

    public void dispatchTouchEvent(MotionEvent event) {
        try {
            rootView.dispatchTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Whats app sharing will work pretty much the same wy as in the videoEditorFragment
     * as the button is clicked we turn on the boolean {@link #isSharingToWhatsApp}  and turning it off
     * when the sharing operation is done ie. the intent is launched.
     * After the clicking of whatsapp Btn the click is performed on download after the file download
     * call back comes the intent is launched.
     * Also share and whatsApp button are treated as 'siblings' as in whatever operation
     * (in terms of visibility) is performed on one will be performed on the other.
     * Hence width of one of these two views can be given 0 safely.
     *
     * @param rootView of this Fragment
     */
    private void initWhatsAppBtn(View rootView) {
        if (AppLibrary.isPackageInstalled(WHATSAPP_PACKAGE_NAME, context)) {
            rootView.findViewById(R.id.addToStream).getLayoutParams().width = 0;
        } else {
            rootView.findViewById(R.id.shareToWhatsApp).getLayoutParams().width = 0;
        }
    }

    private void updateCanvasViews(int length) {
        if (length == 0) {
            if (rootView.findViewById(R.id.addToMoment_IV).getVisibility() == View.VISIBLE)
                rootView.findViewById(R.id.addToMoment_IV).setVisibility(View.GONE);
            if (rootView.findViewById(R.id.addToStream).getVisibility() == View.VISIBLE)
                rootView.findViewById(R.id.addToStream).setVisibility(View.GONE);
            if (rootView.findViewById(R.id.shareToWhatsApp).getVisibility() == View.VISIBLE)
                rootView.findViewById(R.id.shareToWhatsApp).setVisibility(View.GONE);
            if (rootView.findViewById(R.id.hideCanvas).getVisibility() == View.GONE)
                rootView.findViewById(R.id.hideCanvas).setVisibility(View.VISIBLE);
            if (rootView.findViewById(R.id.save_IV).getVisibility() == View.VISIBLE)
                rootView.findViewById(R.id.save_IV).setVisibility(View.GONE);
        } else {
            editText.setHint("");
            if (rootView.findViewById(R.id.addToMoment_IV).getVisibility() == View.GONE)
                rootView.findViewById(R.id.addToMoment_IV).setVisibility(View.VISIBLE);
            if (rootView.findViewById(R.id.addToStream).getVisibility() == View.GONE)
                rootView.findViewById(R.id.addToStream).setVisibility(View.VISIBLE);
            if (rootView.findViewById(R.id.shareToWhatsApp).getVisibility() == View.GONE)
                rootView.findViewById(R.id.shareToWhatsApp).setVisibility(View.VISIBLE);
            if (rootView.findViewById(R.id.hideCanvas).getVisibility() == View.VISIBLE)
                rootView.findViewById(R.id.hideCanvas).setVisibility(View.GONE);
            if (rootView.findViewById(R.id.save_IV).getVisibility() == View.GONE)
                rootView.findViewById(R.id.save_IV).setVisibility(View.VISIBLE);
        }
    }

    FrameLayout frameLayout;
    ScribbleView scribbleView;

    /**
     * @return true if the back button was handled successfully
     * false otherwise
     */
    public boolean handleBack() {
        if (!isColorPickerOpen())
            return false;
        else toggleColorPicker();
        return true;
    }

    OvalColorPicker ovalColorPicker;

    public boolean isColorPickerOpen() {
        return ovalColorPicker.getVisibility() == View.VISIBLE;
    }

    public View getCloseButtonReference() {
        return rootView.findViewById(R.id.closeButton);
    }

    public boolean isCanvasLocked() {
        if (editText.isFocused() || isColorPickerOpen() || editText.getText().toString().length() != 0 || scribbleView.getPathListSize() != 0)
            return true;
        else return false;
    }

    private View progressView;

    public boolean isUiBlocked() {
        return progressView != null && progressView.getVisibility() == View.VISIBLE;
    }

    public void toggleColorPicker() {
        if (isColorPickerOpen())
            ovalColorPicker.setVisibility(View.GONE);
        else ovalColorPicker.setVisibility(View.VISIBLE);
        scribbleView.setDrawingEnabled(isColorPickerOpen());
    }

    private void initColorEditor(View rootView) {
        ovalColorPicker = (OvalColorPicker) rootView.findViewById(R.id.ovalColorPicker);
        ovalColorPicker.setColorPickerListener(new OvalColorPicker.ColorPickerListener() {
            @Override
            public void onColorSelected(int colorInt) {
                scribbleView.setPaintColor(colorInt);
            }

            @Override
            public int onUndoClicked() {
                return scribbleView.undo();
            }
        });

        rootView.findViewById(R.id.save_IV).setOnClickListener(this);
    }

    public Bitmap getBitmap() {
        bitmapView.setDrawingCacheEnabled(true);
        bitmapView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        bitmapView.buildDrawingCache(true);
        Bitmap bmp = Bitmap.createBitmap(bitmapView.getDrawingCache());
        bitmapView.setDrawingCacheEnabled(false);
        return bmp;
    }

    public void toggleEditTextProperties() {
        defaultTextState = !defaultTextState;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) editText.getLayoutParams();
        editText.setGravity(defaultTextState ? Gravity.LEFT : Gravity.CENTER);
//        editText.setTypeface(defaultTextState? Typeface.SANS_SERIF : Typeface.SERIF);
        if (!defaultTextState)
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
        else
            params.removeRule(RelativeLayout.CENTER_IN_PARENT);

        editText.setLayoutParams(params);
    }

    public void applyRandomBackground(boolean next) {
        if (next)
            backgroundCount = ((backgroundCount + 1) % 4);
        else
            backgroundCount = ((backgroundCount - 1) % 4);

        if (backgroundCount < 0)
            backgroundCount = backgroundCount + 4;

        int background;
        switch (backgroundCount) {
            case 0:
                background = R.drawable.canvas_background_default;
                break;
            case 1:
                background = R.drawable.canvas_background_1;
                break;
            case 2:
                background = R.drawable.canvas_background_2;
                break;
            case 3:
                background = R.drawable.canvas_background_3;
                break;
//            case 4:
//                background = R.drawable.canvas_background_default;
//                break;
            default:
                background = R.drawable.canvas_background_default;
                break;
        }

        backGroundColorView.setBackgroundResource(background);
    }


    boolean isSharingToWhatsApp;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeButton:
                undoEditChanges();
                break;
            case R.id.addToMoment_IV:
                if (isColorPickerOpen())
                    toggleColorPicker();
                editText.clearFocus();
                editText.setCursorVisible(false);
                viewControlsCallback.notifyFocus(false);

                gotoShareScreen = true;
                sendEditedImage();
                break;
            case R.id.addToStream:
                if (isColorPickerOpen())
                    toggleColorPicker();
                editText.clearFocus();
                editText.setCursorVisible(false);
                viewControlsCallback.notifyFocus(false);
                rootView.findViewById(R.id.save_IV).performClick();
                gotoShareScreen = false;
                sendEditedImage();
                break;
            case R.id.shareToWhatsApp:
                isSharingToWhatsApp = true;
                rootView.findViewById(R.id.save_IV).performClick();
                break;
            case R.id.hideCanvas:
                undoEditChanges();
                showRevealAnimation(true, revealView, new AnimationListeners() {
                    @Override
                    public void onAnimationStart() {
                    }

                    @Override
                    public void onAnimationEnd() {
                    }
                });
                viewControlsCallback.onHideCanvasFragment();
                break;
            case R.id.save_IV:
                if (isColorPickerOpen())
                    toggleColorPicker();
                editText.clearFocus();
                editText.setCursorVisible(false);
                viewControlsCallback.notifyFocus(false);

                requestSaveEditedMedia();
                break;
        }
    }

    private void requestSaveEditedMedia() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_ACCESS_EXTERNAL_STORAGE);
        } else {
            setImageFilePath(true, getActivity(), AppLibrary.MEDIA_TYPE_IMAGE, "_saved");
            writeFile(getBitmap(), editedFile, true, false, new WriteImageCallback() {
                @Override
                public void onWriteImageComplete() {
                    if (isSharingToWhatsApp)
                        shareMediaToWhatsApp(AppLibrary.MEDIA_TYPE_IMAGE);
                }
            }, !this.isSharingToWhatsApp);

        }
    }

    private void shareMediaToWhatsApp(int mediaType) {
        isSharingToWhatsApp = false;
        launchMediaShareIntent(null, null, AppLibrary.WHATSAPP_PACKAGE_NAME, mediaType, editedFile.getAbsolutePath());

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestSaveEditedMedia(); //Now save the file
                } else {
                    showShortToastMessage(" Please provide access to save to Gallery");
                }
                break;
        }
    }

    public void startRevealAnimation() {
        showRevealAnimation(false, revealView, new AnimationListeners() {
            @Override
            public void onAnimationStart() {

            }

            @Override
            public void onAnimationEnd() {
                editText.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Service.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void sendEditedImage() {
        setImageFilePath(false, getActivity(), AppLibrary.MEDIA_TYPE_IMAGE, "_share");

        if (gotoShareScreen) {
            viewControlsCallback.launchShareFragment(editedFile.getAbsolutePath(), AppLibrary.checkStringObject(editText.getText().toString()));
            writeFile(getBitmap(), editedFile, false, false, null);
        } else {
            writeFile(getBitmap(), editedFile, false, false, new WriteImageCallback() {
                @Override
                public void onWriteImageComplete() {
                    String mediaText = null;
                    if (AppLibrary.checkStringObject(editText.getText().toString()) != null) {
                        mediaText = editText.getText().toString();
                    }
                    HashMap<String, Integer> selectedMomentList = new HashMap<>();
                    selectedMomentList.put(mFireBaseHelper.getMyUserModel().momentId, MY_MOMENT);
                    viewControlsCallback.uploadMediaToFireBase(false, false, PrivacyPopup.ALL_FRIEND_ROW, null, editedFile.getAbsolutePath(), selectedMomentList, null, 0, mediaText);
                    Log.d(TAG, "onWriteImageComplete: ");
                }
            });
        }
    }

    public void undoEditChanges() {
        if (isColorPickerOpen())
            toggleColorPicker();
        scribbleView.undoCompleteCanvas();
        if (!defaultTextState)
            toggleEditTextProperties();
        editText.setText("");
        editText.setHint("What's on your mind?");
        viewControlsCallback.notifyFocus(false);
        editText.clearFocus();
        editText.setCursorVisible(false);
        backGroundColorView.setBackgroundResource(R.drawable.canvas_background_default);
        hideKeyboard();
        viewControlsCallback.notifyFocus(false);
    }

    public void hideKeyboard() {
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public void showKeyBoard() {
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    EditText tutorialText;

    private void addTutorialText() {
        tutorialText = new EditText(getActivity());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 0;
        params.topMargin = 400;
        relativeLayout.addView(tutorialText, params);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                relativeLayout.removeView(editText);
            }
        }, 2000);
    }

    public interface ViewControlsCallback {
        void notifyFocus(boolean hasFocus);

        void onHideCanvasFragment();

        void toggleUnseenTextViewVisibility(boolean hide);

        void launchShareFragment(String absolutePath, String s);

        void uploadMediaToFireBase(boolean facebookPost, boolean postAnonymous, int action_type, HashMap<String, String> selectedRoomsForMoment, String mediaPath, HashMap<String, Integer> momentList
                , HashMap<String, Integer> roomList, int expiryType, String mediaText);
    }

    private class CustomGestureListener implements GestureDetector.OnGestureListener {

        private static final int SWIPE_THRESHOLD = 70;
        private static final int SWIPE_VELOCITY_THRESHOLD = 30;


        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (editText.isFocused()) {
//                if (isCanvasLocked())
//                    viewControlsCallback.notifyFocus(false);
                Rect outRect = new Rect();
                editText.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) e.getRawX(), (int) e.getRawY())) {
                    editText.clearFocus();
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                } else
                    return false; //Pass onto edit text to handle it - for editing, etc
            } else {
                if (!((CameraActivity) getActivity()).isScrolling) {
                    editText.requestFocus();
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
//                    viewControlsCallback.notifyFocus(true);
                }
            }
            return true;
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
                            result = false;
                        } else {
                            result = false;
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
            }
            return result;
        }

        public void onSwipeRight() {
            if (isCanvasLocked())
                applyRandomBackground(false);
        }

        public void onSwipeLeft() {
            if (isCanvasLocked())
                applyRandomBackground(true);
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }
    }
}
