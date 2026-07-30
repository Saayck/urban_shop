package com.urban_shop.backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
    String tenantSlug,
    @NotBlank @Email @Size(max = 150) String email
) {
}
