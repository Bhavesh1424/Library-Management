package com.campus.library.dto.auth;

import com.campus.library.domain.enums.UserRole;

public record LoginResponse(
        String token,
        String tokenType,
        Long userId,
        String universityId,
        String fullName,
        String email,
        UserRole role,
        long expiresIn
) {}
