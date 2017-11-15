package com.pulseapp.android.broadcast;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import com.pulseapp.android.gles.InputSurface;
import com.pulseapp.android.gles.OutputSurface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by bajaj on 12/1/16.
 * Plays the video track from a movie file to a Surface.
 * <p/>
 * TODO: needs more advanced shuttle controls (pause/resume, skip)
 */
public class MoviePlayer implements FrameCallback {
    private static final String TAG = "MoviePlayer";
    private static final boolean VERBOSE = false;
    public static boolean ENABLE_AUDIO = true;

    private static final int MSG_PLAY_STOPPED = 0;

    private static Context mContext;
    private SurfaceTexture mSurfaceTexture;

    public static final boolean CODEC_RENDERER_COMPAT_MODE = true;
    public static Object mDecoderLock = new Object();
    public static Object mRenderLock;

    private HandlerThread mAudioPlaybackThread;
    private LocalHandler mAudioPlaybackHandler;
    private MediaPlayer mAudioPlayer;

    private MediaExtractor mExtractor;
    private long mSeekTimeUs;

    private long audioFrameCount = 0;
    private long mAudioTimestampOffset;
    private long prevAudioFrameTS;
    private MediaCodec mDecoder;
    private int mAudioTrackIndex, mVideoTrackIndex;
    ByteBuffer[] decoderInputBuffers;
    int inputChunk;

    private boolean outputDone, inputDone;

    // If set, Frames are repeated in slow motion to maintain the same fps as original video
    private boolean FRAME_REPETITIONS_ON = true; //In case of slow motion.

    public static final int RENDER_TARGET_DISPLAY = 0x01;
    public static final int RENDER_TARGET_VIDEO = 0x02;

    private int mRenderTargetType;

    //Indicates whether the very first frame of the video has been rendered or not.
    private boolean firstFrameRendered;

    //When all repetitions (in case of a slow motion video) have completed rendering, this is made true
    private boolean frameRepetitionCompleted;
    private boolean renderable, repeatable;
    private boolean mIsPaused;
    private long firstFrameTs;

    private boolean mRequestTogglePlaybackSpeed;
    private boolean mVisibilityChanged;
    private boolean mVisibilityChangeHandled;
    private Surface mSurface;
    private boolean mVideoRecordSuccess;
    private int speedState;

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    private long mDuration;

    // Declare this here to reduce allocations.
    private BufferInfo mBufferInfo = new BufferInfo();

    // May be set/read by different threads.
    private volatile boolean mIsStopRequested;

    private static String mSourceFilePath;
    private String mDestFile;
    private OutputSurface mOutputSurface;

    FrameCallback mFrameCallback;
    FrameReceiver mFrameReceiver;
    private boolean mLoop;
    private int mVideoWidth;
    private int mVideoHeight;

    /* Video timestamp (fast fwd and slow down) based flags and auxillary variables */
    long orig_prev_ts = 0, orig_curr_ts = 0;
    long orig_prev_index = 0, orig_curr_index = -1; // index being processed of the source video
    long mod_prev_index = 0, mod_curr_index = 0; //index of current non-repeated frame in new video
    long mod_prev_ts = 0, mod_curr_ts = 0;
    float speedFactor = 1.0f;
    boolean slow_motion = false;
    boolean fast_motion = false;
    boolean normal_motion = true;

    private int incrementCounter;
    int frameRepeatCount;

    public interface FrameReceiver {
        void addDummyTrack();

        void addTrack(MediaFormat mediaFormat);

        void sendDirectAudioToRecordingMuxer(int trackIndex,
                                             ByteBuffer buffer, BufferInfo bufferInfo);

        void requestRender(boolean updateTs);

        void requestRender(boolean updateTs, long timeStampNs);

        void setFrameTimestamp(long timeStampNs);

        void playerReady();
    }

    /**
     * Interface to be implemented by class that manages playback UI.
     * <p/>
     * Callback methods will be invoked on the UI thread.
     */
    public interface PlayerFeedback {
        void playbackStarted(InputSurface surface);

        void playbackStarted(SurfaceTexture surfaceTexture);

        void closePlayer();

        void playbackStopped(int renderTargetType, boolean videoSuccess);
//        void setPreviousRenderTargetType(int renderTargetType);
    }

    /**
     * Constructs a MoviePlayer.
     *
     * @param sourceFile    The video file to open.
     * @param outputSurface The Surface where frames will be sent.
     * @throws IOException
     */
    /*     * @param frameCallback Callback object, used to pace output.*/
    public MoviePlayer(String sourceFile, OutputSurface outputSurface/*, FrameCallback frameCallback*/)
            throws IOException {
        mSourceFilePath = sourceFile;
        mOutputSurface = outputSurface;

        init();

        initExtractor();
    }

    /**
     * Constructs a MoviePlayer.
     *
     * @param sourceFile The video file to open.
     * @throws IOException
     */
    /*     * @param frameCallback Callback object, used to pace output.*/
    public MoviePlayer(String sourceFile, SurfaceTexture surfaceTexture, int renderTarget, Context context)
            throws IOException {
        mSourceFilePath = sourceFile;
        mSurfaceTexture = surfaceTexture;
        mRenderTargetType = renderTarget;

        init();
        initExtractor();
        mContext = context;
    }

