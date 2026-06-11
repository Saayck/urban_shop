package com.urban_shop.backend.tenant.controller;

import com.urban_shop.backend.tenant.dto.request.CreateTenantRequest;
import com.urban_shop.backend.tenant.dto.response.TenantDetailResponse;
import com.urban_shop.backend.tenant.dto.response.TenantResponse;
import com.urban_shop.backend.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin/tenants")
@RequiredArgsConstructor
public class SuperAdminTenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDetailResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.list();
    }

    @GetMapping("/{id}")
    public TenantDetailResponse getById(@PathVariable UUID id) {
        return tenantService.getById(id);
    }

    @PutMapping("/{id}/suspend")
    public TenantResponse suspend(@PathVariable UUID id) {
        return tenantService.suspend(id);
    }

    @PutMapping("/{id}/activate")
    public TenantResponse activate(@PathVariable UUID id) {
        return tenantService.activate(id);
    }
}
