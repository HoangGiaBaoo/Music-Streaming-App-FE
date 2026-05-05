package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

public class AddAccountActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK) finish();
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        MaterialButton btnRegister = findViewById(R.id.btn_register);
        MaterialButton btnLogin = findViewById(R.id.btn_login);

        btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra(LoginActivity.EXTRA_RETURN_RESULT, true);
            loginLauncher.launch(i);
        });
    }
}
