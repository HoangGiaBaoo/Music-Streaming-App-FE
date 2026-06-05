package com.example.musicstreamingapp.fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.fragment.app.Fragment;

/**
 * Base cho các màn chi tiết nạp bằng animation trượt. Hoãn phần khởi tạo nặng (bật shimmer,
 * đăng ký observe + gọi tải dữ liệu, tải ảnh bìa) tới SAU khi animation vào chạy xong — nhờ vậy
 * frame đầu của lúc trượt chỉ phải dựng layout rỗng, không bị giật.
 *
 * Cách dùng: ở cuối onViewCreated chỉ làm việc nhẹ (bind, set listener, layout manager) rồi gọi
 * {@link #scheduleDeferredSetup()}; đặt việc nặng trong {@link #onEnterAnimationDone()}.
 */
public abstract class BaseDetailFragment extends Fragment {

    private static final long FALLBACK_MS = 340; // > thời lượng anim (280ms) để làm lưới an toàn

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean deferredRan = false;

    /** Việc nặng frame-đầu, chạy 1 lần sau khi animation vào kết thúc. */
    protected abstract void onEnterAnimationDone();

    /** Gọi ở cuối onViewCreated để lên lịch chạy phần nặng sau animation. */
    protected void scheduleDeferredSetup() {
        handler.postDelayed(this::runDeferredOnce, FALLBACK_MS);
    }

    private void runDeferredOnce() {
        if (deferredRan || !isAdded() || getView() == null) return;
        deferredRan = true;
        handler.removeCallbacksAndMessages(null);
        onEnterAnimationDone();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter && nextAnim != 0 && !deferredRan) {
            try {
                Animation anim = AnimationUtils.loadAnimation(requireContext(), nextAnim);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation a) {}
                    @Override public void onAnimationEnd(Animation a) {
                        handler.post(BaseDetailFragment.this::runDeferredOnce);
                    }
                    @Override public void onAnimationRepeat(Animation a) {}
                });
                return anim;
            } catch (Exception ignore) {
                // rơi xuống fallback postDelayed đã đặt ở scheduleDeferredSetup()
            }
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
