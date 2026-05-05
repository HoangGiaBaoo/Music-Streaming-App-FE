package com.example.musicstreamingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.model.SubscribeRequest;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PremiumPlansActivity extends AppCompatActivity {
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_plans);
        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_subscribe_individual).setOnClickListener(v -> subscribe("INDIVIDUAL"));
        findViewById(R.id.btn_subscribe_student).setOnClickListener(v -> subscribe("STUDENT"));
        findViewById(R.id.btn_subscribe_family).setOnClickListener(v -> subscribe("FAMILY"));
    }

    private void subscribe(String plan) {
        api.subscribe(new SubscribeRequest(plan)).enqueue(new Callback<Subscription>() {
            @Override public void onResponse(@NonNull Call<Subscription> call,
                                             @NonNull Response<Subscription> response) {
                String msg = response.isSuccessful() ? "Đăng ký Premium " + plan + " thành công!"
                        : "Không thể đăng ký, hãy thử lại";
                Toast.makeText(PremiumPlansActivity.this, msg, Toast.LENGTH_SHORT).show();
                if (response.isSuccessful()) finish();
            }
            @Override public void onFailure(@NonNull Call<Subscription> call, @NonNull Throwable t) {
                Toast.makeText(PremiumPlansActivity.this,
                        "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
