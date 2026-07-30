package com.urban_shop.backend.shipping.service;

import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneUpdateRequest;
import com.urban_shop.backend.shipping.dto.response.ShippingQuoteResponse;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;

import java.util.List;
import java.util.UUID;

public interface ShippingZoneService {

    ShippingZoneResponse create(UUID tenantId, ShippingZoneCreateRequest request);

    List<ShippingZoneResponse> listAdmin(UUID tenantId);

    ShippingZoneResponse get(UUID tenantId, UUID id);

    ShippingZoneResponse update(UUID tenantId, UUID id, ShippingZoneUpdateRequest request);

    void delete(UUID tenantId, UUID id);

    List<ShippingZoneResponse> listPublic(String tenantSlug);

    /**
     * Resuelve el costo de envio para una direccion. Gana la zona mas especifica:
     * distrito, luego provincia, luego departamento y por ultimo una zona comodin.
     */
    ShippingQuoteResponse quote(UUID tenantId, String department, String province, String district);

    ShippingQuoteResponse quotePublic(String tenantSlug, String department, String province, String district);
}
