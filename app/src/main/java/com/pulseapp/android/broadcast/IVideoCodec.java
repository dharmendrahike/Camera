package com.pulseapp.android.broadcast;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.view.Surface;
import com.pulseapp.android.util.AppLibrary;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by admin on 1/15/2015.
 * 
 * I suggest don't change any code or flow in here.
 */
class IVideoCodec implements Runnable, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {
    private static String TAG;
    private long baseTimestamp;
    private Callback callback;
    private Camera camera;
    private int frameRate;
    private boolean hasVideo;
    private int height;
    private Map<Integer, TSStream> map;
    private MediaRecorder mediaRecorder;
    private int patPid;
    private ParcelFileDescriptor[] pipe;
    private int pmtPid;
    private Surface surface;
    private Thread thread;
    private int videoBitRate;
    private int videoPid;
    private int width;

    public static interface Callback {
        void onVideoError(String str);

        void onVideoNal(byte[] bArr, long j, long j2);
    }

    private class FrameTime {
        public long dts;
        public long pts;

        private FrameTime() {
            this.pts = 0;
            this.dts = 0;
        }
    }

    private class TSStream {
        public long pid;
        public int size;
        public ByteArrayOutputStream stream;

        TSStream() {
            this.size = 0;
            this.pid = 0;
            this.stream = new ByteArrayOutputStream();
        }
    }

    static {
        TAG = "IVideoCodec";
    }

    IVideoCodec(Camera cam, int wdth, int hght, int frate, int brate) {
        this.patPid = 0;
        this.pmtPid = 0;
        this.videoPid = 0;
        this.hasVideo = false;
        this.camera = cam;
        this.width = wdth;
        this.height = hght;
        this.frameRate = frate;
        this.videoBitRate = brate * 1024;
        this.map = new HashMap();
        
        AppLibrary.log_i(TAG, "configuration: " + "videoBitRate: " + this.videoBitRate + ", frameRate: " + this.frameRate + ", params: " + this.width + "x" + this.height);
    }

    private void handlePat(byte[] buffer) {
        this.pmtPid = (buffer[3] & 255) + ((buffer[2] & 31) << 8);
    }

    private void handlePes(boolean pusi, TSStream s, byte[] buffer, int offset) {
        if (pusi) {
            if (s.stream.size() > 0 && s.pid == ((long) this.videoPid)) {
                handleVideo(s.stream.toByteArray());
            }
            if (buffer[offset] == 0 && buffer[offset + 1] == 0 && buffer[offset + 2] == 1) {
                s.stream.reset();
                s.size = (buffer[offset + 5] & 255) + ((buffer[offset + 4] & 255) << 8);
                offset += 6;
            } else {
                return;
            }
        }
        int size = buffer.length - offset;
        if (s.stream.size() + size > s.size) {
            size = s.size - s.stream.size();
        }
        s.stream.write(buffer, offset, size);
        if (s.size > 0 && s.stream.size() >= s.size) {
            if (s.pid == ((long) this.videoPid)) {
                handleVideo(s.stream.toByteArray());
            }
            s.stream.reset();
        }
    }

    private void handlePmt(byte[] buffer) {
        int eslen = 0;
        for (int n = ((buffer[3] & 255) + 4) + ((buffer[2] & 15) << 8); n + 4 < buffer.length; n += eslen + 5) {
            int codec = buffer[n] & 255;
            int pid = (buffer[n + 2] & 255) + ((buffer[n + 1] & 31) << 8);
            int eslen2 = (buffer[n + 4] & 255) + ((buffer[n + 3] & 15) << 8);
            AppLibrary.log_d(TAG, "PMT codec=" + codec + "; pid=" + pid);
            switch (codec) {
                case 27:
                    this.videoPid = pid;
                    AppLibrary.log_d(TAG, "PMT video pid=" + pid);
                    break;
            }
        }
    }

