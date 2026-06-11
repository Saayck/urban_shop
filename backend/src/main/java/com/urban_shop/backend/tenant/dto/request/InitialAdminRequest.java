package com.urban_shop.backend.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InitialAdminRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @Pattern(regexp = "^9\\d{8}$", message = "Celular debe tener 9 digitos e iniciar con 9") String phone
) {
}
