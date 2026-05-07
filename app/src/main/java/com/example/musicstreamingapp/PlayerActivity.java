package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.ExploreArtistAdapter;
import com.example.musicstreamingapp.databinding.ActivityPlayerBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TimeUtil;
import com.example.musicstreamingapp.viewmodel.PlayerViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding b;
    private PlayerViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.rvExploreArtist.setLayoutManager(new LinearLayoutManager(this,
            RecyclerView.HORIZONTAL, false));

        Track intentTrack = (Track) getIntent().getSerializableExtra("track");
        if (intentTrack != null) {
            PlayerManager.getInstance().play(this, intentTrack, new ArrayList<>(), 0);
        }

        vm = new ViewModelProvider(this, new VmFactory(this)).get(PlayerViewModel.class);
        if (intentTrack != null) vm.recordPlay(intentTrack.getTrackId());

        wireControls();
        observeViewModel();
    }

    private void wireControls() {
        b.btnBack.setOnClickListener(v -> finish());
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
            b.tvBreadcrumbBottom.setText(track.getAlbum().getTitle().toUpperCase());
        }

        if (track.getLyrics() != null && !track.getLyrics().isEmpty()) {
            b.tvLyricsTeaser.setText(track.firstLyricLine());
            b.tvLyricsTeaser.setVisibility(View.VISIBLE);
            b.tvLyricsCard.setText(track.getLyrics());
        } else {
            b.tvLyricsTeaser.setVisibility(View.GONE);
            b.tvLyricsCard.setText("Chưa có lời bài hát");
        }

        if (track.getCoverUrl() != null) {
            String url = RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl();
            Glide.with(this).load(url)
                .placeholder(R.drawable.placeholder_gradient)
                .centerCrop().into(b.ivCover);
            Glide.with(this).load(url)
                .transform(new BlurTransformation(25, 3))
                .into(b.ivBgBlur);
        } else {
            b.ivCover.setImageResource(R.drawable.placeholder_gradient);
            b.ivBgBlur.setImageResource(R.drawable.placeholder_gradient);
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
        b.btnPlayPause.setImageResource(
            Boolean.TRUE.equals(isPlaying) ? R.drawable.ic_pause : R.drawable.ic_play);
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
