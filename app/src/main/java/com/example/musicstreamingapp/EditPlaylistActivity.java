package com.example.musicstreamingapp;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicstreamingapp.adapter.EditPlaylistTrackAdapter;
import com.example.musicstreamingapp.databinding.ActivityEditPlaylistBinding;
import com.example.musicstreamingapp.viewmodel.EditPlaylistViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

/**
 * Màn "Chỉnh sửa danh sách phát": nút trừ để xoá bài, tay nắm 3 gạch để kéo sắp xếp,
 * Lưu để lưu thay đổi. Nếu đã thay đổi mà bấm Hủy/back thì hỏi xác nhận trước khi thoát.
 */
public class EditPlaylistActivity extends AppCompatActivity {

    private ActivityEditPlaylistBinding b;
    private EditPlaylistViewModel vm;
    private EditPlaylistTrackAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEditPlaylistBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long playlistId = getIntent().getLongExtra("playlistId", -1);

        adapter = new EditPlaylistTrackAdapter(new EditPlaylistTrackAdapter.Listener() {
            @Override public void onRemove(int position) { adapter.removeAt(position); }
            @Override public void onStartDrag(RecyclerView.ViewHolder vh) { itemTouchHelper.startDrag(vh); }
            @Override public void onListChanged() { /* no-op */ }
        });
        b.rvTracks.setLayoutManager(new LinearLayoutManager(this));
        b.rvTracks.setAdapter(adapter);
        setupDragAndDrop();

        b.btnCancel.setOnClickListener(v -> attemptExit());
        b.btnSave.setOnClickListener(v -> vm.save(adapter.getItems()));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { attemptExit(); }
        });

        vm = new ViewModelProvider(this, new VmFactory(this)).get(EditPlaylistViewModel.class);
        vm.setPlaylistId(playlistId);
        vm.initialTracks().observe(this, tracks -> {
            if (initialized) return;
            initialized = true;
            adapter.setItems(tracks);
        });
        vm.saving().observe(this, saving -> b.btnSave.setEnabled(!Boolean.TRUE.equals(saving)));
        vm.saveResult().observe(this, e -> e.consume(this::onSaveResult));
        vm.loadIfNeeded();
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback cb = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean isLongPressDragEnabled() { return false; }

            @Override public boolean onMove(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(vh.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) { }
        };
        itemTouchHelper = new ItemTouchHelper(cb);
        itemTouchHelper.attachToRecyclerView(b.rvTracks);
    }

    private void onSaveResult(EditPlaylistViewModel.SaveResult result) {
        switch (result) {
            case SAVED:
            case NO_CHANGES:
                setResult(RESULT_OK);
                finish();
                break;
            case FAILED:
                Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    /** Bấm Hủy hoặc back: nếu có thay đổi thì hỏi xác nhận, không thì thoát luôn. */
    private void attemptExit() {
        if (vm != null && vm.hasChanges(adapter.getItems())) {
            showDiscardDialog();
        } else {
            finish();
        }
    }

    private void showDiscardDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_discard_changes, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // "Tiếp tục chỉnh sửa" → đóng dialog, ở lại màn chỉnh sửa.
        view.findViewById(R.id.btn_continue).setOnClickListener(v -> dialog.dismiss());
        // "Hủy" → thoát, bỏ mọi thay đổi.
        view.findViewById(R.id.btn_discard).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        b = null;
    }
}
