package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.adapter.RecentSectionAdapter;
import com.example.musicstreamingapp.model.RecentItem;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecentActivity extends AppCompatActivity {
    private RecyclerView rv;
    private RecentSectionAdapter adapter;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());
        root = findViewById(android.R.id.content);

        rv = findViewById(R.id.rv_recent);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecentSectionAdapter((track, pos) -> {
            PlayerManager.getInstance().play(getApplicationContext(),
                track, Collections.singletonList(track), 0);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        rv.setAdapter(adapter);

        load();
    }

    private void load() {
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.getRecentTracks(50).enqueue(new Callback<List<RecentItem>>() {
            @Override public void onResponse(Call<List<RecentItem>> c, Response<List<RecentItem>> r) {
                if (r.body() == null) return;
                adapter.setItems(buildSections(r.body()));
            }
            @Override public void onFailure(Call<List<RecentItem>> c, Throwable t) {
                Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private List<Object> buildSections(List<RecentItem> items) {
        Map<String, List<Track>> grouped = new LinkedHashMap<>();
        for (RecentItem item : items) {
            if (item == null || item.track == null) continue;
            String key = item.dayKey();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item.track);
        }
        List<Object> flat = new ArrayList<>();
        for (Map.Entry<String, List<Track>> e : grouped.entrySet()) {
            flat.add(formatDateLabel(e.getKey()));
            flat.addAll(e.getValue());
        }
        return flat;
    }

    private String formatDateLabel(String dayKey) {
        if (dayKey == null || dayKey.isEmpty()) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate d = LocalDate.parse(dayKey);
                LocalDate today = LocalDate.now();
                if (d.equals(today)) return getString(R.string.recent_today);
                if (d.equals(today.minusDays(1))) return getString(R.string.recent_yesterday);
                if (d.equals(today.minusDays(2))) return getString(R.string.recent_two_days_ago);
                return getString(R.string.recent_date_format,
                    d.getDayOfMonth(), d.getMonthValue(), d.getYear());
            } catch (Exception ignore) { }
        }
        return dayKey;
    }
}
