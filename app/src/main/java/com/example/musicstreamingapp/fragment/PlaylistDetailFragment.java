package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicstreamingapp.EditPlaylistActivity;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.PlaylistCoverPickerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.SuggestedTrackAdapter;
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.FragmentPlaylistDetailBinding;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.NavHelper;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.PremiumChecker;
import com.example.musicstreamingapp.util.ShuffleController;
import com.example.musicstreamingapp.viewmodel.PlaylistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/** Bản Fragment của màn Playlist (xem {@link AlbumDetailFragment} cho ghi chú kiến trúc). */
public class PlaylistDetailFragment extends BaseDetailFragment {

    private static final String ARG_PLAYLIST_ID = "playlistId";
    private static final String ARG_NAME = "playlistName";

    public static PlaylistDetailFragment newInstance(long playlistId, String name) {
        PlaylistDetailFragment f = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        args.putString(ARG_NAME, name);
        f.setArguments(args);
        return f;
    }

    private FragmentPlaylistDetailBinding b;
    private PlaylistDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private final List<Track> suggestions = new ArrayList<>();
    private TrackDetailAdapter adapter;
    private SuggestedTrackAdapter suggestionAdapter;
    private boolean shimmerHidden = false;
    private long playlistId = -1;

    private SubscriptionViewModel subVm;
    private ShuffleController shuffle;

    @Nullable private Playlist currentPlaylist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playlistId = getArguments() != null ? getArguments().getLong(ARG_PLAYLIST_ID, -1) : -1;
        String name = getArguments() != null ? getArguments().getString(ARG_NAME) : null;

        b.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        if (name != null) b.collapsingToolbar.setTitle(name);

