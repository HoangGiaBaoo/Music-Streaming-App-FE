package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.ArtistAlbumAdapter;
import com.example.musicstreamingapp.adapter.ArtistTrackAdapter;
import com.example.musicstreamingapp.databinding.ActivityArtistDetailBinding;
import com.example.musicstreamingapp.fragment.AddToPlaylistBottomSheet;
import com.example.musicstreamingapp.fragment.ShuffleGateBottomSheet;
import com.example.musicstreamingapp.fragment.TrackMenuBottomSheet;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.BottomNavHelper;
import com.example.musicstreamingapp.util.MiniPlayerController;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.PremiumChecker;
import com.example.musicstreamingapp.viewmodel.ArtistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtistDetailActivity extends AppCompatActivity {

    private ActivityArtistDetailBinding b;
    private ArtistDetailViewModel vm;

    private final List<Album> albums = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();
    private ArtistAlbumAdapter albumAdapter;
    private ArtistTrackAdapter trackAdapter;

    private SubscriptionViewModel subVm;
    private boolean isPremium = false;

    private MiniPlayerController miniPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityArtistDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        miniPlayer = new MiniPlayerController(this, b.miniPlayer);
        BottomNavHelper.setup(this, b.bottomNav);

        long artistId = getIntent().getLongExtra("artistId", -1);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        trackAdapter = new ArtistTrackAdapter(tracks, new ArtistTrackAdapter.Callbacks() {
            @Override public void onTrackClick(Track track) { openPlayer(track); }
            @Override public void onMoreClick(Track track, View anchor) { showTrackMenu(track); }
            @Override public void onInPlaylistClick(Track track) { openAddToPlaylist(track); }
        });
        b.rvTopTracks.setLayoutManager(new LinearLayoutManager(this));
        b.rvTopTracks.setAdapter(trackAdapter);

        albumAdapter = new ArtistAlbumAdapter(albums, album -> {
            Intent i = new Intent(this, AlbumDetailActivity.class);
            i.putExtra("albumId", album.getAlbumId());
            startActivity(i);
        });
        b.rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        b.rvAlbums.setAdapter(albumAdapter);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(ArtistDetailViewModel.class);
        vm.setArtistId(artistId);

        b.btnFollow.setOnClickListener(v -> vm.onFollowClicked());
        b.btnExpandTracks.setOnClickListener(v -> vm.toggleTracksExpanded());
        b.btnPlay.setOnClickListener(v -> playRespectingShuffle());
        b.btnShuffle.setOnClickListener(v -> onShuffleClicked());

        b.btnExpandBio.setOnClickListener(v -> {
            b.tvBio.setMaxLines(Integer.MAX_VALUE);
            b.btnExpandBio.setVisibility(View.GONE);
        });

        observeViewModel();
        vm.loadIfNeeded();

        // Premium mới bật/tắt trộn bài được; Free bị ép trộn (chạm shuffle để tắt → gate).
        subVm = new ViewModelProvider(this, new VmFactory(this)).get(SubscriptionViewModel.class);
        subVm.subscription().observe(this, sub -> {
            isPremium = PremiumChecker.isPremium(sub);
            if (!isPremium) PlayerManager.getInstance().setShuffleEnabled(true);
            updateShuffleUi();
        });
        subVm.loadCurrentSubscription();
        updateShuffleUi();
    }

    private void observeViewModel() {
        vm.artist().observe(this, this::renderArtist);
        vm.albums().observe(this, this::renderAlbums);
        vm.visibleTracks().observe(this, this::renderTracks);

        vm.following().observe(this, isFollowing -> {
            if (Boolean.TRUE.equals(isFollowing)) {
                b.btnFollow.setText(R.string.following_btn);
                b.btnFollow.setStrokeColorResource(R.color.spotify_green);
            } else {
                b.btnFollow.setText(R.string.follow);
                b.btnFollow.setStrokeColorResource(R.color.outline_grey);
            }
        });

        vm.showExpandButton().observe(this, show ->
            b.btnExpandTracks.setVisibility(show ? View.VISIBLE : View.GONE));

        vm.tracksExpanded().observe(this, expanded ->
            b.btnExpandTracks.setText(Boolean.TRUE.equals(expanded) ? "Ẩn bớt" : "Xem thêm"));

        vm.inPlaylistTrackIds().observe(this, ids ->
            trackAdapter.setInPlaylistIds(ids));

        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    private void renderArtist(Artist a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getName());

        if (a.getAvatarUrl() != null) {
            String url = RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl();
            Glide.with(this).load(url).centerCrop().into(b.ivArtistBanner);
            Glide.with(this).load(url).centerCrop().into(b.ivArtistThumb);
            Glide.with(this).load(url).centerCrop().into(b.ivAboutImage);
        }

        b.tvFollowers.setText(formatFollowers(a.getFollowerCount()));
        b.tvAboutName.setText(a.getName() != null ? a.getName() : "");
        b.tvAboutFollowers.setText(formatFollowers(a.getFollowerCount()));

        String bio = a.getBio();
        if (bio != null && !bio.isEmpty()) {
            b.cardAbout.setVisibility(View.VISIBLE);
            b.tvBio.setText(bio);
            b.tvBio.post(() -> {
                if (b.tvBio.getLineCount() > 3) b.btnExpandBio.setVisibility(View.VISIBLE);
            });
        }
    }

    private void renderAlbums(List<Album> data) {
        albums.clear();
        if (data != null) albums.addAll(data);
        albumAdapter.notifyDataSetChanged();
        b.tvShowAllAlbums.setVisibility(albums.size() > 3 ? View.VISIBLE : View.GONE);
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        trackAdapter.notifyDataSetChanged();
    }

    private void showTrackMenu(Track track) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                vm.likeTrack(t.getTrackId(), null);
                Snackbar.make(b.getRoot(), "Đã thêm vào Bài hát đã thích", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onAddToPlaylist(Track t) {
                openAddToPlaylist(t);
            }
            @Override public void onGoToAlbum(Track t) {
                if (t.getAlbum() == null || t.getAlbum().getAlbumId() == null) return;
                Intent i = new Intent(ArtistDetailActivity.this, AlbumDetailActivity.class);
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
            @Override public void onSaved(long trackId, boolean isInAnyPlaylist) {
                vm.updateTrackInPlaylist(trackId, isInAnyPlaylist);
            }
            @Override public void onPlaylistCreated(Playlist playlist) {
                if (track.getTrackId() != null) vm.updateTrackInPlaylist(track.getTrackId(), true);
                Intent i = new Intent(ArtistDetailActivity.this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", playlist.getPlaylistId());
                i.putExtra("playlistName", playlist.getName());
                startActivity(i);
            }
        });
        sheet.show(getSupportFragmentManager(), "add_to_playlist");
    }

    private void openPlayer(Track track) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("track", track);
        startActivity(i);
    }

    /** Nút Play: phát theo trạng thái trộn bài hiện tại (ngẫu nhiên nếu đang bật). Không gate. */
    private void playRespectingShuffle() {
        List<Track> all = vm.getAllTracks();
        if (all.isEmpty()) return;
        int start = PlayerManager.getInstance().isShuffleEnabled()
            ? (int) (Math.random() * all.size()) : 0;
        openPlayer(all.get(start));
    }

    /** Free: chạm shuffle (cố tắt) → gate Premium. Premium: bật/tắt trộn bài thật. */
    private void onShuffleClicked() {
        if (!isPremium) {
            new ShuffleGateBottomSheet().show(getSupportFragmentManager(), "shuffle_gate");
            return;
        }
        PlayerManager pm = PlayerManager.getInstance();
        pm.setShuffleEnabled(!pm.isShuffleEnabled());
        updateShuffleUi();
    }

    /** Nút trộn xanh khi bật, xám khi tắt (chỉ Premium mới tắt được). */
    private void updateShuffleUi() {
        boolean on = PlayerManager.getInstance().isShuffleEnabled();
        b.btnShuffle.setColorFilter(getColor(on ? R.color.spotify_green : R.color.text_secondary));
    }

    private static String formatFollowers(Long count) {
        if (count == null || count == 0) return "";
        if (count >= 1_000_000) {
            return String.format(Locale.US, "%.1f Tr người theo dõi", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format(Locale.US, "%.1f N người theo dõi", count / 1_000.0);
        }
        return count + " người theo dõi";
    }

    @Override
    protected void onResume() {
        super.onResume();
        miniPlayer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        miniPlayer.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
