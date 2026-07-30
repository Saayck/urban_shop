package com.urban_shop.backend.tenant.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.urban_shop.backend.common.security.RawStringDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InitialAdminRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
        message = "password debe contener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    @JsonDeserialize(using = RawStringDeserializer.class)
    String password,
    @Pattern(regexp = "^9\\d{8}$", message = "Celular debe tener 9 digitos e iniciar con 9") String phone
) {
}
