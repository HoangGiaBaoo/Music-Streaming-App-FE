package com.example.musicstreamingapp.model;

public class ProfileUpdateRequest {
    public String displayName;
    public String bio;
    public String avatarUrl;

    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String displayName, String bio, String avatarUrl) {
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }
}
