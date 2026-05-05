package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class Playlist implements Serializable {
    private Long playlistId;
    private String name;
    private String description;
    private String coverUrl;
    private String coverColor;
    private Boolean isPublic;
    private Boolean isCurated;
    private String mood;
    private String createdAt;
    private User user;

    public Long getPlaylistId() { return playlistId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public String getCoverColor() { return coverColor; }
    public Boolean getPublic() { return isPublic; }
    public Boolean getCurated() { return isCurated; }
    public String getMood() { return mood; }
    public String getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
}
