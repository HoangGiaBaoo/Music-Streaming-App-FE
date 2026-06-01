package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.SubscriptionRepository;
import com.example.musicstreamingapp.model.Subscription;

public class SubscriptionViewModel extends ViewModel {

    private final SubscriptionRepository repo;

    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> payUrlEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Subscription> subscription = new MutableLiveData<>();

    public SubscriptionViewModel(SubscriptionRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> processing() { return processing; }
    public LiveData<Event<String>> payUrlEvent() { return payUrlEvent; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Subscription> subscription() { return subscription; }

    /**
     * Bước 1: tạo sub pending → nhận subscriptionId → tạo payment → phát payUrlEvent.
     * KHÔNG báo success ở đây; chờ WebView VNPay trả về rồi mới reload /me.
     */
    public void onSubscribeClicked(String plan) {
        if (Boolean.TRUE.equals(processing.getValue())) return;
        processing.setValue(true);
        repo.subscribe(plan, new RepoCallback<Long>() {
            @Override public void onSuccess(Long subscriptionId) {
                createPayment(subscriptionId);
            }
            @Override public void onError(String message) {
                processing.postValue(false);
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    private void createPayment(long subscriptionId) {
        repo.createPayment(subscriptionId, new RepoCallback<String>() {
            @Override public void onSuccess(String payUrl) {
                processing.postValue(false);
                payUrlEvent.postValue(new Event<>(payUrl));
            }
            @Override public void onError(String message) {
                processing.postValue(false);
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    /** GET /api/subscriptions/me → cập nhật subscription LiveData (gọi sau khi thanh toán xong). */
    public void loadCurrentSubscription() {
        repo.getMine(new RepoCallback<Subscription>() {
            @Override public void onSuccess(Subscription data) {
                subscription.postValue(data);
            }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
