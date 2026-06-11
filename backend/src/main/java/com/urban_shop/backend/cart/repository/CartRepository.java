package com.urban_shop.backend.cart.repository;

import com.urban_shop.backend.cart.entity.Cart;
import com.urban_shop.backend.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByTenantIdAndCustomerIdAndStatus(
        UUID tenantId,
        UUID customerId,
        CartStatus status
    );
}
