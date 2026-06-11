package com.urban_shop.backend.brand.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BrandResponse(
    UUID id,
    String name,
    String description,
    String logoUrl,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
