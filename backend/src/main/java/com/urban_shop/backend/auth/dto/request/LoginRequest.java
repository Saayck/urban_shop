package com.urban_shop.backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "tenantSlug solo admite minusculas, numeros y guiones"
    )
    @Size(max = 100) String tenantSlug
) {
}
