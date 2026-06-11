package com.urban_shop.backend.inventory.dto.response;

import com.urban_shop.backend.inventory.entity.InventoryMovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
    UUID id,
    UUID variantId,
    InventoryMovementType movementType,
    int quantity,
    Integer stockAfter,
    String reason,
    LocalDateTime createdAt
) {
}
