package com.urban_shop.backend.inventory.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.dto.response.InventoryMovementResponse;

import java.util.UUID;

public interface InventoryService {

    InventoryMovementResponse create(UUID tenantId, InventoryMovementRequest request);

    PageResponse<InventoryMovementResponse> list(UUID tenantId, int page, int size);

    void recordInitialStock(UUID tenantId, UUID variantId, int quantity);
}
