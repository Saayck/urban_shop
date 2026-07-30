package com.urban_shop.backend.shipping.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Costo de envio calculado para una direccion concreta.
 *
 * @param covered false cuando la tienda no tiene zonas configuradas o ninguna cubre la direccion
 */
public record ShippingQuoteResponse(
    UUID zoneId,
    String zoneName,
    BigDecimal price,
    String estimatedTime,
    boolean covered
) {

    public static ShippingQuoteResponse notCovered() {
        return new ShippingQuoteResponse(null, null, BigDecimal.ZERO, null, false);
    }
}
