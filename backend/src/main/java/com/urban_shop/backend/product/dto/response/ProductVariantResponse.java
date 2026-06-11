package com.urban_shop.backend.product.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantResponse(
    UUID id,
    String sku,
    String size,
    String color,
    String colorHex,
    int stock,
    BigDecimal price,
    boolean active
) {
}
