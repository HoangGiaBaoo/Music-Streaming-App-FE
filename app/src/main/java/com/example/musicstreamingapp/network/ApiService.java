package com.example.musicstreamingapp.network;

import com.example.musicstreamingapp.model.Album;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.GenreFeedDto;
import com.example.musicstreamingapp.model.HomeSection;
import com.example.musicstreamingapp.model.JwtResponse;
import com.example.musicstreamingapp.model.ListeningStats;
import com.example.musicstreamingapp.model.LoginRequest;
import com.example.musicstreamingapp.model.PlanInfo;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.PlaylistRequest;
import com.example.musicstreamingapp.model.ProfileUpdateRequest;
import com.example.musicstreamingapp.model.RecentItem;
import com.example.musicstreamingapp.model.RegisterRequest;
import com.example.musicstreamingapp.model.SearchResult;
import com.example.musicstreamingapp.model.SubscribeRequest;
import com.example.musicstreamingapp.model.SubscribeResponse;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.model.Track;
import com.example.musicstreamingapp.model.UserMe;
import com.example.musicstreamingapp.model.UserProfile;
import com.example.musicstreamingapp.model.UserSettings;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Auth
    @POST("api/auth/register")
    Call<Map<String, String>> register(@Body RegisterRequest body);

    @POST("api/auth/login")
    Call<JwtResponse> login(@Body LoginRequest body);

    @POST("api/auth/logout")
    Call<Map<String, String>> logout();

    // Users
    @GET("api/users/me")
    Call<UserMe> getMe();

    @GET("api/users/me/profile")
    Call<UserProfile> getMyProfile();

    @GET("api/users/{id}/profile")
    Call<UserProfile> getUserProfile(@Path("id") Long id);

    @PUT("api/users/me/profile")
    Call<UserMe> updateProfile(@Body ProfileUpdateRequest body);

    @Multipart
    @POST("api/users/me/avatar")
    Call<Map<String, String>> uploadAvatar(@Part MultipartBody.Part file);

    // Settings
    @GET("api/users/me/settings")
    Call<UserSettings> getSettings();

    @PUT("api/users/me/settings")
    Call<UserSettings> updateSettings(@Body UserSettings body);

    // Stats
    @GET("api/stats/listening")
    Call<ListeningStats> getListeningStats(@Query("period") String period,
                                           @Query("offset") int offset);

    // Home
    @GET("api/home/feed")
    Call<List<HomeSection>> homeFeed(@Query("filter") String filter);

    // Tracks
    @GET("api/tracks") Call<List<Track>> getTracks();
    @GET("api/tracks/{id}") Call<Track> getTrack(@Path("id") Long id);
    @GET("api/tracks/liked") Call<List<Track>> getLikedTracks();
    @GET("api/tracks/{id}/related") Call<List<Track>> getRelatedTracks(@Path("id") Long id);
    @POST("api/tracks/{id}/like") Call<Map<String, String>> toggleLike(@Path("id") Long id);

    // Artists
    @GET("api/artists") Call<List<Artist>> getArtists();
    @GET("api/artists/{id}") Call<Artist> getArtist(@Path("id") Long id);
    @GET("api/artists/{id}/albums") Call<List<Album>> getArtistAlbums(@Path("id") Long id);
    @GET("api/artists/{id}/tracks/popular") Call<List<Track>> getArtistPopularTracks(@Path("id") Long id);
    @GET("api/artists/{id}/related") Call<List<Artist>> getRelatedArtists(@Path("id") Long id);
    @GET("api/artists/followed") Call<List<Artist>> getFollowedArtists();
    @GET("api/artists/popular") Call<List<Artist>> getPopularArtists();
    @POST("api/artists/{id}/follow") Call<Map<String, String>> toggleFollow(@Path("id") Long id);

    // Albums
    @GET("api/albums") Call<List<Album>> getAlbums();
    @GET("api/albums/new") Call<List<Album>> getNewReleases();
    @GET("api/albums/{id}") Call<Album> getAlbum(@Path("id") Long id);
    @GET("api/albums/{id}/tracks") Call<List<Track>> getAlbumTracks(@Path("id") Long id);

    // Genres
    @GET("api/genres") Call<List<Genre>> getGenres();
    @GET("api/genres/{id}/tracks") Call<List<Track>> getGenreTracks(@Path("id") Long id);
    @GET("api/genres/{id}/feed") Call<GenreFeedDto> getGenreFeed(@Path("id") Long id);

    // Playlists
    @GET("api/playlists") Call<List<Playlist>> getMyPlaylists();
    @GET("api/playlists/curated") Call<List<Playlist>> getCuratedPlaylists(@Query("mood") String mood);
    @POST("api/playlists") Call<Playlist> createPlaylist(@Body PlaylistRequest body);
    @GET("api/playlists/{id}") Call<Playlist> getPlaylist(@Path("id") Long id);
    @GET("api/playlists/{id}/tracks") Call<List<Track>> getPlaylistTracks(@Path("id") Long id);
    @POST("api/playlists/{id}/tracks") Call<Map<String, String>> addTrackToPlaylist(@Path("id") Long id, @Query("trackId") Long trackId);
    @DELETE("api/playlists/{id}/tracks/{trackId}") Call<Map<String, String>> removeTrackFromPlaylist(@Path("id") Long id, @Path("trackId") Long trackId);
    @PUT("api/playlists/{id}") Call<Playlist> updatePlaylist(@Path("id") Long id, @Body PlaylistRequest body);
    @DELETE("api/playlists/{id}") Call<Void> deletePlaylist(@Path("id") Long id);
    @Multipart @POST("api/playlists/{id}/cover")
    Call<Map<String, String>> uploadPlaylistCover(@Path("id") Long id, @Part MultipartBody.Part file);

    // Charts
    @GET("api/charts/tracks") Call<List<Track>> getChartTracks(@Query("limit") Integer limit);
    @GET("api/charts/artists") Call<List<Artist>> getChartArtists(@Query("limit") Integer limit);

    // Recommendations
    @GET("api/recommendations/daily") Call<List<Track>> getDailyRecommendations();
    @GET("api/recommendations/mix") Call<List<Playlist>> getRecommendedMix();

    // History
    @GET("api/history") Call<List<Object>> getHistory();
    @GET("api/history/recent") Call<List<RecentItem>> getRecentTracks(@Query("limit") Integer limit);
    @POST("api/history") Call<Map<String, String>> recordPlay(@Query("trackId") Long trackId);

    // Subscription
    @GET("api/subscriptions/me") Call<Subscription> getMySubscription();
    @GET("api/subscriptions/plans") Call<List<PlanInfo>> getPlans();
    @POST("api/subscriptions/subscribe") Call<SubscribeResponse> subscribe(@Body SubscribeRequest body);
    @POST("api/subscriptions/cancel") Call<Map<String, String>> cancelSubscription();

    // Payment (VNPay)
    @POST("api/payment/create") Call<Map<String, String>> createPayment(@Body Map<String, Long> body);

    // Search
    @GET("api/search") Call<SearchResult> search(@Query("q") String query);
}
