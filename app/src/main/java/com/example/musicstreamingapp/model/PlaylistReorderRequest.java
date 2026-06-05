package com.example.musicstreamingapp.model;

import java.util.List;

/** Body cho PUT /api/playlists/{id}/tracks/order — danh sách trackId theo thứ tự mới. */
public class PlaylistReorderRequest {
    public List<Long> trackIds;

    public PlaylistReorderRequest(List<Long> trackIds) {
        this.trackIds = trackIds;
    }
}
