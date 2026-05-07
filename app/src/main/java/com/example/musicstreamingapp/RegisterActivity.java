package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.databinding.ActivityRegisterBinding;
import com.example.musicstreamingapp.viewmodel.RegisterViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding b;
    private RegisterViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        vm = new ViewModelProvider(this, new VmFactory(this)).get(RegisterViewModel.class);

        b.btnRegister.setOnClickListener(v -> vm.onRegisterClicked(
            b.etUsername.getText().toString().trim(),
            b.etEmail.getText().toString().trim(),
            b.etPassword.getText().toString().trim()));

        b.tvLoginLink.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        vm.loading().observe(this, isLoading -> {
            boolean l = Boolean.TRUE.equals(isLoading);
            b.btnRegister.setEnabled(!l);
            b.progressBar.setVisibility(l ? View.VISIBLE : View.GONE);
        });
        vm.errorEvent().observe(this, e -> e.consume(this::showError));
        vm.registerSuccess().observe(this, e -> e.consume(ok -> onRegistered()));
    }

    private void showError(String code) {
        if ("empty_fields".equals(code)) {
            Snackbar.make(b.getRoot(), "Vui lòng nhập đầy đủ thông tin", Snackbar.LENGTH_SHORT).show();
        } else if ("network_error".equals(code)) {
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(b.getRoot(), code, Snackbar.LENGTH_LONG).show();
        }
    }

    private void onRegistered() {
        Snackbar.make(b.getRoot(), "Đăng ký thành công! Vui lòng đăng nhập.",
            Snackbar.LENGTH_SHORT).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, 1200);
    }
}
