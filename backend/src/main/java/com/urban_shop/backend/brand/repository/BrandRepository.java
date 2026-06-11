package com.urban_shop.backend.brand.repository;

import com.urban_shop.backend.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findAllByTenantIdOrderByNameAsc(UUID tenantId);

    List<Brand> findAllByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<Brand> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(UUID tenantId, String name, UUID id);
}
