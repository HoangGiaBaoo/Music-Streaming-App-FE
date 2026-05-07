package com.example.musicstreamingapp.fragment;

import android.content.Intent;
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
import com.example.musicstreamingapp.adapter.TrackListAdapter;
import com.example.musicstreamingapp.databinding.FragmentLikedTracksBinding;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.viewmodel.LikedTracksViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

public class LikedTracksFragment extends Fragment {

    private FragmentLikedTracksBinding b;
    private LikedTracksViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLikedTracksBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.rvLiked.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TrackListAdapter(tracks, (track, pos) -> {
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("track", track);
            startActivity(intent);
        });
        b.rvLiked.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(LikedTracksViewModel.class);
        vm.tracks().observe(getViewLifecycleOwner(), data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            b.tvEmpty.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
