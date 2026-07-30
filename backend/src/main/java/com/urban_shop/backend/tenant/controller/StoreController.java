package com.urban_shop.backend.tenant.controller;

import com.urban_shop.backend.tenant.dto.response.StoreInfoResponse;
import com.urban_shop.backend.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Pattern;

@RestController
@SecurityRequirements
@RequestMapping("/api/store")
@RequiredArgsConstructor
@Validated
public class StoreController {

    private final TenantService tenantService;

    @GetMapping("/{slug}")
    public StoreInfoResponse getBySlug(
        @PathVariable
        @Pattern(
            regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$",
            message = "slug debe ser minusculas, numeros y guiones"
        )
        String slug
    ) {
        return tenantService.getPublicBySlug(slug);
    }
}
