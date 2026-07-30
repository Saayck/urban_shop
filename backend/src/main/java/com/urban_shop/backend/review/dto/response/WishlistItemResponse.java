package com.urban_shop.backend.review.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WishlistItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String productSlug,
    BigDecimal price,
    String imageUrl,
    boolean available,
    LocalDateTime createdAt
) {
}
