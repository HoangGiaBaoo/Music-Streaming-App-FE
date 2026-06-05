package com.example.musicstreamingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.musicstreamingapp.data.Event;
import com.example.musicstreamingapp.data.RepoCallback;
import com.example.musicstreamingapp.data.repository.LibraryRepository;
import com.example.musicstreamingapp.model.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel cho màn "Chỉnh sửa danh sách phát".
 * Adapter giữ danh sách đang sửa (kéo/xoá trực tiếp); ViewModel chỉ giữ thứ tự gốc
 * để biết đã thay đổi chưa và lưu (xoá track + cập nhật thứ tự).
 */
public class EditPlaylistViewModel extends ViewModel {

    public enum SaveResult { SAVED, FAILED, NO_CHANGES }

    private final LibraryRepository repo;

    private long playlistId = -1;
    private boolean loaded = false;
    private List<Long> originalOrder = new ArrayList<>();

    private final MutableLiveData<List<Track>> initialTracks = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Event<SaveResult>> saveResult = new MutableLiveData<>();

    public EditPlaylistViewModel(LibraryRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Track>> initialTracks() { return initialTracks; }
    public LiveData<Boolean> saving() { return saving; }
    public LiveData<Event<SaveResult>> saveResult() { return saveResult; }

    public void setPlaylistId(long id) { this.playlistId = id; }

    public void loadIfNeeded() {
        if (loaded || playlistId < 0) return;
        loaded = true;
        repo.getPlaylistTracks(playlistId, new RepoCallback<List<Track>>() {
            @Override public void onSuccess(List<Track> data) {
                List<Track> list = data != null ? new ArrayList<>(data) : new ArrayList<>();
                originalOrder = idsOf(list);
                initialTracks.postValue(list);
            }
            @Override public void onError(String message) {
                initialTracks.postValue(new ArrayList<>());
            }
        });
    }

    /** Có thay đổi (xoá bài hoặc đổi thứ tự) so với lúc mở màn không. */
    public boolean hasChanges(List<Track> current) {
        return !idsOf(current).equals(originalOrder);
    }

    /** Lưu: xoá các bài đã bỏ rồi cập nhật thứ tự còn lại. */
    public void save(List<Track> current) {
        if (playlistId < 0) { saveResult.setValue(new Event<>(SaveResult.NO_CHANGES)); return; }
        final List<Long> currentIds = idsOf(current);
        if (currentIds.equals(originalOrder)) {
            saveResult.setValue(new Event<>(SaveResult.NO_CHANGES));
            return;
        }

        Set<Long> currentSet = new HashSet<>(currentIds);
        List<Long> removed = new ArrayList<>();
        for (Long id : originalOrder) {
            if (!currentSet.contains(id)) removed.add(id);
        }

        saving.setValue(true);
        if (removed.isEmpty()) {
            doReorder(currentIds);
            return;
        }
        int[] remaining = { removed.size() };
        for (Long id : removed) {
            repo.removeTrackFromPlaylist(playlistId, id, new RepoCallback<Boolean>() {
                @Override public void onSuccess(Boolean d) { afterRemoval(remaining, currentIds); }
                @Override public void onError(String m) { afterRemoval(remaining, currentIds); }
            });
        }
    }

    private void afterRemoval(int[] remaining, List<Long> currentIds) {
        remaining[0]--;
        if (remaining[0] == 0) doReorder(currentIds);
    }

    private void doReorder(List<Long> ids) {
        if (ids.isEmpty()) {
            originalOrder = new ArrayList<>(ids);
            saving.postValue(false);
            saveResult.postValue(new Event<>(SaveResult.SAVED));
            return;
        }
        repo.reorderPlaylistTracks(playlistId, ids, new RepoCallback<Boolean>() {
            @Override public void onSuccess(Boolean d) {
                originalOrder = new ArrayList<>(ids);
                saving.postValue(false);
                saveResult.postValue(new Event<>(SaveResult.SAVED));
            }
            @Override public void onError(String m) {
                saving.postValue(false);
                saveResult.postValue(new Event<>(SaveResult.FAILED));
            }
        });
    }

    private static List<Long> idsOf(List<Track> tracks) {
        List<Long> ids = new ArrayList<>();
        if (tracks == null) return ids;
        for (Track t : tracks) {
            if (t.getTrackId() != null) ids.add(t.getTrackId());
        }
        return ids;
    }
}
