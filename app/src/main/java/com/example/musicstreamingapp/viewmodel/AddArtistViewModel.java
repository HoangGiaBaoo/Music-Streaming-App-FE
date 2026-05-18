package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Artist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddArtistViewModel extends ViewModel {

    private final LibraryRepository repo;
    private List<Artist> allArtists = new ArrayList<>();
    private final Set<Long> originalFollowed = new HashSet<>();

    private final MutableLiveData<List<Artist>> filtered = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Set<Long>> selectedIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Boolean>> doneEvent = new MutableLiveData<>();

    public AddArtistViewModel(LibraryRepository repo) {
        this.repo = repo;
        load();
    }

    public LiveData<List<Artist>> filtered() { return filtered; }
    public LiveData<Set<Long>> selectedIds() { return selectedIds; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Event<Boolean>> doneEvent() { return doneEvent; }

    private void load() {
        loading.postValue(true);
        repo.getAllArtists(new RepoCallback<List<Artist>>() {
            @Override public void onSuccess(List<Artist> data) {
                allArtists = data != null ? data : Collections.emptyList();
                filtered.postValue(new ArrayList<>(allArtists));
                loadFollowed();
            }
            @Override public void onError(String msg) { loading.postValue(false); }
        });
    }

    private void loadFollowed() {
        repo.getFollowedArtists(new RepoCallback<List<Artist>>() {
            @Override public void onSuccess(List<Artist> data) {
                originalFollowed.clear();
                if (data != null) {
                    for (Artist a : data) {
                        if (a.getArtistId() != null) originalFollowed.add(a.getArtistId());
                    }
                }
                selectedIds.postValue(new HashSet<>(originalFollowed));
                loading.postValue(false);
            }
            @Override public void onError(String msg) { loading.postValue(false); }
        });
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            filtered.postValue(new ArrayList<>(allArtists));
            return;
        }
        String lower = query.toLowerCase();
        List<Artist> result = new ArrayList<>();
        for (Artist a : allArtists) {
            if (a.getName() != null && a.getName().toLowerCase().contains(lower)) {
                result.add(a);
            }
        }
        filtered.postValue(result);
    }

    public void toggleArtist(Artist artist) {
        if (artist.getArtistId() == null) return;
        Set<Long> current = new HashSet<>(selectedIds.getValue() != null
            ? selectedIds.getValue() : Collections.emptySet());
        Long id = artist.getArtistId();
        if (current.contains(id)) {
            current.remove(id);
        } else {
            current.add(id);
        }
        selectedIds.setValue(current);
    }

    public void save() {
        Set<Long> selected = selectedIds.getValue() != null
            ? selectedIds.getValue() : Collections.emptySet();
        List<Long> toFollow = new ArrayList<>();
        for (Long id : selected) {
            if (!originalFollowed.contains(id)) toFollow.add(id);
        }
        if (toFollow.isEmpty()) {
            doneEvent.postValue(new Event<>(true));
            return;
        }
        int[] remaining = {toFollow.size()};
        for (Long id : toFollow) {
            repo.toggleFollow(id, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean data) {
                    if (--remaining[0] == 0) doneEvent.postValue(new Event<>(true));
                }
                @Override public void onError(String msg) {
                    if (--remaining[0] == 0) doneEvent.postValue(new Event<>(true));
                }
            });
        }
    }
}
