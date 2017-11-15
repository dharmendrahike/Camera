package com.pulseapp.android.util;

/**
 * Created by Karthik on 5/10/16.
 */

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import com.pulseapp.android.activities.CameraActivity;

import java.lang.reflect.Field;


/**
 *  This is a thin veneer over EditText, with copy/paste/spell-check removed.
 */
public class NoMenuEditText extends EditText
{
    private final Context context;
    private Handler handler = new Handler();

    /** This is a replacement method for the base TextView class' method of the same name. This
     * method is used in hidden class android.widget.Editor to determine whether the PASTE/REPLACE popup
     * appears when triggered from the text insertion handle. Returning false forces this window
     * to never appear.
     * @return false
     */
    boolean canPaste()
    {
        return false;
    }

    /** This is a replacement method for the base TextView class' method of the same name. This method
     * is used in hidden class android.widget.Editor to determine whether the PASTE/REPLACE popup
     * appears when triggered from the text insertion handle. Returning false forces this window
     * to never appear.
     * @return false
     */
    @Override
    public boolean isSuggestionsEnabled()
    {
        return false;
    }

    public NoMenuEditText(Context context)
    {
        super(context);
        this.context = context;
        init();
    }

    public NoMenuEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        init();
    }

    public NoMenuEditText(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        this.setCustomSelectionActionModeCallback(new ActionModeCallbackInterceptor());
        this.setLongClickable(true);
        this.setTextIsSelectable(true);
    }


    /**
     * Prevents the action bar (top horizontal bar with cut, copy, paste, etc.) from appearing
     * by intercepting the callback that would cause it to be created, and returning false.
     */
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // setInsertionDisabled when user touches the view
            this.setInsertionDisabled();
            setSelection(getSelectionStart());
        }
        else if (event.getAction() == MotionEvent.ACTION_UP && this.getSelectionStart()!=this.getSelectionEnd()) {
            // Something has been selected, don't pass the touch event further to prevent unselection
            final int start = this.getSelectionStart();
            final int end = this.getSelectionEnd();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSelection(start, end);
                }
            }, 30);
        }

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

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP) {

            ((CameraActivity)context).minimizeEditor();

            return false;
        }
        return super.dispatchKeyEvent(event);
    }
}