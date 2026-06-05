package com.example.musicstreamingapp.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.musicstreamingapp.PlaylistCoverPickerActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.SheetPlaylistEditBinding;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.viewmodel.PlaylistDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;

public class PlaylistEditBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PLAYLIST = "playlist";

    private SheetPlaylistEditBinding b;
    private Playlist playlist;
    private PlaylistDetailViewModel vm;

    public static PlaylistEditBottomSheet newInstance(Playlist playlist) {
        PlaylistEditBottomSheet f = new PlaylistEditBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLAYLIST, playlist);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Serializable s = getArguments().getSerializable(ARG_PLAYLIST);
            if (s instanceof Playlist) playlist = (Playlist) s;
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = SheetPlaylistEditBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Chia sẻ ViewModel với host: nếu sheet được mở bởi PlaylistDetailFragment (qua
        // childFragmentManager) thì scope theo fragment đó; nếu mở bởi Activity thì theo Activity.
        ViewModelStoreOwner owner = getParentFragment() != null ? getParentFragment() : requireActivity();
        vm = new ViewModelProvider(owner, new VmFactory(requireContext()))
            .get(PlaylistDetailViewModel.class);

        if (playlist != null) {
            b.etName.setText(playlist.getName() != null ? playlist.getName() : "");
            b.etDescription.setText(playlist.getDescription() != null ? playlist.getDescription() : "");
            b.swPrivate.setChecked(Boolean.FALSE.equals(playlist.getPublic()));
            b.coverPreview.bind(playlist.getCoverUrl(), null);
        }

        b.btnCancel.setOnClickListener(v -> dismiss());

        b.coverPreviewContainer.setOnClickListener(v -> {
            // Mở cover picker trực tiếp (không phụ thuộc host là Activity hay Fragment).
            if (playlist != null && playlist.getPlaylistId() != null) {
                Intent i = new Intent(requireContext(), PlaylistCoverPickerActivity.class);
                i.putExtra("playlistId", playlist.getPlaylistId());
                startActivity(i);
            }
            dismiss();
        });

        b.rowPrivacy.setOnClickListener(v -> b.swPrivate.setChecked(!b.swPrivate.isChecked()));

        b.rowDelete.setOnClickListener(v -> confirmDelete());

        b.btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String newName = b.etName.getText() == null ? "" : b.etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Snackbar.make(b.getRoot(), R.string.playlist_name, Snackbar.LENGTH_SHORT).show();
            return;
        }
        boolean isPublic = !b.swPrivate.isChecked();
        String description = b.etDescription.getText() == null ? "" : b.etDescription.getText().toString().trim();
        vm.updateDetails(newName, isPublic, description);
        dismiss();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
            .setMessage(R.string.delete_playlist_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete, (d, w) -> {
                vm.deletePlaylist();
                dismiss();
            })
            .show();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
