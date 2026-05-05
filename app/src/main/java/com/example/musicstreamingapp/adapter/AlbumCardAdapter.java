package com.example.musicstreamingapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class AlbumCardAdapter extends RecyclerView.Adapter<AlbumCardAdapter.VH> {
    private final List<Album> albums;
    private final OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumCardAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_album_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Album a = albums.get(pos);
        h.title.setText(a.getTitle());
        h.artist.setText(a.getArtist() != null ? a.getArtist().getName() : "");
        Context ctx = h.itemView.getContext();
        if (a.getCoverUrl() != null) {
            Glide.with(ctx)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> listener.onAlbumClick(a));
    }

    @Override public int getItemCount() { return albums.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;
        VH(View v) {
            super(v);
            cover = v.findViewById(R.id.iv_cover);
            title = v.findViewById(R.id.tv_title);
            artist = v.findViewById(R.id.tv_artist);
        }
    }
}
