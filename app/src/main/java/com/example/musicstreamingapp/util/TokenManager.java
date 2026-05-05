package com.example.musicstreamingapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREFS_NAME = "auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";

    public static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveAuth(Context ctx, String token, String username, String role) {
        getPrefs(ctx).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .apply();
    }

    public static String getToken(Context ctx) {
        return getPrefs(ctx).getString(KEY_TOKEN, null);
    }

    public static String getUsername(Context ctx) {
        return getPrefs(ctx).getString(KEY_USERNAME, "");
    }

    public static String getRole(Context ctx) {
        return getPrefs(ctx).getString(KEY_ROLE, "user");
    }

    public static boolean isLoggedIn(Context ctx) {
        return getToken(ctx) != null;
    }

    public static void clear(Context ctx) {
        getPrefs(ctx).edit().clear().apply();
    }
}
