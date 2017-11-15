package com.pulseapp.android.models;


import com.pulseapp.android.util.AppLibrary;

import java.util.HashMap;


/**
 * Created by deepankur on 5/26/16.
 */
public class LocationTemplateModel {
    public Object geo;
    public String name;
    public int status;
    public HashMap<String, LocationSticker> stickers;

    public LocationTemplateModel() {
    }

    public LocationSticker getStickerByIndex(int index) {
        if (stickers == null) return null;
        String[] keySet = stickers.keySet().toArray(new String[stickers.size()]);
        return stickers.get(keySet[index]);

    }

    public static class LocationSticker {
        public int degree;
//        public String id;
        public boolean isTextEnabled;
        public float height;
        public float marginLeft;
        public float marginTop;
        public float width;
        public String mStickerId;
        public String pictureUrl;
        public String localUri;//for internal reference only
        public int downloadStatus;//for internal reference only

        public LocationSticker() {
        }
    }
}
