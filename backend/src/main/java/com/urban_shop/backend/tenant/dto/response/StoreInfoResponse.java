package com.urban_shop.backend.tenant.dto.response;

import java.util.UUID;

public record StoreInfoResponse(
    UUID id,
    String name,
    String slug,
    String status,
    StoreBusinessInfoResponse businessInfo,
    TenantSettingsResponse settings
) {

}
