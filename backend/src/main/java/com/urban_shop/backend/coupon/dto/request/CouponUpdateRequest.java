package com.urban_shop.backend.coupon.dto.request;

import com.urban_shop.backend.coupon.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** El codigo no se puede cambiar: ya pudo haberse difundido a los clientes. */
public record CouponUpdateRequest(

    @Size(max = 255) String description,

    @NotNull DiscountType discountType,

    @NotNull
    @DecimalMin(value = "0.01", message = "el descuento debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2)
    BigDecimal discountValue,

    @Digits(integer = 8, fraction = 2)
    @DecimalMin(value = "0.01")
    BigDecimal maxDiscountAmount,

    @PositiveOrZero
    @Digits(integer = 8, fraction = 2)
    BigDecimal minPurchaseAmount,

    LocalDateTime validFrom,

    LocalDateTime validUntil,

    @Min(1) Integer usageLimit,

    @Min(1) Integer perCustomerLimit,

    @NotNull Boolean active
) {
}
