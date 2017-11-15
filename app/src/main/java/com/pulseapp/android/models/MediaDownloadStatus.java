//package com.pulseapp.android.models;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.pulseapp.android.util.AppLibrary;
//
///**
// * Created by deepankur on 8/1/16.
// */
//
//@JsonIgnoreProperties(ignoreUnknown = AppLibrary.IGNORE_UNRECOGNIZED_FIELD)
//public class MediaDownloadStatus {
//    //        public DynamicDownloader.DownloaderType downloaderType;
//    public String localUri;
//    public String url;
//    public int downloadStatus;
//    public String mediaId;//internalReferenceOnly
//
//
//    public MediaDownloadStatus(String mediaId, String url, String localUri, int downloadStatus) {
//        this.mediaId = mediaId;
//        this.url = url;
//        this.localUri = localUri;
//        this.downloadStatus = downloadStatus;
//    }
//}