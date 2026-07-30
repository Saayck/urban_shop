package com.urban_shop.backend.category.service;

import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.request.CategoryUpdateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(UUID tenantId, CategoryCreateRequest request);

    List<CategoryResponse> listAdmin(UUID tenantId);

    List<CategoryResponse> listPublic(String tenantSlug);

    CategoryResponse get(UUID tenantId, UUID categoryId);

    CategoryResponse update(UUID tenantId, UUID categoryId, CategoryUpdateRequest request);

    /** Elimina la categoria. Falla si tiene subcategorias o productos asociados. */
    void delete(UUID tenantId, UUID categoryId);
}
