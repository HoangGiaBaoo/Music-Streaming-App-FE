package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class Album implements Serializable {
    private Long albumId;
    private String title;
    private String coverUrl;
    private String releaseDate;
    private String albumType;
    private String description;
    private Artist artist;

    public Long getAlbumId() { return albumId; }
    public String getTitle() { return title; }
    public String getCoverUrl() { return coverUrl; }
    public String getReleaseDate() { return releaseDate; }
    public String getAlbumType() { return albumType; }
    public String getDescription() { return description; }
    public Artist getArtist() { return artist; }

    public String getAlbumTypeLabel() {
        if (albumType == null) return "Album";
        switch (albumType) {
            case "ALBUM": return "Album";
            case "SINGLE": return "Đĩa đơn";
            case "EP": return "EP";
            case "COMPILATION": return "Tuyển tập";
            default: return "Album";
        }
    }
}
