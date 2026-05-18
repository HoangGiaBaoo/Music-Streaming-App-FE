package com.example.musicstreamingapp.data.repository;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.PlaylistRequest;
import com.example.musicstreamingapp.model.RecentItem;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.network.ApiService;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryRepository {

    private final ApiService api;

    public LibraryRepository(ApiService api) {
        this.api = api;
    }

    // ---- Albums --------------------------------------------------------

    public void getAllAlbums(RepoCallback<List<Album>> cb)            { enqueue(api.getAlbums(), cb); }
    public void getAlbumTracks(long id, RepoCallback<List<Track>> cb) { enqueue(api.getAlbumTracks(id), cb); }

    // ---- Artists -------------------------------------------------------

    public void getArtist(long id, RepoCallback<Artist> cb)              { enqueue(api.getArtist(id), cb); }
    public void getArtistAlbums(long id, RepoCallback<List<Album>> cb)   { enqueue(api.getArtistAlbums(id), cb); }
    public void getAllArtists(RepoCallback<List<Artist>> cb)             { enqueue(api.getArtists(), cb); }
    public void getFollowedArtists(RepoCallback<List<Artist>> cb)        { enqueue(api.getFollowedArtists(), cb); }
    public void getAllTracks(RepoCallback<List<Track>> cb)               { enqueue(api.getTracks(), cb); }

    public void toggleFollow(long artistId, RepoCallback<Boolean> cb) {
        api.toggleFollow(artistId).enqueue(boolCb(cb));
    }

    public void getArtistPopularTracks(long artistId, RepoCallback<List<Track>> cb) {
        enqueue(api.getArtistPopularTracks(artistId), cb);
    }

    public void toggleLike(long trackId, RepoCallback<Boolean> cb) {
        api.toggleLike(trackId).enqueue(boolCb(cb));
    }

    public void checkFollowState(long artistId, RepoCallback<Boolean> cb) {
        api.getFollowedArtists().enqueue(new Callback<List<Artist>>() {
            @Override public void onResponse(Call<List<Artist>> c, Response<List<Artist>> r) {
                if (!r.isSuccessful() || r.body() == null) { cb.onSuccess(false); return; }
                for (Artist a : r.body()) {
                    if (Long.valueOf(artistId).equals(a.getArtistId())) {
                        cb.onSuccess(true);
                        return;
                    }
                }
                cb.onSuccess(false);
            }
            @Override public void onFailure(Call<List<Artist>> c, Throwable t) {
                cb.onSuccess(false);
            }
        });
    }

    // ---- Tracks --------------------------------------------------------

    public void getLikedTracks(RepoCallback<List<Track>> cb) { enqueue(api.getLikedTracks(), cb); }

    // ---- Playlists -----------------------------------------------------

    public void getMyPlaylists(RepoCallback<List<Playlist>> cb) {
        enqueue(api.getMyPlaylists(), cb);
    }

    public void getPlaylistTracks(long playlistId, RepoCallback<List<Track>> cb) {
        enqueue(api.getPlaylistTracks(playlistId), cb);
    }

    public void createPlaylist(String name, boolean isPublic, RepoCallback<Playlist> cb) {
        PlaylistRequest req = new PlaylistRequest();
        req.name = name;
        req.isPublic = isPublic;
        enqueue(api.createPlaylist(req), cb);
    }

    public void addTrackToPlaylist(long playlistId, long trackId, RepoCallback<Boolean> cb) {
        api.addTrackToPlaylist(playlistId, trackId).enqueue(boolCb(cb));
    }

    public void removeTrackFromPlaylist(long playlistId, long trackId, RepoCallback<Boolean> cb) {
        api.removeTrackFromPlaylist(playlistId, trackId).enqueue(boolCb(cb));
    }

    // ---- Recent --------------------------------------------------------

    public void getRecentTracks(int limit, RepoCallback<List<RecentItem>> cb) {
        enqueue(api.getRecentTracks(limit), cb);
    }

    // ---- helpers -------------------------------------------------------

    private static <T> void enqueue(Call<T> call, RepoCallback<T> cb) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<T> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        });
    }

    private static Callback<Map<String, String>> boolCb(RepoCallback<Boolean> cb) {
        return new Callback<Map<String, String>>() {
            @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                if (r.isSuccessful()) cb.onSuccess(true);
                else cb.onError("HTTP " + r.code());
            }
            @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                cb.onError(safeMessage(t));
            }
        };
    }

    private static String safeMessage(Throwable t) {
        return t.getMessage() == null ? "network_error" : t.getMessage();
    }
}
