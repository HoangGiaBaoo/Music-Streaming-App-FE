package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.RecentSectionAdapter;
import com.example.musicstreamingapp.databinding.FragmentRecentBinding;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.viewmodel.RecentViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bản Fragment của màn "Đã nghe gần đây". */
public class RecentFragment extends Fragment {

    private FragmentRecentBinding b;
    private RecentSectionAdapter adapter;
    private RecentViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentRecentBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        b.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        b.rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecentSectionAdapter((track, pos) -> {
            PlayerManager.getInstance().play(requireContext().getApplicationContext(),
                track, Collections.singletonList(track), 0);
            startActivity(new Intent(requireContext(), PlayerActivity.class));
        });
        b.rvRecent.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(RecentViewModel.class);
        vm.groups().observe(getViewLifecycleOwner(), groups -> adapter.setItems(toFlatList(groups)));
        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg ->
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
