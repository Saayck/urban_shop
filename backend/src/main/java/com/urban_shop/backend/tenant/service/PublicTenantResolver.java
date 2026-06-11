package com.urban_shop.backend.tenant.service;

import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.util.SlugUtils;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PublicTenantResolver {

    private static final List<String> PUBLIC_STATUSES = List.of("ACTIVE", "TRIAL");

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public UUID requireTenantId(String slug) {
        String normalizedSlug;
        try {
            normalizedSlug = SlugUtils.normalizeOrGenerate(slug, slug);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Tienda no encontrada");
        }
        return tenantRepository.findBySlugAndStatusIn(normalizedSlug, PUBLIC_STATUSES)
            .map(tenant -> tenant.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada"));
    }
}
