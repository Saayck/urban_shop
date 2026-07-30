package com.urban_shop.backend.report.dto;

import java.util.UUID;

public record LowStockResponse(
    UUID productId,
    String productName,
    UUID variantId,
    String sku,
    String size,
    String color,
    int stock
) {
}
