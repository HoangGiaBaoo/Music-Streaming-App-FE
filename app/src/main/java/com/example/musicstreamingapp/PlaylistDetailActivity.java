package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.ActivityPlaylistDetailBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.viewmodel.PlaylistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private ActivityPlaylistDetailBinding b;
    private PlaylistDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;
    private boolean shimmerHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPlaylistDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long playlistId = getIntent().getLongExtra("playlistId", -1);
        String name = getIntent().getStringExtra("playlistName");

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (name != null) b.collapsingToolbar.setTitle(name);

        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        adapter.setMoreListener((track, pos) -> vm.onRemoveTrack(track, pos));
        b.rvTracks.setAdapter(adapter);

        b.btnPlayAll.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        b.shimmerPlaylist.getRoot().startShimmer();
        b.shimmerPlaylist.getRoot().postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(PlaylistDetailViewModel.class);
        vm.setPlaylistId(playlistId);
        observeViewModel();
        vm.loadIfNeeded();
    }

    private void observeViewModel() {
        vm.tracks().observe(this, data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            if (data != null) hideShimmer();
        });
        vm.errorEvent().observe(this, e -> e.consume(msg -> {
            hideShimmer();
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        }));
    }

    private void hideShimmer() {
        if (shimmerHidden) return;
        shimmerHidden = true;
        b.shimmerPlaylist.getRoot().stopShimmer();
        b.shimmerPlaylist.getRoot().setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
