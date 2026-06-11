package com.urban_shop.backend.category.repository;

import com.urban_shop.backend.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByTenantIdOrderByDisplayOrderAscNameAsc(UUID tenantId);

    Optional<Category> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);

    boolean existsByTenantIdAndSlugAndIdNot(UUID tenantId, String slug, UUID id);
}
