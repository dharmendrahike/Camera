package com.pulseapp.android.models;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by deepankur on 11/21/16.
 */

public class StickerSerializedData implements java.io.Serializable {
    public HashMap<String, LinkedHashMap<String, LinkedHashMap<String, StickerModel>>> categoryChatCameraStickerMap;
    public LinkedHashMap<String, StickerCategoryModel> categoryModelMap;
    static final long serialVersionUID = 3845460155424136712L;

    public StickerSerializedData() {
    }
}