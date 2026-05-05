package com.example.musicstreamingapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.PlaylistRequest;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBottomSheet extends BottomSheetDialogFragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.row_playlist).setOnClickListener(v -> showCreatePlaylistDialog());
        view.findViewById(R.id.row_collab).setOnClickListener(v ->
            Toast.makeText(getContext(), R.string.create_coming_soon, Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.row_blend).setOnClickListener(v ->
            Toast.makeText(getContext(), R.string.create_coming_soon, Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.playlist_name);
        input.setTextColor(getResources().getColor(R.color.accent_white));
        new AlertDialog.Builder(requireContext(), R.style.AlertDialogDark)
            .setTitle(R.string.new_playlist)
            .setView(input)
            .setPositiveButton(R.string.create, (d, w) -> createPlaylist(input.getText().toString().trim()))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void createPlaylist(String name) {
        if (name.isEmpty()) return;
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(requireContext()));
        PlaylistRequest body = new PlaylistRequest();
        body.name = name;
        body.isPublic = false;
        api.createPlaylist(body).enqueue(new Callback<Playlist>() {
            @Override public void onResponse(@NonNull Call<Playlist> call,
                                             @NonNull Response<Playlist> response) {
                String msg = response.isSuccessful() ? "Đã tạo playlist!" : "Tạo thất bại";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                dismiss();
            }
            @Override public void onFailure(@NonNull Call<Playlist> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
