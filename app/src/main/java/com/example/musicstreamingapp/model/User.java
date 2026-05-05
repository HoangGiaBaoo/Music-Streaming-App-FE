package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class User implements Serializable {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private String createdAt;

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getCreatedAt() { return createdAt; }
}
