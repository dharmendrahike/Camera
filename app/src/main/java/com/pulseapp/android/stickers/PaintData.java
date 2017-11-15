package com.pulseapp.android.stickers;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by deepankur on 18/2/16.
 */
public class PaintData {
    Path path;
    Paint paint;
    ScribbleView.ToolType toolType;

    public PaintData() {
    }

    public PaintData(Path path, Paint paint, ScribbleView.ToolType toolType) {
        this.paint = paint;
        this.path = path;
        this.toolType=toolType;
    }
}
