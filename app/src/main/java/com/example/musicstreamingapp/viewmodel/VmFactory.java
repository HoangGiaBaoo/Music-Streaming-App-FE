package com.example.musicstreamingapp.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.data.repository.AuthRepository;
import com.example.musicstreamingapp.data.repository.HomeRepository;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.data.repository.PlayerRepository;
import com.example.musicstreamingapp.data.repository.SearchRepository;
import com.example.musicstreamingapp.data.repository.SubscriptionRepository;
import com.example.musicstreamingapp.data.repository.UserRepository;
import com.example.musicstreamingapp.network.ApiService;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.util.TokenManager;

public class VmFactory implements ViewModelProvider.Factory {

    private final Context appCtx;

    public VmFactory(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ApiService api = RetrofitClient.getApiService(TokenManager.getPrefs(appCtx));

        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(new UserRepository(api));
        }
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(new AuthRepository(api, appCtx));
        }
        if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
            return (T) new RegisterViewModel(new AuthRepository(api, appCtx));
        }
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(new UserRepository(api));
        }
        if (modelClass.isAssignableFrom(EditProfileViewModel.class)) {
            return (T) new EditProfileViewModel(new UserRepository(api));
        }
        if (modelClass.isAssignableFrom(ListeningStatsViewModel.class)) {
            return (T) new ListeningStatsViewModel(new UserRepository(api));
        }
        if (modelClass.isAssignableFrom(AlbumDetailViewModel.class)) {
            return (T) new AlbumDetailViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(ArtistDetailViewModel.class)) {
            return (T) new ArtistDetailViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(PlaylistDetailViewModel.class)) {
            return (T) new PlaylistDetailViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(RecentViewModel.class)) {
            return (T) new RecentViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(PlayerViewModel.class)) {
            return (T) new PlayerViewModel(new PlayerRepository(api));
        }
        if (modelClass.isAssignableFrom(SubscriptionViewModel.class)) {
            return (T) new SubscriptionViewModel(new SubscriptionRepository(api));
        }
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(new UserRepository(api));
        }
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(new HomeRepository(api));
        }
        if (modelClass.isAssignableFrom(SearchViewModel.class)) {
            return (T) new SearchViewModel(new SearchRepository(api));
        }
        if (modelClass.isAssignableFrom(LibraryViewModel.class)) {
            return (T) new LibraryViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(LikedTracksViewModel.class)) {
            return (T) new LikedTracksViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(PlaylistsViewModel.class)) {
            return (T) new PlaylistsViewModel(new LibraryRepository(api));
        }
        if (modelClass.isAssignableFrom(FollowingArtistsViewModel.class)) {
            return (T) new FollowingArtistsViewModel(new LibraryRepository(api));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
