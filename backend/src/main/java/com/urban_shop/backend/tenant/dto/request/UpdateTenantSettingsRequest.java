package com.urban_shop.backend.tenant.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTenantSettingsRequest(
    UUID templateId,
    String logoUrl,
    String bannerUrl,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String primaryColor,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String secondaryColor,
    @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "color debe ser hex tipo #RRGGBB") String accentColor,
    @Size(max = 100) String fontFamily,
    @Pattern(regexp = "^9\\d{8}$", message = "celular debe tener 9 digitos e iniciar con 9") String whatsappNumber,
    String instagramUrl,
    String facebookUrl,
    String tiktokUrl
) {
}
