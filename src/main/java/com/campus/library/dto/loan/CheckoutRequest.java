package com.campus.library.dto.loan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank(message = "Book barcode is required")
        String barcode,

        Long studentId
) {}
