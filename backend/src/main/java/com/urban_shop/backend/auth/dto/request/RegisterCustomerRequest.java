package com.urban_shop.backend.auth.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.urban_shop.backend.common.security.RawStringDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
    @NotBlank
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "tenantSlug solo admite minusculas, numeros y guiones"
    )
    @Size(max = 100) String tenantSlug,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank
    @Pattern(regexp = "^9\\d{8}$", message = "celular debe tener 9 digitos e iniciar con 9")
    String phone,
    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
        message = "password debe contener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    @JsonDeserialize(using = RawStringDeserializer.class)
    String password,
    @Pattern(regexp = "^(DNI|CE|PASSPORT)$", message = "documentType no valido")
    String documentType,
    @Size(max = 20) String documentNumber
) {
}
