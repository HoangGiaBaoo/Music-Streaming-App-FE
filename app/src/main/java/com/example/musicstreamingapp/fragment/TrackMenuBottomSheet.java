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
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TrackMenuBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onLike(Track track);
        void onAddToPlaylist(Track track);
        void onGoToAlbum(Track track);
    }

    private static final String ARG_TRACK = "track";
    private Listener listener;

    public static TrackMenuBottomSheet newInstance(Track track) {
        TrackMenuBottomSheet sheet = new TrackMenuBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRACK, track);
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
    }

    private static String buildSubtitle(Track track) {
        String artist = track.getArtist() != null ? track.getArtist().getName() : "";
        String album = track.getAlbum() != null ? track.getAlbum().getTitle() : "";
        if (artist.isEmpty()) return album;
        if (album.isEmpty()) return artist;
        return artist + " • " + album;
    }
}
