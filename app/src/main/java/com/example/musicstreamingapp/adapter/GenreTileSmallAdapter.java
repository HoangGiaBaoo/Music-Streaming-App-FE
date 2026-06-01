package com.example.musicstreamingapp.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

/**
 * Tile genre nhỏ (dùng cho section RELATED_GENRES trong Genre Detail). Logic màu
 * nền + ảnh bìa giống {@link GenreTileAdapter} nhưng layout nhỏ hơn để hiển thị
 * trong hàng cuộn ngang.
 */
public class GenreTileSmallAdapter extends RecyclerView.Adapter<GenreTileSmallAdapter.VH> {
    public interface Click { void onClick(Genre g); }
    private final List<Genre> items;
    private final Click click;
    private static final int[] FALLBACK_COLORS = {
        0xFFE91E63, 0xFF26A69A, 0xFF7E57C2, 0xFF8E24AA, 0xFF66BB6A,
        0xFFFFA726, 0xFF42A5F5, 0xFFEF5350, 0xFFAB47BC, 0xFF5C6BC0
    };
    public GenreTileSmallAdapter(List<Genre> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_genre_tile_small, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Genre g = items.get(pos);
        h.name.setText(g.getName() != null ? g.getName() : "");
        int color = FALLBACK_COLORS[pos % FALLBACK_COLORS.length];
        if (g.getCoverColor() != null) {
            try { color = Color.parseColor(g.getCoverColor()); } catch (Exception ignore) {}
        }
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(12f);
        h.itemView.setBackground(gd);

        String coverUrl = g.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            h.pic.setVisibility(View.VISIBLE);
            Glide.with(h.itemView)
                .load(RetrofitClient.BASE_MEDIA_URL + coverUrl)
                .centerCrop()
                .into(h.pic);
        } else {
            h.pic.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> click.onClick(g));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView pic; TextView name;
        VH(@NonNull View v) {
            super(v);
            pic = v.findViewById(R.id.iv_genre_pic);
            name = v.findViewById(R.id.tv_genre_name);
        }
    }
}
