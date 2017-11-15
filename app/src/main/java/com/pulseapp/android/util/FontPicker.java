package com.pulseapp.android.util;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by deepankur on 6/24/16.
 */
public class FontPicker {

    private static FontPicker fontPicker;

    public static FontPicker getInstance(Context context) {
        if (fontPicker == null)
            fontPicker = new FontPicker(context);
        return fontPicker;
    }

    private Typeface museo700,
            museo500,
            MontserratBold,
            MontserratLight,
            MontserratRegular,
            MontserratSemiBold,
            Museo300Regular,
            Museo500Regular,
            Museo700Regular;

    private FontPicker(Context context) {
        museo700 = Typeface.createFromAsset(context.getAssets(), "fonts/Museo700-Regular.otf");
        museo500 = Typeface.createFromAsset(context.getAssets(), "fonts/Museo500-Regular.otf");
        MontserratBold = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Bold.otf");
        MontserratLight = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Light.otf");
        MontserratRegular = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.otf");
        MontserratSemiBold = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-SemiBold.otf");
        Museo300Regular = Typeface.createFromAsset(context.getAssets(), "fonts/Museo300-Regular.otf");
        Museo500Regular = Typeface.createFromAsset(context.getAssets(), "fonts/Museo500-Regular.otf");
        Museo700Regular = Typeface.createFromAsset(context.getAssets(), "fonts/Museo700-Regular.otf");
    }

    public Typeface getMuseo700() {
        return museo700;
    }

    public Typeface getMuseo500() {
        return museo500;
    }

    public Typeface getMontserratBold() {
        return MontserratBold;
    }

    public Typeface getMontserratLight() {
        return MontserratLight;
    }

    public Typeface getMontserratRegular() {
        return MontserratRegular;
    }

    public Typeface getMontserratSemiBold() {
        return MontserratSemiBold;
    }

    public Typeface getMuseo300Regular() {
        return Museo300Regular;
    }

    public Typeface getMuseo500Regular() {
        return Museo500Regular;
    }

    public Typeface getMuseo700Regular() {
        return Museo700Regular;
    }
}
