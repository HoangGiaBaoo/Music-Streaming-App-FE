package com.example.musicstreamingapp.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.PlayerRepository;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.PlayerManager;

public class PlayerViewModel extends ViewModel implements PlayerManager.OnTrackChangeListener {

    private static final long POLL_INTERVAL_MS = 500L;

    private final PlayerRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Track> currentTrack = new MutableLiveData<>();
    private final MutableLiveData<Boolean> playing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> liked = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> following = new MutableLiveData<>(false);
    private final MutableLiveData<long[]> progress = new MutableLiveData<>(new long[]{0L, 0L});

    private final Runnable poller = new Runnable() {
        @Override public void run() {
            long pos = PlayerManager.getInstance().getCurrentPosition();
            long dur = PlayerManager.getInstance().getDuration();
            progress.setValue(new long[]{pos, dur});
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public PlayerViewModel(PlayerRepository repo) {
        this.repo = repo;
        PlayerManager.getInstance().setListener(this);
        currentTrack.setValue(PlayerManager.getInstance().getCurrentTrack());
        playing.setValue(PlayerManager.getInstance().isPlaying());
        handler.post(poller);
    }

    public LiveData<Track> currentTrack() { return currentTrack; }
    public LiveData<Boolean> playing() { return playing; }
    public LiveData<Boolean> liked() { return liked; }
    public LiveData<Boolean> following() { return following; }
    public LiveData<long[]> progress() { return progress; }

    public void onPlayPauseClicked() {
        PlayerManager.getInstance().togglePlayPause();
        playing.setValue(PlayerManager.getInstance().isPlaying());
    }

    public void onNextClicked() { PlayerManager.getInstance().playNext(); }
    public void onPrevClicked() { PlayerManager.getInstance().playPrevious(); }
    public void onSeek(int positionMs) { PlayerManager.getInstance().seekTo(positionMs); }

    public void onLikeClicked() {
        Track t = PlayerManager.getInstance().getCurrentTrack();
        if (t == null || t.getTrackId() == null) return;
        boolean prev = Boolean.TRUE.equals(liked.getValue());
        liked.setValue(!prev);
        repo.toggleLike(t.getTrackId(), null);
    }

    public void onFollowArtistClicked() {
        Track t = PlayerManager.getInstance().getCurrentTrack();
        if (t == null || t.getArtist() == null || t.getArtist().getArtistId() == null) return;
        boolean prev = Boolean.TRUE.equals(following.getValue());
        following.setValue(!prev);
        repo.toggleFollow(t.getArtist().getArtistId(), new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { /* server confirmed */ }
            @Override public void onError(String message) { following.postValue(prev); }
        });
    }

    public void recordPlay(Long trackId) {
        if (trackId == null) return;
        repo.recordPlay(trackId, null);
    }

    @Override
    public void onTrackChanged(Track track) {
        currentTrack.postValue(track);
        liked.postValue(false);
        following.postValue(false);
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        playing.postValue(isPlaying);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(poller);
        PlayerManager.getInstance().setListener(null);
    }
}
