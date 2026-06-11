package com.urban_shop.backend.product.dto.request;

import com.urban_shop.backend.common.validation.HttpUrl;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
    @NotBlank @HttpUrl @Size(max = 2048) String imageUrl,
    @Size(max = 150) String altText,
    @Min(0) Integer displayOrder,
    Boolean main
) {
}
