package com.example.musicstreamingapp;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.databinding.ActivityEditProfileBinding;
import com.example.musicstreamingapp.viewmodel.EditProfileViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_DISPLAY_NAME = "displayName";
    public static final String EXTRA_BIO = "bio";
    public static final String EXTRA_AVATAR = "avatarUrl";

    private ActivityEditProfileBinding b;
    private EditProfileViewModel vm;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> { if (uri != null) handlePickedUri(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        b.etDisplayName.setText(getIntent().getStringExtra(EXTRA_DISPLAY_NAME));
        b.etBio.setText(getIntent().getStringExtra(EXTRA_BIO));

        vm = new ViewModelProvider(this, new VmFactory(this)).get(EditProfileViewModel.class);
        vm.setInitialAvatar(getIntent().getStringExtra(EXTRA_AVATAR));

        b.ivAvatar.setOnClickListener(v -> pickImage.launch("image/*"));
        b.tvChangePhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        b.btnSave.setOnClickListener(v -> vm.onSaveClicked(
            b.etDisplayName.getText() == null ? "" : b.etDisplayName.getText().toString(),
            b.etBio.getText() == null ? "" : b.etBio.getText().toString()));

        observeViewModel();
    }

    private void observeViewModel() {
        vm.avatarUrl().observe(this, url -> {
            if (url == null || url.isEmpty()) return;
            Glide.with(this)
                .load(MainActivity.buildMediaUrl(url))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(b.ivAvatar);
        });
        vm.uploading().observe(this, up -> b.ivAvatar.setEnabled(!Boolean.TRUE.equals(up)));
        vm.saving().observe(this, sv -> b.btnSave.setEnabled(!Boolean.TRUE.equals(sv)));
        vm.message().observe(this, e -> e.consume(this::showMessage));
        vm.saveDone().observe(this, e -> e.consume(ok -> finish()));
    }

    private void showMessage(EditProfileViewModel.Msg msg) {
        int resId;
        switch (msg) {
            case AVATAR_UPLOADED: resId = R.string.profile_avatar_uploaded; break;
            case AVATAR_FAILED:   resId = R.string.profile_avatar_failed;   break;
            case NETWORK_ERROR:   resId = R.string.error_network;           break;
            case SAVE_FAILED:
            default:
                Snackbar.make(b.getRoot(), "Lưu thất bại", Snackbar.LENGTH_SHORT).show();
                return;
        }
        Snackbar.make(b.getRoot(), resId, Snackbar.LENGTH_SHORT).show();
    }

    private void handlePickedUri(Uri uri) {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("openInputStream returned null");
            bytes = readAll(in);
        } catch (IOException e) {
            Snackbar.make(b.getRoot(), R.string.profile_avatar_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String mime = getContentResolver().getType(uri);
        vm.onAvatarPicked(bytes, mime);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
