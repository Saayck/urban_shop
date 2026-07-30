package com.urban_shop.backend.shipping;

import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.dto.response.ShippingQuoteResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ShippingQuoteIntegrationTest {

    @Autowired
    private ShippingZoneService shippingZoneService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void picksTheMostSpecificZoneThatCoversTheAddress() {
        UUID tenantId = createTenant("quote-specificity").getId();
        shippingZoneService.create(tenantId, zone("Nacional", null, null, null, "25.00"));
        shippingZoneService.create(tenantId, zone("Lima", "Lima", null, null, "15.00"));
        shippingZoneService.create(tenantId, zone("Lima Centro", "Lima", "Lima", "Miraflores", "8.00"));

        ShippingQuoteResponse miraflores = shippingZoneService.quote(tenantId, "Lima", "Lima", "Miraflores");
        assertThat(miraflores.covered()).isTrue();
        assertThat(miraflores.zoneName()).isEqualTo("Lima Centro");
        assertThat(miraflores.price()).isEqualByComparingTo("8.00");

        ShippingQuoteResponse otroDistrito = shippingZoneService.quote(tenantId, "Lima", "Lima", "Comas");
        assertThat(otroDistrito.zoneName()).isEqualTo("Lima");
        assertThat(otroDistrito.price()).isEqualByComparingTo("15.00");

        ShippingQuoteResponse provincia = shippingZoneService.quote(tenantId, "Cusco", "Cusco", "Wanchaq");
        assertThat(provincia.zoneName()).isEqualTo("Nacional");
        assertThat(provincia.price()).isEqualByComparingTo("25.00");
    }

    @Test
    void reportsNotCoveredWhenNoZoneMatches() {
        UUID tenantId = createTenant("quote-uncovered").getId();
        shippingZoneService.create(tenantId, zone("Solo Lima", "Lima", null, null, "12.00"));

        ShippingQuoteResponse quote = shippingZoneService.quote(tenantId, "Arequipa", "Arequipa", "Cayma");

        assertThat(quote.covered()).isFalse();
        assertThat(quote.price()).isEqualByComparingTo("0");
        assertThat(quote.zoneId()).isNull();
    }

    @Test
    void reportsNotCoveredWhenTheStoreHasNoZones() {
        UUID tenantId = createTenant("quote-empty").getId();

        assertThat(shippingZoneService.quote(tenantId, "Lima", "Lima", "Miraflores").covered()).isFalse();
    }

    @Test
    void quotesByPublicSlug() {
        String slug = "quote-public";
        UUID tenantId = createTenant(slug).getId();
        shippingZoneService.create(tenantId, zone("Lima", "Lima", null, null, "10.00"));

        ShippingQuoteResponse quote = shippingZoneService.quotePublic(slug, "Lima", "Lima", "Surco");

        assertThat(quote.covered()).isTrue();
        assertThat(quote.price()).isEqualByComparingTo("10.00");
    }

    private ShippingZoneCreateRequest zone(
        String name,
        String department,
        String province,
        String district,
        String price
    ) {
        return new ShippingZoneCreateRequest(
            name,
            department,
            province,
            district,
            new BigDecimal(price),
            "1-2 dias habiles"
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
