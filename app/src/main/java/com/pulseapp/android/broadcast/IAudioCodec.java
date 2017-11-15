package com.pulseapp.android.broadcast;

import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;

import com.pulseapp.android.util.AppLibrary;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by admin on 1/15/2015.
 *
 * I suggest don't change any code or flow in here.
 */
class IAudioCodec implements Runnable, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private static String TAG;
    private int audioBitRate;
    private int audioChannels;
    private boolean isHeadsetOn;
    private int audioSamplingRate;
    private Callback callback;
    private boolean headerSent;
    private MediaRecorder mediaRecorder;
    private ParcelFileDescriptor[] pipe;
    private Thread thread;
    
    public static interface Callback {
        void onAudioError(String str);

        void onAudioFrame(byte[] bArr, long j);

        void onAudioHeader(byte[] bArr, int i, int i2, int i3);
    }

    static {
        TAG = "IAudioCode";
    }

    IAudioCodec(int chan, int srate, int brate, boolean isHeadsetOn) {
        this.audioChannels = chan;
        this.audioSamplingRate = srate;
        this.audioBitRate = brate;
        this.isHeadsetOn = isHeadsetOn;
        AppLibrary.log_i(TAG, "configuration: " + "audioChannels: " + this.audioChannels + ", audioSampleRate: " + this.audioSamplingRate + ", audioBitRate: " + this.audioBitRate);
    }

    public void onError(MediaRecorder m, int what, int extra) {
        if (this.callback != null) {
            this.callback.onAudioError(String.format("Audio error %d/%d", new Object[]{Integer.valueOf(what), Integer.valueOf(extra)}));
        }
    }

    public void onInfo(MediaRecorder m, int what, int extra) {
        AppLibrary.log_d(TAG, String.format("Audio info: what=%d, extra=%d", new Object[]{Integer.valueOf(what), Integer.valueOf(extra)}));
    }


