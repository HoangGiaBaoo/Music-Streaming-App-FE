package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Artist;

import java.util.Collections;
import java.util.List;

public class FollowingArtistsViewModel extends ViewModel {

    private final LibraryRepository repo;

    private final MutableLiveData<List<Artist>> artists = new MutableLiveData<>(Collections.emptyList());

    public FollowingArtistsViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Artist>> artists() { return artists; }

    public void refresh() {
        repo.getFollowedArtists(new RepoCallback<List<Artist>>() {
            @Override public void onSuccess(List<Artist> data) { artists.postValue(data); }
            @Override public void onError(String message) { /* keep stale */ }
        });
    }
}
