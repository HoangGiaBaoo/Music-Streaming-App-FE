package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.PlaylistAdapter;
import com.example.musicstreamingapp.model.UserProfile;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "userId";

    private TextView tvName, tvStats;
    private CircleImageView ivAvatar;
    private RecyclerView rvPlaylists;
    private View emptyState;
    private MaterialButton btnEdit;
    private View root;

    private Long targetUserId;
    private UserProfile loaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        targetUserId = getIntent().hasExtra(EXTRA_USER_ID)
            ? getIntent().getLongExtra(EXTRA_USER_ID, -1L) : null;
        if (targetUserId != null && targetUserId == -1L) targetUserId = null;

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        root = findViewById(android.R.id.content);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);
        tvStats = findViewById(R.id.tv_stats);
        rvPlaylists = findViewById(R.id.rv_playlists);
        emptyState = findViewById(R.id.empty_state);
        btnEdit = findViewById(R.id.btn_edit);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        ImageButton share = findViewById(R.id.btn_share);
        ImageButton more = findViewById(R.id.btn_more);
        share.setOnClickListener(v -> {});
        more.setOnClickListener(v -> {});

        btnEdit.setVisibility(targetUserId == null ? View.VISIBLE : View.GONE);
        btnEdit.setOnClickListener(v -> {
            if (loaded == null) return;
            Intent i = new Intent(this, EditProfileActivity.class);
            i.putExtra(EditProfileActivity.EXTRA_DISPLAY_NAME, loaded.displayName);
            i.putExtra(EditProfileActivity.EXTRA_BIO, loaded.bio);
            i.putExtra(EditProfileActivity.EXTRA_AVATAR, loaded.avatarUrl);
            startActivity(i);
        });

        loadProfile();
    }

    @Override protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        Call<UserProfile> call = (targetUserId == null)
            ? api.getMyProfile()
            : api.getUserProfile(targetUserId);

        call.enqueue(new Callback<UserProfile>() {
            @Override public void onResponse(Call<UserProfile> c, Response<UserProfile> r) {
                if (r.body() == null) return;
                bind(r.body());
            }
            @Override public void onFailure(Call<UserProfile> c, Throwable t) {
                Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void bind(UserProfile p) {
        loaded = p;
        tvName.setText(p.displayName != null ? p.displayName : "—");
        tvStats.setText(getString(R.string.profile_followers_format, p.followers, p.following));
        if (p.avatarUrl != null && !p.avatarUrl.isEmpty()) {
            Glide.with(this)
                .load(MainActivity.buildMediaUrl(p.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(ivAvatar);
        }
        if (p.playlists == null || p.playlists.isEmpty()) {
            rvPlaylists.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvPlaylists.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            rvPlaylists.setAdapter(new PlaylistAdapter(p.playlists, pl -> {
                Intent i = new Intent(this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", pl.getPlaylistId());
                startActivity(i);
            }));
        }
    }
}
