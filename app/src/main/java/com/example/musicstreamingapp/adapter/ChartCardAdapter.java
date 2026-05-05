package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Playlist;

import java.util.List;

public class ChartCardAdapter extends RecyclerView.Adapter<ChartCardAdapter.VH> {
    public interface Click { void onClick(Playlist p); }
    private final List<Playlist> items;
    private final Click click;
    public ChartCardAdapter(List<Playlist> items, Click click) {
        this.items = items; this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_home_chart, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = items.get(pos);
        h.titleTop.setText(p.getName() != null ? p.getName() : "Top 50");
        h.subtitle.setText("VIETNAM");
        h.desc.setText(p.getDescription() != null ? p.getDescription()
            : "Thông tin cập nhật hàng ngày về những bản nhạc đ…");
        h.itemView.setOnClickListener(v -> click.onClick(p));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView titleTop, subtitle, desc;
        VH(@NonNull View v) {
            super(v);
            titleTop = v.findViewById(R.id.tv_chart_title_top);
            subtitle = v.findViewById(R.id.tv_chart_subtitle);
            desc = v.findViewById(R.id.tv_chart_desc);
        }
    }
}
