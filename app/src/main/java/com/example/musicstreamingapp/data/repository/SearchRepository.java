package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.SearchResult;
import com.example.musicstreamingapp.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchRepository {

    private final ApiService api;

    public SearchRepository(ApiService api) {
        this.api = api;
    }

    public void search(String query, RepoCallback<SearchResult> cb) {
        api.search(query).enqueue(new Callback<SearchResult>() {
            @Override public void onResponse(Call<SearchResult> c, Response<SearchResult> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<SearchResult> c, Throwable t) {
                cb.onError(safe(t));
            }
        });
    }

    public void getGenres(RepoCallback<List<Genre>> cb) {
        api.getGenres().enqueue(new Callback<List<Genre>>() {
            @Override public void onResponse(Call<List<Genre>> c, Response<List<Genre>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<List<Genre>> c, Throwable t) {
                cb.onError(safe(t));
            }
        });
    }

    private static String safe(Throwable t) {
        return t.getMessage() == null ? "network_error" : t.getMessage();
    }
}
