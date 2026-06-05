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

import com.example.musicstreamingapp.AddArtistActivity;
import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.util.NavHelper;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.LibraryAdapter;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.databinding.FragmentLibraryBinding;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.example.musicstreamingapp.viewmodel.LibraryViewModel;
import com.example.musicstreamingapp.viewmodel.LibraryViewModel.Chip;
import com.example.musicstreamingapp.viewmodel.MainViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding b;
    private LibraryViewModel vm;
    private MainViewModel mainVm;
    private final List<LibraryAdapter.Item> items = new ArrayList<>();
    private LibraryAdapter adapter;
    private boolean shimmerHidden = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLibraryBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainVm = new ViewModelProvider(requireActivity(), new VmFactory(requireContext()))
            .get(MainViewModel.class);
        mainVm.usernameLetter().observe(getViewLifecycleOwner(),
            letter -> b.tvLibAvatar.setText(letter));

        b.libAvatarContainer.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openDrawer();
            }
        });

        b.libChipPlaylist.setOnClickListener(v -> vm.onChipSelected(Chip.PLAYLIST));
        b.libChipArtist.setOnClickListener(v ->   vm.onChipSelected(Chip.ARTIST));
        b.libChipAlbum.setOnClickListener(v ->    vm.onChipSelected(Chip.ALBUM));
        b.libChipLiked.setOnClickListener(v ->    vm.onChipSelected(Chip.LIKED));

        b.rvLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        LibraryRepository repo = new LibraryRepository(
            RetrofitClient.getApiService(TokenManager.getPrefs(requireContext())));
        adapter = new LibraryAdapter(items, repo);
        b.rvLibrary.setAdapter(adapter);

        b.shimmerLibrary.getRoot().startShimmer();
        b.shimmerLibrary.getRoot().postDelayed(this::hideShimmer, 4000);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(LibraryViewModel.class);

        vm.chip().observe(getViewLifecycleOwner(), c -> { renderChipState(c); renderCurrent(); });
        vm.playlists().observe(getViewLifecycleOwner(), data -> { renderCurrent(); if (data != null && !data.isEmpty()) hideShimmer(); });
        vm.artists().observe(getViewLifecycleOwner(), data -> { renderCurrent(); if (data != null && !data.isEmpty()) hideShimmer(); });
        vm.liked().observe(getViewLifecycleOwner(), data -> { renderCurrent(); if (data != null && !data.isEmpty()) hideShimmer(); });
        vm.likedCount().observe(getViewLifecycleOwner(), n -> renderCurrent());
    }

    private void hideShimmer() {
        if (shimmerHidden || b == null) return;
        shimmerHidden = true;
        b.shimmerLibrary.getRoot().stopShimmer();
        b.shimmerLibrary.getRoot().setVisibility(View.GONE);
    }

    private void renderChipState(Chip c) {
        TextView[] all = { b.libChipPlaylist, b.libChipArtist, b.libChipAlbum, b.libChipLiked };
        TextView active = (c == Chip.ALL) ? null : chipView(c);
        for (TextView t : all) t.setSelected(t == active);
    }

    private TextView chipView(Chip c) {
        switch (c) {
            case PLAYLIST: return b.libChipPlaylist;
            case ARTIST:   return b.libChipArtist;
            case ALBUM:    return b.libChipAlbum;
            case LIKED:    return b.libChipLiked;
            default:       return null;
        }
    }

    private void renderCurrent() {
        Chip c = vm.chip().getValue();
        if (c == null) return;
        items.clear();
        if (c == Chip.ALL || c == Chip.PLAYLIST) addLikedSongsPinned();
        switch (c) {
            case ALL:
                addPlaylists(vm.playlists().getValue());
                addArtists(vm.artists().getValue());
                break;
            case PLAYLIST:
                addPlaylists(vm.playlists().getValue());
                break;
            case ARTIST:
                addArtists(vm.artists().getValue());
                break;
            case LIKED:
                addLiked(vm.liked().getValue());
                break;
            case ALBUM:
            default:
                break;
        }
        appendActions();
        adapter.notifyDataSetChanged();
    }

    /** Item ghim "Bài hát đã thích" (playlist ảo) — luôn ở đầu danh sách, mở LikedSongsActivity. */
    private void addLikedSongsPinned() {
        Integer count = vm.likedCount().getValue();
        int n = count != null ? count : 0;
        items.add(LibraryAdapter.Item.likedSongs(
            getString(R.string.liked_songs_title),
            getString(R.string.liked_songs_subtitle, n),
            () -> NavHelper.openLiked(requireContext())));
    }

    private void addPlaylists(List<Playlist> data) {
        if (data == null) return;
        for (Playlist p : data) {
            items.add(LibraryAdapter.Item.playlistRow(
                p.getPlaylistId(), p.getCoverUrl(), p.getName(), "Danh sách phát",
                () -> openPlaylist(p)));
        }
    }

    private void addArtists(List<Artist> data) {
        if (data == null) return;
        for (Artist a : data) {
            items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_ARTIST,
                a.getAvatarUrl(), a.getName(), "Nghệ sĩ",
                () -> openArtist(a)));
        }
    }

    private void addLiked(List<Track> data) {
        if (data == null) return;
        for (Track t : data) {
            items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_TRACK,
                t.getCoverUrl(), t.getTitle(), t.getArtistName(),
                () -> openPlayer(t)));
        }
    }

    private void appendActions() {
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_artist), () ->
            startActivity(new Intent(getContext(), AddArtistActivity.class))));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_podcast), () -> {}));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_event), () -> {}));
        items.add(LibraryAdapter.Item.action(R.drawable.ic_download, getString(R.string.add_music), () -> {}));
    }

    private void openPlayer(Track t) {
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("track", t);
        startActivity(intent);
    }

    private void openArtist(Artist a) {
        NavHelper.openArtist(requireContext(), a.getArtistId());
    }

    private void openPlaylist(Playlist p) {
        NavHelper.openPlaylist(requireContext(), p.getPlaylistId(), p.getName());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
