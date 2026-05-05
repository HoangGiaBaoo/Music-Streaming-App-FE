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
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class TrackCardAdapter extends RecyclerView.Adapter<TrackCardAdapter.VH> {
    private final List<Track> tracks;
    private final OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    public TrackCardAdapter(List<Track> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_track_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Track t = tracks.get(pos);
        h.title.setText(t.getTitle());
        h.artist.setText(t.getArtist() != null ? t.getArtist().getName() : "");
        Context ctx = h.itemView.getContext();
        if (t.getCoverUrl() != null) {
            Glide.with(ctx)
                .load(RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> listener.onTrackClick(t, h.getAdapterPosition()));
    }

    @Override public int getItemCount() { return tracks.size(); }

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
