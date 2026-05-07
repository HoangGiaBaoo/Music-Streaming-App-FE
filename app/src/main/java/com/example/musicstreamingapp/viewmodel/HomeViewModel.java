package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.HomeRepository;
import com.example.musicstreamingapp.model.HomeSection;

import java.util.Collections;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final HomeRepository repo;

    private final MutableLiveData<List<HomeSection>> sections = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> filter = new MutableLiveData<>(null);
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private boolean loaded = false;

    public HomeViewModel(HomeRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<HomeSection>> sections() { return sections; }
    public LiveData<String> filter() { return filter; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        load(filter.getValue());
    }

    public void onFilterSelected(String newFilter) {
        filter.setValue(newFilter);
        load(newFilter);
    }

    private void load(String f) {
        repo.getFeed(f, new RepoCallback<List<HomeSection>>() {
            @Override public void onSuccess(List<HomeSection> data) { sections.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
