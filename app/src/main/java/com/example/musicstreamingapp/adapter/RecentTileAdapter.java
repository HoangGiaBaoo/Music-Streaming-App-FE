package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class RecentTileAdapter extends RecyclerView.Adapter<RecentTileAdapter.VH> {
    public interface Click { void onClick(Playlist p); }

    private final List<Playlist> items;
    private final Click click;

    public RecentTileAdapter(List<Playlist> items, Click click) {
        this.items = items;
        this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_home_recent_tile, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = items.get(pos);
        h.title.setText(p.getName() != null ? p.getName() : "");
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
        ShapeableImageView cover; TextView title;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_recent_cover);
            title = v.findViewById(R.id.tv_recent_title);
        }
    }
}
