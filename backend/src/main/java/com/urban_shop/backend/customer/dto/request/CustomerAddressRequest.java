package com.urban_shop.backend.customer.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerAddressRequest(
    @NotBlank @Size(max = 100) String department,
    @NotBlank @Size(max = 100) String province,
    @NotBlank @Size(max = 100) String district,
    @NotBlank @Size(max = 500) String address,
    @Size(max = 500) String reference,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    Boolean defaultAddress
) {
}
