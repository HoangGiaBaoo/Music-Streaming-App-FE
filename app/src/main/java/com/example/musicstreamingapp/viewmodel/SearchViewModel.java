package com.example.musicstreamingapp.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.SearchRepository;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.SearchResult;
import com.example.musicstreamingapp.model.Track;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchViewModel extends ViewModel {

    private static final long DEBOUNCE_MS = 300L;

    private static final String[] PREPEND_LABELS = {
        "Sự kiện trực tiếp", "Dành Cho Bạn", "Bản phát hành sắp ra mắt", "Mới phát hành"
    };
    private static final String[] FALLBACK_LABELS = {
        "Nhạc", "Podcast", "Sự kiện trực tiếp", "Dành Cho Bạn",
        "Bản phát hành sắp ra mắt", "Mới phát hành", "Nhạc Việt", "Pop", "K-Pop", "Hip-Hop"
    };

    private final SearchRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private final MutableLiveData<List<Track>> trackResults = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Genre>> genres = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> showingResults = new MutableLiveData<>(false);

    private boolean genresLoaded = false;

    public SearchViewModel(SearchRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Track>> trackResults()   { return trackResults; }
    public LiveData<List<Genre>> genres()         { return genres; }
    public LiveData<Boolean> showingResults()     { return showingResults; }

    public void loadGenresIfNeeded() {
        if (genresLoaded) return;
        genresLoaded = true;
        repo.getGenres(new RepoCallback<List<Genre>>() {
            @Override public void onSuccess(List<Genre> data) {
                genres.postValue(prepend(new ArrayList<>(data), PREPEND_LABELS));
            }
            @Override public void onError(String message) {
                genres.postValue(buildFallback(FALLBACK_LABELS));
            }
        });
    }

    public void onQueryChanged(String query) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        String trimmed = query == null ? "" : query.trim();
        pendingSearch = () -> doSearch(trimmed);
        handler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void doSearch(String query) {
        if (query.isEmpty()) {
            showingResults.setValue(false);
            trackResults.setValue(Collections.emptyList());
            return;
        }
        repo.search(query, new RepoCallback<SearchResult>() {
            @Override public void onSuccess(SearchResult data) {
                List<Track> t = data.getTracks();
                trackResults.postValue(t != null ? t : new ArrayList<>());
                showingResults.postValue(true);
            }
            @Override public void onError(String message) {
                showingResults.postValue(true);
                trackResults.postValue(Collections.emptyList());
            }
        });
    }

    private static List<Genre> prepend(List<Genre> base, String[] labels) {
        List<Genre> seeded = new ArrayList<>();
        for (String label : labels) seeded.add(makeGenre(label));
        seeded.addAll(base);
        return seeded;
    }

    private static List<Genre> buildFallback(String[] labels) {
        List<Genre> out = new ArrayList<>();
        for (String label : labels) out.add(makeGenre(label));
        return out;
    }

    private static Genre makeGenre(String name) {
        Genre g = new Genre();
        try {
            Field f = Genre.class.getDeclaredField("name");
            f.setAccessible(true);
            f.set(g, name);
        } catch (Exception ignore) { }
        return g;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
    }
}
