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
    private final MutableLiveData<Event<String>> successEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    public SubscriptionViewModel(SubscriptionRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> processing() { return processing; }
    public LiveData<Event<String>> successEvent() { return successEvent; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

    public void onSubscribeClicked(String plan) {
        if (Boolean.TRUE.equals(processing.getValue())) return;
        processing.setValue(true);
        repo.subscribe(plan, new RepoCallback<Subscription>() {
            @Override public void onSuccess(Subscription data) {
                processing.postValue(false);
                successEvent.postValue(new Event<>(plan));
            }
            @Override public void onError(String message) {
                processing.postValue(false);
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
