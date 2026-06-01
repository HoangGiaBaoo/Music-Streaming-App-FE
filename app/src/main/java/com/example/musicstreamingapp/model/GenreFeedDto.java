package com.example.musicstreamingapp.model;

import java.io.Serializable;
import java.util.List;

/**
 * Response của {@code GET /api/genres/{id}/feed}: thông tin genre + danh sách
 * section động. Dùng {@link Genre} (model có sẵn) cho field {@code genre}.
 */
public class GenreFeedDto implements Serializable {
    private Genre genre;
    private List<GenreSectionDto> sections;

    public Genre getGenre() { return genre; }
    public List<GenreSectionDto> getSections() { return sections; }
}
