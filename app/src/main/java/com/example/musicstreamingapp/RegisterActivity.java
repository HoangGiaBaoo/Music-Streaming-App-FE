package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.model.RegisterRequest;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etEmail, etPassword;
    private Button btnRegister;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        root = findViewById(android.R.id.content);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        TextView tvLogin = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> doRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String user = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Snackbar.make(root, "Vui lòng nhập đầy đủ thông tin", Snackbar.LENGTH_SHORT).show();
            return;
        }
        btnRegister.setEnabled(false);

        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.register(new RegisterRequest(user, email, pass)).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> call,
                                             Response<Map<String, String>> response) {
                btnRegister.setEnabled(true);
                if (response.isSuccessful()) {
                    Snackbar.make(root, "Đăng ký thành công! Vui lòng đăng nhập.",
                        Snackbar.LENGTH_SHORT).show();
                    new android.os.Handler().postDelayed(() -> {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }, 1200);
                } else {
                    Snackbar.make(root, parseError(response), Snackbar.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                btnRegister.setEnabled(true);
                Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() == null) return "Đăng ký thất bại (HTTP " + response.code() + ")";
            String body = response.errorBody().string();
            if (body == null || body.isEmpty()) return "Đăng ký thất bại (HTTP " + response.code() + ")";
            JsonObject obj = new Gson().fromJson(body, JsonObject.class);
            if (obj != null) {
                if (obj.has("error") && !obj.get("error").isJsonNull()) return obj.get("error").getAsString();
                if (obj.has("message") && !obj.get("message").isJsonNull()) return obj.get("message").getAsString();
            }
        } catch (Exception ignore) { }
        return "Đăng ký thất bại (HTTP " + response.code() + ")";
    }
}
