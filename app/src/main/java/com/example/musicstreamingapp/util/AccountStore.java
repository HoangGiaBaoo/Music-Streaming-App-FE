package com.example.musicstreamingapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountStore {
    private static final String PREF = "accounts";
    private static final String KEY_JSON = "accounts_json";
    private static final String KEY_CURRENT = "current_username";

    private final Context appCtx;
    private final SharedPreferences prefs;
    private static final Gson GSON = new Gson();

    public AccountStore(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        this.prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void addOrUpdate(String username, String token, String displayName, String avatarUrl) {
        Map<String, Account> map = loadAll();
        map.put(username, new Account(username, token, displayName, avatarUrl));
        persist(map);
        prefs.edit().putString(KEY_CURRENT, username).apply();
        TokenManager.saveAuth(appCtx, token, username, "user");
    }

    public List<Account> listAll() {
        return new ArrayList<>(loadAll().values());
    }

    public Account getCurrent() {
        String u = prefs.getString(KEY_CURRENT, null);
        return u == null ? null : loadAll().get(u);
    }

    public void switchTo(String username) {
        Account a = loadAll().get(username);
        if (a == null) return;
        prefs.edit().putString(KEY_CURRENT, username).apply();
        TokenManager.saveAuth(appCtx, a.token, a.username, "user");
    }

    public void remove(String username) {
        Map<String, Account> map = loadAll();
        map.remove(username);
        persist(map);
        if (username.equals(prefs.getString(KEY_CURRENT, null))) {
            prefs.edit().remove(KEY_CURRENT).apply();
        }
    }

    private Map<String, Account> loadAll() {
        String json = prefs.getString(KEY_JSON, "{}");
        Type t = new TypeToken<LinkedHashMap<String, Account>>() {}.getType();
        Map<String, Account> result = GSON.fromJson(json, t);
        return result == null ? new LinkedHashMap<>() : result;
    }

    private void persist(Map<String, Account> map) {
        prefs.edit().putString(KEY_JSON, GSON.toJson(map)).apply();
    }

    public static class Account {
        public String username;
        public String token;
        public String displayName;
        public String avatarUrl;

        public Account() {}
        public Account(String username, String token, String displayName, String avatarUrl) {
            this.username = username;
            this.token = token;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }

        public String shortName() {
            String s = (displayName != null && !displayName.isEmpty()) ? displayName : username;
            return s == null ? "" : s;
        }

        public String initial() {
            String s = shortName();
            return s.isEmpty() ? "U" : s.substring(0, 1).toUpperCase();
        }
    }
}
