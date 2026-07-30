package com.urban_shop.backend.review.repository;

import com.urban_shop.backend.review.dto.response.ReviewSummary;
import com.urban_shop.backend.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<ProductReview> findByProductIdAndCustomerId(UUID productId, UUID customerId);

    boolean existsByProductIdAndCustomerId(UUID productId, UUID customerId);

    Page<ProductReview> findAllByProductIdAndVisibleTrueOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    Page<ProductReview> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<ProductReview> findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(
        UUID tenantId,
        UUID customerId,
        Pageable pageable
    );

    /** Promedio y conteo de las resenas visibles de un producto. */
    @Query("""
        select new com.urban_shop.backend.review.dto.response.ReviewSummary(
            count(review),
            coalesce(avg(review.rating), 0)
        )
        from ProductReview review
        where review.productId = :productId
          and review.visible = true
        """)
    ReviewSummary summarizeForProduct(@Param("productId") UUID productId);
}
