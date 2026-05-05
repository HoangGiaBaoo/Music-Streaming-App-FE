package com.example.musicstreamingapp.model;

import java.util.List;

public class SearchResult {
    private List<Track> tracks;
    private List<Artist> artists;

    public List<Track> getTracks() { return tracks; }
    public List<Artist> getArtists() { return artists; }
}
