package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.SubscribeRequest;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionRepository {

    private final ApiService api;

    public SubscriptionRepository(ApiService api) {
        this.api = api;
    }

    public void subscribe(String plan, RepoCallback<Subscription> cb) {
        api.subscribe(new SubscribeRequest(plan)).enqueue(new Callback<Subscription>() {
            @Override public void onResponse(Call<Subscription> c, Response<Subscription> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<Subscription> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }
}
