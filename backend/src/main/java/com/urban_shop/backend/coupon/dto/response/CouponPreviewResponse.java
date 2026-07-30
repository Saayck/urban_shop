package com.urban_shop.backend.coupon.dto.response;

import java.math.BigDecimal;

/**
 * Resultado de validar un cupon contra un subtotal, antes de crear el pedido.
 *
 * @param reason motivo del rechazo cuando {@code valid} es false
 */
public record CouponPreviewResponse(
    boolean valid,
    String code,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal totalAfterDiscount,
    String reason
) {

    public static CouponPreviewResponse rejected(String code, BigDecimal subtotal, String reason) {
        return new CouponPreviewResponse(false, code, subtotal, BigDecimal.ZERO, subtotal, reason);
    }
}
