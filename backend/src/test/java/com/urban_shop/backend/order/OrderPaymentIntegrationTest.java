package com.urban_shop.backend.order;

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
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import com.urban_shop.backend.inventory.service.InventoryService;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.request.OrderStatusUpdateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.service.OrderService;
import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.PaymentMethod;
import com.urban_shop.backend.payment.entity.PaymentStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class OrderPaymentIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

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
    private InventoryService inventoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void createsOrderSnapshotClosesCartAndDiscountsStock() {
        TestData data = createData("order-create", 5, 2);

        OrderDetailResponse order = orderService.create(
            data.tenantId(),
            data.customerId(),
            deliveryRequest(data.addressId())
        );

        assertThat(order.orderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.paymentStatus()).isEqualTo(OrderPaymentStatus.PENDING);
        assertThat(order.subtotal()).isEqualByComparingTo("159.80");
        assertThat(order.total()).isEqualByComparingTo("159.80");
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Polo Oversize");
            assertThat(item.size()).isEqualTo("M");
            assertThat(item.color()).isEqualTo("Negro");
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("79.90");
        });
        assertThat(order.tracking()).extracting(history -> history.status())
            .containsExactly(OrderStatus.CREATED);
        assertThat(productService.getAdmin(data.tenantId(), data.productId()).totalStock())
            .isEqualTo(3);
        assertThat(cartService.get(data.tenantId(), data.customerId()).items()).isEmpty();
        assertThat(orderService.listCustomer(data.tenantId(), data.customerId(), 0, 20).totalElements())
            .isEqualTo(1);

        LoginResponse otherCustomer = registerCustomer(data.slug(), "other-" + data.slug() + "@urban.pe", "987654329");
        assertThatThrownBy(() -> orderService.getCustomer(
            data.tenantId(),
            otherCustomer.user().id(),
            order.id()
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancellationRestoresStockAndPreventsPayment() {
        TestData data = createData("order-cancel", 4, 3);
        OrderDetailResponse order = orderService.create(
            data.tenantId(),
            data.customerId(),
            deliveryRequest(data.addressId())
        );

        OrderDetailResponse cancelled = orderService.updateStatus(
            data.tenantId(),
            UUID.randomUUID(),
            order.id(),
            new OrderStatusUpdateRequest(OrderStatus.CANCELLED, "Cliente desistio")
        );

        assertThat(cancelled.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.paymentStatus()).isEqualTo(OrderPaymentStatus.CANCELLED);
        assertThat(productService.getAdmin(data.tenantId(), data.productId()).totalStock())
            .isEqualTo(4);
        assertThat(cancelled.tracking()).extracting(history -> history.status())
            .containsExactly(OrderStatus.CREATED, OrderStatus.CANCELLED);
        assertThatThrownBy(() -> paymentService.create(
            data.tenantId(),
            data.customerId(),
            order.id(),
            new PaymentCreateRequest(PaymentMethod.YAPE, "OP-CANCEL", null)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("No se puede pagar");
    }

    @Test
    void rejectsRetriesConfirmsAndCompletesDeliveryWorkflow() {
        TestData data = createData("order-payment", 5, 1);
        OrderDetailResponse order = orderService.create(
            data.tenantId(),
            data.customerId(),
            deliveryRequest(data.addressId())
        );

        assertThatThrownBy(() -> paymentService.create(
            data.tenantId(),
            data.customerId(),
            order.id(),
            new PaymentCreateRequest(PaymentMethod.YAPE, null, null)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("requiere codigo");

        PaymentResponse first = paymentService.create(
            data.tenantId(),
            data.customerId(),
            order.id(),
            new PaymentCreateRequest(PaymentMethod.YAPE, "YAPE-001", null)
        );
        assertThat(first.status()).isEqualTo(PaymentStatus.MANUAL_REVIEW);

        PaymentResponse rejected = paymentService.reject(
            data.tenantId(),
            UUID.randomUUID(),
            first.id(),
            new PaymentRejectRequest("Monto no visible")
        );
        assertThat(rejected.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderService.getAdmin(data.tenantId(), order.id()).paymentStatus())
            .isEqualTo(OrderPaymentStatus.FAILED);

        PaymentResponse retry = paymentService.create(
            data.tenantId(),
            data.customerId(),
            order.id(),
            new PaymentCreateRequest(
                PaymentMethod.BANK_TRANSFER,
                "BANK-002",
                "https://cdn.example.com/proof.png"
            )
        );
        PaymentResponse confirmed = paymentService.confirm(
            data.tenantId(),
            UUID.randomUUID(),
            retry.id()
        );
        assertThat(confirmed.status()).isEqualTo(PaymentStatus.PAID);

        OrderDetailResponse paid = orderService.getAdmin(data.tenantId(), order.id());
        assertThat(paid.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.paymentStatus()).isEqualTo(OrderPaymentStatus.PAID);

        assertThatThrownBy(() -> orderService.updateStatus(
            data.tenantId(),
            UUID.randomUUID(),
            order.id(),
            new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Transicion");

        orderService.updateStatus(
            data.tenantId(),
            UUID.randomUUID(),
            order.id(),
            new OrderStatusUpdateRequest(OrderStatus.PREPARING, null)
        );
        orderService.updateStatus(
            data.tenantId(),
            UUID.randomUUID(),
            order.id(),
            new OrderStatusUpdateRequest(OrderStatus.ON_THE_WAY, null)
        );
        OrderDetailResponse delivered = orderService.updateStatus(
            data.tenantId(),
            UUID.randomUUID(),
            order.id(),
            new OrderStatusUpdateRequest(OrderStatus.DELIVERED, "Entregado al cliente")
        );

        assertThat(delivered.orderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.tracking()).extracting(history -> history.status())
            .containsExactly(
                OrderStatus.CREATED,
                OrderStatus.PAID,
                OrderStatus.PREPARING,
                OrderStatus.ON_THE_WAY,
                OrderStatus.DELIVERED
            );
    }

    @Test
    void rollsBackCheckoutWhenStockChangedAfterCart() {
        TestData data = createData("order-stock", 3, 3);
        inventoryService.create(
            data.tenantId(),
            new InventoryMovementRequest(
                data.variantId(),
                InventoryMovementType.EXIT,
                2,
                "Venta externa"
            )
        );

        assertThatThrownBy(() -> orderService.create(
            data.tenantId(),
            data.customerId(),
            deliveryRequest(data.addressId())
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Stock insuficiente");

        assertThat(orderService.listCustomer(data.tenantId(), data.customerId(), 0, 20).content())
            .isEmpty();
        assertThat(cartService.get(data.tenantId(), data.customerId()).items()).hasSize(1);
        assertThat(productService.getAdmin(data.tenantId(), data.productId()).totalStock())
            .isEqualTo(1);
    }

    private TestData createData(String slug, int stock, int cartQuantity) {
        Tenant tenant = createTenant(slug);
        LoginResponse customer = registerCustomer(slug, slug + "@urban.pe", "987654328");
        CustomerAddressResponse address = customerService.createAddress(
            tenant.getId(),
            customer.user().id(),
            new CustomerAddressRequest(
                "Lima",
                "Lima",
                "Miraflores",
                "Av. Principal 123",
                null,
                null,
                null,
                true
            )
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
            new ProductVariantCreateRequest(
                "SKU-" + slug,
                "M",
                "Negro",
                "#000000",
                stock,
                null,
                true
            )
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        cartService.addItem(
            tenant.getId(),
            customer.user().id(),
            new CartItemCreateRequest(variant.id(), cartQuantity)
        );
        return new TestData(
            slug,
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
        return new OrderCreateRequest(
            DeliveryType.DELIVERY,
            addressId,
            InvoiceType.BOLETA,
            "DNI",
            "12345678",
            null
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

    private record TestData(
        String slug,
        UUID tenantId,
        UUID customerId,
        UUID addressId,
        UUID productId,
        UUID variantId
    ) {
    }
}
