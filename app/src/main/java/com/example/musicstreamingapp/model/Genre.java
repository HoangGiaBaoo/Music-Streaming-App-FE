package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class Genre implements Serializable {
    private Long genreId;
    private String name;
    private String coverColor;
    private String coverUrl;

    public Long getGenreId() { return genreId; }
    public String getName() { return name; }
    public String getCoverColor() { return coverColor; }
    public String getCoverUrl() { return coverUrl; }
}
