package com.urban_shop.backend.shipping.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneUpdateRequest;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.entity.ShippingZone;
import com.urban_shop.backend.shipping.mapper.ShippingZoneMapper;
import com.urban_shop.backend.shipping.repository.ShippingZoneRepository;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingZoneServiceImpl implements ShippingZoneService {

    private final ShippingZoneRepository repository;
    private final PublicTenantResolver publicTenantResolver;

    @Override
    @Transactional
    public ShippingZoneResponse create(UUID tenantId, ShippingZoneCreateRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(tenantId, name, null);

        ShippingZone zone = new ShippingZone();
        zone.setTenantId(tenantId);
        zone.setName(name);
        zone.setDepartment(trimToNull(request.department()));
        zone.setProvince(trimToNull(request.province()));
        zone.setDistrict(trimToNull(request.district()));
        zone.setPrice(request.price());
        zone.setEstimatedTime(trimToNull(request.estimatedTime()));
        return ShippingZoneMapper.toResponse(repository.save(zone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> listAdmin(UUID tenantId) {
        return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
            .map(ShippingZoneMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingZoneResponse get(UUID tenantId, UUID id) {
        return ShippingZoneMapper.toResponse(requireZone(tenantId, id));
    }

    @Override
    @Transactional
    public ShippingZoneResponse update(UUID tenantId, UUID id, ShippingZoneUpdateRequest request) {
        ShippingZone zone = requireZone(tenantId, id);
        String name = request.name().trim();
        ensureNameAvailable(tenantId, name, id);

        zone.setName(name);
        zone.setDepartment(trimToNull(request.department()));
        zone.setProvince(trimToNull(request.province()));
        zone.setDistrict(trimToNull(request.district()));
        zone.setPrice(request.price());
        zone.setEstimatedTime(trimToNull(request.estimatedTime()));
        zone.setActive(request.active());
        return ShippingZoneMapper.toResponse(repository.save(zone));
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        repository.delete(requireZone(tenantId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> listPublic(String tenantSlug) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        return repository.findAllByTenantIdAndActiveTrueOrderByNameAsc(tenantId).stream()
            .map(ShippingZoneMapper::toResponse)
            .toList();
    }

    private ShippingZone requireZone(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Zona de envio no encontrada"));
    }

    private void ensureNameAvailable(UUID tenantId, String name, UUID currentId) {
        boolean exists = currentId == null
            ? repository.existsByTenantIdAndNameIgnoreCase(tenantId, name)
            : repository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, name, currentId);
        if (exists) {
            throw new BusinessException("Ya existe una zona de envio con ese nombre en la tienda");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
