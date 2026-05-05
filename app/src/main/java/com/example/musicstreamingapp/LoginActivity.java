package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.model.JwtResponse;
import com.example.musicstreamingapp.model.LoginRequest;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_RETURN_RESULT = "returnResult";

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private View root;
    private boolean returnResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        returnResult = getIntent().getBooleanExtra(EXTRA_RETURN_RESULT, false);
        root = findViewById(android.R.id.content);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_register_link);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void doLogin() {
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            Snackbar.make(root, "Vui lòng nhập đầy đủ thông tin", Snackbar.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);

        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        api.login(new LoginRequest(user, pass)).enqueue(new Callback<JwtResponse>() {
            @Override public void onResponse(Call<JwtResponse> call, Response<JwtResponse> response) {
                btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    JwtResponse jwt = response.body();
                    TokenManager.saveAuth(LoginActivity.this,
                        jwt.getToken(), jwt.getUsername(), jwt.getRole());
                    new AccountStore(LoginActivity.this).addOrUpdate(
                        jwt.getUsername(), jwt.getToken(), jwt.getUsername(), null);
                    RetrofitClient.reset();
                    if (returnResult) {
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                } else {
                    try {
                        String errJson = response.errorBody().string();
                        JsonObject obj = new Gson().fromJson(errJson, JsonObject.class);
                        Snackbar.make(root, obj.get("error").getAsString(), Snackbar.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Snackbar.make(root, R.string.error_login, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onFailure(Call<JwtResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
