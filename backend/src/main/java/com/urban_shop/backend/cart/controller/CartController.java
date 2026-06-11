package com.urban_shop.backend.cart.controller;

import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.dto.request.CartItemUpdateRequest;
import com.urban_shop.backend.cart.dto.response.CartResponse;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CustomUserDetails principal) {
        requireCustomer(principal);
        return cartService.get(principal.getTenantId(), principal.getPrincipalId());
    }

    @PostMapping("/items")
    public CartResponse addItem(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody CartItemCreateRequest request
    ) {
        requireCustomer(principal);
        return cartService.addItem(principal.getTenantId(), principal.getPrincipalId(), request);
    }

    @PutMapping("/items/{id}")
    public CartResponse updateItem(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody CartItemUpdateRequest request
    ) {
        requireCustomer(principal);
        return cartService.updateItem(
            principal.getTenantId(),
            principal.getPrincipalId(),
            id,
            request
        );
    }

    @DeleteMapping("/items/{id}")
    public CartResponse deleteItem(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        requireCustomer(principal);
        return cartService.deleteItem(principal.getTenantId(), principal.getPrincipalId(), id);
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
