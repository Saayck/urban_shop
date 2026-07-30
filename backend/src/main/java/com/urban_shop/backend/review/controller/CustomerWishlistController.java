package com.urban_shop.backend.review.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.review.dto.response.WishlistItemResponse;
import com.urban_shop.backend.review.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/wishlist")
@Tag(name = "Customer Wishlist", description = "Lista de deseos del cliente")
@RequiredArgsConstructor
public class CustomerWishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(
        summary = "Listar la lista de deseos",
        description = "El campo available indica si el producto sigue publicado y se puede comprar."
    )
    public List<WishlistItemResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        requireCustomer(principal);
        return wishlistService.list(principal.getTenantId(), principal.getPrincipalId());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anadir un producto", description = "Es idempotente: repetirlo no duplica el item.")
    public WishlistItemResponse add(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID productId
    ) {
        requireCustomer(principal);
        return wishlistService.add(principal.getTenantId(), principal.getPrincipalId(), productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar un producto de la lista de deseos")
    public void remove(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID productId
    ) {
        requireCustomer(principal);
        wishlistService.remove(principal.getTenantId(), principal.getPrincipalId(), productId);
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
