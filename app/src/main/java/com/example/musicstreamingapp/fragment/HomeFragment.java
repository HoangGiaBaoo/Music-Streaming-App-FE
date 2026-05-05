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

import com.example.musicstreamingapp.AlbumDetailActivity;
import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.HomeFeedAdapter;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.HomeSection;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements HomeFeedAdapter.OnHomeItemClick {
    private ApiService api;
    private final List<HomeSection> sections = new ArrayList<>();
    private HomeFeedAdapter adapter;
    private TextView chipAll, chipMusic, chipFollowing, chipPodcast;
    private String currentFilter = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));

        TextView avatar = view.findViewById(R.id.tv_avatar_letter);
        String username = TokenManager.getUsername(requireContext());
        avatar.setText(username == null || username.isEmpty()
            ? "U" : username.substring(0, 1).toUpperCase());

        view.findViewById(R.id.avatar_container).setOnClickListener(v -> {
            if (requireActivity() instanceof com.example.musicstreamingapp.MainActivity) {
                ((com.example.musicstreamingapp.MainActivity) requireActivity()).openDrawer();
            }
        });

        chipAll = view.findViewById(R.id.chip_all);
        chipMusic = view.findViewById(R.id.chip_music);
        chipFollowing = view.findViewById(R.id.chip_following);
        chipPodcast = view.findViewById(R.id.chip_podcast);

        chipAll.setSelected(true);
        chipAll.setOnClickListener(v -> setFilter(null, chipAll));
        chipMusic.setOnClickListener(v -> setFilter("music", chipMusic));
        chipFollowing.setOnClickListener(v -> setFilter("following", chipFollowing));
        chipPodcast.setOnClickListener(v -> setFilter(null, chipPodcast));

        RecyclerView rv = view.findViewById(R.id.rv_home_feed);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HomeFeedAdapter(sections, this);
        rv.setAdapter(adapter);

        loadFeed(null);
    }

    private void setFilter(String filter, TextView active) {
        currentFilter = filter;
        TextView[] all = { chipAll, chipMusic, chipFollowing, chipPodcast };
        for (TextView c : all) c.setSelected(c == active);
        loadFeed(filter);
    }

    private void loadFeed(String filter) {
        api.homeFeed(filter).enqueue(new Callback<List<HomeSection>>() {
            @Override public void onResponse(@NonNull Call<List<HomeSection>> call,
                                             @NonNull Response<List<HomeSection>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sections.clear();
                    sections.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<HomeSection>> call, @NonNull Throwable t) {
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override public void onTrack(Track track) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", track);
        startActivity(intent);
    }

    @Override public void onArtist(Artist artist) {
        Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
        intent.putExtra("artistId", artist.getArtistId());
        startActivity(intent);
    }

    @Override public void onAlbum(Album album) {
        Intent intent = new Intent(getContext(), AlbumDetailActivity.class);
        intent.putExtra("albumId", album.getAlbumId());
        startActivity(intent);
    }

    @Override public void onPlaylist(Playlist playlist) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlistId", playlist.getPlaylistId());
        startActivity(intent);
    }
}
