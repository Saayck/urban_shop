package com.urban_shop.backend.payment;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.service.OrderService;
import com.urban_shop.backend.payment.dto.request.PaymentWebhookRequest;
import com.urban_shop.backend.payment.entity.PaymentMethod;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import com.urban_shop.backend.payment.repository.PaymentRepository;
import com.urban_shop.backend.payment.service.PaymentService;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentWebhookIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

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
    private TenantRepository tenantRepository;

    @Test
    void marksTheOrderAsPaidAndIsIdempotentOnResend() {
        Fixture fixture = createOrder("webhook-paid");
        PaymentWebhookRequest event = new PaymentWebhookRequest(
            "mercadopago",
            "MP-TX-0001",
            fixture.orderId(),
            "COMPLETED",
            PaymentMethod.BANK_TRANSFER,
            null
        );

        paymentService.applyWebhookPayment(event);

        OrderDetailResponse paid = orderService.getAdmin(fixture.tenantId(), fixture.orderId());
        assertThat(paid.paymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
        assertThat(paid.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.tracking()).extracting(history -> history.status())
            .containsExactly(OrderStatus.CREATED, OrderStatus.PAID);
        assertThat(paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(fixture.orderId()))
            .singleElement()
            .satisfies(payment -> {
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
                assertThat(payment.getProvider()).isEqualTo("MERCADOPAGO");
                assertThat(payment.getExternalPaymentId()).isEqualTo("MP-TX-0001");
            });

        // Reenvio del mismo evento: no debe duplicar el pago ni el historial.
        paymentService.applyWebhookPayment(event);

        assertThat(paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(fixture.orderId())).hasSize(1);
        assertThat(orderService.getAdmin(fixture.tenantId(), fixture.orderId()).tracking()).hasSize(2);
    }

    @Test
    void ignoresEventsWithANonApprovedStatus() {
        Fixture fixture = createOrder("webhook-pending");

        paymentService.applyWebhookPayment(new PaymentWebhookRequest(
            "mercadopago",
            "MP-TX-0002",
            fixture.orderId(),
            "IN_PROCESS",
            null,
            null
        ));

        OrderDetailResponse order = orderService.getAdmin(fixture.tenantId(), fixture.orderId());
        assertThat(order.paymentStatus()).isEqualTo(OrderPaymentStatus.PENDING);
        assertThat(paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(fixture.orderId())).isEmpty();
    }

    private Fixture createOrder(String slug) {
        Tenant tenant = createTenant(slug);
        LoginResponse customer = authService.registerCustomer(new RegisterCustomerRequest(
            slug,
            "Cliente",
            "Prueba",
            slug + "@urban.pe",
            "987654321",
            "Password123!",
            null,
            null
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
            new ProductVariantCreateRequest("SKU-" + slug, "M", "Negro", "#000000", 5, null, true)
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        cartService.addItem(tenant.getId(), customer.user().id(), new CartItemCreateRequest(variant.id(), 1));

        OrderDetailResponse order = orderService.create(
            tenant.getId(),
            customer.user().id(),
            new OrderCreateRequest(DeliveryType.DELIVERY, address.id(), InvoiceType.BOLETA, "DNI", "12345678", null)
        );
        return new Fixture(tenant.getId(), customer.user().id(), order.id());
    }

    private Tenant createTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug);
        tenant.setStatus("ACTIVE");
        tenant.setPlanName("BASIC");
        return tenantRepository.save(tenant);
    }

    private record Fixture(UUID tenantId, UUID customerId, UUID orderId) {
    }
}
