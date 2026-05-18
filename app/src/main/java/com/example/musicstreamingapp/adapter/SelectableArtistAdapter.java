package com.example.musicstreamingapp.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectableArtistAdapter extends RecyclerView.Adapter<SelectableArtistAdapter.VH> {

    public interface OnToggle { void onToggle(Artist artist); }

    private List<Artist> items;
    private Set<Long> selectedIds;
    private final OnToggle onToggle;
    private final int borderPx;
    private final int greenColor;

    public SelectableArtistAdapter(List<Artist> items, Set<Long> selectedIds,
                                   OnToggle onToggle, Context context) {
        this.items = items;
        this.selectedIds = selectedIds;
        this.onToggle = onToggle;
        this.borderPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
        this.greenColor = ContextCompat.getColor(context, R.color.spotify_green);
    }

    public void setItems(List<Artist> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<Long> ids) {
        this.selectedIds = ids;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_selectable_artist, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Artist a = items.get(pos);
        h.name.setText(a.getName() != null ? a.getName() : "");

        if (a.getAvatarUrl() != null) {
            Glide.with(h.itemView)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(h.avatar);
        } else {
            h.avatar.setImageResource(R.drawable.placeholder_gradient);
        }

        boolean selected = a.getArtistId() != null && selectedIds.contains(a.getArtistId());
        if (selected) {
            h.avatar.setBorderWidth(borderPx);
            h.avatar.setBorderColor(greenColor);
            h.flCheck.setVisibility(View.VISIBLE);
        } else {
            h.avatar.setBorderWidth(0);
            h.flCheck.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> onToggle.onToggle(a));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView avatar;
        TextView name;
        View flCheck;

        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.iv_avatar);
            name = v.findViewById(R.id.tv_name);
            flCheck = v.findViewById(R.id.fl_check);
        }
    }
}
