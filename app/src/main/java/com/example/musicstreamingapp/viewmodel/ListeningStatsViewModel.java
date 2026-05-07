package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.model.ListeningStats;

import java.util.ArrayList;
import java.util.List;

public class ListeningStatsViewModel extends ViewModel {

    public static final int MAX_SECTIONS = 3;
    private static final String DEFAULT_PERIOD = "week";

    private final UserRepository repo;
    private final MutableLiveData<List<Section>> sections = new MutableLiveData<>(new ArrayList<>());

    private boolean loaded = false;

    public ListeningStatsViewModel(UserRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Section>> sections() { return sections; }

    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        loadAt(0);
    }

    private void loadAt(int offset) {
        repo.getListeningStats(DEFAULT_PERIOD, offset, new RepoCallback<ListeningStats>() {
            @Override public void onSuccess(ListeningStats data) {
                appendSection(new Section(offset, data));
                if (offset + 1 < MAX_SECTIONS) loadAt(offset + 1);
            }
            @Override public void onError(String message) {
                if (offset + 1 < MAX_SECTIONS) loadAt(offset + 1);
            }
        });
    }

    private void appendSection(Section s) {
        List<Section> cur = sections.getValue();
        List<Section> next = (cur == null) ? new ArrayList<>() : new ArrayList<>(cur);
        next.add(s);
        sections.postValue(next);
    }

    public static class Section {
        public final int offset;
        public final ListeningStats stats;
        public Section(int offset, ListeningStats stats) {
            this.offset = offset;
            this.stats = stats;
        }
    }
}
