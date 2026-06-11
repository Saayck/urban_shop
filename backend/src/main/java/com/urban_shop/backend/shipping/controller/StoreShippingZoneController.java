package com.urban_shop.backend.shipping.controller;

import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store/{slug}/shipping-zones")
@RequiredArgsConstructor
public class StoreShippingZoneController {

    private final ShippingZoneService shippingZoneService;

    @GetMapping
    public List<ShippingZoneResponse> list(@PathVariable String slug) {
        return shippingZoneService.listPublic(slug);
    }
}
