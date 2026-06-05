package com.example.musicstreamingapp.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.adapter.AddTrackAdapter;
import com.example.musicstreamingapp.viewmodel.AddTracksViewModel;
import com.example.musicstreamingapp.viewmodel.AddTracksViewModel.Tab;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.widget.Toast;

/**
 * Bottom sheet "Thêm vào danh sách phát" — mở từ nút "Thêm" ở màn playlist.
 * 3 tab + ô tìm kiếm; nhấn + để thêm bài vào playlist hiện tại.
 */
public class AddTracksBottomSheet extends BottomSheetDialogFragment {

    public interface Callback { void onTracksChanged(); }

    private static final String ARG_PLAYLIST_ID = "playlistId";

    private AddTracksViewModel vm;
    private AddTrackAdapter adapter;
    private Callback callback;

    private TextView chipRecommended, chipRecent, chipLiked;

    public static AddTracksBottomSheet newInstance(long playlistId) {
        AddTracksBottomSheet sheet = new AddTracksBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setCallback(Callback cb) { this.callback = cb; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_tracks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long playlistId = requireArguments().getLong(ARG_PLAYLIST_ID, -1);

        chipRecommended = view.findViewById(R.id.chip_recommended);
        chipRecent = view.findViewById(R.id.chip_recent);
        chipLiked = view.findViewById(R.id.chip_liked);
        TextView tvHeader = view.findViewById(R.id.tv_header);
        ProgressBar progress = view.findViewById(R.id.progress_bar);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        EditText etSearch = view.findViewById(R.id.et_search);
        RecyclerView rv = view.findViewById(R.id.rv_tracks);

        adapter = new AddTrackAdapter(track -> vm.toggleTrack(track));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        chipRecommended.setOnClickListener(v -> vm.selectTab(Tab.RECOMMENDED));
        chipRecent.setOnClickListener(v -> vm.selectTab(Tab.RECENT));
        chipLiked.setOnClickListener(v -> vm.selectTab(Tab.LIKED));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { vm.filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        vm = new ViewModelProvider(this, new VmFactory(requireContext())).get(AddTracksViewModel.class);
        vm.setPlaylistId(playlistId);

        vm.activeTab().observe(getViewLifecycleOwner(), this::renderChips);
        vm.displayTracks().observe(getViewLifecycleOwner(), tracks -> {
            adapter.setTracks(tracks);
            tvEmpty.setVisibility(tracks == null || tracks.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.addedIds().observe(getViewLifecycleOwner(), ids -> adapter.setAddedIds(ids));
        vm.loading().observe(getViewLifecycleOwner(), loading ->
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        vm.showHeader().observe(getViewLifecycleOwner(), show ->
            tvHeader.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        vm.toast().observe(getViewLifecycleOwner(), e -> e.consume(msg ->
            Toast.makeText(requireContext(), R.string.add_track_added, Toast.LENGTH_SHORT).show()));

        vm.loadIfNeeded();
    }

    private void renderChips(Tab active) {
        styleChip(chipRecommended, active == Tab.RECOMMENDED);
        styleChip(chipRecent, active == Tab.RECENT);
        styleChip(chipLiked, active == Tab.LIKED);
    }

    private void styleChip(TextView chip, boolean selected) {
        int bg = selected ? R.color.spotify_green : R.color.chip_idle;
        int text = selected ? R.color.black : R.color.accent_white;
        chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bg)));
        chip.setTextColor(ContextCompat.getColor(requireContext(), text));
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        sheet.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.92f);
        BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callback != null && vm != null && vm.hasChanges()) callback.onTracksChanged();
    }
}
