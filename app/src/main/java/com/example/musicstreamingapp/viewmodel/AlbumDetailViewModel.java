package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Track;

import java.util.Collections;
import java.util.List;

public class AlbumDetailViewModel extends ViewModel {

    private final LibraryRepository repo;

    private final MutableLiveData<Album> album = new MutableLiveData<>();
    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private Long albumId;
    private boolean loaded = false;

    public AlbumDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<Album> album() { return album; }
    public LiveData<List<Track>> tracks() { return tracks; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

    public void setAlbumId(long id) {
        this.albumId = id;
    }

    public void loadIfNeeded() {
        if (loaded || albumId == null) return;
        loaded = true;

        repo.getAllAlbums(new RepoCallback<List<Album>>() {
            @Override public void onSuccess(List<Album> data) {
                for (Album a : data) {
                    if (a.getAlbumId() != null && a.getAlbumId().equals(albumId)) {
                        album.postValue(a);
                        return;
                    }
                }
            }
            @Override public void onError(String message) { /* ignore — title fallback */ }
        });

        repo.getAlbumTracks(albumId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) { tracks.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
