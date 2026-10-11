package com.campus.library.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBookRequest(
        @NotBlank(message = "ISBN cannot be blank")
        @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$", message = "Invalid 10 or 13-digit ISBN format")
        String isbn,

        @NotBlank(message = "Title cannot be blank")
        String title,

        @NotBlank(message = "Author cannot be blank")
        String author,

        String edition
) {}
