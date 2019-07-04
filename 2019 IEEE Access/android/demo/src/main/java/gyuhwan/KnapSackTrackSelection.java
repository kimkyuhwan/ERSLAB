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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by erslab-gh on 2018-01-29.
 */

public class KnapSackTrackSelection extends BaseTrackSelection
{


    public static final class Factory implements TrackSelection.Factory{
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
        public KnapSackTrackSelection createTrackSelection(TrackGroup group, int... tracks) {
            return new KnapSackTrackSelection(group, tracks, bandwidthMeter, maxInitialBitrate,
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

    private ArrayList<Integer> KnapSackOutputList;
    private FakeNetwork fakeNetwork;
    private int knapsack_frame_count;


    private long ctime=0;

    /**
     * @param group          The {@link TrackGroup}.
     * @param tracks         The indices of the selected tracks within the {@link TrackGroup}. Must not be
     *                       empty. May be in any order.
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     */
    public KnapSackTrackSelection(TrackGroup group, int[] tracks,
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
    public KnapSackTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter,
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
        readKnapSackOuputFile(PlayerClock.getInstance().getKnapsackSelectFileName());
        Log.d("DEBUGYU",PlayerClock.getInstance().getSelectFileName()+" ::");
        // selectedIndex = determineIdealSelectedIndex(Long.MIN_VALUE,ctime);
        reason = C.SELECTION_REASON_INITIAL;
        fakeNetwork=new FakeNetwork();

    }

    private void readKnapSackOuputFile(final String fileUrl){
        readThread readThread=new readThread(fileUrl);
        readThread.start();

        try {
            readThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class readThread extends Thread{
        private String fileUrl="";

        readThread(){
            fileUrl="";
        }
        readThread(String fileUrl){
            this.fileUrl=fileUrl;
        }
        @Override
        public void run() {
            String txtpath = "http://165.246.43.96/knapsack_output/"+fileUrl;
            Log.d("DEBUGYU","path : "+txtpath);
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(txtpath);
            KnapSackOutputList=new ArrayList<Integer>();
            knapsack_frame_count=0;
            try {
                HttpResponse httpResponse = httpClient.execute(getRequest);
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {

                } else {
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        KnapSackOutputList.add(Integer.parseInt(line));
                    }
                    inputStream.close();
                }
                Log.d("DEBUGYU","KnapSackOutputList Size : "+String.valueOf(KnapSackOutputList.size()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            for(int i=0;i<KnapSackOutputList.size();i++){
                Log.d("DDDDDD","index "+i+" : "+KnapSackOutputList.get(i));
            }
        }
    }

    public int getFrameTime(){
        int index=knapsack_frame_count;
        index=Math.min(index,KnapSackOutputList.size()-1);
        Log.d("DEBUGYU_BUFFER_SELECT",": "+"Index :"+index+">>"+(4-KnapSackOutputList.get(index)));
        knapsack_frame_count+=PlayerClock.getInstance().getAddFrame();
        return 4-KnapSackOutputList.get(index);
    }

    @Override
    public void updateSelectedTrack(long bufferedDurationUs) {
        long nowMs = SystemClock.elapsedRealtime();
        int currentSelectedIndex = selectedIndex;
        selectedIndex=0;
        ctime=PlayerClock.getInstance().getFrame();
        selectedIndex = determineIdealSelectedIndex(nowMs, getFrameTime());

        // If we adapted, update the trigger.
        if (selectedIndex != currentSelectedIndex) {
            reason = C.SELECTION_REASON_ADAPTIVE;
        }
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
        int lowestBitrateNonBlacklistedIndex = 0;
        effectiveBitrate=fakeNetwork.makeNetworkSpeed(0);
        Log.d("DEBUGYU-NETWORK","fake network = "+effectiveBitrate);
        for (int i = start; i < length; i++) {
            if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
                Format format = getFormat(i);
                if (format.bitrate <= effectiveBitrate) {
                    return i;
                } else {
                    lowestBitrateNonBlacklistedIndex = i;
                }
            }
        }
        return lowestBitrateNonBlacklistedIndex;
    }


}
