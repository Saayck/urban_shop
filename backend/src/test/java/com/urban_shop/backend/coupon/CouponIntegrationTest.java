package com.urban_shop.backend.coupon;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.coupon.dto.request.CouponCreateRequest;
import com.urban_shop.backend.coupon.dto.request.CouponUpdateRequest;
import com.urban_shop.backend.coupon.dto.response.CouponPreviewResponse;
import com.urban_shop.backend.coupon.dto.response.CouponResponse;
import com.urban_shop.backend.coupon.entity.Coupon;
import com.urban_shop.backend.coupon.entity.DiscountType;
import com.urban_shop.backend.coupon.service.CouponService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CouponIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void calculatesPercentageDiscountRespectingTheCap() {
        UUID tenantId = createTenant("coupon-percentage").getId();
        CouponResponse created = couponService.create(tenantId, new CouponCreateRequest(
            "VERANO20",
            "20% de descuento",
            DiscountType.PERCENTAGE,
            new BigDecimal("20"),
            new BigDecimal("30.00"),
            BigDecimal.ZERO,
            null,
            null,
            null,
            1
        ));
        Coupon coupon = couponService.validateForCheckout(
            tenantId,
            UUID.randomUUID(),
            created.code(),
            new BigDecimal("100.00")
        );

        // 20% de 100 = 20, por debajo del tope.
        assertThat(couponService.calculateDiscount(coupon, new BigDecimal("100.00")))
            .isEqualByComparingTo("20.00");
        // 20% de 500 = 100, pero el tope lo limita a 30.
        assertThat(couponService.calculateDiscount(coupon, new BigDecimal("500.00")))
            .isEqualByComparingTo("30.00");
    }

    @Test
    void neverDiscountsMoreThanTheSubtotal() {
        UUID tenantId = createTenant("coupon-overflow").getId();
        couponService.create(tenantId, fixedAmount("GRANDE", new BigDecimal("500.00")));
        Coupon coupon = couponService.validateForCheckout(
            tenantId,
            UUID.randomUUID(),
            "GRANDE",
            new BigDecimal("80.00")
        );

        assertThat(couponService.calculateDiscount(coupon, new BigDecimal("80.00")))
            .isEqualByComparingTo("80.00");
    }

    @Test
    void rejectsCouponsThatAreInactiveExpiredOrBelowMinimumPurchase() {
        UUID tenantId = createTenant("coupon-rejections").getId();
        UUID customerId = UUID.randomUUID();

        CouponResponse expired = couponService.create(tenantId, new CouponCreateRequest(
            "EXPIRADO", null, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null,
            BigDecimal.ZERO,
            LocalDateTime.now().minusDays(10),
            LocalDateTime.now().minusDays(1),
            null, 1
        ));
        assertThatThrownBy(() -> couponService.validateForCheckout(
            tenantId, customerId, expired.code(), new BigDecimal("100.00")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("vigente");

        CouponResponse minimum = couponService.create(tenantId, new CouponCreateRequest(
            "MINIMO", null, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null,
            new BigDecimal("200.00"), null, null, null, 1
        ));
        assertThatThrownBy(() -> couponService.validateForCheckout(
            tenantId, customerId, minimum.code(), new BigDecimal("100.00")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("compra minima");

        CouponResponse active = couponService.create(tenantId, fixedAmount("APAGADO", new BigDecimal("10.00")));
        couponService.update(tenantId, active.id(), new CouponUpdateRequest(
            null, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null,
            BigDecimal.ZERO, null, null, null, 1, false
        ));
        assertThatThrownBy(() -> couponService.validateForCheckout(
            tenantId, customerId, "APAGADO", new BigDecimal("100.00")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("no esta activo");
    }

    @Test
    void enforcesGlobalAndPerCustomerUsageLimits() {
        UUID tenantId = createTenant("coupon-limits").getId();
        UUID customerId = UUID.randomUUID();
        CouponResponse created = couponService.create(tenantId, new CouponCreateRequest(
            "UNICO", null, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null,
            BigDecimal.ZERO, null, null, 1, 1
        ));

        Coupon coupon = couponService.validateForCheckout(
            tenantId, customerId, created.code(), new BigDecimal("100.00")
        );
        couponService.redeem(tenantId, customerId, UUID.randomUUID(), coupon, new BigDecimal("10.00"));

        // El mismo cliente ya lo uso y ademas se agoto el limite global.
        assertThatThrownBy(() -> couponService.validateForCheckout(
            tenantId, customerId, "UNICO", new BigDecimal("100.00")
        )).isInstanceOf(BusinessException.class);

        assertThat(couponService.get(tenantId, created.id()).usedCount()).isEqualTo(1);
    }

    @Test
    void previewReportsTheReasonInsteadOfFailing() {
        UUID tenantId = createTenant("coupon-preview").getId();
        couponService.create(tenantId, new CouponCreateRequest(
            "MIN50", null, DiscountType.PERCENTAGE, new BigDecimal("10"), null,
            new BigDecimal("50.00"), null, null, null, 1
        ));

        CouponPreviewResponse ok = couponService.preview(
            tenantId, UUID.randomUUID(), "min50", new BigDecimal("100.00")
        );
        assertThat(ok.valid()).isTrue();
        assertThat(ok.discount()).isEqualByComparingTo("10.00");
        assertThat(ok.totalAfterDiscount()).isEqualByComparingTo("90.00");

        CouponPreviewResponse rejected = couponService.preview(
            tenantId, UUID.randomUUID(), "MIN50", new BigDecimal("20.00")
        );
        assertThat(rejected.valid()).isFalse();
        assertThat(rejected.discount()).isEqualByComparingTo("0");
        assertThat(rejected.reason()).contains("compra minima");

        CouponPreviewResponse unknown = couponService.preview(
            tenantId, UUID.randomUUID(), "NOEXISTE", new BigDecimal("100.00")
        );
        assertThat(unknown.valid()).isFalse();
    }

    @Test
    void releasesTheCouponWhenTheOrderIsCancelled() {
        UUID tenantId = createTenant("coupon-release").getId();
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        CouponResponse created = couponService.create(tenantId, fixedAmount("DEVUELTO", new BigDecimal("15.00")));

        Coupon coupon = couponService.validateForCheckout(
            tenantId, customerId, created.code(), new BigDecimal("100.00")
        );
        couponService.redeem(tenantId, customerId, orderId, coupon, new BigDecimal("15.00"));
        assertThat(couponService.get(tenantId, created.id()).usedCount()).isEqualTo(1);

        couponService.releaseForOrder(tenantId, orderId);

        assertThat(couponService.get(tenantId, created.id()).usedCount()).isZero();
        // Liberado el canje, el cliente puede volver a usarlo.
        assertThat(couponService.preview(tenantId, customerId, "DEVUELTO", new BigDecimal("100.00")).valid())
            .isTrue();
    }

    @Test
    void rejectsDuplicateCodesAndDeletionOfRedeemedCoupons() {
        UUID tenantId = createTenant("coupon-integrity").getId();
        CouponResponse created = couponService.create(tenantId, fixedAmount("REPETIDO", new BigDecimal("10.00")));

        assertThatThrownBy(() -> couponService.create(tenantId, fixedAmount("repetido", new BigDecimal("10.00"))))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe un cupon");

        Coupon coupon = couponService.validateForCheckout(
            tenantId, UUID.randomUUID(), "REPETIDO", new BigDecimal("100.00")
        );
        couponService.redeem(tenantId, UUID.randomUUID(), UUID.randomUUID(), coupon, new BigDecimal("10.00"));

        assertThatThrownBy(() -> couponService.delete(tenantId, created.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ya canjeado");
    }

    private CouponCreateRequest fixedAmount(String code, BigDecimal value) {
        return new CouponCreateRequest(
            code, null, DiscountType.FIXED_AMOUNT, value, null,
            BigDecimal.ZERO, null, null, null, 1
        );
    }

    private Tenant createTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug);
        tenant.setStatus("ACTIVE");
        tenant.setPlanName("BASIC");
        return tenantRepository.save(tenant);
    }
}
