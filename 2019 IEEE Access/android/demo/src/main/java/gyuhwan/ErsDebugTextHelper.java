package gyuhwan;

import android.util.Log;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;

/**
 * Created by erslab-gh on 2017-11-28.
 */

public class ErsDebugTextHelper implements Runnable, ExoPlayer.EventListener {
    private static final int REFRESH_INTERVAL_MS = 1000;
    private static final String TAG="DEBUGYU";
    private final SimpleExoPlayer player;
    private final TextView textView;

    private boolean started;

    /**
     * @param player The {@link SimpleExoPlayer} from which debug information should be obtained.
     * @param textView The {@link TextView} that should be updated to display the information.
     */
    public ErsDebugTextHelper(SimpleExoPlayer player, TextView textView) {
        this.player = player;
        this.textView = textView;
    }

    /**
     * Starts periodic updates of the {@link TextView}. Must be called from the application's main
     * thread.
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        player.addListener(this);
        updateAndPost();
    }

    /**
     * Stops periodic updates of the {@link TextView}. Must be called from the application's main
     * thread.
     */
    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        player.removeListener(this);
        textView.removeCallbacks(this);
    }

    // ExoPlayer.EventListener implementation.

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        updateAndPost();
    }

    @Override
    public void onPositionDiscontinuity() {
        updateAndPost();
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Do nothing.
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Do nothing.
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        // Do nothing.
    }

    @Override
    public void onTracksChanged(TrackGroupArray tracks, TrackSelectionArray selections) {
        // Do nothing.
    }

    // Runnable implementation.

    @Override
    public void run() {
        updateAndPost();
    }

    // Private methods.

    private void updateAndPost() {
        //1초마다 실행되며 실행시 DebugText를 바꾼다.
        textView.setText(getPlayerStateString() + getPlayerWindowIndexString() + getVideoString()
                + getAudioString()+"Hello");
        textView.removeCallbacks(this);
        textView.postDelayed(this, REFRESH_INTERVAL_MS);
    }

    private String getPlayerStateString() {
        String text = "playWhenReady:" + player.getPlayWhenReady() + " playbackState:";
        Log.d("DDB","state : "+player.getPlaybackState());
        switch (player.getPlaybackState()) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                PlayerClock.getInstance().InitPrevframe();
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                PlayerClock.getInstance().InitPrevframe();
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
        return text;
    }

    private String getPlayerWindowIndexString() {
        return " window:" + player.getCurrentWindowIndex();
    }

    private String getVideoString() {
        Format format = player.getVideoFormat();
        if (format == null) {
            return "";
        }
        return "\n" + format.sampleMimeType + "(id:" + format.id + " r:" + format.width + "x"
                + format.height + getDecoderCountersBufferCountString(player.getVideoDecoderCounters())
                + ")";
    }

    private String getAudioString() {
        Format format = player.getAudioFormat();
        if (format == null) {
            return "";
        }
        return "\n" + format.sampleMimeType + "(id:" + format.id + " hz:" + format.sampleRate + " ch:"
                + format.channelCount
                + getDecoderCountersBufferCountString(player.getAudioDecoderCounters()) + ")";
    }

    private static String getDecoderCountersBufferCountString(DecoderCounters counters) {
        if (counters == null) {
            return "";
        }
        counters.ensureUpdated();
        return " rb:" + counters.renderedOutputBufferCount
                + " sb:" + counters.skippedOutputBufferCount
                + " db:" + counters.droppedOutputBufferCount
                + " mcdb:" + counters.maxConsecutiveDroppedOutputBufferCount;
    }

}
