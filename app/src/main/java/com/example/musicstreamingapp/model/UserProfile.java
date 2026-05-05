package com.example.musicstreamingapp.model;

import java.io.Serializable;
import java.util.List;

public class UserProfile implements Serializable {
    public Long userId;
    public String displayName;
    public String avatarUrl;
    public String bio;
    public long followers;
    public long following;
    public List<Playlist> playlists;
}
