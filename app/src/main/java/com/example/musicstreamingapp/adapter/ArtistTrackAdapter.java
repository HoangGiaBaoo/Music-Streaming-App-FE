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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ArtistTrackAdapter extends RecyclerView.Adapter<ArtistTrackAdapter.VH> {

    public interface Callbacks {
        void onTrackClick(Track track);
        void onMoreClick(Track track, View anchor);
        default void onInPlaylistClick(Track track) {}
    }

    private final List<Track> items;
    private final Callbacks callbacks;
    private Set<Long> inPlaylistIds = new HashSet<>();

    public ArtistTrackAdapter(List<Track> items, Callbacks callbacks) {
        this.items = items;
        this.callbacks = callbacks;
    }

    public void setInPlaylistIds(Set<Long> ids) {
        this.inPlaylistIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_track, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Track t = items.get(pos);
        h.rank.setText(String.valueOf(pos + 1));
        h.title.setText(t.getTitle() != null ? t.getTitle() : "");
        h.playCount.setText(formatCount(t.getPlayCount()));

        if (t.getCoverUrl() != null) {
            Glide.with(h.itemView)
                .load(RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }

        boolean inPlaylist = t.getTrackId() != null && inPlaylistIds.contains(t.getTrackId());
        h.ivInPlaylist.setVisibility(inPlaylist ? View.VISIBLE : View.GONE);
        h.ivInPlaylist.setOnClickListener(v -> callbacks.onInPlaylistClick(t));

        h.itemView.setOnClickListener(v -> callbacks.onTrackClick(t));
        h.btnMore.setOnClickListener(v -> callbacks.onMoreClick(t, h.btnMore));
    }

    @Override public int getItemCount() { return items.size(); }

    private static String formatCount(Integer count) {
        if (count == null || count == 0) return "";
        return String.format(Locale.US, "%,d", count).replace(',', '.');
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView rank, title, playCount;
        ImageView cover, ivInPlaylist;
        ImageButton btnMore;

        VH(@NonNull View v) {
            super(v);
            rank = v.findViewById(R.id.tv_rank);
            title = v.findViewById(R.id.tv_title);
            playCount = v.findViewById(R.id.tv_play_count);
            cover = v.findViewById(R.id.iv_cover);
            ivInPlaylist = v.findViewById(R.id.iv_in_playlist);
            btnMore = v.findViewById(R.id.btn_more);
        }
    }
}
