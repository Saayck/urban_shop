package com.urban_shop.backend.shipping.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShippingZoneResponse(
    UUID id,
    String name,
    String department,
    String province,
    String district,
    BigDecimal price,
    String estimatedTime,
    boolean active,
    LocalDateTime createdAt
) {
}
