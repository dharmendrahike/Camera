package com.pulseapp.android.broadcast;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.util.Log;

import com.pulseapp.android.util.AppLibrary;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ShwStppr on 26-Jun-2015.
 */
@TargetApi(18)
public class RecordingMuxer implements Runnable {

    private static final String TAG = "RecordingMuxer";
    private static final boolean VERBOSE = false;
    private String mOutputFile = "";
    private Context mContext;
    int mVideoWidth;
    int mVideoHeight;
    int mVideoBitrate;
    int mVideoFPS;
    int mAudioSamplingRate;
    int mAudioChannels;
    int mAudioBitrate;
    private Lock lock;
    private Condition condition;
    private boolean hasAudio;
    private boolean hasVideo;
    MediaFormat audioFormat, videoFormat;
    private LinkedList<MediaFrame> videoQueue;
    private LinkedList<MediaFrame> audioQueue;
    private Thread thread;
    MediaMuxer mMuxer = null;
    boolean mMuxerStarted = false;
    private int audioTrackIndex = -1;
    private int videoTrackIndex = -1;
    private static int MAX_QUEUE_SIZE = 256;
    private Callback callback = null;
    private byte[] mAudioHeader;
    private byte[] mSPS = null;
    private byte[] mPPS = null;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private boolean mIsErrored = false;
    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // 2 seconds between I-frames

    public static class MediaFrame {
        byte[] buffer;
        long timestamp = 0; // will be 0 in case of editing. See @bufferInfo
        MediaFrameType type;
        BufferInfo bufferInfo; // In case of editing, bufferInfo will contain the frame timestamps
        int trackIndex; // Will only be used in case of editing
    }

    public enum MediaFrameType {
        AUDIOHEADER,
        AUDIO,
        VIDEOSPS,
        VIDEOPPS,
        VIDEO,
        EOS
    }

    public static interface Callback {
        void onRecordingError(String str);
    }

    RecordingMuxer(String outputFile,
                   int videoWidth, int videoHeight, int videoBitrate, int videoFPS,
                   int audioSamplingRate, int audioChannels, int audioBitrate, Context mContext) {
        this.mContext = mContext;
        this.mOutputFile = outputFile;
        this.mVideoWidth = videoWidth;
        this.mVideoHeight = videoHeight;
        this.mVideoBitrate = videoBitrate;
        this.mVideoFPS = videoFPS;
        this.mAudioSamplingRate = audioSamplingRate;
        this.mAudioChannels = audioChannels;
        this.mAudioBitrate = audioBitrate;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.audioQueue = new LinkedList();
        this.videoQueue = new LinkedList();
        this.mIsErrored = false;
        powerManager = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Wakelock");

        if(VERBOSE) AppLibrary.log_d(TAG, "Created RecordingMuxer object!");
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    public void prepare() throws IOException {
        this.audioQueue.clear();
        this.videoQueue.clear();
        videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, this.mVideoWidth, this.mVideoHeight);

        // Set some properties for audio & video MediaFormat objects.
//        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.mVideoBitrate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, this.mVideoFPS);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, this.mAudioSamplingRate, this.mAudioChannels);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, this.mAudioBitrate);
        if(VERBOSE)
            AppLibrary.log_d(TAG, "Prepared RecordingMuxer for output "+this.mOutputFile);
    }

    public void setAudioHeader(byte[] audioHeader) {
        this.mAudioHeader = audioHeader;
        ByteBuffer csd0 = ByteBuffer.wrap(this.mAudioHeader);
        audioFormat.setByteBuffer("csd-0", csd0);
        audioTrackIndex = mMuxer.addTrack(audioFormat);
        this.hasAudio = true;
        if(VERBOSE) AppLibrary.log_d(TAG, "Received Audio Header "+this.mAudioHeader.length);
    }

