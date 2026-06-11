package com.urban_shop.backend.tenant.dto.response;

import java.util.UUID;

public record TenantSettingsResponse(
    UUID templateId,
    String templateCode,
    String templateName,
    String logoUrl,
    String bannerUrl,
    String primaryColor,
    String secondaryColor,
    String accentColor,
    String fontFamily,
    String whatsappNumber,
    String instagramUrl,
    String facebookUrl,
    String tiktokUrl
) {
}
