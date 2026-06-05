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
import com.example.musicstreamingapp.adapter.TrackDetailAdapter;
import com.example.musicstreamingapp.databinding.FragmentAlbumDetailBinding;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.PlayerManager;
import com.example.musicstreamingapp.util.PremiumChecker;
import com.example.musicstreamingapp.viewmodel.AlbumDetailViewModel;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Bản Fragment của màn chi tiết Album, nạp vào fragment_container của MainActivity để thanh
 * bottom-nav + mini-player ở dưới đứng yên (không dựng lại mỗi lần mở). Logic giữ nguyên từ
 * {@code AlbumDetailActivity}; khác biệt: dùng getViewLifecycleOwner cho observe và toolbar
 * back → popBackStack.
 */
public class AlbumDetailFragment extends BaseDetailFragment {

    private static final String ARG_ALBUM_ID = "albumId";

    public static AlbumDetailFragment newInstance(long albumId) {
        AlbumDetailFragment f = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ALBUM_ID, albumId);
        f.setArguments(args);
        return f;
    }

    private FragmentAlbumDetailBinding b;
    private AlbumDetailViewModel vm;
    private final List<Track> tracks = new ArrayList<>();
    private TrackDetailAdapter adapter;
    private boolean shimmerHidden = false;

    private SubscriptionViewModel subVm;
    private boolean isPremium = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAlbumDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long albumId = getArguments() != null ? getArguments().getLong(ARG_ALBUM_ID, -1) : -1;

        b.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        b.rvTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrackDetailAdapter(tracks, (track, pos) -> {
            PlayerManager.getInstance().play(requireContext(), track, tracks, pos);
            startActivity(new Intent(requireContext(), PlayerActivity.class));
        });
        b.rvTracks.setAdapter(adapter);

        b.btnPlayAll.setOnClickListener(v -> playRespectingShuffle());
        b.btnShuffle.setOnClickListener(v -> onShuffleClicked());

        // Tạo ViewModel (nhẹ, chưa gọi mạng) để listener dùng được ngay.
        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(AlbumDetailViewModel.class);
        vm.setAlbumId(albumId);
        subVm = new ViewModelProvider(this, new VmFactory(requireContext())).get(SubscriptionViewModel.class);
        updateShuffleUi();

        scheduleDeferredSetup();
    }

    /** Việc nặng: bật shimmer + observe + gọi mạng + ảnh bìa — hoãn tới sau khi trượt xong. */
    @Override
    protected void onEnterAnimationDone() {
        b.shimmerAlbum.getRoot().startShimmer();
        b.shimmerAlbum.getRoot().postDelayed(this::hideShimmer, 4000);

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

    /** Nút "Phát tất cả": phát theo trạng thái trộn bài (ngẫu nhiên nếu đang bật). Không gate. */
    private void playRespectingShuffle() {
        if (tracks.isEmpty()) return;
        int start = PlayerManager.getInstance().isShuffleEnabled()
            ? (int) (Math.random() * tracks.size()) : 0;
        PlayerManager.getInstance().play(requireContext(), tracks.get(start), tracks, start);
        startActivity(new Intent(requireContext(), PlayerActivity.class));
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

    /** Nút "Shuffle" chữ xanh khi bật, xám khi tắt (chỉ Premium mới tắt được). */
    private void updateShuffleUi() {
        boolean on = PlayerManager.getInstance().isShuffleEnabled();
        b.btnShuffle.setTextColor(ContextCompat.getColor(requireContext(),
            on ? R.color.spotify_green : R.color.text_secondary));
    }

    private void observeViewModel() {
        vm.album().observe(getViewLifecycleOwner(), this::renderAlbum);
        vm.tracks().observe(getViewLifecycleOwner(), data -> {
            renderTracks(data);
            if (data != null) hideShimmer();
        });
        vm.errorEvent().observe(getViewLifecycleOwner(), e -> e.consume(msg -> {
            hideShimmer();
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        }));
    }

    private void hideShimmer() {
        if (shimmerHidden || b == null) return;
        shimmerHidden = true;
        b.shimmerAlbum.getRoot().stopShimmer();
        b.shimmerAlbum.getRoot().setVisibility(View.GONE);
    }

    private void renderAlbum(Album a) {
        if (a == null) return;
        b.collapsingToolbar.setTitle(a.getTitle());
        if (a.getCoverUrl() != null && !a.getCoverUrl().isEmpty()) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(b.ivCover);
        }
        if (a.getArtist() != null) b.tvArtistName.setText(a.getArtist().getName());
    }

    private void renderTracks(List<Track> data) {
        tracks.clear();
        if (data != null) tracks.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
