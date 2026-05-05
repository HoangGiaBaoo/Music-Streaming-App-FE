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

import java.util.ArrayList;
import java.util.List;

public class RecentSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface Listener {
        void onTrackClick(Track t, int position);
    }

    private final List<Object> items = new ArrayList<>();
    private final Listener listener;

    public RecentSectionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Object> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inflater.inflate(R.layout.item_recent_header, parent, false));
        }
        return new ItemVH(inflater.inflate(R.layout.item_track_list, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object obj = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).header.setText((String) obj);
        } else {
            ItemVH h = (ItemVH) holder;
            Track t = (Track) obj;
            h.title.setText(t.getTitle());
            h.subtitle.setText(t.getArtist() != null ? t.getArtist().getName() : "");
            if (t.getCoverUrl() != null) {
                Glide.with(h.itemView)
                    .load(RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl())
                    .placeholder(R.drawable.placeholder_gradient)
                    .into(h.cover);
            } else {
                h.cover.setImageResource(R.drawable.placeholder_gradient);
            }
            h.itemView.setOnClickListener(v ->
                listener.onTrackClick(t, h.getAdapterPosition()));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView header;
        HeaderVH(View v) { super(v); header = v.findViewById(R.id.tv_header); }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, subtitle;
        ItemVH(View v) {
            super(v);
            cover = v.findViewById(R.id.iv_cover);
            title = v.findViewById(R.id.tv_title);
            subtitle = v.findViewById(R.id.tv_artist);
        }
    }
}
