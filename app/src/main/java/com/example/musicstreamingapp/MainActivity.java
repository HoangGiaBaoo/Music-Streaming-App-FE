package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.AccountAdapter;
import com.example.musicstreamingapp.fragment.CreateBottomSheet;
import com.example.musicstreamingapp.fragment.HomeFragment;
import com.example.musicstreamingapp.fragment.LibraryFragment;
import com.example.musicstreamingapp.fragment.PremiumFragment;
import com.example.musicstreamingapp.fragment.SearchFragment;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements PlayerManager.OnTrackChangeListener {
    private View miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageButton btnMiniPlay, btnMiniCast;
    private BottomNavigationView bottomNav;

    private DrawerLayout drawerLayout;
    private CircleImageView drawerAvatar;
    private TextView drawerDisplayName;
    private RecyclerView drawerAccountList;
    private AccountAdapter accountAdapter;
    private AccountStore accountStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        accountStore = new AccountStore(this);

        miniPlayer = findViewById(R.id.mini_player);
        ivMiniCover = miniPlayer.findViewById(R.id.iv_mini_cover);
        tvMiniTitle = miniPlayer.findViewById(R.id.tv_mini_title);
        tvMiniArtist = miniPlayer.findViewById(R.id.tv_mini_artist);
        btnMiniPlay = miniPlayer.findViewById(R.id.btn_mini_play);
        btnMiniCast = miniPlayer.findViewById(R.id.btn_mini_cast);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                new CreateBottomSheet().show(getSupportFragmentManager(), "create");
                return false;
            }
            if (id == R.id.nav_home)        loadFragment(new HomeFragment());
            else if (id == R.id.nav_search) loadFragment(new SearchFragment());
            else if (id == R.id.nav_library) loadFragment(new LibraryFragment());
            else if (id == R.id.nav_premium) loadFragment(new PremiumFragment());
            return true;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        PlayerManager.getInstance().setListener(this);
        updateMiniPlayer();

        miniPlayer.setOnClickListener(v -> {
            if (PlayerManager.getInstance().getCurrentTrack() != null) {
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });

        btnMiniPlay.setOnClickListener(v -> {
            PlayerManager.getInstance().togglePlayPause();
            updateMiniPlayer();
        });
        btnMiniCast.setOnClickListener(v -> {});

        setupDrawer();
    }

    private void setupDrawer() {
        drawerAvatar = findViewById(R.id.drawer_avatar);
        drawerDisplayName = findViewById(R.id.drawer_display_name);
        drawerAccountList = findViewById(R.id.drawer_account_list);

        drawerAccountList.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        accountAdapter = new AccountAdapter(accountStore.listAll(),
            new AccountAdapter.Listener() {
                @Override public void onSwitchAccount(AccountStore.Account a) {
                    accountStore.switchTo(a.username);
                    RetrofitClient.reset();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    recreate();
                }
                @Override public void onAddAccount() {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(MainActivity.this, AddAccountActivity.class));
                }
            });
        drawerAccountList.setAdapter(accountAdapter);

        findViewById(R.id.drawer_header).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.drawer_item_add_account).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AddAccountActivity.class));
        });
        findViewById(R.id.drawer_item_stats).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ListeningStatsActivity.class));
        });
        findViewById(R.id.drawer_item_recent).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, RecentActivity.class));
        });
        findViewById(R.id.drawer_item_news).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        findViewById(R.id.drawer_item_settings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, SettingsActivity.class));
        });

        loadDrawerHeader();
    }

    private void loadDrawerHeader() {
        String fallback = TokenManager.getUsername(this);
        drawerDisplayName.setText(fallback == null || fallback.isEmpty() ? "—" : fallback);

        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.getMe().enqueue(new Callback<UserMe>() {
            @Override public void onResponse(Call<UserMe> c, Response<UserMe> r) {
                UserMe me = r.body();
                if (me == null) return;
                drawerDisplayName.setText(me.resolveDisplayName());
                if (me.avatarUrl != null && !me.avatarUrl.isEmpty()) {
                    Glide.with(MainActivity.this)
                        .load(buildMediaUrl(me.avatarUrl))
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(drawerAvatar);
                }
                accountStore.addOrUpdate(me.username, TokenManager.getToken(MainActivity.this),
                    me.resolveDisplayName(), me.avatarUrl);
                refreshAccountList();
            }
            @Override public void onFailure(Call<UserMe> c, Throwable t) { }
        });
    }

    private void refreshAccountList() {
        AccountStore.Account current = accountStore.getCurrent();
        String currentUsername = current == null ? null : current.username;
        accountAdapter.setData(accountStore.listAll(), currentUsername);
        drawerAccountList.setVisibility(
            accountStore.listAll().size() > 1 ? View.VISIBLE : View.GONE);
    }

    public static String buildMediaUrl(String path) {
        if (path == null) return null;
        if (path.startsWith("http")) return path;
        return RetrofitClient.BASE_MEDIA_URL + path;
    }

    public void openDrawer() {
        if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    private void updateMiniPlayer() {
        Track track = PlayerManager.getInstance().getCurrentTrack();
        if (track == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }
        miniPlayer.setVisibility(View.VISIBLE);
        tvMiniTitle.setText(track.getTitle());
        tvMiniArtist.setText(track.getArtist() != null ? track.getArtist().getName() : "");
        if (track.getCoverUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(ivMiniCover);
        }
        btnMiniPlay.setImageResource(
            PlayerManager.getInstance().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override public void onTrackChanged(Track track) {
        runOnUiThread(this::updateMiniPlayer);
    }

    @Override public void onPlayStateChanged(boolean isPlaying) {
        runOnUiThread(() -> btnMiniPlay.setImageResource(
            isPlaying ? R.drawable.ic_pause : R.drawable.ic_play));
    }

    @Override protected void onResume() {
        super.onResume();
        PlayerManager.getInstance().setListener(this);
        updateMiniPlayer();
        refreshAccountList();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PlayerManager.getInstance().setListener(null);
    }

    @Override public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
