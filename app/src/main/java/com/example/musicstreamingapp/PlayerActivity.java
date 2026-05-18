package com.example.musicstreamingapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicstreamingapp.adapter.ExploreArtistAdapter;
import com.example.musicstreamingapp.databinding.ActivityPlayerBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.LrcParser;
import com.example.musicstreamingapp.util.LrcParser.LrcLine;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TimeUtil;
import com.example.musicstreamingapp.viewmodel.PlayerViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding b;
    private PlayerViewModel vm;

    private List<LrcLine> lrcLines = Collections.emptyList();
    private final Handler lyricsHandler = new Handler(Looper.getMainLooper());
    private final Runnable lyricsRunnable = new Runnable() {
        @Override
        public void run() {
            syncLyricsTeaser();
            lyricsHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.rvExploreArtist.setLayoutManager(new LinearLayoutManager(this,
            RecyclerView.HORIZONTAL, false));

        Track intentTrack = (Track) getIntent().getSerializableExtra("track");
        if (intentTrack != null) {
            Track current = PlayerManager.getInstance().getCurrentTrack();
            boolean alreadyPlaying = current != null
                && intentTrack.getTrackId() != null
                && intentTrack.getTrackId().equals(current.getTrackId());
            if (!alreadyPlaying) {
                PlayerManager.getInstance().play(this, intentTrack, new ArrayList<>(), 0);
            }
        }

        vm = new ViewModelProvider(this, new VmFactory(this)).get(PlayerViewModel.class);
        if (intentTrack != null) vm.recordPlay(intentTrack.getTrackId());

        wireControls();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLyricsSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lyricsHandler.removeCallbacks(lyricsRunnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.player_no_anim, R.anim.player_slide_down);
    }

    private void closePlayer() {
        finish();
        overridePendingTransition(R.anim.player_no_anim, R.anim.player_slide_down);
    }

    private void startLyricsSync() {
        lyricsHandler.removeCallbacks(lyricsRunnable);
        if (!lrcLines.isEmpty()) {
            lyricsHandler.post(lyricsRunnable);
        }
    }

    private void syncLyricsTeaser() {
        if (lrcLines.isEmpty()) return;
        long posMs = PlayerManager.getInstance().getCurrentPosition();
        int idx = LrcParser.findActiveIndex(lrcLines, posMs);
        if (idx >= 0) {
            b.tvLyricsTeaser.setText(lrcLines.get(idx).text);
        }
    }

    private void wireControls() {
        b.btnBack.setOnClickListener(v -> closePlayer());
        b.btnPlayPause.setOnClickListener(v -> vm.onPlayPauseClicked());
        b.btnNext.setOnClickListener(v -> vm.onNextClicked());
        b.btnPrev.setOnClickListener(v -> vm.onPrevClicked());
        b.btnLike.setOnClickListener(v -> vm.onLikeClicked());
        b.btnFollowArtist.setOnClickListener(v -> vm.onFollowArtistClicked());
        b.btnShowLyrics.setOnClickListener(v -> openLyrics());

        b.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) vm.onSeek(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
    }

    private void observeViewModel() {
        vm.currentTrack().observe(this, this::renderTrack);
        vm.playing().observe(this, this::renderPlayState);
        vm.following().observe(this, isFollowing -> b.btnFollowArtist.setText(
            Boolean.TRUE.equals(isFollowing) ? R.string.following_btn : R.string.follow));
        vm.progress().observe(this, this::renderProgress);
    }

    private void openLyrics() {
        Track t = PlayerManager.getInstance().getCurrentTrack();
        if (t == null || t.getLyrics() == null) return;
        Intent in = new Intent(this, LyricsActivity.class);
        in.putExtra("title", t.getTitle());
        in.putExtra("artist", t.getArtistName());
        in.putExtra("lyrics", t.getLyrics());
        startActivity(in);
    }

    private void renderTrack(Track track) {
        if (track == null) return;
        b.tvTitle.setText(track.getTitle());
        b.tvArtist.setText(track.getArtistName());
        b.tvCreditArtist.setText(track.getArtistName());
        b.tvTotalTime.setText(track.getDuration() != null
            ? TimeUtil.formatSeconds(track.getDuration()) : "--:--");
        b.tvExploreArtistTitle.setText(getString(R.string.player_explore_artist, track.getArtistName()));

        if (track.getAlbum() != null && track.getAlbum().getTitle() != null) {
            b.tvBreadcrumbTop.setText("Đang phát từ album");
            b.tvBreadcrumbBottom.setText(track.getAlbum().getTitle().toUpperCase());
        } else if (track.getArtist() != null && track.getArtist().getName() != null) {
            b.tvBreadcrumbTop.setText("Đang phát từ nghệ sĩ");
            b.tvBreadcrumbBottom.setText(track.getArtist().getName().toUpperCase());
        } else {
            b.tvBreadcrumbTop.setText("");
            b.tvBreadcrumbBottom.setText("");
        }

        String rawLyrics = track.getLyrics();
        if (rawLyrics != null && !rawLyrics.isEmpty()) {
            lrcLines = LrcParser.parse(rawLyrics);

            // teaser: hiện dòng đầu (handler sẽ cập nhật real-time)
            if (!lrcLines.isEmpty()) {
                b.tvLyricsTeaser.setText(lrcLines.get(0).text);
                b.tvLyricsTeaser.setVisibility(View.VISIBLE);
            }

            // lyrics card: text sạch không có timestamp
            String cleanText = LrcParser.toCleanText(lrcLines);
            b.tvLyricsCard.setText(cleanText.isEmpty() ? rawLyrics : cleanText);

            startLyricsSync();
        } else {
            lrcLines = Collections.emptyList();
            lyricsHandler.removeCallbacks(lyricsRunnable);
            b.tvLyricsTeaser.setVisibility(View.GONE);
            b.tvLyricsCard.setText("Chưa có lời bài hát");
        }

        if (track.getCoverUrl() != null) {
            String url = RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl();
            Glide.with(this).load(url)
                .placeholder(R.drawable.placeholder_gradient)
                .centerCrop().into(b.ivCover);
            // Extract dominant color → set as background gradient (Spotify-style)
            Glide.with(this).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> t) {
                    applyPaletteGradient(resource);
                }
                @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) { }
            });
        } else {
            b.ivCover.setImageResource(R.drawable.placeholder_gradient);
            b.ivBgBlur.setImageDrawable(null);
        }

        if (track.getArtist() != null && track.getArtist().getAvatarUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + track.getArtist().getAvatarUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(b.ivCreditAvatar);
        } else {
            b.ivCreditAvatar.setImageResource(R.drawable.placeholder_gradient);
        }

        renderExploreCards(track);
    }

    private void renderExploreCards(Track track) {
        List<ExploreArtistAdapter.Card> cards = new ArrayList<>();
        String artistName = track.getArtistName();
        if (track.getArtist() != null && track.getArtist().getArtistId() != null) {
            cards.add(new ExploreArtistAdapter.Card(
                "Các bài hát của " + artistName, track.getCoverUrl(), () -> {}));
            cards.add(new ExploreArtistAdapter.Card(
                "Tương tự như " + artistName, track.getCoverUrl(), () -> {}));
        }
        cards.add(new ExploreArtistAdapter.Card(
            "Tương tự như " + track.getTitle(), track.getCoverUrl(), () -> {}));
        b.rvExploreArtist.setAdapter(new ExploreArtistAdapter(cards));
    }

    private void renderPlayState(Boolean isPlaying) {
        boolean playing = Boolean.TRUE.equals(isPlaying);
        b.btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        animateCover(playing);
    }

    /**
     * Set FrameLayout background to a vertical gradient from the cover's dominant
     * color (top) → spotify_black (bottom). Falls back to solid black if Palette
     * can't extract a meaningful color.
     */
    private void applyPaletteGradient(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;
            int top = palette.getDarkVibrantColor(
                palette.getDarkMutedColor(0xFF1E1E1E));
            int bottom = 0xFF121212; // spotify_black
            GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ top, bottom });
            b.ivBgBlur.setImageDrawable(gd);
        });
    }

    /**
     * Spotify-style cover scale: 0.85 (paused/idle) ↔ 1.0 (playing) với
     * OvershootInterpolator nhẹ.
     */
    private void animateCover(boolean playing) {
        float target = playing ? 1.0f : 0.85f;
        ObjectAnimator sx = ObjectAnimator.ofFloat(b.ivCover, "scaleX", b.ivCover.getScaleX(), target);
        ObjectAnimator sy = ObjectAnimator.ofFloat(b.ivCover, "scaleY", b.ivCover.getScaleY(), target);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.setDuration(300);
        set.setInterpolator(new OvershootInterpolator(1.4f));
        set.start();
    }

    private void renderProgress(long[] posDur) {
        if (posDur == null || posDur.length < 2) return;
        long pos = posDur[0];
        long dur = posDur[1];
        if (dur <= 0) return;
        b.seekbar.setMax((int) dur);
        b.seekbar.setProgress((int) pos);
        b.tvCurrentTime.setText(TimeUtil.formatMs(pos));
        b.tvTotalTime.setText("-" + TimeUtil.formatMs(dur - pos));
    }
}
