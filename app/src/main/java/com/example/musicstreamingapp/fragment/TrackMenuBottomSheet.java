package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.ArtistDetailActivity;
import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.SubscriptionRepository;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TrackMenuBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onLike(Track track);
        void onAddToPlaylist(Track track);
        void onGoToAlbum(Track track);
        /** Chỉ dùng khi mở từ trong 1 playlist (showRemove = true). Mặc định không làm gì. */
        default void onRemoveFromPlaylist(Track track) {}
    }

    private static final String ARG_TRACK = "track";
    private static final String ARG_SHOW_REMOVE = "show_remove";
    private Listener listener;

    public static TrackMenuBottomSheet newInstance(Track track) {
        return newInstance(track, false);
    }

    /** showRemove = true khi mở menu từ màn hình chi tiết playlist (hiện "Xóa khỏi danh sách phát này"). */
    public static TrackMenuBottomSheet newInstance(Track track, boolean showRemove) {
        TrackMenuBottomSheet sheet = new TrackMenuBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRACK, track);
        args.putBoolean(ARG_SHOW_REMOVE, showRemove);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_track_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Track track = (Track) requireArguments().getSerializable(ARG_TRACK);
        if (track == null) { dismiss(); return; }
        boolean showRemove = requireArguments().getBoolean(ARG_SHOW_REMOVE, false);

        ImageView cover = view.findViewById(R.id.iv_track_cover);
        TextView tvTitle = view.findViewById(R.id.tv_track_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_track_subtitle);

        tvTitle.setText(track.getTitle() != null ? track.getTitle() : "");
        tvSubtitle.setText(buildSubtitle(track));

        if (track.getCoverUrl() != null) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + track.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(cover);
        }

        view.findViewById(R.id.item_like).setOnClickListener(v -> {
            if (listener != null) listener.onLike(track);
            dismiss();
        });

        view.findViewById(R.id.item_add_playlist).setOnClickListener(v -> {
            if (listener != null) listener.onAddToPlaylist(track);
            dismiss();
        });

        // Nghe nhạc không quảng cáo → mở trang Premium. Ẩn dòng này nếu đã có Premium.
        View premiumItem = view.findViewById(R.id.item_premium);
        premiumItem.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class));
            dismiss();
        });
        hidePremiumRowIfSubscribed(premiumItem);

        // Xóa khỏi danh sách phát này — chỉ hiện khi mở từ trong playlist.
        View removeItem = view.findViewById(R.id.item_remove);
        if (showRemove) {
            removeItem.setVisibility(View.VISIBLE);
            removeItem.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveFromPlaylist(track);
                dismiss();
            });
        } else {
            removeItem.setVisibility(View.GONE);
        }

        // Chuyển đến album.
        View goAlbumItem = view.findViewById(R.id.item_go_album);
        Album album = track.getAlbum();
        if (album != null && album.getAlbumId() != null) {
            goAlbumItem.setOnClickListener(v -> {
                if (listener != null) listener.onGoToAlbum(track);
                dismiss();
            });
        } else {
            goAlbumItem.setVisibility(View.GONE);
        }

        // Chuyển tới trang nghệ sĩ.
        View goArtistItem = view.findViewById(R.id.item_go_artist);
        Artist artist = track.getArtist();
        if (artist != null && artist.getArtistId() != null) {
            goArtistItem.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), ArtistDetailActivity.class);
                i.putExtra("artistId", artist.getArtistId());
                startActivity(i);
                dismiss();
            });
        } else {
            goArtistItem.setVisibility(View.GONE);
        }
    }

    /** Hỏi backend gói hiện tại; nếu đã Premium thì ẩn dòng "Nghe nhạc không quảng cáo". */
    private void hidePremiumRowIfSubscribed(View premiumItem) {
        SubscriptionRepository repo = new SubscriptionRepository(
            RetrofitClient.getApiService(TokenManager.getPrefs(requireContext())));
        repo.getMine(new RepoCallback<Subscription>() {
            @Override public void onSuccess(Subscription data) {
                if (data != null && data.isPremium() && isAdded()) {
                    premiumItem.setVisibility(View.GONE);
                }
            }
            @Override public void onError(String message) { /* giữ nguyên dòng nếu lỗi mạng */ }
        });
    }

    private static String buildSubtitle(Track track) {
        String artist = track.getArtist() != null ? track.getArtist().getName() : "";
        String album = track.getAlbum() != null ? track.getAlbum().getTitle() : "";
        if (artist.isEmpty()) return album;
        if (album.isEmpty()) return artist;
        return artist + " • " + album;
    }
}
