package com.example.musicstreamingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.model.UserSettings;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] QUALITIES =
        {"LOW", "NORMAL", "HIGH", "VERY_HIGH", "AUTO"};

    private ApiService api;
    private UserSettings current;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSave;
    private boolean bindingFromServer = false;

    private SwitchCompat swPrivate, swPush, swData, swAds;
    private TextView subAccount, subQuality;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        api = RetrofitClient.getApiService(TokenManager.getPrefs(this));
        root = findViewById(android.R.id.content);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setNavigationOnClickListener(v -> finish());

        swPrivate = findViewById(R.id.sw_private_session);
        swPush = findViewById(R.id.sw_push_notifications);
        swData = findViewById(R.id.sw_data_saver);
        swAds = findViewById(R.id.sw_personalized_ads);
        subAccount = findViewById(R.id.sub_account);
        subQuality = findViewById(R.id.sub_quality);

        findViewById(R.id.row_quality).setOnClickListener(v -> showQualityDialog());
        ((MaterialButton) findViewById(R.id.btn_logout)).setOnClickListener(v -> doLogout());

        wireSwitch(swPrivate, (s, checked) -> s.privateSession = checked);
        wireSwitch(swPush, (s, checked) -> s.pushNotifications = checked);
        wireSwitch(swData, (s, checked) -> s.dataSaver = checked);
        wireSwitch(swAds, (s, checked) -> s.personalizedAds = checked);

        loadAccountSummary();
        loadSettings();
    }

    private void wireSwitch(SwitchCompat sw, SwitchUpdater u) {
        sw.setOnCheckedChangeListener((b, checked) -> {
            if (bindingFromServer || current == null) return;
            u.apply(current, checked);
            scheduleSave();
        });
    }

    private interface SwitchUpdater {
        void apply(UserSettings s, boolean checked);
    }

    private void loadAccountSummary() {
        api.getMe().enqueue(new Callback<UserMe>() {
            @Override public void onResponse(Call<UserMe> c, Response<UserMe> r) {
                UserMe me = r.body();
                if (me == null) return;
                String plan = me.plan == null ? "FREE" : me.plan;
                subAccount.setText(me.resolveDisplayName() + " • " + plan);
            }
            @Override public void onFailure(Call<UserMe> c, Throwable t) { }
        });
    }

    private void loadSettings() {
        api.getSettings().enqueue(new Callback<UserSettings>() {
            @Override public void onResponse(Call<UserSettings> c, Response<UserSettings> r) {
                if (r.body() != null) bindUI(r.body());
            }
            @Override public void onFailure(Call<UserSettings> c, Throwable t) {
                Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUI(UserSettings s) {
        current = s;
        bindingFromServer = true;
        swPrivate.setChecked(s.isPrivateSession());
        swPush.setChecked(s.isPushNotifications());
        swData.setChecked(s.isDataSaver());
        swAds.setChecked(s.isPersonalizedAds());
        subQuality.setText(s.resolveQuality());
        bindingFromServer = false;
    }

    private void scheduleSave() {
        if (pendingSave != null) handler.removeCallbacks(pendingSave);
        pendingSave = () -> {
            if (current == null) return;
            api.updateSettings(current).enqueue(new Callback<UserSettings>() {
                @Override public void onResponse(Call<UserSettings> c, Response<UserSettings> r) {
                    if (r.body() != null) current = r.body();
                }
                @Override public void onFailure(Call<UserSettings> c, Throwable t) {
                    Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_SHORT).show();
                }
            });
        };
        handler.postDelayed(pendingSave, 500);
    }

    private void showQualityDialog() {
        if (current == null) return;
        int checked = 0;
        for (int i = 0; i < QUALITIES.length; i++) {
            if (QUALITIES[i].equals(current.resolveQuality())) { checked = i; break; }
        }
        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle(R.string.settings_quality)
            .setSingleChoiceItems(QUALITIES, checked, (d, which) -> {
                current.streamQualityWifi = QUALITIES[which];
                subQuality.setText(QUALITIES[which]);
                scheduleSave();
                d.dismiss();
            })
            .show();
    }

    private void doLogout() {
        api.logout().enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                clearAndRedirect();
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                clearAndRedirect();
            }
        });
    }

    private void clearAndRedirect() {
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
