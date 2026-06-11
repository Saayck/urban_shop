package com.urban_shop.backend.tenant.repository;

import com.urban_shop.backend.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findBySlugAndStatusIn(String slug, List<String> statuses);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<Tenant> findAllByOrderByCreatedAtDesc();
}
