package com.estaid.auth.dto;

public record AuthUserResponse(
        String userId,
        String username,
        String accessToken,
        String tokenType
) {
}
