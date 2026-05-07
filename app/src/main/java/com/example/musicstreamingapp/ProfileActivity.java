package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.PlaylistAdapter;
import com.example.musicstreamingapp.databinding.ActivityProfileBinding;
import com.example.musicstreamingapp.model.UserProfile;
import com.example.musicstreamingapp.viewmodel.ProfileViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";

    private ActivityProfileBinding b;
    private ProfileViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        Long targetUserId = getIntent().hasExtra(EXTRA_USER_ID)
            ? getIntent().getLongExtra(EXTRA_USER_ID, -1L) : null;
        if (targetUserId != null && targetUserId == -1L) targetUserId = null;

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        b.btnShare.setOnClickListener(v -> {});
        b.btnMore.setOnClickListener(v -> {});

        vm = new ViewModelProvider(this, new VmFactory(this)).get(ProfileViewModel.class);
        vm.setTarget(targetUserId);

        b.btnEdit.setVisibility(vm.isOwnProfile() ? View.VISIBLE : View.GONE);
        b.btnEdit.setOnClickListener(v -> openEdit());

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.refresh();
    }

    private void observeViewModel() {
        vm.profile().observe(this, this::render);
        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    private void openEdit() {
        UserProfile p = vm.profile().getValue();
        if (p == null) return;
        Intent i = new Intent(this, EditProfileActivity.class);
        i.putExtra(EditProfileActivity.EXTRA_DISPLAY_NAME, p.displayName);
        i.putExtra(EditProfileActivity.EXTRA_BIO, p.bio);
        i.putExtra(EditProfileActivity.EXTRA_AVATAR, p.avatarUrl);
        startActivity(i);
    }

    private void render(UserProfile p) {
        if (p == null) return;
        b.tvName.setText(p.displayName != null ? p.displayName : "—");
        b.tvStats.setText(getString(R.string.profile_followers_format, p.followers, p.following));
        if (p.avatarUrl != null && !p.avatarUrl.isEmpty()) {
            Glide.with(this)
                .load(MainActivity.buildMediaUrl(p.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(b.ivAvatar);
        }
        if (p.playlists == null || p.playlists.isEmpty()) {
            b.rvPlaylists.setVisibility(View.GONE);
            b.emptyState.setVisibility(View.VISIBLE);
        } else {
            b.rvPlaylists.setVisibility(View.VISIBLE);
            b.emptyState.setVisibility(View.GONE);
            b.rvPlaylists.setAdapter(new PlaylistAdapter(p.playlists, pl -> {
                Intent i = new Intent(this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", pl.getPlaylistId());
                startActivity(i);
            }));
        }
    }
}
