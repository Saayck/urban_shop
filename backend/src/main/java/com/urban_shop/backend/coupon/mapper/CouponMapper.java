package com.urban_shop.backend.coupon.mapper;

import com.urban_shop.backend.coupon.dto.response.CouponResponse;
import com.urban_shop.backend.coupon.entity.Coupon;

public final class CouponMapper {

    private CouponMapper() {
    }

    public static CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
            coupon.getId(),
            coupon.getCode(),
            coupon.getDescription(),
            coupon.getDiscountType(),
            coupon.getDiscountValue(),
            coupon.getMaxDiscountAmount(),
            coupon.getMinPurchaseAmount(),
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            coupon.getUsageLimit(),
            coupon.getUsedCount(),
            coupon.getPerCustomerLimit(),
            coupon.isActive(),
            coupon.getCreatedAt(),
            coupon.getUpdatedAt()
        );
    }
}
