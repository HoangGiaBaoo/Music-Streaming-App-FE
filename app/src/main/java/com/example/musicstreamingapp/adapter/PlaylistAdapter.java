package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.util.ItemAnim;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {
    private final List<Playlist> playlists;
    private final OnPlaylistClickListener listener;
    private final int[] lastPosition = {-1};

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
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
        h.name.setText(p.getName());
        h.icon.setImageResource(R.drawable.ic_library);
        h.itemView.setOnClickListener(v -> listener.onPlaylistClick(p));
        ItemAnim.animate(h.itemView, pos, lastPosition);
    }

    @Override public int getItemCount() { return playlists.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, count;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.iv_cover);
            name = v.findViewById(R.id.tv_name);
            count = v.findViewById(R.id.tv_count);
        }
    }
}
