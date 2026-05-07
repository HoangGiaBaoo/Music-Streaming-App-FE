package com.example.musicstreamingapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.databinding.ActivityPremiumPlansBinding;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

public class PremiumPlansActivity extends AppCompatActivity {

    private ActivityPremiumPlansBinding b;
    private SubscriptionViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPremiumPlansBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        vm = new ViewModelProvider(this, new VmFactory(this)).get(SubscriptionViewModel.class);

        b.btnBack.setOnClickListener(v -> finish());
        b.btnSubscribeIndividual.setOnClickListener(v -> vm.onSubscribeClicked("INDIVIDUAL"));
        b.btnSubscribeStudent.setOnClickListener(v -> vm.onSubscribeClicked("STUDENT"));
        b.btnSubscribeFamily.setOnClickListener(v -> vm.onSubscribeClicked("FAMILY"));

        observeViewModel();
    }

    private void observeViewModel() {
        vm.processing().observe(this, p -> {
            boolean enable = !Boolean.TRUE.equals(p);
            b.btnSubscribeIndividual.setEnabled(enable);
            b.btnSubscribeStudent.setEnabled(enable);
            b.btnSubscribeFamily.setEnabled(enable);
        });
        vm.successEvent().observe(this, e -> e.consume(plan -> {
            Toast.makeText(this, "Đăng ký Premium " + plan + " thành công!",
                Toast.LENGTH_SHORT).show();
            finish();
        }));
        vm.errorEvent().observe(this, e -> e.consume(message -> {
            String text = "network_error".equals(message)
                ? "Lỗi mạng" : "Không thể đăng ký, hãy thử lại";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }));
    }
}
