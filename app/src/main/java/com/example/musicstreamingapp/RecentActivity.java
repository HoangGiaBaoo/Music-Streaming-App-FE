package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.adapter.RecentSectionAdapter;
import com.example.musicstreamingapp.databinding.ActivityRecentBinding;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.viewmodel.RecentViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentActivity extends AppCompatActivity {

    private ActivityRecentBinding b;
    private RecentSectionAdapter adapter;
    private RecentViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRecentBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        b.rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecentSectionAdapter((track, pos) -> {
            PlayerManager.getInstance().play(getApplicationContext(),
                track, Collections.singletonList(track), 0);
            startActivity(new Intent(this, PlayerActivity.class));
        });
        b.rvRecent.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(RecentViewModel.class);
        vm.groups().observe(this, groups -> adapter.setItems(toFlatList(groups)));
        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
        vm.loadIfNeeded();
    }

    private List<Object> toFlatList(List<RecentViewModel.DayGroup> groups) {
        List<Object> flat = new ArrayList<>();
        if (groups == null) return flat;
        for (RecentViewModel.DayGroup g : groups) {
            flat.add(formatDateLabel(g.dayKey));
            flat.addAll(g.tracks);
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
