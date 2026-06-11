package com.urban_shop.backend.tenant.repository;

import com.urban_shop.backend.tenant.entity.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {

    Optional<TenantSettings> findByTenantId(UUID tenantId);
}
