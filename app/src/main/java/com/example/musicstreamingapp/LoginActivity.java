package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.databinding.ActivityLoginBinding;
import com.example.musicstreamingapp.util.SessionManager;
import com.example.musicstreamingapp.viewmodel.LoginViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_RETURN_RESULT = "returnResult";

    private ActivityLoginBinding b;
    private LoginViewModel vm;
    private boolean returnResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        SessionManager.init(this);
        returnResult = getIntent().getBooleanExtra(EXTRA_RETURN_RESULT, false);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(LoginViewModel.class);

        b.btnLogin.setOnClickListener(v -> vm.onLoginClicked(
            b.etUsername.getText().toString().trim(),
            b.etPassword.getText().toString().trim()));

        b.tvRegisterLink.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        observeViewModel();
    }

    private void observeViewModel() {
        vm.loading().observe(this, isLoading -> {
            boolean l = Boolean.TRUE.equals(isLoading);
            b.btnLogin.setEnabled(!l);
            b.progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        });
        vm.errorEvent().observe(this, e -> e.consume(this::showError));
        vm.loginSuccess().observe(this, e -> e.consume(ok -> onLoggedIn()));
    }

    private void showError(String code) {
        if ("empty_fields".equals(code)) {
            Snackbar.make(b.getRoot(), "Vui lòng nhập đầy đủ thông tin", Snackbar.LENGTH_SHORT).show();
        } else if ("network_error".equals(code) || code.contains("HTTP")) {
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(b.getRoot(), code, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onLoggedIn() {
        SessionManager.resetExpiredFlag();
        if (returnResult) {
            setResult(RESULT_OK);
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        }
    }
}
