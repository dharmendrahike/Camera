package com.pulseapp.android.broadcast;

import android.hardware.Camera;

import com.pulseapp.android.activities.CameraActivity;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by admin on 1/20/2015.
 */
public class FilterManager implements Serializable {

    private String colorEffect;
    private String flashMode;

    /* can be static, these values wont change */
    private static ArrayList<String> availColorEffects = new ArrayList<>();
    private static ArrayList<String> availFlashModes = new ArrayList<>();

    private static ArrayList<String> defaultAvailColorEffects = new ArrayList<>();

    static {
        defaultAvailColorEffects.add("Natural");
        defaultAvailColorEffects.add("Rich");
        defaultAvailColorEffects.add("Mono");
        defaultAvailColorEffects.add("Night");
        defaultAvailColorEffects.add("Vignette");
        defaultAvailColorEffects.add("Mirror");
        defaultAvailColorEffects.add("Bulge");
        defaultAvailColorEffects.add("NashVille");
        defaultAvailColorEffects.add("X-pro");
        defaultAvailColorEffects.add("Amaro");
        defaultAvailColorEffects.add("Hudson");
        defaultAvailColorEffects.add("Valencia");
        defaultAvailColorEffects.add("Toon");
        defaultAvailColorEffects.add("Sketch");
        defaultAvailColorEffects.add("InkWell");
        defaultAvailColorEffects.add("Hefe");
        defaultAvailColorEffects.add("Brannen");
        defaultAvailColorEffects.add("Moon");
        defaultAvailColorEffects.add("Fast-Forward");
        defaultAvailColorEffects.add("Slow-Mo");
    }

    public static final int FILTER_NATURAL = 0;
    public static final int FILTER_RICH = 1;
    public static final int FILTER_MONO = 2;
    public static final int FILTER_NIGHT = 3;
    public static final int FILTER_VIGNETTE = 4;
    public static final int FILTER_MIRROR = 5;
    public static final int FILTER_BULGE = 6;
    public static final int FILTER_NASHVILLE=7;
    public static final int FILTER_XPRO=8;
    public static final int FILTER_AMARO = 9;
    public static final int FILTER_HUDSON=10;
    public static final int FILTER_VELENCIA=11;
    public static final int FILTER_TOON = 12;
    public static final int FILTER_SKETCH = 13;
    public static final int FILTER_INKWELL=14;
    public static final int FILTER_HEFE=15;
    public static final int FILTER_BRANNAN=16;
    public static final int FILTER_Moon=17;



    public static final int FILTER_COUNT = 18;

    private static int FILTER_RUNNING_COUNT = 17;

    public static final int FILTER_BLUR = ++FILTER_RUNNING_COUNT;
    public static final int FILTER_SHARP= ++FILTER_RUNNING_COUNT;

    public static void initialize(Camera.Parameters params) {
        
        availColorEffects.clear();
        for(String val : defaultAvailColorEffects) {
            availColorEffects.add(val);
        }

        availFlashModes.clear();

        availFlashModes.add("torch");
        availFlashModes.add("off");

    }
    
    public static void destroy() {
        availColorEffects.clear();

        availFlashModes.clear();
    }

    public static FilterManager getInstance (Camera.Parameters params) {
        initialize(params);
        return new FilterManager(params);
    }

    public static FilterManager getInstance(FilterManager filterManager) {
        if (filterManager == null)
            throw new NullPointerException("filterManager should not be null");
        return new FilterManager(filterManager);
    }

    private FilterManager (Camera.Parameters params) {

        this.colorEffect  =   defaultAvailColorEffects.get(0);

        if(CameraActivity.flashMode)
            this.flashMode    =   params.getFlashMode();
    }
    
    private FilterManager (FilterManager fm) {
        this.colorEffect   = fm.colorEffect;
        this.flashMode     = fm.flashMode;
    }

    public void setColorEffect(String colorEffect) {
        if (!availColorEffects.contains(colorEffect)) {
            throw new IllegalStateException(colorEffect + " is unknown, should be selected from available settings");
        }
        this.colorEffect = colorEffect;
    }

    public void setFlashMode(String flashMode) {
        if (!availFlashModes.contains(flashMode)) {
            throw new IllegalStateException(flashMode + " is unknown, should be selected from available settings");
        }
        this.flashMode = flashMode;
    }

    public String rotateFlashMode () {
        int index = availFlashModes.indexOf(flashMode);
        index = (index + 1)%availFlashModes.size();
        setFlashMode(availFlashModes.get(index));
        return flashMode;
    }

    public String getColorEffect() {
        return colorEffect;
    }

    public String getFlashMode() {
        return flashMode;
    }

    public ArrayList<String> getAvailColorEffects () {
        return (ArrayList<String>) availColorEffects.clone();
    }

    public static String getColorEffectName(int index) {
        if(availColorEffects == null || availColorEffects.size() == 0) return null;
        return availColorEffects.get(index);
    }

    public ArrayList<String> getAvailFlashModes () {
        return (ArrayList<String>) availFlashModes.clone();
    }
}
