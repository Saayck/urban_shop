package com.urban_shop.backend.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportResponse(
    LocalDate from,
    LocalDate to,
    long totalOrders,
    long completedOrders,
    long pendingOrders,
    long cancelledOrders,
    BigDecimal totalRevenue,
    BigDecimal averageTicket
) {
}
