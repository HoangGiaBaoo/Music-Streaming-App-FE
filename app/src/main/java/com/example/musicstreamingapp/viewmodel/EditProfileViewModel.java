package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.model.ProfileUpdateRequest;
import com.example.musicstreamingapp.model.UserMe;

public class EditProfileViewModel extends ViewModel {

    public enum Msg { AVATAR_UPLOADED, AVATAR_FAILED, SAVE_FAILED, NETWORK_ERROR }

    private final UserRepository repo;

    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> uploading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Msg>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> saveDone = new MutableLiveData<>();

    public EditProfileViewModel(UserRepository repo) {
        this.repo = repo;
    }

    public LiveData<String> avatarUrl() { return avatarUrl; }
    public LiveData<Boolean> uploading() { return uploading; }
    public LiveData<Boolean> saving() { return saving; }
    public LiveData<Event<Msg>> message() { return message; }
    public LiveData<Event<Boolean>> saveDone() { return saveDone; }

    public void setInitialAvatar(String url) {
        if (avatarUrl.getValue() == null) avatarUrl.setValue(url);
    }

    public void onAvatarPicked(byte[] bytes, String mime) {
        if (Boolean.TRUE.equals(uploading.getValue())) return;
        uploading.setValue(true);
        repo.uploadAvatar(bytes, mime, new RepoCallback<String>() {
            @Override public void onSuccess(String url) {
                uploading.postValue(false);
                avatarUrl.postValue(url);
                message.postValue(new Event<>(Msg.AVATAR_UPLOADED));
            }
            @Override public void onError(String code) {
                uploading.postValue(false);
                message.postValue(new Event<>(
                    "network_error".equals(code) ? Msg.NETWORK_ERROR : Msg.AVATAR_FAILED));
            }
        });
    }

    public void onSaveClicked(String displayName, String bio) {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        saving.setValue(true);
        ProfileUpdateRequest req = new ProfileUpdateRequest(
            displayName == null ? "" : displayName.trim(),
            bio == null ? "" : bio.trim(),
            avatarUrl.getValue()
        );
        repo.updateProfile(req, new RepoCallback<UserMe>() {
            @Override public void onSuccess(UserMe data) {
                saving.postValue(false);
                saveDone.postValue(new Event<>(true));
            }
            @Override public void onError(String code) {
                saving.postValue(false);
                message.postValue(new Event<>(
                    "network_error".equals(code) ? Msg.NETWORK_ERROR : Msg.SAVE_FAILED));
            }
        });
    }
}
