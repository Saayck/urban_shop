package com.urban_shop.backend.customer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank
    @Pattern(regexp = "^9\\d{8}$", message = "celular debe tener 9 digitos e iniciar con 9")
    String phone,
    @Pattern(regexp = "^(DNI|CE|PASSPORT)$", message = "documentType no valido")
    String documentType,
    @Size(max = 20) String documentNumber
) {
}
