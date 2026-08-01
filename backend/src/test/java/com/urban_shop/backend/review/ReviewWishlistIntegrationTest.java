package com.urban_shop.backend.review;

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
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.request.OrderStatusUpdateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.service.OrderService;
import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.PaymentMethod;
import com.urban_shop.backend.payment.service.PaymentService;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.service.ProductService;
import com.urban_shop.backend.review.dto.request.ReviewCreateRequest;
import com.urban_shop.backend.review.dto.request.ReviewModerationRequest;
import com.urban_shop.backend.review.dto.request.ReviewUpdateRequest;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.dto.response.WishlistItemResponse;
import com.urban_shop.backend.review.service.ReviewService;
import com.urban_shop.backend.review.service.WishlistService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;
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
class ReviewWishlistIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private WishlistService wishlistService;

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
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void onlyLetsReviewAfterAnOrderIsDelivered() {
        Fixture fixture = createFixture("review-purchase");

        // Sin compra previa no se puede resenar.
        assertThatThrownBy(() -> reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 5, "Excelente", "Muy buena tela")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("ya recibiste");

        deliverAnOrder(fixture);

        ReviewResponse review = reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 5, "Excelente", "Muy buena tela")
        );
        assertThat(review.rating()).isEqualTo((short) 5);
        assertThat(review.visible()).isTrue();
        // Se publica solo el nombre y la inicial del apellido.
        assertThat(review.customerName()).isEqualTo("Cliente P.");

        // Una sola resena por producto.
        assertThatThrownBy(() -> reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 3, null, null)
        )).isInstanceOf(BusinessException.class).hasMessageContaining("Ya has resenado");
    }

    @Test
    void publishesEditsAndSummarizesReviews() {
        Fixture fixture = createFixture("review-summary");
        deliverAnOrder(fixture);

        ReviewResponse review = reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 4, "Buena", "Cumple")
        );

        assertThat(reviewService.listPublic(fixture.slug(), "polo-oversize", 0, 20).totalElements())
            .isEqualTo(1);
        assertThat(reviewService.summaryPublic(fixture.slug(), "polo-oversize").averageRating())
            .isEqualByComparingTo("4.00");

        reviewService.update(
            fixture.tenantId(),
            fixture.customerId(),
            review.id(),
            new ReviewUpdateRequest((short) 2, "Regular", "Se destineo")
        );
        assertThat(reviewService.summaryPublic(fixture.slug(), "polo-oversize").averageRating())
            .isEqualByComparingTo("2.00");

        assertThat(reviewService.listMine(fixture.tenantId(), fixture.customerId(), 0, 20).totalElements())
            .isEqualTo(1);
    }

    @Test
    void hidesModeratedReviewsFromTheStorefront() {
        Fixture fixture = createFixture("review-moderation");
        deliverAnOrder(fixture);
        ReviewResponse review = reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 1, "Insultos", "Contenido abusivo")
        );

        reviewService.moderate(
            fixture.tenantId(),
            review.id(),
            new ReviewModerationRequest(false, "Lenguaje ofensivo")
        );

        // Deja de verse en la tienda pero sigue existiendo para el admin.
        assertThat(reviewService.listPublic(fixture.slug(), "polo-oversize", 0, 20).totalElements()).isZero();
        assertThat(reviewService.summaryPublic(fixture.slug(), "polo-oversize").totalReviews()).isZero();
        assertThat(reviewService.listAdmin(fixture.tenantId(), 0, 20).totalElements()).isEqualTo(1);
        assertThat(reviewService.listAdmin(fixture.tenantId(), 0, 20).content())
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.visible()).isFalse();
                assertThat(entry.moderationNote()).isEqualTo("Lenguaje ofensivo");
            });
    }

    @Test
    void doesNotLetACustomerEditSomeoneElsesReview() {
        Fixture fixture = createFixture("review-foreign");
        deliverAnOrder(fixture);
        ReviewResponse review = reviewService.create(
            fixture.tenantId(),
            fixture.customerId(),
            new ReviewCreateRequest(fixture.productId(), (short) 5, "Buena", null)
        );
        LoginResponse intruder = registerCustomer(fixture.slug(), "intruso@" + fixture.slug() + ".pe", "987654322");

        assertThatThrownBy(() -> reviewService.update(
            fixture.tenantId(),
            intruder.user().id(),
            review.id(),
            new ReviewUpdateRequest((short) 1, null, null)
        )).isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> reviewService.delete(
            fixture.tenantId(),
            intruder.user().id(),
            review.id()
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void managesTheWishlistIdempotently() {
        Fixture fixture = createFixture("wishlist-basic");

        WishlistItemResponse added = wishlistService.add(
            fixture.tenantId(),
            fixture.customerId(),
            fixture.productId()
        );
        assertThat(added.productName()).isEqualTo("Polo Oversize");
        assertThat(added.available()).isTrue();
        assertThat(added.price()).isEqualByComparingTo("79.90");

        // Anadir de nuevo no duplica.
        wishlistService.add(fixture.tenantId(), fixture.customerId(), fixture.productId());
        List<WishlistItemResponse> items = wishlistService.list(fixture.tenantId(), fixture.customerId());
        assertThat(items).hasSize(1);

        wishlistService.remove(fixture.tenantId(), fixture.customerId(), fixture.productId());
        assertThat(wishlistService.list(fixture.tenantId(), fixture.customerId())).isEmpty();
        // Quitar dos veces es seguro.
        wishlistService.remove(fixture.tenantId(), fixture.customerId(), fixture.productId());
    }

    @Test
    void marksUnavailableProductsInTheWishlist() {
        Fixture fixture = createFixture("wishlist-unavailable");
        wishlistService.add(fixture.tenantId(), fixture.customerId(), fixture.productId());

        productService.updateStatus(fixture.tenantId(), fixture.productId(), ProductStatus.INACTIVE);

        assertThat(wishlistService.list(fixture.tenantId(), fixture.customerId()))
            .singleElement()
            .satisfies(item -> assertThat(item.available()).isFalse());
    }

    @Test
    void rejectsWishlistProductsFromAnotherTenant() {
        Fixture fixture = createFixture("wishlist-tenant-a");
        Tenant other = createTenant("wishlist-tenant-b");

        assertThatThrownBy(() -> wishlistService.add(
            other.getId(),
            fixture.customerId(),
            fixture.productId()
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    /** Lleva un pedido hasta DELIVERED, que es el requisito para poder resenar. */
    private void deliverAnOrder(Fixture fixture) {
        cartService.addItem(
            fixture.tenantId(),
            fixture.customerId(),
            new CartItemCreateRequest(fixture.variantId(), 1)
        );
        OrderDetailResponse order = orderService.create(
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
        PaymentResponse payment = paymentService.create(
            fixture.tenantId(),
            fixture.customerId(),
            order.id(),
            new PaymentCreateRequest(PaymentMethod.YAPE, "OP-" + fixture.slug(), null)
        );
        UUID reviewerId = internalUserId(fixture.tenantId(), fixture.slug());
        paymentService.confirm(fixture.tenantId(), reviewerId, payment.id());

        orderService.updateStatus(fixture.tenantId(), reviewerId, order.id(),
            new OrderStatusUpdateRequest(OrderStatus.PREPARING, null));
        orderService.updateStatus(fixture.tenantId(), reviewerId, order.id(),
            new OrderStatusUpdateRequest(OrderStatus.ON_THE_WAY, null));
        orderService.updateStatus(fixture.tenantId(), reviewerId, order.id(),
            new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null));
    }

    /** El revisor de pagos y el que cambia estados deben existir en users. */
    private UUID internalUserId(UUID tenantId, String slug) {
        Role role = roleRepository.findByName("TENANT_ADMIN").orElseGet(() -> {
            Role created = new Role();
            created.setName("TENANT_ADMIN");
            return roleRepository.save(created);
        });
        User user = new User();
        user.setTenantId(tenantId);
        user.setFullName("Admin " + slug);
        user.setEmail("admin@" + slug + ".pe");
        user.setPasswordHash("x");
        user.setActive(true);
        user.getRoles().add(role);
        return userRepository.save(user).getId();
    }

    private Fixture createFixture(String slug) {
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
            new ProductVariantCreateRequest("SKU-" + slug, "M", "Negro", "#000000", 10, null, true)
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        return new Fixture(
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
            slug, "Cliente", "Prueba", email, phone, "Password123!", null, null
        ));
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
        String slug,
        UUID tenantId,
        UUID customerId,
        UUID addressId,
        UUID productId,
        UUID variantId
    ) {
    }
}
