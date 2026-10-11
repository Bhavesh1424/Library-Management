package com.campus.library.dto.transfer;

import com.campus.library.domain.enums.TransferStatus;

import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long copyId,
        String barcode,
        String bookTitle,
        String isbn,
        Long studentId,
        String studentName,
        String universityId,
        String fromBranchCode,
        String fromBranchName,
        String toBranchCode,
        String toBranchName,
        TransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime dispatchedAt,
        LocalDateTime arrivedAt,
        LocalDateTime expiresAt,
        LocalDateTime completedAt
) {}
