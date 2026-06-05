package com.example.musicstreamingapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.example.musicstreamingapp.AlbumDetailActivity;
import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.LikedSongsActivity;
import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.RecentActivity;
import com.example.musicstreamingapp.fragment.AlbumDetailFragment;
import com.example.musicstreamingapp.fragment.ArtistDetailFragment;
import com.example.musicstreamingapp.fragment.LikedSongsFragment;
import com.example.musicstreamingapp.fragment.PlaylistDetailFragment;
import com.example.musicstreamingapp.fragment.RecentFragment;

/**
 * Router mở các màn chi tiết. Nếu ngữ cảnh nằm trong {@link MainActivity} → mở dạng Fragment
 * (thanh bottom-nav + mini-player đứng yên, không dựng lại). Nếu được gọi từ một Activity riêng
 * (Profile, AddArtist…) → fallback mở Activity cũ như trước. Nhờ vậy mọi call site dùng chung
 * một API, không cần biết mình đang ở đâu.
 */
public final class NavHelper {

    private NavHelper() {}

    private static MainActivity asMain(Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof MainActivity) return (MainActivity) ctx;
            if (ctx instanceof Activity) return null; // Activity khác → không phải Main
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static void openAlbum(Context ctx, long albumId) {
        MainActivity main = asMain(ctx);
        if (main != null) {
            main.openDetail(AlbumDetailFragment.newInstance(albumId));
        } else {
            Intent i = new Intent(ctx, AlbumDetailActivity.class);
            i.putExtra("albumId", albumId);
            ctx.startActivity(i);
        }
    }

    public static void openArtist(Context ctx, long artistId) {
        MainActivity main = asMain(ctx);
        if (main != null) {
            main.openDetail(ArtistDetailFragment.newInstance(artistId));
        } else {
            Intent i = new Intent(ctx, ArtistDetailActivity.class);
            i.putExtra("artistId", artistId);
            ctx.startActivity(i);
        }
    }

    public static void openPlaylist(Context ctx, long playlistId, String name) {
        MainActivity main = asMain(ctx);
        if (main != null) {
            main.openDetail(PlaylistDetailFragment.newInstance(playlistId, name));
        } else {
            Intent i = new Intent(ctx, PlaylistDetailActivity.class);
            i.putExtra("playlistId", playlistId);
            i.putExtra("playlistName", name);
            ctx.startActivity(i);
        }
    }

    public static void openLiked(Context ctx) {
        MainActivity main = asMain(ctx);
        if (main != null) {
            main.openDetail(new LikedSongsFragment());
        } else {
            ctx.startActivity(new Intent(ctx, LikedSongsActivity.class));
        }
    }

    public static void openRecent(Context ctx) {
        MainActivity main = asMain(ctx);
        if (main != null) {
            main.openDetail(new RecentFragment());
        } else {
            ctx.startActivity(new Intent(ctx, RecentActivity.class));
        }
    }
}
