package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Track;

import java.util.Collections;
import java.util.List;

public class LikedTracksViewModel extends ViewModel {

    private final LibraryRepository repo;

    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>(Collections.emptyList());

    public LikedTracksViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Track>> tracks() { return tracks; }

    public void refresh() {
        repo.getLikedTracks(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) { tracks.postValue(data); }
            @Override public void onError(String message) { /* keep stale */ }
        });
    }

    /** Like/bỏ like 1 bài rồi reload danh sách. Dùng repo.toggleLike sẵn có — không đổi logic like. */
    public void toggleLike(long trackId) {
        repo.toggleLike(trackId, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean nowLiked) { refresh(); }
            @Override public void onError(String message) { /* giữ nguyên danh sách nếu lỗi */ }
        });
    }
}
