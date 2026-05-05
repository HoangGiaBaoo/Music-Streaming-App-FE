package com.example.musicstreamingapp.adapter;

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

public class AlbumLabelAdapter extends RecyclerView.Adapter<AlbumLabelAdapter.VH> {
    public interface Click { void onClick(Album a); }
    private final List<Album> items;
    private final Click click;
    public AlbumLabelAdapter(List<Album> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_album_label_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Album a = items.get(pos);
        h.label.setText(a.getAlbumTypeLabel());
        h.title.setText(a.getTitle() != null ? a.getTitle() : "");
        h.artist.setText(a.getArtist() != null ? a.getArtist().getName() : "");
        if (a.getCoverUrl() != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient).into(h.cover);
        }
        h.itemView.setOnClickListener(v -> click.onClick(a));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView label, title, artist;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_album_cover);
            label = v.findViewById(R.id.tv_album_label);
            title = v.findViewById(R.id.tv_album_title);
            artist = v.findViewById(R.id.tv_album_artist);
        }
    }
}
