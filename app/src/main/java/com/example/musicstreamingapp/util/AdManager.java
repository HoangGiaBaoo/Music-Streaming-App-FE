package com.example.musicstreamingapp.util;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

// PlayerManager cùng package util — không cần import.

/**
 * Quản lý quảng cáo AdMob (test IDs của Google — không tính tiền thật).
 * Banner do layout tự khai báo adUnitId; lớp này lo interstitial + đếm số bài cho user Free.
 *
 * Trạng thái Premium được set từ MainActivity/PlayerActivity (qua {@link #setPremium(boolean)}),
 * còn việc đếm bài do listener phụ của PlayerManager kích hoạt (xem MusicApp).
 */
public final class AdManager {

    /** Test Banner ID — dùng trong layout activity_main.xml. */
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    /** Test Interstitial ID. */
    private static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    /** Cứ mỗi 3 bài (user Free) hiện 1 interstitial. */
    private static final int TRACKS_PER_AD = 3;

    private static InterstitialAd interstitialAd;
    private static int trackCount = 0;
    private static volatile boolean premium = false;
    /** Đã đủ ngưỡng 3 bài nhưng chưa kịp hiện (chưa có Activity foreground / ad chưa load). */
    private static boolean pendingShow = false;
    /** Nhạc đang phát lúc ad hiện → phát lại sau khi đóng ad. */
    private static boolean resumeMusicAfterAd = false;

    private AdManager() {}

    /** Cập nhật trạng thái Premium (gọi khi load /me xong). */
    public static void setPremium(boolean isPremium) {
        premium = isPremium;
    }

    public static boolean isPremium() {
        return premium;
    }

    /** Load sẵn 1 interstitial để lúc cần show ngay. */
    public static void loadInterstitial(Context ctx) {
        InterstitialAd.load(ctx.getApplicationContext(), INTERSTITIAL_ID,
            new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                }
                @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    interstitialAd = null;
                }
            });
    }

    /**
     * Gọi mỗi khi user phát một bài mới (từ listener phụ của PlayerManager).
     * LUÔN đếm (kể cả khi activity tạm null lúc chuyển màn) — Premium thì bỏ qua.
     * Đủ ngưỡng thì đánh dấu pendingShow rồi cố hiện ngay (nếu được).
     *
     * @param activity Activity foreground hiện tại, có thể null trong lúc chuyển màn.
     */
    public static void onTrackPlayed(@Nullable Activity activity) {
        if (premium) return;
        trackCount++;
        if (trackCount % TRACKS_PER_AD == 0) {
            pendingShow = true;
        }
        maybeShowPending(activity);
    }

    /**
     * Thử hiện interstitial đang chờ. Gọi lại mỗi khi có Activity resume (xem MusicApp) để
     * "xả" lần hiện bị hoãn vì khe chuyển màn (vd phát bài từ trang nghệ sĩ → mở PlayerActivity).
     */
    public static void maybeShowPending(@Nullable Activity activity) {
        if (premium || !pendingShow || activity == null) return;
        if (interstitialAd == null) {
            loadInterstitial(activity); // chưa có ad → nạp, lần resume sau sẽ thử lại
            return;
        }
        pendingShow = false;
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdShowedFullScreenContent() {
                // Tạm dừng nhạc trong lúc xem quảng cáo (tránh chồng tiếng).
                if (PlayerManager.getInstance().isPlaying()) {
                    resumeMusicAfterAd = true;
                    PlayerManager.getInstance().pause();
                }
            }
            @Override public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                if (resumeMusicAfterAd) {
                    PlayerManager.getInstance().resume();
                    resumeMusicAfterAd = false;
                }
                loadInterstitial(activity); // preload lần kế
            }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                loadInterstitial(activity);
            }
        });
        interstitialAd.show(activity);
    }
}
