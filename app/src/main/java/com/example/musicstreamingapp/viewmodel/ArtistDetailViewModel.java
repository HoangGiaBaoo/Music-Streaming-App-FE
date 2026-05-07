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
import java.util.List;

public class ArtistDetailViewModel extends ViewModel {

    private final LibraryRepository repo;

    private final MutableLiveData<Artist> artist = new MutableLiveData<>();
    private final MutableLiveData<List<Album>> albums = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> following = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private Long artistId;
    private boolean loaded = false;

    public ArtistDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<Artist> artist() { return artist; }
    public LiveData<List<Album>> albums() { return albums; }
    public LiveData<List<Track>> tracks() { return tracks; }
    public LiveData<Boolean> following() { return following; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

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

        repo.getArtistAlbums(artistId, new RepoCallback<List<Album>>() {
            @Override public void onSuccess(List<Album> data) { albums.postValue(data); }
            @Override public void onError(String message) { /* ignore — section is optional */ }
        });

        repo.getAllTracks(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                List<Track> filtered = new ArrayList<>();
                for (Track t : data) {
                    if (t.getArtist() != null
                        && t.getArtist().getArtistId() != null
                        && t.getArtist().getArtistId().equals(artistId)) {
                        filtered.add(t);
                    }
                }
                tracks.postValue(filtered);
            }
            @Override public void onError(String message) { /* ignore */ }
        });
    }

    public void onFollowClicked() {
        if (artistId == null) return;
        boolean prev = Boolean.TRUE.equals(following.getValue());
        following.setValue(!prev);
        repo.toggleFollow(artistId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { /* server confirmed */ }
            @Override public void onError(String message) {
                following.postValue(prev);
            }
        });
    }
}
