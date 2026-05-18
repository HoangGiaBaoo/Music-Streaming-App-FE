package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.util.LrcParser.LrcLine;

import java.util.List;

public class LyricLineAdapter extends RecyclerView.Adapter<LyricLineAdapter.VH> {

    private final List<LrcLine> lines;
    private int activeIndex = -1;

    public LyricLineAdapter(List<LrcLine> lines) {
        this.lines = lines;
    }

    public void setActiveIndex(int newIndex) {
        if (newIndex == activeIndex) return;
        int old = activeIndex;
        activeIndex = newIndex;
        if (old >= 0) notifyItemChanged(old);
        if (newIndex >= 0) notifyItemChanged(newIndex);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_lyric_line, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tv.setText(lines.get(position).text);
        holder.tv.setAlpha(position == activeIndex ? 1.0f : 0.35f);
    }

    @Override
    public int getItemCount() { return lines.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tv_lyric_line);
        }
    }
}
