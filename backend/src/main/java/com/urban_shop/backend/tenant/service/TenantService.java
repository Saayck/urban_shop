package com.urban_shop.backend.tenant.service;

import com.urban_shop.backend.tenant.dto.request.CreateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateBusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
import com.urban_shop.backend.tenant.dto.response.StoreInfoResponse;
import com.urban_shop.backend.tenant.dto.response.TenantBusinessInfoResponse;
import com.urban_shop.backend.tenant.dto.response.TenantDetailResponse;
import com.urban_shop.backend.tenant.dto.response.TenantResponse;
import com.urban_shop.backend.tenant.dto.response.TenantSettingsResponse;

import java.util.List;
import java.util.UUID;

public interface TenantService {

    TenantDetailResponse create(CreateTenantRequest request);

    List<TenantResponse> list();

    TenantDetailResponse getById(UUID tenantId);

    TenantResponse suspend(UUID tenantId);

    TenantResponse activate(UUID tenantId);

    TenantBusinessInfoResponse getBusinessInfo(UUID tenantId);

    TenantBusinessInfoResponse updateBusinessInfo(UUID tenantId, UpdateBusinessInfoRequest request);

    TenantSettingsResponse getSettings(UUID tenantId);

    TenantSettingsResponse updateSettings(UUID tenantId, UpdateTenantSettingsRequest request);

    StoreInfoResponse getPublicBySlug(String slug);
}