    private void init() {
        mFrameCallback = this/*frameCallback*/;
        mSeekTimeUs = -1;
        mIsPaused = false;
        outputDone = true;
        mRequestTogglePlaybackSpeed = false;
        mVisibilityChanged = false;
        mVisibilityChangeHandled = false;

        mDecoderLock = new Object();
        mRenderLock = new Object();
        resetCounters();
    }

    public void start() {
        mIsPaused = false;
    }

    public void resume() {
        resumePlayer();
        mIsPaused = false;
    }

    public void pause() {
        pause(false);
    }

    public void pause(boolean visibilityChanged) {
        mIsPaused = true;
        // mIsPaused must be set to be true before setting mVisibilityChanged.
        mVisibilityChanged = visibilityChanged;
        mVisibilityChangeHandled = false;
    }

    public void setRenderTargetType(int renderTargetType) {
        mRenderTargetType = renderTargetType;
    }

    public int getRenderTargetType() {
        return mRenderTargetType;
    }

    public void setFrameReceiver(FrameReceiver frameReceiver) {
        mFrameReceiver = frameReceiver;
    }

    public void requestTogglePlaybackSpeed(int speedState) {
//        synchronized (this) {
        mRequestTogglePlaybackSpeed = true;
        this.speedState = speedState; //State 1 - Normal, State 2 - FastForward, State 3 - Slow-Mo
//        }
    }

    public void toggleVideoPlaybackSpeed() {
        replay(true);

        if (speedState == 1) { //Reset to Normal
            slow_motion = false;
            fast_motion = false;
            normal_motion = true;
            speedFactor = 1.0f;
        } else if (speedState==2) { //Fast-Forward
            if(VERBOSE) {
                Log.d(TAG, "playbackspeed: fast");
            }
            slow_motion = false;
            fast_motion = true;
            normal_motion = false;
            speedFactor = 2.0f;
        } else if (speedState==3) { //Slow-Mo
            if(VERBOSE) {
                Log.d(TAG, "playbackspeed: slow");
            }
            slow_motion = true;
            fast_motion = false;
            normal_motion = false;
            speedFactor = 0.5f;
        }

        if (slow_motion || fast_motion) {
            pauseAndResetAudio();
            ENABLE_AUDIO = false;
        } else {
            ENABLE_AUDIO = true;
        }

        if (slow_motion) {
            frameRepeatCount = FRAME_REPETITIONS_ON ? (int) (1.0f / speedFactor) : 1;
        }
    }

    private void resumePlayer() {
    }

    public void toggleAudio(boolean mute) {

        if (mute) {
            ENABLE_AUDIO = false;

            if (mAudioPlayer != null) {
                Log.d(TAG, "Audio player Muting");
                mAudioPlayer.setVolume(0f,0f);
            }
        } else {
            ENABLE_AUDIO = true;
            if (mAudioPlayer != null) {
                Log.d(TAG, "Audio player UnMuting");
                mAudioPlayer.setVolume(1.0f,1.0f);
            }
        }
    }

    private void initExtractor() throws IOException {
        // Pop the file open and pull out the video characteristics.
        // TODO: consider leaving the extractor open.  Should be able to just seek back to
        //       the start after each iteration of play.  Need to rearrange the API a bit --
        //       currently play() is taking an all-in-one open+work+release approach.
        MediaExtractor extractor = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(mSourceFilePath.toString());
            mExtractor = extractor;
            int trackIndex = findVideoTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + mSourceFilePath);
            }

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

            if (VERBOSE) {
                Log.d(TAG, "Video size is " + mVideoWidth + "x" + mVideoHeight);
            }
        } catch (IOException e) {
            extractor = null;
            mExtractor = null;
            throw e;
        } finally {
            if (extractor != null) {
                extractor.release();
            }
        }
    }

    private int findVideoTrack(MediaExtractor extractor) {

        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        int videoTrackIndex = -1;
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor Potential video select track " + i + " (" + mime + "): " + format);
                }
                mDuration = format.getLong(MediaFormat.KEY_DURATION);
                videoTrackIndex = i;
                break;
            } else {
                if (VERBOSE) {
                    Log.d(TAG, "Rejecting track: " + i + " (" + mime + "): " + format);
                }
            }
        }
        return videoTrackIndex;
    }

    private SparseIntArray findAudioTracks(MediaExtractor extractor) {
        SparseIntArray intArray = new SparseIntArray(4); // So that no memory allocations happen while adding track entry
        int numTracks = extractor.getTrackCount();

        int numAudioTracks = -1;
        for (int i = 0; i < numTracks; i++) {

            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                numAudioTracks++;
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                mDuration = format.getLong(MediaFormat.KEY_DURATION);
                intArray.put(numAudioTracks, i);
                if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    int currentFrameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    int newFrameRate = (int) (currentFrameRate * speedFactor);
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, newFrameRate);
                }
                if (ENABLE_AUDIO) {
                    extractor.selectTrack(i);
                } else {
                    extractor.unselectTrack(i);
                }

            }
