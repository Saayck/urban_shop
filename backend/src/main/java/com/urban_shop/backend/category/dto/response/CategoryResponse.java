package com.urban_shop.backend.category.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    UUID parentId,
    String parentName,
    String name,
    String slug,
    String description,
    String imageUrl,
    boolean active,
    int displayOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
