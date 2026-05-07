package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.ActivityAlbumDetailBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.viewmodel.AlbumDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {

    private ActivityAlbumDetailBinding b;
    private AlbumDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAlbumDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long albumId = getIntent().getLongExtra("albumId", -1);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        b.rvTracks.setAdapter(adapter);

        b.btnPlayAll.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        vm = new ViewModelProvider(this, new VmFactory(this)).get(AlbumDetailViewModel.class);
        vm.setAlbumId(albumId);
        observeViewModel();
        vm.loadIfNeeded();
    }

    private void observeViewModel() {
        vm.album().observe(this, this::renderAlbum);
        vm.tracks().observe(this, this::renderTracks);
        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    private void renderAlbum(Album a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getTitle());
        if (a.getCoverUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .centerCrop().into(b.ivCover);
        }
        if (a.getArtist() != null) b.tvArtistName.setText(a.getArtist().getName());
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
