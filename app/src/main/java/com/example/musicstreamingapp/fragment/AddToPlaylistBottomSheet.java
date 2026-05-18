package com.example.musicstreamingapp.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.PlaylistPickerAdapter;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.viewmodel.AddToPlaylistViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class AddToPlaylistBottomSheet extends BottomSheetDialogFragment {

    public interface Callback {
        void onSaved(long trackId, boolean isInAnyPlaylist);
        void onPlaylistCreated(Playlist playlist);
    }

    private static final String ARG_TRACK_ID = "trackId";
    private static final String ARG_TRACK_TITLE = "trackTitle";

    private AddToPlaylistViewModel vm;
    private PlaylistPickerAdapter adapter;
    private Callback callback;

    public static AddToPlaylistBottomSheet newInstance(long trackId, String trackTitle) {
        AddToPlaylistBottomSheet sheet = new AddToPlaylistBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_TRACK_ID, trackId);
        args.putString(ARG_TRACK_TITLE, trackTitle != null ? trackTitle : "");
        sheet.setArguments(args);
        return sheet;
    }

    public void setCallback(Callback cb) {
        this.callback = cb;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_to_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long trackId = requireArguments().getLong(ARG_TRACK_ID);

        adapter = new PlaylistPickerAdapter(
            playlist -> vm.togglePlaylist(playlist),
            () -> vm.clearAllSaved()
        );

        RecyclerView rv = view.findViewById(R.id.rv_playlists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        ProgressBar progress = view.findViewById(R.id.progress_bar);
        View searchContainer = view.findViewById(R.id.search_container);
        EditText etSearch = view.findViewById(R.id.et_search);
        MaterialButton btnDone = view.findViewById(R.id.btn_done);
        MaterialButton btnNewPlaylist = view.findViewById(R.id.btn_new_playlist);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(v -> dismiss());
        btnDone.setOnClickListener(v -> vm.save());
        btnNewPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                vm.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        vm = new ViewModelProvider(this, new VmFactory(requireContext()))
            .get(AddToPlaylistViewModel.class);
        vm.setTrackId(trackId);

        vm.loading().observe(getViewLifecycleOwner(), loading ->
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        vm.showSearch().observe(getViewLifecycleOwner(), show ->
            searchContainer.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));

        vm.displayItems().observe(getViewLifecycleOwner(), items ->
            adapter.setItems(items));

        vm.selectedIds().observe(getViewLifecycleOwner(), ids ->
            adapter.setSelectedIds(ids));

        vm.saveEvent().observe(getViewLifecycleOwner(), e -> e.consume(ok -> {
            if (callback != null) {
                Long tid = vm.getTrackId();
                if (tid != null) callback.onSaved(tid, vm.isInAnyPlaylist());
            }
            dismiss();
        }));

        vm.playlistCreatedEvent().observe(getViewLifecycleOwner(), e -> e.consume(playlist -> {
            if (callback != null) {
                Long tid = vm.getTrackId();
                if (tid != null) callback.onSaved(tid, true);
                callback.onPlaylistCreated(playlist);
            }
            dismiss();
        }));

        vm.loadIfNeeded();
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.88f);
        sheet.getLayoutParams().height = height;
        BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_playlist, null);
        EditText etName = dialogView.findViewById(R.id.et_playlist_name);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_create).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                vm.createPlaylist(name);
                dialog.dismiss();
            }
        });

        etName.setOnEditorActionListener((tv, actionId, event) -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                vm.createPlaylist(name);
                dialog.dismiss();
            }
            return true;
        });

        dialog.show();
        etName.requestFocus();
    }
}
