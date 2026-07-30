package com.urban_shop.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffUserUpdateRequest(

    @NotBlank @Size(max = 150) String fullName,

    @Pattern(regexp = "^9\\d{8}$", message = "Celular debe tener 9 digitos e iniciar con 9")
    String phone,

    @NotBlank
    @Pattern(regexp = "^(TENANT_ADMIN|SALES_STAFF)$", message = "role debe ser TENANT_ADMIN o SALES_STAFF")
    String role,

    @NotNull Boolean active
) {
}
