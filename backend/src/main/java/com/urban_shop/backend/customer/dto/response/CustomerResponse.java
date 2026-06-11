package com.urban_shop.backend.customer.dto.response;

import com.urban_shop.backend.customer.entity.CustomerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    UUID tenantId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String documentType,
    String documentNumber,
    boolean emailVerified,
    boolean phoneVerified,
    CustomerStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
