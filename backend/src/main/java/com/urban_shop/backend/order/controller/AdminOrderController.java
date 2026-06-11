package com.urban_shop.backend.order.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.order.dto.request.OrderStatusUpdateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final OrderService orderService;
    private final CurrentTenant currentTenant;

    @GetMapping
    public PageResponse<OrderResponse> list(
        @RequestParam(required = false) OrderStatus orderStatus,
        @RequestParam(required = false) OrderPaymentStatus paymentStatus,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return orderService.listAdmin(
            currentTenant.requireId(),
            orderStatus,
            paymentStatus,
            page,
            size
        );
    }

    @GetMapping("/{id}")
    public OrderDetailResponse get(@PathVariable UUID id) {
        return orderService.getAdmin(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}/status")
    public OrderDetailResponse updateStatus(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        requireInternal(principal);
        return orderService.updateStatus(
            currentTenant.requireId(),
            principal.getPrincipalId(),
            id,
            request
        );
    }

    private void requireInternal(CustomUserDetails principal) {
        if (principal == null || principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un usuario interno");
        }
    }
}
