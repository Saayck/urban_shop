package com.urban_shop.backend.auth.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.urban_shop.backend.common.security.RawStringDeserializer;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    @JsonDeserialize(using = RawStringDeserializer.class)
    String refreshToken
) {
}
