package com.example.musicstreamingapp.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;

import java.util.List;

public class ExploreCardAdapter extends RecyclerView.Adapter<ExploreCardAdapter.VH> {

    public static class Item {
        public final String hashtag;
        public final int colorRes;
        public Item(String hashtag, int colorRes) {
            this.hashtag = hashtag;
            this.colorRes = colorRes;
        }
    }

    private final List<Item> items;
    public ExploreCardAdapter(List<Item> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_explore_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Item it = items.get(pos);
        h.tag.setText(it.hashtag);
        try { h.cover.setBackgroundColor(h.itemView.getContext().getColor(it.colorRes)); }
        catch (Exception e) { h.cover.setBackgroundColor(Color.GRAY); }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView tag;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_explore_cover);
            tag = v.findViewById(R.id.tv_explore_tag);
        }
    }
}
