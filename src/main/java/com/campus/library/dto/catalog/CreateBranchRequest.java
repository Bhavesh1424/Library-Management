package com.campus.library.dto.catalog;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
        @NotBlank(message = "Branch code cannot be blank")
        String code,

        @NotBlank(message = "Branch name cannot be blank")
        String name,

        @Email(message = "Valid email is required")
        @NotBlank(message = "Contact email cannot be blank")
        String contactEmail
) {}
