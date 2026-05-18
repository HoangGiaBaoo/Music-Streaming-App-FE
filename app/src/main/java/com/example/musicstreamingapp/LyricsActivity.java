package com.example.musicstreamingapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;

import com.example.musicstreamingapp.adapter.LyricLineAdapter;
import com.example.musicstreamingapp.databinding.ActivityLyricsBinding;
import com.example.musicstreamingapp.util.LrcParser;
import com.example.musicstreamingapp.util.LrcParser.LrcLine;
import com.example.musicstreamingapp.util.PlayerManager;

import java.util.List;

public class LyricsActivity extends AppCompatActivity {

    private ActivityLyricsBinding b;
    private LinearLayoutManager layoutManager;
    private LyricLineAdapter adapter;
    private List<LrcLine> lines;
    private boolean isLrc;
    private int lastScrollIdx = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            syncPosition();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLyricsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String rawLyrics = getIntent().getStringExtra("lyrics");

        b.tvLyricsTitle.setText(title != null ? title : "");
        b.tvLyricsArtist.setText(artist != null ? artist : "");
        b.btnLyricsBack.setOnClickListener(v -> finish());

        lines = LrcParser.parse(rawLyrics);
        isLrc = LrcParser.isLrc(rawLyrics);

        layoutManager = new LinearLayoutManager(this);
        b.rvLyrics.setLayoutManager(layoutManager);

        b.rvLyrics.setItemAnimator(null);

        adapter = new LyricLineAdapter(lines);
        b.rvLyrics.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLrc) {
            handler.post(syncRunnable);
        } else {
            adapter.setActiveIndex(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(syncRunnable);
    }

    private void syncPosition() {
        long posMs = PlayerManager.getInstance().getCurrentPosition();
        int idx = LrcParser.findActiveIndex(lines, posMs);
        if (idx < 0) return;
        adapter.setActiveIndex(idx);
        if (idx != lastScrollIdx) {
            lastScrollIdx = idx;
            scrollToCenter(idx);
        }
    }

    private void scrollToCenter(int position) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(this) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd,
                                        int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2)
                    - (viewStart + (viewEnd - viewStart) / 2);
            }
        };
        scroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(scroller);
    }
}
