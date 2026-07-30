package com.urban_shop.backend.auth.dto.response;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    String refreshToken,
    UserInfoResponse user
) {
}
