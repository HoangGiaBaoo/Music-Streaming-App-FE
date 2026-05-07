package com.example.musicstreamingapp.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.model.UserSettings;

public class SettingsViewModel extends ViewModel {

    private static final long SAVE_DEBOUNCE_MS = 500L;

    private final UserRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<UserMe> userMe = new MutableLiveData<>();
    private final MutableLiveData<UserSettings> settings = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> logoutDone = new MutableLiveData<>();

    private Runnable pendingSave;
    private boolean loaded = false;

    public SettingsViewModel(UserRepository repo) {
        this.repo = repo;
    }

    public LiveData<UserMe> userMe() { return userMe; }
    public LiveData<UserSettings> settings() { return settings; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }
    public LiveData<Event<Boolean>> logoutDone() { return logoutDone; }

    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        repo.getMe(new RepoCallback<UserMe>() {
            @Override public void onSuccess(UserMe data) { userMe.postValue(data); }
            @Override public void onError(String message) { /* silently ignore for header */ }
        });
        repo.getSettings(new RepoCallback<UserSettings>() {
            @Override public void onSuccess(UserSettings data) { settings.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
    }

    public void onPrivateSessionChanged(boolean value)    { mutate(s -> s.privateSession = value); }
    public void onPushNotificationsChanged(boolean value) { mutate(s -> s.pushNotifications = value); }
    public void onDataSaverChanged(boolean value)         { mutate(s -> s.dataSaver = value); }
    public void onPersonalizedAdsChanged(boolean value)   { mutate(s -> s.personalizedAds = value); }
    public void onQualitySelected(String quality)         { mutate(s -> s.streamQualityWifi = quality); }

    private void mutate(Mutator m) {
        UserSettings cur = settings.getValue();
        if (cur == null) return;
        m.apply(cur);
        settings.setValue(cur);
        scheduleSave(cur);
    }

    private void scheduleSave(UserSettings body) {
        if (pendingSave != null) handler.removeCallbacks(pendingSave);
        pendingSave = () -> repo.updateSettings(body, new RepoCallback<UserSettings>() {
            @Override public void onSuccess(UserSettings data) { settings.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        });
        handler.postDelayed(pendingSave, SAVE_DEBOUNCE_MS);
    }

    public void onLogoutClicked() {
        repo.logout(new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean data) { logoutDone.postValue(new Event<>(true)); }
            @Override public void onError(String message) { logoutDone.postValue(new Event<>(true)); }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pendingSave != null) handler.removeCallbacks(pendingSave);
    }

    @FunctionalInterface
    private interface Mutator {
        void apply(@NonNull UserSettings s);
    }
}
