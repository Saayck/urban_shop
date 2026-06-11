package com.urban_shop.backend.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CartItemCreateRequest(
    @NotNull UUID variantId,
    @NotNull @Min(1) Integer quantity
) {
}
