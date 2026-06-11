package com.urban_shop.backend.cart.service;

import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.dto.request.CartItemUpdateRequest;
import com.urban_shop.backend.cart.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse get(UUID tenantId, UUID customerId);

    CartResponse addItem(UUID tenantId, UUID customerId, CartItemCreateRequest request);

    CartResponse updateItem(
        UUID tenantId,
        UUID customerId,
        UUID itemId,
        CartItemUpdateRequest request
    );

    CartResponse deleteItem(UUID tenantId, UUID customerId, UUID itemId);
}
