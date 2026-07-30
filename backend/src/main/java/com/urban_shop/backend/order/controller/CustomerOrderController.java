package com.urban_shop.backend.order.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.order.dto.request.OrderCancelRequest;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
@Tag(name = "Customer Orders", description = "Pedidos del cliente autenticado")
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

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "Cancelar el propio pedido",
        description = "Solo permitido mientras el pedido siga en estado CREATED y sin pago confirmado. "
            + "Repone el stock y anula los pagos pendientes."
    )
    public OrderDetailResponse cancel(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) OrderCancelRequest request
    ) {
        requireCustomer(principal);
        return orderService.cancelByCustomer(
            principal.getTenantId(),
            principal.getPrincipalId(),
            id,
            request
        );
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
