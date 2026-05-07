package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.databinding.ActivitySettingsBinding;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.model.UserSettings;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.TokenManager;
import com.example.musicstreamingapp.viewmodel.SettingsViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] QUALITIES =
        {"LOW", "NORMAL", "HIGH", "VERY_HIGH", "AUTO"};

    private ActivitySettingsBinding b;
    private SettingsViewModel vm;
    private boolean bindingFromVm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        vm = new ViewModelProvider(this, new VmFactory(this)).get(SettingsViewModel.class);

        wireSwitches();
        b.rowQuality.setOnClickListener(v -> showQualityDialog());
        b.btnLogout.setOnClickListener(v -> vm.onLogoutClicked());

        observeViewModel();
        vm.loadIfNeeded();
    }

    private void wireSwitches() {
        b.swPrivateSession.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingFromVm) vm.onPrivateSessionChanged(checked);
        });
        b.swPushNotifications.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingFromVm) vm.onPushNotificationsChanged(checked);
        });
        b.swDataSaver.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingFromVm) vm.onDataSaverChanged(checked);
        });
        b.swPersonalizedAds.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingFromVm) vm.onPersonalizedAdsChanged(checked);
        });
    }

    private void observeViewModel() {
        vm.userMe().observe(this, this::renderUserMe);
        vm.settings().observe(this, this::renderSettings);
        vm.errorEvent().observe(this, e -> e.consume(msg ->
            Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show()));
        vm.logoutDone().observe(this, e -> e.consume(done -> redirectToLogin()));
    }

    private void renderUserMe(UserMe me) {
        if (me == null) return;
        String plan = me.plan == null ? "FREE" : me.plan;
        b.subAccount.setText(me.resolveDisplayName() + " • " + plan);
    }

    private void renderSettings(UserSettings s) {
        if (s == null) return;
        bindingFromVm = true;
        b.swPrivateSession.setChecked(s.isPrivateSession());
        b.swPushNotifications.setChecked(s.isPushNotifications());
        b.swDataSaver.setChecked(s.isDataSaver());
        b.swPersonalizedAds.setChecked(s.isPersonalizedAds());
        b.subQuality.setText(s.resolveQuality());
        bindingFromVm = false;
    }

    private void showQualityDialog() {
        UserSettings cur = vm.settings().getValue();
        if (cur == null) return;
        int checked = 0;
        for (int i = 0; i < QUALITIES.length; i++) {
            if (QUALITIES[i].equals(cur.resolveQuality())) { checked = i; break; }
        }
        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle(R.string.settings_quality)
            .setSingleChoiceItems(QUALITIES, checked, (d, which) -> {
                vm.onQualitySelected(QUALITIES[which]);
                d.dismiss();
            })
            .show();
    }

    private void redirectToLogin() {
        AccountStore store = new AccountStore(this);
        AccountStore.Account acc = store.getCurrent();
        if (acc != null) store.remove(acc.username);
        TokenManager.clear(this);
        RetrofitClient.reset();
        startActivity(new Intent(this, LoginActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
