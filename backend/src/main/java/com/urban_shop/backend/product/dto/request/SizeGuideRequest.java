package com.urban_shop.backend.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SizeGuideRequest(
    @NotBlank @Size(max = 30) String size,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal chestCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal waistCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal hipCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal lengthCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal shoulderCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal sleeveCm,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal inseamCm,
    @Size(max = 1000) String notes
) {
}
