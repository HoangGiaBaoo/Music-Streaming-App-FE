package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.HomeSection;
import com.example.musicstreamingapp.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRepository {

    private final ApiService api;

    public HomeRepository(ApiService api) {
        this.api = api;
    }

    public void getFeed(String filter, RepoCallback<List<HomeSection>> cb) {
        api.homeFeed(filter).enqueue(new Callback<List<HomeSection>>() {
            @Override public void onResponse(Call<List<HomeSection>> c, Response<List<HomeSection>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<List<HomeSection>> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }
}
