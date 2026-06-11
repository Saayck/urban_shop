package com.urban_shop.backend.order.dto.response;

import com.urban_shop.backend.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusHistoryResponse(
    UUID id,
    OrderStatus status,
    UUID changedBy,
    String notes,
    LocalDateTime createdAt
) {
}
