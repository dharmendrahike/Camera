package com.pulseapp.android.broadcast;

/**
 * Created by bajaj on 7/9/15.
 */
public interface PreviewModeChangeListener {
    void setFilter(int filter, boolean enable);
    void triggerFilterThumbnail(int filterBegin, int filterEnd);
    void stopFilterThumbnail();
}
