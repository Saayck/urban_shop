package com.urban_shop.backend.cart.repository;

import com.urban_shop.backend.cart.entity.Cart;
import com.urban_shop.backend.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByTenantIdAndCustomerIdAndStatus(
        UUID tenantId,
        UUID customerId,
        CartStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select cart from Cart cart
        where cart.tenantId = :tenantId
          and cart.customerId = :customerId
          and cart.status = :status
        """)
    Optional<Cart> findByTenantIdAndCustomerIdAndStatusForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("customerId") UUID customerId,
        @Param("status") CartStatus status
    );
}
