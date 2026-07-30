package com.urban_shop.backend.user.service;

import com.urban_shop.backend.user.dto.request.StaffUserCreateRequest;
import com.urban_shop.backend.user.dto.request.StaffUserUpdateRequest;
import com.urban_shop.backend.user.dto.response.StaffUserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Gestion del personal interno de una tienda (TENANT_ADMIN y SALES_STAFF).
 */
public interface StaffUserService {

    StaffUserResponse create(UUID tenantId, StaffUserCreateRequest request);

    List<StaffUserResponse> list(UUID tenantId);

    StaffUserResponse get(UUID tenantId, UUID userId);

    StaffUserResponse update(UUID tenantId, UUID actingUserId, UUID userId, StaffUserUpdateRequest request);

    /** Desactiva al usuario y revoca sus sesiones; no borra el registro por trazabilidad. */
    void deactivate(UUID tenantId, UUID actingUserId, UUID userId);
}
