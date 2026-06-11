package com.urban_shop.backend.brand.dto.request;

import com.urban_shop.backend.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 2000) String description,
    @HttpUrl @Size(max = 2048) String logoUrl
) {
}
