package gyuhwan;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.trackselection.BaseTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static gyuhwan.Constants.FAKE_NETWORK_TYPE;

/**
 * Created by erslab-gh on 2017-09-05.
 */

public class ErsAdaptiveTrackSelection extends BaseTrackSelection {

    /**
     * Factory for {@link com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection} instances.
     */
    public static final class Factory implements TrackSelection.Factory {

        private final BandwidthMeter bandwidthMeter;
        private final int maxInitialBitrate;
        private final int minDurationForQualityIncreaseMs;
        private final int maxDurationForQualityDecreaseMs;
        private final int minDurationToRetainAfterDiscardMs;
        private final float bandwidthFraction;

        /**
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
         */
        public Factory(BandwidthMeter bandwidthMeter) {
            this(bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                    DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                    DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                    DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
        }

        /**
         * @param bandwidthMeter                    Provides an estimate of the currently available bandwidth.
         * @param maxInitialBitrate                 The maximum bitrate in bits per second that should be assumed
         *                                          when a bandwidth estimate is unavailable.
         * @param minDurationForQualityIncreaseMs   The minimum duration of buffered data required for
         *                                          the selected track to switch to one of higher quality.
         * @param maxDurationForQualityDecreaseMs   The maximum duration of buffered data required for
         *                                          the selected track to switch to one of lower quality.
         * @param minDurationToRetainAfterDiscardMs When switching to a track of significantly higher
         *                                          quality, the selection may indicate that media already buffered at the lower quality can
         *                                          be discarded to speed up the switch. This is the minimum duration of media that must be
         *                                          retained at the lower quality.
         * @param bandwidthFraction                 The fraction of the available bandwidth that the selection should
         *                                          consider available for use. Setting to a value less than 1 is recommended to account
         *                                          for inaccuracies in the bandwidth estimator.
         */
        public Factory(BandwidthMeter bandwidthMeter, int maxInitialBitrate,
                       int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs,
                       int minDurationToRetainAfterDiscardMs, float bandwidthFraction) {
            this.bandwidthMeter = bandwidthMeter;
            this.maxInitialBitrate = maxInitialBitrate;
            this.minDurationForQualityIncreaseMs = minDurationForQualityIncreaseMs;
            this.maxDurationForQualityDecreaseMs = maxDurationForQualityDecreaseMs;
            this.minDurationToRetainAfterDiscardMs = minDurationToRetainAfterDiscardMs;
            this.bandwidthFraction = bandwidthFraction;
        }

        @Override
        public ErsAdaptiveTrackSelection createTrackSelection(TrackGroup group, int... tracks) {
            return new ErsAdaptiveTrackSelection(group, tracks, bandwidthMeter, maxInitialBitrate,
                    minDurationForQualityIncreaseMs, maxDurationForQualityDecreaseMs,
                    minDurationToRetainAfterDiscardMs, bandwidthFraction);
        }

    }

    public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;
    public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
    public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
    public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
    public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;

    private final BandwidthMeter bandwidthMeter;
    private final int maxInitialBitrate;
    private final long minDurationForQualityIncreaseUs;
    private final long maxDurationForQualityDecreaseUs;
    private final long minDurationToRetainAfterDiscardUs;
    private final float bandwidthFraction;

    private int selectedIndex;
    private int reason;
    private KnapsackConnection connection;
    private FakeNetwork fakeNetwork;
    private long ctime=0;
    private int segmentIndex=0;

    /**
     * @param group          The {@link TrackGroup}.
     * @param tracks         The indices of the selected tracks within the {@link TrackGroup}. Must not be
     *                       empty. May be in any order.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     */
    public ErsAdaptiveTrackSelection(TrackGroup group, int[] tracks,
                                     BandwidthMeter bandwidthMeter) {
        this(group, tracks, bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
    }

    /**
     * @param group                             The {@link TrackGroup}.
     * @param tracks                            The indices of the selected tracks within the {@link TrackGroup}. Must not be
     *                                          empty. May be in any order.
     * @param bandwidthMeter                    Provides an estimate of the currently available bandwidth.
     * @param maxInitialBitrate                 The maximum bitrate in bits per second that should be assumed when a
     *                                          bandwidth estimate is unavailable.
     * @param minDurationForQualityIncreaseMs   The minimum duration of buffered data required for the
     *                                          selected track to switch to one of higher quality.
     * @param maxDurationForQualityDecreaseMs   The maximum duration of buffered data required for the
     *                                          selected track to switch to one of lower quality.
     * @param minDurationToRetainAfterDiscardMs When switching to a track of significantly higher
     *                                          quality, the selection may indicate that media already buffered at the lower quality can
     *                                          be discarded to speed up the switch. This is the minimum duration of media that must be
     *                                          retained at the lower quality.
     * @param bandwidthFraction                 The fraction of the available bandwidth that the selection should
     *                                          consider available for use. Setting to a value less than 1 is recommended to account
     *                                          for inaccuracies in the bandwidth estimator.
     */
    public ErsAdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter,
                                     int maxInitialBitrate, long minDurationForQualityIncreaseMs,
                                     long maxDurationForQualityDecreaseMs, long minDurationToRetainAfterDiscardMs,
                                     float bandwidthFraction) {
        super(group, tracks);
        this.bandwidthMeter = bandwidthMeter;
        this.maxInitialBitrate = maxInitialBitrate;
        this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
        this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
        this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
        this.bandwidthFraction = bandwidthFraction;
        this.connection=new KnapsackConnection(PlayerClock.getInstance().getVideo_idx(),PlayerClock.getInstance().getSelectKnapsackEnergyRate());
        Log.d("DEBUGYU_ALGO","video Idx"+PlayerClock.getInstance().getVideo_idx()+"Rate : "+PlayerClock.getInstance().getSelectKnapsackEnergyRate());
        //readMaxResolution(PlayerClock.getInstance().getSelectFileName());
        // selectedIndex = determineIdealSelectedIndex(Long.MIN_VALUE,ctime);
        reason = C.SELECTION_REASON_INITIAL;
        fakeNetwork=new FakeNetwork();

    }

