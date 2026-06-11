package com.urban_shop.backend.tenant.controller;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.tenant.TenantContext;
import com.urban_shop.backend.tenant.dto.request.UpdateBusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
import com.urban_shop.backend.tenant.dto.response.TenantBusinessInfoResponse;
import com.urban_shop.backend.tenant.dto.response.TenantDetailResponse;
import com.urban_shop.backend.tenant.dto.response.TenantSettingsResponse;
import com.urban_shop.backend.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tenant")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantService tenantService;

    @GetMapping
    public TenantDetailResponse getCurrent() {
        return tenantService.getById(requireCurrentTenantId());
    }

    @GetMapping("/business-info")
    public TenantBusinessInfoResponse getBusinessInfo() {
        return tenantService.getBusinessInfo(requireCurrentTenantId());
    }

    @PutMapping("/business-info")
    public TenantBusinessInfoResponse updateBusinessInfo(@Valid @RequestBody UpdateBusinessInfoRequest request) {
        return tenantService.updateBusinessInfo(requireCurrentTenantId(), request);
    }

    @GetMapping("/settings")
    public TenantSettingsResponse getSettings() {
        return tenantService.getSettings(requireCurrentTenantId());
    }

    @PutMapping("/settings")
    public TenantSettingsResponse updateSettings(@Valid @RequestBody UpdateTenantSettingsRequest request) {
        return tenantService.updateSettings(requireCurrentTenantId(), request);
    }

    private UUID requireCurrentTenantId() {
        UUID id = TenantContext.getTenantId();
        if (id == null) {
            throw new BusinessException("Usuario sin tenant asignado. Use /api/super-admin/tenants para SUPER_ADMIN.");
        }
        return id;
    }
}
