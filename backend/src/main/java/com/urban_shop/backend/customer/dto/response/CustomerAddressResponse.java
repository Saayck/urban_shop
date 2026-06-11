package com.urban_shop.backend.customer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerAddressResponse(
    UUID id,
    String department,
    String province,
    String district,
    String address,
    String reference,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean defaultAddress,
    LocalDateTime createdAt
) {
}
