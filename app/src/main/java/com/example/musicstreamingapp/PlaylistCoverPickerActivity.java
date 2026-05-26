package com.example.musicstreamingapp;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.databinding.ActivityPlaylistCoverPickerBinding;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PlaylistCoverPickerActivity extends AppCompatActivity {

    private ActivityPlaylistCoverPickerBinding b;
    private long playlistId;
    private LibraryRepository repo;
    private boolean uploading = false;

    private final ActivityResultLauncher<PickVisualMediaRequest> picker =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri == null) return;
            handlePickedUri(uri);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPlaylistCoverPickerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        playlistId = getIntent().getLongExtra("playlistId", -1);
        repo = new LibraryRepository(
            RetrofitClient.getApiService(TokenManager.getPrefs(getApplicationContext())));

        b.coverPreview.bind(null, null);

        b.btnClose.setOnClickListener(v -> finish());
        b.btnChangeCover.setOnClickListener(v -> picker.launch(
            new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()
        ));
    }

    private void handlePickedUri(Uri uri) {
        if (uploading || playlistId < 0) return;
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("openInputStream returned null");
            bytes = readAll(in);
        } catch (IOException e) {
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Preview the picked image immediately while uploading.
        ((android.widget.ImageView) b.coverPreview.findViewById(R.id.cover_single))
            .setImageURI(uri);

        String mime = getContentResolver().getType(uri);
        uploading = true;
        repo.uploadPlaylistCover(playlistId, bytes, mime, new RepoCallback<String>() {
            @Override public void onSuccess(String coverUrl) {
                uploading = false;
                runOnUiThread(() -> {
                    Snackbar.make(b.getRoot(), R.string.playlist_updated, Snackbar.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override public void onError(String message) {
                uploading = false;
                runOnUiThread(() ->
                    Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show());
            }
        });
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
