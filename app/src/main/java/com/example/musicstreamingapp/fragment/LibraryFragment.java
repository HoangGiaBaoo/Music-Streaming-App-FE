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
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.PlaylistDetailActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.LibraryAdapter;
import com.example.musicstreamingapp.databinding.FragmentLibraryBinding;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
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

        b.libChipPlaylist.setOnClickListener(v -> vm.onChipSelected(Chip.PLAYLIST));
        b.libChipArtist.setOnClickListener(v ->   vm.onChipSelected(Chip.ARTIST));
        b.libChipAlbum.setOnClickListener(v ->    vm.onChipSelected(Chip.ALBUM));
        b.libChipLiked.setOnClickListener(v ->    vm.onChipSelected(Chip.LIKED));

        b.rvLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LibraryAdapter(items);
        b.rvLibrary.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(LibraryViewModel.class);

        vm.chip().observe(getViewLifecycleOwner(), this::renderChipState);
        vm.playlists().observe(getViewLifecycleOwner(), data -> {
            if (vm.chip().getValue() == Chip.PLAYLIST) renderPlaylists(data);
        });
        vm.artists().observe(getViewLifecycleOwner(), data -> {
            if (vm.chip().getValue() == Chip.ARTIST) renderArtists(data);
        });
        vm.liked().observe(getViewLifecycleOwner(), data -> {
            if (vm.chip().getValue() == Chip.LIKED) renderLiked(data);
        });
    }

    private void renderChipState(Chip c) {
        TextView[] all = { b.libChipPlaylist, b.libChipArtist, b.libChipAlbum, b.libChipLiked };
        TextView active = chipView(c);
        for (TextView t : all) t.setSelected(t == active);
        if (c == Chip.ALBUM) renderAlbumPlaceholder();
    }

    private TextView chipView(Chip c) {
        switch (c) {
            case PLAYLIST: return b.libChipPlaylist;
            case ARTIST:   return b.libChipArtist;
            case ALBUM:    return b.libChipAlbum;
            case LIKED:    return b.libChipLiked;
            default:       return b.libChipArtist;
        }
    }

    private void renderPlaylists(List<Playlist> data) {
        items.clear();
        if (data != null) {
            for (Playlist p : data) {
                items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_PLAYLIST,
                    p.getCoverUrl(), p.getName(), "Danh sách phát",
                    () -> openPlaylist(p)));
            }
        }
        appendActions();
        adapter.notifyDataSetChanged();
    }

    private void renderArtists(List<Artist> data) {
        items.clear();
        if (data != null) {
            for (Artist a : data) {
                items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_ARTIST,
                    a.getAvatarUrl(), a.getName(), "Nghệ sĩ",
                    () -> openArtist(a)));
            }
        }
        appendActions();
        adapter.notifyDataSetChanged();
    }

    private void renderLiked(List<Track> data) {
        items.clear();
        if (data != null) {
            for (Track t : data) {
                items.add(LibraryAdapter.Item.row(LibraryAdapter.TYPE_TRACK,
                    t.getCoverUrl(), t.getTitle(), t.getArtistName(),
                    () -> openPlayer(t)));
            }
        }
        appendActions();
        adapter.notifyDataSetChanged();
    }

    private void renderAlbumPlaceholder() {
        items.clear();
        appendActions();
        adapter.notifyDataSetChanged();
    }

    private void appendActions() {
        items.add(LibraryAdapter.Item.action(R.drawable.ic_add, getString(R.string.add_artist), () -> {}));
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
        Intent intent = new Intent(getContext(), ArtistDetailActivity.class);
        intent.putExtra("artistId", a.getArtistId());
        startActivity(intent);
    }

    private void openPlaylist(Playlist p) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlistId", p.getPlaylistId());
        intent.putExtra("playlistName", p.getName());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
