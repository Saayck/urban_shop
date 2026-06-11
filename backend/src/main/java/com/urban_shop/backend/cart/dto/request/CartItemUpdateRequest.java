package com.urban_shop.backend.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(
    @NotNull @Min(1) Integer quantity
) {
}
