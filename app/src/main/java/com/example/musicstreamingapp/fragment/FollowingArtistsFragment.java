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

import com.example.musicstreamingapp.AddArtistActivity;
import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.adapter.ArtistListAdapter;
import com.example.musicstreamingapp.databinding.FragmentFollowingArtistsBinding;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.viewmodel.FollowingArtistsViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

public class FollowingArtistsFragment extends Fragment {

    private FragmentFollowingArtistsBinding b;
    private FollowingArtistsViewModel vm;
    private final List<Artist> artists = new ArrayList<>();
    private ArtistListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentFollowingArtistsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.rvFollowing.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArtistListAdapter(artists, artist -> {
            Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
            intent.putExtra("artistId", artist.getArtistId());
            startActivity(intent);
        });
        b.rvFollowing.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(requireContext()))
            .get(FollowingArtistsViewModel.class);
        vm.artists().observe(getViewLifecycleOwner(), data -> {
            artists.clear();
            if (data != null) artists.addAll(data);
            adapter.notifyDataSetChanged();
            b.tvEmpty.setVisibility(artists.isEmpty() ? View.VISIBLE : View.GONE);
        });

        b.fabAddArtist.setOnClickListener(v ->
            startActivity(new Intent(getContext(), AddArtistActivity.class)));
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
