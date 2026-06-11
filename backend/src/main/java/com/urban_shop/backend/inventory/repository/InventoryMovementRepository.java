package com.urban_shop.backend.inventory.repository;

import com.urban_shop.backend.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findAllByTenantId(UUID tenantId, Pageable pageable);
}
