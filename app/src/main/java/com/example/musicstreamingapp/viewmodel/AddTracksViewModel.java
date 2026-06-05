package com.example.musicstreamingapp.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.RecentItem;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel cho bottom sheet "Thêm vào danh sách phát".
 * 3 tab: Bài hát đề xuất / Mới phát gần đây / Từ Bài hát đã thích.
 * Mỗi bài có nút +; nhấn để thêm/bỏ khỏi playlist hiện tại (toggle).
 */
public class AddTracksViewModel extends ViewModel {

    public enum Tab { RECOMMENDED, RECENT, LIKED }

    private final LibraryRepository repo;

    private long playlistId = -1;
    private boolean loaded = false;
    private String query = "";
    private boolean changed = false;

    /** Track ids đang nằm trong playlist (để hiển thị dấu tích & xử lý toggle). */
    private final Set<Long> inPlaylistIds = new HashSet<>();
    private final Map<Tab, List<Track>> tabData = new LinkedHashMap<>();

    private final MutableLiveData<Tab> activeTab = new MutableLiveData<>(Tab.RECOMMENDED);
    private final MutableLiveData<List<Track>> displayTracks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<Long>> addedIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showHeader = new MutableLiveData<>(true);
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    public AddTracksViewModel(LibraryRepository repo) {
        this.repo = repo;
        tabData.put(Tab.RECOMMENDED, new ArrayList<>());
        tabData.put(Tab.RECENT, new ArrayList<>());
        tabData.put(Tab.LIKED, new ArrayList<>());
    }

    public LiveData<Tab> activeTab() { return activeTab; }
    public LiveData<List<Track>> displayTracks() { return displayTracks; }
    public LiveData<Set<Long>> addedIds() { return addedIds; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<Boolean> showHeader() { return showHeader; }
    public LiveData<Event<String>> toast() { return toast; }

    public void setPlaylistId(long id) { this.playlistId = id; }

    /** Đã có thay đổi nào (thêm/bỏ bài) chưa — để màn cha refresh khi đóng sheet. */
    public boolean hasChanges() { return changed; }

    public void loadIfNeeded() {
        if (loaded || playlistId < 0) return;
        loaded = true;
        loading.setValue(true);

        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                inPlaylistIds.clear();
                if (data != null) {
                    for (Track t : data) {
                        if (t.getTrackId() != null) inPlaylistIds.add(t.getTrackId());
                    }
                }
                addedIds.setValue(new HashSet<>(inPlaylistIds));
                rebuildDisplay();
            }
            @Override public void onError(String message) { /* silent */ }
        });

        repo.getDailyRecommendations(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                if (data == null || data.isEmpty()) {
                    // Fallback: nếu chưa có gợi ý, dùng toàn bộ bài hát.
                    repo.getAllTracks(new RepoCallback<List<Track>>() {
                        @Override public void onSuccess(List<Track> all) { setTab(Tab.RECOMMENDED, all); }
                        @Override public void onError(String message) { setTab(Tab.RECOMMENDED, null); }
                    });
                } else {
                    setTab(Tab.RECOMMENDED, data);
                }
            }
            @Override public void onError(String message) {
                repo.getAllTracks(new RepoCallback<List<Track>>() {
                    @Override public void onSuccess(List<Track> all) { setTab(Tab.RECOMMENDED, all); }
                    @Override public void onError(String message) { setTab(Tab.RECOMMENDED, null); }
                });
            }
        });

        repo.getRecentTracks(50, new RepoCallback<List<RecentItem>>() {
            @Override public void onSuccess(List<RecentItem> data) {
                List<Track> tracks = new ArrayList<>();
                if (data != null) {
                    for (RecentItem it : data) {
                        if (it != null && it.track != null) tracks.add(it.track);
                    }
                }
                setTab(Tab.RECENT, tracks);
            }
            @Override public void onError(String message) { setTab(Tab.RECENT, null); }
        });

        repo.getLikedTracks(new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) { setTab(Tab.LIKED, data); }
            @Override public void onError(String message) { setTab(Tab.LIKED, null); }
        });
    }

    private void setTab(Tab tab, List<Track> data) {
        tabData.put(tab, data != null ? new ArrayList<>(data) : new ArrayList<>());
        loading.postValue(false);
        rebuildDisplay();
    }

    public void selectTab(Tab tab) {
        if (tab == activeTab.getValue()) return;
        activeTab.setValue(tab);
        rebuildDisplay();
    }

    public void filter(String q) {
        query = q != null ? q.trim() : "";
        rebuildDisplay();
    }

    /** Nhấn nút + (hoặc dấu tích) cạnh 1 bài: thêm nếu chưa có, bỏ nếu đã có. */
    public void toggleTrack(@NonNull Track track) {
        if (playlistId < 0 || track.getTrackId() == null) return;
        final long trackId = track.getTrackId();
        Set<Long> cur = addedIds.getValue();
        boolean isAdded = cur != null && cur.contains(trackId);

        // Cập nhật UI ngay (optimistic) rồi gọi API.
        Set<Long> next = cur != null ? new HashSet<>(cur) : new HashSet<>();
        if (isAdded) next.remove(trackId); else next.add(trackId);
        addedIds.setValue(next);

        if (isAdded) {
            repo.removeTrackFromPlaylist(playlistId, trackId, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean data) { changed = true; }
                @Override public void onError(String message) { revert(trackId, true); }
            });
        } else {
            repo.addTrackToPlaylist(playlistId, trackId, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean data) {
                    changed = true;
                    toast.postValue(new Event<>("added"));
                }
                @Override public void onError(String message) { revert(trackId, false); }
            });
        }
    }

    /** Khôi phục dấu tích nếu API thất bại. */
    private void revert(long trackId, boolean wasAdded) {
        Set<Long> cur = addedIds.getValue();
        Set<Long> next = cur != null ? new HashSet<>(cur) : new HashSet<>();
        if (wasAdded) next.add(trackId); else next.remove(trackId);
        addedIds.postValue(next);
    }

    private void rebuildDisplay() {
        Tab tab = activeTab.getValue();
        if (tab == null) tab = Tab.RECOMMENDED;

        List<Track> result;
        if (query.isEmpty()) {
            result = new ArrayList<>(tabData.getOrDefault(tab, new ArrayList<>()));
        } else {
            // Tìm trên hợp của cả 3 tab (loại trùng theo trackId).
            Map<Long, Track> pool = new LinkedHashMap<>();
            for (List<Track> list : tabData.values()) {
                for (Track t : list) {
                    if (t.getTrackId() != null && !pool.containsKey(t.getTrackId())) {
                        pool.put(t.getTrackId(), t);
                    }
                }
            }
            result = new ArrayList<>();
            String lower = query.toLowerCase();
            for (Track t : pool.values()) {
                String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
                String artist = t.getArtistName() != null ? t.getArtistName().toLowerCase() : "";
                if (title.contains(lower) || artist.contains(lower)) result.add(t);
            }
        }
        displayTracks.setValue(result);
        showHeader.setValue(query.isEmpty() && tab == Tab.RECOMMENDED && !result.isEmpty());
    }
}
