package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Playlist;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddToPlaylistViewModel extends ViewModel {

    public static class ListItem {
        public static final int TYPE_SECTION = 0;
        public static final int TYPE_PLAYLIST = 1;

        public final int type;
        public final String sectionLabel;
        public final boolean showClearAll;
        public final Playlist playlist;
        public final int trackCount;

        private ListItem(int type, String label, boolean clearAll, Playlist p, int count) {
            this.type = type;
            this.sectionLabel = label;
            this.showClearAll = clearAll;
            this.playlist = p;
            this.trackCount = count;
        }

        public static ListItem section(String label, boolean showClearAll) {
            return new ListItem(TYPE_SECTION, label, showClearAll, null, -1);
        }

        public static ListItem playlist(Playlist p, int trackCount) {
            return new ListItem(TYPE_PLAYLIST, null, false, p, trackCount);
        }
    }

    private final LibraryRepository repo;

    private Long trackId;
    private List<Playlist> allPlaylists = new ArrayList<>();
    private final Set<Long> originalInPlaylistIds = new HashSet<>();
    private final Map<Long, Integer> playlistTrackCounts = new HashMap<>();

    private final MutableLiveData<List<ListItem>> displayItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<Long>> selectedIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showSearch = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Playlist>> playlistCreatedEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> saveEvent = new MutableLiveData<>();

    private boolean loaded = false;
    private String currentFilter = "";

    public AddToPlaylistViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public void setTrackId(long id) {
        this.trackId = id;
    }

    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        loading.setValue(true);
        repo.getMyPlaylists(new RepoCallback<List<Playlist>>() {
            @Override public void onSuccess(List<Playlist> data) {
                allPlaylists = data != null ? data : new ArrayList<>();
                if (allPlaylists.isEmpty()) {
                    loading.setValue(false);
                    buildDisplayList();
                    return;
                }
                checkPlaylistMembership(allPlaylists);
            }
            @Override public void onError(String message) {
                allPlaylists = new ArrayList<>();
                loading.setValue(false);
                buildDisplayList();
            }
        });
    }

    private void checkPlaylistMembership(List<Playlist> playlists) {
        int[] remaining = {playlists.size()};
        for (Playlist p : playlists) {
            final long pid = p.getPlaylistId();
            repo.getPlaylistTracks(pid, new RepoCallback<List<Track>>() {
                @Override public void onSuccess(List<Track> tracks) {
                    playlistTrackCounts.put(pid, tracks.size());
                    if (trackId != null) {
                        for (Track t : tracks) {
                            if (trackId.equals(t.getTrackId())) {
                                originalInPlaylistIds.add(pid);
                                break;
                            }
                        }
                    }
                    onCheckDone(remaining);
                }
                @Override public void onError(String message) {
                    onCheckDone(remaining);
                }
            });
        }
    }

    private void onCheckDone(int[] remaining) {
        remaining[0]--;
        if (remaining[0] != 0) return;
        Set<Long> initSelected = new HashSet<>(originalInPlaylistIds);
        selectedIds.setValue(initSelected);
        showSearch.setValue(!originalInPlaylistIds.isEmpty());
        loading.setValue(false);
        buildDisplayList();
    }

    public void filter(String query) {
        currentFilter = query != null ? query.trim() : "";
        buildDisplayList();
    }

    public void togglePlaylist(Playlist playlist) {
        Set<Long> cur = selectedIds.getValue();
        if (cur == null) cur = new HashSet<>();
        Set<Long> next = new HashSet<>(cur);
        Long pid = playlist.getPlaylistId();
        if (next.contains(pid)) next.remove(pid);
        else next.add(pid);
        selectedIds.setValue(next);
        buildDisplayList();
    }

    public void clearAllSaved() {
        Set<Long> cur = selectedIds.getValue();
        if (cur == null) return;
        Set<Long> next = new HashSet<>(cur);
        next.removeAll(originalInPlaylistIds);
        selectedIds.setValue(next);
        buildDisplayList();
    }

    private void buildDisplayList() {
        List<Playlist> filtered = applyFilter(allPlaylists);
        List<Playlist> saved = new ArrayList<>();
        List<Playlist> other = new ArrayList<>();

        for (Playlist p : filtered) {
            if (originalInPlaylistIds.contains(p.getPlaylistId())) saved.add(p);
            else other.add(p);
        }

        List<ListItem> items = new ArrayList<>();
        if (!saved.isEmpty()) {
            items.add(ListItem.section("Đã lưu vào", true));
            for (Playlist p : saved) {
                int count = playlistTrackCounts.containsKey(p.getPlaylistId())
                    ? playlistTrackCounts.get(p.getPlaylistId()) : -1;
                items.add(ListItem.playlist(p, count));
            }
        }
        if (!other.isEmpty()) {
            items.add(ListItem.section("Mới cập nhật gần đây", false));
            for (Playlist p : other) {
                int count = playlistTrackCounts.containsKey(p.getPlaylistId())
                    ? playlistTrackCounts.get(p.getPlaylistId()) : -1;
                items.add(ListItem.playlist(p, count));
            }
        }
        displayItems.setValue(items);
    }

    private List<Playlist> applyFilter(List<Playlist> src) {
        if (currentFilter.isEmpty()) return new ArrayList<>(src);
        String lower = currentFilter.toLowerCase();
        List<Playlist> result = new ArrayList<>();
        for (Playlist p : src) {
            if (p.getName() != null && p.getName().toLowerCase().contains(lower)) result.add(p);
        }
        return result;
    }

    public void save() {
        if (trackId == null) { saveEvent.setValue(new Event<>(true)); return; }

        Set<Long> selected = selectedIds.getValue();
        if (selected == null) selected = new HashSet<>();

        Set<Long> toAdd = new HashSet<>(selected);
        toAdd.removeAll(originalInPlaylistIds);

        Set<Long> toRemove = new HashSet<>(originalInPlaylistIds);
        toRemove.removeAll(selected);

        int total = toAdd.size() + toRemove.size();
        if (total == 0) { saveEvent.setValue(new Event<>(true)); return; }

        final long finalTrackId = trackId;
        int[] remaining = {total};
        Runnable checkDone = () -> {
            remaining[0]--;
            if (remaining[0] == 0) saveEvent.postValue(new Event<>(true));
        };

        for (Long pid : toAdd) {
            repo.addTrackToPlaylist(pid, finalTrackId, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean d) { checkDone.run(); }
                @Override public void onError(String msg) { checkDone.run(); }
            });
        }
        for (Long pid : toRemove) {
            repo.removeTrackFromPlaylist(pid, finalTrackId, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean d) { checkDone.run(); }
                @Override public void onError(String msg) { checkDone.run(); }
            });
        }
    }

    public void createPlaylist(String name) {
        if (trackId == null) return;
        final long finalTrackId = trackId;
        repo.createPlaylist(name, false, new RepoCallback<Playlist>() {
            @Override public void onSuccess(Playlist playlist) {
                if (playlist.getPlaylistId() == null) return;
                repo.addTrackToPlaylist(playlist.getPlaylistId(), finalTrackId, new RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean d) {
                        playlistCreatedEvent.postValue(new Event<>(playlist));
                    }
                    @Override public void onError(String msg) {
                        playlistCreatedEvent.postValue(new Event<>(playlist));
                    }
                });
            }
            @Override public void onError(String msg) { /* silent */ }
        });
    }

    public boolean isInAnyPlaylist() {
        Set<Long> selected = selectedIds.getValue();
        return selected != null && !selected.isEmpty();
    }

    public Long getTrackId() { return trackId; }

    public LiveData<List<ListItem>> displayItems() { return displayItems; }
    public LiveData<Set<Long>> selectedIds() { return selectedIds; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Boolean> showSearch() { return showSearch; }
    public LiveData<Event<Playlist>> playlistCreatedEvent() { return playlistCreatedEvent; }
    public LiveData<Event<Boolean>> saveEvent() { return saveEvent; }
}
