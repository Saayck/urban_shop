package com.urban_shop.backend.cart.repository;

import com.urban_shop.backend.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCartIdOrderByCreatedAtAsc(UUID cartId);

    Optional<CartItem> findByCartIdAndVariantId(UUID cartId, UUID variantId);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
