package com.urban_shop.backend.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductResponse(
    UUID productId,
    String productName,
    long unitsSold,
    BigDecimal revenue
) {
}
