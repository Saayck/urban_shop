package com.urban_shop.backend.order.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Metricas de compra de un cliente, agregadas en base de datos. */
public record CustomerPurchaseStats(
    UUID customerId,
    long totalOrders,
    BigDecimal totalSpent,
    LocalDateTime lastOrderAt
) {
}
