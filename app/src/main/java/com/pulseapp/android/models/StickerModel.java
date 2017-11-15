package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by deepankur on 6/2/16.
 */
public class StickerModel implements Serializable{

    public String stickerId;//for Internal reference
    public int degree;
    public float height;
    public float marginLeft;
    public float marginTop;
    public float width;
    public String name;
    public String pictureUrl;
    public int status;
    public int type;
    //local uri will represent asset in case the image was shipped with app and internal directory otherwise
    public String localUri;//for internal reference only
    public boolean stickerPresentInAssets;//for internal reference
    public HashMap<String,Integer>keywords;
    static final long serialVersionUID =6242834083400357545L;

    public StickerModel() {
    }
}
