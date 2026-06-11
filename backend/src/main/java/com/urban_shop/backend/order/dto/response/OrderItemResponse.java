package com.urban_shop.backend.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID productId,
    UUID variantId,
    String productName,
    String size,
    String color,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {
}
