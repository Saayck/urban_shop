package com.urban_shop.backend.template.dto.response;

import java.util.UUID;

public record StoreTemplateResponse(
    UUID id,
    String name,
    String code,
    String description,
    String previewImageUrl,
    boolean active
) {
}
