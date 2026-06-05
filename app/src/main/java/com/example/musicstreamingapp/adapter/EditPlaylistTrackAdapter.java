package com.example.musicstreamingapp.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter cho màn "Chỉnh sửa danh sách phát": mỗi hàng có nút trừ (xoá) ở đầu
 * và tay nắm 3 gạch (kéo để sắp xếp) ở cuối. Adapter tự giữ danh sách đang sửa.
 */
public class EditPlaylistTrackAdapter extends RecyclerView.Adapter<EditPlaylistTrackAdapter.VH> {

    public interface Listener {
        void onRemove(int position);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
        void onListChanged();
    }

    private final List<Track> items = new ArrayList<>();
    private final Listener listener;

    public EditPlaylistTrackAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Track> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public List<Track> getItems() {
        return new ArrayList<>(items);
    }

    public void onItemMove(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        if (from < to) {
            for (int i = from; i < to; i++) Collections.swap(items, i, i + 1);
        } else {
            for (int i = from; i > to; i--) Collections.swap(items, i, i - 1);
        }
        notifyItemMoved(from, to);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
        if (listener != null) listener.onListChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_edit_playlist_track, parent, false);
        return new VH(v);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Track t = items.get(position);
        h.title.setText(t.getTitle());
        h.artist.setText(t.getArtistName());
        Glide.with(h.cover.getContext())
            .load(t.getCoverUrl() != null ? RetrofitClient.BASE_MEDIA_URL + t.getCoverUrl() : null)
            .placeholder(R.drawable.placeholder_gradient)
            .into(h.cover);

        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) listener.onRemove(pos);
        });

        h.drag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && listener != null) {
                listener.onStartDrag(h);
            }
            return false;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover, drag;
        TextView title, artist;
        ImageButton btnRemove;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.iv_cover);
            drag = v.findViewById(R.id.iv_drag);
            title = v.findViewById(R.id.tv_title);
            artist = v.findViewById(R.id.tv_artist);
            btnRemove = v.findViewById(R.id.btn_remove);
        }
    }
}
