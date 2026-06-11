package com.urban_shop.backend.category.mapper;

import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category, String parentName) {
        return new CategoryResponse(
            category.getId(),
            category.getParentId(),
            parentName,
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getImageUrl(),
            category.isActive(),
            category.getDisplayOrder(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
