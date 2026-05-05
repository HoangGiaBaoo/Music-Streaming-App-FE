package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.AlbumCardAdapter;
import com.example.musicstreamingapp.adapter.TrackListAdapter;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistDetailActivity extends AppCompatActivity {
    private ApiService api;
    private Long artistId;
    private boolean isFollowing = false;
    private Button btnFollow;
    private List<Album> albums = new ArrayList<>();
    private List<Track> tracks = new ArrayList<>();
    private AlbumCardAdapter albumAdapter;
    private TrackListAdapter trackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        artistId = getIntent().getLongExtra("artistId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnFollow = findViewById(R.id.btn_follow);
        RecyclerView rvAlbums = findViewById(R.id.rv_albums);
        RecyclerView rvTracks = findViewById(R.id.rv_top_tracks);

        rvAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTracks.setLayoutManager(new LinearLayoutManager(this));

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

        rvAlbums.setAdapter(albumAdapter);
        rvTracks.setAdapter(trackAdapter);

        btnFollow.setOnClickListener(v -> toggleFollow());

        loadArtist();
        loadAlbums();
        loadTracks();
    }

    private void loadArtist() {
        api.getArtist(artistId).enqueue(new Callback<Artist>() {
            @Override public void onResponse(Call<Artist> call, Response<Artist> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Artist a = response.body();
                    CollapsingToolbarLayout ctl = findViewById(R.id.collapsing_toolbar);
                    ctl.setTitle(a.getName());
                    ImageView ivHeader = findViewById(R.id.iv_artist_banner);
                    if (a.getAvatarUrl() != null) {
                        Glide.with(ArtistDetailActivity.this)
                            .load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                            .centerCrop().into(ivHeader);
                    }
                }
            }
            @Override public void onFailure(Call<Artist> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content),
                    R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAlbums() {
        api.getArtistAlbums(artistId).enqueue(new Callback<List<Album>>() {
            @Override public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    albums.clear();
                    albums.addAll(response.body());
                    albumAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) {}
        });
    }

    private void loadTracks() {
        api.getTracks().enqueue(new Callback<List<Track>>() {
            @Override public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tracks.clear();
                    for (Track t : response.body()) {
                        if (t.getArtist() != null && t.getArtist().getArtistId().equals(artistId))
                            tracks.add(t);
                    }
                    trackAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(Call<List<Track>> call, Throwable t) {}
        });
    }

    private void toggleFollow() {
        isFollowing = !isFollowing;
        btnFollow.setText(isFollowing ? R.string.following_btn : R.string.follow);
        api.toggleFollow(artistId).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> call,
                                             Response<Map<String, String>> response) {}
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                isFollowing = !isFollowing;
                runOnUiThread(() -> btnFollow.setText(isFollowing ? R.string.following_btn : R.string.follow));
            }
        });
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
