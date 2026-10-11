package com.campus.library.dto.catalog;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BranchDto(
        Long id,
        String code,
        String name,
        String contactEmail
) {}
