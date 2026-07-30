package com.urban_shop.backend.audit.repository;

import com.urban_shop.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
        select log from AuditLog log
        where log.tenantId = :tenantId
          and (:action is null or log.action = :action)
          and (:entityName is null or log.entityName = :entityName)
          and (:from is null or log.createdAt >= :from)
          and (:to is null or log.createdAt < :to)
        """)
    Page<AuditLog> search(
        @Param("tenantId") UUID tenantId,
        @Param("action") String action,
        @Param("entityName") String entityName,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}
