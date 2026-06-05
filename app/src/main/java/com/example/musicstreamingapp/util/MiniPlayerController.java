package com.example.musicstreamingapp.util;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.LayoutMiniPlayerBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;

/**
 * Mini-player dùng chung cho các màn Activity ngoài MainActivity (Playlist/Album/Artist…).
 * Poll trạng thái từ {@link PlayerManager} mỗi 500ms nên KHÔNG tranh chấp listener với
 * MainViewModel (PlayerManager chỉ có 1 listener slot). Activity chỉ cần include
 * layout_mini_player rồi gọi {@link #onResume()} / {@link #onPause()}.
 */
public class MiniPlayerController {

    private static final long TICK_MS = 500;

    private final AppCompatActivity activity;
    private final LayoutMiniPlayerBinding mini;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Long lastTrackId = null;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, TICK_MS);
        }
    };

    public MiniPlayerController(AppCompatActivity activity, LayoutMiniPlayerBinding mini) {
        this.activity = activity;
        this.mini = mini;

        mini.getRoot().setOnClickListener(v -> {
            if (PlayerManager.getInstance().getCurrentTrack() != null) {
                activity.startActivity(new Intent(activity, PlayerActivity.class));
            }
        });
        mini.btnMiniPlay.setOnClickListener(v -> {
            PlayerManager.getInstance().togglePlayPause();
            refresh();
        });
        mini.btnMiniCast.setOnClickListener(v -> { /* placeholder, giống MainActivity */ });
    }

    /** Gọi trong Activity.onResume(). */
    public void onResume() {
        refresh();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    /** Gọi trong Activity.onPause(). */
    public void onPause() {
        handler.removeCallbacks(tick);
    }

    private void refresh() {
        PlayerManager pm = PlayerManager.getInstance();
        Track track = pm.getCurrentTrack();
        if (track == null) {
            mini.getRoot().setVisibility(View.GONE);
            lastTrackId = null;
            return;
        }
        mini.getRoot().setVisibility(View.VISIBLE);

        // Chỉ cập nhật ảnh/tên khi đổi bài (tránh reload Glide mỗi 500ms).
        if (lastTrackId == null || !lastTrackId.equals(track.getTrackId())) {
            lastTrackId = track.getTrackId();
            mini.tvMiniTitle.setText(track.getTitle());
            mini.tvMiniTitle.setSelected(true); // bật marquee
            mini.tvMiniArtist.setText(track.getArtist() != null ? track.getArtist().getName() : "");
            Glide.with(activity)
                .load(track.getCoverUrl() != null ? RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl() : null)
                .placeholder(R.drawable.placeholder_gradient)
                .into(mini.ivMiniCover);
        }

        mini.btnMiniPlay.setImageResource(pm.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        long pos = pm.getCurrentPosition();
        long dur = pm.getDuration();
        mini.pbMiniProgress.setProgress(dur > 0 ? (int) ((pos * 100) / dur) : 0);
    }
}
