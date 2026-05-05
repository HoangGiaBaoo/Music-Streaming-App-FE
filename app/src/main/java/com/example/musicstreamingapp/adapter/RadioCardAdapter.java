package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

public class RadioCardAdapter extends RecyclerView.Adapter<RadioCardAdapter.VH> {
    public interface Click { void onClick(Artist a); }
    private final List<Artist> items;
    private final Click click;
    private static final int[] PASTEL_BG = {
        R.drawable.bg_radio_pastel,
        R.drawable.bg_radio_orange,
        R.drawable.bg_radio_teal,
        R.drawable.bg_radio_purple
    };
    public RadioCardAdapter(List<Artist> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_radio_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Artist a = items.get(pos);
        h.name.setText(a.getName() != null ? a.getName() : "");
        h.subtitle.setText(a.getBio() != null ? a.getBio() : "");
        h.bgFrame.setBackgroundResource(PASTEL_BG[pos % PASTEL_BG.length]);
        if (a.getAvatarUrl() != null) {
            Glide.with(h.itemView).load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                .placeholder(R.drawable.placeholder_gradient).into(h.avatar);
        }
        h.itemView.setOnClickListener(v -> click.onClick(a));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name, subtitle; FrameLayout bgFrame;
        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.iv_radio_artist);
            name = v.findViewById(R.id.tv_radio_artist);
            subtitle = v.findViewById(R.id.tv_radio_subtitle);
            bgFrame = v.findViewById(R.id.fl_radio_bg);
        }
    }
}
