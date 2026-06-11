package com.urban_shop.backend.inventory.dto.request;

import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InventoryMovementRequest(
    @NotNull UUID variantId,
    @NotNull InventoryMovementType movementType,
    @NotNull Integer quantity,
    @Size(max = 1000) String reason
) {
}
