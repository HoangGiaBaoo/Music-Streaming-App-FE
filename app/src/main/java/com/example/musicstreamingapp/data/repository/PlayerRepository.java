package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.network.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerRepository {

    private final ApiService api;

    public PlayerRepository(ApiService api) {
        this.api = api;
    }

    public void toggleLike(long trackId, RepoCallback<Boolean> cb) {
        api.toggleLike(trackId).enqueue(noopCb(cb));
    }

    public void toggleFollow(long artistId, RepoCallback<Boolean> cb) {
        api.toggleFollow(artistId).enqueue(noopCb(cb));
    }

    public void recordPlay(long trackId, RepoCallback<Boolean> cb) {
        api.recordPlay(trackId).enqueue(noopCb(cb));
    }

    private static Callback<Map<String, String>> noopCb(RepoCallback<Boolean> cb) {
        return new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (cb != null) cb.onSuccess(true);
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                if (cb != null) cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        };
    }
}
