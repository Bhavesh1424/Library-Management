package com.campus.library.dto.catalog;

public record BranchStockDto(
        Long branchId,
        String branchCode,
        String branchName,
        long available,
        long reserved,
        long inTransit,
        long readyPickup,
        long issued,
        long totalCopies
) {}
