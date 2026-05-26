package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.ui.PlaylistCoverView;
import com.example.musicstreamingapp.util.ItemAnim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
    private final List<Playlist> playlists;
    private final OnPlaylistClickListener listener;
    private final LibraryRepository repo;
    private final Map<Long, List<Track>> tracksCache = new HashMap<>();
    private final int[] lastPosition = {-1};

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(List<Playlist> playlists, LibraryRepository repo,
                           OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.repo = repo;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = playlists.get(pos);
        Long pid = p.getPlaylistId();
        h.name.setText(p.getName());
        h.boundPlaylistId = pid;

        String coverUrl = p.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            h.cover.bind(coverUrl, null);
        } else {
            List<Track> cached = tracksCache.get(pid);
            if (cached != null) {
                h.cover.bind(null, cached);
            } else {
                h.cover.bind(null, null);
                fetchTracksForCover(pid, h);
            }
        }

        h.itemView.setOnClickListener(v -> listener.onPlaylistClick(p));
        ItemAnim.animate(h.itemView, pos, lastPosition);
    }

    private void fetchTracksForCover(Long playlistId, VH h) {
        if (repo == null || playlistId == null) return;
        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                tracksCache.put(playlistId, data);
                if (Objects.equals(h.boundPlaylistId, playlistId)) {
                    h.cover.bind(null, data);
                }
            }
            @Override public void onError(String message) { /* keep placeholder */ }
        });
    }

    @Override public int getItemCount() { return playlists.size(); }

    static class VH extends RecyclerView.ViewHolder {
        PlaylistCoverView cover;
        TextView name, count;
        @Nullable Long boundPlaylistId;
        VH(View v) {
            super(v);
            cover = v.findViewById(R.id.cover_view);
            name = v.findViewById(R.id.tv_name);
            count = v.findViewById(R.id.tv_count);
        }
    }
}
