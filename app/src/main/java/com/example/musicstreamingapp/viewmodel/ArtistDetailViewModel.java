package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtistDetailViewModel extends ViewModel {

    private static final int COLLAPSED_TRACK_COUNT = 5;

    private final LibraryRepository repo;

    private final MutableLiveData<Artist> artist = new MutableLiveData<>();
    private final MutableLiveData<List<Album>> albums = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Track>> visibleTracks = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> following = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showExpandButton = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> tracksExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Set<Long>> inPlaylistTrackIds = new MutableLiveData<>(new HashSet<>());

    private Long artistId;
    private boolean loaded = false;
    private final List<Track> allTracks = new ArrayList<>();

    public ArtistDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<Artist> artist() { return artist; }
    public LiveData<List<Album>> albums() { return albums; }
    public LiveData<List<Track>> visibleTracks() { return visibleTracks; }
    public LiveData<Boolean> following() { return following; }
    public LiveData<Boolean> showExpandButton() { return showExpandButton; }
    public LiveData<Boolean> tracksExpanded() { return tracksExpanded; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Set<Long>> inPlaylistTrackIds() { return inPlaylistTrackIds; }

    public void updateTrackInPlaylist(long trackId, boolean inPlaylist) {
        Set<Long> cur = inPlaylistTrackIds.getValue();
        if (cur == null) cur = new HashSet<>();
        Set<Long> next = new HashSet<>(cur);
        if (inPlaylist) next.add(trackId);
        else next.remove(trackId);
        inPlaylistTrackIds.setValue(next);
    }

    public void setArtistId(long id) {
        this.artistId = id;
    }

    public void loadIfNeeded() {
        if (loaded || artistId == null) return;
        loaded = true;

        repo.getArtist(artistId, new RepoCallback<Artist>() {
            @Override public void onSuccess(Artist data) { artist.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });

        repo.checkFollowState(artistId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { following.postValue(data); }
            @Override public void onError(String message) { }
        });

        repo.getArtistAlbums(artistId, new RepoCallback<List<Album>>() {
            @Override public void onSuccess(List<Album> data) { albums.postValue(data); }
            @Override public void onError(String message) { }
        });

        repo.getArtistPopularTracks(artistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                allTracks.clear();
                if (data != null) allTracks.addAll(data);
                updateVisibleTracks();
            }
            @Override public void onError(String message) { }
        });
    }

    public void toggleTracksExpanded() {
        boolean expanded = !Boolean.TRUE.equals(tracksExpanded.getValue());
        tracksExpanded.setValue(expanded);
        updateVisibleTracks();
    }

    private void updateVisibleTracks() {
        boolean expanded = Boolean.TRUE.equals(tracksExpanded.getValue());
        if (expanded || allTracks.size() <= COLLAPSED_TRACK_COUNT) {
            visibleTracks.postValue(new ArrayList<>(allTracks));
        } else {
            visibleTracks.postValue(new ArrayList<>(allTracks.subList(0, COLLAPSED_TRACK_COUNT)));
        }
        showExpandButton.postValue(allTracks.size() > COLLAPSED_TRACK_COUNT);
    }

    public void onFollowClicked() {
        if (artistId == null) return;
        boolean prev = Boolean.TRUE.equals(following.getValue());
        following.setValue(!prev);
        repo.toggleFollow(artistId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { }
            @Override public void onError(String message) {
                following.postValue(prev);
            }
        });
    }

    public void likeTrack(long trackId, RepoCallback<Boolean> cb) {
        repo.toggleLike(trackId, cb);
    }

    public List<Track> getAllTracks() {
        return Collections.unmodifiableList(allTracks);
    }
}
