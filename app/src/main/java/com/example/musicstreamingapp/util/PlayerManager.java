package com.example.musicstreamingapp.util;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    private static PlayerManager instance;
    private ExoPlayer player;
    private Track currentTrack;
    private List<Track> queue = new ArrayList<>();
    private int currentIndex = 0;
    private OnTrackChangeListener listener;
    /** Listener PHỤ chỉ phục vụ đếm quảng cáo — không bị UI ghi đè, không ảnh hưởng phát nhạc. */
    private OnTrackChangeListener adListener;

    public interface OnTrackChangeListener {
        void onTrackChanged(Track track);
        void onPlayStateChanged(boolean isPlaying);
    }

    private PlayerManager() {}

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    public void init(Context ctx) {
        if (player == null) {
            // setEnableAudioTrackPlaybackParams(false) disables hardware speed adjustment via
            // AudioTrack.setPlaybackParams(). This prevents speed drift caused by unreliable
            // AudioTrack timestamps on Android emulators (sdk_gphone64_x86_64, API 36+).
            DefaultRenderersFactory renderersFactory =
                new DefaultRenderersFactory(ctx.getApplicationContext())
                    .setEnableAudioTrackPlaybackParams(false);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

            player = new ExoPlayer.Builder(ctx.getApplicationContext(), renderersFactory)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
                .setHandleAudioBecomingNoisy(true)
                .build();

            // Explicitly lock speed to 1x – guards against any internal adjustment
            player.setPlaybackParameters(PlaybackParameters.DEFAULT);

            player.addListener(new androidx.media3.common.Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (listener != null) listener.onPlayStateChanged(isPlaying);
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == androidx.media3.common.Player.STATE_ENDED) {
                        playNext();
                    }
                }
            });
        }
    }

    public void play(Context ctx, Track track, List<Track> trackList, int index) {
        init(ctx);
        this.queue = trackList != null ? new ArrayList<>(trackList) : new ArrayList<>();
        if (!this.queue.contains(track)) this.queue.add(0, track);
        this.currentIndex = this.queue.indexOf(track);
        this.currentTrack = track;
        String url = RetrofitClient.BASE_MEDIA_URL + track.getAudioUrl();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        if (listener != null) listener.onTrackChanged(track);
        if (adListener != null) adListener.onTrackChanged(track);
    }

    public void playNext() {
        if (queue.isEmpty()) return;
        currentIndex = (currentIndex + 1) % queue.size();
        Track next = queue.get(currentIndex);
        currentTrack = next;
        String url = RetrofitClient.BASE_MEDIA_URL + next.getAudioUrl();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        if (listener != null) listener.onTrackChanged(next);
        if (adListener != null) adListener.onTrackChanged(next);
    }

    public void playPrevious() {
        if (queue.isEmpty()) return;
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        Track prev = queue.get(currentIndex);
        currentTrack = prev;
        String url = RetrofitClient.BASE_MEDIA_URL + prev.getAudioUrl();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        if (listener != null) listener.onTrackChanged(prev);
        if (adListener != null) adListener.onTrackChanged(prev);
    }

    public void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) player.pause();
        else player.play();
    }

    public void pause() {
        if (player != null) player.pause();
    }

    public void resume() {
        if (player != null) player.play();
    }

    public void seekTo(long ms) {
        if (player != null) player.seekTo(ms);
    }

    public void seekForward() {
        if (player != null) player.seekTo(player.getCurrentPosition() + 5000);
    }

    public void seekBackward() {
        if (player != null) player.seekTo(Math.max(0, player.getCurrentPosition() - 5000));
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public Track getCurrentTrack() { return currentTrack; }
    public ExoPlayer getPlayer() { return player; }

    public void setListener(OnTrackChangeListener l) { this.listener = l; }

    /** Gắn listener phụ cho đếm quảng cáo (tách biệt với listener UI). */
    public void setAdListener(OnTrackChangeListener l) { this.adListener = l; }

    public void release() {
        if (player != null) { player.release(); player = null; }
        instance = null;
    }
}
