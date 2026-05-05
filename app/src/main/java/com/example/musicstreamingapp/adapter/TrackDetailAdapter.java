package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.TimeUtil;

import java.util.List;

public class TrackDetailAdapter extends RecyclerView.Adapter<TrackDetailAdapter.VH> {
    private final List<Track> tracks;
    private final OnTrackClickListener listener;
    private OnTrackMoreListener moreListener;

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    public interface OnTrackMoreListener {
        void onMoreClick(Track track, int position);
    }

    public TrackDetailAdapter(List<Track> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    public void setMoreListener(OnTrackMoreListener l) { this.moreListener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_track_detail, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Track t = tracks.get(pos);
        h.number.setText(String.valueOf(pos + 1));
        h.title.setText(t.getTitle());
        h.artist.setText(t.getArtist() != null ? t.getArtist().getName() : "");
        h.duration.setText(t.getDuration() != null ? TimeUtil.formatSeconds(t.getDuration()) : "");
        h.itemView.setOnClickListener(v -> listener.onTrackClick(t, h.getAdapterPosition()));
        h.btnMore.setOnClickListener(v -> {
            if (moreListener != null) moreListener.onMoreClick(t, h.getAdapterPosition());
        });
    }

    @Override public int getItemCount() { return tracks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView number, title, artist, duration;
        View btnMore;
        VH(View v) {
            super(v);
            number = v.findViewById(R.id.tv_number);
            title = v.findViewById(R.id.tv_title);
            artist = v.findViewById(R.id.tv_artist);
            duration = v.findViewById(R.id.tv_duration);
            btnMore = v.findViewById(R.id.btn_more);
        }
    }
}
