package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailActivity extends AppCompatActivity {
    private ApiService api;
    private Long playlistId;
    private List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        playlistId = getIntent().getLongExtra("playlistId", -1);
        String name = getIntent().getStringExtra("playlistName");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout ctl = findViewById(R.id.collapsing_toolbar);
        if (name != null) ctl.setTitle(name);

        RecyclerView rv = findViewById(R.id.rv_tracks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        adapter.setMoreListener((track, pos) -> removeTrack(track, pos));
        rv.setAdapter(adapter);

        Button btnPlay = findViewById(R.id.btn_play_all);
        btnPlay.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        loadTracks();
    }

    private void loadTracks() {
        api.getPlaylistTracks(playlistId).enqueue(new Callback<List<Track>>() {
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

    private void removeTrack(Track track, int pos) {
        api.removeTrackFromPlaylist(playlistId, track.getTrackId())
            .enqueue(new Callback<Map<String, String>>() {
                @Override public void onResponse(Call<Map<String, String>> call,
                                                 Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        tracks.remove(pos);
                        adapter.notifyItemRemoved(pos);
                    }
                }
                @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {}
            });
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
