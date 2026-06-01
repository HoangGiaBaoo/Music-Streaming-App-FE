package com.example.musicstreamingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicstreamingapp.adapter.SuggestedTrackAdapter;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.ActivityPlaylistDetailBinding;
import com.example.musicstreamingapp.fragment.AddToPlaylistBottomSheet;
import com.example.musicstreamingapp.fragment.PlaylistEditBottomSheet;
import com.example.musicstreamingapp.fragment.TrackMenuBottomSheet;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.viewmodel.PlaylistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private ActivityPlaylistDetailBinding b;
    private PlaylistDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private final List<Track> suggestions = new ArrayList<>();
    private TrackDetailAdapter adapter;
    private SuggestedTrackAdapter suggestionAdapter;
    private boolean shimmerHidden = false;

    @Nullable private Playlist currentPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPlaylistDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long playlistId = getIntent().getLongExtra("playlistId", -1);
        String name = getIntent().getStringExtra("playlistName");

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (name != null) b.collapsingToolbar.setTitle(name);

        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        adapter.setMoreListener((track, pos) -> showTrackMenu(track, pos));
        b.rvTracks.setAdapter(adapter);

        b.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        suggestionAdapter = new SuggestedTrackAdapter(suggestions,
            (track, pos) -> vm.addSuggestion(track));
        b.rvSuggestions.setAdapter(suggestionAdapter);

        b.btnPlayFab.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });
        b.btnShuffle.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                int pos = (int) (Math.random() * tracks.size());
                PlayerManager.getInstance().play(this, tracks.get(pos), tracks, pos);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        b.coverView.setOnClickListener(v -> openCoverPicker());
        b.btnEditDetails.setOnClickListener(v -> openEditSheet());

        b.shimmerPlaylist.getRoot().startShimmer();
        b.shimmerPlaylist.getRoot().postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(PlaylistDetailViewModel.class);
        vm.setPlaylistId(playlistId);
        observeViewModel();
        vm.loadIfNeeded();
    }

    private void observeViewModel() {
        vm.playlist().observe(this, this::renderPlaylist);
        vm.tracks().observe(this, data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            renderCover();
            renderEmptyState();
            if (data != null) hideShimmer();
        });
        vm.suggestions().observe(this, data -> {
            suggestions.clear();
            if (data != null) suggestions.addAll(data);
            suggestionAdapter.notifyDataSetChanged();
        });
        vm.errorEvent().observe(this, e -> e.consume(msg -> {
            hideShimmer();
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        }));
        vm.editResult().observe(this, e -> e.consume(this::onEditResult));
    }

    private void onEditResult(PlaylistDetailViewModel.EditResult result) {
        switch (result) {
            case UPDATED:
                Snackbar.make(b.getRoot(), R.string.playlist_updated, Snackbar.LENGTH_SHORT).show();
                break;
            case DELETED:
                finish();
                break;
            case UPDATE_FAILED:
            case DELETE_FAILED:
                Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // After returning from the cover picker, refresh to pick up new coverUrl.
        if (currentPlaylist != null) vm.reload();
    }

    private void renderEmptyState() {
        boolean empty = tracks.isEmpty();
        b.rowActions.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) vm.loadSuggestionsIfNeeded();
    }

    private void renderPlaylist(Playlist p) {
        if (p == null) return;
        currentPlaylist = p;
        if (p.getName() != null) b.collapsingToolbar.setTitle(p.getName());
        renderCover();
    }

    private void renderCover() {
        String coverUrl = currentPlaylist != null ? currentPlaylist.getCoverUrl() : null;
        b.coverView.bind(coverUrl, tracks);
        applyPaletteGradient();
    }

    /**
     * Extract the dominant color of the playlist cover (or first track cover) and apply
     * it as a top→bottom gradient on the app bar, matching the Player screen's behavior.
     * If no usable cover is available, the explicit dark background in XML stays.
     */
    private void applyPaletteGradient() {
        String url = pickPaletteUrl();
        if (url == null) return;
        Glide.with(this).asBitmap()
            .load(RetrofitClient.BASE_MEDIA_URL + url)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource,
                                            @Nullable Transition<? super Bitmap> t) {
                    Palette.from(resource).generate(palette -> {
                        if (palette == null) return;
                        int top = palette.getDarkVibrantColor(
                            palette.getDarkMutedColor(0xFF1E1E1E));
                        int bottom = 0xFF121212;
                        GradientDrawable gd = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{top, bottom});
                        b.appBar.setBackground(gd);
                    });
                }
                @Override
                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
            });
    }

    @Nullable
    private String pickPaletteUrl() {
        if (currentPlaylist != null) {
            String url = currentPlaylist.getCoverUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        for (Track t : tracks) {
            String url = t.getCoverUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }

    /** Opens the cover picker. Called from cover tap or the edit bottom sheet's cover tap. */
    public void openCoverPicker() {
        if (currentPlaylist == null) return;
        Intent i = new Intent(this, PlaylistCoverPickerActivity.class);
        i.putExtra("playlistId", currentPlaylist.getPlaylistId());
        startActivity(i);
    }

    /** Opens the name/privacy edit bottom sheet. Called from the "Thay đổi" button. */
    public void openEditSheet() {
        if (currentPlaylist == null) return;
        PlaylistEditBottomSheet.newInstance(currentPlaylist)
            .show(getSupportFragmentManager(), "edit_playlist");
    }

    /** Mở menu 3 chấm cho 1 bài hát trong playlist (kèm tuỳ chọn "Xóa khỏi danh sách phát này"). */
    private void showTrackMenu(Track track, int position) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track, true);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                vm.likeTrack(t.getTrackId());
                Snackbar.make(b.getRoot(), "Đã thêm vào Bài hát đã thích", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onAddToPlaylist(Track t) {
                openAddToPlaylist(t);
            }
            @Override public void onGoToAlbum(Track t) {
                if (t.getAlbum() == null || t.getAlbum().getAlbumId() == null) return;
                Intent i = new Intent(PlaylistDetailActivity.this, AlbumDetailActivity.class);
                i.putExtra("albumId", t.getAlbum().getAlbumId());
                startActivity(i);
            }
            @Override public void onRemoveFromPlaylist(Track t) {
                vm.onRemoveTrack(t, position);
            }
        });
        sheet.show(getSupportFragmentManager(), "track_menu");
    }

    private void openAddToPlaylist(Track track) {
        if (track.getTrackId() == null) return;
        AddToPlaylistBottomSheet sheet = AddToPlaylistBottomSheet.newInstance(
            track.getTrackId(), track.getTitle());
        sheet.setCallback(new AddToPlaylistBottomSheet.Callback() {
            @Override public void onSaved(long trackId, boolean isInAnyPlaylist) { /* no-op */ }
            @Override public void onPlaylistCreated(Playlist playlist) {
                Intent i = new Intent(PlaylistDetailActivity.this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", playlist.getPlaylistId());
                i.putExtra("playlistName", playlist.getName());
                startActivity(i);
            }
        });
        sheet.show(getSupportFragmentManager(), "add_to_playlist");
    }

    private void hideShimmer() {
        if (shimmerHidden) return;
        shimmerHidden = true;
        b.shimmerPlaylist.getRoot().stopShimmer();
        b.shimmerPlaylist.getRoot().setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