    private void handlePsi(boolean pusi, TSStream s, byte[] buffer, int offset) {
        if (pusi) {
            int headerLength = buffer[offset] + 9;
            offset += buffer[offset] + 1;
            s.stream.reset();
            s.size = ((buffer[offset + 2] & 255) + ((buffer[offset + 1] & 15) << 8)) - headerLength;
            offset += 8;
        }
        int length = buffer.length - offset;
        if (s.stream.size() + length > s.size) {
            length = s.size - s.stream.size();
        }
        s.stream.write(buffer, offset, length);
        if (s.stream.size() >= s.size) {
            if (s.pid == ((long) this.patPid)) {
                handlePat(s.stream.toByteArray());
            } else if (s.pid == ((long) this.pmtPid)) {
                handlePmt(s.stream.toByteArray());
            }
            s.stream.reset();
        }
    }

    private void handleTsPacket(byte[] buffer) {
        if (buffer[0] != 71) {
            AppLibrary.log_e(TAG, "No MPEG-TS sync byte");
        } else {
            boolean pusi;
            boolean hasAdaptation;
            boolean hasPayload;
            int pid = (buffer[2] & 255) | ((buffer[1] & 31) << 8);
            if ((buffer[1] & 64) != 0) {
                pusi = true;
            } else {
                pusi = false;
            }
            if ((buffer[3] & 32) != 0) {
                hasAdaptation = true;
            } else {
                hasAdaptation = false;
            }
            if ((buffer[3] & 16) != 0) {
                hasPayload = true;
            } else {
                hasPayload = false;
            }
            int payloadStart = 4;
            if (hasAdaptation) {
                payloadStart = 4 + ((buffer[4] & 255) + 1);
            }
            Integer pidKey = new Integer(pid);
            TSStream s = (TSStream) this.map.get(pidKey);
            if (s == null) {
                s = new TSStream();
                s.pid = (long) pid;
                this.map.put(pidKey, s);
            }
            if (s.stream.size() == 0 && !pusi) {
                AppLibrary.log_e(TAG, "TS skip partial packet pid=" + pid);
            } else if (hasPayload) {
                try {
                    if (pid == this.patPid || pid == this.pmtPid) {
                        handlePsi(pusi, s, buffer, payloadStart);
                    } else if (pid == this.videoPid) {
                        handlePes(pusi, s, buffer, payloadStart);
                    } else {
                        AppLibrary.log_d(TAG, "TS unexpected ts packet pid=" + pid);
                    }
                } catch (Exception e) {
                    AppLibrary.log_d(TAG, "TS error parsing packet pid=" + pid);
                }
            }
        }
    }

    private void handleVideo(byte[] buffer) {
        ByteArrayOutputStream nal = null;
        FrameTime ft = new FrameTime();
        int n = parsePesOptional(buffer, ft);
        if (!this.hasVideo) {
            this.baseTimestamp = ft.dts;
            this.hasVideo = true;
        }
        ft.pts -= this.baseTimestamp;
        ft.dts -= this.baseTimestamp;
        while (n < buffer.length) {
            if (n + 2 < buffer.length && buffer[n] == 0 && buffer[n + 1] == 0 && buffer[n + 2] == (byte) 1) {
                n += 3;
            } else if (n + 3 < buffer.length && buffer[n] == 0 && buffer[n + 1] == 0 && buffer[n + 2] == 0 && buffer[n + 3] == (byte) 1) {
                n += 4;
            } else {
                if (nal != null) {
                    nal.write(buffer[n]);
                }
                n++;
                continue;
            }
            if (nal != null) {
                handleVideoNal(nal.toByteArray(), ft);
                nal.reset();
            } else {
                nal = new ByteArrayOutputStream();
            }
        }
        if (nal != null && nal.size() > 0) {
            handleVideoNal(nal.toByteArray(), ft);
        }
    }