//            int dstIndex = muxer.addTrack(format);
        }
        return intArray;
    }

    /**
     * Returns the width, in pixels, of the video.
     */
    public int getVideoWidth() {
        return mVideoWidth;
    }

    /**
     * Returns the height, in pixels, of the video.
     */
    public int getVideoHeight() {
        return mVideoHeight;
    }

    /**
     * Sets the loop mode.  If true, playback will loop forever.
     */
    public void setLoopMode(boolean loopMode) {
        mLoop = loopMode;
    }

    /**
     * Asks the player to stop.  Returns without waiting for playback to halt.
     * <p/>
     * Called from arbitrary thread.
     */
    public void requestStop() {
        if (!outputDone && !mIsStopRequested) {
            mIsStopRequested = true;
        } else {
/*            if (mFrameReceiver != null) {
                mFrameReceiver.playerReady();
            } else {
                Log.w(TAG, "mFrameReceiver Callback is null, not a good Sign");
            }*/
        }
    }

    @Override
    public boolean preRender(long presentationTimeUsec) {
        if (VERBOSE) Log.d(TAG, "Pre Render");
        return true;
    }

    void feed(long frame_index, long timestampus) {

    }

    @Override
    public void postRender() {
        if (VERBOSE) Log.d(TAG, "Post Render");
    }

    @Override
    public void loopReset() {
        Log.d(TAG, "Loop reset");
    }

    /**
     * Decodes the video stream, sending frames to the surface.
     * <p/>
     * Does not return until video playback is complete, or we get a "stop" signal from
     * frameCallback.
     */
    public boolean play() throws IOException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        resetCounters();
        mVideoRecordSuccess = false;
        mIsStopRequested = false;
        mIsPaused = false;
        firstFrameRendered = false;
        frameRepetitionCompleted = false;
        incrementCounter = 0;
        audioFrameCount = 0;
        prevAudioFrameTS = 0;
        mVisibilityChanged = false;
        mVisibilityChangeHandled = false;

        renderable = false;
        repeatable = false;
        File sourceFile = new File(mSourceFilePath);
        // The MediaExtractor error messages aren't very useful.  Check to see if the input
        // file exists so we can throw a better one if it's not there.
        if (!sourceFile.canRead()) {
            throw new FileNotFoundException("Unable to read " + sourceFile);
        }

        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(mSourceFilePath);
            int videoTrackIndex = selectTrack(mExtractor);
            if (videoTrackIndex < 0) {
//                throw new RuntimeException("No video track found in " + sourceFile);
                Log.w(TAG, "No video track found in " + sourceFile + ". Perhaps video is not long enough");
                if (mExtractor != null) {
                    mExtractor.release();
                    mExtractor = null;
                }
                return false;
            }

            SparseIntArray audiotrackMap = findAudioTracks(mExtractor);
            for (int i = 0; i < audiotrackMap.size(); i++) {
                MediaFormat inputAudioFormat = mExtractor.getTrackFormat(audiotrackMap.get(i));
/*                long duration = inputAudioFormat.getLong("durationUs");
                Log.d(TAG, "Audio track duration: " + duration);
                inputAudioFormat.setLong("durationUs", duration * 2);*/
                if ((mRenderTargetType & RENDER_TARGET_VIDEO) > 0) {
                    if (ENABLE_AUDIO) {
                        mFrameReceiver.addTrack(inputAudioFormat);
                    } else {
                        mFrameReceiver.addDummyTrack();
                    }
                }
            }

            int audioTrackIndex = audiotrackMap.get(0);
            mAudioTrackIndex = audioTrackIndex;
            mVideoTrackIndex = videoTrackIndex;

            MediaFormat format = mExtractor.getTrackFormat(mVideoTrackIndex);

            // Create a MediaCodec decoder, and configure it with the MediaFormat from the
            // extractor.  It's very important to use the format from the extractor because
            // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
            String mime = format.getString(MediaFormat.KEY_MIME);

            mDecoder = MediaCodec.createDecoderByType(mime);
            configureDecoder();
            inputChunk = 0;

            doExtract();

            long duration = format.getLong(MediaFormat.KEY_DURATION);
            Log.d(TAG, "track duration is : " + duration);
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "Unable to use DataSource " + mSourceFilePath + ". Perhaps video too short");
/*
            throw e;
*/
        } finally {
//            releaseDecoder();
        }
        return mVideoRecordSuccess;
    }

    public void releaseDecoder() {
        // release everything we grabbed
        if (mDecoder != null) {
            try {
                mDecoder.flush();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            try {
                mDecoder.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            try {
                mDecoder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
            mDecoder = null;
        }

        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    private void configureDecoder() throws IOException {
        Log.d(TAG, "Configuring decoder with SurfaceTexture: " + mSurfaceTexture);

        if (mSurface!=null) {
            mSurface.release();
            mSurface = null;
        }

        mSurface = new Surface(mSurfaceTexture);
        MediaFormat format = mExtractor.getTrackFormat(mVideoTrackIndex);

        mDecoder.configure(format, mSurface, null, 0);
        mDecoder.start();
        decoderInputBuffers = mDecoder.getInputBuffers();
    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        int videoTrackIndex = -1;
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                mDuration = format.getLong(MediaFormat.KEY_DURATION);
                videoTrackIndex = i;
                break;
            }
        }
        if (videoTrackIndex >= 0) {
            Log.d(TAG, "Selecting video track: " + videoTrackIndex);
            extractor.selectTrack(videoTrackIndex);
        } else {
            Log.d(TAG, "Not selecting any video track");
        }
        return videoTrackIndex;
    }

    public void seekTo(long timeUs) {
        mSeekTimeUs = timeUs;
    }

    final int MAX_SAMPLE_SIZE = 100000;
    int bufferSize = MAX_SAMPLE_SIZE;
    ByteBuffer audioDestBuf = ByteBuffer.allocate(bufferSize);

    /**
     * Work loop.  We execute here until we run out of video or are told to stop.
     */
    private void doExtract() throws IOException {
        // We need to strike a balance between providing input and reading output that
        // operates efficiently without delays on the output side.

        int offset = 100;

        outputDone = false;
        inputDone = false;
        mExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop");
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested");
                //releaseDecoder();
                return;
            }
            if (mIsPaused) {
                Log.d(TAG, "Pause requested");
                boolean visibilityChangeHandled = mVisibilityChangeHandled;
                if(!mVisibilityChangeHandled && mVisibilityChanged) {
                    try {
                        mDecoder.flush();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    try {
                        mDecoder.stop();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    pauseAndResetAudio();
                    mVisibilityChangeHandled = true;
                }
                try {
                    if(visibilityChangeHandled) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    continue;
                }
            } else {
                if(mVisibilityChanged) {
                    Log.d(TAG, "Handling visibility changed");
/*                    try {*/
                    replay();
                    mVisibilityChanged = false;
                    try {
                        if(ENABLE_AUDIO && mAudioPlayer!=null) {
                            mAudioPlayer.start();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    try {
                        configureDecoder();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
/*                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }*/
                }
            }

/*            synchronized (this) {*/
            // Feed more data to the decoder.
            if (!inputDone) {
                //Process Audio
                if (mExtractor.getSampleTrackIndex() == mAudioTrackIndex) {
                    if(VERBOSE) {
                        Log.d(TAG, "inside audio");
                    }
                    if (ENABLE_AUDIO) {
                        if ((mRenderTargetType & RENDER_TARGET_VIDEO) > 0) {
                            readAndProcessAudioFrame();
                        }
                        mExtractor.advance();
                        continue;
                    } else {
                        mExtractor.advance();
                        continue;
                    }
                } else {
                    boolean skipCurrentLoop = true;
                    try {
                        synchronized (mDecoderLock) {
                            skipCurrentLoop = readVideoFrame();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } finally {
                        if (skipCurrentLoop) continue;
                    }
                }
            }

            if (!outputDone) {
                processVideoFrame();
            }

            if (mRequestTogglePlaybackSpeed) {
                toggleVideoPlaybackSpeed();
                if(VERBOSE) {
                    Log.d(TAG, "Curr frame index changed to : " + orig_curr_index);
                }
                mRequestTogglePlaybackSpeed = false;
            }

    /*            }*/
        }
        if(outputDone) {
            mVideoRecordSuccess = true;
        }
    }

    private void readAndProcessAudioFrame() {

        ByteBuffer audioBuf = ByteBuffer.allocate(bufferSize);

        int chunkSize = mExtractor.readSampleData(audioBuf, 0);
        if (chunkSize >= 0) {
/*                        ByteBuffer audioBuf = ByteBuffer.allocate(chunkSize+offset);
                        audioBuf.position(0);*/
//                        chunkSize = extractor.readSampleData(audioBuf, offset);
            BufferInfo audioBufInfo = new BufferInfo();
//                        audioBufInfo.offset = offset;
            audioBufInfo.size = chunkSize;
            if (audioFrameCount == 0) {
/*                byte[] audiodata = audioBuf.array();
                Log.d(TAG, "audio frame 1: ");
                String str = "";
                for(int i = 0; i < chunkSize; i++) {
                    str += "" + audiodata[i];
                }
                Log.d(TAG, str);*/
                mAudioTimestampOffset = mExtractor.getSampleTime();
                if(VERBOSE) {
                    Log.d(TAG, "audio offset is: " + mAudioTimestampOffset);
                }
            }
            long audio_ts = /*(long) (audioFrameCount * 1000 / 44.1)*/mExtractor.getSampleTime() - mAudioTimestampOffset;
/*            if (slow_motion || fast_motion) {
                audio_ts = (long) (audio_ts * (1.0d / speedFactor));
            }*/
            audioBufInfo.presentationTimeUs = audio_ts;
            //noinspection ResourceType
            audioBufInfo.flags = mExtractor.getSampleFlags();
            int tIndex = mExtractor.getSampleTrackIndex();
            if (audioBufInfo.presentationTimeUs > prevAudioFrameTS) {
                if(VERBOSE) {
                    Log.d(TAG, "pushing audio data to muxer with timestamp: " + audioBufInfo.presentationTimeUs);
                }
                mFrameReceiver.sendDirectAudioToRecordingMuxer(tIndex, audioBuf, audioBufInfo);
                prevAudioFrameTS = audioBufInfo.presentationTimeUs;
            }
            audioFrameCount++;
        } else {
            Log.w(TAG, "audio chunk size less than 0");
            inputDone = true;
        }
    }

    private boolean readVideoFrame() {
/*        if(mRequestTogglePlaybackSpeed) return false;*/
        boolean touchOriginal = !slow_motion || !FRAME_REPETITIONS_ON || (frameRepetitionCompleted || !firstFrameRendered);
        if (!touchOriginal) {
            if(VERBOSE) {
                Log.d(TAG, "readVideoFrame Not touching");
            }
            return false;
        }
        if(VERBOSE) {
            Log.d(TAG, "inside video");
        }
        //Process Video
        int inputBufIndex;
        try {
            inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }

        if (inputBufIndex >= 0) {
            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
            // Read the sample data into the ByteBuffer.  This neither respects nor
            // updates inputBuf's position, limit, etc.
            int chunkSize = mExtractor.readSampleData(inputBuf, 0);
            if (chunkSize < 0) {
                // End of stream -- send empty frame with EOS flag set.
                mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                inputDone = true;
                if (VERBOSE) Log.d(TAG, "sent input EOS");
            } else {
                long presentationTimeUs = mExtractor.getSampleTime();
                mDecoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                        presentationTimeUs, 0 /*flags*/);
                if (VERBOSE) {
                    Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                            chunkSize);
                }
                if (mExtractor.getSampleTrackIndex() == mVideoTrackIndex) {
                    inputChunk++;
                    if (mSeekTimeUs > 0) {
                        //Used for seek/jump functionality
                        try {
                            mExtractor.seekTo(mSeekTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            return true;
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        } finally {
                            mSeekTimeUs = -1;
                        }
                    }
                    mExtractor.advance();
                } else {
                    Log.w(TAG, "Weird.., Got a Non-video frame when expecting a video frame");
                }
            }
        } else {
            if (VERBOSE) Log.d(TAG, "input buffer not available");
        }
        return false;
    }

    final int TIMEOUT_USEC = 2000;

    boolean doLoop = false;

    private void processVideoFrame() {
        doLoop = false;
        boolean touchOriginal = !slow_motion || !FRAME_REPETITIONS_ON || (frameRepetitionCompleted || !firstFrameRendered);
        int decoderStatus = -1;
        renderable = false;
        if (touchOriginal) {
            try {
                decoderStatus = mDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }

            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (VERBOSE) Log.d(TAG, "no output from decoder available");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not important for us, since we're using Surface
                if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mDecoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
            } else if (decoderStatus < 0) {
                throw new RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus);
            } else { // decoderStatus >= 0
                doLoop = false;
                if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                        " (size=" + mBufferInfo.size + ")");
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "output EOS");
                    if (mLoop) {
                        doLoop = true;
                    } else {
                        outputDone = true;
                    }
                }
                orig_curr_index++;
                orig_curr_ts = mBufferInfo.presentationTimeUs;
                if(VERBOSE) {
                    Log.d(TAG, "Original timestamp of key frame - " + orig_curr_ts);
                }
                renderable = true;
            }
            frameRepetitionCompleted = false;
        } else {
            repeatable = FRAME_REPETITIONS_ON && slow_motion;
            if(VERBOSE) {
                Log.d(TAG, "Not touching this frame");
            }
        }

        boolean doRender = false;
        if (renderable) {
            doRender = (mBufferInfo.size != 0);
        }

        if (renderable || repeatable) {

            try {
                if (ENABLE_AUDIO && ((mRenderTargetType & RENDER_TARGET_DISPLAY) >0) && mAudioPlayer != null && (!mAudioPlayer.isPlaying() || doLoop)) {
                    if (mAudioPlayer.isPlaying())
                        mAudioPlayer.pause();

                    mAudioPlayer.seekTo(0);

                    if (!mAudioPlayer.isPlaying())
                        mAudioPlayer.start();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }


//            if (doRender && mFrameCallback != null) {
//                doRender = mFrameCallback.preRender(mBufferInfo.presentationTimeUs);
//            }

            try {
                //Time to finally render the frame
                renderFrame(decoderStatus);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

            if (doRender && mFrameCallback != null) {
                mFrameCallback.postRender();
            }
        }

        if (renderable) {
            if (doLoop) {
                replay(true);
            }
        }
    }

    private void replay() {
        replay(false);
    }

    private void replay(boolean flushBuffers) {
        Log.d(TAG, "Looping from beginning");
        inputDone = false;
        if(flushBuffers) {
            mDecoder.flush();
        }
        mExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        mFrameCallback.loopReset();

        resetCounters();
    }

    public void resetCounters() {
        inputChunk = 0;

        inputDone = false;
        outputDone = false;
        firstFrameTs = 0;

        incrementCounter = 0;
        frameRepetitionCompleted = false;
        firstFrameRendered = false;

        orig_curr_index = -1;
        mod_curr_index = 0;
        orig_curr_ts = 0;
        mod_curr_ts = 0;
        orig_prev_index = 0;
        mod_prev_index = 0;
        orig_prev_ts = 0;
        mod_prev_ts = 0;

        renderable = false;
        repeatable = false;
    }

    private void renderFrame(int decoderStatus) throws IllegalStateException {
        if(VERBOSE) {
            Log.d(TAG, "Rendering frame!");
        }
        boolean render = false;
        boolean skip = false;
        boolean originalFrame = false;
        if (slow_motion) {
            if(VERBOSE) {
                Log.d(TAG, "Rendering slow motion video frame");
            }
            mod_curr_ts = (long) (orig_curr_ts * (1.0d / speedFactor));
            // Repeat the previous k frames depending on the speed factor
            // before feeding the next frame of right index from the original video
            if (!frameRepetitionCompleted && (incrementCounter == 0)) {
                originalFrame = true;
                if(VERBOSE) {
                    Log.d(TAG, "Key Frame index: " + orig_curr_index + " new Frame index: " + mod_curr_index + ", Timestamp: " + mod_curr_ts);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !CODEC_RENDERER_COMPAT_MODE) {
                    mDecoder.releaseOutputBuffer(decoderStatus, mod_curr_ts * 1000);
                } else {
                    mFrameReceiver.setFrameTimestamp(mod_curr_ts * 1000);
                    mDecoder.releaseOutputBuffer(decoderStatus, true);
                }
                incrementCounter++;
                if (incrementCounter == frameRepeatCount) {
                    frameRepetitionCompleted = true;
                    incrementCounter = 0;
                }
                render = true;
            } else if (!frameRepetitionCompleted && (incrementCounter < frameRepeatCount)) {
                if(VERBOSE) {
                    Log.d(TAG, "Previous modified time stamp: " + mod_prev_ts);
                }
                long inter_timestamp = mod_curr_ts + (incrementCounter * (mod_curr_ts - mod_prev_ts)) / frameRepeatCount;
                if(VERBOSE) {
                    Log.d(TAG, "Intermediate Frame index: " + incrementCounter + " new Frame index: " + mod_curr_index + ", Timestamp: " + inter_timestamp);
                }

                mFrameReceiver.requestRender(false, inter_timestamp * 1000);
                // give the call back to repeat the previous frame
                incrementCounter++;
                if (incrementCounter == frameRepeatCount) {
                    frameRepetitionCompleted = true;
                    incrementCounter = 0;
                }
                render = true;
            } else {
                Log.d(TAG, "wtf's hppng");
            }
        } else if (fast_motion) {
            if(VERBOSE) {
                Log.d(TAG, "Rendering fast motion video frame");
            }
            originalFrame = true;
            if(VERBOSE) {
                Log.d(TAG, "CHECK orig index: " + orig_curr_index + " mod index: " + mod_curr_index);
            }
            if ((mod_curr_index == 0) || ((int) (((float) orig_curr_index) / mod_curr_index / speedFactor) >= 1)) {
                if(VERBOSE) {
                    Log.d(TAG, "Render orig index: " + orig_curr_index + " mod index: " + mod_curr_index);
                }
                mod_curr_ts = (long) ((float) orig_curr_ts / speedFactor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !CODEC_RENDERER_COMPAT_MODE) {
                    mDecoder.releaseOutputBuffer(decoderStatus, mod_curr_ts * 1000);
                } else {
                    mFrameReceiver.setFrameTimestamp(mod_curr_ts * 1000);
                    mDecoder.releaseOutputBuffer(decoderStatus, true);
                }
                if(VERBOSE) {
                    Log.d(TAG, "modified fast fwd timestamp: " + mod_curr_ts);
                }
                render = true;
            } else {
                skip = true;
                mDecoder.releaseOutputBuffer(decoderStatus, false);
                if(VERBOSE) {
                    Log.d(TAG, "Skip orig index: " + orig_curr_index + " mod index: " + mod_curr_index);
                }
            }
            frameRepetitionCompleted = true;
        } else {
            if(VERBOSE) {
                Log.d(TAG, "Rendering normal motion video frame");
            }
            render = true;
            originalFrame = true;
            mod_curr_ts = orig_curr_ts;

            if(VERBOSE) {
                Log.d(TAG, "Key Frame index: " + orig_curr_index + ", Timestamp : " + mod_curr_ts);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !CODEC_RENDERER_COMPAT_MODE) {
                mDecoder.releaseOutputBuffer(decoderStatus, mod_curr_ts * 1000);
            } else {
                mFrameReceiver.setFrameTimestamp(mod_curr_ts * 1000);
                mDecoder.releaseOutputBuffer(decoderStatus, true);
//                mFrameReceiver.requestRender(true, mod_curr_ts * 1000);
            }
            frameRepetitionCompleted = true;
        }

        //No need to go beyond if the frame was neither rendered nor skipped.
        if (!render && !skip) return;

        if (!firstFrameRendered) {
            orig_prev_ts = orig_curr_ts - 40000;
        } else if (frameRepetitionCompleted) {
            orig_prev_ts = orig_curr_ts;
        }

        if (originalFrame) {
            orig_prev_index = orig_curr_index;
        }

        //No need to go beyond if the frame was not rendered.
        if (!render) return;

        long tsDiff = 0;
        if (!firstFrameRendered) {
            mod_prev_ts = (long) (orig_curr_ts - 40000 * 1.0 / speedFactor);
            tsDiff = mod_curr_ts - mod_prev_ts;
            firstFrameRendered = true;
            firstFrameTs = System.currentTimeMillis();
        } else if (frameRepetitionCompleted) {
            tsDiff = mod_curr_ts - mod_prev_ts;
            mod_prev_ts = mod_curr_ts;
        }
        mod_prev_index = mod_curr_index++;

        if((mRenderTargetType & RENDER_TARGET_VIDEO) > 0) {
            long t1 = System.currentTimeMillis();
            Log.d(TAG, "Waiting for signal to decode : " + t1);
            synchronized (mRenderLock) {
                try {
                    mRenderLock.wait(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Got the signal to decode! : " + (System.currentTimeMillis()-t1));
        }
        if (((mRenderTargetType & RENDER_TARGET_DISPLAY) > 0) && (tsDiff >= 0)) {
//            Log.d(TAG, "waiting for display renderer before proceeding to next frame");
            while (System.currentTimeMillis() - firstFrameTs < tsDiff / 1000) {
                long sleepTime = 1;
                try {
                    if (!mIsStopRequested)
                        Thread.sleep(sleepTime);
                    else
                        break; //Stop the decoder sleep if stopped
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            Log.d(TAG, "done with display renderer, proceeding to next frame");
            firstFrameTs += tsDiff / 1000;
        }
    }

    private void seekIfNeeded() {

    }

    private Runnable mAudioPlaybackTask = new Runnable() {
        @Override
        public void run() {
            try {
                if ((mRenderTargetType & RENDER_TARGET_DISPLAY) == 0) return;
                if(VERBOSE) {
                    Log.d(TAG, "Audio player starting");
                }
//                    mAudioPlayer = MediaPlayer.create(mContext, Uri.parse("rtsp://v8.cache3.c.youtube.com/CjgLENy73wIaLwlQP1m32SiSYxMYJCAkFEIJbXYtZ29vZ2xlSARSB3JlbGF0ZWRggqG7w9aS2-1MDA==/0/0/0/video.3gp"));
//                    mAudioPlayer = MediaPlayer.create(mContext, Uri.parse("/mnt/sdcard/Movies/InstaLively/16-04-26-17-23-14_1461671594559.mp4"));
                mAudioPlayer = new MediaPlayer();
//                    mAudioPlayer.setDataSource(mContext, Uri.parse("/mnt/sdcard/Movies/InstaLively/16-04-26-17-23-14_1461671594559.mp4"));
                mAudioPlayer.setDataSource(mContext, Uri.parse(mSourceFilePath));
                mAudioPlayer.setSurface(null);
//                    mAudioPlayer.setDataSource("rtsp://v8.cache3.c.youtube.com/CjgLENy73wIaLwlQP1m32SiSYxMYJCAkFEIJbXYtZ29vZ2xlSARSB3JlbGF0ZWRggqG7w9aS2-1MDA==/0/0/0/video.3gp");
                mAudioPlayer.prepare();
//                mAudioPlayer.setLooping(true);
//                mAudioPlayer.start();
                if(VERBOSE) {
                    Log.d(TAG, "Audio player started");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static class LocalHandler extends Handler {

        public LocalHandler() {
            super();
        }

        public LocalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;

/*            switch (what) {
                case MSG_PLAY_STOPPED:
                    PlayerFeedback fb = (PlayerFeedback) msg.obj;
                    fb.playbackStopped();
*//*                       if(mFrameReceiver != null) mFrameReceiver.playerReady();*//*
                    break;
                default:
                    throw new RuntimeException("Unknown msg " + what);
            }*/
        }
    }

    /**
     * Thread helper for video playback.
     * <p/>
     * The PlayerFeedback callbacks will execute on the thread that creates the object,
     * assuming that thread has a looper.  Otherwise, they will execute on the main looper.
     */
    public static class PlayTask implements Runnable {

        private MoviePlayer mPlayer;
        PlayerFeedback mFeedback;
        private boolean mDoLoop;
        private HandlerThread mVideoRenderEncodeThread;
        private LocalHandler mVideoRenderEncodeHandler;

        private final Object mStopLock = new Object();
        private volatile boolean mStopped = false;

        private volatile boolean mStarted = false;
        /**
         * Prepares new PlayTask.
         *
         * @param player   The player object, configured with control and output.
         * @param feedback UI feedback object.
         */
        public PlayTask(MoviePlayer player, PlayerFeedback feedback) {
            mPlayer = player;
            mFeedback = feedback;
        }

        /**
         * Sets the loop mode.  If true, playback will loop forever.
         */
        public void setLoopMode(boolean loopMode) {
            mDoLoop = loopMode;
        }

        /**
         * Creates a new thread, and starts execution of the player.
         */
        public void execute() {
            if (mPlayer == null) return;
//            mStopped = false;
            if(mStarted) {
                mPlayer.resume();
            } else {
                mStarted = true;
                mPlayer.setLoopMode(mDoLoop);
                mVideoRenderEncodeThread = new HandlerThread("Movie Player", Thread.NORM_PRIORITY);
                mVideoRenderEncodeThread.start();
                mVideoRenderEncodeHandler = new LocalHandler(mVideoRenderEncodeThread.getLooper());
                mVideoRenderEncodeHandler.post(this);
                if (mPlayer.getRenderTargetType()==RENDER_TARGET_DISPLAY) {
                    Log.d(TAG, "Turning audio on");
                    mPlayer.turnOnAudio();
                }
            }
        }

        /**
         * Requests that the player stop.
         * <p/>
         * Called from arbitrary thread.
         */
        public void requestStop() {
            mPlayer.requestStop();
        }

        /**
         * Wait for the player to stop.
         * <p/>
         * Called from any thread other than the PlayTask thread.
         */
        public void waitForStop() {
            synchronized (mStopLock) {
                while (!mStopped) {
                    try {
                        mStopLock.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void run() {
            boolean videoSuccess = false;
            try {
                mStopped = false;
                videoSuccess = mPlayer.play();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                mFeedback.closePlayer();
            } finally {
                if (mPlayer.getRenderTargetType()==RENDER_TARGET_DISPLAY) {
                    mPlayer.turnOffAudio();
                }

                mFeedback.playbackStopped(mPlayer.getRenderTargetType(), videoSuccess);
                // tell anybody waiting on us that we're done
                synchronized (mStopLock) {
                    mStarted = false;
                    mStopped = true;
                    mStopLock.notifyAll();
                }

                // Send message through Handler so it runs on the right thread.
/*                mVideoRenderEncodeHandler.sendMessage(
                        mVideoRenderEncodeHandler.obtainMessage(MSG_PLAY_STOPPED, mFeedback));*/
/*                mVideoRenderEncodeThread.quitSafely();*/
            }
        }

    }

    private void turnOnAudio() {
//        ENABLE_AUDIO = ENABLE_AUDIO && ((mRenderTargetType & RENDER_TARGET_DISPLAY) > 0);
        if (ENABLE_AUDIO) {
            mAudioPlaybackThread = new HandlerThread("Audio Player", Thread.NORM_PRIORITY);
            mAudioPlaybackThread.start();
            mAudioPlaybackHandler = new LocalHandler(mAudioPlaybackThread.getLooper());
            mAudioPlaybackHandler.post(mAudioPlaybackTask);
        }
    }

    private void turnOffAudio() {
        if (ENABLE_AUDIO) {
            try {
                if (mAudioPlayer != null && mAudioPlayer.isPlaying()) {
                    Log.d(TAG, "Audio player stop");
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mAudioPlayer != null) {
                        mAudioPlayer.stop();
                        mAudioPlayer.reset();
                        Log.d(TAG, "Releasing audio player");
                        mAudioPlayer.release();
                        mAudioPlayer = null;
                    }
                    if (mAudioPlaybackThread != null) {
                        mAudioPlaybackThread.quitSafely();
                        mAudioPlaybackThread = null;
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } finally {
                    if (mAudioPlayer != null) {
                        Log.d(TAG, "Finally Releasing audio player");
                        mAudioPlayer.release();
                        mAudioPlayer = null;
                    }
                    if (mAudioPlaybackThread != null) {
                        mAudioPlaybackThread.quitSafely();
                        mAudioPlaybackThread = null;
                    }
                }
            }
        }
    }

    private void pauseAndResetAudio() {
        if (ENABLE_AUDIO) {
            try {
                if (mAudioPlayer != null) {
                    Log.d(TAG, "Audio player - Pausing");
                    if (mAudioPlayer.isPlaying()) {
                        mAudioPlayer.pause();
                    }
                    mAudioPlayer.seekTo(0);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
}




/**
 * Callback invoked when rendering video frames.  The MoviePlayer client must
 * provide one of these.
 */
interface FrameCallback {
    /**
     * Called immediately before the frame is rendered.
     *
     * @param presentationTimeUsec The desired presentation time, in microseconds.
     */
    boolean preRender(long presentationTimeUsec);

    /**
     * Called immediately after the frame render call returns.  The frame may not have
     * actually been rendered yet.
     * TODO: is this actually useful?
     */
    void postRender();

    /**
     * Called after the last frame of a looped movie has been rendered.  This allows the
     * callback to adjust its expectations of the next presentation time stamp.
     */
    void loopReset();
}
