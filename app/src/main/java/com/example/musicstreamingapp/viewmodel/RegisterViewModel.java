package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.AuthRepository;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> registerSuccess = new MutableLiveData<>();

    public RegisterViewModel(AuthRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Event<Boolean>> registerSuccess() { return registerSuccess; }

    public void onRegisterClicked(String username, String email, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (username == null || username.isEmpty()
            || email == null || email.isEmpty()
            || password == null || password.isEmpty()) {
            errorEvent.setValue(new Event<>("empty_fields"));
            return;
        }
        loading.setValue(true);
        repo.register(username, email, password, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) {
                loading.postValue(false);
                registerSuccess.postValue(new Event<>(true));
            }
            @Override public void onError(String message) {
                loading.postValue(false);
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
