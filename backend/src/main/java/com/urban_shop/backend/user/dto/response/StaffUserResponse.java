package com.urban_shop.backend.user.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record StaffUserResponse(
    UUID id,
    UUID tenantId,
    String fullName,
    String email,
    String phone,
    boolean active,
    Set<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
