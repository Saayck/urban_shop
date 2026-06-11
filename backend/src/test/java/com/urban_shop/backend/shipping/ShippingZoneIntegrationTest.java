package com.urban_shop.backend.shipping;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneUpdateRequest;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ShippingZoneIntegrationTest {

    @Autowired
    private ShippingZoneService shippingZoneService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void createListAndUpdateZone() {
        Tenant tenant = createTenant("shipping-test");

        ShippingZoneResponse zone = shippingZoneService.create(tenant.getId(),
            new ShippingZoneCreateRequest(
                "Lima Metropolitana",
                "Lima",
                "Lima",
                null,
                new BigDecimal("8.00"),
                "1-2 dias habiles"
            ));

        assertThat(zone.name()).isEqualTo("Lima Metropolitana");
        assertThat(zone.price()).isEqualByComparingTo("8.00");
        assertThat(zone.active()).isTrue();

        List<ShippingZoneResponse> admin = shippingZoneService.listAdmin(tenant.getId());
        assertThat(admin).hasSize(1);

        List<ShippingZoneResponse> publicList = shippingZoneService.listPublic("shipping-test");
        assertThat(publicList).hasSize(1);

        ShippingZoneResponse updated = shippingZoneService.update(tenant.getId(), zone.id(),
            new ShippingZoneUpdateRequest(
                "Lima Metropolitana",
                "Lima",
                "Lima",
                null,
                new BigDecimal("10.00"),
                "1 dia habil",
                false
            ));
        assertThat(updated.price()).isEqualByComparingTo("10.00");
        assertThat(updated.active()).isFalse();

        List<ShippingZoneResponse> activeOnly = shippingZoneService.listPublic("shipping-test");
        assertThat(activeOnly).isEmpty();
    }

    @Test
    void rejectsDuplicateNamePerTenant() {
        Tenant tenant = createTenant("shipping-dup");
        shippingZoneService.create(tenant.getId(),
            new ShippingZoneCreateRequest("Provincias", null, null, null, BigDecimal.TEN, null));

        assertThatThrownBy(() -> shippingZoneService.create(tenant.getId(),
            new ShippingZoneCreateRequest("PROVINCIAS", null, null, null, BigDecimal.TEN, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe");
    }

    @Test
    void deleteRemovesZone() {
        Tenant tenant = createTenant("shipping-del");
        ShippingZoneResponse zone = shippingZoneService.create(tenant.getId(),
            new ShippingZoneCreateRequest("Callao", "Lima", "Callao", null, new BigDecimal("5.00"), null));

        shippingZoneService.delete(tenant.getId(), zone.id());

        assertThatThrownBy(() -> shippingZoneService.get(tenant.getId(), zone.id()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(shippingZoneService.listAdmin(tenant.getId())).isEmpty();
    }

    @Test
    void tenantIsolation() {
        Tenant tenantA = createTenant("shipping-iso-a");
        Tenant tenantB = createTenant("shipping-iso-b");

        ShippingZoneResponse zoneA = shippingZoneService.create(tenantA.getId(),
            new ShippingZoneCreateRequest("Zona A", null, null, null, BigDecimal.ONE, null));

        assertThatThrownBy(() -> shippingZoneService.get(tenantB.getId(), zoneA.id()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(shippingZoneService.listAdmin(tenantB.getId())).isEmpty();
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
