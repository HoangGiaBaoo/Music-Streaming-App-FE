package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.TrackListAdapter;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LikedTracksFragment extends Fragment {
    private ApiService api;
    private List<Track> tracks = new ArrayList<>();
    private TrackListAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_liked_tracks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.rv_liked);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TrackListAdapter(tracks, (track, pos) -> {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("track", track);
            startActivity(intent);
        });
        rv.setAdapter(adapter);
        loadLiked();
    }

    @Override public void onResume() {
        super.onResume();
        loadLiked();
    }

    private void loadLiked() {
        api.getLikedTracks().enqueue(new Callback<List<Track>>() {
            @Override public void onResponse(@NonNull Call<List<Track>> call,
                                             @NonNull Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tracks.clear();
                    tracks.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Track>> call, @NonNull Throwable t) {}
        });
    }
}
