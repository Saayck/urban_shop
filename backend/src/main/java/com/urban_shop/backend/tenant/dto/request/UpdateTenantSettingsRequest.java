package com.urban_shop.backend.tenant.dto.request;

import com.urban_shop.backend.common.validation.HttpUrl;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTenantSettingsRequest(
    UUID templateId,
    @HttpUrl @Size(max = 2048) String logoUrl,
    @HttpUrl @Size(max = 2048) String bannerUrl,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String primaryColor,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String secondaryColor,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String accentColor,
    @Size(max = 100) String fontFamily,
    @Pattern(regexp = "^9\\d{8}$", message = "celular debe tener 9 digitos e iniciar con 9") String whatsappNumber,
    @HttpUrl @Size(max = 2048) String instagramUrl,
    @HttpUrl @Size(max = 2048) String facebookUrl,
    @HttpUrl @Size(max = 2048) String tiktokUrl
) {
}
