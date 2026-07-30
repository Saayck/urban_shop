package com.urban_shop.backend.report.dto;

import java.math.BigDecimal;

/** Fila agregada que devuelve la base de datos para el resumen de ventas. */
public record SalesSummary(
    long totalOrders,
    long completedOrders,
    long pendingOrders,
    long cancelledOrders,
    BigDecimal totalRevenue
) {
}
