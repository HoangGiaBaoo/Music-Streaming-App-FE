package com.example.musicstreamingapp.model;

import java.io.Serializable;
import java.util.List;

public class Track implements Serializable {
    private Long trackId;
    private String title;
    private String audioUrl;
    private String coverUrl;
    private Integer duration;
    private Integer playCount;
    private String lyrics;
    private String createdAt;
    private Artist artist;
    private Album album;
    private List<Genre> genres;

    public Long getTrackId() { return trackId; }
    public String getTitle() { return title; }
    public String getAudioUrl() { return audioUrl; }
    public String getCoverUrl() { return coverUrl; }
    public Integer getDuration() { return duration; }
    public Integer getPlayCount() { return playCount; }
    public String getLyrics() { return lyrics; }
    public String getCreatedAt() { return createdAt; }
    public Artist getArtist() { return artist; }
    public Album getAlbum() { return album; }
    public List<Genre> getGenres() { return genres; }

    public String getArtistName() {
        return artist != null ? artist.getName() : "";
    }

    public String firstLyricLine() {
        if (lyrics == null || lyrics.isEmpty()) return "";
        for (String line : lyrics.split("\n")) {
            String trimmed = line.trim();
            // strip LRC timestamp prefix [mm:ss.xx]
            String text = trimmed.replaceFirst("^\\[\\d{2}:\\d{2}\\.\\d{2}]", "").trim();
            if (!text.isEmpty()) return text;
        }
        return "";
    }
}
