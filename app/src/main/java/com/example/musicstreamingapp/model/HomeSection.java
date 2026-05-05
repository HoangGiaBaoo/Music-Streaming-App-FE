package com.example.musicstreamingapp.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HomeSection implements Serializable {
    public static final String FEATURED = "FEATURED";
    public static final String TOP_PICKS = "TOP_PICKS";
    public static final String RADIO = "RADIO";
    public static final String RECENTLY_PLAYED = "RECENTLY_PLAYED";
    public static final String RECOMMENDED = "RECOMMENDED";
    public static final String POPULAR_ARTISTS = "POPULAR_ARTISTS";
    public static final String CHART = "CHART";
    public static final String MOOD_PLAYLIST = "MOOD_PLAYLIST";
    public static final String NEW_RELEASES = "NEW_RELEASES";
    public static final String START_LISTENING = "START_LISTENING";

    private String kind;
    private String title;
    private String subtitle;
    private List<JsonElement> items;

    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public List<JsonElement> getRawItems() { return items; }

    public List<Track> asTracks(Gson gson) {
        return parseList(gson, Track.class);
    }

    public List<Artist> asArtists(Gson gson) {
        return parseList(gson, Artist.class);
    }

    public List<Album> asAlbums(Gson gson) {
        return parseList(gson, Album.class);
    }

    public List<Playlist> asPlaylists(Gson gson) {
        return parseList(gson, Playlist.class);
    }

    private <T> List<T> parseList(Gson gson, Class<T> cls) {
        List<T> out = new ArrayList<>();
        if (items == null) return out;
        for (JsonElement e : items) {
            if (e == null || e.isJsonNull()) continue;
            try { out.add(gson.fromJson(e, cls)); } catch (Exception ignore) {}
        }
        return out;
    }
}
