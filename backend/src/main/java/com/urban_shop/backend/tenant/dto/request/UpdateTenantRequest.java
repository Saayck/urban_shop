package com.urban_shop.backend.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank
    @Pattern(
        regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$",
        message = "slug debe ser minusculas, numeros y guiones"
    )
    @Size(max = 100) String slug,
    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "planName debe usar mayusculas, numeros y guion bajo")
    @Size(max = 50) String planName
) {
}
