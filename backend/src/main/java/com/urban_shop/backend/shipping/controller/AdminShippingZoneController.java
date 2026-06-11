package com.urban_shop.backend.shipping.controller;

import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneUpdateRequest;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/admin/shipping-zones")
@RequiredArgsConstructor
public class AdminShippingZoneController {

    private final ShippingZoneService shippingZoneService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShippingZoneResponse create(@Valid @RequestBody ShippingZoneCreateRequest request) {
        return shippingZoneService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    public List<ShippingZoneResponse> list() {
        return shippingZoneService.listAdmin(currentTenant.requireId());
    }

    @GetMapping("/{id}")
    public ShippingZoneResponse get(@PathVariable UUID id) {
        return shippingZoneService.get(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    public ShippingZoneResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody ShippingZoneUpdateRequest request
    ) {
        return shippingZoneService.update(currentTenant.requireId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        shippingZoneService.delete(currentTenant.requireId(), id);
    }
}
