package com.urban_shop.backend.order;

import com.urban_shop.backend.audit.repository.AuditLogRepository;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.order.dto.request.OrderCancelRequest;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.service.OrderService;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.service.ProductService;
import com.urban_shop.backend.report.dto.SalesReportResponse;
import com.urban_shop.backend.report.dto.TopProductResponse;
import com.urban_shop.backend.report.service.ReportService;
import com.urban_shop.backend.shipping.dto.request.ShippingZoneCreateRequest;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CustomerOrderFlowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ShippingZoneService shippingZoneService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void chargesTheShippingCostOfTheZoneCoveringTheAddress() {
        Fixture fixture = createFixture("flow-shipping", 5);
        shippingZoneService.create(fixture.tenantId(), new ShippingZoneCreateRequest(
            "Lima Miraflores",
            "Lima",
            "Lima",
            "Miraflores",
            new BigDecimal("12.50"),
            "1 dia habil"
        ));
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 2));

        OrderDetailResponse order = orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            deliveryRequest(fixture.addressId())
        );

        assertThat(order.subtotal()).isEqualByComparingTo("159.80");
        assertThat(order.shippingCost()).isEqualByComparingTo("12.50");
        assertThat(order.total()).isEqualByComparingTo("172.30");
    }

    @Test
    void rejectsDeliveryToAnAddressOutsideTheConfiguredZones() {
        Fixture fixture = createFixture("flow-uncovered", 5);
        shippingZoneService.create(fixture.tenantId(), new ShippingZoneCreateRequest(
            "Solo Cusco",
            "Cusco",
            null,
            null,
            new BigDecimal("30.00"),
            "3 dias"
        ));
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 1));

        assertThatThrownBy(() -> orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            deliveryRequest(fixture.addressId())
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("no realiza envios");
    }

    @Test
    void letsTheCustomerCancelRestoringStockAndLeavingAnAuditTrail() {
        Fixture fixture = createFixture("flow-cancel", 5);
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 2));
        OrderDetailResponse order = orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            deliveryRequest(fixture.addressId())
        );
        assertThat(productService.getAdmin(fixture.tenantId(), fixture.productId()).totalStock()).isEqualTo(3);

        OrderDetailResponse cancelled = orderService.cancelByCustomer(
            fixture.tenantId(),
            fixture.customerId(),
            order.id(),
            new OrderCancelRequest("Me equivoque de talla")
        );

        assertThat(cancelled.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.paymentStatus()).isEqualTo(OrderPaymentStatus.CANCELLED);
        assertThat(productService.getAdmin(fixture.tenantId(), fixture.productId()).totalStock()).isEqualTo(5);
        assertThat(auditLogRepository.findAll())
            .anyMatch(entry -> "ORDER_CANCELLED_BY_CUSTOMER".equals(entry.getAction())
                && order.id().equals(entry.getEntityId()));

        // Idempotente: repetir la cancelacion no vuelve a reponer stock.
        orderService.cancelByCustomer(fixture.tenantId(), fixture.customerId(), order.id(), null);
        assertThat(productService.getAdmin(fixture.tenantId(), fixture.productId()).totalStock()).isEqualTo(5);
    }

    @Test
    void doesNotLetACustomerCancelSomeoneElsesOrder() {
        Fixture fixture = createFixture("flow-foreign", 5);
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 1));
        OrderDetailResponse order = orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            deliveryRequest(fixture.addressId())
        );
        LoginResponse intruder = registerCustomer("flow-foreign", "intruso@flow-foreign.pe", "987654322");

        assertThatThrownBy(() -> orderService.cancelByCustomer(
            fixture.tenantId(),
            intruder.user().id(),
            order.id(),
            null
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearsTheCartWithoutTouchingTheOrders() {
        Fixture fixture = createFixture("flow-clear-cart", 5);
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 3));
        assertThat(cartService.get(fixture.tenantId(), fixture.customerId()).items()).hasSize(1);

        assertThat(cartService.clear(fixture.tenantId(), fixture.customerId()).items()).isEmpty();
        // Vaciar dos veces es seguro.
        assertThat(cartService.clear(fixture.tenantId(), fixture.customerId()).items()).isEmpty();
    }

    @Test
    void aggregatesSalesAndTopProductsInTheReport() {
        Fixture fixture = createFixture("flow-report", 10);
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 2));
        orderService.create(fixture.tenantId(), fixture.customerId(), deliveryRequest(fixture.addressId()));
        cartService.addItem(fixture.tenantId(), fixture.customerId(), new CartItemCreateRequest(fixture.variantId(), 1));
        OrderDetailResponse second = orderService.create(
            fixture.tenantId(),
            fixture.customerId(),
            deliveryRequest(fixture.addressId())
        );
        orderService.cancelByCustomer(fixture.tenantId(), fixture.customerId(), second.id(), null);

        LocalDate today = LocalDate.now();
        SalesReportResponse report = reportService.getSalesReport(fixture.tenantId(), today.minusDays(1), today);

        assertThat(report.totalOrders()).isEqualTo(2);
        assertThat(report.cancelledOrders()).isEqualTo(1);
        assertThat(report.pendingOrders()).isEqualTo(1);
        assertThat(report.totalRevenue()).isEqualByComparingTo("159.80");
        assertThat(report.averageTicket()).isEqualByComparingTo("159.80");

        List<TopProductResponse> top = reportService.getTopProducts(fixture.tenantId(), today.minusDays(1), today, 5);
        assertThat(top).singleElement().satisfies(entry -> {
            assertThat(entry.productName()).isEqualTo("Polo Oversize");
            // Solo cuenta el pedido no cancelado.
            assertThat(entry.unitsSold()).isEqualTo(2);
        });

        assertThat(reportService.generateSalesReportCsv(fixture.tenantId(), today.minusDays(1), today))
            .startsWith("Order ID,Order Number,Order Status")
            .contains("CANCELLED");
    }

    @Test
    void rejectsAnInvertedDateRange() {
        UUID tenantId = createTenant("flow-report-range").getId();
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> reportService.getSalesReport(tenantId, today, today.minusDays(5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("posterior");
    }

    private Fixture createFixture(String slug, int stock) {
        Tenant tenant = createTenant(slug);
        LoginResponse customer = registerCustomer(slug, slug + "@urban.pe", "987654321");
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
                null,
                category.id(),
                "Polo Oversize",
                "polo-oversize",
                null,
                "Algodon",
                "OVERSIZE",
                new BigDecimal("89.90"),
                new BigDecimal("79.90"),
                false,
                true
            )
        );
        ProductVariantResponse variant = productService.addVariant(
            tenant.getId(),
            product.id(),
            new ProductVariantCreateRequest("SKU-" + slug, "M", "Negro", "#000000", stock, null, true)
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        return new Fixture(
            tenant.getId(),
            customer.user().id(),
            address.id(),
            product.id(),
            variant.id()
        );
    }

    private LoginResponse registerCustomer(String slug, String email, String phone) {
        return authService.registerCustomer(new RegisterCustomerRequest(
            slug,
            "Cliente",
            "Prueba",
            email,
            phone,
            "Password123!",
            null,
            null
        ));
    }

    private OrderCreateRequest deliveryRequest(UUID addressId) {
        return deliveryRequest(addressId, null);
    }

    private OrderCreateRequest deliveryRequest(UUID addressId, String couponCode) {
        return new OrderCreateRequest(
            DeliveryType.DELIVERY,
            addressId,
            InvoiceType.BOLETA,
            "DNI",
            "12345678",
            couponCode
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
        UUID addressId,
        UUID productId,
        UUID variantId
    ) {
    }
}
