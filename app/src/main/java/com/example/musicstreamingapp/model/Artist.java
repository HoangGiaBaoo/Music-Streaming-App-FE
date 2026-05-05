package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class Artist implements Serializable {
    private Long artistId;
    private String name;
    private String bio;
    private String avatarUrl;
    private Long followerCount;
    private String createdAt;

    public Long getArtistId() { return artistId; }
    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public Long getFollowerCount() { return followerCount; }
    public String getCreatedAt() { return createdAt; }
}
