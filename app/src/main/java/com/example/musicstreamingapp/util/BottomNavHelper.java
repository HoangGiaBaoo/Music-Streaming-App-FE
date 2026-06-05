package com.example.musicstreamingapp.util;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.fragment.CreateBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Gắn thanh bottom-nav (5 icon) cho các màn Activity ngoài MainActivity (Playlist/Album…).
 * Bấm icon → quay về MainActivity và mở đúng tab (Home/Search/Library/Premium); "Tạo" mở
 * CreateBottomSheet ngay tại màn hiện tại. Dùng CLEAR_TOP|SINGLE_TOP nên màn detail đang nằm
 * trên MainActivity sẽ bị gỡ khỏi back-stack, giống hành vi đổi tab của Spotify.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(AppCompatActivity activity, BottomNavigationView nav) {
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                new CreateBottomSheet().show(activity.getSupportFragmentManager(), "create");
                return false;
            }
            Intent i = new Intent(activity, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(MainActivity.EXTRA_TAB, id);
            activity.startActivity(i);
            return false; // không highlight item trên màn detail
        });
    }
}
