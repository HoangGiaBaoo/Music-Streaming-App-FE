package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.AuthRepository;
import com.example.musicstreamingapp.model.JwtResponse;

public class LoginViewModel extends ViewModel {

    private final AuthRepository repo;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> loginSuccess = new MutableLiveData<>();

    public LoginViewModel(AuthRepository repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Event<Boolean>> loginSuccess() { return loginSuccess; }

    public void onLoginClicked(String username, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            errorEvent.setValue(new Event<>("empty_fields"));
            return;
        }
        loading.setValue(true);
        repo.login(username, password, new RepoCallback<JwtResponse>() {
            @Override public void onSuccess(JwtResponse data) {
                loading.postValue(false);
                loginSuccess.postValue(new Event<>(true));
            }
            @Override public void onError(String message) {
                loading.postValue(false);
                errorEvent.postValue(new Event<>(message));
            }
        });
    }
}
