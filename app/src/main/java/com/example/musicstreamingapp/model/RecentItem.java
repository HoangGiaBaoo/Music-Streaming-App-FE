package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class RecentItem implements Serializable {
    public Track track;
    public String playedAt;

    public String dayKey() {
        if (playedAt == null || playedAt.length() < 10) return "";
        return playedAt.substring(0, 10);
    }
}
