package com.urban_shop.backend.order.dto.response;

import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID customerId,
    BigDecimal total,
    OrderPaymentStatus paymentStatus,
    OrderStatus orderStatus,
    DeliveryType deliveryType,
    int totalItems,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
