package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.ListeningStats;
import com.example.musicstreamingapp.model.ProfileUpdateRequest;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.model.UserProfile;
import com.example.musicstreamingapp.model.UserSettings;
import com.example.musicstreamingapp.network.ApiService;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final ApiService api;

    public UserRepository(ApiService api) {
        this.api = api;
    }

    public void getMe(RepoCallback<UserMe> cb) {
        api.getMe().enqueue(new Callback<UserMe>() {
            @Override public void onResponse(Call<UserMe> c, Response<UserMe> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<UserMe> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void getMyProfile(RepoCallback<UserProfile> cb) {
        enqueueProfile(api.getMyProfile(), cb);
    }

    public void getUserProfile(long id, RepoCallback<UserProfile> cb) {
        enqueueProfile(api.getUserProfile(id), cb);
    }

    private void enqueueProfile(Call<UserProfile> call, RepoCallback<UserProfile> cb) {
        call.enqueue(new Callback<UserProfile>() {
            @Override public void onResponse(Call<UserProfile> c, Response<UserProfile> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<UserProfile> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void updateProfile(ProfileUpdateRequest req, RepoCallback<UserMe> cb) {
        api.updateProfile(req).enqueue(new Callback<UserMe>() {
            @Override public void onResponse(Call<UserMe> c, Response<UserMe> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<UserMe> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void uploadAvatar(byte[] bytes, String mime, RepoCallback<String> cb) {
        String safeMime = mime == null ? "image/jpeg" : mime;
        RequestBody body = RequestBody.create(bytes, MediaType.parse(safeMime));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "avatar.jpg", body);
        api.uploadAvatar(part).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().get("avatarUrl") != null) {
                    cb.onSuccess(r.body().get("avatarUrl"));
                } else {
                    cb.onError("upload_failed");
                }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void getSettings(RepoCallback<UserSettings> cb) {
        api.getSettings().enqueue(new Callback<UserSettings>() {
            @Override public void onResponse(Call<UserSettings> c, Response<UserSettings> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<UserSettings> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void updateSettings(UserSettings body, RepoCallback<UserSettings> cb) {
        api.updateSettings(body).enqueue(new Callback<UserSettings>() {
            @Override public void onResponse(Call<UserSettings> c, Response<UserSettings> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<UserSettings> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void getListeningStats(String period, int offset, RepoCallback<ListeningStats> cb) {
        api.getListeningStats(period, offset).enqueue(new Callback<ListeningStats>() {
            @Override public void onResponse(Call<ListeningStats> c, Response<ListeningStats> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<ListeningStats> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    public void logout(RepoCallback<Boolean> cb) {
        api.logout().enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                cb.onSuccess(true);
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                cb.onSuccess(true);
            }
        });
    }

    private static String safeMessage(Throwable t) {
        return t.getMessage() == null ? "network_error" : t.getMessage();
    }
}
