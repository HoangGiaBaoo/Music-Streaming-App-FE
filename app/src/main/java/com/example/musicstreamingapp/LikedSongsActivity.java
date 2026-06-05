package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.ActivityLikedSongsBinding;
import com.example.musicstreamingapp.fragment.AddToPlaylistBottomSheet;
import com.example.musicstreamingapp.fragment.TrackMenuBottomSheet;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.BottomNavHelper;
import com.example.musicstreamingapp.util.MiniPlayerController;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.example.musicstreamingapp.viewmodel.LikedTracksViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn chi tiết "Bài hát đã thích" — playlist ảo lấy dữ liệu từ GET /api/tracks/liked.
 * Mô phỏng {@link PlaylistDetailActivity} nhưng không có cover/edit (không phải playlist thật).
 */
public class LikedSongsActivity extends AppCompatActivity {

    private ActivityLikedSongsBinding b;
    private LikedTracksViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;

    private MiniPlayerController miniPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLikedSongsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        miniPlayer = new MiniPlayerController(this, b.miniPlayer);
        BottomNavHelper.setup(this, b.bottomNav);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b.collapsingToolbar.setTitle(getString(R.string.liked_songs_title));

        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        adapter.setMoreListener(this::showTrackMenu);
        b.rvTracks.setAdapter(adapter);

        b.btnPlayFab.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(this, tracks.get(0), tracks, 0);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });
        b.btnShuffle.setOnClickListener(v -> {
            PlayerManager pm = PlayerManager.getInstance();
            pm.setShuffleEnabled(!pm.isShuffleEnabled());
            renderShuffle();
        });
        renderShuffle();

        vm = new ViewModelProvider(this, new VmFactory(this)).get(LikedTracksViewModel.class);
        vm.tracks().observe(this, data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            renderSubtitle();
            renderEmptyState();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bỏ like ở Player/TrackMenu nơi khác → quay lại đây danh sách phải cập nhật.
        vm.refresh();
        renderShuffle();
        miniPlayer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        miniPlayer.onPause();
    }

    /** Nút shuffle chỉ bật/tắt chế độ trộn của PlayerManager: xanh = bật, xám = tắt. */
    private void renderShuffle() {
        boolean on = PlayerManager.getInstance().isShuffleEnabled();
        int color = ContextCompat.getColor(this,
            on ? R.color.spotify_green : R.color.text_secondary);
        b.btnShuffle.setColorFilter(color);
    }

    private void renderSubtitle() {
        String user = TokenManager.getUsername(this);
        b.tvSubtitle.setText(getString(R.string.liked_songs_owner_subtitle, user, tracks.size()));
    }

    private void renderEmptyState() {
        boolean empty = tracks.isEmpty();
        b.rowActions.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** Menu 3 chấm cho 1 bài đã thích. Không có "Xóa khỏi danh sách phát" (playlist ảo). */
    private void showTrackMenu(Track track, int position) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                // Bài đang ở danh sách đã thích → thao tác này bỏ thích; reload để bài biến mất.
                vm.toggleLike(t.getTrackId());
            }
            @Override public void onAddToPlaylist(Track t) {
                openAddToPlaylist(t);
            }
            @Override public void onGoToAlbum(Track t) {
                if (t.getAlbum() == null || t.getAlbum().getAlbumId() == null) return;
                Intent i = new Intent(LikedSongsActivity.this, AlbumDetailActivity.class);
                i.putExtra("albumId", t.getAlbum().getAlbumId());
                startActivity(i);
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
                Intent i = new Intent(LikedSongsActivity.this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", playlist.getPlaylistId());
                i.putExtra("playlistName", playlist.getName());
                startActivity(i);
            }
        });
        sheet.show(getSupportFragmentManager(), "add_to_playlist");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
