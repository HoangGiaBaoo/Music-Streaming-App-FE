package com.example.musicstreamingapp.network;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = "http://10.0.2.2:8080/musicapp/";
    public static final String BASE_MEDIA_URL = "http://10.0.2.2:8080/musicapp";

    public static final Gson GSON = new GsonBuilder().setLenient().create();

    private static Retrofit instance;

    public static Retrofit getInstance(SharedPreferences prefs) {
        if (instance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(prefs))
                .addInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

            instance = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        }
        return instance;
    }

    public static ApiService getApiService(SharedPreferences prefs) {
        return getInstance(prefs).create(ApiService.class);
    }

    public static void reset() {
        instance = null;
    }
}
