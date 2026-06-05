package com.example.musicstreamingapp.model;

public class PlaylistRequest {
    public String name;
    public Boolean isPublic;
    public String description;

    public PlaylistRequest() {}

    public PlaylistRequest(String name, boolean isPublic) {
        this.name = name;
        this.isPublic = isPublic;
    }
}
