package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.Resource;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.GenreFeedDto;

public class GenreDetailViewModel extends ViewModel {

    private final LibraryRepository repo;
    private final MutableLiveData<Resource<GenreFeedDto>> feed = new MutableLiveData<>();

    private boolean loaded = false;

    public GenreDetailViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<Resource<GenreFeedDto>> feed() { return feed; }

    /** Idempotent — chỉ gọi network lần đầu, sống qua config change. */
    public void load(long genreId) {
        if (loaded) return;
        loaded = true;
        feed.setValue(Resource.loading(null));
        repo.getGenreFeed(genreId, new RepoCallback<GenreFeedDto>() {
            @Override public void onSuccess(GenreFeedDto data) {
                feed.postValue(Resource.success(data));
            }
            @Override public void onError(String message) {
                feed.postValue(Resource.error(message, null));
            }
        });
    }
}
