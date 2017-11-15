package com.pulseapp.android.util;

import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by Karthik on 27/07/16.
 */
public class FocusAnimation extends Animation {

    private AnimatedCircleView circle;

    private float oldAngle;
    private float newAngle;

    public FocusAnimation(AnimatedCircleView circle, int newAngle) {
        this.oldAngle = circle.getAngle();
        this.newAngle = newAngle;
        this.circle = circle;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float angle = oldAngle + ((newAngle - oldAngle) * interpolatedTime);

        circle.setAngle(angle);
        circle.requestLayout();
    }
}
