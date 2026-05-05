package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.ExploreCardAdapter;
import com.example.musicstreamingapp.adapter.GenreTileAdapter;
import com.example.musicstreamingapp.adapter.HomeTrackRowAdapter;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.SearchResult;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private ApiService api;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private LinearLayout defaultContainer, resultsContainer;
    private TextView tvEmpty, tvResultsHeader, tvAvatar;
    private RecyclerView rvResults;
    private final List<Track> trackResults = new ArrayList<>();
    private HomeTrackRowAdapter resultsAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));

        tvAvatar = view.findViewById(R.id.tv_search_avatar);
        String username = TokenManager.getUsername(requireContext());
        tvAvatar.setText(username == null || username.isEmpty()
            ? "U" : username.substring(0, 1).toUpperCase());

        EditText etSearch = view.findViewById(R.id.et_search);
        defaultContainer = view.findViewById(R.id.default_container);
        resultsContainer = view.findViewById(R.id.results_container);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tvResultsHeader = view.findViewById(R.id.tv_results_header);
        rvResults = view.findViewById(R.id.rv_results);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsAdapter = new HomeTrackRowAdapter(trackResults, this::openPlayer);
        rvResults.setAdapter(resultsAdapter);

        // Explore cards (hardcoded hashtags - tier 2)
        RecyclerView rvExplore = view.findViewById(R.id.rv_explore);
        rvExplore.setLayoutManager(new LinearLayoutManager(getContext(),
            RecyclerView.HORIZONTAL, false));
        rvExplore.setAdapter(new ExploreCardAdapter(Arrays.asList(
            new ExploreCardAdapter.Item("#pop đương đại", R.color.placeholder_start),
            new ExploreCardAdapter.Item("#downtown vibes", R.color.placeholder_end),
            new ExploreCardAdapter.Item("#athleisure", R.color.spotify_green),
            new ExploreCardAdapter.Item("#chill night", R.color.bg_secondary)
        )));

        // Browse all - 2 column grid of genres
        RecyclerView rvBrowseAll = view.findViewById(R.id.rv_browse_all);
        rvBrowseAll.setLayoutManager(new GridLayoutManager(getContext(), 2));
        loadGenres(rvBrowseAll);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> doSearch(s.toString().trim());
                handler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadGenres(RecyclerView rv) {
        api.getGenres().enqueue(new Callback<List<Genre>>() {
            @Override public void onResponse(@NonNull Call<List<Genre>> call,
                                             @NonNull Response<List<Genre>> response) {
                List<Genre> base = response.isSuccessful() && response.body() != null
                    ? new ArrayList<>(response.body()) : new ArrayList<>();
                Genre g1 = new Genre(); inject(g1, "Sự kiện trực tiếp");
                Genre g2 = new Genre(); inject(g2, "Dành Cho Bạn");
                Genre g3 = new Genre(); inject(g3, "Bản phát hành sắp ra mắt");
                Genre g4 = new Genre(); inject(g4, "Mới phát hành");
                base.add(0, g4); base.add(0, g3); base.add(0, g2); base.add(0, g1);
                rv.setAdapter(new GenreTileAdapter(base, g -> {}));
            }
            @Override public void onFailure(@NonNull Call<List<Genre>> call, @NonNull Throwable t) {
                List<Genre> fallback = new ArrayList<>();
                Genre g1 = new Genre(); inject(g1, "Nhạc"); fallback.add(g1);
                Genre g2 = new Genre(); inject(g2, "Podcast"); fallback.add(g2);
                Genre g3 = new Genre(); inject(g3, "Sự kiện trực tiếp"); fallback.add(g3);
                Genre g4 = new Genre(); inject(g4, "Dành Cho Bạn"); fallback.add(g4);
                Genre g5 = new Genre(); inject(g5, "Bản phát hành sắp ra mắt"); fallback.add(g5);
                Genre g6 = new Genre(); inject(g6, "Mới phát hành"); fallback.add(g6);
                Genre g7 = new Genre(); inject(g7, "Nhạc Việt"); fallback.add(g7);
                Genre g8 = new Genre(); inject(g8, "Pop"); fallback.add(g8);
                Genre g9 = new Genre(); inject(g9, "K-Pop"); fallback.add(g9);
                Genre g10 = new Genre(); inject(g10, "Hip-Hop"); fallback.add(g10);
                rv.setAdapter(new GenreTileAdapter(fallback, g -> {}));
            }
        });
    }

    // Reflection-free name injection (since Genre fields are private)
    private void inject(Genre g, String name) {
        try {
            java.lang.reflect.Field f = Genre.class.getDeclaredField("name");
            f.setAccessible(true);
            f.set(g, name);
        } catch (Exception ignore) {}
    }

    private void doSearch(String q) {
        if (q.isEmpty()) {
            defaultContainer.setVisibility(View.VISIBLE);
            resultsContainer.setVisibility(View.GONE);
            return;
        }
        api.search(q).enqueue(new Callback<SearchResult>() {
            @Override public void onResponse(@NonNull Call<SearchResult> call,
                                             @NonNull Response<SearchResult> response) {
                defaultContainer.setVisibility(View.GONE);
                resultsContainer.setVisibility(View.VISIBLE);
                trackResults.clear();
                if (response.isSuccessful() && response.body() != null) {
                    SearchResult r = response.body();
                    if (r.getTracks() != null) trackResults.addAll(r.getTracks());
                }
                resultsAdapter.notifyDataSetChanged();
                tvResultsHeader.setText(getString(R.string.track_results) + " (" + trackResults.size() + ")");
                tvEmpty.setVisibility(trackResults.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onFailure(@NonNull Call<SearchResult> call, @NonNull Throwable t) {}
        });
    }

    private void openPlayer(Track track) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", track);
        startActivity(intent);
    }
}
