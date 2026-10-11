package com.campus.library.dto.catalog;

import com.campus.library.domain.enums.CopyStatus;

public record CopyDetailDto(
        Long copyId,
        Long branchId,
        String branchCode,
        String branchName,
        String barcode,
        String shelfLocation,
        CopyStatus status
) {}
