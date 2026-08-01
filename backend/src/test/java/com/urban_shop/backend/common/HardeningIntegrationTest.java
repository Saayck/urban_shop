package com.urban_shop.backend.common;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.entity.Brand;
import com.urban_shop.backend.brand.repository.BrandRepository;
import com.urban_shop.backend.brand.service.BrandService;
import com.urban_shop.backend.common.config.CorrelationIdFilter;
import com.urban_shop.backend.coupon.dto.request.CouponCreateRequest;
import com.urban_shop.backend.coupon.entity.DiscountType;
import com.urban_shop.backend.coupon.service.CouponService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class HardeningIntegrationTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void detectsConcurrentEditsInsteadOfSilentlyOverwriting() {
        UUID tenantId = createTenant("hardening-locking").getId();
        BrandResponse created = brandService.create(
            tenantId,
            new BrandCreateRequest("Marca Original", null, null)
        );

        // Dos "sesiones" leen la misma version del registro.
        Brand first = brandRepository.findByTenantIdAndId(tenantId, created.id()).orElseThrow();
        Brand second = brandRepository.findByTenantIdAndId(tenantId, created.id()).orElseThrow();
        assertThat(first.getVersion()).isZero();

        first.setName("Editado por A");
        brandRepository.saveAndFlush(first);

        // La segunda escritura ya no puede pisar a la primera sin enterarse.
        second.setName("Editado por B");
        assertThatThrownBy(() -> brandRepository.saveAndFlush(second))
            .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(brandService.get(tenantId, created.id()).name()).isEqualTo("Editado por A");
    }

    @Test
    void usesABoundedCacheWithExpiry() {
        // ConcurrentMapCacheManager no tenia ni TTL ni tope y crecia sin limite.
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        assertThat(cacheManager.getCacheNames())
            .containsExactlyInAnyOrder("storeProducts", "storeCategories", "storeTemplates");
    }

    @Test
    void assignsACorrelationIdAndCleansTheThreadAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
        // El hilo se reutiliza: el valor no puede quedar colgado para la siguiente peticion.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesTheIncomingCorrelationIdButStripsInjectionAttempts() throws Exception {
        MockHttpServletRequest clean = new MockHttpServletRequest();
        clean.addHeader(CorrelationIdFilter.HEADER, "pedido-12345");
        MockHttpServletResponse cleanResponse = new MockHttpServletResponse();
        correlationIdFilter.doFilter(clean, cleanResponse, mock(FilterChain.class));
        assertThat(cleanResponse.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("pedido-12345");

        // Un salto de linea permitiria falsificar entradas en el log.
        MockHttpServletRequest malicious = new MockHttpServletRequest();
        malicious.addHeader(CorrelationIdFilter.HEADER, "abc\n2026-01-01 ERROR falso");
        MockHttpServletResponse maliciousResponse = new MockHttpServletResponse();
        correlationIdFilter.doFilter(malicious, maliciousResponse, mock(FilterChain.class));
        assertThat(maliciousResponse.getHeader(CorrelationIdFilter.HEADER)).doesNotContain("\n", " ");
    }

    @Test
    void paginatesAndFiltersTheCouponListing() {
        UUID tenantId = createTenant("hardening-coupon-page").getId();
        for (int i = 0; i < 5; i++) {
            couponService.create(tenantId, new CouponCreateRequest(
                "CUPON" + i, null, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), null,
                BigDecimal.ZERO, null, null, null, 1
            ));
        }

        assertThat(couponService.list(tenantId, null, 0, 2).content()).hasSize(2);
        assertThat(couponService.list(tenantId, null, 0, 2).totalElements()).isEqualTo(5);
        assertThat(couponService.list(tenantId, null, 0, 2).totalPages()).isEqualTo(3);
        assertThat(couponService.list(tenantId, true, 0, 20).totalElements()).isEqualTo(5);
        assertThat(couponService.list(tenantId, false, 0, 20).totalElements()).isZero();
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
