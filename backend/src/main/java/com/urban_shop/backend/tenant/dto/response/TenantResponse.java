package com.urban_shop.backend.tenant.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String name,
    String slug,
    String status,
    String planName,
    LocalDateTime createdAt
) {
}
