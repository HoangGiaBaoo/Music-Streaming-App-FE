package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.RecentItem;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecentViewModel extends ViewModel {

    private static final int DEFAULT_LIMIT = 50;

    private final LibraryRepository repo;

    private final MutableLiveData<List<DayGroup>> groups = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private boolean loaded = false;

    public RecentViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<DayGroup>> groups() { return groups; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        repo.getRecentTracks(DEFAULT_LIMIT, new RepoCallback<List<RecentItem>>() {
            @Override public void onSuccess(List<RecentItem> data) {
                groups.postValue(group(data));
            }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    private static List<DayGroup> group(List<RecentItem> items) {
        Map<String, List<Track>> grouped = new LinkedHashMap<>();
        for (RecentItem item : items) {
            if (item == null || item.track == null) continue;
            String key = item.dayKey();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item.track);
        }
        List<DayGroup> out = new ArrayList<>();
        for (Map.Entry<String, List<Track>> e : grouped.entrySet()) {
            out.add(new DayGroup(e.getKey(), e.getValue()));
        }
        return out;
    }

    public static class DayGroup {
        public final String dayKey;
        public final List<Track> tracks;
        public DayGroup(String dayKey, List<Track> tracks) {
            this.dayKey = dayKey;
            this.tracks = tracks;
        }
    }
}
