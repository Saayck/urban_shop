package com.urban_shop.backend.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String productSlug,
    String imageUrl,
    UUID variantId,
    String sku,
    String size,
    String color,
    String colorHex,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    int availableStock,
    boolean available
) {
}
