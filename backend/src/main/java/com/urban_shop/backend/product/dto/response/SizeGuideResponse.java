package com.urban_shop.backend.product.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SizeGuideResponse(
    UUID id,
    String size,
    BigDecimal chestCm,
    BigDecimal waistCm,
    BigDecimal hipCm,
    BigDecimal lengthCm,
    BigDecimal shoulderCm,
    BigDecimal sleeveCm,
    BigDecimal inseamCm,
    String notes
) {
}
