package com.urban_shop.backend.order.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@Validated
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse create(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody OrderCreateRequest request
    ) {
        requireCustomer(principal);
        return orderService.create(principal.getTenantId(), principal.getPrincipalId(), request);
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        requireCustomer(principal);
        return orderService.listCustomer(
            principal.getTenantId(),
            principal.getPrincipalId(),
            page,
            size
        );
    }

    @GetMapping("/{id}")
    public OrderDetailResponse get(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        requireCustomer(principal);
        return orderService.getCustomer(principal.getTenantId(), principal.getPrincipalId(), id);
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
