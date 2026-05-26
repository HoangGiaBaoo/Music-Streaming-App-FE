package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistDetailViewModel extends ViewModel {

    public enum EditResult { UPDATED, DELETED, UPDATE_FAILED, DELETE_FAILED }

    private final LibraryRepository repo;

    private final MutableLiveData<Playlist> playlist = new MutableLiveData<>();
    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Track>> suggestions = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> trackRemoved = new MutableLiveData<>();
    private final MutableLiveData<Event<EditResult>> editResult = new MutableLiveData<>();

    private Long playlistId;
    private boolean loaded = false;
    private boolean suggestionsLoaded = false;

    public PlaylistDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<Playlist> playlist() { return playlist; }
    public LiveData<List<Track>> tracks() { return tracks; }
    public LiveData<List<Track>> suggestions() { return suggestions; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Event<Integer>> trackRemoved() { return trackRemoved; }
    public LiveData<Event<EditResult>> editResult() { return editResult; }

    public void setPlaylistId(long id) {
        this.playlistId = id;
    }

    public void loadIfNeeded() {
        if (loaded || playlistId == null) return;
        loaded = true;
        fetchPlaylistAndTracks();
    }

    /** Force-refresh playlist + tracks from server (e.g. after returning from cover picker). */
    public void reload() {
        if (playlistId == null) return;
        fetchPlaylistAndTracks();
    }

    private void fetchPlaylistAndTracks() {
        repo.getPlaylist(playlistId, new RepoCallback<Playlist>() {
            @Override public void onSuccess(Playlist data) { playlist.postValue(data); }
            @Override public void onError(String message) { /* header fallback */ }
        });
        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) { tracks.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    public void updateDetails(String name, Boolean isPublic) {
        if (playlistId == null) return;
        repo.updatePlaylist(playlistId, name, isPublic, new RepoCallback<Playlist>() {
            @Override public void onSuccess(Playlist data) {
                playlist.postValue(data);
                editResult.postValue(new Event<>(EditResult.UPDATED));
            }
            @Override public void onError(String message) {
                editResult.postValue(new Event<>(EditResult.UPDATE_FAILED));
            }
        });
    }

    public void deletePlaylist() {
        if (playlistId == null) return;
        repo.deletePlaylist(playlistId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) {
                editResult.postValue(new Event<>(EditResult.DELETED));
            }
            @Override public void onError(String message) {
                editResult.postValue(new Event<>(EditResult.DELETE_FAILED));
            }
        });
    }

    /** Fetch a list of all tracks to use as "Suggested tracks" in the empty state. */
    public void loadSuggestionsIfNeeded() {
        if (suggestionsLoaded) return;
        suggestionsLoaded = true;
        repo.getAllTracks(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                List<Track> capped = new ArrayList<>(data.subList(0, Math.min(10, data.size())));
                suggestions.postValue(capped);
            }
            @Override public void onError(String message) { /* silent */ }
        });
    }

    /** Add a track from the suggestions list into this playlist, then refresh both lists. */
    public void addSuggestion(Track track) {
        if (playlistId == null || track == null || track.getTrackId() == null) return;
        final Long trackId = track.getTrackId();
        repo.addTrackToPlaylist(playlistId, trackId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) {
                repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
                    @Override public void onSuccess(List<Track> data) { tracks.postValue(data); }
                    @Override public void onError(String message) { /* silent */ }
                });
                List<Track> cur = suggestions.getValue();
                if (cur == null) return;
                List<Track> next = new ArrayList<>(cur.size());
                for (Track t : cur) {
                    if (t.getTrackId() != null && !t.getTrackId().equals(trackId)) next.add(t);
                }
                suggestions.postValue(next);
            }
            @Override public void onError(String message) { /* silent */ }
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
