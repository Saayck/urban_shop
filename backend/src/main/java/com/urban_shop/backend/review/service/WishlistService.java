package com.urban_shop.backend.review.service;

import com.urban_shop.backend.review.dto.response.WishlistItemResponse;

import java.util.List;
import java.util.UUID;

public interface WishlistService {

    List<WishlistItemResponse> list(UUID tenantId, UUID customerId);

    /** Anadir dos veces el mismo producto es idempotente: devuelve el item existente. */
    WishlistItemResponse add(UUID tenantId, UUID customerId, UUID productId);

    void remove(UUID tenantId, UUID customerId, UUID productId);
}
