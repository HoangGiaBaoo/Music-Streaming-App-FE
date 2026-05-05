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

import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.ArtistListAdapter;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowingArtistsFragment extends Fragment {
    private ApiService api;
    private List<Artist> artists = new ArrayList<>();
    private ArtistListAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_following_artists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));
        tvEmpty = view.findViewById(R.id.tv_empty);

        RecyclerView rv = view.findViewById(R.id.rv_following);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArtistListAdapter(artists, artist -> {
            Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
            intent.putExtra("artistId", artist.getArtistId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);
        loadFollowing();
    }

    @Override public void onResume() {
        super.onResume();
        loadFollowing();
    }

    private void loadFollowing() {
        api.getArtists().enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(@NonNull Call<List<Artist>> call,
                                             @NonNull Response<List<Artist>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    artists.clear();
                    artists.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(artists.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Artist>> call, @NonNull Throwable t) {}
        });
    }
}
