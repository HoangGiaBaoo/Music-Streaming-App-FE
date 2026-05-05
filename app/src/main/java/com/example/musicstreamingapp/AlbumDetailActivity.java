package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumDetailActivity extends AppCompatActivity {
    private ApiService api;
    private Long albumId;
    private List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        albumId = getIntent().getLongExtra("albumId", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.rv_tracks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        rv.setAdapter(adapter);

        Button btnPlay = findViewById(R.id.btn_play_all);
        btnPlay.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        loadAlbum();
        loadTracks();
    }

    private void loadAlbum() {
        api.getAlbums().enqueue(new Callback<List<Album>>() {
            @Override public void onResponse(Call<List<Album>> call, Response<List<Album>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Album a : response.body()) {
                        if (a.getAlbumId().equals(albumId)) {
                            CollapsingToolbarLayout ctl = findViewById(R.id.collapsing_toolbar);
                            ctl.setTitle(a.getTitle());
                            ImageView ivCover = findViewById(R.id.iv_cover);
                            if (a.getCoverUrl() != null) {
                                Glide.with(AlbumDetailActivity.this)
                                    .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                                    .centerCrop().into(ivCover);
                            }
                            TextView tvArtist = findViewById(R.id.tv_artist_name);
                            if (a.getArtist() != null) tvArtist.setText(a.getArtist().getName());
                            break;
                        }
                    }
                }
            }
            @Override public void onFailure(Call<List<Album>> call, Throwable t) {}
        });
    }

    private void loadTracks() {
        api.getAlbumTracks(albumId).enqueue(new Callback<List<Track>>() {
            @Override public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tracks.clear();
                    tracks.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(Call<List<Track>> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content),
                    R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
