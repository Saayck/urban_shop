package com.urban_shop.backend.customer.dto.response;

import com.urban_shop.backend.customer.entity.CustomerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de un cliente para el panel de administracion, con sus metricas de compra.
 */
public record CustomerSummaryResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String documentType,
    String documentNumber,
    boolean emailVerified,
    CustomerStatus status,
    long totalOrders,
    BigDecimal totalSpent,
    LocalDateTime lastOrderAt,
    LocalDateTime createdAt
) {
}
