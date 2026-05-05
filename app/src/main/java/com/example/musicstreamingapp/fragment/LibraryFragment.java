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
import com.example.musicstreamingapp.adapter.LibraryAdapter;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {
    private ApiService api;
    private RecyclerView rv;
    private final List<LibraryAdapter.Item> items = new ArrayList<>();
    private LibraryAdapter adapter;
    private TextView chipPlaylist, chipArtist, chipAlbum, chipLiked;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));

        TextView avatar = view.findViewById(R.id.tv_lib_avatar);
        String username = TokenManager.getUsername(requireContext());
        avatar.setText(username == null || username.isEmpty()
            ? "U" : username.substring(0, 1).toUpperCase());

        chipPlaylist = view.findViewById(R.id.lib_chip_playlist);
        chipArtist   = view.findViewById(R.id.lib_chip_artist);
        chipAlbum    = view.findViewById(R.id.lib_chip_album);
        chipLiked    = view.findViewById(R.id.lib_chip_liked);

        chipPlaylist.setOnClickListener(v -> { selectChip(chipPlaylist); loadPlaylists(); });
        chipArtist.setOnClickListener(v ->   { selectChip(chipArtist);   loadArtists(); });
        chipAlbum.setOnClickListener(v ->    { selectChip(chipAlbum);    loadAlbums(); });
        chipLiked.setOnClickListener(v ->    { selectChip(chipLiked);    loadLiked(); });

        rv = view.findViewById(R.id.rv_library);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LibraryAdapter(items);
        rv.setAdapter(adapter);

        // Default = Artist (matches PDF page 18)
        selectChip(chipArtist);
        loadArtists();
    }

    private void selectChip(TextView active) {
        TextView[] all = { chipPlaylist, chipArtist, chipAlbum, chipLiked };
        for (TextView c : all) c.setSelected(c == active);
    }

    private void loadPlaylists() {
        api.getMyPlaylists().enqueue(new Callback<List<Playlist>>() {
            @Override public void onResponse(@NonNull Call<List<Playlist>> call,
                                             @NonNull Response<List<Playlist>> response) {
                items.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Playlist p : response.body()) {
                        items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_PLAYLIST,
                            p.getCoverUrl(), p.getName(), "Danh sách phát",
                            () -> openPlaylist(p)));
                    }
                }
                appendActions();
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(@NonNull Call<List<Playlist>> call, @NonNull Throwable t) {
                items.clear(); appendActions(); adapter.notifyDataSetChanged();
            }
        });
    }

    private void loadArtists() {
        api.getFollowedArtists().enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(@NonNull Call<List<Artist>> call,
                                             @NonNull Response<List<Artist>> response) {
                items.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Artist a : response.body()) {
                        items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_ARTIST,
                            a.getAvatarUrl(), a.getName(), "Nghệ sĩ",
                            () -> openArtist(a)));
                    }
                }
                appendActions();
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(@NonNull Call<List<Artist>> call, @NonNull Throwable t) {
                items.clear(); appendActions(); adapter.notifyDataSetChanged();
            }
        });
    }

    private void loadAlbums() {
        items.clear();
        appendActions();
        adapter.notifyDataSetChanged();
    }

    private void loadLiked() {
        api.getLikedTracks().enqueue(new Callback<List<Track>>() {
            @Override public void onResponse(@NonNull Call<List<Track>> call,
                                             @NonNull Response<List<Track>> response) {
                items.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (Track t : response.body()) {
                        items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_TRACK,
                            t.getCoverUrl(), t.getTitle(), t.getArtistName(),
                            () -> openPlayer(t)));
                    }
                }
                appendActions();
                adapter.notifyDataSetChanged();
            }
            @Override public void onFailure(@NonNull Call<List<Track>> call, @NonNull Throwable t) {
                items.clear(); appendActions(); adapter.notifyDataSetChanged();
            }
        });
    }

    private void appendActions() {
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_artist), () -> {}));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_podcast), () -> {}));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_event), () -> {}));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_download, getString(R.string.add_music), () -> {}));
    }

    private void openPlayer(Track t) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", t);
        startActivity(intent);
    }

    private void openArtist(Artist a) {
        Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
        intent.putExtra("artistId", a.getArtistId());
        startActivity(intent);
    }

    private void openPlaylist(Playlist p) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlistId", p.getPlaylistId());
        intent.putExtra("playlistName", p.getName());
        startActivity(intent);
    }
}