    public void run() {
        AppLibrary.log_d(TAG, "Thread");
        int videoFrames = 0;
        BufferInfo bufferInfo = new BufferInfo();
        boolean endOfStream = false;
        long AUDIO_VIDEO_START_TS_DIFF = 0; //Assuming that the 1st type1/5 frame comes at 3000
        try {
            //Some initialization
            this.mMuxer = new MediaMuxer(this.mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            byte[] videoSeparator = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1}; //Need this for 0001 prefix of NALs
            while (true) {
                this.lock.lock();
                MediaFrame audioFrame = null;
                MediaFrame videoFrame = null;
                if (!this.audioQueue.isEmpty())
                    audioFrame = this.audioQueue.peek();
                if (!this.videoQueue.isEmpty())
                    videoFrame = this.videoQueue.peek();
                if(audioFrame==null && videoFrame==null)
                    this.condition.await();
                //For checking logs
//                boolean isAudioNull = audioFrame==null;
//                boolean isVideoNull = videoFrame==null;
//                long audioTS = isAudioNull?0:audioFrame.timestamp;
//                long videoTS = isVideoNull?0:videoFrame.timestamp;
//                AppLibrary.log_d(TAG, "In Run "+isAudioNull+" "+isVideoNull+" "+audioTS+" "+videoTS);
                if(audioFrame!=null && ((videoFrame==null)||(videoFrame!=null && audioFrame.timestamp<videoFrame.timestamp-AUDIO_VIDEO_START_TS_DIFF))) {
                    if (this.hasAudio && audioTrackIndex >= 0 && mMuxerStarted) {
                        this.audioQueue.poll();
                        ByteBuffer buf = ByteBuffer.wrap(audioFrame.buffer);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) > 0) {
                                Log.d(TAG, "Received audio EOS");
                                endOfStream = true; //Not going to be used, but others are doing it too... :P
                                break;
                            }
                            int trackIndex = audioFrame.trackIndex;
                            bufferInfo = audioFrame.bufferInfo;
                            if(VERBOSE) {
                                Log.d(TAG, "Received audio frame: " + audioFrame + "bufferinfo: " + audioFrame.bufferInfo + "timestamp: " + audioFrame.bufferInfo.presentationTimeUs);
                            }
                            bufferInfo.set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                            if (!writeMuxerSampleData(audioTrackIndex, buf, bufferInfo)) {
                                return;
                            }
                    } else if (!mMuxerStarted) {
                        AppLibrary.log_d(TAG, "Audio frame & muxer hasn't started! Video Frame null? " + (videoFrame==null));
                        if (videoFrame!=null)
                            this.audioQueue.poll(); //Skip this audio frame as the timestamp check above might result in an infinite loop
                        else {
                            this.condition.await();
                            continue;
                        }
                    }

                    if (videoFrame!=null) {
                        if (videoFrame.type == MediaFrameType.EOS) {
                            AppLibrary.log_d(TAG, "---------------Reached EOS!");
                            endOfStream = true;
                            this.lock.unlock();
                            break;
                        }
                    }

//                    if (!mMuxerStarted)
//                        startMuxer(); //Try starting muxer
                    } else {
//                    Log.d(TAG, "Went to process video frame");
                    if(videoFrame!=null) {
                        this.videoQueue.poll();
                        if (videoFrame.type == MediaFrameType.EOS) {
                            AppLibrary.log_d(TAG, "---------------Reached EOS!");
                            endOfStream = true;
                            this.lock.unlock();
                            break;
                        }
                        // Wrap a byte array into a buffer
                        byte[] c = new byte[videoFrame.buffer.length + 4];
                        System.arraycopy(videoSeparator, 0, c, 0, videoSeparator.length);
                        System.arraycopy(videoFrame.buffer, 0, c, videoSeparator.length, videoFrame.buffer.length);
                        ByteBuffer buf = ByteBuffer.wrap(c);
                        int flag = 0;
                        int type = videoFrame.buffer[0] & 31;
                        if (videoFrame.type != MediaFrameType.EOS) {
                            if (type == 1 && this.mSPS!=null && this.mPPS!=null & mMuxerStarted) {
                                flag = 0;
                                bufferInfo.set(0, videoFrame.buffer.length + 4, (videoFrame.timestamp-AUDIO_VIDEO_START_TS_DIFF)/* * 1000*/, flag);
                                if (VERBOSE) {
                                    AppLibrary.log_d(TAG, "----Video " + videoFrames + " " + bufferInfo.size + "framets: " + videoFrame.timestamp + " TS " + bufferInfo.presentationTimeUs + " queue size " + this.videoQueue.size() + " " + type);
                                }
                                if(writeMuxerSampleData(videoTrackIndex, buf, bufferInfo)) {
//                                    Log.d(TAG, "writing video frame of type 1");
                                    videoFrames++;
                                } else {
                                    Log.d(TAG, "Failed to write frame of type 1");
                                    return;
                                }
                            } else if (type == 5 && this.mSPS!=null && this.mPPS!=null) {
                                if (videoTrackIndex < 0) { //First frame is a type 5 frame, so setting format and starting Muxer here
                                    videoTrackIndex = mMuxer.addTrack(videoFormat); //Video format object here added after setting SPS and PPS
                                }

                                if (audioTrackIndex < 0 && audioFormat!= null) { // In case muxer was null while adding the track
                                    audioTrackIndex = mMuxer.addTrack(audioFormat);
                                }

                                if(!mMuxerStarted)
                                    startMuxer(); //Try starting muxer

                                if(mMuxerStarted) {
                                    flag = 1;
                                    bufferInfo.set(0, c.length, (videoFrame.timestamp - AUDIO_VIDEO_START_TS_DIFF)/* * 1000*/, flag);
                                    if (VERBOSE) {
                                        AppLibrary.log_d(TAG, "----Video " + videoFrames + " " + bufferInfo.size + " TS " + bufferInfo.presentationTimeUs + " queue size " + this.videoQueue.size() + " " + type);
                                    }
                                    if (writeMuxerSampleData(videoTrackIndex, buf, bufferInfo)) {
                                        videoFrames++;
                                    }
                                    else {
                                        return;
                                    }
                                }
                            } else if (type == 7) {
                                this.mSPS = videoFrame.buffer;
                                if (VERBOSE)
                                    AppLibrary.log_d(TAG, "Received SPS "+this.mSPS.length);
                                videoFormat.setByteBuffer("csd-0", buf);
                            } else if (type == 8) {
                                this.mPPS = videoFrame.buffer;
                                if (VERBOSE)
                                    AppLibrary.log_d(TAG, "Received PPS "+this.mPPS.length);
                                videoFormat.setByteBuffer("csd-1", buf);
                            }
                        } else {
                            Log.d(TAG, "EOS received");
                        }
//                    if(VERBOSE)
//                        AppLibrary.log_d("CheckNALPrefix", "" + (int) buf.get() + "" + (int) buf.get() + "" + (int) buf.get() + "" + (int) buf.get());
                    }
/*                    else {
                        Log.w(TAG, "video frame is null");
                    }*/
                }
                this.lock.unlock();
            }
        }
        catch (InterruptedException ex1) {
            AppLibrary.log_e(TAG, "Recording Thread interrupted!");
            try {
                this.lock.unlock();
            } catch (IllegalMonitorStateException imse) {}
        }
        catch (Exception ex) {
            if (this.callback != null) {
                mIsErrored = true;
                /*Calling callback here will cause crash when if stop() is called */
//                this.callback.onRecordingError("Recording thread error! "+ ex.getMessage());
                sendDelayedErrorCallback("Recording thread error! " + ex.getMessage(), 10);
                AppLibrary.log_e(TAG, "Thread error! " + ex.getMessage());
                ex.printStackTrace();
                try {
                    this.lock.unlock();
                } catch (IllegalMonitorStateException imse) {}
            }
        } finally {
            if (videoFrames == 0)
                mMuxerStarted = false;
        }
    }

    private void startMuxer() {
        if (videoTrackIndex >= 0 && audioTrackIndex>=0 && mSPS!=null && mPPS!=null && this.hasAudio) { //We wait for the stars to align before starting the muxer :)
            Log.d(TAG, "Starting muxer and hasAudio is: " + hasAudio);
            mMuxer.start();
            mMuxerStarted = true;
            AppLibrary.log_d(TAG, "Started MediaMuxer! Audio at " +
                    this.audioTrackIndex +
                    " Video at " +
                    this.videoTrackIndex +
                    " "+this.audioQueue.size()+" "+this.videoQueue.size());
            AppLibrary.log_d(TAG, "File: "+mOutputFile);
        }
    }

    public void postFrame(MediaFrame frame) {
        if(this.mIsErrored) {
            if(frame.type!=MediaFrameType.EOS)
                return; //Do not allow frame posting if there is error. Only allow for EOS type
        }
        this.lock.lock();
//        if (frame.type == MediaFrameType.EOS) {
//            //Stop saving...
//        } else {
        LinkedList<MediaFrame> queue;
        if(frame.type==MediaFrameType.AUDIO || frame.type==MediaFrameType.AUDIOHEADER)
            queue =  this.audioQueue;
        else
            queue = this.videoQueue;

        if (queue == this.audioQueue && queue.size() >= MAX_QUEUE_SIZE && (!mMuxerStarted || (this.videoQueue.size() <= 3))) {
            AppLibrary.log_e(TAG, "Entered max audio queue size loop");
            queue.clear();
            //At the risk of losing the audio header in this, but the probability is extremely low as by the time
            //64 audio frames is sent, the first audio frame header would have been processed by the thread
        } else if (queue.size() >= MAX_QUEUE_SIZE) {
            AppLibrary.log_e(TAG, "Some error max queue size reached! "+ frame.type + " size "+queue.size());
            this.lock.unlock();
            this.mIsErrored = true;
            this.callback.onRecordingError("Some error while recording, Max queue size reached!");
        } else {
            queue.offer(frame);
            this.condition.signal();
//            Log.d(TAG, "condition signalled");
            this.lock.unlock();
        }
//        }
    }

    public void start() throws IOException {
        if(VERBOSE) AppLibrary.log_d(TAG, "Start");
        wakeLock.acquire();
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void stop() {
        AppLibrary.log_d(TAG, "Stop");
        if (this.thread != null && this.thread.isAlive()) {
/*            MediaFrame frame = new MediaFrame();
            frame.type = MediaFrameType.EOS;
            postFrame(frame);*/
            try {
                this.thread.join(2000);
            } catch (Exception ex) {
                AppLibrary.log_e(TAG, "Error in joining recording thread! "+ex.getMessage());
            }
//            try {
//                this.thread.interrupt();
//                if(VERBOSE) AppLibrary.log_d(TAG, "Thread exited");
//            } catch (Exception ex) {
//                AppLibrary.log_e(TAG, "Error on stop! "+ex.getMessage());
//            }
        } else {
            AppLibrary.log_e(TAG, "FML");
        }
        this.thread = null;
        if(this.mMuxer!=null) {
            try {
                if(mMuxerStarted)
                    this.mMuxer.stop();
                this.mMuxer.release();
            } catch (IllegalStateException ise) {
                AppLibrary.log_e(TAG, "Unable to stop, release Muxer. Probably already done! "+ise.getMessage());
            }
            mMuxer = null;
        }
        this.callback = null;
        this.audioQueue.clear();
        this.videoQueue.clear();
        wakeLock.release();
    }

    public void addDummyTrack() {
        this.hasAudio = true;
        Log.d(TAG, "Added dummy audio track");
        audioTrackIndex = 5;
    }

    public void addTrack(MediaFormat mediaFormat) {
        this.hasAudio = true;
        Log.d(TAG, "Added audio track with format: " + mediaFormat.toString());

        if (mMuxer!=null) {
            audioTrackIndex = mMuxer.addTrack(mediaFormat);
        } else {
            audioFormat = mediaFormat; //If muxer hasn't been setup yet, lets set up the track later
        }
    }

    public void feedMuxerData(MediaFrame mediaFrame) {
        audioQueue.offer(mediaFrame);
        Log.d(TAG, "Fed audio frame: " + mediaFrame + "bufferinfo: " + mediaFrame.bufferInfo + "timestamp: " + mediaFrame.bufferInfo.presentationTimeUs);
    }

    /*
    To simplify writing sample data for MediaMuxer and make changes only at one place.
    Do not call this method anywhere other than run() after muxer start
     */
    private boolean writeMuxerSampleData(int trackIndex,
                                         ByteBuffer buffer,
                                         BufferInfo bufferInfo) {
        if(!mMuxerStarted || mMuxer==null) return false; //mMuxerStarted cannot be false here technically, but just in case
        try {
            mMuxer.writeSampleData(trackIndex, buffer, bufferInfo);
        } catch (Exception ex) {
            mIsErrored = true;
            mMuxerStarted = false; //It stops itself
            sendDelayedErrorCallback("writeSampleData error! " + ex.getMessage(), 10);
            this.lock.unlock();
            return false;
        }
        return true;
    }

    /*
    This will help RecordingMuxer stop fast as thread won't be running for long
     */
    public void flushStop() {
        this.lock.lock();
        this.audioQueue.clear();
        this.videoQueue.clear();
        this.lock.unlock();
        stop();
    }

    private void sendDelayedErrorCallback(final String exMessage, int delay) {
        Timer errorCallbackTimer =  new Timer();
        errorCallbackTimer.schedule(new TimerTask() {
            public void run() {
                if (callback != null) {
                    callback.onRecordingError(exMessage);
                }
            }
        }, delay);
    }

    public void cloneMediaUsingMuxer(String ManipulatedMedia) throws IOException {
            // Set up MediaExtractor to read from the source.
            int MAX_SAMPLE_SIZE = 100000;
            float SPEED_INDEX = 0.5f;
            File recordedFile = new File (StreamingActionControllerKitKat.getLastRecordedFilepath());
            ParcelFileDescriptor srcFd = ParcelFileDescriptor.open(recordedFile,ParcelFileDescriptor.MODE_READ_WRITE);
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(srcFd.getFileDescriptor());
            int trackCount = extractor.getTrackCount();
//            assertEquals("wrong number of tracks", expectedTrackCount, trackCount);
            // Set up MediaMuxer for the destination.
            MediaMuxer muxer;
            muxer = new MediaMuxer(ManipulatedMedia, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // Set up the tracks.
            HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
            for (int i = 0; i < trackCount; i++) {
                extractor.selectTrack(i);
                MediaFormat format = extractor.getTrackFormat(i);
                if(format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    int currentFrameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    int newFrameRate = (int) (currentFrameRate*SPEED_INDEX);
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, newFrameRate);
                }
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
            }
            // Copy the samples from MediaExtractor to MediaMuxer.
            boolean sawEOS = false;
            long lastVideoTS = 0;
            long lastAudioTS = 0;
            int bufferSize = MAX_SAMPLE_SIZE;
            int frameCount = 0;
            int offset = 100;
            ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
            BufferInfo bufferInfo = new BufferInfo();
//            if (degrees >= 0) {
//                muxer.setOrientationHint(degrees);
//            }
            muxer.start();
            while (!sawEOS) {
                bufferInfo.offset = offset;
                bufferInfo.size = extractor.readSampleData(dstBuf, offset);
                if (bufferInfo.size < 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "saw input EOS.");
                    }
                    sawEOS = true;
                    bufferInfo.size = 0;
                } else {
                    bufferInfo.flags = extractor.getSampleFlags();
                    int trackIndex = extractor.getSampleTrackIndex();

                    if(trackIndex==0) { //For Video
                        if(lastVideoTS!=0) { //Not the first frame, first frame is written only once
                            bufferInfo.presentationTimeUs = ((long)(extractor.getSampleTime()/SPEED_INDEX) - lastVideoTS)/2; //Equally space the frame between the new Frame and previous frame
                            muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                        }

                        bufferInfo.presentationTimeUs = (long) (extractor.getSampleTime()/SPEED_INDEX);
                        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                        lastVideoTS = (long) (extractor.getSampleTime()/SPEED_INDEX);
                    }
                     else { //For Audio
                        if(lastAudioTS!=0) { //Not the first frame, first frame is written only once
                            bufferInfo.presentationTimeUs = ((long)(extractor.getSampleTime()/SPEED_INDEX) - lastVideoTS)/2; //Equally space the frame between the new Frame and previous frame
                            muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                        }

                        bufferInfo.presentationTimeUs = (long) (extractor.getSampleTime()/SPEED_INDEX);
                        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                        lastAudioTS = (long) (extractor.getSampleTime()/SPEED_INDEX);
                    }

//                    muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);
                    extractor.advance();
                    frameCount++;
                    if (VERBOSE) {
                        Log.d(TAG, "CLONE Frame (" + frameCount + ") " +
                                "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
                                " Flags:" + bufferInfo.flags +
                                " TrackIndex:" + trackIndex +
                                " Size(KB) " + bufferInfo.size / 1024 +
                                " LastVideoTS " + lastVideoTS +
                                " LastAudioTS " + lastAudioTS);
                    }
                }
            }
            muxer.stop();
            muxer.release();
            srcFd.close();
            return;
 }



    public String getOutputFilePath() {
        return this.mOutputFile;
    }

    public boolean errored() {
        return mIsErrored;
    }
}
