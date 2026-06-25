package com.example.musicstreamingapp.util;

import androidx.fragment.app.FragmentManager;

import com.example.musicstreamingapp.fragment.ShuffleGateBottomSheet;

/**
 * Gộp logic "nút trộn bài + gate Premium" dùng chung cho Player và các màn chi tiết
 * (Album / Nghệ sĩ / Playlist) — trước đây bị lặp ở 7 nơi.
 *
 * <p>Quy tắc (giữ nguyên hành vi cũ):
 * <ul>
 *   <li>Free bị ép trộn bài luôn bật; chạm nút trộn (cố tắt) → mở {@link ShuffleGateBottomSheet}.</li>
 *   <li>Premium bật/tắt trộn bài thật.</li>
 *   <li>Nút "Phát tất cả" không gate — chỉ hỏi {@link #startIndex(int)} (ngẫu nhiên nếu đang trộn).</li>
 * </ul>
 *
 * <p>View nút trộn khác nhau giữa các màn (ImageView dùng {@code setColorFilter} vs Button dùng
 * {@code setTextColor}) nên phần tô màu để mỗi màn tự làm qua {@link Renderer}.
 */
public final class ShuffleController {

    /** Mỗi màn tự vẽ trạng thái nút trộn: xanh khi {@code shuffleOn}, xám khi tắt. */
    public interface Renderer {
        void render(boolean shuffleOn);
    }

    private final FragmentManager gateFm;
    private final Renderer renderer;
    private boolean premium = false;

    /** Vẽ trạng thái ban đầu ngay khi tạo (thay cho lần gọi updateShuffleUi() đầu tiên cũ). */
    public ShuffleController(FragmentManager gateFm, Renderer renderer) {
        this.gateFm = gateFm;
        this.renderer = renderer;
        render();
    }

    /** Cập nhật trạng thái Premium (thường gọi trong observer subscription). Free bị ép bật trộn. */
    public void setPremium(boolean premium) {
        this.premium = premium;
        if (!premium) PlayerManager.getInstance().setShuffleEnabled(true);
        render();
    }

    /** Chạm nút trộn: Free → gate Premium; Premium → bật/tắt thật rồi vẽ lại. */
    public void onShuffleClicked() {
        if (!premium) {
            new ShuffleGateBottomSheet().show(gateFm, "shuffle_gate");
            return;
        }
        PlayerManager pm = PlayerManager.getInstance();
        pm.setShuffleEnabled(!pm.isShuffleEnabled());
        render();
    }

    public boolean isShuffleOn() {
        return PlayerManager.getInstance().isShuffleEnabled();
    }

    /** Vị trí bắt đầu cho "Phát tất cả": ngẫu nhiên nếu đang trộn, ngược lại 0. */
    public int startIndex(int size) {
        if (size <= 0) return 0;
        return isShuffleOn() ? (int) (Math.random() * size) : 0;
    }

    /** Vẽ lại nút trộn theo trạng thái hiện tại. */
    public void render() {
        renderer.render(isShuffleOn());
    }
}
