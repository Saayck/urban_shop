package com.urban_shop.backend.tenant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank
    @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$", message = "slug debe ser minusculas, numeros y guiones")
    @Size(max = 100) String slug,
    @NotBlank @Size(max = 50) String planName,
    @NotNull @Valid BusinessInfoRequest businessInfo,
    @NotNull @Valid InitialAdminRequest initialAdmin
) {
}
