package com.urban_shop.backend.review.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID productId,
    UUID customerId,
    String customerName,
    short rating,
    String title,
    String comment,
    boolean visible,
    String moderationNote,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
