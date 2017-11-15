package com.pulseapp.android.broadcast;

import android.os.Handler;
import android.os.Looper;

import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.util.AppLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by admin on 1/15/2015.
 * 
 * I suggest don't change any code or flow in here.
 */
class RtmpMuxer implements Runnable {
    private static final boolean VERBOSE = false;
    private static int CHUNK_SIZE;
    private static int CONNECT_TIMEOUT;
    private static int MAX_QUEUE_SIZE;
    public static boolean SMART_BITRATE_MODE_ENABLED = true;
    private static String TAG;
    private int aacFormat;
    private byte[] aacHeader;
    private String app;
    private LinkedList<Frame> audioQueue;
    private long audioTimestamp;
    private Callback callback;
    private Condition condition;
    private boolean hasAudio;
    private boolean hasVideo;
    private String host;
    private InputStream in;
    private Lock lock;
    private int numChannels;
    private OutputStream out;
    private String playpath;
    private int port;
    private byte[] pps;
    private int sampleRate;
    private int sampleSize;
    private Socket socket;
    private byte[] sps;
    private Thread thread;
    private LinkedList<Frame> videoQueue;
    private long videoTimestamp;
    private float dropRate = 0;
    private boolean isAudioError = false;
    private boolean isVideoError = false;
    private Frame SPSFrame = null;
    private Frame PPSFrame = null;

    //Objects/variables for drop frames information.
    private int mAudioFramesReceived = 0;
    private int mVideoFramesReceived = 0;
    private int mAudioFramesDropped = 0;
    private int mVideoFramesDropped = 0;

    //Objects for drop frames check, audio-only mode
    public static boolean mIsBitrateUpperThresholdForCurrentSession = false;
    public static boolean mIsBitrateLowerThresholdForCurrentSession = false;
    private boolean StopBitrateCheck = false;
    private Timer DropBitrateCheckTimer = null;
    private boolean isDropBitrateCheckTimerRunning = false;
    private Timer IncreaseBitrateCheckTimer = null;
    private boolean isIncreaseBitrateCheckTimerRunning = false;
    private final int DROP_BITRATE_CHECK_TIMER_TIME_INTERVAL = 2*1000;
    private final int INCREASE_BITRATE_CHECK_TIMER_TIME_INTERVAL = 5*1000;
    private final int TARGET_AUDIO_QUEUE_SIZE = 100;
    private boolean pauseBitrateCheck = false;
    private Handler rtmpHandler = null;


    /*Objects for timed socket closure on stop()*/
    private boolean mIsSocketCloseTimerRunning = false;
    private Timer mSocketCloseTimer = null;
    //Using 4(+1) seconds as current max fps is assumed 30 and since max video queue size is 128
    private static final int SOCKET_CLOSE_TIMEOUT_INTERVAL = 5*1000;

    /*To keep track if stop is called*/
    private boolean mIsStopped = false;
    private boolean isFlushStopped = false;

    public static interface Callback {
        void onRtmpError(String str);
        void onRtmpDecreaseBitrate(float dropRate);
        void onRtmpIncreaseBitrate();
        void stopRTMPStreaming(boolean stopBroadcast);
    }

    public static class Frame {
        byte[] buffer;
        long dts;
        long pts;
        FrameType type;
    }

    public enum FrameType {
        AUDIO,
        VIDEO,
        EOS
    }

    static {
        CONNECT_TIMEOUT = 3000;
        MAX_QUEUE_SIZE = 128;
        CHUNK_SIZE = 4096;
        TAG = "I/RtmpMuxer";
    }

    RtmpMuxer(String _host, int _port, String _app, String _playpath) {
        this.mIsStopped = false;
        this.isFlushStopped = false;
        this.StopBitrateCheck = false;
        this.pauseBitrateCheck = false;
        this.isAudioError = false;
        this.isVideoError = false;
        this.hasVideo = false;
        this.hasAudio = false;
        this.host = _host;
        this.port = _port;
        this.app = _app;
        this.playpath = _playpath;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.audioQueue = new LinkedList();
        this.videoQueue = new LinkedList();
    }

