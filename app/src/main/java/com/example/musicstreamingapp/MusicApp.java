package com.example.musicstreamingapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.AdManager;
import com.example.musicstreamingapp.util.PlayerManager;
import com.google.android.gms.ads.MobileAds;

/**
 * Application class:
 *  - Khởi tạo Google Mobile Ads SDK một lần khi app start.
 *  - Theo dõi Activity đang foreground để biết nơi hiển thị interstitial.
 *  - Gắn listener PHỤ vào PlayerManager để đếm bài (mọi màn), interstitial hiện trên Activity hiện tại.
 * Đăng ký trong AndroidManifest qua android:name=".MusicApp".
 */
public class MusicApp extends Application implements Application.ActivityLifecycleCallbacks {

    @Nullable private Activity currentActivity;

    private final PlayerManager.OnTrackChangeListener adTrackListener =
        new PlayerManager.OnTrackChangeListener() {
            @Override public void onTrackChanged(Track track) {
                // Luôn đếm; activity có thể null trong khe chuyển màn — AdManager sẽ hoãn việc hiện.
                AdManager.onTrackPlayed(currentActivity);
            }
            @Override public void onPlayStateChanged(boolean isPlaying) { /* không dùng cho ads */ }
        };

    @Override
    public void onCreate() {
        super.onCreate();
        MobileAds.initialize(this, initializationStatus -> {});
        registerActivityLifecycleCallbacks(this);
    }

    // --- ActivityLifecycleCallbacks ---

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        // Re-attach mỗi lần resume: PlayerManager singleton có thể bị release() khi logout → tạo lại.
        PlayerManager.getInstance().setAdListener(adTrackListener);
        // Xả interstitial bị hoãn do khe chuyển màn (vd phát bài từ trang nghệ sĩ → mở PlayerActivity).
        AdManager.maybeShowPending(activity);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (currentActivity == activity) currentActivity = null;
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}
