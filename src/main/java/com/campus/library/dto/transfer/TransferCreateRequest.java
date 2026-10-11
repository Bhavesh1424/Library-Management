package com.campus.library.dto.transfer;

import jakarta.validation.constraints.NotNull;

public record TransferCreateRequest(
        @NotNull(message = "Book copy ID is required")
        Long copyId,

        @NotNull(message = "Destination branch ID is required")
        Long destinationBranchId
) {}
