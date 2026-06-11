package com.urban_shop.backend.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(
    UUID brandId,
    UUID categoryId,
    @NotBlank @Size(max = 150) String name,
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "slug solo admite minusculas, numeros y guiones"
    )
    @Size(max = 180) String slug,
    @Size(max = 5000) String description,
    @Size(max = 150) String material,
    @Size(max = 50) String fitType,
    @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal basePrice,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal salePrice,
    @NotNull Boolean featured,
    @NotNull Boolean newProduct
) {
}
