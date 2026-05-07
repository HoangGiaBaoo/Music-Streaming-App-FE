package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.databinding.ActivityAddAccountBinding;

public class AddAccountActivity extends AppCompatActivity {

    private ActivityAddAccountBinding b;

    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) finish();
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAddAccountBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        b.btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        b.btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra(LoginActivity.EXTRA_RETURN_RESULT, true);
            loginLauncher.launch(i);
        });
    }
}
