package com.urban_shop.backend.shipping.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShippingZoneUpdateRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 100) String department,
    @Size(max = 100) String province,
    @Size(max = 100) String district,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @Size(max = 100) String estimatedTime,
    @NotNull Boolean active
) {
}
