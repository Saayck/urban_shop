package com.urban_shop.backend.auth.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.urban_shop.backend.common.security.RawStringDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank @JsonDeserialize(using = RawStringDeserializer.class) String password,
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "tenantSlug solo admite minusculas, numeros y guiones"
    )
    @Size(max = 100) String tenantSlug
) {
}
