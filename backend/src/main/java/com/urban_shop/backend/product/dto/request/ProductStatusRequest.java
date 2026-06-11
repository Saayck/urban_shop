package com.urban_shop.backend.product.dto.request;

import com.urban_shop.backend.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
    @NotNull ProductStatus status
) {
}
