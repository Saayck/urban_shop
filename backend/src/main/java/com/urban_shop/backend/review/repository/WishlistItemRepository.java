package com.urban_shop.backend.review.repository;

import com.urban_shop.backend.review.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    Optional<WishlistItem> findByTenantIdAndCustomerIdAndProductId(
        UUID tenantId,
        UUID customerId,
        UUID productId
    );

    boolean existsByCustomerIdAndProductId(UUID customerId, UUID productId);
}
