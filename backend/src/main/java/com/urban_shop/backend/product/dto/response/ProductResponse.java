package com.urban_shop.backend.product.dto.response;

import com.urban_shop.backend.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID brandId,
    String brandName,
    UUID categoryId,
    String categoryName,
    String name,
    String slug,
    BigDecimal basePrice,
    BigDecimal salePrice,
    BigDecimal effectivePrice,
    ProductStatus status,
    boolean featured,
    boolean newProduct,
    String mainImageUrl,
    int totalStock,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
