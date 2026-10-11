package com.campus.library.dto.catalog;

public record BookSummaryResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String edition,
        long totalCopies,
        long totalAvailable
) {}
