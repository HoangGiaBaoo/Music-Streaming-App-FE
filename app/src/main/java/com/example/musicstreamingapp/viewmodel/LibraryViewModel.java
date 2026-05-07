package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;

import java.util.Collections;
import java.util.List;

public class LibraryViewModel extends ViewModel {

    public enum Chip { PLAYLIST, ARTIST, ALBUM, LIKED }

    private final LibraryRepository repo;

    private final MutableLiveData<Chip> chip = new MutableLiveData<>(Chip.ARTIST);
    private final MutableLiveData<List<Playlist>> playlists = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Artist>> artists = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Track>> liked = new MutableLiveData<>(Collections.emptyList());

    public LibraryViewModel(LibraryRepository repo) {
        this.repo = repo;
        loadFor(Chip.ARTIST);
    }

    public LiveData<Chip> chip()                   { return chip; }
    public LiveData<List<Playlist>> playlists()    { return playlists; }
    public LiveData<List<Artist>> artists()        { return artists; }
    public LiveData<List<Track>> liked()           { return liked; }

    public void onChipSelected(Chip newChip) {
        chip.setValue(newChip);
        loadFor(newChip);
    }

    private void loadFor(Chip c) {
        switch (c) {
            case PLAYLIST:
                repo.getMyPlaylists(new RepoCallback<List<Playlist>>() {
                    @Override public void onSuccess(List<Playlist> data) { playlists.postValue(data); }
                    @Override public void onError(String message) { playlists.postValue(Collections.emptyList()); }
                });
                break;
            case ARTIST:
                repo.getFollowedArtists(new RepoCallback<List<Artist>>() {
                    @Override public void onSuccess(List<Artist> data) { artists.postValue(data); }
                    @Override public void onError(String message) { artists.postValue(Collections.emptyList()); }
                });
                break;
            case LIKED:
                repo.getLikedTracks(new RepoCallback<List<Track>>() {
                    @Override public void onSuccess(List<Track> data) { liked.postValue(data); }
                    @Override public void onError(String message) { liked.postValue(Collections.emptyList()); }
                });
                break;
            case ALBUM:
            default:
                /* no albums endpoint hooked up — original code left blank */
                break;
        }
    }
}
