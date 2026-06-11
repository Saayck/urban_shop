package com.urban_shop.backend.cart;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.dto.request.CartItemUpdateRequest;
import com.urban_shop.backend.cart.dto.response.CartResponse;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import com.urban_shop.backend.inventory.service.InventoryService;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void addsUpdatesAndRemovesCartItemsWithCurrentPrice() {
        TestData data = createData("cart-flow", 5);

        CartResponse added = cartService.addItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            new CartItemCreateRequest(data.variant().id(), 2)
        );
        assertThat(added.totalItems()).isEqualTo(2);
        assertThat(added.subtotal()).isEqualByComparingTo("159.80");
        assertThat(added.readyForCheckout()).isTrue();

        CartResponse accumulated = cartService.addItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            new CartItemCreateRequest(data.variant().id(), 1)
        );
        assertThat(accumulated.items()).hasSize(1);
        assertThat(accumulated.totalItems()).isEqualTo(3);

        CartResponse updated = cartService.updateItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            accumulated.items().getFirst().id(),
            new CartItemUpdateRequest(1)
        );
        assertThat(updated.totalItems()).isEqualTo(1);

        CartResponse empty = cartService.deleteItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            updated.items().getFirst().id()
        );
        assertThat(empty.items()).isEmpty();
        assertThat(empty.readyForCheckout()).isFalse();
    }

    @Test
    void rejectsExcessStockAndReportsCartUnavailableAfterInventoryChange() {
        TestData data = createData("cart-stock", 3);

        assertThatThrownBy(() -> cartService.addItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            new CartItemCreateRequest(data.variant().id(), 4)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Stock insuficiente");

        CartResponse cart = cartService.addItem(
            data.customer().user().tenantId(),
            data.customer().user().id(),
            new CartItemCreateRequest(data.variant().id(), 3)
        );
        inventoryService.create(
            data.customer().user().tenantId(),
            new InventoryMovementRequest(
                data.variant().id(),
                InventoryMovementType.EXIT,
                2,
                "Venta externa"
            )
        );

        CartResponse refreshed = cartService.get(
            data.customer().user().tenantId(),
            data.customer().user().id()
        );
        assertThat(refreshed.readyForCheckout()).isFalse();
        assertThat(refreshed.items().getFirst().available()).isFalse();
        assertThat(refreshed.items().getFirst().availableStock()).isEqualTo(1);
        assertThat(cart.items().getFirst().available()).isTrue();
    }

    private TestData createData(String slug, int stock) {
        Tenant tenant = createTenant(slug);
        LoginResponse customer = authService.registerCustomer(
            new RegisterCustomerRequest(
                slug,
                "Luis",
                "Ramos",
                slug + "@urban.pe",
                "987654323",
                "Password123!",
                null,
                null
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
                "POLO-M-" + slug,
                "M",
                "Negro",
                "#000000",
                stock,
                null,
                true
            )
        );
        productService.updateStatus(tenant.getId(), product.id(), ProductStatus.ACTIVE);
        return new TestData(customer, variant);
    }

    private Tenant createTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug);
        tenant.setStatus("ACTIVE");
        tenant.setPlanName("BASIC");
        return tenantRepository.save(tenant);
    }

    private record TestData(LoginResponse customer, ProductVariantResponse variant) {
    }
}
