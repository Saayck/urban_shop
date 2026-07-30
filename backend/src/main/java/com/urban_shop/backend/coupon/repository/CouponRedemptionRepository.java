package com.urban_shop.backend.coupon.repository;

import com.urban_shop.backend.coupon.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    long countByCouponIdAndCustomerId(UUID couponId, UUID customerId);

    Optional<CouponRedemption> findByOrderId(UUID orderId);
}
