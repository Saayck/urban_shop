package com.urban_shop.backend.brand.service;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.request.BrandUpdateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.entity.Brand;
import com.urban_shop.backend.brand.mapper.BrandMapper;
import com.urban_shop.backend.brand.repository.BrandRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PublicTenantResolver publicTenantResolver;

    @Override
    @Transactional
    public BrandResponse create(UUID tenantId, BrandCreateRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(tenantId, name, null);

        Brand brand = new Brand();
        brand.setTenantId(tenantId);
        brand.setName(name);
        brand.setDescription(trimToNull(request.description()));
        brand.setLogoUrl(trimToNull(request.logoUrl()));
        return BrandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listAdmin(UUID tenantId) {
        return brandRepository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
            .map(BrandMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listPublic(String tenantSlug) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        return brandRepository.findAllByTenantIdAndActiveTrueOrderByNameAsc(tenantId).stream()
            .map(BrandMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse get(UUID tenantId, UUID brandId) {
        return BrandMapper.toResponse(requireBrand(tenantId, brandId));
    }

    @Override
    @Transactional
    public BrandResponse update(UUID tenantId, UUID brandId, BrandUpdateRequest request) {
        Brand brand = requireBrand(tenantId, brandId);
        String name = request.name().trim();
        ensureNameAvailable(tenantId, name, brandId);

        brand.setName(name);
        brand.setDescription(trimToNull(request.description()));
        brand.setLogoUrl(trimToNull(request.logoUrl()));
        brand.setActive(request.active());
        return BrandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID brandId) {
        Brand brand = requireBrand(tenantId, brandId);
        if (productRepository.existsByTenantIdAndBrandId(tenantId, brandId)) {
            throw new BusinessException(
                "No se puede eliminar una marca con productos asociados. Desactivela en su lugar."
            );
        }
        brandRepository.delete(brand);
    }

    private Brand requireBrand(UUID tenantId, UUID brandId) {
        return brandRepository.findByTenantIdAndId(tenantId, brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
    }

    private void ensureNameAvailable(UUID tenantId, String name, UUID currentId) {
        boolean exists = currentId == null
            ? brandRepository.existsByTenantIdAndNameIgnoreCase(tenantId, name)
            : brandRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, name, currentId);
        if (exists) {
            throw new BusinessException("Ya existe una marca con ese nombre en la tienda");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
