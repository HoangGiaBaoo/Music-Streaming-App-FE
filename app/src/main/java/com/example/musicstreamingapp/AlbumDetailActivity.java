package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.ActivityAlbumDetailBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.BottomNavHelper;
import com.example.musicstreamingapp.util.MiniPlayerController;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.PremiumChecker;
import com.example.musicstreamingapp.util.ShuffleController;
import com.example.musicstreamingapp.viewmodel.AlbumDetailViewModel;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {

    private ActivityAlbumDetailBinding b;
    private AlbumDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;
    private boolean shimmerHidden = false;

    private SubscriptionViewModel subVm;
    private ShuffleController shuffle;

    private MiniPlayerController miniPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAlbumDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        miniPlayer = new MiniPlayerController(this, b.miniPlayer);
        BottomNavHelper.setup(this, b.bottomNav);

        long albumId = getIntent().getLongExtra("albumId", -1);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(this, track, tracks, pos);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        b.rvTracks.setAdapter(adapter);

        b.btnPlayAll.setOnClickListener(v -> playRespectingShuffle());
        b.btnShuffle.setOnClickListener(v -> shuffle.onShuffleClicked());

        b.shimmerAlbum.getRoot().startShimmer();
        b.shimmerAlbum.getRoot().postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(AlbumDetailViewModel.class);
        vm.setAlbumId(albumId);
        observeViewModel();
        vm.loadIfNeeded();

        // Premium mới bật/tắt trộn bài được; Free bị ép trộn (chạm shuffle để tắt → gate).
        subVm = new ViewModelProvider(this, new VmFactory(this)).get(SubscriptionViewModel.class);
        shuffle = new ShuffleController(getSupportFragmentManager(), on ->
            b.btnShuffle.setTextColor(getColor(on ? R.color.spotify_green : R.color.text_secondary)));
        subVm.subscription().observe(this, sub -> shuffle.setPremium(PremiumChecker.isPremium(sub)));
        subVm.loadCurrentSubscription();
    }

    /** Nút "Phát tất cả": phát theo trạng thái trộn bài (ngẫu nhiên nếu đang bật). Không gate. */
    private void playRespectingShuffle() {
        if (tracks.isEmpty()) return;
        int start = shuffle.startIndex(tracks.size());
        PlayerManager.getInstance().play(this, tracks.get(start), tracks, start);
        startActivity(new Intent(this, PlayerActivity.class));
    }

    private void observeViewModel() {
        vm.album().observe(this, this::renderAlbum);
        vm.tracks().observe(this, data -> {
            renderTracks(data);
            if (data != null) hideShimmer();
        });
        vm.errorEvent().observe(this, e -> e.consume(msg -> {
            hideShimmer();
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        }));
    }

    private void hideShimmer() {
        if (shimmerHidden) return;
        shimmerHidden = true;
        b.shimmerAlbum.getRoot().stopShimmer();
        b.shimmerAlbum.getRoot().setVisibility(View.GONE);
    }

    private void renderAlbum(Album a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getTitle());
        if (a.getCoverUrl() != null && !a.getCoverUrl().isEmpty()) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(b.ivCover);
        }
        if (a.getArtist() != null) b.tvArtistName.setText(a.getArtist().getName());
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        adapter.notifyDataSetChanged();
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
