package com.urban_shop.backend.inventory.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.dto.response.InventoryMovementResponse;
import com.urban_shop.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory/movements")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public InventoryMovementResponse create(@Valid @RequestBody InventoryMovementRequest request) {
        return inventoryService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    public PageResponse<InventoryMovementResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return inventoryService.list(currentTenant.requireId(), page, size);
    }
}
