package com.urban_shop.backend.product.repository;

import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Product> findByTenantIdAndSlug(UUID tenantId, String slug);

    Optional<Product> findByTenantIdAndSlugAndStatusIn(
        UUID tenantId,
        String slug,
        Collection<ProductStatus> statuses
    );

    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndBrandId(UUID tenantId, UUID brandId);

    boolean existsByTenantIdAndCategoryId(UUID tenantId, UUID categoryId);

    boolean existsByTenantIdAndSlugAndIdNot(UUID tenantId, String slug, UUID id);

    @Query("""
        select p from Product p
        where p.tenantId = :tenantId
          and (:status is null or p.status = :status)
          and (:brandId is null or p.brandId = :brandId)
          and (:categoryId is null or p.categoryId = :categoryId)
        """)
    Page<Product> findAdmin(
        @Param("tenantId") UUID tenantId,
        @Param("status") ProductStatus status,
        @Param("brandId") UUID brandId,
        @Param("categoryId") UUID categoryId,
        Pageable pageable
    );

    @Query("""
        select p from Product p
        where p.tenantId = :tenantId
          and p.status in :statuses
          and exists (
              select category.id from Category category
              where category.id = p.categoryId
                and category.tenantId = :tenantId
                and category.active = true
          )
          and (
              p.brandId is null
              or exists (
                  select brand.id from Brand brand
                  where brand.id = p.brandId
                    and brand.tenantId = :tenantId
                    and brand.active = true
              )
          )
          and (:brandId is null or p.brandId = :brandId)
          and (:categoryId is null or p.categoryId = :categoryId)
        """)
    Page<Product> findPublic(
        @Param("tenantId") UUID tenantId,
        @Param("statuses") Collection<ProductStatus> statuses,
        @Param("brandId") UUID brandId,
        @Param("categoryId") UUID categoryId,
        Pageable pageable
    );
}
