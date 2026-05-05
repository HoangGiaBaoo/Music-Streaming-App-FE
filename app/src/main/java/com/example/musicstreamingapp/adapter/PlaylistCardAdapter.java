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
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class PlaylistCardAdapter extends RecyclerView.Adapter<PlaylistCardAdapter.VH> {
    public interface Click { void onClick(Playlist p); }
    private final List<Playlist> items;
    private final Click click;
    public PlaylistCardAdapter(List<Playlist> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = items.get(pos);
        h.name.setText(p.getName() != null ? p.getName() : "");
        String desc = p.getDescription();
        h.subtitle.setText(desc != null ? desc : "");
        h.subtitle.setVisibility(desc != null && !desc.isEmpty() ? View.VISIBLE : View.GONE);
        if (p.getCoverUrl() != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + p.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient).into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> click.onClick(p));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView name, subtitle;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_playlist_cover);
            name = v.findViewById(R.id.tv_playlist_name);
            subtitle = v.findViewById(R.id.tv_playlist_subtitle);
        }
    }
}
