package com.example.musicstreamingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Menu 3 chấm của màn Album. Chỉ giữ các chức năng đã làm được:
 * thêm/xóa Thư viện, tải xuống (gate Premium), chuyển tới trang nghệ sĩ.
 */
public class AlbumMenuBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onToggleLibrary();
        void onDownload();
        void onGoToArtist();
    }

    private static final String ARG_ALBUM = "album";
    private static final String ARG_SAVED = "saved";

    private Listener listener;

    public static AlbumMenuBottomSheet newInstance(Album album, boolean saved) {
        AlbumMenuBottomSheet sheet = new AlbumMenuBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ALBUM, album);
        args.putBoolean(ARG_SAVED, saved);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_album_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Album album = (Album) requireArguments().getSerializable(ARG_ALBUM);
        if (album == null) { dismiss(); return; }
        boolean saved = requireArguments().getBoolean(ARG_SAVED, false);

        ImageView cover = view.findViewById(R.id.iv_cover);
        TextView title = view.findViewById(R.id.tv_title);
        TextView subtitle = view.findViewById(R.id.tv_subtitle);

        title.setText(album.getTitle() != null ? album.getTitle() : "");
        subtitle.setText(album.getArtist() != null ? album.getArtist().getName() : "");
        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + album.getCoverUrl())
                .placeholder(R.drawable.placeholder_gradient)
                .into(cover);
        }

        // Trạng thái Thư viện: đã lưu → dấu tích + "Xóa khỏi Thư viện".
        ImageView libIcon = view.findViewById(R.id.iv_library_icon);
        TextView libLabel = view.findViewById(R.id.tv_library_label);
        libIcon.setImageResource(saved ? R.drawable.ic_check_circle_green : R.drawable.ic_add_circle_outline);
        libLabel.setText(saved ? R.string.action_remove_from_library : R.string.action_add_to_library);

        view.findViewById(R.id.item_library).setOnClickListener(v -> {
            if (listener != null) listener.onToggleLibrary();
            dismiss();
        });
        view.findViewById(R.id.item_download).setOnClickListener(v -> {
            if (listener != null) listener.onDownload();
            dismiss();
        });

        View goArtist = view.findViewById(R.id.item_go_artist);
        if (album.getArtist() != null && album.getArtist().getArtistId() != null) {
            goArtist.setOnClickListener(v -> {
                if (listener != null) listener.onGoToArtist();
                dismiss();
            });
        } else {
            goArtist.setVisibility(View.GONE);
        }
    }
}
