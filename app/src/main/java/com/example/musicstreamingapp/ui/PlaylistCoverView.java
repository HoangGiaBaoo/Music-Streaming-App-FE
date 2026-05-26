package com.example.musicstreamingapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

/**
 * Composite cover view for a playlist.
 * - coverUrl set → load that single image
 * - tracks ≥ 4 → 2×2 grid of first 4 track covers
 * - 1 ≤ tracks ≤ 3 → first track's cover as single
 * - empty → music-note placeholder
 */
public class PlaylistCoverView extends FrameLayout {

    private ImageView single;
    private GridLayout grid;
    private final ImageView[] quads = new ImageView[4];

    public PlaylistCoverView(Context ctx) { super(ctx); init(ctx); }
    public PlaylistCoverView(Context ctx, @Nullable AttributeSet a) { super(ctx, a); init(ctx); }
    public PlaylistCoverView(Context ctx, @Nullable AttributeSet a, int d) { super(ctx, a, d); init(ctx); }

    private void init(Context ctx) {
        LayoutInflater.from(ctx).inflate(R.layout.view_playlist_cover, this, true);
        single = findViewById(R.id.cover_single);
        grid = findViewById(R.id.cover_grid);
        quads[0] = findViewById(R.id.cover_q0);
        quads[1] = findViewById(R.id.cover_q1);
        quads[2] = findViewById(R.id.cover_q2);
        quads[3] = findViewById(R.id.cover_q3);
    }

    public void bind(@Nullable String coverUrl, @Nullable List<Track> tracks) {
        if (coverUrl != null && !coverUrl.isEmpty()) {
            showSingleUrl(coverUrl);
            return;
        }
        int n = tracks == null ? 0 : tracks.size();
        if (n >= 4) {
            showGrid(tracks);
        } else if (n > 0) {
            showSingleUrl(firstTrackCover(tracks));
        } else {
            showPlaceholder();
        }
    }

    private void showSingleUrl(@Nullable String url) {
        grid.setVisibility(View.GONE);
        single.setVisibility(View.VISIBLE);
        if (url == null || url.isEmpty()) {
            single.setImageResource(R.drawable.placeholder_gradient);
        } else {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + url)
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(single);
        }
    }

    private void showGrid(List<Track> tracks) {
        single.setVisibility(View.GONE);
        grid.setVisibility(View.VISIBLE);
        for (int i = 0; i < 4; i++) {
            String url = tracks.get(i).getCoverUrl();
            if (url == null || url.isEmpty()) {
                quads[i].setImageResource(R.drawable.placeholder_gradient);
            } else {
                Glide.with(this)
                    .load(RetrofitClient.BASE_MEDIA_URL + url)
                    .placeholder(R.drawable.placeholder_gradient)
                    .error(R.drawable.placeholder_gradient)
                    .centerCrop()
                    .into(quads[i]);
            }
        }
    }

    private void showPlaceholder() {
        grid.setVisibility(View.GONE);
        single.setVisibility(View.VISIBLE);
        single.setImageResource(R.drawable.placeholder_gradient);
    }

    @Nullable
    private static String firstTrackCover(List<Track> tracks) {
        for (Track t : tracks) {
            String url = t.getCoverUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }
}
