package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistDetailViewModel extends ViewModel {

    private final LibraryRepository repo;

    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> trackRemoved = new MutableLiveData<>();

    private Long playlistId;
    private boolean loaded = false;

    public PlaylistDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Track>> tracks() { return tracks; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Event<Integer>> trackRemoved() { return trackRemoved; }

    public void setPlaylistId(long id) {
        this.playlistId = id;
    }

    public void loadIfNeeded() {
        if (loaded || playlistId == null) return;
        loaded = true;
        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) { tracks.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    public void onRemoveTrack(Track track, int position) {
        if (playlistId == null || track == null || track.getTrackId() == null) return;
        repo.removeTrackFromPlaylist(playlistId, track.getTrackId(), new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) {
                List<Track> cur = tracks.getValue();
                if (cur == null || position < 0 || position >= cur.size()) return;
                List<Track> next = new ArrayList<>(cur);
                next.remove(position);
                tracks.postValue(next);
                trackRemoved.postValue(new Event<>(position));
            }
            @Override public void onError(String message) { /* silent */ }
        });
    }
}
