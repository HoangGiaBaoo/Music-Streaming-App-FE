package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.model.UserProfile;

public class ProfileViewModel extends ViewModel {

    private final UserRepository repo;

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    private Long targetUserId;

    public ProfileViewModel(UserRepository repo) {
        this.repo = repo;
    }

    public LiveData<UserProfile> profile() { return profile; }
    public LiveData<Event<String>> errorEvent() { return errorEvent; }

    public boolean isOwnProfile() {
        return targetUserId == null;
    }

    public void setTarget(Long userId) {
        this.targetUserId = userId;
    }

    public void refresh() {
        RepoCallback<UserProfile> cb = new RepoCallback<UserProfile>() {
            @Override public void onSuccess(UserProfile data) { profile.postValue(data); }
            @Override public void onError(String message) {
                errorEvent.postValue(new Event<>(message));
            }
        };
        if (targetUserId == null) repo.getMyProfile(cb);
        else repo.getUserProfile(targetUserId, cb);
    }
}
