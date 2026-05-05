package com.example.musicstreamingapp.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.PlaylistAdapter;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.PlaylistRequest;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistsFragment extends Fragment {
    private ApiService api;
    private List<Playlist> playlists = new ArrayList<>();
    private PlaylistAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.rv_playlists);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter(playlists, playlist -> {
            Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
            intent.putExtra("playlistId", playlist.getPlaylistId());
            intent.putExtra("playlistName", playlist.getName());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showCreateDialog());

        loadPlaylists();
    }

    @Override public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists() {
        api.getMyPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override public void onResponse(@NonNull Call<List<Playlist>> call,
                                             @NonNull Response<List<Playlist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlists.clear();
                    playlists.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Playlist>> call, @NonNull Throwable t) {
                if (getView() != null)
                    Snackbar.make(getView(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialog() {
        EditText et = new EditText(requireContext());
        et.setHint(getString(R.string.playlist_name));
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_playlist)
            .setView(et)
            .setPositiveButton(R.string.create, (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) createPlaylist(name);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void createPlaylist(String name) {
        api.createPlaylist(new PlaylistRequest(name, false)).enqueue(new Callback<Playlist>() {
            @Override public void onResponse(@NonNull Call<Playlist> call,
                                             @NonNull Response<Playlist> response) {
                if (response.isSuccessful()) loadPlaylists();
            }
            @Override public void onFailure(@NonNull Call<Playlist> call, @NonNull Throwable t) {}
        });
    }
}
