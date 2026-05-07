package com.example.musicstreamingapp.network;

import android.content.SharedPreferences;

import com.example.musicstreamingapp.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final SharedPreferences prefs;

    public AuthInterceptor(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String path = request.url().encodedPath();
        boolean isAuthEndpoint = path.contains("/api/auth/");

        String token = prefs.getString("token", null);
        if (token != null && !isAuthEndpoint) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        }
        Response response = chain.proceed(request);
        if (!isAuthEndpoint && (response.code() == 401 || response.code() == 403)) {
            SessionManager.markExpired();
        }
        return response;
    }
}
