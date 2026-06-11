package com.urban_shop.backend.product.repository;

import com.urban_shop.backend.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findAllByProductIdOrderByDisplayOrderAscCreatedAtAsc(UUID productId);

    List<ProductImage> findAllByProductIdInOrderByDisplayOrderAscCreatedAtAsc(Collection<UUID> productIds);

    Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ProductImage image set image.main = false where image.productId = :productId")
    void clearMainImage(UUID productId);
}
