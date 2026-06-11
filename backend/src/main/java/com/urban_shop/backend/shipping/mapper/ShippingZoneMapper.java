package com.urban_shop.backend.shipping.mapper;

import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.entity.ShippingZone;

public final class ShippingZoneMapper {

    private ShippingZoneMapper() {
    }

    public static ShippingZoneResponse toResponse(ShippingZone zone) {
        return new ShippingZoneResponse(
            zone.getId(),
            zone.getName(),
            zone.getDepartment(),
            zone.getProvince(),
            zone.getDistrict(),
            zone.getPrice(),
            zone.getEstimatedTime(),
            zone.isActive(),
            zone.getCreatedAt()
        );
    }
}
