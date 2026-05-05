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
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class HomeTrackRowAdapter extends RecyclerView.Adapter<HomeTrackRowAdapter.VH> {
    public interface Click { void onClick(Track t); }
    private final List<Track> items;
    private final Click click;
    public HomeTrackRowAdapter(List<Track> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_track_row, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Track t = items.get(pos);
        h.title.setText(t.getTitle() != null ? t.getTitle() : "");
        h.artist.setText(t.getArtistName());
        if (t.getCoverUrl() != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient).into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> click.onClick(t));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView title, artist;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_track_cover);
            title = v.findViewById(R.id.tv_track_title);
            artist = v.findViewById(R.id.tv_track_artist);
        }
    }
}
