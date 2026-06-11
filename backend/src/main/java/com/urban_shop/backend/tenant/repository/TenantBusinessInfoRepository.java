package com.urban_shop.backend.tenant.repository;

import com.urban_shop.backend.tenant.entity.TenantBusinessInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantBusinessInfoRepository extends JpaRepository<TenantBusinessInfo, UUID> {

    Optional<TenantBusinessInfo> findByTenantId(UUID tenantId);
}
