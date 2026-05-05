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
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class ExploreArtistAdapter extends RecyclerView.Adapter<ExploreArtistAdapter.VH> {

    public static class Card {
        public final String label;
        public final String coverUrl;
        public final Runnable onClick;
        public Card(String label, String coverUrl, Runnable onClick) {
            this.label = label; this.coverUrl = coverUrl; this.onClick = onClick;
        }
    }

    private final List<Card> items;
    public ExploreArtistAdapter(List<Card> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_explore_artist_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Card c = items.get(pos);
        h.label.setText(c.label);
        if (c.coverUrl != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + c.coverUrl)
                .placeholder(R.drawable.placeholder_gradient).into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.placeholder_gradient);
        }
        h.itemView.setOnClickListener(v -> { if (c.onClick != null) c.onClick.run(); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView label;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_explore_artist_cover);
            label = v.findViewById(R.id.tv_explore_artist_label);
        }
    }
}
