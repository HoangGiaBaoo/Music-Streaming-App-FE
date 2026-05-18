package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.adapter.AccountAdapter;
import com.example.musicstreamingapp.databinding.ActivityMainBinding;
import com.example.musicstreamingapp.fragment.CreateBottomSheet;
import com.example.musicstreamingapp.fragment.HomeFragment;
import com.example.musicstreamingapp.fragment.LibraryFragment;
import com.example.musicstreamingapp.fragment.PremiumFragment;
import com.example.musicstreamingapp.fragment.SearchFragment;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.example.musicstreamingapp.viewmodel.MainViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;
    private MainViewModel vm;

    private CircleImageView drawerAvatar;
    private android.widget.TextView drawerDisplayName;
    private RecyclerView drawerAccountList;
    private AccountAdapter accountAdapter;
    private AccountStore accountStore;

    private boolean miniPlayerShown = false;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (b.miniPlayer.getRoot().getVisibility() == View.VISIBLE) {
                long pos = PlayerManager.getInstance().getCurrentPosition();
                long dur = PlayerManager.getInstance().getDuration();
                if (dur > 0) {
                    int pct = (int) ((pos * 100) / dur);
                    b.miniPlayer.pbMiniProgress.setProgress(pct);
                }
                progressHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        accountStore = new AccountStore(this);
        vm = new ViewModelProvider(this, new VmFactory(this)).get(MainViewModel.class);

        setupBottomNav();
        setupMiniPlayer();
        setupDrawer();

        if (savedInstanceState == null) loadFragment(new HomeFragment());

        observeViewModel();
        vm.updateUsername(TokenManager.getUsername(this));
        vm.loadDrawerHeader();
    }

    private void setupBottomNav() {
        b.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                new CreateBottomSheet().show(getSupportFragmentManager(), "create");
                return false;
            }
            if (id == R.id.nav_home)         loadFragment(new HomeFragment());
            else if (id == R.id.nav_search)  loadFragment(new SearchFragment());
            else if (id == R.id.nav_library) loadFragment(new LibraryFragment());
            else if (id == R.id.nav_premium) loadFragment(new PremiumFragment());
            return true;
        });
    }

    private void setupMiniPlayer() {
        b.miniPlayer.getRoot().setOnClickListener(v -> {
            if (vm.currentTrack().getValue() != null) {
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });
        b.miniPlayer.btnMiniPlay.setOnClickListener(v -> vm.onMiniPlayPauseClicked());
        b.miniPlayer.btnMiniCast.setOnClickListener(v -> {});
    }

    private void setupDrawer() {
        View root = b.getRoot();
        drawerAvatar      = root.findViewById(R.id.drawer_avatar);
        drawerDisplayName = root.findViewById(R.id.drawer_display_name);
        drawerAccountList = root.findViewById(R.id.drawer_account_list);

        drawerAccountList.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        accountAdapter = new AccountAdapter(accountStore.listAll(),
            new AccountAdapter.Listener() {
                @Override public void onSwitchAccount(AccountStore.Account a) {
                    accountStore.switchTo(a.username);
                    RetrofitClient.reset();
                    Intent restart = new Intent(MainActivity.this, MainActivity.class);
                    restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(restart);
                }
                @Override public void onAddAccount() {
                    b.drawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(MainActivity.this, AddAccountActivity.class));
                }
            });
        drawerAccountList.setAdapter(accountAdapter);

        root.findViewById(R.id.drawer_header).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        root.findViewById(R.id.drawer_item_add_account).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AddAccountActivity.class));
        });
        root.findViewById(R.id.drawer_item_stats).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ListeningStatsActivity.class));
        });
        root.findViewById(R.id.drawer_item_recent).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, RecentActivity.class));
        });
        root.findViewById(R.id.drawer_item_news).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        });
        root.findViewById(R.id.drawer_item_settings).setOnClickListener(v -> {
            b.drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, SettingsActivity.class));
        });

        String fallback = TokenManager.getUsername(this);
        drawerDisplayName.setText(fallback == null || fallback.isEmpty() ? "—" : fallback);
    }

    private void observeViewModel() {
        vm.userMe().observe(this, this::renderDrawerHeader);
        vm.currentTrack().observe(this, this::renderMiniPlayer);
        vm.playing().observe(this, isPlaying -> b.miniPlayer.btnMiniPlay.setImageResource(
            Boolean.TRUE.equals(isPlaying) ? R.drawable.ic_pause : R.drawable.ic_play));
    }

    private void renderDrawerHeader(UserMe me) {
        if (me == null) return;
        drawerDisplayName.setText(me.resolveDisplayName());
        if (me.avatarUrl != null && !me.avatarUrl.isEmpty()) {
            Glide.with(this)
                .load(buildMediaUrl(me.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(drawerAvatar);
        }
        // Only persist account data when it matches the current logged-in session
        // to prevent a stale or wrong-user API response from overwriting the current account
        String currentUsername = TokenManager.getUsername(this);
        if (me.username != null && me.username.equals(currentUsername)) {
            accountStore.addOrUpdate(me.username, TokenManager.getToken(this),
                me.resolveDisplayName(), me.avatarUrl);
        }
        refreshAccountList();
    }

    private void renderMiniPlayer(Track track) {
        if (track == null) {
            if (miniPlayerShown) {
                b.miniPlayer.getRoot().startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.mini_player_slide_down));
                b.miniPlayer.getRoot().setVisibility(View.GONE);
                miniPlayerShown = false;
                progressHandler.removeCallbacks(progressTick);
            }
            return;
        }
        if (!miniPlayerShown) {
            b.miniPlayer.getRoot().setVisibility(View.VISIBLE);
            b.miniPlayer.getRoot().startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.mini_player_slide_up));
            miniPlayerShown = true;
            progressHandler.post(progressTick);
        }
        b.miniPlayer.tvMiniTitle.setText(track.getTitle());
        b.miniPlayer.tvMiniTitle.setSelected(true); // enable marquee
        b.miniPlayer.tvMiniArtist.setText(track.getArtist() != null ? track.getArtist().getName() : "");
        if (track.getCoverUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(b.miniPlayer.ivMiniCover);
        }
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
        b.drawerLayout.openDrawer(GravityCompat.START);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.refreshFromPlayer();
        vm.updateUsername(TokenManager.getUsername(this));
        vm.loadDrawerHeader();
        refreshAccountList();
    }

    @Override
    public void onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressTick);
    }
}
