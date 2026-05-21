package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.AlbumDetailActivity;
import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.HomeFeedAdapter;
import com.example.musicstreamingapp.databinding.FragmentHomeBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.HomeSection;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.viewmodel.HomeViewModel;
import com.example.musicstreamingapp.viewmodel.MainViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements HomeFeedAdapter.OnHomeItemClick {

    private FragmentHomeBinding b;
    private HomeViewModel vm;
    private MainViewModel mainVm;
    private final List<HomeSection> sections = new ArrayList<>();
    private HomeFeedAdapter adapter;
    private boolean shimmerHidden = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainVm = new ViewModelProvider(requireActivity(), new VmFactory(requireContext()))
            .get(MainViewModel.class);
        mainVm.usernameLetter().observe(getViewLifecycleOwner(),
            letter -> b.tvAvatarLetter.setText(letter));

        b.avatarContainer.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        });

        b.chipAll.setSelected(true);
        b.chipAll.setOnClickListener(v -> selectChip(b.chipAll, null));
        b.chipMusic.setOnClickListener(v -> selectChip(b.chipMusic, "music"));
        b.chipFollowing.setOnClickListener(v -> selectChip(b.chipFollowing, "following"));
        b.chipPodcast.setOnClickListener(v -> selectChip(b.chipPodcast, null));

        b.rvHomeFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HomeFeedAdapter(sections, this);
        b.rvHomeFeed.setAdapter(adapter);

        b.shimmerHome.startShimmer();
        b.shimmerHome.postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(HomeViewModel.class);
        vm.sections().observe(getViewLifecycleOwner(), data -> {
            sections.clear();
            if (data != null) sections.addAll(data);
            adapter.notifyDataSetChanged();
            if (data != null && !data.isEmpty()) hideShimmer();
        });
        // Match original behavior: silently ignore feed errors (HTTP non-2xx + network failures)
        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg -> hideShimmer()));
        vm.loadIfNeeded();
    }

    private void hideShimmer() {
        if (shimmerHidden || b == null) return;
        shimmerHidden = true;
        b.shimmerHome.stopShimmer();
        b.shimmerHome.setVisibility(View.GONE);
    }

    private void selectChip(TextView active, String filter) {
        TextView[] chips = { b.chipAll, b.chipMusic, b.chipFollowing, b.chipPodcast };
        for (TextView c : chips) c.setSelected(c == active);
        vm.onFilterSelected(filter);
    }

    @Override public void onTrack(Track track) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", track);
        startActivity(intent);
    }

    @Override public void onArtist(Artist artist) {
        Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
        intent.putExtra("artistId", artist.getArtistId());
        startActivity(intent);
    }

    @Override public void onAlbum(Album album) {
        Intent intent = new Intent(getContext(), AlbumDetailActivity.class);
        intent.putExtra("albumId", album.getAlbumId());
        startActivity(intent);
    }

    @Override public void onPlaylist(Playlist playlist) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlistId", playlist.getPlaylistId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
