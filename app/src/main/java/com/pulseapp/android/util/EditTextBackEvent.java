package com.pulseapp.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Created by deepankur on 19/3/16.
 */
public class EditTextBackEvent extends EditText {

    private EditTextImeBackListener mOnImeBack;
    private boolean enableImeDone = false;
//    private Handler handler = new Handler();


    public EditTextBackEvent(Context context) {
        super(context);
        init();
    }

    public EditTextBackEvent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextBackEvent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.setCustomSelectionActionModeCallback(new ActionModeCallbackInterceptor());
        this.setLongClickable(true);
        this.setTextIsSelectable(true);
    }


    boolean canPaste()
    {
        return false;
    }

    @Override
    public boolean isSuggestionsEnabled()
    {
        return false;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mOnImeBack != null) mOnImeBack.onImeBack(this, this.getText().toString());
        }
        return super.dispatchKeyEvent(event);
    }

    public void setOnEditTextImeBackListener(EditTextImeBackListener listener) {
        mOnImeBack = listener;
    }

    public void setEnableImeDone(boolean enable){
        enableImeDone = enable;
    }

    public interface EditTextImeBackListener {
        public abstract void onImeBack(EditTextBackEvent ctrl, String text);
    }

    private class ActionModeCallbackInterceptor implements ActionMode.Callback
    {
        private final String TAG = NoMenuEditText.class.getSimpleName();

        public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
        public void onDestroyActionMode(ActionMode mode) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            // setInsertionDisabled when user touches the view
//            this.setInsertionDisabled();
//            setSelection(getSelectionStart());
//        }
//        else if (event.getAction() == MotionEvent.ACTION_UP && this.getSelectionStart()!=this.getSelectionEnd()) {
//            // Something has been selected, don't pass the touch event further to prevent unselection
//            final int start = this.getSelectionStart();
//            final int end = this.getSelectionEnd();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    setSelection(start, end);
//                }
//            }, 30);
//        }

        return super.onTouchEvent(event);
    }

    /**
     * This method sets TextView#Editor#mInsertionControllerEnabled field to false
     * to return false from the Editor#hasInsertionController() method to PREVENT showing
     * of the insertionController from EditText
     * The Editor#hasInsertionController() method is called in  Editor#onTouchUpEvent(MotionEvent event) method.
     */

    private void setInsertionDisabled() {
        try {
            Field editorField = TextView.class.getDeclaredField("mEditor");
            editorField.setAccessible(true);
            Object editorObject = editorField.get(this);

            Class editorClass = Class.forName("android.widget.Editor");
            Field mInsertionControllerEnabledField = editorClass.getDeclaredField("mInsertionControllerEnabled");
            mInsertionControllerEnabledField.setAccessible(true);
            mInsertionControllerEnabledField.set(editorObject, false);
        }
        catch (Exception ignored) {
            // ignore exception here
        }
    }

}

