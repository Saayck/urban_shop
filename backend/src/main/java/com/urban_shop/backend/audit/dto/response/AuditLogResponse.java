package com.urban_shop.backend.audit.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID tenantId,
    UUID userId,
    String action,
    String entityName,
    UUID entityId,
    String oldValue,
    String newValue,
    String ipAddress,
    String userAgent,
    LocalDateTime createdAt
) {
}
