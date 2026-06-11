package com.urban_shop.backend.tenant.controller;

import com.urban_shop.backend.tenant.dto.response.StoreInfoResponse;
import com.urban_shop.backend.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final TenantService tenantService;

    @GetMapping("/{slug}")
    public StoreInfoResponse getBySlug(@PathVariable String slug) {
        return tenantService.getPublicBySlug(slug);
    }
}
