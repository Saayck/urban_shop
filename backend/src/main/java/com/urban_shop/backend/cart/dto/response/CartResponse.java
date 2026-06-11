package com.urban_shop.backend.cart.dto.response;

import com.urban_shop.backend.cart.entity.CartStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartResponse(
    UUID id,
    CartStatus status,
    List<CartItemResponse> items,
    int totalItems,
    BigDecimal subtotal,
    boolean readyForCheckout,
    LocalDateTime updatedAt
) {
}
