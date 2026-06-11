package com.urban_shop.backend.complaint.repository;

import com.urban_shop.backend.complaint.entity.ComplaintBook;
import com.urban_shop.backend.complaint.entity.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<ComplaintBook, UUID> {

    Optional<ComplaintBook> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
        select c from ComplaintBook c
        where c.tenantId = :tenantId
          and (:status is null or c.status = :status)
        """)
    Page<ComplaintBook> findAdmin(
        @Param("tenantId") UUID tenantId,
        @Param("status") ComplaintStatus status,
        Pageable pageable
    );
}
