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
import com.example.musicstreamingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Menu 3 chấm của màn Playlist. Chỉ giữ các chức năng đã làm được:
 * tải xuống (gate Premium), thêm bài, chỉnh sửa, tên &amp; chi tiết, tạo ảnh bìa, xóa playlist.
 */
public class PlaylistMenuBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onDownload();
        void onAddTracks();
        void onEditTracks();
        void onEditDetails();
        void onCreateCover();
        void onDelete();
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_COVER = "cover";

    private Listener listener;

    public static PlaylistMenuBottomSheet newInstance(String title, @Nullable String coverUrl) {
        PlaylistMenuBottomSheet sheet = new PlaylistMenuBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_COVER, coverUrl);
        sheet.setArguments(args);
        return sheet;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_playlist_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";
        String coverUrl = getArguments() != null ? getArguments().getString(ARG_COVER) : null;

        ((TextView) view.findViewById(R.id.tv_title)).setText(title);
        ImageView cover = view.findViewById(R.id.iv_cover);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + coverUrl)
                .placeholder(R.drawable.placeholder_gradient)
                .into(cover);
        }

        wire(view, R.id.item_download, () -> { if (listener != null) listener.onDownload(); });
        wire(view, R.id.item_add_tracks, () -> { if (listener != null) listener.onAddTracks(); });
        wire(view, R.id.item_edit_tracks, () -> { if (listener != null) listener.onEditTracks(); });
        wire(view, R.id.item_details, () -> { if (listener != null) listener.onEditDetails(); });
        wire(view, R.id.item_cover, () -> { if (listener != null) listener.onCreateCover(); });
        wire(view, R.id.item_delete, () -> { if (listener != null) listener.onDelete(); });
    }

    private void wire(View root, int id, Runnable action) {
        root.findViewById(id).setOnClickListener(v -> {
            action.run();
            dismiss();
        });
    }
}
