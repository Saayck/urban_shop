package com.urban_shop.backend.brand.mapper;

import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.entity.Brand;

public final class BrandMapper {

    private BrandMapper() {
    }

    public static BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
            brand.getId(),
            brand.getName(),
            brand.getDescription(),
            brand.getLogoUrl(),
            brand.isActive(),
            brand.getCreatedAt(),
            brand.getUpdatedAt()
        );
    }
}
