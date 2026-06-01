package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.SubscribeRequest;
import com.example.musicstreamingapp.model.SubscribeResponse;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.network.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionRepository {

    /** Sentinel: user đã có gói Premium active (backend trả 409). */
    public static final String ERR_ALREADY_ACTIVE = "already_active";

    private final ApiService api;

    public SubscriptionRepository(ApiService api) {
        this.api = api;
    }

    /**
     * POST /api/subscriptions/subscribe — tạo sub pending, trả subscriptionId.
     * KHÔNG kích hoạt premium ngay; caller phải gọi tiếp createPayment().
     */
    public void subscribe(String plan, RepoCallback<Long> cb) {
        api.subscribe(new SubscribeRequest(plan)).enqueue(new Callback<SubscribeResponse>() {
            @Override public void onResponse(Call<SubscribeResponse> c, Response<SubscribeResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().getSubscriptionId() != null) {
                    cb.onSuccess(r.body().getSubscriptionId());
                } else if (r.code() == 409) {
                    // Đã có gói Premium đang hoạt động — backend từ chối tạo trùng.
                    cb.onError(ERR_ALREADY_ACTIVE);
                } else {
                    cb.onError("HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<SubscribeResponse> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }

    /**
     * POST /api/payment/create — đổi subscriptionId lấy payUrl VNPay.
     */
    public void createPayment(long subscriptionId, RepoCallback<String> cb) {
        Map<String, Long> body = new HashMap<>();
        body.put("subscriptionId", subscriptionId);
        api.createPayment(body).enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().get("payUrl") != null) {
                    cb.onSuccess(r.body().get("payUrl"));
                } else {
                    cb.onError("HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }

    /**
     * GET /api/subscriptions/me — sub hiện tại (có thể null nếu user chưa từng đăng ký).
     */
    public void getMine(RepoCallback<Subscription> cb) {
        api.getMySubscription().enqueue(new Callback<Subscription>() {
            @Override public void onResponse(Call<Subscription> c, Response<Subscription> r) {
                if (r.isSuccessful()) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<Subscription> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }

    /**
     * POST /api/subscriptions/cancel — huỷ sub hiện tại.
     */
    public void cancel(RepoCallback<String> cb) {
        api.cancelSubscription().enqueue(new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful()) {
                    String msg = r.body() != null ? r.body().get("message") : null;
                    cb.onSuccess(msg);
                } else {
                    cb.onError("HTTP " + r.code());
                }
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                cb.onError(t.getMessage() == null ? "network_error" : t.getMessage());
            }
        });
    }
}
