package com.example.musicstreamingapp.util;

import android.content.Context;
import android.content.Intent;

import com.example.musicstreamingapp.LoginActivity;
import com.example.musicstreamingapp.network.RetrofitClient;

public class SessionManager {

    private static Context appCtx;
    private static volatile boolean expiredHandled = false;

    private SessionManager() { }

    public static void init(Context ctx) {
        if (appCtx == null) {
            appCtx = ctx.getApplicationContext();
        }
    }

    public static synchronized void resetExpiredFlag() {
        expiredHandled = false;
    }

    public static synchronized void markExpired() {
        if (expiredHandled || appCtx == null) return;
        expiredHandled = true;

        TokenManager.clear(appCtx);
        AccountStore store = new AccountStore(appCtx);
        AccountStore.Account current = store.getCurrent();
        if (current != null) store.remove(current.username);
        RetrofitClient.reset();

        Intent intent = new Intent(appCtx, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        appCtx.startActivity(intent);
    }
}
