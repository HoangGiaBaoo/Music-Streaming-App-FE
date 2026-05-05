package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class ListeningStats implements Serializable {
    public String periodLabel;
    public String periodStart;
    public String periodEnd;
    public TopArtist topArtist;
    public TopTrack topTrack;
    public long totalPlays;
    public long totalMinutes;

    public boolean isEmpty() {
        return topArtist == null && topTrack == null;
    }

    public static class TopArtist implements Serializable {
        public Long artistId;
        public String name;
        public String avatarUrl;
        public long playCount;
    }

    public static class TopTrack implements Serializable {
        public Long trackId;
        public String title;
        public String coverUrl;
        public String artistName;
        public long playCount;
    }
}
