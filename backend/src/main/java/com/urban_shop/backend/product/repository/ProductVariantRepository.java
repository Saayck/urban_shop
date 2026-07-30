package com.urban_shop.backend.product.repository;

import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.report.dto.LowStockResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findAllByProductIdOrderBySizeAscColorAsc(UUID productId);

    List<ProductVariant> findAllByProductIdIn(Collection<UUID> productIds);

    /** Variantes activas por debajo del umbral de stock, para alertas de reposicion. */
    @Query("""
        select new com.urban_shop.backend.report.dto.LowStockResponse(
            product.id,
            product.name,
            variant.id,
            variant.sku,
            variant.size,
            variant.color,
            variant.stock
        )
        from ProductVariant variant, Product product
        where variant.productId = product.id
          and product.tenantId = :tenantId
          and variant.active = true
          and variant.stock <= :threshold
        order by variant.stock asc, product.name asc
        """)
    List<LowStockResponse> findLowStock(
        @Param("tenantId") UUID tenantId,
        @Param("threshold") int threshold
    );

    boolean existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(UUID productId, String size, String color);

    boolean existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCaseAndIdNot(
        UUID productId,
        String size,
        String color,
        UUID id
    );

    @Query("""
        select variant from ProductVariant variant, Product product
        where variant.productId = product.id
          and product.tenantId = :tenantId
          and variant.id = :variantId
        """)
    Optional<ProductVariant> findByTenantIdAndId(
        @Param("tenantId") UUID tenantId,
        @Param("variantId") UUID variantId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select variant from ProductVariant variant, Product product
        where variant.productId = product.id
          and product.tenantId = :tenantId
          and variant.id = :variantId
        """)
    Optional<ProductVariant> findByTenantIdAndIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("variantId") UUID variantId
    );
}
