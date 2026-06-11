package com.urban_shop.backend.shipping.repository;

import com.urban_shop.backend.shipping.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingZoneRepository extends JpaRepository<ShippingZone, UUID> {

    List<ShippingZone> findAllByTenantIdOrderByNameAsc(UUID tenantId);

    List<ShippingZone> findAllByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<ShippingZone> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(UUID tenantId, String name, UUID id);
}
