package com.urban_shop.backend.product.dto.response;

import com.urban_shop.backend.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
    UUID id,
    UUID brandId,
    String brandName,
    UUID categoryId,
    String categoryName,
    String name,
    String slug,
    String description,
    String material,
    String fitType,
    BigDecimal basePrice,
    BigDecimal salePrice,
    BigDecimal effectivePrice,
    ProductStatus status,
    boolean featured,
    boolean newProduct,
    int totalStock,
    List<ProductImageResponse> images,
    List<ProductVariantResponse> variants,
    List<SizeGuideResponse> sizeGuides,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
