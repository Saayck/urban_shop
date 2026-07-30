package com.urban_shop.backend.coupon.service;

import com.urban_shop.backend.coupon.dto.request.CouponCreateRequest;
import com.urban_shop.backend.coupon.dto.request.CouponUpdateRequest;
import com.urban_shop.backend.coupon.dto.response.CouponPreviewResponse;
import com.urban_shop.backend.coupon.dto.response.CouponResponse;
import com.urban_shop.backend.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CouponService {

    CouponResponse create(UUID tenantId, CouponCreateRequest request);

    List<CouponResponse> list(UUID tenantId);

    CouponResponse get(UUID tenantId, UUID couponId);

    CouponResponse update(UUID tenantId, UUID couponId, CouponUpdateRequest request);

    void delete(UUID tenantId, UUID couponId);

    /**
     * Valida un cupon contra un subtotal sin consumirlo. Pensado para que el cliente
     * vea el descuento antes de confirmar la compra.
     */
    CouponPreviewResponse preview(UUID tenantId, UUID customerId, String code, BigDecimal subtotal);

    /**
     * Valida el cupon y devuelve la entidad lista para canjear. Lanza excepcion si no aplica.
     * Usado durante el checkout, dentro de la transaccion del pedido.
     */
    Coupon validateForCheckout(UUID tenantId, UUID customerId, String code, BigDecimal subtotal);

    BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal);

    /** Registra el canje e incrementa el contador de uso, bajo bloqueo pesimista. */
    void redeem(UUID tenantId, UUID customerId, UUID orderId, Coupon coupon, BigDecimal discount);

    /** Devuelve el cupon al inventario cuando el pedido que lo consumio se cancela. */
    void releaseForOrder(UUID tenantId, UUID orderId);
}
