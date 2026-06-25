package com.example.musicstreamingapp.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.HomeSection;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.ui.PlaylistCoverView;
import com.example.musicstreamingapp.util.ItemAnim;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VT_FEATURED = 0;
    private static final int VT_SECTION = 1;
    private static final int VT_TRACK_ROW = 2;

    public interface OnHomeItemClick {
        void onTrack(Track track);
        void onArtist(Artist artist);
        void onAlbum(Album album);
        void onPlaylist(Playlist playlist);
    }

    private final List<HomeSection> sections;
    private final OnHomeItemClick listener;
    private final int[] lastPosition = {-1};
    // Banner FEATURED chưa có ảnh bìa → mượn track để ghép cover 2x2 (giống thẻ playlist).
    private final Map<Long, List<Track>> featuredTracksCache = new HashMap<>();
    private LibraryRepository repo;

    public HomeFeedAdapter(List<HomeSection> sections, OnHomeItemClick listener) {
        this.sections = sections;
        this.listener = listener;
    }

    private LibraryRepository repo(@NonNull View anyView) {
        if (repo == null) {
            android.content.Context ctx = anyView.getContext().getApplicationContext();
            repo = new LibraryRepository(RetrofitClient.getApiService(TokenManager.getPrefs(ctx)));
        }
        return repo;
    }

    @Override
    public int getItemViewType(int position) {
        HomeSection s = sections.get(position);
        if (HomeSection.FEATURED.equals(s.getKind())) return VT_FEATURED;
        if (HomeSection.START_LISTENING.equals(s.getKind())
                || HomeSection.CHART.equals(s.getKind())) return VT_TRACK_ROW;
        return VT_SECTION;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VT_FEATURED) {
            return new FeaturedVH(inf.inflate(R.layout.item_home_featured, parent, false));
        }
        if (viewType == VT_TRACK_ROW) {
            return new TrackListVH(inf.inflate(R.layout.item_home_section, parent, false));
        }
        return new SectionVH(inf.inflate(R.layout.item_home_section, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        HomeSection s = sections.get(position);
        if (h instanceof FeaturedVH) ((FeaturedVH) h).bind(s);
        else if (h instanceof TrackListVH) ((TrackListVH) h).bind(s);
        else ((SectionVH) h).bind(s);
        ItemAnim.animate(h.itemView, position, lastPosition);
    }

    @Override public int getItemCount() { return sections.size(); }

    // ---- Featured banner ----
    class FeaturedVH extends RecyclerView.ViewHolder {
        PlaylistCoverView cover; TextView tv;
        Long boundPlaylistId;
        FeaturedVH(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_featured_cover);
            tv = itemView.findViewById(R.id.tv_featured_title);
        }
        void bind(HomeSection s) {
            List<Playlist> list = s.asPlaylists(RetrofitClient.GSON);
            if (list.isEmpty()) {
                tv.setText(s.getTitle() != null ? s.getTitle() : "");
                boundPlaylistId = null;
                cover.bind(null, null);
                return;
            }
            Playlist p = list.get(0);
            tv.setText(p.getName() != null ? p.getName().toUpperCase() : "");
            boundPlaylistId = p.getPlaylistId();

            String url = p.getCoverUrl();
            if (url != null && !url.isEmpty()) {
                cover.bind(url, null);
            } else {
                List<Track> cached = featuredTracksCache.get(p.getPlaylistId());
                if (cached != null) {
                    cover.bind(null, cached);
                } else {
                    cover.bind(null, null);
                    fetchFeaturedTracks(p.getPlaylistId());
                }
            }
            itemView.setOnClickListener(v -> listener.onPlaylist(p));
        }

        private void fetchFeaturedTracks(Long playlistId) {
            if (playlistId == null) return;
            repo(itemView).getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
                @Override public void onSuccess(List<Track> data) {
                    featuredTracksCache.put(playlistId, data);
                    if (Objects.equals(boundPlaylistId, playlistId)) {
                        cover.bind(null, data);
                    }
                }
                @Override public void onError(String message) { /* giữ placeholder */ }
            });
        }
    }

    // ---- Track list (START_LISTENING) ----
    class TrackListVH extends RecyclerView.ViewHolder {
        TextView title; RecyclerView rv;
        TrackListVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_section_title);
            rv = itemView.findViewById(R.id.rv_section_items);
        }
        void bind(HomeSection s) {
            title.setText(s.getTitle() != null ? s.getTitle() : "");
            List<Track> tracks = s.asTracks(RetrofitClient.GSON);
            rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rv.setAdapter(new HomeTrackRowAdapter(tracks, listener::onTrack));
        }
    }

    // ---- Generic horizontal section ----
    class SectionVH extends RecyclerView.ViewHolder {
        TextView title, more; RecyclerView rv;
        SectionVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_section_title);
            more = itemView.findViewById(R.id.tv_section_more);
            rv = itemView.findViewById(R.id.rv_section_items);
        }
        void bind(HomeSection s) {
            title.setText(s.getTitle() != null ? s.getTitle() : "");
            Context ctx = itemView.getContext();
            String kind = s.getKind();
            while (rv.getItemDecorationCount() > 0) rv.removeItemDecorationAt(0);
            more.setVisibility(View.GONE);
            rv.setNestedScrollingEnabled(true);
            if (HomeSection.RECENTLY_PLAYED.equals(kind)) {
                more.setVisibility(View.VISIBLE);
                rv.setLayoutManager(new GridLayoutManager(ctx, 2));
                rv.addItemDecoration(new RecentGridSpacing(ctx, 8));
                rv.setNestedScrollingEnabled(false);
                rv.setAdapter(new RecentTileAdapter(s.asTracks(RetrofitClient.GSON), listener::onTrack));
                return;
            }
            rv.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false));
            switch (kind == null ? "" : kind) {
                case HomeSection.RADIO:
                    rv.setAdapter(new RadioCardAdapter(s.asArtists(RetrofitClient.GSON), listener::onArtist));
                    break;
                case HomeSection.POPULAR_ARTISTS:
                    rv.setAdapter(new ArtistCircleAdapter(s.asArtists(RetrofitClient.GSON), listener::onArtist));
                    break;
                case HomeSection.NEW_RELEASES:
                case HomeSection.RECOMMENDED:
                    rv.setAdapter(new AlbumLabelAdapter(s.asAlbums(RetrofitClient.GSON), listener::onAlbum));
                    break;
                default:
                    // TOP_PICKS / MOOD_PLAYLIST / others -> playlist cards
                    rv.setAdapter(new PlaylistCardAdapter(s.asPlaylists(RetrofitClient.GSON), listener::onPlaylist));
                    break;
            }
        }
    }

    static class RecentGridSpacing extends RecyclerView.ItemDecoration {
        private final int gapPx;
        RecentGridSpacing(Context ctx, int gapDp) {
            this.gapPx = Math.round(gapDp * ctx.getResources().getDisplayMetrics().density);
        }
        @Override
        public void getItemOffsets(@NonNull Rect out, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;
            int col = pos % 2;
            int half = gapPx / 2;
            out.left = col == 0 ? 0 : half;
            out.right = col == 0 ? half : 0;
            out.bottom = gapPx;
        }
    }
}
