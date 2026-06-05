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

    /** Pseudo-id đại diện "Bài hát đã thích" (playlist ảo, không có id thật). */
    public static final long LIKED_SONGS_ID = -1000L;
    private static final String LIKED_SONGS_NAME = "Bài hát đã thích";

    public static class ListItem {
        public static final int TYPE_SECTION = 0;
        public static final int TYPE_PLAYLIST = 1;

        public final int type;
        public final String sectionLabel;
        public final boolean showClearAll;
        public final Playlist playlist;        // null nếu là hàng "Bài hát đã thích"
        public final int trackCount;
        public final List<Track> sampleTracks;  // tối đa 4 track để ghép cover 2x2
        public final boolean liked;             // true = hàng "Bài hát đã thích"
        public final long id;                   // playlistId thật hoặc LIKED_SONGS_ID

        private ListItem(int type, String label, boolean clearAll, Playlist p,
                         int count, List<Track> sample, boolean liked, long id) {
            this.type = type;
            this.sectionLabel = label;
            this.showClearAll = clearAll;
            this.playlist = p;
            this.trackCount = count;
            this.sampleTracks = sample;
            this.liked = liked;
            this.id = id;
        }

        public static ListItem section(String label, boolean showClearAll) {
            return new ListItem(TYPE_SECTION, label, showClearAll, null, -1, null, false, 0);
        }

        public static ListItem playlist(Playlist p, int trackCount, List<Track> sample) {
            long id = p.getPlaylistId() != null ? p.getPlaylistId() : 0L;
            return new ListItem(TYPE_PLAYLIST, null, false, p, trackCount, sample, false, id);
        }

        public static ListItem likedRow(int trackCount) {
            return new ListItem(TYPE_PLAYLIST, null, false, null, trackCount, null, true, LIKED_SONGS_ID);
        }

        public String displayName() {
            if (liked) return LIKED_SONGS_NAME;
            return playlist != null && playlist.getName() != null ? playlist.getName() : "";
        }
    }

    private final LibraryRepository repo;

    private Long trackId;
    private List<Playlist> allPlaylists = new ArrayList<>();
    private final Set<Long> originalInPlaylistIds = new HashSet<>();
    private final Map<Long, Integer> playlistTrackCounts = new HashMap<>();
    private final Map<Long, List<Track>> playlistSampleTracks = new HashMap<>();
    private int likedCount = 0;

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
        // Nạp trạng thái "đã thích" trước, rồi tới danh sách playlist.
        repo.getLikedTracks(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> liked) {
                applyLiked(liked);
                loadPlaylists();
            }
            @Override public void onError(String message) {
                loadPlaylists();
            }
        });
    }

    private void applyLiked(List<Track> liked) {
        likedCount = liked != null ? liked.size() : 0;
        if (liked != null && trackId != null) {
            for (Track t : liked) {
                if (trackId.equals(t.getTrackId())) {
                    originalInPlaylistIds.add(LIKED_SONGS_ID);
                    break;
                }
            }
        }
    }

    private void loadPlaylists() {
        repo.getMyPlaylists(new RepoCallback<List<Playlist>>() {
            @Override public void onSuccess(List<Playlist> data) {
                allPlaylists = data != null ? data : new ArrayList<>();
                if (allPlaylists.isEmpty()) {
                    finishLoad();
                    return;
                }
                checkPlaylistMembership(allPlaylists);
            }
            @Override public void onError(String message) {
                allPlaylists = new ArrayList<>();
                finishLoad();
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
                    playlistSampleTracks.put(pid,
                        new ArrayList<>(tracks.subList(0, Math.min(4, tracks.size()))));
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
        finishLoad();
    }

    private void finishLoad() {
        selectedIds.setValue(new HashSet<>(originalInPlaylistIds));
        showSearch.setValue(!originalInPlaylistIds.isEmpty());
        loading.setValue(false);
        buildDisplayList();
    }

    public void filter(String query) {
        currentFilter = query != null ? query.trim() : "";
        buildDisplayList();
    }

    public void togglePlaylist(long id) {
        Set<Long> cur = selectedIds.getValue();
        if (cur == null) cur = new HashSet<>();
        Set<Long> next = new HashSet<>(cur);
        if (next.contains(id)) next.remove(id);
        else next.add(id);
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
        List<ListItem> saved = new ArrayList<>();
        List<ListItem> other = new ArrayList<>();

        // Hàng "Bài hát đã thích" (playlist ảo) — luôn xuất hiện.
        if (matchesFilter(LIKED_SONGS_NAME)) {
            ListItem likedItem = ListItem.likedRow(likedCount);
            if (originalInPlaylistIds.contains(LIKED_SONGS_ID)) saved.add(likedItem);
            else other.add(likedItem);
        }

        for (Playlist p : allPlaylists) {
            if (!matchesFilter(p.getName())) continue;
            Long pid = p.getPlaylistId();
            int count = pid != null && playlistTrackCounts.containsKey(pid)
                ? playlistTrackCounts.get(pid) : -1;
            List<Track> sample = pid != null ? playlistSampleTracks.get(pid) : null;
            ListItem it = ListItem.playlist(p, count, sample);
            if (pid != null && originalInPlaylistIds.contains(pid)) saved.add(it);
            else other.add(it);
        }

        List<ListItem> items = new ArrayList<>();
        if (!saved.isEmpty()) {
            items.add(ListItem.section("Đã lưu vào", true));
            items.addAll(saved);
        }
        if (!other.isEmpty()) {
            items.add(ListItem.section("Mới cập nhật gần đây", false));
            items.addAll(other);
        }
        displayItems.setValue(items);
    }

    private boolean matchesFilter(String name) {
        if (currentFilter.isEmpty()) return true;
        return name != null && name.toLowerCase().contains(currentFilter.toLowerCase());
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

        // Thêm: với "Bài hát đã thích" → toggleLike (đang chưa thích → thành thích).
        for (Long pid : toAdd) {
            if (pid != null && pid == LIKED_SONGS_ID) {
                repo.toggleLike(finalTrackId, new RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean d) { checkDone.run(); }
                    @Override public void onError(String msg) { checkDone.run(); }
                });
            } else {
                repo.addTrackToPlaylist(pid, finalTrackId, new RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean d) { checkDone.run(); }
                    @Override public void onError(String msg) { checkDone.run(); }
                });
            }
        }
        // Bỏ: với "Bài hát đã thích" → toggleLike (đang thích → bỏ thích).
        for (Long pid : toRemove) {
            if (pid != null && pid == LIKED_SONGS_ID) {
                repo.toggleLike(finalTrackId, new RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean d) { checkDone.run(); }
                    @Override public void onError(String msg) { checkDone.run(); }
                });
            } else {
                repo.removeTrackFromPlaylist(pid, finalTrackId, new RepoCallback<Boolean>() {
                    @Override public void onSuccess(Boolean d) { checkDone.run(); }
                    @Override public void onError(String msg) { checkDone.run(); }
                });
            }
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

    /** Chỉ tính playlist THẬT (loại "Bài hát đã thích") để dấu tích ở trang nghệ sĩ giữ nguyên ý nghĩa. */
    public boolean isInAnyPlaylist() {
        Set<Long> selected = selectedIds.getValue();
        if (selected == null) return false;
        for (Long id : selected) {
            if (id != null && id != LIKED_SONGS_ID) return true;
        }
        return false;
    }

    public Long getTrackId() { return trackId; }

    public LiveData<List<ListItem>> displayItems() { return displayItems; }
    public LiveData<Set<Long>> selectedIds() { return selectedIds; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Boolean> showSearch() { return showSearch; }
    public LiveData<Event<Playlist>> playlistCreatedEvent() { return playlistCreatedEvent; }
    public LiveData<Event<Boolean>> saveEvent() { return saveEvent; }
}
