package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

import java.io.Serializable;

/**
 * Created by deepankur on 6/1/16.
 */
public class StickerCategoryModel implements Serializable{
    public String categoryId;
    public String imageUrl;
    public boolean isActive;
    public String title;
    public int type;
    public boolean imagePresentInAssets;
    public String localUri;//for image
    static final long serialVersionUID =-6473921767599615423L;

    public StickerCategoryModel(String categoryId, boolean isActive, String title) {
        this.categoryId = categoryId;
        this.isActive = isActive;
        this.title = title;
    }

    public StickerCategoryModel() {
    }
}
