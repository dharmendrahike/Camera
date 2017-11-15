package com.pulseapp.android.util;

/**
 * Created by Morph on 8/3/2015.
 */
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.pulseapp.android.util.ExoPlayer.RendererBuilder;
import com.pulseapp.android.util.ExoPlayer.RendererBuilderCallback;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.tx3g.Tx3gParser;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;

/**
 * A {@link RendererBuilder} for streams that can be read using an {@link Extractor}.
 */
public class ExtractorRendererBuilder implements RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 50;

    private final Context context;
    private final String userAgent;
    private final Uri uri;
    private final Extractor extractor;

    public ExtractorRendererBuilder(Context context, String userAgent, Uri uri, Extractor extractor) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
        this.extractor = extractor;
    }

    @Override
    public void buildRenderers(ExoPlayer player, RendererBuilderCallback callback) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource,
                allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 5000, null, player.getMainHandler(),
                player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, player.getMainHandler(), player);
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper(), new Tx3gParser());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[ExoPlayer.RENDERER_COUNT];
        renderers[ExoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[ExoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[ExoPlayer.TYPE_TEXT] = textRenderer;
        callback.onRenderers(null, null, renderers, bandwidthMeter);
    }

}