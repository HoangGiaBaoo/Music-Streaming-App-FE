package com.example.musicstreamingapp.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddTrackAdapter extends RecyclerView.Adapter<AddTrackAdapter.VH> {

    public interface OnToggle { void onToggle(Track track); }

    private List<Track> tracks = new ArrayList<>();
    private Set<Long> addedIds = new HashSet<>();
    private final OnToggle onToggle;

    public AddTrackAdapter(OnToggle onToggle) {
        this.onToggle = onToggle;
    }

    public void setTracks(List<Track> data) {
        this.tracks = data != null ? new ArrayList<>(data) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setAddedIds(Set<Long> ids) {
        this.addedIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_add_track, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Track t = tracks.get(position);
        h.title.setText(t.getTitle());
        h.artist.setText(t.getArtistName());
        Glide.with(h.cover.getContext())
            .load(t.getCoverUrl() != null ? RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl() : null)
            .placeholder(R.drawable.placeholder_gradient)
            .into(h.cover);

        boolean added = t.getTrackId() != null && addedIds.contains(t.getTrackId());
        if (added) {
            h.btnAdd.setImageResource(R.drawable.ic_check_circle_green);
            ImageViewCompat.setImageTintList(h.btnAdd, null);
        } else {
            h.btnAdd.setImageResource(R.drawable.ic_add_circle_outline);
            ImageViewCompat.setImageTintList(h.btnAdd,
                ColorStateList.valueOf(ContextCompat.getColor(h.btnAdd.getContext(), R.color.accent_white)));
        }

        View.OnClickListener toggle = v -> {
            if (onToggle != null) onToggle.onToggle(t);
        };
        h.btnAdd.setOnClickListener(toggle);
        h.itemView.setOnClickListener(toggle);
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