    private void handleVideoNal(byte[] buffer, FrameTime ft) {
        if (this.callback != null) {
            try {
                this.callback.onVideoNal(buffer, ft.pts / 90, ft.dts / 90);
            } catch (Exception e) {
                AppLibrary.log_e(TAG, "Processing error: " + e.getMessage());
                this.callback.onVideoError("Video processing error");
            }
        }
    }

    private int parsePesOptional(byte[] buffer, FrameTime ft) {
        int n = 3;
        if ((buffer[1] & 128) != 0) {
            long parseTimestamp = parseTimestamp(buffer, 3);
            ft.pts = parseTimestamp;
            ft.dts = parseTimestamp;
            n = 3 + 5;
        }
        if ((buffer[1] & 64) != 0) {
            ft.pts = parseTimestamp(buffer, n);
        }
        return buffer[2] + 3;
    }

    private long parseTimestamp(byte[] buffer, int offset) {
        return ((((0 | ((14 & ((long) buffer[offset])) << 29)) | ((((long) buffer[offset + 1]) & 255) << 22)) | ((((long) buffer[offset + 2]) & 254) << 14)) | ((((long) buffer[offset + 3]) & 255) << 7)) | ((((long) buffer[offset + 4]) & 254) >> 1);
    }

    public void onError(MediaRecorder m, int what, int extra) {
        if (this.callback != null) {
            this.callback.onVideoError(String.format("Video error %d/%d", new Object[]{Integer.valueOf(what), Integer.valueOf(extra)}));
        }
    }

    public void onInfo(MediaRecorder m, int what, int extra) {
        AppLibrary.log_d(TAG, String.format("Video info: what=%d, extra=%d", new Object[]{Integer.valueOf(what), Integer.valueOf(extra)}));
    }

    public void prepare() throws IOException {
        AppLibrary.log_d(TAG, "Prepare");
        this.mediaRecorder = new MediaRecorder();
        this.mediaRecorder.setCamera(this.camera);
        this.mediaRecorder.setOnErrorListener(this);
        this.mediaRecorder.setOnInfoListener(this);
        this.mediaRecorder.setVideoSource(0);
        this.mediaRecorder.setOutputFormat(8);
        this.mediaRecorder.setVideoSize(this.width, this.height);
        if (this.frameRate != 0) {
            this.mediaRecorder.setVideoFrameRate(this.frameRate);
        }
        this.mediaRecorder.setVideoEncodingBitRate(this.videoBitRate);
        this.mediaRecorder.setVideoEncoder(2);
        this.mediaRecorder.setPreviewDisplay(this.surface);
        this.pipe = ParcelFileDescriptor.createPipe();
        this.mediaRecorder.setOutputFile(this.pipe[1].getFileDescriptor());
        AppLibrary.log_d(TAG, "Calling prepare");
        this.mediaRecorder.prepare();
    }

    public void run() {
        FileInputStream is = new FileInputStream(this.pipe[0].getFileDescriptor());
        byte[] buffer = new byte[188];
        while (true) {
            int offs = 0;
            while (offs < 188) {
                try {
                    if (!Thread.interrupted()) {
                        int n = is.read(buffer, offs, 188 - offs);
                        if (n <= 0) {
                            AppLibrary.log_d(TAG, "Input stream eof");
                            return;
                        } else {
                            offs += n;
                        }
                    } else {
                        return;
                    }
                } catch (IOException e) {
                    e = e;
                    AppLibrary.log_e(TAG, String.format("IO error: %s", new Object[]{e.getMessage()}));
                    if (this.callback != null) {
                        this.callback.onVideoError(String.format("Video IO error: %s", new Object[]{e.getMessage()}));
                    }
                }
            }
            handleTsPacket(buffer);
        }
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public void setSurface(Surface srf) {
        this.surface = srf;
    }

    public void start() {
        AppLibrary.log_d(TAG, "Start");
        this.camera.unlock();
        try {
            this.mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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
                this.thread.join();
            } catch (Exception e3) {
            }
            this.thread = null;
        }
    }
}