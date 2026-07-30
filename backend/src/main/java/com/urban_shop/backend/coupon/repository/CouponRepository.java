package com.urban_shop.backend.coupon.repository;

import com.urban_shop.backend.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Coupon> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Coupon> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(UUID tenantId, String code, UUID id);

    /**
     * Bloquea la fila para incrementar {@code usedCount} sin condiciones de carrera:
     * dos checkouts simultaneos no deben poder superar el limite de canjes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select coupon from Coupon coupon
        where coupon.tenantId = :tenantId and coupon.id = :couponId
        """)
    Optional<Coupon> findByTenantIdAndIdForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("couponId") UUID couponId
    );
}
