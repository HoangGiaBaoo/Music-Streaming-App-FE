package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class UserMe implements Serializable {
    public Long userId;
    public String username;
    public String displayName;
    public String email;
    public String avatarUrl;
    public String bio;
    public String role;
    public String plan;

    public String resolveDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : username;
    }
}
