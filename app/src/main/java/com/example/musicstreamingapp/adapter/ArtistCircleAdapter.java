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
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class ArtistCircleAdapter extends RecyclerView.Adapter<ArtistCircleAdapter.VH> {
    public interface Click { void onClick(Artist a); }
    private final List<Artist> items;
    private final Click click;
    public ArtistCircleAdapter(List<Artist> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_circle, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Artist a = items.get(pos);
        h.name.setText(a.getName() != null ? a.getName() : "");
        if (a.getAvatarUrl() != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                .placeholder(R.drawable.placeholder_gradient).into(h.avatar);
        } else {
            h.avatar.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> click.onClick(a));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name;
        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.iv_artist_avatar);
            name = v.findViewById(R.id.tv_artist_name);
        }
    }
}
