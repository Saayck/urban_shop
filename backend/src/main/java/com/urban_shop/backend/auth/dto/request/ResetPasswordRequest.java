package com.urban_shop.backend.auth.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.urban_shop.backend.common.security.RawStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @JsonDeserialize(using = RawStringDeserializer.class) String token,
    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#_-])[A-Za-z\\d@$!%*?&.#_-]{8,}$",
        message = "la nueva contraseña debe contener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    @JsonDeserialize(using = RawStringDeserializer.class)
    String newPassword
) {
}
