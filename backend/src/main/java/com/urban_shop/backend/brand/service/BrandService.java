package com.urban_shop.backend.brand.service;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.request.BrandUpdateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    BrandResponse create(UUID tenantId, BrandCreateRequest request);

    List<BrandResponse> listAdmin(UUID tenantId);

    List<BrandResponse> listPublic(String tenantSlug);

    BrandResponse get(UUID tenantId, UUID brandId);

    BrandResponse update(UUID tenantId, UUID brandId, BrandUpdateRequest request);

    /** Elimina la marca. Falla si tiene productos asociados: en ese caso hay que desactivarla. */
    void delete(UUID tenantId, UUID brandId);
}
