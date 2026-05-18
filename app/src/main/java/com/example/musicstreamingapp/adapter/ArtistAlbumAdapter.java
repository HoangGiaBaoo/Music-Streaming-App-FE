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

public class ArtistAlbumAdapter extends RecyclerView.Adapter<ArtistAlbumAdapter.VH> {

    public interface OnAlbumClick { void onClick(Album album); }

    private final List<Album> items;
    private final OnAlbumClick click;

    public ArtistAlbumAdapter(List<Album> items, OnAlbumClick click) {
        this.items = items;
        this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_album_vertical, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Album a = items.get(pos);
        h.title.setText(a.getTitle() != null ? a.getTitle() : "");
        h.subtitle.setText(buildSubtitle(a));

        if (a.getCoverUrl() != null) {
            Glide.with(h.itemView)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }

        h.itemView.setOnClickListener(v -> click.onClick(a));
    }

    @Override public int getItemCount() { return items.size(); }

    private static String buildSubtitle(Album a) {
        String type = a.getAlbumTypeLabel();
        String year = extractYear(a.getReleaseDate());
        if (year.isEmpty()) return type;
        return type + " • " + year;
    }

    private static String extractYear(String date) {
        if (date == null || date.length() < 4) return "";
        return date.substring(0, 4);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, subtitle;

        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_cover);
            title = v.findViewById(R.id.tv_title);
            subtitle = v.findViewById(R.id.tv_subtitle);
        }
    }
}
