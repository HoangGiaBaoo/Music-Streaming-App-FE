package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.ui.PlaylistCoverView;
import com.example.musicstreamingapp.viewmodel.AddToPlaylistViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION = AddToPlaylistViewModel.ListItem.TYPE_SECTION;
    private static final int TYPE_PLAYLIST = AddToPlaylistViewModel.ListItem.TYPE_PLAYLIST;

    public interface OnToggle { void onToggle(long id); }
    public interface OnClearAll { void onClearAll(); }

    private List<AddToPlaylistViewModel.ListItem> items = new ArrayList<>();
    private Set<Long> selectedIds = new HashSet<>();
    private final OnToggle onToggle;
    private final OnClearAll onClearAll;

    public PlaylistPickerAdapter(OnToggle onToggle, OnClearAll onClearAll) {
        this.onToggle = onToggle;
        this.onClearAll = onClearAll;
    }

    public void setItems(List<AddToPlaylistViewModel.ListItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedIds(Set<Long> ids) {
        this.selectedIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            return new SectionVH(inf.inflate(R.layout.item_playlist_picker_section, parent, false));
        }
        return new PlaylistVH(inf.inflate(R.layout.item_playlist_picker, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AddToPlaylistViewModel.ListItem item = items.get(position);
        if (holder instanceof SectionVH) {
            SectionVH svh = (SectionVH) holder;
            svh.tvLabel.setText(item.sectionLabel);
            if (item.showClearAll) {
                svh.tvClearAll.setVisibility(View.VISIBLE);
                svh.tvClearAll.setOnClickListener(v -> { if (onClearAll != null) onClearAll.onClearAll(); });
            } else {
                svh.tvClearAll.setVisibility(View.GONE);
            }
        } else if (holder instanceof PlaylistVH) {
            PlaylistVH pvh = (PlaylistVH) holder;
            pvh.tvName.setText(item.displayName());

            if (item.trackCount < 0) {
                pvh.tvTrackCount.setVisibility(View.GONE);
            } else {
                pvh.tvTrackCount.setVisibility(View.VISIBLE);
                pvh.tvTrackCount.setText(item.trackCount == 0 ? "Trống" : item.trackCount + " bài hát");
            }

            boolean selected = selectedIds.contains(item.id);
            pvh.ivCheck.setImageResource(selected
                ? R.drawable.ic_check_circle_green : R.drawable.ic_circle_outline);

            if (item.liked) {
                // "Bài hát đã thích": cover gradient tím + trái tim trắng.
                pvh.coverView.setVisibility(View.GONE);
                pvh.likedCover.setVisibility(View.VISIBLE);
            } else {
                pvh.likedCover.setVisibility(View.GONE);
                pvh.coverView.setVisibility(View.VISIBLE);
                String coverUrl = item.playlist != null ? item.playlist.getCoverUrl() : null;
                pvh.coverView.bind(coverUrl, item.sampleTracks);
            }

            final long id = item.id;
            pvh.itemView.setOnClickListener(v -> { if (onToggle != null) onToggle.onToggle(id); });
        }
    }

    static class SectionVH extends RecyclerView.ViewHolder {
        TextView tvLabel, tvClearAll;
        SectionVH(@NonNull View v) {
            super(v);
            tvLabel = v.findViewById(R.id.tv_section_label);
            tvClearAll = v.findViewById(R.id.tv_clear_all);
        }
    }

    static class PlaylistVH extends RecyclerView.ViewHolder {
        PlaylistCoverView coverView;
        View likedCover;
        ImageView ivCheck;
        TextView tvName, tvTrackCount;
        PlaylistVH(@NonNull View v) {
            super(v);
            coverView = v.findViewById(R.id.cover_view);
            likedCover = v.findViewById(R.id.liked_cover);
            ivCheck = v.findViewById(R.id.iv_check);
            tvName = v.findViewById(R.id.tv_name);
            tvTrackCount = v.findViewById(R.id.tv_track_count);
        }
    }
}
