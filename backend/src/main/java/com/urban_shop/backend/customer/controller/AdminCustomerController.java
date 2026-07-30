package com.urban_shop.backend.customer.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.customer.dto.request.CustomerStatusRequest;
import com.urban_shop.backend.customer.dto.response.CustomerSummaryResponse;
import com.urban_shop.backend.customer.entity.CustomerStatus;
import com.urban_shop.backend.customer.service.AdminCustomerService;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
@Tag(name = "Admin Customers", description = "Gestion de los clientes de la tienda")
@RequiredArgsConstructor
@Validated
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;
    private final CurrentTenant currentTenant;

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(
        summary = "Listar clientes de la tienda",
        description = "Incluye pedidos totales, gasto acumulado y fecha de la ultima compra. "
            + "El parametro search busca por nombre, correo, telefono o documento."
    )
    public PageResponse<CustomerSummaryResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) CustomerStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminCustomerService.list(currentTenant.requireId(), search, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Ficha de un cliente con sus metricas de compra")
    public CustomerSummaryResponse get(@PathVariable UUID id) {
        return adminCustomerService.get(currentTenant.requireId(), id);
    }

    @GetMapping("/{id}/orders")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Historial de pedidos de un cliente")
    public PageResponse<OrderResponse> listOrders(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminCustomerService.listOrders(currentTenant.requireId(), id, page, size);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Activar, desactivar o bloquear a un cliente",
        description = "Cualquier estado distinto de ACTIVE revoca las sesiones abiertas del cliente."
    )
    public CustomerSummaryResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody CustomerStatusRequest request
    ) {
        return adminCustomerService.updateStatus(currentTenant.requireId(), id, request);
    }
}
