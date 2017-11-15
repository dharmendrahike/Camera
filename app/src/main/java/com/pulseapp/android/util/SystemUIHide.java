package com.pulseapp.android.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by admin on 1/20/2015.
 */
public class SystemUIHide {
    
    private static final String TAG = "I/SystemUIHider";
    
    /**
     * Whether or not the system UI is currently visible. This is a cached value
     * from calls to {@link #hide()} and {@link #show()}.
     */
    private boolean mVisible = true;
    
    private Activity mContext;
    
    private int mShowFlags;
    private int mHideFlags;
    
    public SystemUIHide(Activity context) {
        this.mContext = context;

        mShowFlags = View.SYSTEM_UI_FLAG_VISIBLE;
        mHideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mShowFlags |=
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            
        }
    }
    
    public void setup() {
        mContext.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    public boolean isVisible() {
        return mVisible;
    }
    
    public void hide() {
        if(!isVisible()) 
            return;
        
    }
    
    public void show() {
        if (isVisible())
            return;
        
    }
    
    public void toggle () {
        if (isVisible()){
            hide();
        } else {
            show();
        }
    }

    /**
     * A callback interface used to listen for system UI visibility changes.
     */
    public interface OnVisibilityChangeListener {
        /**
         * Called when the system UI visibility has changed.
         *
         * @param visible True if the system UI is visible.
         */
        public void onVisibilityChange(boolean visible);
    }

    /**
     * Detects and toggles immersive mode.
     */
    public static void toggleHideyBar(Activity context) {
        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = context.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            AppLibrary.log_i(TAG, "Turning immersive mode mode off. ");
        } else {
            AppLibrary.log_i(TAG, "Turning immersive mode mode on.");
        }

        // Immersive mode: Backward compatible to KitKat (API 19).
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // This sample uses the "sticky" form of immersive mode, which will let the user swipe
        // the bars back in again, but will automatically make them disappear a few seconds later.
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        context.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
    
    public static void turnImmersiveMode(Activity context) {
        int uiOptions = context.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        
        if ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions)
            return;

        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        context.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
