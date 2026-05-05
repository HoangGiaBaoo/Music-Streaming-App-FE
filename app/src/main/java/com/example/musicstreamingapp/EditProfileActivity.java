package com.example.musicstreamingapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.model.ProfileUpdateRequest;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    public static final String EXTRA_DISPLAY_NAME = "displayName";
    public static final String EXTRA_BIO = "bio";
    public static final String EXTRA_AVATAR = "avatarUrl";

    private TextInputEditText etDisplayName, etBio;
    private MaterialButton btnSave;
    private CircleImageView ivAvatar;
    private FrameLayout avatarFrame;
    private String currentAvatar;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> { if (uri != null) uploadAvatar(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        etDisplayName = findViewById(R.id.et_display_name);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save);
        ivAvatar = findViewById(R.id.iv_avatar);
        avatarFrame = (FrameLayout) ivAvatar.getParent();

        etDisplayName.setText(getIntent().getStringExtra(EXTRA_DISPLAY_NAME));
        etBio.setText(getIntent().getStringExtra(EXTRA_BIO));
        currentAvatar = getIntent().getStringExtra(EXTRA_AVATAR);

        if (currentAvatar != null && !currentAvatar.isEmpty()) {
            Glide.with(this)
                .load(MainActivity.buildMediaUrl(currentAvatar))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(ivAvatar);
        }

        avatarFrame.setOnClickListener(v -> pickImage.launch("image/*"));
        btnSave.setOnClickListener(v -> save());
    }

    private void uploadAvatar(Uri uri) {
        byte[] bytes;
        String mime;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("openInputStream returned null");
            bytes = readAll(in);
        } catch (IOException e) {
            Snackbar.make(btnSave, R.string.profile_avatar_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        mime = getContentResolver().getType(uri);
        if (mime == null) mime = "image/jpeg";

        RequestBody body = RequestBody.create(bytes, MediaType.parse(mime));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "avatar.jpg", body);

        avatarFrame.setEnabled(false);
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.uploadAvatar(part).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                avatarFrame.setEnabled(true);
                if (r.isSuccessful() && r.body() != null && r.body().get("avatarUrl") != null) {
                    currentAvatar = r.body().get("avatarUrl");
                    Glide.with(EditProfileActivity.this)
                        .load(MainActivity.buildMediaUrl(currentAvatar))
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into(ivAvatar);
                    Snackbar.make(btnSave, R.string.profile_avatar_uploaded, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(btnSave, R.string.profile_avatar_failed, Snackbar.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                avatarFrame.setEnabled(true);
                Snackbar.make(btnSave, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private void save() {
        ProfileUpdateRequest req = new ProfileUpdateRequest(
            etDisplayName.getText() == null ? "" : etDisplayName.getText().toString().trim(),
            etBio.getText() == null ? "" : etBio.getText().toString().trim(),
            currentAvatar
        );
        btnSave.setEnabled(false);
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.updateProfile(req).enqueue(new Callback<UserMe>() {
            @Override public void onResponse(Call<UserMe> c, Response<UserMe> r) {
                btnSave.setEnabled(true);
                if (r.isSuccessful()) finish();
                else Snackbar.make(btnSave, "Lưu thất bại", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<UserMe> c, Throwable t) {
                btnSave.setEnabled(true);
                Snackbar.make(btnSave, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
