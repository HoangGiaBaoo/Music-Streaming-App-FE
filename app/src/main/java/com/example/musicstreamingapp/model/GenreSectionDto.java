package com.example.musicstreamingapp.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Một section trong feed của Genre Detail. Mirror {@link HomeSection}: backend trả
 * {@code items} là mảng object kiểu khác nhau tuỳ {@code kind}, nên giữ dạng
 * {@code List<JsonElement>} rồi parse runtime qua các helper {@code as*()} dùng
 * cùng Gson instance của {@code RetrofitClient.GSON}.
 */
public class GenreSectionDto implements Serializable {
    public static final String POPULAR_PLAYLISTS = "POPULAR_PLAYLISTS";
    public static final String NEW_RELEASES      = "NEW_RELEASES";
    public static final String POPULAR_TRACKS    = "POPULAR_TRACKS";
    public static final String POPULAR_ARTISTS   = "POPULAR_ARTISTS";
    public static final String RELATED_GENRES    = "RELATED_GENRES";

    private String kind;
    private String title;
    private List<JsonElement> items;

    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public List<JsonElement> getRawItems() { return items; }

    public List<Track> asTracks(Gson gson)       { return parseList(gson, Track.class); }
    public List<Artist> asArtists(Gson gson)     { return parseList(gson, Artist.class); }
    public List<Album> asAlbums(Gson gson)       { return parseList(gson, Album.class); }
    public List<Playlist> asPlaylists(Gson gson) { return parseList(gson, Playlist.class); }
    public List<Genre> asGenres(Gson gson)       { return parseList(gson, Genre.class); }

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
