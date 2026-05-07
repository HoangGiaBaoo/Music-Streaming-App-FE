package com.example.musicstreamingapp.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.PlaylistAdapter;
import com.example.musicstreamingapp.databinding.FragmentPlaylistsBinding;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.viewmodel.PlaylistsViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private FragmentPlaylistsBinding b;
    private PlaylistsViewModel vm;
    private final List<Playlist> playlists = new ArrayList<>();
    private PlaylistAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPlaylistsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter(playlists, playlist -> {
            Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
            intent.putExtra("playlistId", playlist.getPlaylistId());
            intent.putExtra("playlistName", playlist.getName());
            startActivity(intent);
        });
        b.rvPlaylists.setAdapter(adapter);

        b.fabAdd.setOnClickListener(v -> showCreateDialog());

        vm = new ViewModelProvider(requireActivity(), new VmFactory(requireContext()))
            .get(PlaylistsViewModel.class);
        vm.playlists().observe(getViewLifecycleOwner(), data -> {
            playlists.clear();
            if (data != null) playlists.addAll(data);
            adapter.notifyDataSetChanged();
            b.tvEmpty.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refresh();
    }

    private void showCreateDialog() {
        EditText et = new EditText(requireContext());
        et.setHint(getString(R.string.playlist_name));
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_playlist)
            .setView(et)
            .setPositiveButton(R.string.create, (d, w) -> vm.create(et.getText().toString()))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
