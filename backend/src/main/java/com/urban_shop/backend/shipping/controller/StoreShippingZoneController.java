package com.urban_shop.backend.shipping.controller;

import com.urban_shop.backend.shipping.dto.response.ShippingQuoteResponse;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/shipping-zones")
@Tag(name = "Store Shipping", description = "Zonas y costos de envio publicos de una tienda")
@RequiredArgsConstructor
@Validated
public class StoreShippingZoneController {

    private final ShippingZoneService shippingZoneService;

    @GetMapping
    @Operation(summary = "Listar zonas de envio activas de la tienda")
    public List<ShippingZoneResponse> list(@PathVariable String slug) {
        return shippingZoneService.listPublic(slug);
    }

    @GetMapping("/quote")
    @Operation(
        summary = "Cotizar el costo de envio de una direccion",
        description = "Permite al cliente conocer el costo de despacho antes de crear el pedido. "
            + "Si covered es false la tienda no cubre esa direccion."
    )
    public ShippingQuoteResponse quote(
        @PathVariable String slug,
        @RequestParam @NotBlank String department,
        @RequestParam @NotBlank String province,
        @RequestParam @NotBlank String district
    ) {
        return shippingZoneService.quotePublic(slug, department, province, district);
    }
}
