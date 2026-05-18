package com.example.musicstreamingapp.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Spotify-style RecyclerView item entrance animation: fade + slide-up,
 * 300ms decelerate. Only animates positions newer than the last one
 * tracked, so already-seen items don't re-animate on re-bind (scroll back).
 *
 * Usage in onBindViewHolder:
 *   ItemAnim.animate(holder.itemView, position, lastPosition);
 *
 * Adapter holds: {@code private final int[] lastPosition = {-1};}
 */
public final class ItemAnim {

    private ItemAnim() {}

    public static void animate(View view, int position, int[] lastPositionHolder) {
        if (position <= lastPositionHolder[0]) return;
        lastPositionHolder[0] = position;
        ObjectAnimator fade = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator slide = ObjectAnimator.ofFloat(view, "translationY", 40f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(fade, slide);
        set.setDuration(300);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
}
