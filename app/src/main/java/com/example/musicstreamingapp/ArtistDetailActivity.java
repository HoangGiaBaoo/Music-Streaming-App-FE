package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.AlbumCardAdapter;
import com.example.musicstreamingapp.adapter.TrackListAdapter;
import com.example.musicstreamingapp.databinding.ActivityArtistDetailBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.viewmodel.ArtistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ArtistDetailActivity extends AppCompatActivity {

    private ActivityArtistDetailBinding b;
    private ArtistDetailViewModel vm;

    private final List<Album> albums = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();
    private AlbumCardAdapter albumAdapter;
    private TrackListAdapter trackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityArtistDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long artistId = getIntent().getLongExtra("artistId", -1);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        b.rvAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.rvTopTracks.setLayoutManager(new LinearLayoutManager(this));

        albumAdapter = new AlbumCardAdapter(albums, album -> {
            Intent intent = new Intent(this, AlbumDetailActivity.class);
            intent.putExtra("albumId", album.getAlbumId());
            startActivity(intent);
        });
        trackAdapter = new TrackListAdapter(tracks, (track, pos) -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("track", track);
            startActivity(intent);
        });

        b.rvAlbums.setAdapter(albumAdapter);
        b.rvTopTracks.setAdapter(trackAdapter);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(ArtistDetailViewModel.class);
        vm.setArtistId(artistId);

        b.btnFollow.setOnClickListener(v -> vm.onFollowClicked());

        observeViewModel();
        vm.loadIfNeeded();
    }

    private void observeViewModel() {
        vm.artist().observe(this, this::renderArtist);
        vm.albums().observe(this, this::renderAlbums);
        vm.tracks().observe(this, this::renderTracks);
        vm.following().observe(this, isFollowing -> b.btnFollow.setText(
            Boolean.TRUE.equals(isFollowing) ? R.string.following_btn : R.string.follow));
        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    private void renderArtist(Artist a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getName());
        if (a.getAvatarUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                .centerCrop().into(b.ivArtistBanner);
        }
    }

    private void renderAlbums(List<Album> data) {
        albums.clear();
        if (data != null) albums.addAll(data);
        albumAdapter.notifyDataSetChanged();
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        trackAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
