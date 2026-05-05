package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.ExploreArtistAdapter;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TimeUtil;
import com.example.musicstreamingapp.util.TokenManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.wasabeef.glide.transformations.BlurTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity implements PlayerManager.OnTrackChangeListener {
    private ImageView ivBg, ivCover, ivCreditAvatar;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvLyricsTeaser, tvLyricsCard,
        tvExploreArtistTitle, tvCreditArtist, tvBreadcrumbBottom;
    private ImageButton btnPlayPause, btnNext, btnPrev, btnLike, btnBack;
    private Button btnFollowArtist, btnShowLyrics;
    private SeekBar seekBar;
    private RecyclerView rvExploreArtist;
    private ApiService api;
    private boolean isLiked = false, isFollowing = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));

        ivBg = findViewById(R.id.iv_bg_blur);
        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tvLyricsTeaser = findViewById(R.id.tv_lyrics_teaser);
        tvLyricsCard = findViewById(R.id.tv_lyrics_card);
        tvExploreArtistTitle = findViewById(R.id.tv_explore_artist_title);
        tvCreditArtist = findViewById(R.id.tv_credit_artist);
        tvBreadcrumbBottom = findViewById(R.id.tv_breadcrumb_bottom);
        seekBar = findViewById(R.id.seekbar);
        btnBack = findViewById(R.id.btn_back);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);
        btnLike = findViewById(R.id.btn_like);
        btnFollowArtist = findViewById(R.id.btn_follow_artist);
        btnShowLyrics = findViewById(R.id.btn_show_lyrics);
        rvExploreArtist = findViewById(R.id.rv_explore_artist);
        ivCreditAvatar = findViewById(R.id.iv_credit_avatar);

        rvExploreArtist.setLayoutManager(new LinearLayoutManager(this,
            RecyclerView.HORIZONTAL, false));

        Track track = (Track) getIntent().getSerializableExtra("track");
        if (track != null) {
            PlayerManager.getInstance().play(this, track, new ArrayList<>(), 0);
            recordHistory(track.getTrackId());
        }

        PlayerManager.getInstance().setListener(this);

        btnBack.setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> {
            PlayerManager.getInstance().togglePlayPause();
            updatePlayButton();
        });
        btnNext.setOnClickListener(v -> PlayerManager.getInstance().playNext());
        btnPrev.setOnClickListener(v -> PlayerManager.getInstance().playPrevious());
        btnLike.setOnClickListener(v -> toggleLike());
        btnFollowArtist.setOnClickListener(v -> toggleFollowArtist());
        btnShowLyrics.setOnClickListener(v -> {
            Track t = PlayerManager.getInstance().getCurrentTrack();
            if (t != null && t.getLyrics() != null) {
                Intent in = new Intent(this, LyricsActivity.class);
                in.putExtra("title", t.getTitle());
                in.putExtra("artist", t.getArtistName());
                in.putExtra("lyrics", t.getLyrics());
                startActivity(in);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) PlayerManager.getInstance().seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        updateUI(PlayerManager.getInstance().getCurrentTrack());
        startProgressUpdater();
    }

    private void updateUI(Track track) {
        if (track == null) return;
        tvTitle.setText(track.getTitle());
        tvArtist.setText(track.getArtistName());
        tvCreditArtist.setText(track.getArtistName());
        tvTotalTime.setText(track.getDuration() != null
            ? TimeUtil.formatSeconds(track.getDuration()) : "--:--");
        tvExploreArtistTitle.setText(getString(R.string.player_explore_artist, track.getArtistName()));

        if (track.getAlbum() != null && track.getAlbum().getTitle() != null) {
            tvBreadcrumbBottom.setText(track.getAlbum().getTitle().toUpperCase());
        }

        if (track.getLyrics() != null && !track.getLyrics().isEmpty()) {
            tvLyricsTeaser.setText(track.firstLyricLine());
            tvLyricsTeaser.setVisibility(View.VISIBLE);
            tvLyricsCard.setText(track.getLyrics());
        } else {
            tvLyricsTeaser.setVisibility(View.GONE);
            tvLyricsCard.setText("Chưa có lời bài hát");
        }

        if (track.getCoverUrl() != null) {
            String url = RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl();
            Glide.with(this).load(url)
                .placeholder(R.drawable.placeholder_gradient)
                .centerCrop().into(ivCover);
            Glide.with(this).load(url)
                .transform(new BlurTransformation(25, 3))
                .into(ivBg);
        } else {
            ivCover.setImageResource(R.drawable.placeholder_gradient);
            ivBg.setImageResource(R.drawable.placeholder_gradient);
        }

        if (track.getArtist() != null && track.getArtist().getAvatarUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + track.getArtist().getAvatarUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(ivCreditAvatar);
        } else {
            ivCreditAvatar.setImageResource(R.drawable.placeholder_gradient);
        }

        loadExploreArtist(track);
        updatePlayButton();
    }

    private void loadExploreArtist(Track track) {
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
        rvExploreArtist.setAdapter(new ExploreArtistAdapter(cards));
    }

    private void updatePlayButton() {
        btnPlayPause.setImageResource(
            PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void startProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override public void run() {
                long pos = PlayerManager.getInstance().getCurrentPosition();
                long dur = PlayerManager.getInstance().getDuration();
                if (dur > 0) {
                    seekBar.setMax((int) dur);
                    seekBar.setProgress((int) pos);
                    tvCurrentTime.setText(TimeUtil.formatMs(pos));
                    long remaining = dur - pos;
                    tvTotalTime.setText("-" + TimeUtil.formatMs(remaining));
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressUpdater);
    }

    private void toggleLike() {
        Track current = PlayerManager.getInstance().getCurrentTrack();
        if (current == null) return;
        isLiked = !isLiked;
        api.toggleLike(current.getTrackId()).enqueue(emptyCb());
    }

    private void toggleFollowArtist() {
        Track current = PlayerManager.getInstance().getCurrentTrack();
        if (current == null || current.getArtist() == null) return;
        isFollowing = !isFollowing;
        btnFollowArtist.setText(isFollowing ? R.string.following_btn : R.string.follow);
        api.toggleFollow(current.getArtist().getArtistId()).enqueue(emptyCb());
    }

    private Callback<Map<String, String>> emptyCb() {
        return new Callback<Map<String, String>>() {
            @Override public void onResponse(@NonNull Call<Map<String, String>> call,
                                             @NonNull Response<Map<String, String>> response) {}
            @Override public void onFailure(@NonNull Call<Map<String, String>> call,
                                            @NonNull Throwable t) {}
        };
    }

    private void recordHistory(Long trackId) {
        api.recordPlay(trackId).enqueue(emptyCb());
    }

    @Override public void onTrackChanged(Track track) {
        runOnUiThread(() -> updateUI(track));
    }

    @Override public void onPlayStateChanged(boolean isPlaying) {
        runOnUiThread(this::updatePlayButton);
    }

    @Override protected void onResume() {
        super.onResume();
        PlayerManager.getInstance().setListener(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (progressUpdater != null) handler.removeCallbacks(progressUpdater);
        PlayerManager.getInstance().setListener(null);
    }
}