//    public void prepare() throws IOException {
//        this.mediaRecorder = new MediaRecorder();
//        this.mediaRecorder.setOnErrorListener(this);
//        this.mediaRecorder.setOnInfoListener(this);
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//        this.mediaRecorder.setAudioEncodingBitRate(this.audioBitRate);
//        this.mediaRecorder.setAudioChannels(profile.audioChannels);
//        this.mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
//        this.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
//        this.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        this.pipe = ParcelFileDescriptor.createPipe();
//        this.mediaRecorder.setOutputFile(this.pipe[1].getFileDescriptor());
//        this.mediaRecorder.prepare();
//
//        AppLibrary.log_d(TAG, "Audio channels is: " + profile.audioChannels);
//        AppLibrary.log_d(TAG, "Audio bitrate is: " + profile.audioSampleRate);
//
//
//    }



    public void prepare() throws IOException {
        this.mediaRecorder = new MediaRecorder();
        this.mediaRecorder.setOnErrorListener(this);
        this.mediaRecorder.setOnInfoListener(this);
        if(this.isHeadsetOn) //If headset is on, set source as Default as there is a risk of plugging out the Mic midway the stream and headset might not have Mic
            this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        else
            this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        this.mediaRecorder.setOutputFormat(6);
        if (this.audioChannels != 0) {
            this.mediaRecorder.setAudioChannels(this.audioChannels);
        }
        if (this.audioSamplingRate != 0) {
            this.mediaRecorder.setAudioSamplingRate(this.audioSamplingRate);
        }
        this.mediaRecorder.setAudioEncodingBitRate(this.audioBitRate);
        this.mediaRecorder.setAudioEncoder(3);
        this.pipe = ParcelFileDescriptor.createPipe();
        this.mediaRecorder.setOutputFile(this.pipe[1].getFileDescriptor());
        this.mediaRecorder.prepare();
    }


    public void run() {
        IOException e;
        FileInputStream is = new FileInputStream(this.pipe[0].getFileDescriptor());
        byte[] header = new byte[7];
        int freq = 0;
        long counter = 0;
        while (true) {
            int n = 0;
            int offs = 0;
            while (offs < 7) {
                try {
                    if (!Thread.interrupted()) {
                        n = is.read(header, offs, 7 - offs);
                        if (n <= 0) {
                            AppLibrary.log_d(TAG, "Input stream eof");
                            return;
                        } else {
                            offs += n;
                        }
                    } else {
                        return;
                    }
                } catch (IOException e2) {
                    e = e2;
                    String str = TAG;
                    str = "IO error: %s";
                    Object[] objArr = new Object[1];
                    objArr[0] = e.getMessage();
                    AppLibrary.log_e(str, String.format(str, objArr));
                    if (this.callback != null) {
                        String str2;
                        Object[] objArr2;
                        Callback callback = this.callback;
                        str2 = "Audio IO error: %s";
                        objArr2 = new Object[1];
                        objArr2[0] = e.getMessage();
                        callback.onAudioError(String.format(str2, objArr2));
                    }
                }
            }
            if (!((header[0] & 255) == 255 && (header[0] & 240) == 240)) {
                AppLibrary.log_e(TAG, "Bad ADTS signature");
                if (this.callback != null) {
                    this.callback.onAudioError("Audio signature error");
                }
            }
            int size = (((header[3] & 3) << 11) | ((header[4] & 255) << 3)) | ((header[5] & 224) >> 5);
            if (size < 7 && this.callback != null) {
                AppLibrary.log_e(TAG, "Bad ADTS packet size:" + size);
                this.callback.onAudioError("Audio packet size error");
                return;
            } else if (size != 7) {
                size -= 7;
                byte[] buffer = new byte[size];
                offs = 0;
                while (offs < size) {
                    if (!Thread.interrupted()) {
                        try {
                            n = is.read(buffer, offs, size - offs);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        if (n <= 0) {
                            AppLibrary.log_d(TAG, "Input stream eof");
                            return;
                        } else {
                            offs += n;
                        }
                    } else {
                        return;
                    }
                }
                if (this.callback != null) {
                    try {
                        if (!this.headerSent) {
                            int aot = ((header[2] & 192) >> 6) + 1;
                            int freqIndex = (header[2] & 60) >> 2;
                            int channelConf = ((header[2] & 1) << 2) | ((header[3] & 192) >> 6);
                            int i = 13;
                            int[] freqTable = new int[]{96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};
                            if (freqIndex < 0 || freqIndex >= freqTable.length) {
                                break;
                            }
                            freq = freqTable[freqIndex];
                            int chan = channelConf;
                            String str3 = TAG;
                            String str2 = "AAC aot=%d; freq=%d(%d); chan=%d";
                            Object[] objArr2 = new Object[4];
                            objArr2[0] = Integer.valueOf(aot);
                            objArr2[1] = Integer.valueOf(freqIndex);
                            objArr2[2] = Integer.valueOf(freq);
                            objArr2[3] = Integer.valueOf(channelConf);
                            AppLibrary.log_d(str3, String.format(str2, objArr2));
                            this.callback.onAudioHeader(new byte[]{(byte) ((freqIndex >> 1) | 16), (byte) ((freqIndex << 7) | (channelConf << 3))}, chan, freq, 2);
                            this.headerSent = true;
                        }
                        this.callback.onAudioFrame(buffer, ((1000 * counter) * 1024) / ((long) freq));
                    } catch (Exception e3) {
                        AppLibrary.log_e(TAG, "Processing error");
                        this.callback.onAudioError("Audio processing error");
                    }
                }
                counter++;
            }
        }
        this.callback.onAudioError("Bad AAC frequency index");
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public void start() {
        AppLibrary.log_d(TAG, "Start");
        this.headerSent = false;
        this.mediaRecorder.start();
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void stop() {
        AppLibrary.log_d(TAG, "Stop");
        if (this.mediaRecorder != null) {
            try {
                this.mediaRecorder.stop();
            } catch (Exception e) {
            }
            this.mediaRecorder = null;
        }
        if (this.pipe != null) {
            try {
                this.pipe[0].close();
                this.pipe[1].close();
            } catch (Exception e2) {
            }
            this.pipe = null;
        }
        if (this.thread != null) {
            try {
                this.thread.interrupt();
                this.thread.join(500);
            } catch (Exception e3) {
            }
            this.thread = null;
        }
    }
}
