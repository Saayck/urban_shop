package com.urban_shop.backend.auth.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserInfoResponse(
    UUID id,
    UUID tenantId,
    String email,
    String fullName,
    Set<String> roles,
    String principalType
) {
}
