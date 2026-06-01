package com.example.musicstreamingapp;

import android.app.Activity;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.data.repository.SubscriptionRepository;
import com.example.musicstreamingapp.databinding.ActivityPremiumPlansBinding;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

public class PremiumPlansActivity extends AppCompatActivity {

    private ActivityPremiumPlansBinding b;
    private SubscriptionViewModel vm;

    private final ActivityResultLauncher<android.content.Intent> paymentLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            String status = result.getData() != null
                ? result.getData().getStringExtra(PaymentActivity.EXTRA_STATUS) : null;
            if (result.getResultCode() == Activity.RESULT_OK && "success".equals(status)) {
                // Thanh toán xong — xác nhận lại với backend bằng /me.
                vm.loadCurrentSubscription();
            } else {
                Snackbar.make(b.getRoot(), "Thanh toán không thành công", Snackbar.LENGTH_LONG).show();
            }
        });

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

        vm.payUrlEvent().observe(this, e -> e.consume(payUrl ->
            paymentLauncher.launch(PaymentActivity.newIntent(this, payUrl))));

        vm.subscription().observe(this, sub -> {
            if (sub != null && sub.isPremium()) {
                Snackbar.make(b.getRoot(), "Nâng cấp Premium thành công!", Snackbar.LENGTH_LONG).show();
                b.getRoot().postDelayed(this::finish, 1200);
            }
        });

        vm.errorEvent().observe(this, e -> e.consume(message -> {
            String text;
            if (SubscriptionRepository.ERR_ALREADY_ACTIVE.equals(message)) {
                text = "Bạn đã có gói Premium đang hoạt động";
            } else if ("network_error".equals(message)) {
                text = "Lỗi mạng";
            } else {
                text = "Không thể đăng ký, hãy thử lại";
            }
            Snackbar.make(b.getRoot(), text, Snackbar.LENGTH_LONG).show();
        }));
    }
}
