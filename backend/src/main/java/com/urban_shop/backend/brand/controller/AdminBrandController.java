package com.urban_shop.backend.brand.controller;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.request.BrandUpdateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.service.BrandService;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public BrandResponse create(@Valid @RequestBody BrandCreateRequest request) {
        return brandService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    public List<BrandResponse> list() {
        return brandService.listAdmin(currentTenant.requireId());
    }

    @GetMapping("/{id}")
    public BrandResponse get(@PathVariable UUID id) {
        return brandService.get(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public BrandResponse update(@PathVariable UUID id, @Valid @RequestBody BrandUpdateRequest request) {
        return brandService.update(currentTenant.requireId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void delete(@PathVariable UUID id) {
        brandService.delete(currentTenant.requireId(), id);
    }
}