    public int getFrameTime(){
        int selectedVersion=connection.requestCurrentVideoSegmentVersion();
        Log.d("DEBUGYU_BUFFER_SELECT",": "+selectedVersion);
        PlayerClock.getInstance().getAddFrame();
        return 4-selectedVersion;
    }

    @Override
    public void updateSelectedTrack(long bufferedDurationUs) {
        long nowMs = SystemClock.elapsedRealtime();
        // Stash the current selection, then make a new one.
        int currentSelectedIndex = getFrameTime();
        //   Log.d("DEBUGYU","nowMs : "+String.valueOf(nowMs));
        ctime=PlayerClock.getInstance().getFrame();
        Log.d("DEBUGYU-TIME","Current Index = "+currentSelectedIndex);
        selectedIndex = determineIdealSelectedIndex(nowMs, currentSelectedIndex);
        Log.d("DEBUGYU-TIME","Selected Index = "+selectedIndex);

        if (selectedIndex != currentSelectedIndex) {
            reason = C.SELECTION_REASON_ADAPTIVE;
            connection.execute();
        }
        Log.d("DEBUGYU","Select Version = "+selectedIndex);
        connection.setPrevSelectVesion(selectedIndex);
    }

    @Override
    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    public int getSelectionReason() {
        return reason;
    }

    @Override
    public Object getSelectionData() {
        return null;
    }

    @Override
    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        if (queue.isEmpty()) {
            return 0;
        }
        int queueSize = queue.size();
        long bufferedDurationUs = queue.get(queueSize - 1).endTimeUs - playbackPositionUs;
        if (bufferedDurationUs < minDurationToRetainAfterDiscardUs) {
            return queueSize;
        }
        int idealSelectedIndex = determineIdealSelectedIndex(SystemClock.elapsedRealtime(),getFrameTime());
        Format idealFormat = getFormat(idealSelectedIndex);
        // If the chunks contain video, discard from the first SD chunk beyond
        // minDurationToRetainAfterDiscardUs whose resolution and bitrate are both lower than the ideal
        // track.
        for (int i = 0; i < queueSize; i++) {
            MediaChunk chunk = queue.get(i);
            Format format = chunk.trackFormat;
            long durationBeforeThisChunkUs = chunk.startTimeUs - playbackPositionUs;
            if (durationBeforeThisChunkUs >= minDurationToRetainAfterDiscardUs
                    && format.bitrate < idealFormat.bitrate
                    && format.height != Format.NO_VALUE && format.height < 720
                    && format.width != Format.NO_VALUE && format.width < 1280
                    && format.height < idealFormat.height) {
                return i;
            }
        }
        return queueSize;
    }

    private int determineIdealSelectedIndex(long nowMs,int start) {
        long bitrateEstimate = bandwidthMeter.getBitrateEstimate();
        long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);
        effectiveBitrate=fakeNetwork.getConstNetworkSpeed(FAKE_NETWORK_TYPE,segmentIndex++);
        Log.d("DEBUGYU-NETWORK","BSA-DP fake network = "+effectiveBitrate);
        int lowestBitrateNonBlacklistedIndex = 0;
        for (int i = start; i < length; i++) {
            if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
                Format format = getFormat(i);
                if (format.bitrate <= effectiveBitrate) {
                    lowestBitrateNonBlacklistedIndex= i;
                    break;
                } else {
                    lowestBitrateNonBlacklistedIndex = i;
                }
            }
        }
        Log.w("DEBUGYU_ALGO","Selection Index : "+lowestBitrateNonBlacklistedIndex);
        return lowestBitrateNonBlacklistedIndex;
    }

}