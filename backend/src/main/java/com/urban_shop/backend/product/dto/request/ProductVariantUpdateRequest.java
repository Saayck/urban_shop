package com.urban_shop.backend.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantUpdateRequest(
    @Size(max = 100) String sku,
    @NotBlank @Size(max = 30) String size,
    @NotBlank @Size(max = 50) String color,
    @Pattern(
        regexp = "^#[A-Fa-f0-9]{6}$",
        message = "colorHex debe ser hex tipo #RRGGBB"
    )
    String colorHex,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
    @NotNull Boolean active
) {
}