    private void connect() throws IOException {
        byte[] buffer = new byte[]{(byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 20, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 0, (byte) 7, (byte) 99, (byte) 111, (byte) 110, (byte) 110, (byte) 101, (byte) 99, (byte) 116, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 3, (byte) 0, (byte) 3, (byte) 97, (byte) 112, (byte) 112, (byte) 2, (byte) 0, (byte) 0};
        byte[] bufferEnd = new byte[]{(byte) 0, (byte) 0, (byte) 9};
        int length = this.app.length() + 31;
        buffer[4] = (byte) (length >> 16);
        buffer[5] = (byte) (length >> 8);
        buffer[6] = (byte) length;
        buffer[buffer.length - 2] = (byte) (this.app.length() >> 8);
        buffer[buffer.length - 1] = (byte) this.app.length();
        this.out.write(buffer);
        this.out.write(this.app.getBytes());
        this.out.write(bufferEnd);
    }

    private void handshake() throws IOException {
        byte[] buffer = new byte[3073];
        buffer[0] = (byte) 3;
        for (int i = 1; i < buffer.length; i++) {
            buffer[i] = (byte) 0;
        }
        this.out.write(buffer, 0, 1537);
        int left = 3073;
        while (left > 0) {
            int rc = this.in.read(buffer, 0, left);
            if (rc == -1) {
                throw new IOException("Server EOF");
            }
            left -= rc;
        }
        this.out.write(buffer, 0, 1536);
    }

    private void publish() throws IOException {
        byte[] buffer = new byte[]{(byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 20, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 0, (byte) 7, (byte) 112, (byte) 117, (byte) 98, (byte) 108, (byte) 105, (byte) 115, (byte) 104, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 5, (byte) 2, (byte) 0, (byte) 0};
        int length = this.playpath.length() + 23;
        buffer[4] = (byte) (length >> 16);
        buffer[5] = (byte) (length >> 8);
        buffer[6] = (byte) length;
        buffer[buffer.length - 2] = (byte) (this.playpath.length() >> 8);
        buffer[buffer.length - 1] = (byte) this.playpath.length();
        this.out.write(buffer);
        this.out.write(this.playpath.getBytes());
    }

    private void readAll() throws IOException {
        int n = this.in.available();
        if (n > 0) {
            this.in.skip((long) n);
        }
    }

    private void sendAudioFrame(byte[] frame, long ts) throws IOException {
        if((!(this.aacHeader == null || this.hasAudio)) || (this.aacHeader != null && isAudioError)){
            startAudio(ts);
        }
        if (this.hasAudio) {
            byte[] buffer = new byte[]{(byte) 72, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, (byte) 0, (byte) 1};
            long delta = ts - this.audioTimestamp;
            this.audioTimestamp = ts;
            buffer[1] = (byte) ((int) ((delta >> 16) & 255));
            buffer[2] = (byte) ((int) ((delta >> 8) & 255));
            buffer[3] = (byte) ((int) (255 & delta));
            int totalLength = frame.length + 2;
            buffer[4] = (byte) ((totalLength >> 16) & 255);
            buffer[5] = (byte) ((totalLength >> 8) & 255);
            buffer[6] = (byte) (totalLength & 255);
            buffer[8] = (byte) this.aacFormat;
            this.out.write(buffer);
            int length = CHUNK_SIZE - 2;
            if (length > frame.length) {
                length = frame.length;
            }
            this.out.write(frame, 0, length);
            int offset = length;
            while (offset < frame.length) {
                length = CHUNK_SIZE;
                if (offset + length > frame.length) {
                    length = frame.length - offset;
                }
                this.out.write(200);
                this.out.write(frame, offset, length);
                offset += length;
            }
        } else {
            AppLibrary.log_d(TAG, "Skip audio frame");
        }
    }

    private void sendVideoNal(byte[] nal, long pts, long dts) throws IOException {
        int nalType = nal[0] & 31;
        if (nalType == 7 && !this.hasVideo) {
            AppLibrary.log_d(TAG, "SPS arrived");
            this.sps = nal;
        } else if (nalType == 8 && !this.hasVideo) {
            AppLibrary.log_d(TAG, "PPS arrived");
            this.pps = nal;
        } else {
            if ((!(this.sps == null || this.pps == null || this.hasVideo)) || (this.sps !=null && this.pps!=null && isVideoError)) {
                startVideo(dts);
            }else if(SPSFrame!=null & PPSFrame!=null & (!this.hasVideo || isVideoError)){  //Encoder could have released SPS/PPS but 'cos of RTMP Error checks, lost out on the corresponding call to sendVideoNal()
                this.sps = SPSFrame.buffer;
                this.pps = PPSFrame.buffer;
                startVideo(dts);
            }

            if (this.hasVideo) {
                byte[] buffer = new byte[]{(byte) 73, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, (byte) 39, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
                long delta = dts - this.videoTimestamp;
                this.videoTimestamp = dts;
                buffer[1] = (byte) ((int) (255 & (delta >> 16)));
                buffer[2] = (byte) ((int) (255 & (delta >> 8)));
                buffer[3] = (byte) ((int) (255 & delta));
                int totalLength = nal.length + 9;
                buffer[4] = (byte) ((totalLength >> 16) & 255);
                buffer[5] = (byte) ((totalLength >> 8) & 255);
                buffer[6] = (byte) (totalLength & 255);
                if (nalType == 5) {
                    buffer[8] = (byte) 23;
                }
                long delay = pts - dts;
                buffer[buffer.length - 7] = (byte) ((int) (255 & (delay >> 16)));
                buffer[buffer.length - 6] = (byte) ((int) (255 & (delay >> 8)));
                buffer[buffer.length - 5] = (byte) ((int) (255 & delay));
                buffer[buffer.length - 4] = (byte) ((nal.length >> 24) & 255);
                buffer[buffer.length - 3] = (byte) ((nal.length >> 16) & 255);
                buffer[buffer.length - 2] = (byte) ((nal.length >> 8) & 255);
                buffer[buffer.length - 1] = (byte) (nal.length & 255);
                this.out.write(buffer);
                int length = CHUNK_SIZE - 9;
                if (length > nal.length) {
                    length = nal.length;
                }
                this.out.write(nal, 0, length);
                int offset = length;
                while (offset < nal.length) {
                    length = CHUNK_SIZE;
                    if (offset + length > nal.length) {
                        length = nal.length - offset;
                    }
                    this.out.write(201);
                    this.out.write(nal, offset, length);
                    offset += length;
                }
            } else {
                AppLibrary.log_d(TAG, "Skip video frame");
            }
        }
    }

    private void setChunkSize() throws IOException {
        byte[] buffer = new byte[]{(byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 4, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        buffer[buffer.length - 4] = (byte) (CHUNK_SIZE >> 24);
        buffer[buffer.length - 3] = (byte) (CHUNK_SIZE >> 16);
        buffer[buffer.length - 2] = (byte) (CHUNK_SIZE >> 8);
        buffer[buffer.length - 1] = (byte) CHUNK_SIZE;
        this.out.write(buffer);
    }

    private void startAudio(long ts) throws IOException {
        byte[] buffer = new byte[]{(byte) 8, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 8, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        AppLibrary.log_d(TAG, "Starting audio");
        this.audioTimestamp = ts;
        buffer[1] = (byte) ((int) ((ts >> 16) & 255));
        buffer[2] = (byte) ((int) ((ts >> 8) & 255));
        buffer[3] = (byte) ((int) (255 & ts));
        int length = this.aacHeader.length + 2;
        buffer[4] = (byte) ((length >> 16) & 255);
        buffer[5] = (byte) ((length >> 8) & 255);
        buffer[6] = (byte) (length & 255);
        this.aacFormat = (((this.numChannels - 1) & 1) | 172) | (((this.sampleSize - 1) & 1) << 1);
        buffer[12] = (byte) this.aacFormat;
        this.out.write(buffer);
        this.out.write(this.aacHeader);
        this.hasAudio = true;
        this.isAudioError = false;
    }

    private void startVideo(long ts) throws IOException {
        byte[] buffer = new byte[]{(byte) 9, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 9, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 23, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -31, (byte) 0, (byte) 0};
        byte[] bufferEnd = new byte[]{(byte) 1, (byte) 0, (byte) 0};
        AppLibrary.log_d(TAG, "Starting video");
        this.videoTimestamp = ts;
        buffer[1] = (byte) ((int) ((ts >> 16) & 255));
        buffer[2] = (byte) ((int) ((ts >> 8) & 255));
        buffer[3] = (byte) ((int) (255 & ts));
        int length = (this.sps.length + 16) + this.pps.length;
        buffer[4] = (byte) ((length >> 16) & 255);
        buffer[5] = (byte) ((length >> 8) & 255);
        buffer[6] = (byte) (length & 255);
        buffer[buffer.length - 2] = (byte) ((this.sps.length >> 8) & 255);
        buffer[buffer.length - 1] = (byte) (this.sps.length & 255);
        bufferEnd[bufferEnd.length - 2] = (byte) ((this.pps.length >> 8) & 255);
        bufferEnd[bufferEnd.length - 1] = (byte) (this.pps.length & 255);
        this.out.write(buffer);
        this.out.write(this.sps);
        this.out.write(bufferEnd);
        this.out.write(this.pps);
        this.hasVideo = true;
        this.isVideoError = false;
    }

    public void postFrame(Frame frame) {
        if(this.mIsStopped && frame.type != FrameType.EOS) return;  //Do not add new frames if stop is already called
        this.lock.lock();
        if (frame.type == FrameType.EOS) {
            this.videoQueue.offer(frame);
            this.audioQueue.offer(frame);
            this.condition.signal();
            this.lock.unlock();
        } else {
            LinkedList<Frame> queue = frame.type == FrameType.AUDIO ? this.audioQueue : this.videoQueue;

            if(frame.type == FrameType.AUDIO)
                mAudioFramesReceived++;
            else if(frame.type == FrameType.VIDEO)
                mVideoFramesReceived++;


            if(this.SMART_BITRATE_MODE_ENABLED) {
                if(!this.StopBitrateCheck) {
                    if(pauseBitrateCheck)    //Only used to pause bitrate checks briefly if required
                    {
                        stopIncreaseBitrateTimer();
                        stopDropBitrateTimer();
                    }
                    else if (this.audioQueue.size() >= MAX_QUEUE_SIZE) {    //  Queue>=128
                        stopIncreaseBitrateTimer();      //Less probability, but just for safety
                        if (!mIsBitrateLowerThresholdForCurrentSession)
                            startDropBitrateCheckTimer();
                    } else if (this.audioQueue.size() <= 4 * MAX_QUEUE_SIZE / 16) {     // Queue<=32
                        stopDropBitrateTimer();     //Less probability, but just for safety
                        if (!mIsBitrateUpperThresholdForCurrentSession)
                            startIncreaseBitrateTimer();
                    } else {
                        stopDropBitrateTimer();
                        stopIncreaseBitrateTimer();
                    }
                }
            }

            if ((frame.type == FrameType.AUDIO && queue.size() >= (2.5*MAX_QUEUE_SIZE)) ||
                    (frame.type == FrameType.VIDEO && queue.size() >= MAX_QUEUE_SIZE)) {
                if(frame.type == FrameType.AUDIO)
                    mAudioFramesDropped++;
                else if(frame.type == FrameType.VIDEO)
                    mVideoFramesDropped++;
                if(VERBOSE)
                    AppLibrary.log_e(TAG, "Drop frame");
                this.lock.unlock();
            } else {
                queue.offer(frame);
                this.condition.signal();
                this.lock.unlock();
            }
        }
    }

    public void setSPSPPS(int type, Frame frame)
    {
        if(type==7)
           SPSFrame = frame;
        else if(type==8)
           PPSFrame = frame;
    }

    public void forceCloseConnection()
    {
        clearQueues();
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }

        if(this.socket!=null)
        {
            try {
                this.socket.close();
                AppLibrary.log_d(TAG, "Socket closed in force close!");
            } catch (Exception e) {
            }
        }

        this.socket = null;
        this.in = null;
        this.out = null;
    }

    public void prepare() throws UnknownHostException, IOException {
        if(android.os.Build.VERSION.SDK_INT == CameraActivity.CUTOFF_SDK)
            this.SMART_BITRATE_MODE_ENABLED = false;   //Change bitrate encoder calls was added in API 19 only

            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(this.host, this.port), CONNECT_TIMEOUT);
            this.socket.setSoLinger(false, 0);
            this.out = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
    }

    public void isRTMPError()
    {
        if (this.callback != null) {
            isVideoError = true;
            isAudioError = true;
            this.callback.onRtmpError("Rtmp error in prepare");
        }
    }

    public void run() {
        AppLibrary.log_d(TAG, "Thread");
        boolean video = false;
        boolean audio = false;
        try {
            handshake();
            setChunkSize();
            connect();
            publish();
            readAll();
            while (true) {
                this.lock.lock();
                Frame audioFrame = null;
                Frame videoFrame = null;
                Frame frame = null;
                while (true) {
                    if (this.audioQueue.isEmpty() && audio) {
                        this.condition.await();
                    } else {
                        if (!this.audioQueue.isEmpty()) {
                            audioFrame = this.audioQueue.peek();
                            audio = true;
                            if (audioFrame.type == FrameType.EOS) {
                                return;
                            }
                        }
                        while (this.videoQueue.isEmpty() && video) {
                            this.condition.await();
                        }
                        if (!this.videoQueue.isEmpty()) {
                            videoFrame = this.videoQueue.peek();
                            video = true;
                            if (videoFrame.type == FrameType.EOS) {
                                return;
                            }
                        }
                        if (audioFrame != null && (videoFrame == null || audioFrame.dts < videoFrame.dts)) {
                            if(VERBOSE)
                                AppLibrary.log_d(TAG, "--Sending AUDIO DTS "+audioFrame.dts+" size "+audioFrame.buffer.length+" Video Queue Size "+videoQueue.size()+" "+this.StopBitrateCheck);
                            frame = audioFrame;
                            this.audioQueue.poll();
                        } else if (videoFrame != null) {
                            if(VERBOSE)
                                AppLibrary.log_d(TAG, "----Sending VIDEO DTS "+videoFrame.dts+" size "+videoFrame.buffer.length+" Audio Queue Size "+audioQueue.size()+" "+this.StopBitrateCheck);
                            frame = videoFrame;
                            this.videoQueue.poll();
                        } else {
                            this.condition.await();
                        }
                        if (frame != null) {
                            this.lock.unlock();
                            try {
                                readAll();
                                switch (frame.type) {
                                    case AUDIO:
                                        sendAudioFrame(frame.buffer, frame.pts);
                                        break;
                                    case VIDEO:
                                        sendVideoNal(frame.buffer, frame.pts, frame.dts);
                                        break;
                                    default:
                                        break;
                                }
                            } catch (IOException e) {
                                IOException e2 = e;
                                AppLibrary.log_e(TAG, String.format("IO error: %s", new Object[]{e2.getMessage()}));
                                if (this.callback != null) {
                                    isVideoError = true;
                                    isAudioError = true;
                                    this.callback.onRtmpError(String.format("IO error: %s", new Object[]{e2.getMessage()}));
                                    return;
                                }

                            } catch (Exception e3) {
                                AppLibrary.log_e(TAG, "Error sending frame");
                                if (this.callback != null) {
                                    isVideoError = true;
                                    isAudioError = true;
                                    this.callback.onRtmpError("Rtmp error sending frame");
                                    return;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e4) {
            AppLibrary.log_e(TAG, "RTMP Thread error");
            if (this.callback != null) {
                isVideoError = true;
                isAudioError = true;
                this.callback.onRtmpError("Rtmp thread error");
                return;
            }
        }
    }

    public void setAudioHeader(byte[] header, int nchan, int srate, int ssize) {
        this.aacHeader = header;
        this.numChannels = nchan;
        this.sampleRate = srate;
        this.sampleSize = ssize;
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public void clearQueues()
    {
        this.lock.lock();
        this.audioQueue.clear();
        this.videoQueue.clear();
        this.lock.unlock();
    }

    public void start() throws IOException {
        AppLibrary.log_d(TAG, "Start");
        this.mAudioFramesReceived = 0;
        this.mVideoFramesReceived = 0;
        this.mAudioFramesDropped = 0;
        this.mVideoFramesDropped = 0;
        this.audioQueue.clear();
        this.videoQueue.clear();
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void stop() {
        AppLibrary.log_d(TAG, "Stop");
        //set mIsStopped true so frame posting stops
        this.mIsStopped = true;
        //Start timer that will close the socket after given interval
        startSocketCloseTimer();
        if (this.thread != null) {
            Frame frame = new Frame();
            frame.type = FrameType.EOS;
            postFrame(frame);
            try {
                this.thread.join();
            } catch (Exception e2) {
            }
            this.thread = null;
        }
        //Voila! Thread completed even before timer timed out, let's close socket
        if (this.mIsSocketCloseTimerRunning) {
            stopSocketCloseTimer();
            try {
                if(this.socket!=null)
                    this.socket.close();
                AppLibrary.log_d(TAG, "Socket closed in stop()!");
            } catch (Exception e) {
            }
        }

        if(!isFlushStopped)
            StopStreamingCallbackForLive(true);
        else {
            isFlushStopped = false;
            StopStreamingCallbackForLive(false);
        }

        this.socket = null;
        this.in = null;
        this.out = null;
        this.mIsBitrateUpperThresholdForCurrentSession = false;
        this.mIsBitrateLowerThresholdForCurrentSession = false;
        isVideoError = false;
        isAudioError = false;
        this.StopBitrateCheck = false;
        this.pauseBitrateCheck = false;
        if (this.isIncreaseBitrateCheckTimerRunning)
            stopIncreaseBitrateTimer();
        this.isIncreaseBitrateCheckTimerRunning = false;
        if (this.isDropBitrateCheckTimerRunning)
            stopDropBitrateTimer();
        this.isDropBitrateCheckTimerRunning = false;
        this.audioQueue.clear();
        this.videoQueue.clear();
    }

    public void flushStop() {
        AppLibrary.log_d(TAG, "Flush stop");
        //set mIsStopped true so frame posting stops
        this.mIsStopped = true;
        this.isFlushStopped = true;
        this.lock.lock();
        this.audioQueue.clear();
        this.videoQueue.clear();
        this.lock.unlock();
        stop();
    }

    public void startDropBitrateCheckTimer() {
        if(!this.isDropBitrateCheckTimerRunning) {

            //set a new timer
            this.DropBitrateCheckTimer =  new Timer();

            this.DropBitrateCheckTimer.schedule(new TimerTask() {
                public void run() {
                    if((audioQueue.size() - TARGET_AUDIO_QUEUE_SIZE) > 0)
                        dropRate = (float) (Math.sqrt((audioQueue.size() - TARGET_AUDIO_QUEUE_SIZE)) + (audioQueue.size() - TARGET_AUDIO_QUEUE_SIZE)/6);  //Sqrt(x) + x/6 - Proprietary bitrate dropping algo? :)
                    else
                        dropRate = 10;          //Not possible. If this happens, nothing left to live for :D

//                  dropRate = (float) ((audioQueue.size() - TARGET_AUDIO_QUEUE_SIZE))/(DROP_BITRATE_CHECK_TIMER_TIME_INTERVAL/1000);

                    sendBitrateDecreaseCallback(dropRate);  //Sends dropRate as % number

                    isDropBitrateCheckTimerRunning = false;
                }
            }, DROP_BITRATE_CHECK_TIMER_TIME_INTERVAL);
            this.isDropBitrateCheckTimerRunning = true;

            if(VERBOSE)
                AppLibrary.log_d(TAG, "Drop Bitrate check timer started!");
        }
    }

    public void stopDropBitrateTimer() {
        if(this.isDropBitrateCheckTimerRunning) {
            //stop timer
            this.DropBitrateCheckTimer.cancel();
            this.DropBitrateCheckTimer = null;
            this.isDropBitrateCheckTimerRunning = false;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Drop Bitrate check timer stopped!");
        }
    }

    public void startIncreaseBitrateTimer() {
        if(!this.isIncreaseBitrateCheckTimerRunning) {
            //set a new timer
            this.IncreaseBitrateCheckTimer =  new Timer();

            this.IncreaseBitrateCheckTimer.schedule(new TimerTask() {
                public void run() {
                    sendBitrateIncreaseCallback();
                    isIncreaseBitrateCheckTimerRunning = false;
                }
            }, INCREASE_BITRATE_CHECK_TIMER_TIME_INTERVAL);
            this.isIncreaseBitrateCheckTimerRunning = true;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Increase Bitrate check timer started!");
        }
    }

    public void stopIncreaseBitrateTimer() {
        if(this.isIncreaseBitrateCheckTimerRunning) {
            //stop timer
            this.IncreaseBitrateCheckTimer.cancel();
            this.IncreaseBitrateCheckTimer = null;
            this.isIncreaseBitrateCheckTimerRunning = false;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Increase Bitrate check timer stopped!");
        }
    }

    public void sendBitrateDecreaseCallback(float dropRate) {
        AppLibrary.log_d(TAG, "Dropping bitrate!");

        if(this.callback!=null)
            this.callback.onRtmpDecreaseBitrate(dropRate);
    }

    public void StopStreamingCallbackForLive(boolean stopBroadcast) {
        AppLibrary.log_d(TAG, "Stopped Streaming to rtmp!");

        if(this.callback!=null)
            this.callback.stopRTMPStreaming(stopBroadcast);
    }


    public void sendBitrateIncreaseCallback() {
        AppLibrary.log_d(TAG, "Increasing bitrate!");

        if(this.callback!=null)
            this.callback.onRtmpIncreaseBitrate();
    }

    public void toggleBitrateChecks() {
        this.StopBitrateCheck = !this.StopBitrateCheck;
        stopBitrateCheckTimers();
        if(this.StopBitrateCheck)
            AppLibrary.log_d(TAG, "Bitrate checks stopped!");
        else
            AppLibrary.log_d(TAG, "Bitrate checks started again!");
    }

    public void stopBitrateChecks(){
        stopBitrateCheckTimers();
        this.StopBitrateCheck = true;
        AppLibrary.log_d(TAG, "Bitrate checks stopped!");
    }

    public void startBitrateChecks(){
        stopBitrateCheckTimers();
        this.StopBitrateCheck = false;
        AppLibrary.log_d(TAG, "Bitrate checks started!");
    }

    public void stopBitrateCheckTimers() {
        stopIncreaseBitrateTimer();
        stopDropBitrateTimer();
    }

    public void pauseBitrateChecks(int time) {
        if (!this.pauseBitrateCheck) {
            this.pauseBitrateCheck = true;
            AppLibrary.log_d(TAG, "Bitrate checks paused!");

            if(rtmpHandler==null)
                rtmpHandler = new Handler(Looper.getMainLooper());

            rtmpHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pauseBitrateCheck = false;
                    AppLibrary.log_d(TAG, "Bitrate checks unPaused and back!");
                }
                }, time);
        }
    }

    public void startSocketCloseTimer() {
        if(!this.mIsSocketCloseTimerRunning) {
            //set a new timer
            this.mSocketCloseTimer =  new Timer();

            this.mSocketCloseTimer.schedule(new TimerTask() {
                public void run() {
                    if(socket!=null && socket.isConnected()) {
                        try {
                            socket.close();
                            AppLibrary.log_d(TAG, "Socket closed by timer!");
                        } catch (Exception e) {
                            AppLibrary.log_e(TAG, "Unable to close socket in timer block: "+e.getMessage());
                        }
                    }
                    mIsSocketCloseTimerRunning = false;
                }
            }, RtmpMuxer.SOCKET_CLOSE_TIMEOUT_INTERVAL);
            this.mIsSocketCloseTimerRunning = true;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Socket close timer started!");
        }
    }

    private void stopSocketCloseTimer() {
        if(this.mIsSocketCloseTimerRunning) {
            this.mSocketCloseTimer.cancel();
            this.mSocketCloseTimer = null;
            this.mIsSocketCloseTimerRunning = false;
            if(VERBOSE)
                AppLibrary.log_d(TAG, "Socket close timer stopped!");
        }
    }

    public int getTotalReceivedFrames() {
        return this.mAudioFramesReceived+this.mVideoFramesReceived;
    }

    public int getTotalDroppedFrames() {
        return this.mAudioFramesDropped+this.mVideoFramesDropped;
    }
}