        b.rvTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(requireContext(), track, tracks, pos);
            startActivity(new Intent(requireContext(), PlayerActivity.class));
        });
        adapter.setMoreListener(this::showTrackMenu);
        b.rvTracks.setAdapter(adapter);

        b.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionAdapter = new SuggestedTrackAdapter(suggestions, (track, pos) -> vm.addSuggestion(track));
        b.rvSuggestions.setAdapter(suggestionAdapter);

        b.btnPlayFab.setOnClickListener(v -> playFromStart());
        b.btnShuffle.setOnClickListener(v -> shuffle.onShuffleClicked());

        b.coverView.setOnClickListener(v -> openCoverPicker());
        b.btnAdd.setOnClickListener(v -> openAddTracks());
        b.btnEdit.setOnClickListener(v -> openEditTracks());
        b.btnDetails.setOnClickListener(v -> openEditSheet());

        b.btnDownload.setOnClickListener(v -> showDownloadGate());
        b.btnMore.setOnClickListener(v -> showPlaylistMenu());

        // ViewModel tạo nhẹ (chưa gọi mạng) để listener/onResume dùng được ngay.
        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(PlaylistDetailViewModel.class);
        vm.setPlaylistId(playlistId);
        subVm = new ViewModelProvider(this, new VmFactory(requireContext())).get(SubscriptionViewModel.class);
        shuffle = new ShuffleController(getChildFragmentManager(), on ->
            b.btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(),
                on ? R.color.spotify_green : R.color.text_secondary)));

        scheduleDeferredSetup();
    }

    /** Việc nặng: shimmer + observe + gọi mạng + ảnh bìa/palette — hoãn tới sau khi trượt xong. */
    @Override
    protected void onEnterAnimationDone() {
        b.shimmerPlaylist.getRoot().startShimmer();
        b.shimmerPlaylist.getRoot().postDelayed(this::hideShimmer, 4000);

        observeViewModel();
        vm.loadIfNeeded();

        // Trạng thái Premium quyết định việc bật/tắt trộn bài (Free bị ép bật).
        subVm.subscription().observe(getViewLifecycleOwner(), sub ->
            shuffle.setPremium(PremiumChecker.isPremium(sub)));
        subVm.loadCurrentSubscription();
    }

    private void observeViewModel() {
        vm.playlist().observe(getViewLifecycleOwner(), this::renderPlaylist);
        vm.tracks().observe(getViewLifecycleOwner(), data -> {
            tracks.clear();
            if (data != null) tracks.addAll(data);
            adapter.notifyDataSetChanged();
            renderCover();
            renderEmptyState();
            if (data != null) hideShimmer();
        });
        vm.suggestions().observe(getViewLifecycleOwner(), data -> {
            suggestions.clear();
            if (data != null) suggestions.addAll(data);
            suggestionAdapter.notifyDataSetChanged();
        });
        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg -> {
            hideShimmer();
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        }));
        vm.editResult().observe(getViewLifecycleOwner(), e -> e.consume(this::onEditResult));
    }

    private void onEditResult(PlaylistDetailViewModel.EditResult result) {
        switch (result) {
            case UPDATED:
                Snackbar.make(b.getRoot(), R.string.playlist_updated, Snackbar.LENGTH_SHORT).show();
                break;
            case DELETED:
                getParentFragmentManager().popBackStack();
                break;
            case UPDATE_FAILED:
            case DELETE_FAILED:
                Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // After returning from the cover picker, refresh to pick up new coverUrl.
        if (currentPlaylist != null) vm.reload();
    }

    private void renderEmptyState() {
        boolean empty = tracks.isEmpty();
        b.rowActions.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) vm.loadSuggestionsIfNeeded();
    }

    private void renderPlaylist(Playlist p) {
        if (p == null) return;
        currentPlaylist = p;
        if (p.getName() != null) b.collapsingToolbar.setTitle(p.getName());
        renderCover();
    }

    private void renderCover() {
        String coverUrl = currentPlaylist != null ? currentPlaylist.getCoverUrl() : null;
        b.coverView.bind(coverUrl, tracks);
        applyPaletteGradient();
    }

    /**
     * Lấy màu chủ đạo của ảnh bìa (hoặc bìa bài đầu) và áp làm gradient trên app bar, giống màn
     * Player. Nếu không có bìa dùng được, nền tối mặc định trong XML giữ nguyên.
     */
    private void applyPaletteGradient() {
        String url = pickPaletteUrl();
        if (url == null) return;
        Glide.with(this).asBitmap()
            .load(RetrofitClient.BASE_MEDIA_URL + url)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource,
                                            @Nullable Transition<? super Bitmap> t) {
                    Palette.from(resource).generate(palette -> {
                        if (palette == null || b == null) return;
                        int top = palette.getDarkVibrantColor(palette.getDarkMutedColor(0xFF1E1E1E));
                        int bottom = 0xFF121212;
                        GradientDrawable gd = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
                        b.appBar.setBackground(gd);
                    });
                }
                @Override
                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
            });
    }

    @Nullable
    private String pickPaletteUrl() {
        if (currentPlaylist != null) {
            String url = currentPlaylist.getCoverUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        for (Track t : tracks) {
            String url = t.getCoverUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }

    /** Mở cover picker (từ ảnh bìa hoặc từ nút trong bottom sheet sửa chi tiết). */
    public void openCoverPicker() {
        if (currentPlaylist == null) return;
        Intent i = new Intent(requireContext(), PlaylistCoverPickerActivity.class);
        i.putExtra("playlistId", currentPlaylist.getPlaylistId());
        startActivity(i);
    }

    /** Mở bottom sheet sửa tên/quyền riêng tư (nút "Tên và thông tin chi tiết"). */
    public void openEditSheet() {
        if (currentPlaylist == null) return;
        PlaylistEditBottomSheet.newInstance(currentPlaylist)
            .show(getChildFragmentManager(), "edit_playlist");
    }

    /** Mở bottom sheet "Thêm vào danh sách phát" (nút Thêm); refresh playlist khi đóng. */
    private void openAddTracks() {
        if (playlistId < 0) return;
        AddTracksBottomSheet sheet = AddTracksBottomSheet.newInstance(playlistId);
        sheet.setCallback(() -> vm.reload());
        sheet.show(getChildFragmentManager(), "add_tracks");
    }

    /** Mở màn "Chỉnh sửa danh sách phát" (nút Chỉnh sửa). */
    private void openEditTracks() {
        if (playlistId < 0) return;
        Intent i = new Intent(requireContext(), EditPlaylistActivity.class);
        i.putExtra("playlistId", playlistId);
        startActivity(i);
    }

    /** Tải về luôn mở gate Premium (bản học tập chưa tải file thật). */
    private void showDownloadGate() {
        String name = currentPlaylist != null && currentPlaylist.getName() != null
            ? currentPlaylist.getName() : "";
        String cover = currentPlaylist != null ? currentPlaylist.getCoverUrl() : null;
        DownloadGateBottomSheet.newInstance(name, cover).show(getChildFragmentManager(), "download_gate");
    }

    /** Menu 3 chấm của playlist (chỉ các chức năng đã làm được). */
    private void showPlaylistMenu() {
        if (currentPlaylist == null) return;
        PlaylistMenuBottomSheet sheet = PlaylistMenuBottomSheet.newInstance(
            currentPlaylist.getName(), currentPlaylist.getCoverUrl());
        sheet.setListener(new PlaylistMenuBottomSheet.Listener() {
            @Override public void onDownload() { showDownloadGate(); }
            @Override public void onAddTracks() { openAddTracks(); }
            @Override public void onEditTracks() { openEditTracks(); }
            @Override public void onEditDetails() { openEditSheet(); }
            @Override public void onCreateCover() { openCoverPicker(); }
            @Override public void onDelete() { confirmDeletePlaylist(); }
        });
        sheet.show(getChildFragmentManager(), "playlist_menu");
    }

    /** Hỏi xác nhận trước khi xóa playlist (thao tác không hoàn tác). */
    private void confirmDeletePlaylist() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_playlist_confirm_title)
            .setMessage(R.string.delete_playlist_confirm_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete, (d, w) -> vm.deletePlaylist())
            .show();
    }

    /** Nút Play: phát từ đầu (hoặc ngẫu nhiên nếu đang bật trộn bài). */
    private void playFromStart() {
        if (tracks.isEmpty()) return;
        int start = shuffle.startIndex(tracks.size());
        PlayerManager.getInstance().play(requireContext(), tracks.get(start), tracks, start);
        startActivity(new Intent(requireContext(), PlayerActivity.class));
    }

    /** Mở menu 3 chấm cho 1 bài trong playlist (kèm "Xóa khỏi danh sách phát này"). */
    private void showTrackMenu(Track track, int position) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track, true);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                vm.likeTrack(t.getTrackId());
                Snackbar.make(b.getRoot(), "Đã thêm vào Bài hát đã thích", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onAddToPlaylist(Track t) {
                openAddToPlaylist(t);
            }
            @Override public void onGoToAlbum(Track t) {
                if (t.getAlbum() == null || t.getAlbum().getAlbumId() == null) return;
                NavHelper.openAlbum(requireContext(), t.getAlbum().getAlbumId());
            }
            @Override public void onRemoveFromPlaylist(Track t) {
                vm.onRemoveTrack(t, position);
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

    private void hideShimmer() {
        if (shimmerHidden || b == null) return;
        shimmerHidden = true;
        b.shimmerPlaylist.getRoot().stopShimmer();
        b.shimmerPlaylist.getRoot().setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
