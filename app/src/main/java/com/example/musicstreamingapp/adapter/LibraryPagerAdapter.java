package com.example.musicstreamingapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.musicstreamingapp.fragment.FollowingArtistsFragment;
import com.example.musicstreamingapp.fragment.LikedTracksFragment;
import com.example.musicstreamingapp.fragment.PlaylistsFragment;

public class LibraryPagerAdapter extends FragmentStateAdapter {
    public LibraryPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new PlaylistsFragment();
            case 1: return new LikedTracksFragment();
            case 2: return new FollowingArtistsFragment();
            default: return new PlaylistsFragment();
        }
    }

    @Override public int getItemCount() { return 3; }
}
