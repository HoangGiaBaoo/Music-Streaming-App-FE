package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.FragmentLikedSongsBinding;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.util.NavHelper;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.TokenManager;
import com.example.musicstreamingapp.viewmodel.LikedTracksViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.List;

/** Bản Fragment của màn "Bài hát đã thích" (playlist ảo từ GET /api/tracks/liked). */
public class LikedSongsFragment extends Fragment {

    private FragmentLikedSongsBinding b;
    private LikedTracksViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLikedSongsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        b.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        b.collapsingToolbar.setTitle(getString(R.string.liked_songs_title));

        b.rvTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(requireContext(), track, tracks, pos);
            startActivity(new Intent(requireContext(), PlayerActivity.class));
        });
        adapter.setMoreListener(this::showTrackMenu);
        b.rvTracks.setAdapter(adapter);

        b.btnPlayFab.setOnClickListener(v -> {
            if (!tracks.isEmpty()) {
                PlayerManager.getInstance().play(requireContext(), tracks.get(0), tracks, 0);
                startActivity(new Intent(requireContext(), PlayerActivity.class));
            }
        });
        b.btnShuffle.setOnClickListener(v -> {
            PlayerManager pm = PlayerManager.getInstance();
            pm.setShuffleEnabled(!pm.isShuffleEnabled());
            renderShuffle();
        });
        renderShuffle();

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(LikedTracksViewModel.class);
        vm.tracks().observe(getViewLifecycleOwner(), data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            renderSubtitle();
            renderEmptyState();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Bỏ like ở Player/TrackMenu nơi khác → quay lại đây danh sách phải cập nhật.
        vm.refresh();
        renderShuffle();
    }

    /** Nút shuffle chỉ bật/tắt chế độ trộn của PlayerManager: xanh = bật, xám = tắt. */
    private void renderShuffle() {
        boolean on = PlayerManager.getInstance().isShuffleEnabled();
        int color = ContextCompat.getColor(requireContext(),
            on ? R.color.spotify_green : R.color.text_secondary);
        b.btnShuffle.setColorFilter(color);
    }

    private void renderSubtitle() {
        String user = TokenManager.getUsername(requireContext());
        b.tvSubtitle.setText(getString(R.string.liked_songs_owner_subtitle, user, tracks.size()));
    }

    private void renderEmptyState() {
        boolean empty = tracks.isEmpty();
        b.rowActions.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** Menu 3 chấm cho 1 bài đã thích. Không có "Xóa khỏi danh sách phát" (playlist ảo). */
    private void showTrackMenu(Track track, int position) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                vm.toggleLike(t.getTrackId());
            }
            @Override public void onAddToPlaylist(Track t) {
                openAddToPlaylist(t);
            }
            @Override public void onGoToAlbum(Track t) {
                if (t.getAlbum() == null || t.getAlbum().getAlbumId() == null) return;
                NavHelper.openAlbum(requireContext(), t.getAlbum().getAlbumId());
            }
        });
        sheet.show(getChildFragmentManager(), "track_menu");
    }

    private void openAddToPlaylist(Track track) {
        if (track.getTrackId() == null) return;
        AddToPlaylistBottomSheet sheet = AddToPlaylistBottomSheet.newInstance(
            track.getTrackId(), track.getTitle());
        sheet.setCallback(new AddToPlaylistBottomSheet.Callback() {
            @Override public void onSaved(long trackId, boolean isInAnyPlaylist) { /* no-op */ }
            @Override public void onPlaylistCreated(Playlist playlist) {
                NavHelper.openPlaylist(requireContext(), playlist.getPlaylistId(), playlist.getName());
            }
        });
        sheet.show(getChildFragmentManager(), "add_to_playlist");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
