/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pulseapp.android.broadcast;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;

import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.util.AppLibrary;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
@TargetApi(18)
public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private Callback callback;
    private static final boolean VERBOSE = false;
    private static final boolean SHOW_FRAME_LOGS = false;

    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // 1 seconds between I-frames
    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private long lastTS = 0;
    private long frameCounter = 0;
    private long remainingTS = 0;
    private int RESYNC_STATE = 0;
    //Defining RESYNC_STATE
    //STATE = 0 if the streaming is about to start and its the first frame
    //STATE = 1 if the streaming is stable
    //STATE = 2 if the streaming timestamps need to reset after creating new surface Texture

    private long tsDiffRem = 0;

    private byte[] mSPS = null;
    private byte[] mStoredSPS = null;
    private String mB64SPS = null;
    private boolean spsSent = false;

    private byte[] mPPS = null;
    private byte[] mStoredPPS = null;
    private String mB64PPS = null;
    private boolean ppsSent = false;

    private SharedPreferences mPreferences = null;
    private String mPreferencesKeyPrefix = null;

    private int mWidth, mHeight;
    private int mErrorCount = 0;

    public static interface Callback {
        void onVideoNal(byte[] bArr, long j, long j2);
        void onVideoError(String e);
    }

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public VideoEncoderCore(int width, int height, int bitRate, int frameRate)
            throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();

        mPreferencesKeyPrefix = "Encoder-"+
                width+
                "x"+
                height+
                "-"+
                bitRate+
                "-"+
                IFRAME_INTERVAL+
                "-"+
                frameRate+
                "-";
        mPreferences = CameraActivity.getApplicationContextPublic().getSharedPreferences(AppLibrary.BroadCastSettings, Context.MODE_PRIVATE);
        mB64SPS = mPreferences.getString(mPreferencesKeyPrefix + "sps", null);
        mB64PPS = mPreferences.getString(mPreferencesKeyPrefix+"pps", null);
        if(mB64PPS!=null) {
            if(VERBOSE) Log.d(TAG, "Read SPS from SharedPreferences");
            mStoredSPS = Base64.decode(mB64SPS, Base64.NO_WRAP);
        }
        if(mB64PPS!=null) {
            if(VERBOSE) Log.d(TAG, "Read PPS from SharedPreferences");
            mStoredPPS = Base64.decode(mB64PPS, Base64.NO_WRAP);
        }

        mWidth = width;
        mHeight = height;

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        //Commented until we find better use for it
//        selectCodec(MIME_TYPE);

     try {

             // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
             // we can use for input and wrap it with a class that handles the EGL work.
             mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
             Log.d(TAG, "Encoder name is: " + mEncoder.getName() + " Codec Info is: " + mEncoder.getCodecInfo().getName());

            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();

             // Encoder info can only be queried after starting it.
             // So we start it. Check whether it is fucking mediaTek, adjust the width/height, stop it and restart it
             //sahil.bajaj@instalively.com

         if (width%16==0 && height%16==0) {
             Log.d(TAG, "Encoder width and height is a multiple of 16, all chill! :)");
         }
         else if (mEncoder.getCodecInfo().getName().toLowerCase().contains("mtk")) {
                mWidth = (width + 0x000F) & 0x7FF0;
                mHeight = (height + 0x000F) & 0x7FF0;
                Log.d(TAG, "MediaTek chipset.. Adjusting width and height to " + mWidth + "x" + mHeight);
                mEncoder.stop();
//                if (mInputSurface!=null) //Not sure if we can release here, To test
//                    mInputSurface.release();

                format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
                format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);
                mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mInputSurface = mEncoder.createInputSurface();
                mEncoder.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to create encoder surface!");
            e.printStackTrace();
        }

    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        spsSent = false;
        ppsSent = false;
        if (mEncoder != null) {
            try {
                mEncoder.stop();
                mEncoder.release();
            }catch (Exception e)
            {
                Log.e(TAG, "Unable to release encoder!");
                e.printStackTrace();
            }
            mEncoder = null;
        }

        if (mInputSurface!=null) {
            mInputSurface.release();
            mInputSurface = null;
        }
    }

    @TargetApi(19)
    public void changeBitrate(int bitrate)
    {
        Bundle params = new Bundle();
        params.putSerializable(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE,bitrate*1024);
        try {
            mEncoder.setParameters(params);
        }catch (Exception e)
        {
            Log.e(TAG, "Unable to change bitrate!");
            e.printStackTrace();
        }
    }

    public void resync()
    {
        RESYNC_STATE = 2;
    }


    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 5000;
        if (VERBOSE && SHOW_FRAME_LOGS) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            try {
                mEncoder.signalEndOfInputStream();
            } catch (IllegalStateException ise) {
                //Codec isn't in executing state
                Log.e(TAG, "Drain encoder error - ISE while calling signalEndOfInputStream! "+ise.getMessage());
            }
        }

        ByteBuffer[] encoderOutputBuffers;
        try {
            encoderOutputBuffers = mEncoder.getOutputBuffers();
        }catch (Exception e) {
            Log.e(TAG, "Get output buffers failed!");
            e.printStackTrace();
            return;
        }

        while (true) {
            int encoderStatus = 0;
            try {
                encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            }catch (Exception e) {
                Log.e(TAG, "Encoder dequeueOutputBuffer exception");
                e.printStackTrace();
                mErrorCount++;
                if(mErrorCount >= 10) {
                    if (VERBOSE)
                        Log.d(TAG, "Encoder dequeueOutputBuffer exception occurred multiple times. Exiting");
                    break;
                }
                continue;
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    mErrorCount++;
                    if(mErrorCount >= 25) {
                        if (VERBOSE)
                            Log.d(TAG, "no output available, Not spinning anymore");
                        break;
                    }
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder

                try {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                }catch (Exception e) {
                    Log.e(TAG, "Get output buffers failed!");
                    e.printStackTrace();
                    continue;
                }

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mErrorCount = 0;
                spsSent = false;
                ppsSent = false;
                MediaFormat newFormat = mEncoder.getOutputFormat();

                Editor editor = mPreferences.edit();
                // The SPS and PPS should be there
                MediaFormat format = mEncoder.getOutputFormat();
                ByteBuffer spsb = format.getByteBuffer("csd-0");
                ByteBuffer ppsb = format.getByteBuffer("csd-1");
                mSPS = new byte[spsb.capacity()-4];
                spsb.position(4);
                spsb.get(mSPS, 0, mSPS.length);

                mPPS = new byte[ppsb.capacity()-4];
                ppsb.position(4);
                ppsb.get(mPPS,0,mPPS.length);

                mB64SPS = Base64.encodeToString(mSPS, 0, mSPS.length, Base64.NO_WRAP);
                if (mB64SPS != null && !mB64SPS.isEmpty())
                    editor.putString(mPreferencesKeyPrefix + "-sps", mB64SPS);
                mB64PPS = Base64.encodeToString(mPPS, 0, mPPS.length, Base64.NO_WRAP);
                if (mB64PPS != null && !mB64PPS.isEmpty())
                    editor.putString(mPreferencesKeyPrefix + "-pps", mB64PPS);

                if ((mB64SPS != null && !mB64SPS.isEmpty()) || (mB64PPS != null && !mB64PPS.isEmpty())) {
                    if(VERBOSE) Log.d(TAG, "Storing SPS-PPS in SharedPreferences");
                    editor.commit();
                }

                int type = mSPS[0]&31;
                if(type==7) {
                    if (VERBOSE) Log.d(TAG, "SPS found!");
                    handleVideoNal(mSPS, 1);
                    spsSent = true;
                }

                type = mPPS[0]&31;
                if(type==8) {
                    if (VERBOSE) Log.d(TAG, "PPS found!");
                    handleVideoNal(mPPS, 1);
                    ppsSent = true;
                }
                Log.d(TAG, "encoder output format changed: "+""+newFormat);

            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it

            } else {
                mErrorCount = 0;
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    Log.d(TAG, "encoderOutputBuffer " + encoderStatus +
                            " was null");
                    continue;
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    long ts = mBufferInfo.presentationTimeUs;
                    byte[] buffer = new byte[mBufferInfo.size-4];
                    encodedData.position(encodedData.position()+4);
                    encodedData.get(buffer, 0, buffer.length);
                    encodedData.position(mBufferInfo.offset);

                    long tsDiff = (ts-lastTS);
                    if(RESYNC_STATE == 0) {  //New surfaceTextureTimestamp, avoiding random remainder and tsDiff calculations
                        Log.d(TAG, "New surfaceTexture created for the first time. Ignore remainder timestamps calculation");
                        tsDiff = 1; //Send the first frame with a timestamp difference of 1
                        RESYNC_STATE++; //Setting RESYNC_STATE to normal state
                    }
                    else if (RESYNC_STATE == 2)  //New surface Texture after pressing home button
                    {
                        Log.d(TAG, "New surfaceTexture created after pressing home button. Ignore this frame");
                    }
                    else {
                        tsDiffRem = tsDiff % 1000;
                        tsDiff /= 1000;
                        remainingTS += tsDiffRem;
                        if (remainingTS >= 1000) {
                            tsDiff++;
                            remainingTS -= 1000;
                        }
                    }
                    if(!spsSent) {
                        if(mStoredSPS!=null) {
                            if (VERBOSE) Log.d(TAG, "SPS found! From SharedPreferences");
                            handleVideoNal(mStoredSPS, 1);
                            spsSent = true;
                        } else {
                            Log.e(TAG, "Some error! SPS not found for encoder. Should we stop?");
                            handleVideoError("Unable to retrieve PPS from encoder!");
                        }
                    }
                    if(!ppsSent) {
                        if (mStoredPPS != null) {
                            if (VERBOSE) Log.d(TAG, "PPS found! From SharedPreferences");
                            handleVideoNal(mStoredPPS, 1);
                            ppsSent = true;
                        } else {
                            Log.e(TAG, "Some error! PPS not found for encoder. Should we stop?");
                            handleVideoError("Unable to retrieve PPS from encoder!");
                        }
                    }

                    if(RESYNC_STATE == 2)
                        RESYNC_STATE = 1;   //Skip one video frame after pressing home button to reset benchmark timestamp
                    else
                        handleVideoNal(buffer, tsDiff);

                    int type = buffer[0] & 31;

                    if (VERBOSE && SHOW_FRAME_LOGS) {
                        Log.d(TAG, "sent " + buffer.length +" bytes "+frameCounter + " to muxer, ts=" +
                                ts + " Diff "+tsDiff+" "+tsDiffRem+" "+ remainingTS + " "+type + "Flag "+mBufferInfo.flags);
                    }
                    lastTS = ts;
                    frameCounter++;
                }

                try {
                    mEncoder.releaseOutputBuffer(encoderStatus, false);
                }catch (Exception e) {
                    Log.e(TAG, "Release output buffers failed!");
                    e.printStackTrace();
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    byte [] buffer = new byte[1];
                    buffer[0] = 9; // TYPE EOS
                    handleVideoNal(buffer, 30);
                    break;      // out of while
                }
            }
        }
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }


    private void selectCodec(String mimeType) {
    int numCodecs = MediaCodecList.getCodecCount();
    for (int i = 0; i < numCodecs; i++) {
        MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

        if (!codecInfo.isEncoder()) {
            continue;
        }

        String[] types = codecInfo.getSupportedTypes();
        MediaCodecInfo[] supportedCodecInfoList = null;
        for (int j = 0; j < types.length; j++) {
            if (types[j].equalsIgnoreCase(mimeType)) {
                Log.d(TAG, "Encoder name: " + codecInfo.getName());
                String[] supported = codecInfo.getSupportedTypes();
                for (int l = 0; l < supported.length; l++) {
                    Log.d(TAG, "Encoder supported types list: " + supported[l]);
                }

                MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d(TAG, "Is constant bitrate mode supported? " + codecCapabilities.getEncoderCapabilities().isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR));
                }
                MediaCodecInfo.CodecProfileLevel[] profileLevels = codecCapabilities.profileLevels;
                Log.d(TAG, "Supported profile levels are " + profileLevels.toString());
//                supportedCodecInfoList[j] = codecInfo;
            }
        }
    }


    }

    void handleVideoNal(byte[] buffer, long ts) {
        if(this.callback != null) {
            this.callback.onVideoNal(buffer, ts, ts);
        }
    }
    void handleVideoError(String message) {
        if(this.callback != null) {
            this.callback.onVideoError(message);
        }
    }
}
