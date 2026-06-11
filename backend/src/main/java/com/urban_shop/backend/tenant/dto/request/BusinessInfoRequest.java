package com.urban_shop.backend.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessInfoRequest(
    @NotBlank
    @Pattern(
        regexp = "^(EMPRESA|PERSONA_NATURAL)$",
        message = "businessType debe ser EMPRESA o PERSONA_NATURAL"
    )
    @Size(max = 50) String businessType,
    @NotBlank @Size(max = 150) String commercialName,
    @Size(max = 200) String legalName,
    @Pattern(regexp = "^\\d{11}$", message = "RUC debe tener 11 digitos") String ruc,
    @Pattern(regexp = "^9\\d{8}$", message = "celular debe tener 9 digitos e iniciar con 9") String phone,
    @Email @Size(max = 150) String email,
    String address,
    @Size(max = 100) String district,
    @Size(max = 100) String province,
    @Size(max = 100) String department
) {
}
