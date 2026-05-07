package com.example.musicstreamingapp.data.repository;

import android.content.Context;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.JwtResponse;
import com.example.musicstreamingapp.model.LoginRequest;
import com.example.musicstreamingapp.model.RegisterRequest;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.AccountStore;
import com.example.musicstreamingapp.util.TokenManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService api;
    private final Context appCtx;

    public AuthRepository(ApiService api, Context ctx) {
        this.api = api;
        this.appCtx = ctx.getApplicationContext();
    }

    public void login(String username, String password, RepoCallback<JwtResponse> cb) {
        api.login(new LoginRequest(username, password)).enqueue(new Callback<JwtResponse>() {
            @Override public void onResponse(Call<JwtResponse> c, Response<JwtResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    JwtResponse jwt = r.body();
                    TokenManager.saveAuth(appCtx, jwt.getToken(), jwt.getUsername(), jwt.getRole());
                    new AccountStore(appCtx).addOrUpdate(
                        jwt.getUsername(), jwt.getToken(), jwt.getUsername(), null);
                    RetrofitClient.reset();
                    cb.onSuccess(jwt);
                } else {
                    cb.onError(parseError(r, "login_failed"));
                }
            }
            @Override public void onFailure(Call<JwtResponse> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void register(String username, String email, String password, RepoCallback<Boolean> cb) {
        api.register(new RegisterRequest(username, email, password))
            .enqueue(new Callback<Map<String, String>>() {
                @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                    if (r.isSuccessful()) cb.onSuccess(true);
                    else cb.onError(parseError(r, "register_failed"));
                }
                @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                    cb.onError(safeMessage(t));
                }
            });
    }

    private static String parseError(Response<?> response, String fallbackKey) {
        try {
            if (response.errorBody() == null) return "HTTP " + response.code();
            String body = response.errorBody().string();
            if (body == null || body.isEmpty()) return "HTTP " + response.code();
            JsonObject obj = new Gson().fromJson(body, JsonObject.class);
            if (obj != null) {
                if (obj.has("error") && !obj.get("error").isJsonNull()) return obj.get("error").getAsString();
                if (obj.has("message") && !obj.get("message").isJsonNull()) return obj.get("message").getAsString();
            }
        } catch (Exception ignore) { }
        return "HTTP " + response.code();
    }

    private static String safeMessage(Throwable t) {
        return t.getMessage() == null ? "network_error" : t.getMessage();
    }
}
