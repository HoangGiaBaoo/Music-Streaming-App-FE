package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Playlist;

import java.util.Collections;
import java.util.List;

public class PlaylistsViewModel extends ViewModel {

    public enum CreateResult { CREATED, FAILED, NETWORK_ERROR }

    private final LibraryRepository repo;

    private final MutableLiveData<List<Playlist>> playlists = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<CreateResult>> createResult = new MutableLiveData<>();

    public PlaylistsViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Playlist>> playlists()              { return playlists; }
    public LiveData<Event<String>> errorEvent()              { return errorEvent; }
    public LiveData<Event<CreateResult>> createResult()      { return createResult; }

    public void refresh() {
        repo.getMyPlaylists(new RepoCallback<List<Playlist>>() {
            @Override public void onSuccess(List<Playlist> data) { playlists.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    public void create(String name) {
        if (name == null || name.trim().isEmpty()) return;
        repo.createPlaylist(name.trim(), false, new RepoCallback<Playlist>() {
            @Override public void onSuccess(Playlist data) {
                createResult.postValue(new Event<>(CreateResult.CREATED));
                refresh();
            }
            @Override public void onError(String message) {
                createResult.postValue(new Event<>(
                    "network_error".equals(message) ? CreateResult.NETWORK_ERROR : CreateResult.FAILED));
            }
        });
    }
}
