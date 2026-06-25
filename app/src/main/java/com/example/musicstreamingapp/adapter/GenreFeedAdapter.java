package com.example.musicstreamingapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.util.NavHelper;
import com.example.musicstreamingapp.GenreDetailActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.GenreSectionDto;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.ItemAnim;
import com.example.musicstreamingapp.util.PlayerManager;

import java.util.List;

/**
 * Vertical feed của Genre Detail. Mỗi section là 1 item (header + RecyclerView
 * ngang). {@link #getItemViewType(int)} dựa vào {@code kind} để chọn adapter con
 * cho hàng ngang. Navigation xử lý ngay trong ViewHolder qua context của item.
 */
public class GenreFeedAdapter extends RecyclerView.Adapter<GenreFeedAdapter.SectionVH> {

    private static final int VT_OTHER             = 0;
    private static final int VT_POPULAR_PLAYLISTS = 1;
    private static final int VT_NEW_RELEASES      = 2;
    private static final int VT_POPULAR_TRACKS    = 3;
    private static final int VT_POPULAR_ARTISTS   = 4;
    private static final int VT_RELATED_GENRES    = 5;

    private final List<GenreSectionDto> sections;
    private final int[] lastPosition = {-1};

    public GenreFeedAdapter(List<GenreSectionDto> sections) {
        this.sections = sections;
    }

    @Override
    public int getItemViewType(int position) {
        String kind = sections.get(position).getKind();
        if (kind == null) return VT_OTHER;
        switch (kind) {
            case GenreSectionDto.POPULAR_PLAYLISTS: return VT_POPULAR_PLAYLISTS;
            case GenreSectionDto.NEW_RELEASES:      return VT_NEW_RELEASES;
            case GenreSectionDto.POPULAR_TRACKS:    return VT_POPULAR_TRACKS;
            case GenreSectionDto.POPULAR_ARTISTS:   return VT_POPULAR_ARTISTS;
            case GenreSectionDto.RELATED_GENRES:    return VT_RELATED_GENRES;
            default:                                return VT_OTHER;
        }
    }

    @NonNull @Override
    public SectionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SectionVH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_genre_section, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SectionVH h, int position) {
        h.bind(sections.get(position));
        ItemAnim.animate(h.itemView, position, lastPosition);
    }

    @Override public int getItemCount() { return sections.size(); }

    static class SectionVH extends RecyclerView.ViewHolder {
        final TextView title;
        final RecyclerView rv;

        SectionVH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_section_title);
            rv = v.findViewById(R.id.rv_section_items);
        }

        void bind(GenreSectionDto s) {
            Context ctx = itemView.getContext();
            title.setText(s.getTitle() != null ? s.getTitle() : "");
            rv.setLayoutManager(new LinearLayoutManager(ctx, RecyclerView.HORIZONTAL, false));
            String kind = s.getKind() == null ? "" : s.getKind();

            switch (kind) {
                case GenreSectionDto.POPULAR_PLAYLISTS: {
                    List<Playlist> playlists = s.asPlaylists(RetrofitClient.GSON);
                    rv.setAdapter(new PlaylistCardAdapter(playlists, p -> openPlaylist(ctx, p)));
                    break;
                }
                case GenreSectionDto.NEW_RELEASES: {
                    List<Album> albums = s.asAlbums(RetrofitClient.GSON);
                    rv.setAdapter(new AlbumCardAdapter(albums, a -> openAlbum(ctx, a)));
                    break;
                }
                case GenreSectionDto.POPULAR_TRACKS: {
                    List<Track> tracks = s.asTracks(RetrofitClient.GSON);
                    rv.setAdapter(new TrackCardAdapter(tracks,
                        (track, pos) -> playTrack(ctx, track, tracks, pos)));
                    break;
                }
                case GenreSectionDto.POPULAR_ARTISTS: {
                    List<Artist> artists = s.asArtists(RetrofitClient.GSON);
                    rv.setAdapter(new ArtistCircleAdapter(artists, a -> openArtist(ctx, a)));
                    break;
                }
                case GenreSectionDto.RELATED_GENRES: {
                    List<Genre> genres = s.asGenres(RetrofitClient.GSON);
                    rv.setAdapter(new GenreTileSmallAdapter(genres, g -> openGenre(ctx, g)));
                    break;
                }
                default:
                    rv.setAdapter(null);
                    break;
            }
        }

        // ---- navigation (Bước 7) — dùng đúng key Intent của các Activity đích ----

        private void openPlaylist(Context ctx, Playlist p) {
            if (p == null || p.getPlaylistId() == null) return;
            NavHelper.openPlaylist(ctx, p.getPlaylistId(), p.getName());
        }

        private void openAlbum(Context ctx, Album a) {
            if (a == null || a.getAlbumId() == null) return;
            NavHelper.openAlbum(ctx, a.getAlbumId());
        }

        private void openArtist(Context ctx, Artist a) {
            if (a == null || a.getArtistId() == null) return;
            NavHelper.openArtist(ctx, a.getArtistId());
        }

        private void openGenre(Context ctx, Genre g) {
            if (g == null || g.getGenreId() == null) return;
            Intent i = new Intent(ctx, GenreDetailActivity.class);
            i.putExtra(GenreDetailActivity.EXTRA_GENRE_ID, g.getGenreId());
            i.putExtra(GenreDetailActivity.EXTRA_GENRE_NAME, g.getName());
            ctx.startActivity(i);
        }

        private void playTrack(Context ctx, Track track, List<Track> queue, int position) {
            PlayerManager.getInstance().play(ctx, track, queue, position);
            ctx.startActivity(new Intent(ctx, PlayerActivity.class));
        }
    }
}
