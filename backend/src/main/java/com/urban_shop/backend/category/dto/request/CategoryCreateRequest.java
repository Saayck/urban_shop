package com.urban_shop.backend.category.dto.request;

import com.urban_shop.backend.common.validation.HttpUrl;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryCreateRequest(
    UUID parentId,
    @NotBlank @Size(max = 100) String name,
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "slug solo admite minusculas, numeros y guiones"
    )
    @Size(max = 120) String slug,
    @Size(max = 2000) String description,
    @HttpUrl @Size(max = 2048) String imageUrl,
    @Min(0) Integer displayOrder
) {
}
