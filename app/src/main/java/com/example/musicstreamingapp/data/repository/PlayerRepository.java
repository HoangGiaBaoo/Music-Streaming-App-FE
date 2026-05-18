package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.ApiService;

import java.util.List;
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
        api.toggleLike(trackId).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful()) { if (cb != null) cb.onSuccess(true); }
                else { if (cb != null) cb.onError("HTTP " + r.code()); }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                if (cb != null) cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }

    public void toggleFollow(long artistId, RepoCallback<Boolean> cb) {
        api.toggleFollow(artistId).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful()) { if (cb != null) cb.onSuccess(true); }
                else { if (cb != null) cb.onError("HTTP " + r.code()); }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                if (cb != null) cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }

    public void checkFollowState(long artistId, RepoCallback<Boolean> cb) {
        api.getFollowedArtists().enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(Call<List<Artist>> c, Response<List<Artist>> r) {
                if (!r.isSuccessful() || r.body() == null) { cb.onSuccess(false); return; }
                for (Artist a : r.body()) {
                    if (Long.valueOf(artistId).equals(a.getArtistId())) {
                        cb.onSuccess(true);
                        return;
                    }
                }
                cb.onSuccess(false);
            }
            @Override public void onFailure(Call<List<Artist>> c, Throwable t) {
                cb.onSuccess(false);
            }
        });
    }

    public void recordPlay(long trackId, RepoCallback<Boolean> cb) {
        api.recordPlay(trackId).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) { }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) { }
        });
    }
}
