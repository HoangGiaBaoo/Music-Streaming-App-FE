package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.ui.PlaylistCoverView;
import com.example.musicstreamingapp.util.ItemAnim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_PLAYLIST = 0;
    public static final int TYPE_ARTIST = 1;
    public static final int TYPE_ALBUM = 2;
    public static final int TYPE_TRACK = 3;
    public static final int TYPE_ACTION = 4;

    public static class Item {
        public int type;
        public String coverUrl;
        public String title;
        public String subtitle;
        public int iconRes;
        public Runnable onClick;
        @Nullable public Long playlistId;

        public static Item row(int type, String coverUrl, String title, String subtitle, Runnable click) {
            Item it = new Item();
            it.type = type; it.coverUrl = coverUrl; it.title = title; it.subtitle = subtitle;
            it.onClick = click;
            return it;
        }

        public static Item playlistRow(Long playlistId, String coverUrl, String title,
                                       String subtitle, Runnable click) {
            Item it = row(TYPE_PLAYLIST, coverUrl, title, subtitle, click);
            it.playlistId = playlistId;
            return it;
        }

        public static Item action(int iconRes, String label, Runnable click) {
            Item it = new Item();
            it.type = TYPE_ACTION; it.iconRes = iconRes; it.title = label;
            it.onClick = click;
            return it;
        }
    }

    private final List<Item> items;
    @Nullable private final LibraryRepository repo;
    private final Map<Long, List<Track>> tracksCache = new HashMap<>();
    private final int[] lastPosition = {-1};

    public LibraryAdapter(List<Item> items, @Nullable LibraryRepository repo) {
        this.items = items;
        this.repo = repo;
    }

    @Override public int getItemViewType(int position) { return items.get(position).type; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ARTIST) return new ArtistVH(inf.inflate(R.layout.item_library_artist, parent, false));
        if (viewType == TYPE_ACTION) return new ActionVH(inf.inflate(R.layout.item_library_action, parent, false));
        return new RowVH(inf.inflate(R.layout.item_library_row, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        Item it = items.get(pos);
        if (h instanceof RowVH) bindRow((RowVH) h, it);
        else if (h instanceof ArtistVH) ((ArtistVH) h).bind(it);
        else ((ActionVH) h).bind(it);
        ItemAnim.animate(h.itemView, pos, lastPosition);
    }

    private void bindRow(RowVH h, Item it) {
        h.title.setText(it.title);
        h.subtitle.setText(it.subtitle);
        h.boundPlaylistId = it.playlistId;

        if (it.coverUrl != null && !it.coverUrl.isEmpty()) {
            h.cover.bind(it.coverUrl, null);
        } else if (it.type == TYPE_PLAYLIST && it.playlistId != null) {
            List<Track> cached = tracksCache.get(it.playlistId);
            if (cached != null) {
                h.cover.bind(null, cached);
            } else {
                h.cover.bind(null, null);
                fetchTracksForCover(it.playlistId, h);
            }
        } else {
            h.cover.bind(null, null);
        }

        h.itemView.setOnClickListener(v -> { if (it.onClick != null) it.onClick.run(); });
    }

    private void fetchTracksForCover(Long playlistId, RowVH h) {
        if (repo == null || playlistId == null) return;
        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                tracksCache.put(playlistId, data);
                if (Objects.equals(h.boundPlaylistId, playlistId)) {
                    h.cover.bind(null, data);
                }
            }
            @Override public void onError(String message) { /* keep placeholder */ }
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class RowVH extends RecyclerView.ViewHolder {
        PlaylistCoverView cover; TextView title, subtitle;
        @Nullable Long boundPlaylistId;
        RowVH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.lib_cover_view);
            title = v.findViewById(R.id.tv_lib_title);
            subtitle = v.findViewById(R.id.tv_lib_subtitle);
        }
    }

    static class ArtistVH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name;
        ArtistVH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.iv_lib_avatar);
            name = v.findViewById(R.id.tv_lib_artist_name);
        }
        void bind(Item it) {
            name.setText(it.title);
            if (it.coverUrl != null) {
                Glide.with(itemView).load(RetrofitClient.BASE_MEDIA_URL + it.coverUrl)
                    .placeholder(R.drawable.placeholder_gradient).into(avatar);
            } else {
                avatar.setImageResource(R.drawable.placeholder_gradient);
            }
            itemView.setOnClickListener(v -> { if (it.onClick != null) it.onClick.run(); });
        }
    }

    static class ActionVH extends RecyclerView.ViewHolder {
        ImageView icon; TextView label;
        ActionVH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.iv_action_icon);
            label = v.findViewById(R.id.tv_action_label);
        }
        void bind(Item it) {
            icon.setImageResource(it.iconRes);
            label.setText(it.title);
            itemView.setOnClickListener(v -> { if (it.onClick != null) it.onClick.run(); });
        }
    }
}
