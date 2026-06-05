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

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.PlayerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.ArtistAlbumAdapter;
import com.example.musicstreamingapp.adapter.ArtistTrackAdapter;
import com.example.musicstreamingapp.databinding.FragmentArtistDetailBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.NavHelper;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.PremiumChecker;
import com.example.musicstreamingapp.viewmodel.ArtistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bản Fragment của màn Nghệ sĩ (xem {@link AlbumDetailFragment} cho ghi chú kiến trúc). */
public class ArtistDetailFragment extends BaseDetailFragment {

    private static final String ARG_ARTIST_ID = "artistId";

    public static ArtistDetailFragment newInstance(long artistId) {
        ArtistDetailFragment f = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ARTIST_ID, artistId);
        f.setArguments(args);
        return f;
    }

    private FragmentArtistDetailBinding b;
    private ArtistDetailViewModel vm;

    private final List<Album> albums = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();
    private ArtistAlbumAdapter albumAdapter;
    private ArtistTrackAdapter trackAdapter;

    private SubscriptionViewModel subVm;
    private boolean isPremium = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentArtistDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long artistId = getArguments() != null ? getArguments().getLong(ARG_ARTIST_ID, -1) : -1;

        b.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        trackAdapter = new ArtistTrackAdapter(tracks, new ArtistTrackAdapter.Callbacks() {
            @Override public void onTrackClick(Track track) { openPlayer(track); }
            @Override public void onMoreClick(Track track, View anchor) { showTrackMenu(track); }
            @Override public void onInPlaylistClick(Track track) { openAddToPlaylist(track); }
        });
        b.rvTopTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvTopTracks.setAdapter(trackAdapter);

        albumAdapter = new ArtistAlbumAdapter(albums, album ->
            NavHelper.openAlbum(requireContext(), album.getAlbumId()));
        b.rvAlbums.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvAlbums.setAdapter(albumAdapter);

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(ArtistDetailViewModel.class);
        vm.setArtistId(artistId);

        b.btnFollow.setOnClickListener(v -> vm.onFollowClicked());
        b.btnExpandTracks.setOnClickListener(v -> vm.toggleTracksExpanded());
        b.btnPlay.setOnClickListener(v -> playRespectingShuffle());
        b.btnShuffle.setOnClickListener(v -> onShuffleClicked());

        b.btnExpandBio.setOnClickListener(v -> {
            b.tvBio.setMaxLines(Integer.MAX_VALUE);
            b.btnExpandBio.setVisibility(View.GONE);
        });

        // ViewModel tạo nhẹ (chưa gọi mạng); subVm dành cho phần hoãn bên dưới.
        subVm = new ViewModelProvider(this, new VmFactory(requireContext())).get(SubscriptionViewModel.class);
        updateShuffleUi();

        scheduleDeferredSetup();
    }

    /** Việc nặng: observe + gọi mạng + ảnh — hoãn tới sau khi trượt xong. */
    @Override
    protected void onEnterAnimationDone() {
        observeViewModel();
        vm.loadIfNeeded();

        // Premium mới bật/tắt trộn bài được; Free bị ép trộn (chạm shuffle để tắt → gate).
        subVm.subscription().observe(getViewLifecycleOwner(), sub -> {
            isPremium = PremiumChecker.isPremium(sub);
            if (!isPremium) PlayerManager.getInstance().setShuffleEnabled(true);
            updateShuffleUi();
        });
        subVm.loadCurrentSubscription();
    }

    private void observeViewModel() {
        vm.artist().observe(getViewLifecycleOwner(), this::renderArtist);
        vm.albums().observe(getViewLifecycleOwner(), this::renderAlbums);
        vm.visibleTracks().observe(getViewLifecycleOwner(), this::renderTracks);

        vm.following().observe(getViewLifecycleOwner(), isFollowing -> {
            if (Boolean.TRUE.equals(isFollowing)) {
                b.btnFollow.setText(R.string.following_btn);
                b.btnFollow.setStrokeColorResource(R.color.spotify_green);
            } else {
                b.btnFollow.setText(R.string.follow);
                b.btnFollow.setStrokeColorResource(R.color.outline_grey);
            }
        });

        vm.showExpandButton().observe(getViewLifecycleOwner(), show ->
            b.btnExpandTracks.setVisibility(show ? View.VISIBLE : View.GONE));

        vm.tracksExpanded().observe(getViewLifecycleOwner(), expanded ->
            b.btnExpandTracks.setText(Boolean.TRUE.equals(expanded) ? "Ẩn bớt" : "Xem thêm"));

        vm.inPlaylistTrackIds().observe(getViewLifecycleOwner(), ids ->
            trackAdapter.setInPlaylistIds(ids));

        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
    }

    private void renderArtist(Artist a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getName());

        if (a.getAvatarUrl() != null) {
            String url = RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl();
            Glide.with(this).load(url).centerCrop().into(b.ivArtistBanner);
            Glide.with(this).load(url).centerCrop().into(b.ivArtistThumb);
            Glide.with(this).load(url).centerCrop().into(b.ivAboutImage);
        }

        b.tvFollowers.setText(formatFollowers(a.getFollowerCount()));
        b.tvAboutName.setText(a.getName() != null ? a.getName() : "");
        b.tvAboutFollowers.setText(formatFollowers(a.getFollowerCount()));

        String bio = a.getBio();
        if (bio != null && !bio.isEmpty()) {
            b.cardAbout.setVisibility(View.VISIBLE);
            b.tvBio.setText(bio);
            b.tvBio.post(() -> {
                if (b != null && b.tvBio.getLineCount() > 3) b.btnExpandBio.setVisibility(View.VISIBLE);
            });
        }
    }

    private void renderAlbums(List<Album> data) {
        albums.clear();
        if (data != null) albums.addAll(data);
        albumAdapter.notifyDataSetChanged();
        b.tvShowAllAlbums.setVisibility(albums.size() > 3 ? View.VISIBLE : View.GONE);
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        trackAdapter.notifyDataSetChanged();
    }

    private void showTrackMenu(Track track) {
        TrackMenuBottomSheet sheet = TrackMenuBottomSheet.newInstance(track);
        sheet.setListener(new TrackMenuBottomSheet.Listener() {
            @Override public void onLike(Track t) {
                if (t.getTrackId() == null) return;
                vm.likeTrack(t.getTrackId(), null);
                Snackbar.make(b.getRoot(), "Đã thêm vào Bài hát đã thích", Snackbar.LENGTH_SHORT).show();
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
            @Override public void onSaved(long trackId, boolean isInAnyPlaylist) {
                vm.updateTrackInPlaylist(trackId, isInAnyPlaylist);
            }
            @Override public void onPlaylistCreated(Playlist playlist) {
                if (track.getTrackId() != null) vm.updateTrackInPlaylist(track.getTrackId(), true);
                NavHelper.openPlaylist(requireContext(), playlist.getPlaylistId(), playlist.getName());
            }
        });
        sheet.show(getChildFragmentManager(), "add_to_playlist");
    }

    private void openPlayer(Track track) {
        Intent i = new Intent(requireContext(), PlayerActivity.class);
        i.putExtra("track", track);
        startActivity(i);
    }

    /** Nút Play: phát theo trạng thái trộn bài hiện tại (ngẫu nhiên nếu đang bật). Không gate. */
    private void playRespectingShuffle() {
        List<Track> all = vm.getAllTracks();
        if (all.isEmpty()) return;
        int start = PlayerManager.getInstance().isShuffleEnabled()
            ? (int) (Math.random() * all.size()) : 0;
        openPlayer(all.get(start));
    }

    /** Free: chạm shuffle (cố tắt) → gate Premium. Premium: bật/tắt trộn bài thật. */
    private void onShuffleClicked() {
        if (!isPremium) {
            new ShuffleGateBottomSheet().show(getChildFragmentManager(), "shuffle_gate");
            return;
        }
        PlayerManager pm = PlayerManager.getInstance();
        pm.setShuffleEnabled(!pm.isShuffleEnabled());
        updateShuffleUi();
    }

    /** Nút trộn xanh khi bật, xám khi tắt (chỉ Premium mới tắt được). */
    private void updateShuffleUi() {
        boolean on = PlayerManager.getInstance().isShuffleEnabled();
        b.btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(),
            on ? R.color.spotify_green : R.color.text_secondary));
    }

    private static String formatFollowers(Long count) {
        if (count == null || count == 0) return "";
        if (count >= 1_000_000) {
            return String.format(Locale.US, "%.1f Tr người theo dõi", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format(Locale.US, "%.1f N người theo dõi", count / 1_000.0);
        }
        return count + " người theo dõi";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
