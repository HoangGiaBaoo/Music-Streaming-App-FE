package com.example.musicstreamingapp.adapter;

import android.content.Context;
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
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.ui.PlaylistCoverView;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlaylistCardAdapter extends RecyclerView.Adapter<PlaylistCardAdapter.VH> {
    public interface Click { void onClick(Playlist p); }
    private final List<Playlist> items;
    private final Click click;
    // Khi playlist chưa có ảnh bìa riêng → mượn ảnh track để ghép cover 2x2 (giống Thư viện).
    private final Map<Long, List<Track>> tracksCache = new HashMap<>();
    private LibraryRepository repo;

    public PlaylistCardAdapter(List<Playlist> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (repo == null) {
            Context ctx = parent.getContext().getApplicationContext();
            repo = new LibraryRepository(RetrofitClient.getApiService(TokenManager.getPrefs(ctx)));
        }
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = items.get(pos);
        Long pid = p.getPlaylistId();
        h.boundPlaylistId = pid;
        h.name.setText(p.getName() != null ? p.getName() : "");
        String desc = p.getDescription();
        h.subtitle.setText(desc != null ? desc : "");
        h.subtitle.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE);

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

        h.itemView.setOnClickListener(v -> click.onClick(p));
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
            @Override public void onError(String message) { /* giữ placeholder */ }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        PlaylistCoverView cover; TextView name, subtitle;
        @Nullable Long boundPlaylistId;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.cover_view);
            name = v.findViewById(R.id.tv_playlist_name);
            subtitle = v.findViewById(R.id.tv_playlist_subtitle);
        }
    }
}
