package com.urban_shop.backend.customer;

import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RefreshTokenRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.request.CustomerStatusRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.dto.response.CustomerSummaryResponse;
import com.urban_shop.backend.customer.entity.CustomerStatus;
import com.urban_shop.backend.customer.service.AdminCustomerService;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.service.OrderService;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.service.ProductService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdminCustomerIntegrationTest {

    @Autowired
    private AdminCustomerService adminCustomerService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void listsCustomersWithTheirPurchaseMetrics() {
        String slug = "admin-customer-metrics";
        Fixture fixture = createFixture(slug);

        // Cliente sin compras: metricas en cero, no nulas.
        CustomerSummaryResponse before = adminCustomerService.get(fixture.tenantId(), fixture.customerId());
        assertThat(before.totalOrders()).isZero();
        assertThat(before.totalSpent()).isEqualByComparingTo("0");
        assertThat(before.lastOrderAt()).isNull();

        placeOrder(fixture, 2);
        placeOrder(fixture, 1);

        CustomerSummaryResponse after = adminCustomerService.get(fixture.tenantId(), fixture.customerId());
        assertThat(after.totalOrders()).isEqualTo(2);
        assertThat(after.totalSpent()).isEqualByComparingTo("239.70");
        assertThat(after.lastOrderAt()).isNotNull();

        assertThat(adminCustomerService.listOrders(fixture.tenantId(), fixture.customerId(), 0, 20)
            .totalElements()).isEqualTo(2);
    }

    @Test
    void searchesByNameEmailPhoneAndDocument() {
        String slug = "admin-customer-search";
        Fixture fixture = createFixture(slug);

        assertThat(adminCustomerService.list(fixture.tenantId(), "Cliente", null, 0, 20).totalElements())
            .isEqualTo(1);
        assertThat(adminCustomerService.list(fixture.tenantId(), slug + "@urban.pe", null, 0, 20).totalElements())
            .isEqualTo(1);
        assertThat(adminCustomerService.list(fixture.tenantId(), "987654321", null, 0, 20).totalElements())
            .isEqualTo(1);
        assertThat(adminCustomerService.list(fixture.tenantId(), "noexiste", null, 0, 20).totalElements())
            .isZero();
        assertThat(adminCustomerService.list(fixture.tenantId(), null, CustomerStatus.BLOCKED, 0, 20)
            .totalElements()).isZero();
    }

    @Test
    void blockingACustomerRevokesTheirSessions() {
        String slug = "admin-customer-block";
        Fixture fixture = createFixture(slug);

        CustomerSummaryResponse blocked = adminCustomerService.updateStatus(
            fixture.tenantId(),
            fixture.customerId(),
            new CustomerStatusRequest(CustomerStatus.BLOCKED, "Fraude reiterado")
        );
        assertThat(blocked.status()).isEqualTo(CustomerStatus.BLOCKED);

        // El refresh token emitido al registrarse deja de servir.
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(fixture.refreshToken())))
            .isInstanceOf(BadCredentialsException.class);
        // Y tampoco puede volver a entrar con sus credenciales.
        assertThatThrownBy(() -> authService.login(
            new LoginRequest(slug + "@urban.pe", "Password123!", slug)
        )).isInstanceOf(BadCredentialsException.class);

        adminCustomerService.updateStatus(
            fixture.tenantId(),
            fixture.customerId(),
            new CustomerStatusRequest(CustomerStatus.ACTIVE, null)
        );
        assertThat(authService.login(new LoginRequest(slug + "@urban.pe", "Password123!", slug)).token())
            .isNotBlank();
    }

    @Test
    void doesNotExposeCustomersFromAnotherTenant() {
        Fixture fixture = createFixture("admin-customer-tenant-a");
        Tenant other = createTenant("admin-customer-tenant-b");

        assertThatThrownBy(() -> adminCustomerService.get(other.getId(), fixture.customerId()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(adminCustomerService.list(other.getId(), null, null, 0, 20).totalElements()).isZero();
    }

    private void placeOrder(Fixture fixture, int quantity) {
        cartService.addItem(
            fixture.tenantId(),
            fixture.customerId(),
            new CartItemCreateRequest(fixture.variantId(), quantity)
        );
        orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new OrderCreateRequest(
                DeliveryType.DELIVERY,
                fixture.addressId(),
                InvoiceType.BOLETA,
                "DNI",
                "12345678",
                null
            )
        );
    }

    private Fixture createFixture(String slug) {
        Tenant tenant = createTenant(slug);
        LoginResponse customer = authService.registerCustomer(new RegisterCustomerRequest(
            slug, "Cliente", "Prueba", slug + "@urban.pe", "987654321", "Password123!", null, null
        ));
        CustomerAddressResponse address = customerService.createAddress(
            tenant.getId(),
            customer.user().id(),
            new CustomerAddressRequest("Lima", "Lima", "Miraflores", "Av. Principal 123", null, null, null, true)
        );
        CategoryResponse category = categoryService.create(
            tenant.getId(),
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse product = productService.create(
            tenant.getId(),
            new ProductCreateRequest(
                null, category.id(), "Polo Oversize", "polo-oversize", null, "Algodon", "OVERSIZE",
                new BigDecimal("89.90"), new BigDecimal("79.90"), false, true
            )
        );
        ProductVariantResponse variant = productService.addVariant(
            tenant.getId(),
            product.id(),
            new ProductVariantCreateRequest("SKU-" + slug, "M", "Negro", "#000000", 20, null, true)
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        return new Fixture(
            tenant.getId(),
            customer.user().id(),
            customer.refreshToken(),
            address.id(),
            variant.id()
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

    private record Fixture(
        UUID tenantId,
        UUID customerId,
        String refreshToken,
        UUID addressId,
        UUID variantId
    ) {
    }
}
