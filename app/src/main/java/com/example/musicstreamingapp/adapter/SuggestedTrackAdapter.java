package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class SuggestedTrackAdapter extends RecyclerView.Adapter<SuggestedTrackAdapter.VH> {

    public interface OnAddListener {
        void onAdd(Track track, int position);
    }

    private final List<Track> tracks;
    private final OnAddListener listener;

    public SuggestedTrackAdapter(List<Track> tracks, OnAddListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_suggested_track, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Track t = tracks.get(pos);
        h.title.setText(t.getTitle());
        h.artist.setText(t.getArtist() != null ? t.getArtist().getName() : "");

        String url = t.getCoverUrl();
        if (url == null || url.isEmpty()) {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        } else {
            Glide.with(h.cover.getContext())
                .load(RetrofitClient.BASE_MEDIA_URL + url)
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(h.cover);
        }

        h.btnAdd.setOnClickListener(v -> {
            if (listener != null) listener.onAdd(t, h.getAdapterPosition());
        });
    }

    @Override public int getItemCount() { return tracks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist;
        ImageButton btnAdd;
        VH(View v) {
            super(v);
            cover = v.findViewById(R.id.iv_cover);
            title = v.findViewById(R.id.tv_title);
            artist = v.findViewById(R.id.tv_artist);
            btnAdd = v.findViewById(R.id.btn_add);
        }
    }
}
