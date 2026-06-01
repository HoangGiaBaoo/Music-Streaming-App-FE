package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.GenreDetailActivity;
import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.ExploreCardAdapter;
import com.example.musicstreamingapp.adapter.GenreTileAdapter;
import com.example.musicstreamingapp.adapter.HomeTrackRowAdapter;
import com.example.musicstreamingapp.databinding.FragmentSearchBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.viewmodel.MainViewModel;
import com.example.musicstreamingapp.viewmodel.SearchViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding b;
    private SearchViewModel vm;
    private MainViewModel mainVm;

    private final List<Track> trackResults = new ArrayList<>();
    private HomeTrackRowAdapter resultsAdapter;
    private boolean shimmerHidden = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentSearchBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainVm = new ViewModelProvider(requireActivity(), new VmFactory(requireContext()))
            .get(MainViewModel.class);
        mainVm.usernameLetter().observe(getViewLifecycleOwner(),
            letter -> b.tvSearchAvatar.setText(letter));

        b.searchAvatarContainer.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        });

        b.rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsAdapter = new HomeTrackRowAdapter(trackResults, this::openPlayer);
        b.rvResults.setAdapter(resultsAdapter);

        b.rvExplore.setLayoutManager(new LinearLayoutManager(getContext(),
            RecyclerView.HORIZONTAL, false));
        b.rvExplore.setAdapter(new ExploreCardAdapter(Arrays.asList(
            new ExploreCardAdapter.Item("#pop đương đại", R.color.placeholder_start),
            new ExploreCardAdapter.Item("#downtown vibes", R.color.placeholder_end),
            new ExploreCardAdapter.Item("#athleisure", R.color.spotify_green),
            new ExploreCardAdapter.Item("#chill night", R.color.bg_secondary)
        )));

        b.rvBrowseAll.setLayoutManager(new GridLayoutManager(getContext(), 2));

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                vm.onQueryChanged(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.shimmerSearchGrid.getRoot().startShimmer();
        b.shimmerSearchGrid.getRoot().postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(SearchViewModel.class);
        vm.trackResults().observe(getViewLifecycleOwner(), this::renderResults);
        vm.genres().observe(getViewLifecycleOwner(), genres -> {
            b.rvBrowseAll.setAdapter(new GenreTileAdapter(genres, g -> {
                Intent intent = new Intent(getContext(), GenreDetailActivity.class);
                intent.putExtra(GenreDetailActivity.EXTRA_GENRE_ID,
                    g.getGenreId() != null ? g.getGenreId() : -1L);
                intent.putExtra(GenreDetailActivity.EXTRA_GENRE_NAME, g.getName());
                startActivity(intent);
            }));
            if (genres != null && !genres.isEmpty()) hideShimmer();
        });
        vm.showingResults().observe(getViewLifecycleOwner(), this::renderModeSwitch);
        vm.loadGenresIfNeeded();
    }

    private void hideShimmer() {
        if (shimmerHidden || b == null) return;
        shimmerHidden = true;
        b.shimmerSearchGrid.getRoot().stopShimmer();
        b.shimmerSearchGrid.getRoot().setVisibility(View.GONE);
    }

    private void renderResults(List<Track> tracks) {
        trackResults.clear();
        if (tracks != null) trackResults.addAll(tracks);
        resultsAdapter.notifyDataSetChanged();
        b.tvResultsHeader.setText(getString(R.string.track_results) + " (" + trackResults.size() + ")");
        b.tvEmpty.setVisibility(trackResults.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderModeSwitch(Boolean showResults) {
        boolean show = Boolean.TRUE.equals(showResults);
        b.defaultContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        b.resultsContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void openPlayer(Track track) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", track);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
