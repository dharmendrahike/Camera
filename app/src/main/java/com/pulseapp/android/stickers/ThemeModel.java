package com.pulseapp.android.stickers;

public class ThemeModel {
    public ThemeAsset[] mAssets;
    public String mThemeName;
    public String mThemeId;

    public ThemeModel() {
    }

    public int size() {
        return mAssets.length;
    }

    public ThemeAsset[] getAssets() {
        return mAssets;
    }
}
