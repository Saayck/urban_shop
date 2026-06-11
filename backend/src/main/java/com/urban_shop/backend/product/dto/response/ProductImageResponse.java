package com.urban_shop.backend.product.dto.response;

import java.util.UUID;

public record ProductImageResponse(
    UUID id,
    String imageUrl,
    String altText,
    int displayOrder,
    boolean main
) {
}
