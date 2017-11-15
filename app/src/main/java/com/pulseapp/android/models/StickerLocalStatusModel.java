package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

/**
 * Created by deepankur on 5/31/16.
 */
public class StickerLocalStatusModel {
    public String localUri;
    public String pictureUrl;
    public int downloadStatus;
    public String stickerId;//internalReferenceOnly

    public StickerLocalStatusModel() {
    }

    public StickerLocalStatusModel(String stickerId, String pictureUrl, String localUri, int downloadStatus) {
        this.stickerId=stickerId;
        this.pictureUrl = pictureUrl;
        this.localUri = localUri;
        this.downloadStatus = downloadStatus;
    }
}