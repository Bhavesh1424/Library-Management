package com.campus.library.dto.catalog;

import java.util.List;

public record BookInventoryResponse(
        Long bookId,
        String isbn,
        String title,
        String author,
        String edition,
        long totalCopies,
        long totalAvailable,
        List<BranchStockDto> branchBreakdown,
        List<CopyDetailDto> availableCopies
) {}
