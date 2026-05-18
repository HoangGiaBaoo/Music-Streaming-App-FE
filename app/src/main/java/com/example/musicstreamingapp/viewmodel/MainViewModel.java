package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.util.PlayerManager;

public class MainViewModel extends ViewModel implements PlayerManager.OnTrackChangeListener {

    private final UserRepository userRepo;

    private final MutableLiveData<UserMe> userMe = new MutableLiveData<>();
    private final MutableLiveData<Track> currentTrack = new MutableLiveData<>();
    private final MutableLiveData<Boolean> playing = new MutableLiveData<>(false);
    private final MutableLiveData<String> usernameLetter = new MutableLiveData<>("U");

    public MainViewModel(UserRepository userRepo) {
        this.userRepo = userRepo;
        attachListener();
        currentTrack.setValue(PlayerManager.getInstance().getCurrentTrack());
        playing.setValue(PlayerManager.getInstance().isPlaying());
    }

    public LiveData<UserMe> userMe() { return userMe; }
    public LiveData<Track> currentTrack() { return currentTrack; }
    public LiveData<Boolean> playing() { return playing; }
    public LiveData<String> usernameLetter() { return usernameLetter; }

    public void updateUsername(String username) {
        String letter = (username == null || username.isEmpty())
            ? "U" : username.substring(0, 1).toUpperCase();
        usernameLetter.setValue(letter);
    }

    public void loadDrawerHeader() {
        userRepo.getMe(new RepoCallback<UserMe>() {
            @Override public void onSuccess(UserMe data) { userMe.postValue(data); }
            @Override public void onError(String message) { /* keep stale value */ }
        });
    }

    public void clearUserMe() {
        userMe.setValue(null);
    }

    public void onMiniPlayPauseClicked() {
        PlayerManager.getInstance().togglePlayPause();
        playing.setValue(PlayerManager.getInstance().isPlaying());
    }

    public void refreshFromPlayer() {
        attachListener();
        currentTrack.setValue(PlayerManager.getInstance().getCurrentTrack());
        playing.setValue(PlayerManager.getInstance().isPlaying());
    }

    private void attachListener() {
        PlayerManager.getInstance().setListener(this);
    }

    @Override
    public void onTrackChanged(Track track) { currentTrack.postValue(track); }

    @Override
    public void onPlayStateChanged(boolean isPlaying) { playing.postValue(isPlaying); }

    @Override
    protected void onCleared() {
        super.onCleared();
        PlayerManager.getInstance().setListener(null);
    }
}
